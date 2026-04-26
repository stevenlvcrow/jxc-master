<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonNumberInput from '@/components/CommonNumberInput.vue';
import CommonSelectorDialog, {
  type SelectorColumn,
  type SelectorTreeNode,
} from '@/components/CommonSelectorDialog.vue';
import {
  approvePeriodOpeningApi,
  createPeriodOpeningApi,
  fetchPeriodOpeningDetailApi,
  rejectPeriodOpeningApi,
  submitPeriodOpeningApi,
  updatePeriodOpeningApi,
  type PeriodOpeningDetail,
  type PeriodOpeningSavePayload,
} from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { fetchStoreWarehousesApi, fetchWarehousesApi, type WarehouseRow } from '@/api/modules/warehouse';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useRequiredOrgScope } from '@/composables/useRequiredOrgScope';
import { useSessionStore } from '@/stores/session';
import { parseGroupId, parseStoreId } from '@/utils/org';

type WarehouseOption = {
  id: number;
  name: string;
  code: string;
  label: string;
};

type ItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  unitName: string;
  openingQty: number | null;
  openingAmount: number | null;
  remark: string;
};

type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  unitName: string;
  status: string;
};

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const { orgId, requireOrgId } = useRequiredOrgScope();
const INVENTORY_PERIOD_TYPE_DICT = 'inventory.period_type';
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const ITEM_STATUS_DICT = 'item.status';
const { optionsOf } = useDictionaryOptions([
  INVENTORY_PERIOD_TYPE_DICT,
  INVENTORY_DOCUMENT_STATUS_DICT,
  ITEM_STATUS_DICT,
]);
const periodTypeOptions = optionsOf(INVENTORY_PERIOD_TYPE_DICT);
const documentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);
const itemStatusOptions = optionsOf(ITEM_STATUS_DICT, { enabled: true, label: '全部', value: '' });

const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '期初明细' },
];

const openingId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const isCreateMode = computed(() => route.name === 'PeriodOpeningCreate');
const isViewMode = computed(() => route.name === 'PeriodOpeningView');
const isEditMode = computed(() => route.name === 'PeriodOpeningEdit');
const isApprovalMode = computed(() => String(route.query.approvalMode ?? '').trim() === '1' && openingId.value != null);
const draftStatus = computed(() =>
  documentStatusOptions.value.find((item) => item.itemKey === 'DRAFT')?.itemCode ?? '',
);
const submittedStatus = computed(() =>
  documentStatusOptions.value.find((item) => item.itemKey === 'SUBMITTED')?.itemCode ?? '',
);
const approvedStatus = computed(() =>
  documentStatusOptions.value.find((item) => item.itemKey === 'APPROVED')?.itemCode ?? '',
);
const statusReady = computed(() => Boolean(draftStatus.value && submittedStatus.value && approvedStatus.value));

const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);
const loading = ref(false);
const saving = ref(false);
const detailStatus = ref('');
const rowSeed = ref(2);
const warehouseOptions = ref<WarehouseOption[]>([]);

const form = reactive({
  documentCode: '',
  warehouseName: '',
  periodType: 'MONTH',
  periodStartDate: '',
  periodEndDate: '',
  sourceType: 'MANUAL',
  remark: '',
  rejectionReason: '',
  creator: '',
  createdAt: '',
  auditor: '',
  approvedAt: '',
});

const createEmptyRow = (id: number): ItemRow => ({
  id,
  itemCode: '',
  itemName: '',
  spec: '',
  category: '',
  unitName: '',
  openingQty: null,
  openingAmount: null,
  remark: '',
});

const rows = ref<ItemRow[]>([createEmptyRow(1)]);

const itemSelectorVisible = ref(false);
const itemSelectorKeyword = ref('');
const itemSelectorStatus = ref('');
const activeItemTreeId = ref('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingItemRowIndex = ref<number | null>(null);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);
const itemCandidateSource = ref<ItemCandidate[]>([]);

const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'category', label: '物品类别', minWidth: 120 },
  { prop: 'unitName', label: '库存单位', minWidth: 100 },
  { prop: 'status', label: '状态', minWidth: 80 },
];

const normalizedItemStatusOptions = computed(() => itemStatusOptions.value.map((item) => ({
  label: item.itemLabel,
  value: item.itemCode,
})));

const statusLabelMap = computed(() =>
  documentStatusOptions.value.reduce<Record<string, string>>((result, item) => {
    result[item.itemCode] = item.itemLabel;
    return result;
  }, {}),
);
const sourceTypeLabel = computed(() => (form.sourceType === 'GENERATED' ? '结存生成' : '人工录入'));
const isApproved = computed(() => Boolean(approvedStatus.value) && detailStatus.value === approvedStatus.value);
const isGenerated = computed(() => form.sourceType === 'GENERATED');
const showSubmitAction = computed(() =>
  isViewMode.value
  && !isApprovalMode.value
  && Boolean(draftStatus.value)
  && detailStatus.value === draftStatus.value,
);
const showApprovalActions = computed(() =>
  (isApprovalMode.value || isViewMode.value)
  && Boolean(submittedStatus.value)
  && detailStatus.value === submittedStatus.value,
);
const isReadonlyMode = computed(() => {
  if (isApprovalMode.value || isViewMode.value || isGenerated.value || isApproved.value) {
    return true;
  }
  if (detailStatus.value && !statusReady.value) {
    return true;
  }
  return !isCreateMode.value && !isEditMode.value;
});
const showActions = computed(() => showSubmitAction.value || showApprovalActions.value || !isReadonlyMode.value);
const actionPrimaryText = computed(() => {
  if (showApprovalActions.value) {
    return '审核通过';
  }
  if (showSubmitAction.value) {
    return '提交';
  }
  return '保存';
});
const actionSecondaryText = computed(() => (showApprovalActions.value ? '驳回' : '保存草稿'));
const totalQuantity = computed(() => rows.value.reduce((sum, row) => sum + Number(row.openingQty ?? 0), 0));
const totalAmount = computed(() => rows.value.reduce((sum, row) => sum + Number(row.openingAmount ?? 0), 0));

const resetForm = () => {
  detailStatus.value = '';
  form.documentCode = '';
  form.warehouseName = '';
  form.periodType = 'MONTH';
  form.periodStartDate = '';
  form.periodEndDate = '';
  form.sourceType = 'MANUAL';
  form.remark = '';
  form.rejectionReason = '';
  form.creator = '';
  form.createdAt = '';
  form.auditor = '';
  form.approvedAt = '';
  rowSeed.value = 2;
  rows.value = [createEmptyRow(1)];
};

const mapWarehouseOption = (row: WarehouseRow): WarehouseOption => ({
  id: row.id,
  name: row.warehouseName,
  code: row.warehouseCode,
  label: row.warehouseCode ? `${row.warehouseName} / ${row.warehouseCode}` : row.warehouseName,
});

const loadWarehouses = async () => {
  if (!orgId.value) {
    warehouseOptions.value = [];
    return;
  }
  const groupId = parseGroupId(orgId.value);
  const storeId = parseStoreId(orgId.value);
  const data = groupId
    ? await fetchWarehousesApi(groupId, { status: 'ENABLED' })
    : storeId
      ? await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' })
      : [];
  warehouseOptions.value = data.map(mapWarehouseOption).sort((left, right) =>
    left.name.localeCompare(right.name, 'zh-Hans-CN'),
  );
};

const applyDetail = (detail: PeriodOpeningDetail) => {
  detailStatus.value = detail.status ?? '';
  form.documentCode = detail.documentCode ?? '';
  form.warehouseName = detail.warehouseName ?? '';
  form.periodType = detail.periodType ?? 'MONTH';
  form.periodStartDate = detail.periodStartDate ?? '';
  form.periodEndDate = detail.periodEndDate ?? '';
  form.sourceType = detail.sourceType ?? 'MANUAL';
  form.remark = detail.remark ?? '';
  form.rejectionReason = detail.rejectionReason ?? '';
  form.creator = detail.creator ?? '';
  form.createdAt = detail.createdAt ?? '';
  form.auditor = detail.auditor ?? '';
  form.approvedAt = detail.approvedAt ?? '';
  const detailRows = detail.items?.length
    ? detail.items.map((item, index) => ({
      id: index + 1,
      itemCode: item.itemCode ?? '',
      itemName: item.itemName ?? '',
      spec: item.spec ?? '',
      category: item.category ?? '',
      unitName: item.unitName ?? '',
      openingQty: Number(item.openingQty ?? 0),
      openingAmount: Number(item.openingAmount ?? 0),
      remark: item.remark ?? '',
    }))
    : [createEmptyRow(1)];
  rows.value = detailRows;
  rowSeed.value = detailRows.length + 1;
};

const loadDetail = async () => {
  if (openingId.value == null) {
    return;
  }
  const currentOrgId = orgId.value;
  if (!currentOrgId) {
    return;
  }
  const detail = await fetchPeriodOpeningDetailApi(openingId.value, currentOrgId);
  applyDetail(detail);
};

const normalizeItemTreeNodes = (nodes: ItemCategoryTreeNode[]): SelectorTreeNode[] => nodes.map((node) => ({
  id: String(node.label ?? 'all'),
  label: String(node.label ?? ''),
  children: Array.isArray(node.children) ? normalizeItemTreeNodes(node.children) : undefined,
}));

const loadItemTree = async () => {
  if (!orgId.value) {
    itemTreeData.value = [{ id: 'all', label: '全部' }];
    return;
  }
  const tree = await fetchItemCategoryTreeApi(orgId.value);
  itemTreeData.value = Array.isArray(tree) && tree.length
    ? [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }]
    : [{ id: 'all', label: '全部' }];
};

const mapItemCandidate = (row: ItemVO): ItemCandidate => ({
  id: row.id || row.code,
  code: row.code,
  name: row.name,
  spec: row.spec,
  category: row.category,
  unitName: row.stockUnit,
  status: row.status,
});

const loadItemCandidates = async () => {
  if (!orgId.value) {
    itemCandidateSource.value = [];
    itemSelectorTotal.value = 0;
    return;
  }
  itemSelectorLoading.value = true;
  try {
    const page = await fetchItemsApi({
      pageNo: itemSelectorCurrentPage.value,
      pageSize: itemSelectorPageSize.value,
      keyword: itemSelectorKeyword.value.trim() || undefined,
      category: activeItemTreeId.value === 'all' ? undefined : activeItemTreeId.value,
      status: itemSelectorStatus.value || undefined,
    }, orgId.value);
    itemCandidateSource.value = page.list.map(mapItemCandidate);
    itemSelectorTotal.value = Number(page.total ?? 0);
  } finally {
    itemSelectorLoading.value = false;
  }
};

const loadPageData = async () => {
  resetForm();
  loading.value = true;
  try {
    await Promise.all([loadWarehouses(), loadItemTree()]);
    await loadDetail();
  } finally {
    loading.value = false;
  }
};

const handleBack = () => {
  router.push('/inventory/period-openings');
};

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const addRow = (index?: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  const targetIndex = typeof index === 'number' ? index + 1 : rows.value.length;
  rows.value.splice(targetIndex, 0, createEmptyRow(rowSeed.value));
  rowSeed.value += 1;
};

const removeRow = (index: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  if (rows.value.length <= 1) {
    ElMessage.warning('至少保留一条物品');
    return;
  }
  rows.value.splice(index, 1);
};

const openItemSelector = async (index: number | null = null) => {
  if (isReadonlyMode.value) {
    return;
  }
  selectingItemRowIndex.value = index;
  selectedItemCandidates.value = [];
  await loadItemCandidates();
  itemSelectorVisible.value = true;
};

const handleItemSelectorSearch = (payload: { keyword: string; status: string }) => {
  itemSelectorKeyword.value = payload.keyword;
  itemSelectorStatus.value = payload.status;
  itemSelectorCurrentPage.value = 1;
  void loadItemCandidates();
};

const handleItemNodeChange = (node: SelectorTreeNode | null) => {
  activeItemTreeId.value = String(node?.id ?? 'all');
  itemSelectorCurrentPage.value = 1;
  void loadItemCandidates();
};

const handleItemSelectionChange = (selectedRows: Array<Record<string, unknown>>) => {
  selectedItemCandidates.value = selectedRows;
};

const handleItemClear = () => {
  selectedItemCandidates.value = [];
};

const applyItemToRow = (row: ItemRow, item: ItemCandidate) => {
  row.itemCode = item.code;
  row.itemName = item.name;
  row.spec = item.spec;
  row.category = item.category;
  row.unitName = item.unitName;
};

const hasDuplicateItem = (itemCode: string, ignoreIndex: number | null) => rows.value.some((row, index) =>
  row.itemCode === itemCode && index !== ignoreIndex,
);

const handleItemSelectorConfirm = (selectedRows: Array<Record<string, unknown>>) => {
  const picked = selectedRows as ItemCandidate[];
  if (!picked.length) {
    ElMessage.warning('请至少选择一个物品');
    return;
  }
  const targetIndex = selectingItemRowIndex.value;
  const duplicate = picked.find((item, index) =>
    hasDuplicateItem(item.code, index === 0 ? targetIndex : null),
  );
  if (duplicate) {
    ElMessage.warning(`物品 ${duplicate.code} 已存在期初明细`);
    return;
  }
  if (targetIndex != null && rows.value[targetIndex]) {
    applyItemToRow(rows.value[targetIndex], picked[0]);
    const appendRows = picked.slice(1).map((item) => {
      const row = createEmptyRow(rowSeed.value);
      rowSeed.value += 1;
      applyItemToRow(row, item);
      return row;
    });
    rows.value.splice(targetIndex + 1, 0, ...appendRows);
  } else {
    picked.forEach((item) => {
      const row = createEmptyRow(rowSeed.value);
      rowSeed.value += 1;
      applyItemToRow(row, item);
      rows.value.push(row);
    });
  }
  itemSelectorVisible.value = false;
};

const formatMoney = (value: number) => Number(value || 0).toFixed(2);

const calcAvgCost = (row: ItemRow) => {
  const quantity = Number(row.openingQty ?? 0);
  const amount = Number(row.openingAmount ?? 0);
  if (quantity <= 0) {
    return '0.00';
  }
  return formatMoney(amount / quantity);
};

const rowHasInput = (row: ItemRow) => Boolean(
  row.itemCode
  || row.itemName
  || row.unitName
  || row.openingQty != null
  || row.openingAmount != null,
);

const validateRows = () => {
  const filledRows = rows.value.filter(rowHasInput);
  if (!filledRows.length) {
    ElMessage.warning('请添加期初明细');
    return null;
  }
  for (const row of filledRows) {
    if (!row.itemCode || !row.itemName || !row.unitName) {
      ElMessage.warning('请完善物品编码、名称和库存单位');
      return null;
    }
    const quantity = Number(row.openingQty);
    const amount = Number(row.openingAmount);
    if (!Number.isFinite(quantity) || !Number.isFinite(amount) || quantity < 0 || amount < 0) {
      ElMessage.warning('期初数量和期初金额必须为非负数');
      return null;
    }
    if (quantity === 0 && amount !== 0) {
      ElMessage.warning(`物品 ${row.itemCode} 数量为0时金额必须为0`);
      return null;
    }
    if (quantity > 0 && amount <= 0) {
      ElMessage.warning(`物品 ${row.itemCode} 数量大于0时金额必须大于0`);
      return null;
    }
  }
  return filledRows;
};

const buildPayload = (): PeriodOpeningSavePayload | null => {
  if (!form.warehouseName) {
    ElMessage.warning('请选择仓库');
    return null;
  }
  if (!form.periodType) {
    ElMessage.warning('请选择周期类型');
    return null;
  }
  if (!form.periodStartDate) {
    ElMessage.warning('请选择周期开始日期');
    return null;
  }
  const validRows = validateRows();
  if (!validRows) {
    return null;
  }
  return {
    warehouseName: form.warehouseName,
    periodType: form.periodType,
    periodStartDate: form.periodStartDate,
    remark: form.remark || undefined,
    items: validRows.map((row) => ({
      itemCode: row.itemCode,
      itemName: row.itemName,
      spec: row.spec,
      category: row.category,
      unitName: row.unitName,
      openingQty: Number(row.openingQty ?? 0),
      openingAmount: Number(row.openingAmount ?? 0),
      remark: row.remark || undefined,
    })),
  };
};

const handleSave = async () => {
  if (isReadonlyMode.value) {
    return;
  }
  const currentOrgId = requireOrgId();
  if (!currentOrgId) {
    return;
  }
  const payload = buildPayload();
  if (!payload) {
    return;
  }
  saving.value = true;
  try {
    if (isEditMode.value && openingId.value != null) {
      await updatePeriodOpeningApi(openingId.value, payload, currentOrgId);
      ElMessage.success('保存成功');
      await router.push({ name: 'PeriodOpeningView', params: { id: openingId.value } });
      return;
    }
    const created = await createPeriodOpeningApi(payload, currentOrgId);
    ElMessage.success(`保存成功：${created.documentCode}`);
    await router.push({ name: 'PeriodOpeningView', params: { id: created.id } });
  } finally {
    saving.value = false;
  }
};

const handleSubmitAction = async () => {
  const currentOrgId = requireOrgId();
  if (!currentOrgId || openingId.value == null) {
    return;
  }
  await submitPeriodOpeningApi(openingId.value, currentOrgId);
  ElMessage.success('提交成功');
  await loadPageData();
};

const handleApproveAction = async () => {
  const currentOrgId = requireOrgId();
  if (!currentOrgId || openingId.value == null) {
    return;
  }
  await approvePeriodOpeningApi(openingId.value, currentOrgId);
  ElMessage.success('审核通过成功');
  await router.push('/inventory/period-openings');
};

const handleRejectAction = async () => {
  const currentOrgId = requireOrgId();
  if (!currentOrgId || openingId.value == null) {
    return;
  }
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回期初库存', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空',
  });
  await rejectPeriodOpeningApi(openingId.value, value.trim(), currentOrgId);
  ElMessage.success('已驳回');
  await router.push('/inventory/period-openings');
};

const handlePrimaryAction = () => {
  if (showApprovalActions.value) {
    void handleApproveAction();
    return;
  }
  if (showSubmitAction.value) {
    void handleSubmitAction();
    return;
  }
  void handleSave();
};

const handleSecondaryAction = () => {
  if (showApprovalActions.value) {
    void handleRejectAction();
  }
};

watch(
  () => [route.name, route.params.id, sessionStore.currentOrgId],
  () => {
    void loadPageData();
  },
  { immediate: true },
);
</script>

<template>
  <div class="item-create-page period-opening-create-page">
    <FixedActionBreadcrumb
      :navs="navs"
      :active-key="activeNav"
      :show-actions="showActions"
      :primary-action-text="actionPrimaryText"
      :secondary-action-text="actionSecondaryText"
      :show-primary-action="showActions"
      :show-secondary-action="showApprovalActions"
      @back="handleBack"
      @save-draft="handleSecondaryAction"
      @save="handlePrimaryAction"
      @navigate="scrollToSection"
    />

    <section v-loading="loading || saving" class="panel form-panel">
      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础信息</h3>
        <el-form label-width="104px" class="item-create-form period-opening-form">
          <div class="item-form-grid period-opening-basic-grid">
            <el-form-item label="期初单号">
              <div class="readonly-field">{{ form.documentCode || '保存后生成' }}</div>
            </el-form-item>
            <el-form-item label="仓库" required>
              <el-select v-model="form.warehouseName" placeholder="请选择仓库" filterable :disabled="isReadonlyMode">
                <el-option
                  v-for="option in warehouseOptions"
                  :key="option.id"
                  :label="option.label"
                  :value="option.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="周期类型" required>
              <el-select v-model="form.periodType" placeholder="请选择周期类型" :disabled="isReadonlyMode">
                <el-option
                  v-for="option in periodTypeOptions"
                  :key="option.itemCode"
                  :label="option.itemLabel"
                  :value="option.itemCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="周期开始" required>
              <el-date-picker
                v-model="form.periodStartDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择周期开始"
                :disabled="isReadonlyMode"
              />
            </el-form-item>
            <el-form-item label="周期结束">
              <div class="readonly-field">{{ form.periodEndDate || '-' }}</div>
            </el-form-item>
            <el-form-item label="来源">
              <div class="readonly-field">{{ sourceTypeLabel }}</div>
            </el-form-item>
            <el-form-item label="状态">
              <div class="readonly-field">{{ statusLabelMap[detailStatus] ?? (detailStatus || '-') }}</div>
            </el-form-item>
            <el-form-item label="创建人">
              <div class="readonly-field">{{ form.creator || '-' }}</div>
            </el-form-item>
            <el-form-item label="创建时间">
              <div class="readonly-field">{{ form.createdAt || '-' }}</div>
            </el-form-item>
            <el-form-item label="审核人">
              <div class="readonly-field">{{ form.auditor || '-' }}</div>
            </el-form-item>
            <el-form-item label="审核时间">
              <div class="readonly-field">{{ form.approvedAt || '-' }}</div>
            </el-form-item>
            <el-form-item label="备注" class="period-opening-wide-item">
              <el-input v-model="form.remark" maxlength="200" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item v-if="form.rejectionReason" label="驳回原因" class="period-opening-wide-item">
              <el-input v-model="form.rejectionReason" readonly />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">期初明细</h3>
        <div class="table-toolbar">
          <el-button :disabled="isReadonlyMode" @click="openItemSelector(null)">
            <el-icon><Plus /></el-icon>
            选择物品
          </el-button>
        </div>

        <el-table :data="rows" border stripe class="erp-table period-opening-item-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="96" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" :disabled="isReadonlyMode" @click="addRow($index)">+</el-button>
              <el-button text :disabled="isReadonlyMode" @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="170">
            <template #default="{ row, $index }">
              <el-input
                :model-value="row.itemCode"
                readonly
                placeholder="点击选择物品"
                class="item-code-picker"
                :disabled="isReadonlyMode"
                @click="openItemSelector($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="130">
            <template #default="{ row }">{{ row.itemName || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="120">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="120">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column label="库存单位" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.unitName" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="期初数量" min-width="120">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.openingQty" :min="0" :precision="4" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="期初金额" min-width="120">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.openingAmount" :min="0" :precision="2" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="期初均价" min-width="110">
            <template #default="{ row }">{{ calcAvgCost(row) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.remark" maxlength="100" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <template #append>
            <div class="period-opening-summary-row">
              <span class="summary-title">合计</span>
              <span class="summary-cell">数量：{{ totalQuantity.toFixed(4) }}</span>
              <span class="summary-cell">金额：{{ totalAmount.toFixed(2) }}</span>
            </div>
          </template>
        </el-table>
      </div>
    </section>

    <CommonSelectorDialog
      v-model="itemSelectorVisible"
      title="选择物品"
      :tree-data="itemTreeData"
      :table-data="itemCandidateSource"
      :loading="itemSelectorLoading"
      :columns="itemTableColumns"
      row-key="id"
      selected-label-key="name"
      :selected-rows="selectedItemCandidates"
      :keyword-value="itemSelectorKeyword"
      :status-value="itemSelectorStatus"
      keyword-label="物品"
      keyword-placeholder="支持按物品编码和名称查询"
      status-label="启用状态"
      :status-options="normalizedItemStatusOptions"
      :total="itemSelectorTotal"
      :current-page="itemSelectorCurrentPage"
      :page-size="itemSelectorPageSize"
      @search="handleItemSelectorSearch"
      @node-change="handleItemNodeChange"
      @selection-change="handleItemSelectionChange"
      @clear-selection="handleItemClear"
      @page-change="(page) => { itemSelectorCurrentPage = page; void loadItemCandidates(); }"
      @page-size-change="(size) => { itemSelectorPageSize = size; itemSelectorCurrentPage = 1; void loadItemCandidates(); }"
      @confirm="handleItemSelectorConfirm"
    />
  </div>
</template>

<style scoped lang="scss">
.period-opening-basic-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.period-opening-wide-item {
  grid-column: span 2;
}

.readonly-field {
  display: flex;
  align-items: center;
  min-height: 22px;
  color: #334155;
  font-size: 11px;
  line-height: 1.2;
}

.period-opening-form :deep(.el-date-editor.el-input) {
  width: 100%;
}

.period-opening-item-table :deep(.common-number-input) {
  width: 100%;
}

.period-opening-item-table :deep(.el-input__wrapper),
.period-opening-item-table :deep(.el-select__wrapper) {
  min-height: 24px;
}

.period-opening-item-table :deep(.item-code-picker .el-input__wrapper) {
  cursor: pointer;
}

.period-opening-summary-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 32px;
  padding: 10px 16px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  color: #334155;
  font-size: 12px;
}

.summary-title {
  font-weight: 700;
}

.summary-cell {
  min-width: 120px;
  text-align: right;
}

@media (max-width: 1180px) {
  .period-opening-basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .period-opening-basic-grid {
    grid-template-columns: 1fr;
  }

  .period-opening-wide-item {
    grid-column: span 1;
  }
}
</style>
