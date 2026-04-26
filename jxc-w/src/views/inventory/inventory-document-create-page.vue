<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type UploadFile, type UploadUserFile } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import CommonFormSection from '@/components/CommonFormSection.vue';
import CommonNumberInput from '@/components/CommonNumberInput.vue';
import CommonPageNotice from '@/components/CommonPageNotice.vue';
import CommonSelectorDialog, { type SelectorColumn, type SelectorTreeNode } from '@/components/CommonSelectorDialog.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import {
  batchApproveGenericInventoryDocumentApi,
  batchUnapproveGenericInventoryDocumentApi,
  createGenericInventoryDocumentApi,
  fetchGenericInventoryDocumentDetailApi,
  fetchGenericInventoryDocumentPermissionApi,
  updateGenericInventoryDocumentApi,
  type GenericInventoryDocumentLinePayload,
  type GenericInventoryDocumentSavePayload,
} from '@/api/modules/inventory';
import { fetchDishesApi, type DishListRow } from '@/api/modules/dish';
import { fetchItemCategoryTreeApi, fetchItemDetailApi, fetchItemsApi, type ItemCategoryTreeNode, type ItemCreatePayload, type ItemVO } from '@/api/modules/item';
import { fetchCurrentUserRolesApi } from '@/api/modules/auth';
import { fetchStoreSalesmenApi, type SalesmanCandidateItem } from '@/api/modules/system-admin';
import { fetchStoreWarehousesApi, type WarehouseRow, type WarehouseType } from '@/api/modules/warehouse';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import type { InventoryDocumentMeta } from '@/views/inventory/document-meta';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useRequiredOrgScope } from '@/composables/useRequiredOrgScope';

const props = defineProps<{
  meta: InventoryDocumentMeta;
}>();

type WarehouseOption = {
  id: number;
  name: string;
  label: string;
  warehouseType: WarehouseType;
};

type SalesmanOption = {
  userId: number;
  realName: string;
  phone: string;
  label: string;
};

type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  stockUnit: string;
  baseUnit: string;
  status: string;
};

type ItemUnitOption = {
  label: string;
  value: string;
  rate: number | null;
};

type DocumentItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  unitName: string;
  unitOptions: ItemUnitOption[];
  unitRate: number | null;
  baseUnit: string;
  baseUnitQuantity: number | null;
  availableQty: number | null;
  quantity: number | null;
  unitPrice: number | null;
  amount: number | null;
  lineReason: string;
  dishId: string;
  dishName: string;
  damageReason: string;
  remark: string;
};

type DishOption = {
  value: string;
  label: string;
  dishName: string;
};

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const { orgId: currentOrgId, storeId } = useRequiredOrgScope();
const ITEM_STATUS_DICT = 'item.status';
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const { optionsOf } = useDictionaryOptions([ITEM_STATUS_DICT, INVENTORY_DOCUMENT_STATUS_DICT]);
const itemStatusOptions = optionsOf(ITEM_STATUS_DICT, { enabled: true, label: '全部', value: '' });
const inventoryDocumentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);
const normalizedItemStatusOptions = computed(() => itemStatusOptions.value.map((item) => ({
  label: item.itemLabel,
  value: item.itemCode,
})));
const submittedStatus = computed(() => (
  inventoryDocumentStatusOptions.value.find((item) => item.itemKey === 'SUBMITTED')?.itemCode ?? '已提交'
));
const approvedStatus = computed(() => (
  inventoryDocumentStatusOptions.value.find((item) => item.itemKey === 'APPROVED')?.itemCode ?? '已审核'
));
const saving = ref(false);
const loading = ref(false);
const canCreate = ref(false);
const canUpdate = ref(false);
const canApprove = ref(false);
const canUnapprove = ref(false);
const detailStatus = ref('');
const activeNav = ref('basic');
const warehouses = ref<WarehouseOption[]>([]);
const salesmen = ref<SalesmanOption[]>([]);
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
const rowSeed = ref(1);
const dishOptions = ref<DishOption[]>([]);
const {
  supplierOptions,
  loadSupplierOptions,
} = useSupplierArchiveOptions();
const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '物品信息' },
];
const counterpartySupplierSelect = computed(() =>
  props.meta.counterpartyField?.kind === 'select'
    && props.meta.counterpartyField.label === '供应商',
);
const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'category', label: '物品类别', minWidth: 120 },
  { prop: 'stockUnit', label: '库存单位', minWidth: 100 },
  { prop: 'status', label: '状态', minWidth: 80 },
];

const documentId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});

const isCreateMode = computed(() => route.name === props.meta.createRouteName);
const isViewMode = computed(() => route.name === props.meta.viewRouteName);
const isEditMode = computed(() => route.name === props.meta.editRouteName);
const isApprovalMode = computed(() => String(route.query.approvalMode ?? '').trim() === '1' && documentId.value != null);
const showApprovalActions = computed(() =>
  isApprovalMode.value
    && detailStatus.value === submittedStatus.value
    && (canApprove.value || canUnapprove.value),
);
const isReadonlyMode = computed(() => {
  if (isApprovalMode.value) {
    return true;
  }
  if (isViewMode.value || detailStatus.value === approvedStatus.value) {
    return true;
  }
  if (isCreateMode.value) {
    return !canCreate.value;
  }
  if (isEditMode.value) {
    return !canUpdate.value;
  }
  return true;
});
const showDocumentCode = computed(() => props.meta.showDocumentCode !== false);
const showUpstreamCode = computed(() => props.meta.showUpstreamCode === true);
const usePurchaseInboundItemTableStyle = computed(() => props.meta.itemTableStyle === 'purchase-inbound');
const usePurchaseReturnOutboundItemTableStyle = computed(() => props.meta.itemTableStyle === 'purchase-return-outbound');
const isDepartmentPicking = computed(() => props.meta.type === 'department-picking');
const isProductionInbound = computed(() => props.meta.type === 'production-inbound');
const isPurchaseReturnOutbound = computed(() => props.meta.type === 'purchase-return-outbound');
const itemCodeColumnLabel = computed(() => (isProductionInbound.value ? '加工品编码' : '物品编码'));
const itemNameColumnLabel = computed(() => (isProductionInbound.value ? '加工品名称' : '物品名称'));
const itemCategoryColumnLabel = computed(() => (isProductionInbound.value ? '加工品类别' : '物品类别'));
const unitColumnLabel = computed(() => {
  if (usePurchaseReturnOutboundItemTableStyle.value) {
    return '采购单位';
  }
  return (isWarehouseOpeningBalance.value || isDepartmentPicking.value || isProductionInbound.value) ? '库存单位' : '单位';
});
const availableQtyColumnLabel = computed(() => (
  usePurchaseReturnOutboundItemTableStyle.value || isDepartmentPicking.value ? '可出库量' : '可用数量'
));
const quantityColumnLabel = computed(() => {
  if (isWarehouseOpeningBalance.value) {
    return '入库数量';
  }
  if (isDepartmentPicking.value) {
    return '领料数量';
  }
  if (isProductionInbound.value) {
    return '入库数量';
  }
  return '数量';
});
const showUnitRateColumn = computed(() => isWarehouseOpeningBalance.value || isDepartmentPicking.value);
const unitPriceColumnLabel = computed(() => (isProductionInbound.value ? '入库单价' : '单价'));
const showReasonColumn = computed(() =>
  !isWarehouseOpeningBalance.value
    && !usePurchaseReturnOutboundItemTableStyle.value
    && !isDepartmentPicking.value
    && !isProductionInbound.value,
);
const showDishColumn = computed(() =>
  ['customer-sales-outbound', 'dish-consumption-outbound', 'damage-outbound'].includes(props.meta.type),
);
const showDamageReasonColumn = computed(() => props.meta.type === 'damage-outbound');
const showAttachment = computed(() => props.meta.showAttachment === true);
const isWarehouseOpeningBalance = computed(() => false);
const totalQuantity = computed(() => rows.value.reduce((sum, row) => sum + Number(row.quantity ?? 0), 0));
const totalAmount = computed(() => rows.value.reduce((sum, row) => sum + Number(row.amount ?? 0), 0));
const totalBaseUnitQuantity = computed(() => rows.value.reduce((sum, row) => sum + Number(row.baseUnitQuantity ?? 0), 0));
const warehouseOpeningBalanceViewVisible = computed(() => isWarehouseOpeningBalance.value && isViewMode.value);
const itemTableHeight = computed(() => {
  const headerHeight = 20;
  const rowHeight = 20;
  const summaryHeight = isWarehouseOpeningBalance.value ? 20 : 0;
  return Math.max(200, headerHeight + rows.value.length * rowHeight + summaryHeight);
});
const actionPrimaryText = computed(() => (showApprovalActions.value ? '审核通过' : '保存'));
const actionSecondaryText = computed(() => (showApprovalActions.value ? '审核不通过' : '保存草稿'));

const form = reactive({
  documentCode: '',
  documentDate: '',
  primaryName: '',
  secondaryName: '',
  counterpartyName: '',
  counterpartyName2: '',
  reason: '',
  upstreamCode: '',
  salesmanUserId: undefined as number | undefined,
  salesmanName: '',
  remark: '',
  attachmentName: '',
  attachmentFiles: [] as UploadUserFile[],
  extraFields: {} as Record<string, string>,
  creator: '',
  createdAt: '',
  auditor: '',
  auditedAt: '',
});

const rows = ref<DocumentItemRow[]>([]);

const presetWarehouseId = computed(() => {
  const raw = route.query.warehouseId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const presetWarehouseName = computed(() => {
  const raw = route.query.warehouseName;
  const value = Array.isArray(raw) ? raw[0] : raw;
  return typeof value === 'string' ? value : '';
});

const initExtraFields = () => {
  const target: Record<string, string> = {};
  props.meta.extraFields?.forEach((field) => {
    target[field.key] = '';
  });
  form.extraFields = target;
};

const createEmptyRow = (): DocumentItemRow => ({
  id: rowSeed.value++,
  itemCode: '',
  itemName: '',
  spec: '',
  category: '',
  unitName: '',
  unitOptions: [],
  unitRate: null,
  baseUnit: '',
  baseUnitQuantity: null,
  availableQty: null,
  quantity: null,
  unitPrice: null,
  amount: null,
  lineReason: '',
  dishId: '',
  dishName: '',
  damageReason: '',
  remark: '',
});

const syncWarehouseOpeningBalanceQuantities = (row: DocumentItemRow) => {
  if (row.quantity == null || row.unitRate == null) {
    row.baseUnitQuantity = null;
    return;
  }
  const quantity = Number(row.quantity);
  const unitRate = Number(row.unitRate);
  row.baseUnitQuantity = Number.isFinite(quantity * unitRate)
    ? Number((quantity * unitRate).toFixed(4))
    : null;
};

const syncRowAmount = (row: DocumentItemRow) => {
  const quantity = Number(row.quantity ?? 0);
  const unitPrice = Number(row.unitPrice ?? 0);
  row.amount = Number.isFinite(quantity * unitPrice) ? Number((quantity * unitPrice).toFixed(2)) : 0;
};

const formatUnitRateValue = (value: number | null) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return '';
  }
  return Number(value).toFixed(4).replace(/\.?0+$/, '');
};

const formatWarehouseOpeningBalanceUnitRateText = (row: DocumentItemRow) => {
  if (!row.unitName || !row.baseUnit || row.unitRate == null) {
    return '-';
  }
  return `1${row.unitName}=${formatUnitRateValue(row.unitRate)}${row.baseUnit}`;
};

const formatWarehouseOpeningBalanceDate = (value: string) => {
  if (!value) {
    return '';
  }
  return value.replace(/-/g, '/');
};

const formatWarehouseOpeningBalanceDateTime = (value: string) => {
  if (!value) {
    return '';
  }
  return value.replace(/^(\d{4})-(\d{2})-(\d{2})/, '$1/$2/$3');
};

const warehouseOpeningBalanceStatusText = computed(() => (
  detailStatus.value === approvedStatus.value ? '已完成' : detailStatus.value
));

const getItemTableSummaries = ({ columns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let summaryLabelFilled = false;
  return columns.map((column) => {
    if (!summaryLabelFilled && column.type !== 'selection') {
      summaryLabelFilled = true;
      return '合计';
    }
    const property = column.property;
    if (property === 'quantity') {
      return totalQuantity.value.toFixed(4);
    }
    if (property === 'amount') {
      return totalAmount.value.toFixed(2);
    }
    if (property === 'baseUnitQuantity' && isWarehouseOpeningBalance.value) {
      return totalBaseUnitQuantity.value.toFixed(4);
    }
    return '';
  });
};

const addRow = (index: number) => {
  const targetIndex = Number.isInteger(index) ? index + 1 : rows.value.length;
  rows.value.splice(targetIndex, 0, createEmptyRow());
};

const removeRow = (index: number) => {
  if (rows.value.length <= 1) {
    rows.value = [createEmptyRow()];
    return;
  }
  rows.value.splice(index, 1);
};

const normalizeItemTreeNodes = (nodes: ItemCategoryTreeNode[]): SelectorTreeNode[] => nodes.map((node) => ({
  id: String(node.label ?? 'all'),
  label: String(node.label ?? ''),
  children: Array.isArray(node.children) ? normalizeItemTreeNodes(node.children) : undefined,
}));

const loadItemTree = async () => {
  if (!currentOrgId.value) {
    itemTreeData.value = [{ id: 'all', label: '全部' }];
    return;
  }
  const tree = await fetchItemCategoryTreeApi(currentOrgId.value);
  if (!Array.isArray(tree) || !tree.length) {
    itemTreeData.value = [{ id: 'all', label: '全部' }];
    return;
  }
  itemTreeData.value = [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }];
};

const mapItemCandidate = (row: ItemVO): ItemCandidate => ({
  id: row.id || row.code,
  code: row.code,
  name: row.name,
  spec: row.spec,
  category: row.category,
  stockUnit: row.stockUnit,
  baseUnit: row.baseUnit,
  status: row.status,
});

const loadItemCandidates = async () => {
  if (!currentOrgId.value) {
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
    }, currentOrgId.value);
    itemCandidateSource.value = Array.isArray(page.list) ? page.list.map(mapItemCandidate) : [];
    itemSelectorTotal.value = Number(page.total ?? 0);
  } finally {
    itemSelectorLoading.value = false;
  }
};

const openItemSelector = async (index: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  if (isPurchaseReturnOutbound.value) {
    if (!String(form.counterpartyName ?? '').trim() || !String(form.primaryName ?? '').trim()) {
      ElMessage.warning('请先选择供应商和仓库');
      return;
    }
  }
  selectingItemRowIndex.value = index;
  selectedItemCandidates.value = [];
  if (!itemTreeData.value.length) {
    await loadItemTree();
  }
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

const buildUnitOptionsFromItemDetail = (detail: ItemCreatePayload, fallbackUnit = '') => {
  const baseUnit = detail.unitSettingRows?.[0]?.unit?.trim() || fallbackUnit;
  const unitOptions = (detail.unitSettingRows ?? [])
    .map((row, index) => {
      const unit = row.unit?.trim() || '';
      if (!unit) {
        return null;
      }
      if (index === 0 || unit === baseUnit) {
        return {
          label: unit,
          value: unit,
          rate: 1,
        } satisfies ItemUnitOption;
      }
      const convertFrom = Number(row.convertFrom ?? 0);
      const convertTo = Number(row.convertTo ?? 0);
      return {
        label: unit,
        value: unit,
        rate: convertFrom > 0 && convertTo > 0 ? Number((convertTo / convertFrom).toFixed(4)) : null,
      } satisfies ItemUnitOption;
    })
    .filter((option): option is ItemUnitOption => Boolean(option));
  return {
    baseUnit,
    unitOptions,
  };
};

const parseUnitOptions = (raw: string | undefined, currentUnitName = '') => {
  if (!raw) {
    return currentUnitName ? [{ label: currentUnitName, value: currentUnitName, rate: null }] : [];
  }
  try {
    const parsed = JSON.parse(raw) as Array<{ label?: string; value?: string; rate?: number | null }>;
    const options = Array.isArray(parsed)
      ? parsed
        .map((option) => {
          const value = String(option?.value ?? '').trim();
          if (!value) {
            return null;
          }
          return {
            label: String(option?.label ?? value),
            value,
            rate: typeof option?.rate === 'number' ? option.rate : null,
          } satisfies ItemUnitOption;
        })
        .filter((option): option is ItemUnitOption => Boolean(option))
      : [];
    if (options.length) {
      return options;
    }
  } catch {
    // Ignore invalid stored data and fall back to current value.
  }
  return currentUnitName ? [{ label: currentUnitName, value: currentUnitName, rate: null }] : [];
};

const resolveUnitRateFromOptions = (unitOptions: ItemUnitOption[], unitName: string) => (
  unitOptions.find((option) => option.value === unitName)?.rate ?? null
);

const resetRowUnitMeta = (row: DocumentItemRow) => {
  row.unitName = '';
  row.unitOptions = [];
  row.unitRate = null;
  row.baseUnit = '';
  row.baseUnitQuantity = null;
};

const updateWarehouseOpeningBalanceUnit = (row: DocumentItemRow, unitName: string) => {
  row.unitName = unitName;
  row.unitRate = resolveUnitRateFromOptions(row.unitOptions, unitName);
  syncWarehouseOpeningBalanceQuantities(row);
};

const handleWarehouseOpeningBalanceUnitChange = (
  row: DocumentItemRow,
  value: string | number | boolean | undefined,
) => {
  updateWarehouseOpeningBalanceUnit(row, String(value ?? ''));
};

const resolveItemUnitMeta = async (item: ItemCandidate) => {
  const fallbackBaseUnit = item.baseUnit || item.stockUnit || '';
  const fallbackOptions = [item.stockUnit || fallbackBaseUnit]
    .filter(Boolean)
    .map((unit) => ({
      label: unit,
      value: unit,
      rate: unit === fallbackBaseUnit ? 1 : null,
    }));
  if (!currentOrgId.value) {
    return {
      baseUnit: fallbackBaseUnit,
      unitOptions: fallbackOptions,
      unitName: item.stockUnit || fallbackBaseUnit,
    };
  }
  try {
    const detail = await fetchItemDetailApi(item.id, currentOrgId.value);
    const resolved = buildUnitOptionsFromItemDetail(detail, fallbackBaseUnit);
    const preferredUnitName = detail.defaultStockUnit?.trim()
      || item.stockUnit
      || resolved.unitOptions[0]?.value
      || fallbackBaseUnit;
    const unitName = resolved.unitOptions.some((option) => option.value === preferredUnitName)
      ? preferredUnitName
      : (resolved.unitOptions[0]?.value || preferredUnitName);
    return {
      baseUnit: resolved.baseUnit,
      unitOptions: resolved.unitOptions.length ? resolved.unitOptions : fallbackOptions,
      unitName,
    };
  } catch {
    return {
      baseUnit: fallbackBaseUnit,
      unitOptions: fallbackOptions,
      unitName: item.stockUnit || fallbackBaseUnit,
    };
  }
};

const loadDishOptions = async () => {
  if (!showDishColumn.value || !currentOrgId.value) {
    dishOptions.value = [];
    return;
  }
  const page = await fetchDishesApi({ pageNo: 1, pageSize: 500, deleted: 'N' }, currentOrgId.value);
  dishOptions.value = (page.list ?? []).map((row: DishListRow) => ({
    value: row.dishId,
    label: `${row.spuCode} / ${row.dishName}`,
    dishName: row.dishName,
  }));
};

const handleDishChange = (row: DocumentItemRow) => {
  const matched = dishOptions.value.find((item) => item.value === row.dishId);
  row.dishName = matched?.dishName ?? '';
};

const applyItemToRow = async (row: DocumentItemRow, item: ItemCandidate) => {
  row.itemCode = item.code;
  row.itemName = item.name;
  row.spec = item.spec;
  row.category = item.category;
  resetRowUnitMeta(row);
  if (isWarehouseOpeningBalance.value || isDepartmentPicking.value) {
    const unitMeta = await resolveItemUnitMeta(item);
    row.baseUnit = unitMeta.baseUnit;
    row.unitOptions = unitMeta.unitOptions;
    updateWarehouseOpeningBalanceUnit(row, unitMeta.unitName);
    return;
  }
  row.unitName = item.stockUnit || '';
};

const handleQuantityValueUpdate = (row: DocumentItemRow, value: number | null) => {
  row.quantity = value;
  if (isWarehouseOpeningBalance.value || isDepartmentPicking.value) {
    syncWarehouseOpeningBalanceQuantities(row);
    return;
  }
  syncRowAmount(row);
};

const handleItemSelectorConfirm = async (selectedRows: Array<Record<string, unknown>>) => {
  const picked = selectedRows as ItemCandidate[];
  if (!picked.length) {
    ElMessage.warning('请至少选择一个物品');
    return;
  }
  if (picked.length > 1) {
    ElMessage.warning('当前仅支持选择一个物品');
    return;
  }
  const targetIndex = selectingItemRowIndex.value ?? 0;
  const targetRow = rows.value[targetIndex];
  if (!targetRow) {
    ElMessage.warning('未找到目标行，请重试');
    return;
  }
  await applyItemToRow(targetRow, picked[0]);
  itemSelectorVisible.value = false;
};

const loadWarehouses = async () => {
  if (!storeId.value || (!props.meta.primaryField && !props.meta.secondaryField)) {
    warehouses.value = [];
    return;
  }
  try {
    const result = await fetchStoreWarehousesApi(storeId.value, { status: 'ENABLED' });
    warehouses.value = result.map((item: WarehouseRow) => ({
      id: item.id,
      name: item.warehouseName,
      label: `${item.warehouseName}（${item.warehouseCode}）`,
      warehouseType: item.warehouseType,
    }));
  } catch {
    warehouses.value = [];
    ElMessage.error('仓库列表加载失败');
  }
};

const getWarehouseOptions = (field?: InventoryDocumentMeta['primaryField']) => {
  if (!field) {
    return [];
  }
  if (!field.warehouseTypes?.length) {
    return warehouses.value;
  }
  return warehouses.value.filter((item) => field.warehouseTypes?.includes(item.warehouseType));
};

const applyPresetWarehouse = () => {
  if (!isCreateMode.value || !props.meta.primaryField || props.meta.primaryField.kind !== 'warehouse' || form.primaryName) {
    return;
  }
  const matchedById = presetWarehouseId.value == null
    ? null
    : getWarehouseOptions(props.meta.primaryField).find((item) => item.id === presetWarehouseId.value);
  if (matchedById) {
    form.primaryName = matchedById.name;
    return;
  }
  if (presetWarehouseName.value) {
    const matchedByName = getWarehouseOptions(props.meta.primaryField).find((item) => item.name === presetWarehouseName.value);
    if (matchedByName) {
      form.primaryName = matchedByName.name;
      return;
    }
    form.primaryName = presetWarehouseName.value;
  }
};

const loadSalesmen = async () => {
  if (!currentOrgId.value) {
    salesmen.value = [];
    return;
  }
  try {
    const [salesmanResult, roleList] = await Promise.all([
      fetchStoreSalesmenApi(currentOrgId.value),
      fetchCurrentUserRolesApi(currentOrgId.value),
    ]);
    const isSalesman = roleList.some((role) => role.roleCode === 'SALESMAN');
    const normalizedSalesmen = Array.from(new Map(
      salesmanResult.map((item: SalesmanCandidateItem) => [item.userId, {
        userId: item.userId,
        realName: item.realName,
        phone: item.phone,
        label: `${item.realName}${item.phone ? `（${item.phone}）` : ''}`,
      } satisfies SalesmanOption]),
    ).values());
    salesmen.value = isSalesman
      ? normalizedSalesmen.filter((item) => item.phone === sessionStore.userPhone)
      : normalizedSalesmen;
    if (isCreateMode.value && !isReadonlyMode.value && form.salesmanUserId == null) {
      const selfCandidate = salesmen.value.find((item) => item.phone && item.phone === sessionStore.userPhone);
      if (selfCandidate) {
        form.salesmanUserId = selfCandidate.userId;
        form.salesmanName = selfCandidate.realName;
      }
    }
  } catch {
    salesmen.value = [];
    ElMessage.error('业务员列表加载失败');
  }
};

const loadPermission = async () => {
  if (!currentOrgId.value) {
    canCreate.value = false;
    canUpdate.value = false;
    canApprove.value = false;
    canUnapprove.value = false;
    return;
  }
  try {
    const result = await fetchGenericInventoryDocumentPermissionApi(props.meta.type, currentOrgId.value || undefined);
    canCreate.value = Boolean(result.canCreate);
    canUpdate.value = Boolean(result.canUpdate);
    canApprove.value = Boolean(result.canApprove);
    canUnapprove.value = Boolean(result.canUnapprove);
  } catch {
    canCreate.value = false;
    canUpdate.value = false;
    canApprove.value = false;
    canUnapprove.value = false;
    ElMessage.error('权限信息加载失败');
  }
};

const loadCounterpartyOptions = async () => {
  if (counterpartySupplierSelect.value) {
    await loadSupplierOptions();
  }
};

const fillDetail = async () => {
  if (!documentId.value) {
    detailStatus.value = '';
    form.documentCode = '';
    form.documentDate = '';
    form.primaryName = '';
    form.secondaryName = '';
    form.counterpartyName = '';
    form.counterpartyName2 = '';
    form.reason = '';
    form.upstreamCode = '';
    form.salesmanUserId = undefined;
    form.salesmanName = '';
    form.remark = '';
    form.attachmentName = '';
    form.attachmentFiles = [];
    form.creator = '';
    form.createdAt = '';
    form.auditor = '';
    form.auditedAt = '';
    initExtraFields();
    rows.value = [createEmptyRow()];
    return;
  }
  loading.value = true;
  try {
    const detail = await fetchGenericInventoryDocumentDetailApi(props.meta.type, documentId.value, currentOrgId.value || undefined);
    detailStatus.value = detail.status;
    form.documentCode = detail.documentCode;
    form.documentDate = detail.documentDate;
    form.primaryName = detail.primaryName;
    form.secondaryName = detail.secondaryName;
    form.counterpartyName = detail.counterpartyName;
    form.counterpartyName2 = detail.counterpartyName2;
    form.reason = detail.reason;
    form.upstreamCode = detail.upstreamCode;
    form.salesmanUserId = detail.salesmanUserId ?? undefined;
    form.salesmanName = detail.salesmanName;
    form.remark = detail.remark;
    form.attachmentName = detail.extraFields?.attachmentName ?? '';
    form.attachmentFiles = form.attachmentName
      ? [{ name: form.attachmentName, url: detail.extraFields?.attachmentUrl || '' }]
      : [];
    form.creator = detail.creator;
    form.createdAt = detail.createdAt;
    form.auditor = detail.auditor;
    form.auditedAt = detail.auditedAt;
    initExtraFields();
    Object.entries(detail.extraFields ?? {}).forEach(([key, value]) => {
      form.extraFields[key] = value;
    });
    delete form.extraFields.attachmentName;
    delete form.extraFields.attachmentUrl;
    rows.value = detail.items.map((item) => {
      const unitOptions = parseUnitOptions(item.extraFields?.unitOptions, item.unitName);
      const unitRate = item.extraFields?.unitRate
        ? Number(item.extraFields.unitRate)
        : resolveUnitRateFromOptions(unitOptions, item.unitName);
      return {
        id: rowSeed.value++,
        itemCode: item.itemCode,
        itemName: item.itemName,
        spec: item.spec,
        category: item.category,
        unitName: item.unitName,
        unitOptions,
        unitRate,
        baseUnit: item.extraFields?.baseUnit ?? '',
        baseUnitQuantity: item.extraFields?.baseUnitQuantity
          ? Number(item.extraFields.baseUnitQuantity)
          : ((item.quantity != null && unitRate != null)
            ? Number((Number(item.quantity) * Number(unitRate)).toFixed(4))
            : null),
        availableQty: item.availableQty,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        amount: item.amount,
        lineReason: item.lineReason,
        dishId: item.dishId,
        dishName: item.dishName,
        damageReason: item.damageReason,
        remark: item.remark,
      };
    });
    if (!rows.value.length && !warehouseOpeningBalanceViewVisible.value) {
      rows.value = [createEmptyRow()];
    }
  } catch {
    detailStatus.value = '';
    initExtraFields();
    rows.value = [createEmptyRow()];
    ElMessage.error(`${props.meta.title}详情加载失败`);
  } finally {
    loading.value = false;
  }
};

const validateRows = () => {
  const validRows = rows.value.filter((item) => item.itemCode.trim() || item.itemName.trim());
  if (!validRows.length) {
    ElMessage.warning('请至少录入一条物品明细');
    return null;
  }
  const invalid = validRows.some((item) => !item.itemCode.trim() || !item.itemName.trim() || !(Number(item.quantity) > 0));
  if (invalid) {
    ElMessage.warning('请完善物品编码、名称和数量');
    return null;
  }
  if (isWarehouseOpeningBalance.value) {
    const invalidWarehouseOpeningBalanceRow = validRows.some((item) => !String(item.unitName ?? '').trim());
    if (invalidWarehouseOpeningBalanceRow) {
      ElMessage.warning('请完善库存单位');
      return null;
    }
  }
  return validRows;
};

const buildPayload = (): GenericInventoryDocumentSavePayload | null => {
  if (!form.documentDate) {
    ElMessage.warning(`请填写${props.meta.dateLabel}`);
    return null;
  }
  if (props.meta.primaryField && !String(form.primaryName ?? '').trim()) {
    ElMessage.warning(`请选择${props.meta.primaryField.label}`);
    return null;
  }
  const validRows = validateRows();
  if (!validRows) {
    return null;
  }
  const extraFields = { ...form.extraFields };
  delete extraFields.attachmentName;
  delete extraFields.attachmentUrl;
  if (form.attachmentName) {
    extraFields.attachmentName = form.attachmentName;
  }
  const payload: GenericInventoryDocumentSavePayload = {
    documentDate: form.documentDate,
    primaryName: form.primaryName || undefined,
    secondaryName: form.secondaryName || undefined,
    counterpartyName: form.counterpartyName || undefined,
    counterpartyName2: form.counterpartyName2 || undefined,
    reason: form.reason || undefined,
    upstreamCode: form.upstreamCode || undefined,
    salesmanUserId: form.salesmanUserId,
    salesmanName: form.salesmanName || undefined,
    remark: form.remark || undefined,
    extraFields,
    items: validRows.map((item): GenericInventoryDocumentLinePayload => ({
      itemCode: item.itemCode.trim(),
      itemName: item.itemName.trim(),
      spec: item.spec || undefined,
      category: item.category || undefined,
      unitName: item.unitName || undefined,
      availableQty: item.availableQty,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      amount: item.amount,
      lineReason: item.lineReason || undefined,
      dishId: item.dishId || undefined,
      dishName: item.dishName || undefined,
      damageReason: item.damageReason || undefined,
      remark: item.remark || undefined,
      extraFields: {
        ...(item.unitOptions.length ? { unitOptions: JSON.stringify(item.unitOptions) } : {}),
        ...(item.unitRate != null ? { unitRate: String(item.unitRate) } : {}),
        ...(item.baseUnit ? { baseUnit: item.baseUnit } : {}),
        ...(item.baseUnitQuantity != null ? { baseUnitQuantity: String(item.baseUnitQuantity) } : {}),
      },
    })),
  };
  return payload;
};

const handleAttachmentChange = (uploadFile: UploadFile) => {
  form.attachmentName = uploadFile.name || '';
  form.attachmentFiles = form.attachmentName ? [{ name: form.attachmentName, url: '' }] : [];
};

const handleSubmit = async () => {
  if (isReadonlyMode.value) {
    return;
  }
  const payload = buildPayload();
  if (!payload) {
    return;
  }
  saving.value = true;
  try {
    if (documentId.value && isEditMode.value) {
      await updateGenericInventoryDocumentApi(props.meta.type, documentId.value, payload, currentOrgId.value || undefined);
      ElMessage.success('保存成功');
      await navigateToList();
      return;
    }
    await createGenericInventoryDocumentApi(props.meta.type, payload, currentOrgId.value || undefined);
    ElMessage.success('保存成功');
    await navigateToList();
  } catch (error) {
    const message = error instanceof Error ? error.message : '';
    if (!message) {
      ElMessage.error('保存失败');
    }
  } finally {
    saving.value = false;
  }
};

const handleApproveAction = async () => {
  if (!documentId.value) {
    return;
  }
  if (!canApprove.value) {
    ElMessage.warning('当前账号无审核权限');
    return;
  }
  saving.value = true;
  try {
    await batchApproveGenericInventoryDocumentApi(props.meta.type, [documentId.value], currentOrgId.value || undefined);
    ElMessage.success('审核通过成功');
    await navigateToList();
  } finally {
    saving.value = false;
  }
};

const handleRejectAction = async () => {
  if (!documentId.value) {
    return;
  }
  if (!canUnapprove.value) {
    ElMessage.warning('当前账号无审核权限');
    return;
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入不通过原因', '审核不通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入不通过原因',
      inputValidator: (input: string) => input.trim() ? true : '请填写不通过原因',
    });
    saving.value = true;
    try {
      await batchUnapproveGenericInventoryDocumentApi(
        props.meta.type,
        [documentId.value],
        value.trim(),
        currentOrgId.value || undefined,
      );
      ElMessage.success('审核不通过成功');
      await navigateToList();
    } finally {
      saving.value = false;
    }
  } catch {
    // 用户取消时不提示
  }
};

const navigateToList = async () => {
  const activeMenuPath = typeof route.meta.activeMenu === 'string' ? route.meta.activeMenu.trim() : '';
  if (activeMenuPath) {
    await router.push(activeMenuPath);
    return;
  }
  if (router.hasRoute(props.meta.listRouteName)) {
    await router.push({ name: props.meta.listRouteName });
    return;
  }
  await router.back();
};

const handleCancel = async () => {
  await navigateToList();
};

const handleSaveDraft = () => {
  if (showApprovalActions.value) {
    void handleRejectAction();
    return;
  }
  void handleSubmit();
};

const handlePrimaryAction = () => {
  if (showApprovalActions.value) {
    void handleApproveAction();
    return;
  }
  void handleSubmit();
};

const updateSalesmanName = (userId?: number) => {
  const target = salesmen.value.find((item) => item.userId === userId);
  form.salesmanName = target?.realName ?? '';
};

const reloadPageContext = async () => {
  initExtraFields();
  rows.value = [createEmptyRow()];
  itemSelectorVisible.value = false;
  itemSelectorKeyword.value = '';
  itemSelectorStatus.value = '';
  activeItemTreeId.value = 'all';
  itemSelectorCurrentPage.value = 1;
  itemSelectorPageSize.value = 10;
  itemSelectorTotal.value = 0;
  selectingItemRowIndex.value = null;
  selectedItemCandidates.value = [];
  itemTreeData.value = [];
  itemCandidateSource.value = [];
  await Promise.all([loadPermission(), loadWarehouses(), loadSalesmen(), loadCounterpartyOptions(), loadDishOptions()]);
  await fillDetail();
  applyPresetWarehouse();
};

watch(
  () => [sessionStore.currentOrgId, route.name, route.params.id, route.query.warehouseId, route.query.warehouseName],
  () => {
    void reloadPageContext();
  },
);

onMounted(async () => {
  await reloadPageContext();
});
</script>

<template>
  <div class="item-create-page">
    <section
      v-if="warehouseOpeningBalanceViewVisible"
      v-loading="loading"
      class="panel warehouse-opening-balance-view"
    >
      <div class="warehouse-opening-view-header">
        <el-button text class="warehouse-opening-back" @click="handleCancel">
          <span class="warehouse-opening-back-icon">&lt;</span>
        </el-button>
        <span class="warehouse-opening-title">查看</span>
      </div>

      <div class="warehouse-opening-info-grid">
        <div class="warehouse-opening-info-item warehouse-opening-info-wide">
          <span class="warehouse-opening-info-label">仓库：</span>
          <span>{{ form.primaryName }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">期初设置状态：</span>
          <span>{{ warehouseOpeningBalanceStatusText }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">期初日期：</span>
          <span>{{ formatWarehouseOpeningBalanceDate(form.documentDate) }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">业务员：</span>
          <span>{{ form.salesmanName }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">备注：</span>
          <span>{{ form.remark }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">入库单号：</span>
          <span class="warehouse-opening-code">{{ form.documentCode }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">创建人：</span>
          <span>{{ form.creator }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">创建时间：</span>
          <span>{{ formatWarehouseOpeningBalanceDateTime(form.createdAt) }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">审核人：</span>
          <span>{{ form.auditor }}</span>
        </div>
        <div class="warehouse-opening-info-item">
          <span class="warehouse-opening-info-label">审核时间：</span>
          <span>{{ formatWarehouseOpeningBalanceDateTime(form.auditedAt) }}</span>
        </div>
      </div>

      <el-table
        :data="rows"
        border
        stripe
        class="warehouse-opening-detail-table"
        :fit="false"
        show-summary
        :summary-method="getItemTableSummaries"
      >
        <el-table-column type="index" label="序号" width="60" fixed="left" />
        <el-table-column prop="itemCode" label="物品编码" min-width="130" />
        <el-table-column prop="itemName" label="物品名称" min-width="140" />
        <el-table-column prop="spec" label="规格型号" min-width="120" />
        <el-table-column prop="unitName" label="库存单位" min-width="100" />
        <el-table-column prop="unitRate" label="库存单位换算率" min-width="160">
          <template #default="{ row }">
            {{ formatWarehouseOpeningBalanceUnitRateText(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="入库数量" min-width="110">
          <template #default="{ row }">
            {{ row.quantity != null ? Number(row.quantity).toFixed(4) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" min-width="110">
          <template #default="{ row }">
            {{ row.amount != null ? Number(row.amount).toFixed(2) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="baseUnit" label="基准单位" min-width="100" />
        <el-table-column prop="baseUnitQuantity" label="基准单位数量" min-width="130">
          <template #default="{ row }">
            {{ row.baseUnitQuantity != null ? Number(row.baseUnitQuantity).toFixed(4) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
    </section>

    <section v-else v-loading="loading" class="panel form-panel inventory-document-create-page">
      <FixedActionBreadcrumb
        :navs="navs"
        :active-key="activeNav"
        :show-actions="showApprovalActions || !isReadonlyMode"
        :primary-action-text="actionPrimaryText"
        :secondary-action-text="actionSecondaryText"
        :show-primary-action="showApprovalActions ? canApprove : true"
        :show-secondary-action="showApprovalActions ? canUnapprove : false"
        @back="handleCancel"
        @save-draft="handleSaveDraft"
        @save="handlePrimaryAction"
        @navigate="(key) => { activeNav = key; }"
      />

      <CommonFormSection title="基础信息">
        <CommonPageNotice v-if="props.meta.noticeLines?.length" :lines="props.meta.noticeLines" />
        <el-form label-width="110px" class="item-create-form">
          <el-row :gutter="16">
            <el-col v-if="showDocumentCode" :span="8">
              <el-form-item label="单据编号">
                <el-input :model-value="form.documentCode || '保存后生成'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="props.meta.dateLabel">
                <el-date-picker
                  v-model="form.documentDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :disabled="isReadonlyMode"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="业务员">
                <el-select v-model="form.salesmanUserId" :disabled="isReadonlyMode" clearable filterable placeholder="请选择" style="width: 100%" @change="updateSalesmanName">
                  <el-option v-for="item in salesmen" :key="item.userId" :label="item.label" :value="item.userId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="props.meta.primaryField" :span="8">
              <el-form-item :label="props.meta.primaryField.label">
                <el-select
                  v-if="props.meta.primaryField.kind === 'warehouse'"
                  v-model="form.primaryName"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option v-for="item in getWarehouseOptions(props.meta.primaryField)" :key="item.id" :label="item.label" :value="item.name" />
                </el-select>
                <el-select
                  v-else-if="props.meta.primaryField.kind === 'select'"
                  v-model="form.primaryName"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  :placeholder="`请选择${props.meta.primaryField.label}`"
                  style="width: 100%"
                >
                  <el-option v-for="item in props.meta.primaryField.options ?? []" :key="item" :label="item" :value="item" />
                </el-select>
                <el-input v-else v-model="form.primaryName" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col v-if="props.meta.secondaryField" :span="8">
              <el-form-item :label="props.meta.secondaryField.label">
                <el-select
                  v-if="props.meta.secondaryField.kind === 'warehouse'"
                  v-model="form.secondaryName"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option v-for="item in getWarehouseOptions(props.meta.secondaryField)" :key="item.id" :label="item.label" :value="item.name" />
                </el-select>
                <el-input v-else v-model="form.secondaryName" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col v-if="props.meta.counterpartyField" :span="8">
              <el-form-item :label="props.meta.counterpartyField.label">
                <el-select
                  v-if="props.meta.counterpartyField.kind === 'warehouse'"
                  v-model="form.counterpartyName"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option v-for="item in getWarehouseOptions(props.meta.counterpartyField)" :key="item.id" :label="item.label" :value="item.name" />
                </el-select>
                <el-select
                  v-else-if="props.meta.counterpartyField.kind === 'select'"
                  v-model="form.counterpartyName"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  :placeholder="props.meta.counterpartyField.label === '供应商' ? '请选择供应商' : `请选择${props.meta.counterpartyField.label}`"
                  style="width: 100%"
                >
                  <template v-if="counterpartySupplierSelect">
                    <el-option
                      v-for="item in supplierOptions"
                      :key="item.id"
                      :label="item.label"
                      :value="item.value"
                    />
                  </template>
                  <template v-else>
                    <el-option
                      v-for="item in props.meta.counterpartyField.options ?? []"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </template>
                </el-select>
                <el-input v-else v-model="form.counterpartyName" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col v-if="props.meta.counterpartyField2" :span="8">
              <el-form-item :label="props.meta.counterpartyField2.label">
                <el-input v-model="form.counterpartyName2" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col v-if="props.meta.reasonField" :span="8">
              <el-form-item :label="props.meta.reasonField.label">
                <el-select
                  v-if="props.meta.reasonField.kind === 'select'"
                  v-model="form.reason"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  :placeholder="`请选择${props.meta.reasonField.label}`"
                  style="width: 100%"
                >
                  <el-option v-for="item in props.meta.reasonField.options ?? []" :key="item" :label="item" :value="item" />
                </el-select>
                <el-input v-else v-model="form.reason" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col v-for="field in props.meta.extraFields ?? []" :key="field.key" :span="8">
              <el-form-item :label="field.label">
                <el-select
                  v-if="field.kind === 'warehouse'"
                  v-model="form.extraFields[field.key]"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option v-for="item in getWarehouseOptions(field)" :key="item.id" :label="item.label" :value="item.name" />
                </el-select>
                <el-select
                  v-else-if="field.kind === 'select'"
                  v-model="form.extraFields[field.key]"
                  :disabled="isReadonlyMode"
                  clearable
                  filterable
                  :placeholder="`请选择${field.label}`"
                  style="width: 100%"
                >
                  <el-option v-for="item in field.options ?? []" :key="item" :label="item" :value="item" />
                </el-select>
                <el-input v-else v-model="form.extraFields[field.key]" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col v-if="showUpstreamCode" :span="8">
              <el-form-item label="上游单号">
                <el-input v-model="form.upstreamCode" :disabled="isReadonlyMode" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" :disabled="isReadonlyMode" clearable maxlength="500" placeholder="在此填写备注信息..." />
              </el-form-item>
            </el-col>
            <el-col v-if="showAttachment" :span="24">
              <el-form-item label="附件">
                <div class="attachment-field">
                  <el-upload
                    v-model:file-list="form.attachmentFiles"
                    :auto-upload="false"
                    :limit="1"
                    :show-file-list="false"
                    :disabled="isReadonlyMode"
                    @change="handleAttachmentChange"
                  >
                    <el-button :disabled="isReadonlyMode">上传文件</el-button>
                  </el-upload>
                  <el-input
                    v-model="form.attachmentName"
                    :disabled="isReadonlyMode"
                    placeholder="请选择文件"
                    readonly
                  />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </CommonFormSection>

      <CommonFormSection title="物品信息">
        <CommonTableSection
          :data="rows"
          :height="itemTableHeight"
          :show-summary="isWarehouseOpeningBalance || isDepartmentPicking"
          :summary-method="getItemTableSummaries"
          :class="{ 'purchase-inbound-item-table': usePurchaseInboundItemTableStyle }"
        >
          <el-table-column v-if="usePurchaseReturnOutboundItemTableStyle" type="selection" width="44" :selectable="() => false" fixed="left" />
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="96" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" :disabled="isReadonlyMode" @click="addRow($index)">+</el-button>
              <el-button text :disabled="isReadonlyMode" @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="itemCode" :label="itemCodeColumnLabel" min-width="130">
            <template #default="{ row, $index }">
              <el-input
                :model-value="row.itemCode"
                placeholder="点击选择物品"
                readonly
                :disabled="isReadonlyMode"
                class="item-code-picker"
                @click="openItemSelector($index)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="itemName" :label="itemNameColumnLabel" min-width="140">
            <template #default="{ row }">
              {{ row.itemName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格型号" min-width="120">
            <template #default="{ row }">
              {{ row.spec || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="!isWarehouseOpeningBalance" prop="category" :label="itemCategoryColumnLabel" min-width="120">
            <template #default="{ row }">
              {{ row.category || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="unitName" :label="unitColumnLabel" min-width="100">
            <template #default="{ row }">
              <template v-if="isWarehouseOpeningBalance || isDepartmentPicking">
                <el-select
                  v-model="row.unitName"
                  :disabled="isReadonlyMode || !row.itemCode"
                  clearable
                  filterable
                  style="width: 100%"
                  @change="handleWarehouseOpeningBalanceUnitChange(row, $event)"
                >
                  <el-option
                    v-for="option in row.unitOptions"
                    :key="`${row.id}-${option.value}`"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </template>
              <el-input v-else v-model="row.unitName" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column v-if="showUnitRateColumn" prop="unitRate" label="库存单位换算率" min-width="160">
            <template #default="{ row }">
              {{ formatWarehouseOpeningBalanceUnitRateText(row) }}
            </template>
          </el-table-column>
          <el-table-column v-if="props.meta.showAvailableQty" :label="availableQtyColumnLabel" min-width="100">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.availableQty" :disabled="isReadonlyMode" :precision="4" :min="0" />
            </template>
          </el-table-column>
          <el-table-column prop="quantity" :label="quantityColumnLabel" min-width="100">
            <template #default="{ row }">
              <CommonNumberInput
                :model-value="row.quantity"
                :disabled="isReadonlyMode"
                :precision="4"
                :min="0"
                @update:model-value="handleQuantityValueUpdate(row, $event)"
                @change="handleQuantityValueUpdate(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="!isWarehouseOpeningBalance" prop="unitPrice" :label="unitPriceColumnLabel" min-width="100">
            <template #default="{ row }">
              <CommonNumberInput v-model="row.unitPrice" :disabled="isReadonlyMode" :precision="4" :min="0" @change="syncRowAmount(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" min-width="110">
            <template #default="{ row }">
              <CommonNumberInput
                v-if="isWarehouseOpeningBalance"
                v-model="row.amount"
                :disabled="isReadonlyMode"
                :precision="2"
                :min="0"
              />
              <span v-else>{{ Number(row.amount ?? 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isWarehouseOpeningBalance" prop="baseUnit" label="基准单位" min-width="100">
            <template #default="{ row }">
              {{ row.baseUnit || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isWarehouseOpeningBalance" prop="baseUnitQuantity" label="基准单位数量" min-width="130">
            <template #default="{ row }">
              {{ row.baseUnitQuantity != null ? Number(row.baseUnitQuantity).toFixed(4) : '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="showReasonColumn" label="原因" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.lineReason" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column v-if="showDishColumn" label="关联菜品" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.dishId"
                :disabled="isReadonlyMode"
                clearable
                filterable
                placeholder="请选择"
                @change="handleDishChange(row)"
              >
                <el-option
                  v-for="option in dishOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column v-if="showDamageReasonColumn" label="报损原因" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.damageReason" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.remark" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <template v-if="usePurchaseInboundItemTableStyle && !isWarehouseOpeningBalance" #append>
            <div class="purchase-inbound-summary-row">
              <span class="summary-title">合计</span>
              <span class="summary-cell">{{ quantityColumnLabel }}：{{ totalQuantity.toFixed(4) }}</span>
              <span class="summary-cell">金额：{{ totalAmount.toFixed(2) }}</span>
              <span v-if="isWarehouseOpeningBalance" class="summary-cell">基准单位数量：{{ totalBaseUnitQuantity.toFixed(4) }}</span>
            </div>
          </template>
        </CommonTableSection>
      </CommonFormSection>
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
      @page-change="(page) => { itemSelectorCurrentPage = page; loadItemCandidates(); }"
      @page-size-change="(size) => { itemSelectorPageSize = size; itemSelectorCurrentPage = 1; loadItemCandidates(); }"
      @confirm="handleItemSelectorConfirm"
    />
  </div>
</template>

<style scoped>
.inventory-document-create-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.warehouse-opening-balance-view {
  min-height: calc(100vh - 96px);
  padding: 0;
  background: #fff;
}

.warehouse-opening-view-header {
  display: flex;
  align-items: center;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.warehouse-opening-back {
  width: 28px;
  height: 28px;
  margin-right: 4px;
  padding: 0;
  color: #606266;
}

.warehouse-opening-back-icon {
  font-size: 18px;
  line-height: 1;
}

.warehouse-opening-title {
  font-size: 14px;
  font-weight: 600;
}

.warehouse-opening-info-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr 1fr 1fr;
  row-gap: 14px;
  column-gap: 24px;
  padding: 18px 18px 16px;
  font-size: 13px;
  color: #303133;
}

.warehouse-opening-info-wide {
  grid-column: span 2;
}

.warehouse-opening-info-item {
  display: flex;
  min-width: 0;
  line-height: 20px;
}

.warehouse-opening-info-label {
  flex: 0 0 auto;
  color: #606266;
}

.warehouse-opening-code {
  color: #f56c6c;
}

.warehouse-opening-detail-table {
  width: calc(100% - 36px);
  margin: 0 18px 18px;
}

:deep(.warehouse-opening-detail-table .el-table__header-wrapper th.el-table__cell),
:deep(.warehouse-opening-detail-table .el-table__body-wrapper td.el-table__cell),
:deep(.warehouse-opening-detail-table .el-table__footer-wrapper td.el-table__cell) {
  height: 32px;
  padding: 0;
}

:deep(.warehouse-opening-detail-table .cell) {
  font-size: 12px;
  line-height: 32px;
}

.purchase-inbound-summary-row {
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

.item-code-picker {
  cursor: pointer;
}

.attachment-field {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.attachment-field :deep(.el-upload) {
  flex: 0 0 auto;
}

.attachment-field :deep(.el-input) {
  flex: 1 1 auto;
  min-width: 0;
}

:deep(.purchase-inbound-item-table .common-number-input) {
  width: 100%;
  height: 20px !important;
  margin: 0 !important;
}

:deep(.purchase-inbound-item-table .el-input),
:deep(.purchase-inbound-item-table .el-select) {
  --el-input-height: 20px;
  height: 20px !important;
  margin: 0 !important;
}

:deep(.purchase-inbound-item-table .el-input__wrapper),
:deep(.purchase-inbound-item-table .el-select__wrapper) {
  min-height: 20px !important;
  height: 20px !important;
  padding: 0 6px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}

:deep(.purchase-inbound-item-table .el-input__inner),
:deep(.purchase-inbound-item-table .el-select__placeholder),
:deep(.purchase-inbound-item-table .el-select__selected-item),
:deep(.purchase-inbound-item-table .el-select__selection-text) {
  height: 20px !important;
  line-height: 20px !important;
}

:deep(.purchase-inbound-item-table .el-select__selection) {
  min-height: 20px !important;
  height: 20px !important;
  margin: 0 !important;
}

:deep(.purchase-inbound-item-table .el-table__header-wrapper th.el-table__cell),
:deep(.purchase-inbound-item-table .el-table__body-wrapper td.el-table__cell),
:deep(.purchase-inbound-item-table .el-table__footer-wrapper td.el-table__cell) {
  height: 20px !important;
  line-height: 20px !important;
}

:deep(.purchase-inbound-item-table .el-table__header-wrapper .cell),
:deep(.purchase-inbound-item-table .el-table__body-wrapper .cell),
:deep(.purchase-inbound-item-table .el-table__footer-wrapper .cell) {
  display: flex;
  align-items: center;
  height: 20px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  line-height: 20px !important;
}

:deep(.purchase-inbound-item-table .el-table__body-wrapper .cell .el-input),
:deep(.purchase-inbound-item-table .el-table__body-wrapper .cell .el-select),
:deep(.purchase-inbound-item-table .el-table__body-wrapper .cell .common-number-input) {
  align-self: stretch;
}

:deep(.purchase-inbound-item-table .el-table__body-wrapper .el-button) {
  min-height: 20px !important;
  height: 20px !important;
  margin: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  line-height: 20px !important;
}

:deep(.purchase-inbound-item-table .item-code-picker .el-input__wrapper) {
  cursor: pointer;
}
</style>
