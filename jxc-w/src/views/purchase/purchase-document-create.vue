<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonNumberInput from '@/components/CommonNumberInput.vue';
import CommonSelectorDialog, {
  type SelectorColumn,
  type SelectorTreeNode,
} from '@/components/CommonSelectorDialog.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useSessionStore } from '@/stores/session';
import {
  createPurchaseDocumentApi,
  fetchPurchaseDocumentDetailApi,
  updatePurchaseDocumentApi,
  type PurchaseDocument,
  type PurchaseDocumentSavePayload,
  type PurchaseDocumentType,
} from '@/api/modules/purchase';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type ManagedDocumentType = Extract<PurchaseDocumentType, 'applications' | 'orders' | 'receipts' | 'returns'>;
type FormMode = 'create' | 'edit' | 'view';
type PageConfig = {
  documentType: ManagedDocumentType;
  title: string;
  listPath: string;
  dateLabel: string;
  codeLabel: string;
  warehouseLabel: string;
  defaultBizType: string;
  requireSupplier: boolean;
  showReceivedQty: boolean;
  showReturnReason: boolean;
};
type ItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  supplier: string;
  warehouse: string;
  purchaseUnit: string;
  baseUnit: string;
  baseConversion: string;
  quantity: number | null;
  reviewQty: number | null;
  receivedQty: number | null;
  unitPrice: number | null;
  amount: number | null;
  gift: boolean;
  expectedArrivalDate: string;
  remark: string;
};
type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  purchaseUnit: string;
  baseUnit: string;
  status: string;
};

const PURCHASE_DOCUMENT_CONFIGS: Record<ManagedDocumentType, PageConfig> = {
  applications: {
    documentType: 'applications',
    title: '采购单申请',
    listPath: '/purchase/applications',
    dateLabel: '申请日期',
    codeLabel: '申请单号',
    warehouseLabel: '仓库',
    defaultBizType: '手工创建',
    requireSupplier: false,
    showReceivedQty: false,
    showReturnReason: false,
  },
  orders: {
    documentType: 'orders',
    title: '采购订单',
    listPath: '/purchase/orders',
    dateLabel: '订单日期',
    codeLabel: '订单号',
    warehouseLabel: '仓库',
    defaultBizType: '普通采购',
    requireSupplier: true,
    showReceivedQty: false,
    showReturnReason: false,
  },
  receipts: {
    documentType: 'receipts',
    title: '采购收货',
    listPath: '/purchase/receipts',
    dateLabel: '收货日期',
    codeLabel: '收货单号',
    warehouseLabel: '收货仓库',
    defaultBizType: '采购订单收货',
    requireSupplier: true,
    showReceivedQty: true,
    showReturnReason: false,
  },
  returns: {
    documentType: 'returns',
    title: '采购退货',
    listPath: '/purchase/returns',
    dateLabel: '退货日期',
    codeLabel: '退货单号',
    warehouseLabel: '退货仓库',
    defaultBizType: '采购退货',
    requireSupplier: true,
    showReceivedQty: false,
    showReturnReason: true,
  },
};

const ITEM_STATUS_DICT = 'item.status';
const DOCUMENT_PRINT_STATUS_DICT = 'document.print_status';

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();
const { optionsOf } = useDictionaryOptions([ITEM_STATUS_DICT, DOCUMENT_PRINT_STATUS_DICT]);
const itemStatusOptions = optionsOf(ITEM_STATUS_DICT, { enabled: true, label: '全部', value: '' });
const printStatusOptions = optionsOf(DOCUMENT_PRINT_STATUS_DICT);

const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);
const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '物品信息' },
];

const config = computed(() => {
  const documentType = route.meta.purchaseDocumentType as ManagedDocumentType | undefined;
  if (!documentType || !PURCHASE_DOCUMENT_CONFIGS[documentType]) {
    throw new Error('采购单据路由缺少 purchaseDocumentType');
  }
  return PURCHASE_DOCUMENT_CONFIGS[documentType];
});
const formMode = computed(() => {
  const mode = route.meta.purchaseDocumentMode as FormMode | undefined;
  if (mode !== 'create' && mode !== 'edit' && mode !== 'view') {
    throw new Error('采购单据路由缺少 purchaseDocumentMode');
  }
  return mode;
});
const documentId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const isCreateMode = computed(() => formMode.value === 'create');
const isViewMode = computed(() => formMode.value === 'view');
const isReadonlyMode = computed(() => isViewMode.value);
const actionPrimaryText = computed(() => (isCreateMode.value ? '保存' : '保存'));
const actionSecondaryText = computed(() => '保存草稿');
const orgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode) ?? undefined);

const formLoading = ref(false);
const saving = ref(false);
const itemSelectorVisible = ref(false);
const itemSelectorKeyword = ref('');
const itemSelectorStatus = ref('');
const activeItemTreeId = ref<string>('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingItemRowIndex = ref<number | null>(null);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);
const itemCandidateSource = ref<ItemCandidate[]>([]);
const rowSeed = ref(2);

const form = reactive({
  documentCode: '',
  documentDate: '',
  expectedArrivalDate: '',
  purchaseOrg: '',
  warehouse: '',
  supplier: '',
  sourceDocumentCode: '',
  downstreamDocumentCode: '',
  documentBizType: '',
  printStatus: '',
  returnReason: '',
  remark: '',
  rejectionReason: '',
});
const rows = ref<ItemRow[]>([]);

const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'category', label: '物品类别', minWidth: 120 },
  { prop: 'purchaseUnit', label: '采购单位', minWidth: 100 },
  { prop: 'status', label: '状态', minWidth: 80 },
];

const warehouseOptions = computed(() => {
  const result: Array<{ value: string; label: string }> = [];
  const walk = (nodes: WarehouseTreeNode[]) => {
    nodes.forEach((node) => {
      if (node.children?.length) {
        walk(node.children);
        return;
      }
      result.push({ value: node.value, label: node.label });
    });
  };
  walk(warehouseTree.value);
  return result;
});
const totalQuantity = computed(() => rows.value.reduce((sum, row) => sum + Number(row.quantity ?? 0), 0));
const totalAmount = computed(() => rows.value.reduce((sum, row) => {
  const amount = row.amount ?? Number(row.quantity ?? 0) * Number(row.unitPrice ?? 0);
  return sum + Number(amount ?? 0);
}, 0));
const normalizedItemStatusOptions = computed(() => itemStatusOptions.value.map((item) => ({
  label: item.itemLabel,
  value: item.itemCode,
})));
const normalizedPrintStatusOptions = computed(() => printStatusOptions.value.map((item) => ({
  label: item.itemLabel,
  value: item.itemCode,
})));

const createEmptyRow = (id: number): ItemRow => ({
  id,
  itemCode: '',
  itemName: '',
  spec: '',
  category: '',
  supplier: '',
  warehouse: form.warehouse,
  purchaseUnit: '',
  baseUnit: '',
  baseConversion: '',
  quantity: null,
  reviewQty: null,
  receivedQty: null,
  unitPrice: null,
  amount: null,
  gift: false,
  expectedArrivalDate: form.expectedArrivalDate,
  remark: '',
});

const resetForm = () => {
  form.documentCode = '';
  form.documentDate = new Date().toISOString().slice(0, 10);
  form.expectedArrivalDate = '';
  form.purchaseOrg = sessionStore.currentOrg?.name || '';
  form.warehouse = '';
  form.supplier = '';
  form.sourceDocumentCode = '';
  form.downstreamDocumentCode = '';
  form.documentBizType = config.value.defaultBizType;
  form.printStatus = normalizedPrintStatusOptions.value[0]?.value ?? '';
  form.returnReason = '';
  form.remark = '';
  form.rejectionReason = '';
  rowSeed.value = 2;
  rows.value = [createEmptyRow(1)];
};

const applyDetail = (detail: PurchaseDocument) => {
  form.documentCode = detail.documentCode ?? '';
  form.documentDate = detail.documentDate ?? '';
  form.expectedArrivalDate = detail.expectedArrivalDate === '-' ? '' : detail.expectedArrivalDate;
  form.purchaseOrg = detail.purchaseOrg === '-' ? '' : detail.purchaseOrg;
  form.warehouse = detail.warehouse === '-' ? '' : detail.warehouse;
  form.supplier = detail.supplier === '-' ? '' : detail.supplier;
  form.sourceDocumentCode = detail.sourceDocumentCode === '-' ? '' : detail.sourceDocumentCode;
  form.downstreamDocumentCode = detail.downstreamDocumentCode === '-' ? '' : detail.downstreamDocumentCode;
  form.documentBizType = detail.documentBizType || config.value.defaultBizType;
  form.printStatus = detail.printStatus || normalizedPrintStatusOptions.value[0]?.value || '';
  form.returnReason = detail.returnReason === '-' ? '' : detail.returnReason;
  form.remark = detail.remark === '-' ? '' : detail.remark;
  form.rejectionReason = detail.rejectionReason === '-' ? '' : detail.rejectionReason;
  rows.value = (detail.items?.length ? detail.items : [null]).map((item, index) => {
    if (!item) {
      return createEmptyRow(index + 1);
    }
    return {
      id: index + 1,
      itemCode: item.itemCode,
      itemName: item.itemName,
      spec: item.spec,
      category: item.itemCategory,
      supplier: item.supplier || detail.supplier,
      warehouse: item.warehouse || detail.warehouse,
      purchaseUnit: item.purchaseUnit,
      baseUnit: item.baseUnit,
      baseConversion: item.baseConversion,
      quantity: Number(item.quantity ?? 0),
      reviewQty: Number(item.reviewQty ?? item.quantity ?? 0),
      receivedQty: Number(item.receivedQty ?? 0),
      unitPrice: Number(item.unitPrice ?? 0),
      amount: Number(item.amount ?? 0),
      gift: Boolean(item.isGift),
      expectedArrivalDate: item.expectedArrivalDate || detail.expectedArrivalDate,
      remark: item.remark,
    };
  });
  rowSeed.value = rows.value.length + 1;
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
  purchaseUnit: row.purchaseUnit || row.baseUnit,
  baseUnit: row.baseUnit,
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

const loadDetail = async () => {
  if (isCreateMode.value) {
    return;
  }
  if (documentId.value == null) {
    resetForm();
    return;
  }
  formLoading.value = true;
  try {
    const detail = await fetchPurchaseDocumentDetailApi(config.value.documentType, documentId.value, orgId.value);
    applyDetail(detail);
  } finally {
    formLoading.value = false;
  }
};

const loadPageData = async () => {
  resetForm();
  await Promise.all([
    loadWarehouseTree(),
    loadSupplierOptions(),
    loadItemTree(),
  ]);
  await loadDetail();
};

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'items' ? itemSectionRef.value : basicSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleBack = () => {
  router.push(config.value.listPath);
};

const handleSupplierChange = () => {
  rows.value.forEach((row) => {
    if (!row.supplier) {
      row.supplier = form.supplier;
    }
  });
};

const handleWarehouseChange = () => {
  rows.value.forEach((row) => {
    if (!row.warehouse) {
      row.warehouse = form.warehouse;
    }
  });
};

const handleExpectedArrivalDateChange = () => {
  rows.value.forEach((row) => {
    if (!row.expectedArrivalDate) {
      row.expectedArrivalDate = form.expectedArrivalDate;
    }
  });
};

const addRow = (index: number) => {
  rows.value.splice(index + 1, 0, createEmptyRow(rowSeed.value++));
};

const removeRow = (index: number) => {
  if (rows.value.length === 1) {
    rows.value = [createEmptyRow(rowSeed.value++)];
    return;
  }
  rows.value.splice(index, 1);
};

const openItemSelector = (index: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  selectingItemRowIndex.value = index;
  selectedItemCandidates.value = [];
  itemSelectorVisible.value = true;
  void loadItemCandidates();
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

const applyCandidateToRow = (target: ItemRow, item: ItemCandidate) => {
  target.itemCode = item.code;
  target.itemName = item.name;
  target.spec = item.spec;
  target.category = item.category;
  target.purchaseUnit = item.purchaseUnit;
  target.baseUnit = item.baseUnit;
  target.baseConversion = item.purchaseUnit && item.baseUnit && item.purchaseUnit !== item.baseUnit
    ? `1${item.purchaseUnit}=1${item.baseUnit}`
    : '';
  target.supplier = target.supplier || form.supplier;
  target.warehouse = target.warehouse || form.warehouse;
  target.expectedArrivalDate = target.expectedArrivalDate || form.expectedArrivalDate;
};

const handleItemSelectorConfirm = (selectedRows: Array<Record<string, unknown>>) => {
  if (selectingItemRowIndex.value == null || selectedRows.length === 0) {
    itemSelectorVisible.value = false;
    return;
  }
  const candidates = selectedRows as unknown as ItemCandidate[];
  const startIndex = selectingItemRowIndex.value;
  candidates.forEach((item, offset) => {
    const rowIndex = startIndex + offset;
    if (!rows.value[rowIndex]) {
      rows.value.splice(rowIndex, 0, createEmptyRow(rowSeed.value++));
    }
    applyCandidateToRow(rows.value[rowIndex], item);
  });
  itemSelectorVisible.value = false;
};

const validateForm = () => {
  if (!form.documentDate) {
    ElMessage.warning(`请选择${config.value.dateLabel}`);
    return false;
  }
  if (!form.warehouse) {
    ElMessage.warning(`请选择${config.value.warehouseLabel}`);
    return false;
  }
  if (config.value.requireSupplier && !form.supplier) {
    ElMessage.warning('请选择供应商');
    return false;
  }
  const invalidRow = rows.value.find((row) => !row.itemCode || !row.itemName || Number(row.quantity ?? 0) <= 0);
  if (invalidRow) {
    ElMessage.warning('请完整填写物品明细，数量必须大于 0');
    return false;
  }
  return true;
};

const buildPayload = (): PurchaseDocumentSavePayload => ({
  documentDate: form.documentDate,
  expectedArrivalDate: form.expectedArrivalDate,
  purchaseOrg: form.purchaseOrg,
  warehouse: form.warehouse,
  supplier: form.supplier,
  sourceDocumentCode: form.sourceDocumentCode,
  downstreamDocumentCode: form.downstreamDocumentCode,
  documentBizType: form.documentBizType || config.value.defaultBizType,
  receiveStatus: config.value.showReceivedQty ? '未收货' : undefined,
  shipStatus: config.value.showReceivedQty ? '未发货' : undefined,
  printStatus: form.printStatus,
  returnReason: form.returnReason,
  applicant: sessionStore.userName || sessionStore.currentOrg?.name || '',
  submitter: sessionStore.userName || '',
  remark: form.remark,
  items: rows.value.map((row) => {
    const quantity = Number(row.quantity ?? 0);
    const unitPrice = Number(row.unitPrice ?? 0);
    return {
      itemCode: row.itemCode,
      itemName: row.itemName,
      spec: row.spec,
      itemCategory: row.category,
      supplier: row.supplier || form.supplier,
      purchaseUnit: row.purchaseUnit,
      baseUnit: row.baseUnit,
      baseConversion: row.baseConversion,
      quantity,
      reviewQty: Number(row.reviewQty ?? quantity),
      receivedQty: Number(row.receivedQty ?? 0),
      unitPrice,
      amount: Number(row.amount ?? quantity * unitPrice),
      isGift: row.gift,
      warehouse: row.warehouse || form.warehouse,
      expectedArrivalDate: row.expectedArrivalDate || form.expectedArrivalDate,
      remark: row.remark,
    };
  }),
});

const saveDocument = async () => {
  if (isReadonlyMode.value || saving.value || !validateForm()) {
    return;
  }
  saving.value = true;
  try {
    const payload = buildPayload();
    if (isCreateMode.value) {
      await createPurchaseDocumentApi(config.value.documentType, payload, orgId.value);
      ElMessage.success(`${config.value.title}已新增`);
    } else if (documentId.value != null) {
      await updatePurchaseDocumentApi(config.value.documentType, documentId.value, payload, orgId.value);
      ElMessage.success(`${config.value.title}已更新`);
    }
    router.push(config.value.listPath);
  } finally {
    saving.value = false;
  }
};

watch(
  () => [route.name, route.params.id, sessionStore.currentOrgId, sessionStore.platformAdminMode],
  () => {
    loadPageData().catch(() => {
      // Global error message handled in http interceptor.
    });
  },
  { immediate: true },
);

watch(
  normalizedPrintStatusOptions,
  (options) => {
    if (!form.printStatus && options.length) {
      form.printStatus = options[0].value;
    }
  },
);
</script>

<template>
  <div v-loading="formLoading" class="item-create-page">
    <FixedActionBreadcrumb
      :navs="navs"
      :active-key="activeNav"
      :show-actions="!isReadonlyMode"
      :primary-action-text="actionPrimaryText"
      :secondary-action-text="actionSecondaryText"
      :show-secondary-action="isCreateMode"
      @back="handleBack"
      @save-draft="saveDocument"
      @save="saveDocument"
      @navigate="scrollToSection"
    />

    <section class="panel form-panel">
      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础信息</h3>
        <el-form label-width="96px" class="item-create-form purchase-document-create-form">
          <div class="item-form-grid purchase-document-basic-grid">
            <el-form-item :label="config.codeLabel">
              <div class="readonly-field">{{ form.documentCode || '保存后生成' }}</div>
            </el-form-item>
            <el-form-item label="采购组织">
              <el-input v-model="form.purchaseOrg" placeholder="请输入采购组织" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item :label="config.dateLabel">
              <el-date-picker v-model="form.documentDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width: 100%" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item label="期望到货">
              <el-date-picker v-model="form.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width: 100%" :disabled="isReadonlyMode" @change="handleExpectedArrivalDateChange" />
            </el-form-item>
            <el-form-item :label="config.warehouseLabel">
              <el-select v-model="form.warehouse" placeholder="请选择仓库" style="width: 100%" :disabled="isReadonlyMode" @change="handleWarehouseChange">
                <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="供应商">
              <el-select v-model="form.supplier" placeholder="请选择供应商" style="width: 100%" :disabled="isReadonlyMode" :loading="supplierLoading" clearable filterable @change="handleSupplierChange">
                <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="来源单号">
              <el-input v-model="form.sourceDocumentCode" placeholder="请输入来源单号" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item label="下游单号">
              <el-input v-model="form.downstreamDocumentCode" placeholder="请输入下游单号" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item label="单据类型">
              <el-input v-model="form.documentBizType" placeholder="请输入单据类型" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item label="打印状态">
              <el-select v-model="form.printStatus" placeholder="请选择打印状态" style="width: 100%" :disabled="isReadonlyMode">
                <el-option v-for="option in normalizedPrintStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="config.showReturnReason" label="退货原因">
              <el-input v-model="form.returnReason" placeholder="请输入退货原因" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item label="备注" class="purchase-document-remark-item">
              <el-input v-model="form.remark" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item v-if="form.rejectionReason" label="拒审原因" class="purchase-document-remark-item">
              <el-input v-model="form.rejectionReason" readonly />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">物品信息</h3>
        <el-table :data="rows" border stripe class="erp-table purchase-document-item-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="96" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" :disabled="isReadonlyMode" @click="addRow($index)">+</el-button>
              <el-button text :disabled="isReadonlyMode" @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="180">
            <template #default="{ row, $index }">
              <el-input :model-value="row.itemCode" placeholder="点击选择物品" readonly :disabled="isReadonlyMode" class="item-code-picker" @click="openItemSelector($index)" />
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="130">
            <template #default="{ row }">{{ row.itemName || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="120">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="110">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column label="供应商" min-width="160">
            <template #default="{ row }">
              <el-select v-model="row.supplier" placeholder="请选择供应商" :disabled="isReadonlyMode" filterable clearable>
                <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="仓库" min-width="140">
            <template #default="{ row }">
              <el-select v-model="row.warehouse" placeholder="请选择仓库" :disabled="isReadonlyMode">
                <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="采购单位" min-width="110">
            <template #default="{ row }">{{ row.purchaseUnit || '-' }}</template>
          </el-table-column>
          <el-table-column label="数量" min-width="110">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.quantity" :min="0" :precision="4" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column v-if="config.documentType === 'applications'" label="审核数量" min-width="110">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.reviewQty" :min="0" :precision="4" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column v-if="config.showReceivedQty" label="收货数量" min-width="110">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.receivedQty" :min="0" :precision="4" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="采购单价" min-width="120">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.unitPrice" :min="0" :precision="4" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="金额" min-width="120">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.amount" :min="0" :precision="2" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="是否赠品" min-width="96">
            <template #default="{ row }">
              <el-switch v-model="row.gift" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="期望到货" min-width="150">
            <template #default="{ row }">
              <el-date-picker v-model="row.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" :disabled="isReadonlyMode" style="width: 136px" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <template #append>
            <div class="purchase-document-summary-row">
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
      keyword-placeholder="支持按物品编码和名称查询..."
      status-label="启用状态"
      :status-options="normalizedItemStatusOptions"
      :total="itemSelectorTotal"
      :current-page="itemSelectorCurrentPage"
      :page-size="itemSelectorPageSize"
      @search="handleItemSelectorSearch"
      @node-change="handleItemNodeChange"
      @selection-change="handleItemSelectionChange"
      @clear-selection="handleItemClear"
      @page-change="(p) => { itemSelectorCurrentPage = p; loadItemCandidates(); }"
      @page-size-change="(s) => { itemSelectorPageSize = s; itemSelectorCurrentPage = 1; loadItemCandidates(); }"
      @confirm="handleItemSelectorConfirm"
    />
  </div>
</template>

<style scoped lang="scss">
.purchase-document-basic-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.purchase-document-remark-item {
  grid-column: span 2;
}

.readonly-field {
  display: flex;
  align-items: center;
  min-height: 22px;
  color: #334155;
  font-size: 11px;
  line-height: 1;
}

.purchase-document-create-form :deep(.el-input__wrapper),
.purchase-document-create-form :deep(.el-select__wrapper),
.purchase-document-create-form :deep(.el-textarea__inner) {
  min-height: 22px;
}

.purchase-document-create-form :deep(.el-form-item__label) {
  font-size: 11px;
}

.purchase-document-create-form :deep(.el-input__inner),
.purchase-document-create-form :deep(.el-select__selected-item),
.purchase-document-create-form :deep(.el-date-editor .el-input__inner) {
  font-size: 11px;
}

.purchase-document-item-table :deep(.common-number-input) {
  width: 100%;
}

.purchase-document-item-table :deep(.el-input__wrapper),
.purchase-document-item-table :deep(.el-select__wrapper) {
  min-height: 24px;
}

.purchase-document-item-table :deep(.item-code-picker .el-input__wrapper) {
  cursor: pointer;
}

.purchase-document-summary-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 32px;
  padding: 10px 16px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-size: 12px;
  color: #334155;
}

.summary-title {
  margin-right: auto;
  color: #0f172a;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .purchase-document-basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .purchase-document-basic-grid {
    grid-template-columns: 1fr;
  }

  .purchase-document-remark-item {
    grid-column: auto;
  }
}
</style>
