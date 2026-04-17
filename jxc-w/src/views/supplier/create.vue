<script setup lang="ts">
import type { UploadUserFile } from 'element-plus';
import type { ComponentPublicInstance } from 'vue';
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  type CreateSupplierPayload,
  createSupplierApi,
  fetchSupplierDetailApi,
  fetchSupplierCategoryTreeApi,
  updateSupplierApi,
  type SupplierCategoryTreeNode,
} from '@/api/modules/supplier';
import { useSessionStore } from '@/stores/session';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';

type SectionKey = 'basic' | 'params' | 'supply' | 'qualification' | 'contract' | 'finance' | 'invoice';

type QualificationRow = {
  id: string;
  files: UploadUserFile[];
  qualificationType: string;
  validTo: string;
  remark: string;
};

type ContractRow = {
  id: string;
  files: UploadUserFile[];
  contractName: string;
  contractCode: string;
  validTo: string;
};

type FinanceRow = {
  id: string;
  bankAccount: string;
  accountName: string;
  bankName: string;
};

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const supplierId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const isEditMode = computed(() => supplierId.value !== null);

const sectionNavs: Array<{ key: SectionKey; label: string }> = [
  { key: 'basic', label: '基础信息' },
  { key: 'params', label: '参数设置' },
  { key: 'supply', label: '供货关系' },
  { key: 'qualification', label: '资质管理' },
  { key: 'contract', label: '合同管理' },
  { key: 'finance', label: '财务信息' },
  { key: 'invoice', label: '开票信息' },
];

const activeSectionKey = ref<SectionKey>('basic');
const contentScrollEl = ref<HTMLElement | null>(null);
const sectionRefs = ref<Record<SectionKey, HTMLElement | null>>({
  basic: null,
  params: null,
  supply: null,
  qualification: null,
  contract: null,
  finance: null,
  invoice: null,
});

const supplierCategoryTree = ref<SupplierCategoryTreeNode[]>([]);

const settlementMethods = ['预付款', '货到付款', '日结', '月结'];
const qualificationTypeOptions = ['营业执照', '食品经营许可证', '餐饮服务许可证', '生产许可证', '卫生许可证', '其他'];
const reconciliationModeOptions = [
  '模式1：不允许供应商在系统发起对账',
  '模式2：允许供应商查看与我往来单据，并发起明对账申请',
  '模式3：允许供应商发起盲对账申请，支撑查看对账差异',
  '模式4：允许供应商发起盲对账申请，不支持查看对账差异',
];

const form = reactive({
  supplierCode: '',
  supplierName: '',
  supplierShortName: '',
  supplierMnemonic: '',
  supplierCategory: '',
  taxRate: 13,
  enabled: true,
  contactPerson: '',
  contactPhone: '',
  email: '',
  contactAddress: '',
  remark: '',

  settlementMethod: '月结',
  orderSummaryRule: '按机构',
  inputBatchWhenDelivery: false,
  syncReceiptData: true,
  purchaseReceiptDependShipping: '不依赖',
  deliveryDependShipping: '不依赖',
  supplierManageInventory: false,
  controlOrderTime: false,
  allowCloseOrder: true,
  reconciliationMode: reconciliationModeOptions[0],

  scopeControl: '开启',

  invoiceCompanyName: '',
  taxpayerId: '',
  invoicePhone: '',
  invoiceAddress: '',
});

const qualificationSeed = ref(1);
const contractSeed = ref(1);
const financeSeed = ref(1);

const createQualificationRow = (): QualificationRow => ({
  id: `qualification-${qualificationSeed.value++}`,
  files: [],
  qualificationType: '营业执照',
  validTo: '',
  remark: '',
});

const createContractRow = (): ContractRow => ({
  id: `contract-${contractSeed.value++}`,
  files: [],
  contractName: '',
  contractCode: '',
  validTo: '',
});

const createFinanceRow = (): FinanceRow => ({
  id: `finance-${financeSeed.value++}`,
  bankAccount: '',
  accountName: '',
  bankName: '',
});

const qualificationRows = ref<QualificationRow[]>([createQualificationRow()]);
const contractRows = ref<ContractRow[]>([createContractRow()]);
const financeRows = ref<FinanceRow[]>([createFinanceRow()]);
const financeDefaultId = ref(financeRows.value[0].id);

const addQualificationRow = (index: number) => {
  qualificationRows.value.splice(index + 1, 0, createQualificationRow());
};

const removeQualificationRow = (index: number) => {
  if (qualificationRows.value.length <= 1) {
    ElMessage.warning('至少保留一条资质记录');
    return;
  }
  qualificationRows.value.splice(index, 1);
};

const addContractRow = (index: number) => {
  contractRows.value.splice(index + 1, 0, createContractRow());
};

const removeContractRow = (index: number) => {
  if (contractRows.value.length <= 1) {
    ElMessage.warning('至少保留一条合同记录');
    return;
  }
  contractRows.value.splice(index, 1);
};

const addFinanceRow = (index: number) => {
  financeRows.value.splice(index + 1, 0, createFinanceRow());
};

const removeFinanceRow = (index: number) => {
  if (financeRows.value.length <= 1) {
    ElMessage.warning('至少保留一条财务记录');
    return;
  }
  const target = financeRows.value[index];
  financeRows.value.splice(index, 1);
  if (financeDefaultId.value === target.id) {
    financeDefaultId.value = financeRows.value[0].id;
  }
};

const resolveContractStatus = (validTo: string) => {
  if (!validTo) {
    return '待维护';
  }
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const target = new Date(`${validTo}T00:00:00`);
  const diffDays = Math.floor((target.getTime() - now.getTime()) / (24 * 60 * 60 * 1000));
  if (diffDays < 0) {
    return '已过期';
  }
  if (diffDays <= 30) {
    return '临期';
  }
  return '有效';
};

const registerSectionRef = (key: SectionKey) => (el: Element | ComponentPublicInstance | null) => {
  if (!el) {
    sectionRefs.value[key] = null;
    return;
  }
  if ('$el' in el) {
    sectionRefs.value[key] = (el.$el as HTMLElement | null);
    return;
  }
  sectionRefs.value[key] = (el as HTMLElement);
};

const updateActiveSectionByScroll = () => {
  const container = contentScrollEl.value;
  if (!container) {
    return;
  }
  const containerTop = container.getBoundingClientRect().top;
  const offset = 74;
  let current = sectionNavs[0].key;
  sectionNavs.forEach((nav) => {
    const sectionEl = sectionRefs.value[nav.key];
    if (!sectionEl) {
      return;
    }
    const top = sectionEl.getBoundingClientRect().top - containerTop;
    if (top <= offset) {
      current = nav.key;
    }
  });
  activeSectionKey.value = current;
};

const scrollToSection = (key: string) => {
  if (!sectionNavs.some((item) => item.key === key)) {
    return;
  }
  const sectionKey = key as SectionKey;
  const target = sectionRefs.value[sectionKey];
  if (!target) {
    return;
  }
  activeSectionKey.value = sectionKey;
  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

onMounted(async () => {
  contentScrollEl.value = document.querySelector('.content');
  contentScrollEl.value?.addEventListener('scroll', updateActiveSectionByScroll, { passive: true });
  updateActiveSectionByScroll();
  await fetchCategoryTree();
  if (isEditMode.value && supplierId.value) {
    try {
      await loadSupplierDetail(supplierId.value);
    } catch {
      router.push('/archive/3/1');
    }
  }
});

onBeforeUnmount(() => {
  contentScrollEl.value?.removeEventListener('scroll', updateActiveSectionByScroll);
});

const goBack = () => {
  router.push('/archive/3/1');
};

const saveDraft = () => {
  ElMessage.success('草稿保存成功');
};

const resolveSectionByField = (fieldPath: string): SectionKey | null => {
  const normalized = fieldPath.replace(/\[\d+\]/g, '');
  if ([
    'supplierCode', 'supplierName', 'supplierShortName', 'supplierMnemonic', 'supplierCategory', 'taxRate',
    'status', 'contactPerson', 'contactPhone', 'email', 'contactAddress', 'remark',
  ].some((key) => normalized.startsWith(key))) {
    return 'basic';
  }
  if ([
    'settlementMethod', 'orderSummaryRule', 'inputBatchWhenDelivery', 'syncReceiptData',
    'purchaseReceiptDependShipping', 'deliveryDependShipping', 'supplierManageInventory',
    'controlOrderTime', 'allowCloseOrder', 'reconciliationMode',
  ].some((key) => normalized.startsWith(key))) {
    return 'params';
  }
  if (normalized.startsWith('scopeControl')) {
    return 'supply';
  }
  if (normalized.startsWith('qualificationList')) {
    return 'qualification';
  }
  if (normalized.startsWith('contractList')) {
    return 'contract';
  }
  if (normalized.startsWith('financeList')) {
    return 'finance';
  }
  if ([
    'invoiceCompanyName', 'taxpayerId', 'invoicePhone', 'invoiceAddress',
  ].some((key) => normalized.startsWith(key))) {
    return 'invoice';
  }
  return null;
};

const resolveFirstValidationField = (message: string) => {
  if (!message) {
    return null;
  }
  const parts = message.split(';').map((item) => item.trim()).filter(Boolean);
  for (const part of parts) {
    const index = part.indexOf(':');
    if (index > 0) {
      const field = part.slice(0, index).replace(/^请求参数校验失败/, '').replace(/^[:：]\s*/, '').trim();
      if (field && !field.includes('请求参数校验失败')) {
        return field;
      }
    }
  }
  const match = message.match(/([a-zA-Z][\w.[\]]*)\s*[:：]/);
  return match?.[1] ?? null;
};

const focusFieldByPath = async (fieldPath: string) => {
  await nextTick();
  const root = document.querySelector('.item-create-page');
  if (!root) {
    return;
  }
  const containers = Array.from(root.querySelectorAll<HTMLElement>('[data-field]'));
  const container = containers.find((item) => item.getAttribute('data-field') === fieldPath);
  if (!container) {
    return;
  }

  const focusTarget = container.querySelector<HTMLElement>(
    'input, textarea, .el-input__inner, .el-textarea__inner, button, .el-select__wrapper, .el-date-editor',
  ) ?? container;

  if (typeof focusTarget.focus === 'function') {
    focusTarget.focus();
  }
};

const resolveSupplierOrgId = () => {
  const orgId = (sessionStore.currentOrgId ?? '').trim();
  if (!orgId) {
    return undefined;
  }
  if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
    return orgId;
  }
  return undefined;
};

const categoryExists = (nodes: SupplierCategoryTreeNode[], value: string): boolean => {
  for (const node of nodes) {
    if (node.label === value) {
      return true;
    }
    if (node.children?.length && categoryExists(node.children, value)) {
      return true;
    }
  }
  return false;
};

const fetchCategoryTree = async () => {
  try {
    supplierCategoryTree.value = await fetchSupplierCategoryTreeApi(resolveSupplierOrgId());
  } catch {
    supplierCategoryTree.value = [];
    ElMessage.error('供应商类别加载失败');
  }
  if (form.supplierCategory && !categoryExists(supplierCategoryTree.value, form.supplierCategory)) {
    form.supplierCategory = '';
  }
};

const buildUploadFiles = (name?: string, url?: string): UploadUserFile[] => {
  if (!name && !url) {
    return [];
  }
  return [{
    uid: Date.now(),
    name: name || '已上传文件',
    url,
    status: 'success',
  }];
};

const loadSupplierDetail = async (id: number) => {
  const detail = await fetchSupplierDetailApi(id, resolveSupplierOrgId());
  form.supplierCode = detail.supplierCode ?? '';
  form.supplierName = detail.supplierName ?? '';
  form.supplierShortName = detail.supplierShortName ?? '';
  form.supplierMnemonic = detail.supplierMnemonic ?? '';
  form.supplierCategory = detail.supplierCategory ?? '';
  form.taxRate = Number(detail.taxRate ?? 0);
  form.enabled = detail.status !== '停用';
  form.contactPerson = detail.contactPerson ?? '';
  form.contactPhone = detail.contactPhone ?? '';
  form.email = detail.email ?? '';
  form.contactAddress = detail.contactAddress ?? '';
  form.remark = detail.remark ?? '';
  form.settlementMethod = (detail.settlementMethod || '月结') as '预付款' | '货到付款' | '日结' | '月结';
  form.orderSummaryRule = (detail.orderSummaryRule || '按机构') as '按机构' | '按仓库';
  form.inputBatchWhenDelivery = Boolean(detail.inputBatchWhenDelivery);
  form.syncReceiptData = Boolean(detail.syncReceiptData);
  form.purchaseReceiptDependShipping = (detail.purchaseReceiptDependShipping || '不依赖') as '不依赖' | '依赖';
  form.deliveryDependShipping = (detail.deliveryDependShipping || '不依赖') as '不依赖' | '依赖';
  form.supplierManageInventory = Boolean(detail.supplierManageInventory);
  form.controlOrderTime = Boolean(detail.controlOrderTime);
  form.allowCloseOrder = Boolean(detail.allowCloseOrder);
  form.reconciliationMode = detail.reconciliationMode || reconciliationModeOptions[0];
  form.scopeControl = (detail.scopeControl || '开启') as '开启' | '关闭';
  form.invoiceCompanyName = detail.invoiceCompanyName ?? '';
  form.taxpayerId = detail.taxpayerId ?? '';
  form.invoicePhone = detail.invoicePhone ?? '';
  form.invoiceAddress = detail.invoiceAddress ?? '';

  qualificationRows.value = (detail.qualificationList?.length ? detail.qualificationList : [{
    qualificationType: '营业执照',
  }]).map((item) => ({
    id: `qualification-${qualificationSeed.value++}`,
    files: buildUploadFiles(item.fileName, item.fileUrl),
    qualificationType: item.qualificationType || '营业执照',
    validTo: item.validTo || '',
    remark: item.remark || '',
  }));

  contractRows.value = (detail.contractList?.length ? detail.contractList : [{}]).map((item) => ({
    id: `contract-${contractSeed.value++}`,
    files: buildUploadFiles(item.attachmentName, item.attachmentUrl),
    contractName: item.contractName || '',
    contractCode: item.contractCode || '',
    validTo: item.validTo || '',
  }));

  const financeList = detail.financeList ?? [];
  financeRows.value = (financeList.length ? financeList : [{
    bankAccount: '',
    accountName: '',
    bankName: '',
    defaultAccount: true,
  }]).map((item) => ({
    id: `finance-${financeSeed.value++}`,
    bankAccount: item.bankAccount || '',
    accountName: item.accountName || '',
    bankName: item.bankName || '',
  }));
  const defaultIndex = financeList.findIndex((item) => item.defaultAccount);
  financeDefaultId.value = financeRows.value[Math.max(defaultIndex, 0)]?.id || financeRows.value[0].id;
};

const buildPayload = (): CreateSupplierPayload => ({
  supplierCode: isEditMode.value ? form.supplierCode.trim() : undefined,
  supplierName: form.supplierName.trim(),
  supplierShortName: form.supplierShortName.trim() || undefined,
  supplierMnemonic: form.supplierMnemonic.trim() || undefined,
  supplierCategory: form.supplierCategory.trim(),
  taxRate: Number(form.taxRate),
  status: form.enabled ? '启用' : '停用',
  contactPerson: form.contactPerson.trim() || undefined,
  contactPhone: form.contactPhone.trim() || undefined,
  email: form.email.trim() || undefined,
  contactAddress: form.contactAddress.trim() || undefined,
  remark: form.remark.trim() || undefined,
  settlementMethod: form.settlementMethod as '预付款' | '货到付款' | '日结' | '月结',
  orderSummaryRule: form.orderSummaryRule as '按机构' | '按仓库',
  inputBatchWhenDelivery: form.inputBatchWhenDelivery,
  syncReceiptData: form.syncReceiptData,
  purchaseReceiptDependShipping: form.purchaseReceiptDependShipping as '不依赖' | '依赖',
  deliveryDependShipping: form.deliveryDependShipping as '不依赖' | '依赖',
  supplierManageInventory: form.supplierManageInventory,
  controlOrderTime: form.controlOrderTime,
  allowCloseOrder: form.allowCloseOrder,
  reconciliationMode: form.reconciliationMode,
  scopeControl: form.scopeControl as '开启' | '关闭',
  qualificationList: qualificationRows.value.map((row) => ({
    fileName: row.files?.map((file) => file.name).join(',') || undefined,
    fileUrl: row.files?.[0]?.url || undefined,
    qualificationType: row.qualificationType,
    validTo: row.validTo || undefined,
    status: '临期',
    remark: row.remark.trim() || undefined,
  })),
  contractList: contractRows.value.map((row) => ({
    attachmentName: row.files?.map((file) => file.name).join(',') || undefined,
    attachmentUrl: row.files?.[0]?.url || undefined,
    contractName: row.contractName.trim() || undefined,
    contractCode: row.contractCode.trim() || undefined,
    validTo: row.validTo || undefined,
    status: resolveContractStatus(row.validTo),
  })),
  financeList: financeRows.value.map((row) => ({
    bankAccount: row.bankAccount.trim(),
    accountName: row.accountName.trim(),
    bankName: row.bankName.trim(),
    defaultAccount: row.id === financeDefaultId.value,
  })),
  invoiceCompanyName: form.invoiceCompanyName.trim() || undefined,
  taxpayerId: form.taxpayerId.trim() || undefined,
  invoicePhone: form.invoicePhone.trim() || undefined,
  invoiceAddress: form.invoiceAddress.trim() || undefined,
});

const saveSupplier = async () => {
  const hasDefaultAccount = financeRows.value.some((item) => item.id === financeDefaultId.value);
  if (!hasDefaultAccount) {
    ElMessage.warning('请设置默认银行账号');
    return;
  }
  try {
    const payload = buildPayload();
    if (isEditMode.value && supplierId.value) {
      await updateSupplierApi(supplierId.value, payload, resolveSupplierOrgId());
    } else {
      await createSupplierApi(payload, resolveSupplierOrgId());
    }
    ElMessage.success(isEditMode.value ? '供应商更新成功' : '供应商保存成功');
    router.push('/archive/3/1');
  } catch (error) {
    const message = error instanceof Error ? error.message : '';
    const fieldPath = resolveFirstValidationField(message);
    if (fieldPath) {
      const section = resolveSectionByField(fieldPath);
      if (section) {
        scrollToSection(section);
      }
      await focusFieldByPath(fieldPath);
      ElMessage.warning(`校验失败字段：${fieldPath}`);
    }
  }
};
</script>

<template>
  <div class="item-create-page">
    <section class="panel form-panel">
      <FixedActionBreadcrumb
        :navs="sectionNavs"
        :active-key="activeSectionKey"
        @back="goBack"
        @save-draft="saveDraft"
        @save="saveSupplier"
        @navigate="scrollToSection"
      />

      <el-form :model="form" label-width="100px" class="item-create-form">
        <div class="form-section-block" :ref="registerSectionRef('basic')">
          <div class="form-section-title">基础信息</div>
          <div class="item-form-grid">
            <el-form-item v-if="isEditMode" label="供货商编码" data-field="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="请输入供货商编码" />
            </el-form-item>
            <el-form-item label="供货商名称" data-field="supplierName">
              <el-input v-model="form.supplierName" placeholder="请输入供货商名称" />
            </el-form-item>
            <el-form-item label="供货商简称" data-field="supplierShortName">
              <el-input v-model="form.supplierShortName" placeholder="请输入供货商简称" />
            </el-form-item>
            <el-form-item label="供货商助记码" data-field="supplierMnemonic">
              <el-input v-model="form.supplierMnemonic" placeholder="请输入供货商助记码" />
            </el-form-item>
            <el-form-item label="供货商类别" data-field="supplierCategory">
              <el-tree-select
                v-model="form.supplierCategory"
                :data="supplierCategoryTree"
                node-key="id"
                :props="{ label: 'label', children: 'children', value: 'label' }"
                check-strictly
                clearable
                placeholder="请选择供货商类别"
              />
            </el-form-item>
            <el-form-item label="税率(%)" data-field="taxRate">
              <el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="2" :step="0.5" />
            </el-form-item>
            <el-form-item label="启用状态" data-field="status">
              <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
            <el-form-item label="联系人" data-field="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
            <el-form-item label="联系电话" data-field="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="邮箱地址" data-field="email">
              <el-input v-model="form.email" placeholder="请输入邮箱地址" />
            </el-form-item>
            <el-form-item label="联系地址" class="supplier-form-half" data-field="contactAddress">
              <el-input v-model="form.contactAddress" placeholder="请输入联系地址" />
            </el-form-item>
            <el-form-item label="备注" class="supplier-form-full" data-field="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('params')">
          <div class="form-section-title">参数设置</div>
          <div class="item-form-grid">
            <el-form-item label="结算方式" data-field="settlementMethod">
              <el-select v-model="form.settlementMethod">
                <el-option v-for="item in settlementMethods" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="订单汇总规则" class="supplier-form-half" data-field="orderSummaryRule">
              <el-radio-group v-model="form.orderSummaryRule">
                <el-radio label="按机构">按机构</el-radio>
                <el-radio label="按仓库">按仓库</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="发货时录入批次" data-field="inputBatchWhenDelivery">
              <el-switch v-model="form.inputBatchWhenDelivery" />
            </el-form-item>
            <el-form-item label="同步收货数据" data-field="syncReceiptData">
              <el-switch v-model="form.syncReceiptData" />
            </el-form-item>
            <el-form-item label="采购收货依赖供货商发货" class="supplier-form-half" data-field="purchaseReceiptDependShipping">
              <el-radio-group v-model="form.purchaseReceiptDependShipping">
                <el-radio label="不依赖">不依赖</el-radio>
                <el-radio label="依赖">依赖</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="配送依赖供应商发货" class="supplier-form-half" data-field="deliveryDependShipping">
              <el-radio-group v-model="form.deliveryDependShipping">
                <el-radio label="不依赖">不依赖</el-radio>
                <el-radio label="依赖">依赖</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="供货商管理库存" data-field="supplierManageInventory">
              <el-switch v-model="form.supplierManageInventory" />
            </el-form-item>
            <el-form-item label="控制下单时间" data-field="controlOrderTime">
              <el-switch v-model="form.controlOrderTime" />
            </el-form-item>
            <el-form-item label="允许关闭订单" data-field="allowCloseOrder">
              <el-switch v-model="form.allowCloseOrder" />
            </el-form-item>
            <el-form-item label="供应商对账模式" class="supplier-form-full" data-field="reconciliationMode">
              <el-select v-model="form.reconciliationMode" style="width: 100%">
                <el-option v-for="item in reconciliationModeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('supply')">
          <div class="form-section-title">供货关系</div>
          <div class="item-form-grid">
            <el-form-item label="范围控制" class="supplier-form-half" data-field="scopeControl">
              <el-radio-group v-model="form.scopeControl">
                <el-radio label="开启">开启</el-radio>
                <el-radio label="关闭">关闭</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('qualification')">
          <div class="form-section-title">资质管理</div>
          <el-table :data="qualificationRows" border class="erp-table supplier-dynamic-table" :fit="false">
            <el-table-column type="index" label="序号" width="56" />
            <el-table-column label="操作" width="78">
              <template #default="{ $index }">
                <div class="supplier-cell-center">
                  <el-button text type="primary" class="unit-op-btn" @click="addQualificationRow($index)">+</el-button>
                  <el-button text type="danger" class="unit-op-btn" @click="removeQualificationRow($index)">-</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="资质文件" min-width="140" align="center">
              <template #default="{ row, $index }">
                <div class="supplier-cell-center" :data-field="`qualificationList[${$index}].fileUrl`">
                  <el-upload
                    v-model:file-list="row.files"
                    :auto-upload="false"
                    :limit="1"
                    :show-file-list="false"
                  >
                    <el-button class="unit-op-btn">上传文件</el-button>
                  </el-upload>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="资质类型" min-width="160">
              <template #default="{ row, $index }">
                <div :data-field="`qualificationList[${$index}].qualificationType`">
                  <el-select v-model="row.qualificationType">
                    <el-option v-for="item in qualificationTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="有效期至" width="130">
              <template #default="{ row, $index }">
                <div :data-field="`qualificationList[${$index}].validTo`">
                  <el-date-picker
                    v-model="row.validTo"
                    type="date"
                    value-format="YYYY-MM-DD"
                    format="YYYY-MM-DD"
                    placeholder="请选择"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="88">
              <template #default>
                临期
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="180">
              <template #default="{ row, $index }">
                <div :data-field="`qualificationList[${$index}].remark`">
                  <el-input v-model="row.remark" placeholder="请输入备注" />
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('contract')">
          <div class="form-section-title">合同管理</div>
          <el-table :data="contractRows" border class="erp-table supplier-dynamic-table" :fit="false">
            <el-table-column type="index" label="序号" width="56" />
            <el-table-column label="操作" width="78">
              <template #default="{ $index }">
                <div class="supplier-cell-center">
                  <el-button text type="primary" class="unit-op-btn" @click="addContractRow($index)">+</el-button>
                  <el-button text type="danger" class="unit-op-btn" @click="removeContractRow($index)">-</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="合同附件" min-width="140" align="center">
              <template #default="{ row, $index }">
                <div class="supplier-cell-center" :data-field="`contractList[${$index}].attachmentUrl`">
                  <el-upload
                    v-model:file-list="row.files"
                    :auto-upload="false"
                    :limit="1"
                    :show-file-list="false"
                  >
                    <el-button class="unit-op-btn">上传文件</el-button>
                  </el-upload>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="合同名称" min-width="160">
              <template #default="{ row, $index }">
                <div :data-field="`contractList[${$index}].contractName`">
                  <el-input v-model="row.contractName" placeholder="请输入合同名称" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="合同编号" min-width="140">
              <template #default="{ row, $index }">
                <div :data-field="`contractList[${$index}].contractCode`">
                  <el-input v-model="row.contractCode" placeholder="请输入合同编号" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="有效期至" width="130">
              <template #default="{ row, $index }">
                <div :data-field="`contractList[${$index}].validTo`">
                  <el-date-picker
                    v-model="row.validTo"
                    type="date"
                    value-format="YYYY-MM-DD"
                    format="YYYY-MM-DD"
                    placeholder="请选择"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="88">
              <template #default="{ row }">
                {{ resolveContractStatus(row.validTo) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('finance')">
          <div class="form-section-title">财务信息</div>
          <el-table :data="financeRows" border class="erp-table supplier-dynamic-table" :fit="false">
            <el-table-column type="index" label="序号" width="56" />
            <el-table-column label="操作" width="78">
              <template #default="{ $index }">
                <div class="supplier-cell-center">
                  <el-button text type="primary" class="unit-op-btn" @click="addFinanceRow($index)">+</el-button>
                  <el-button text type="danger" class="unit-op-btn" @click="removeFinanceRow($index)">-</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="银行账号" min-width="180">
              <template #default="{ row, $index }">
                <div :data-field="`financeList[${$index}].bankAccount`">
                  <el-input v-model="row.bankAccount" placeholder="请输入银行账号" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="账户名称" min-width="160">
              <template #default="{ row, $index }">
                <div :data-field="`financeList[${$index}].accountName`">
                  <el-input v-model="row.accountName" placeholder="请输入账户名称" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="银行名称" min-width="160">
              <template #default="{ row, $index }">
                <div :data-field="`financeList[${$index}].bankName`">
                  <el-input v-model="row.bankName" placeholder="请输入银行名称" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="默认银行账号" width="120" align="center">
              <template #default="{ row, $index }">
                <div :data-field="`financeList[${$index}].defaultAccount`">
                  <el-radio v-model="financeDefaultId" :label="row.id">默认</el-radio>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('invoice')">
          <div class="form-section-title">开票信息</div>
          <div class="item-form-grid">
            <el-form-item label="单位名称" data-field="invoiceCompanyName">
              <el-input v-model="form.invoiceCompanyName" placeholder="请输入单位名称" />
            </el-form-item>
            <el-form-item label="纳税人识别号" data-field="taxpayerId">
              <el-input v-model="form.taxpayerId" placeholder="请输入纳税人识别号" />
            </el-form-item>
            <el-form-item label="电话" data-field="invoicePhone">
              <el-input v-model="form.invoicePhone" placeholder="请输入电话" />
            </el-form-item>
            <el-form-item label="单位地址" class="supplier-form-half" data-field="invoiceAddress">
              <el-input v-model="form.invoiceAddress" placeholder="请输入单位地址" />
            </el-form-item>
          </div>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.supplier-form-full {
  grid-column: 1 / -1;
}

.supplier-form-half {
  grid-column: span 2;
}

.supplier-dynamic-table {
  width: 100%;
}

.supplier-cell-center {
  width: 100%;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.supplier-cell-center :deep(.el-upload) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
