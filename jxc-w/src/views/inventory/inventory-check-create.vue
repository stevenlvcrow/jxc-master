<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Download } from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonNumberInput from '@/components/CommonNumberInput.vue';
import CommonSelectorDialog, {
  type SelectorColumn,
  type SelectorTreeNode,
} from '@/components/CommonSelectorDialog.vue';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { fetchCurrentUserRolesApi } from '@/api/modules/auth';
import { fetchStoreSalesmenApi, type SalesmanCandidateItem } from '@/api/modules/system-admin';
import { fetchStoreWarehousesApi, type WarehouseRow as ApiWarehouseRow } from '@/api/modules/warehouse';
import {
  createInventoryCheckApi,
  fetchInventoryCheckDetailApi,
  fetchInventoryBalancesApi,
  fetchInventoryCheckPermissionApi,
  type InventoryBalanceRow as ApiInventoryBalanceRow,
  updateInventoryCheckApi,
  type InventoryCheckDetail,
  type InventoryCheckSavePayload,
} from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';
import { useRequiredOrgScope } from '@/composables/useRequiredOrgScope';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

type SalesmanOption = {
  userId: number;
  realName: string;
  phone: string;
  label: string;
};

type WarehouseOption = {
  id: number;
  name: string;
  code: string;
  label: string;
};

type CheckTypeOption = '指定物品' | '分区盘点' | '全仓盘点';
type UnitOption = '库存单位';

type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  stockUnit: string;
  productionCost: string;
  status: string;
};

type InventoryCheckItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  abnormalFlag: string;
  spec: string;
  category: string;
  unit1ActualQty: number | null;
  unit1: string;
  actualTotalQty: number | null;
  actualTotalUnit: string;
  bookQty: number | null;
  bookPrice: number | null;
  profitQty: number | null;
  lossQty: number | null;
  profitLossReason: string;
  actualAmount: number | null;
  bookAmount: number | null;
  profitInboundPrice: number | null;
  profitAmount: number | null;
  lossOutboundPrice: number | null;
  lossAmount: number | null;
  differenceReasonCode: string;
  remark: string;
};

const checkTypeOptions: CheckTypeOption[] = ['指定物品', '分区盘点', '全仓盘点'];
const summaryUnitOptions: UnitOption[] = ['库存单位'];
const checkRangeTypeCodeMap: Record<CheckTypeOption, string> = {
  指定物品: 'SPECIFIC_ITEM',
  分区盘点: 'PARTITION',
  全仓盘点: 'FULL_WAREHOUSE',
};
const checkRangeTypeLabelMap: Record<string, CheckTypeOption> = {
  SPECIFIC_ITEM: '指定物品',
  PARTITION: '分区盘点',
  FULL_WAREHOUSE: '全仓盘点',
};

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const { orgId: currentOrgId, storeId } = useRequiredOrgScope();
const ITEM_STATUS_DICT = 'item.status';
const STOCKTAKE_FREQUENCY_DICT = 'inventory.stocktake_frequency';
const DIFFERENCE_REASON_DICT = 'inventory.check_difference_reason';
const { optionsOf } = useDictionaryOptions([ITEM_STATUS_DICT, STOCKTAKE_FREQUENCY_DICT, DIFFERENCE_REASON_DICT]);
const itemStatusOptions = optionsOf(ITEM_STATUS_DICT, { enabled: true, label: '全部', value: '' });
const stocktakeFrequencyOptions = optionsOf(STOCKTAKE_FREQUENCY_DICT);
const differenceReasonOptions = optionsOf(DIFFERENCE_REASON_DICT);
const normalizedItemStatusOptions = computed(() => itemStatusOptions.value.map((item) => ({
  label: item.itemLabel,
  value: item.itemCode,
})));
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);
const pageLoading = ref(false);
const snapshotRefreshing = ref(false);
const hydratingDetail = ref(false);

const sectionNavs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '盘点明细' },
];

const routeId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const isCreateMode = computed(() => route.name === 'InventoryCheckCreate');
const isViewMode = computed(() => route.name === 'InventoryCheckView');
const isEditMode = computed(() => route.name === 'InventoryCheckEdit');
const canCreate = ref(false);
const canUpdate = ref(false);
const canApprove = ref(false);
const canUnapprove = ref(false);
const detailStatus = ref('');
const isReadonlyMode = computed(() => isViewMode.value
  || detailStatus.value === 'APPROVED'
  || (isCreateMode.value && !canCreate.value)
  || (isEditMode.value && !canUpdate.value));

const warehouseOptions = ref<WarehouseOption[]>([]);
const salesmanOptions = ref<SalesmanOption[]>([]);
const salesmanSelectOptions = computed(() => {
  const options = [...salesmanOptions.value];
  if (form.salesmanUserId != null && form.salesmanName && !options.some((item) => item.userId === form.salesmanUserId)) {
    options.unshift({
      userId: form.salesmanUserId,
      realName: form.salesmanName,
      phone: '',
      label: form.salesmanName,
    });
  }
  return options;
});

const itemSelectorVisible = ref(false);
const itemSelectorKeyword = ref('');
const itemSelectorStatus = ref('');
const activeItemTreeId = ref<string>('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);
const itemCandidateSource = ref<ItemCandidate[]>([]);
const selectingItemRowIndex = ref<number | null>(null);
const itemSelectorMode = ref<'append' | 'replace'>('append');

const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'category', label: '物品类别', minWidth: 120 },
  { prop: 'stockUnit', label: '库存单位', minWidth: 100 },
  { prop: 'status', label: '状态', minWidth: 80 },
];

const form = reactive({
  warehouseId: 0,
  warehouseName: '',
  checkDate: '',
  checkType: '指定物品' as CheckTypeOption,
  stocktakeFrequency: 'DAILY',
  summaryUnit: '库存单位' as UnitOption,
  freezeStock: false,
  collaborativeFlag: false,
  thirdPartyDocument: '--',
  salesmanUserId: undefined as number | undefined,
  salesmanName: '',
  planName: '',
  remark: '',
});

const rowSeed = ref(2);
const rows = ref<InventoryCheckItemRow[]>([]);

const createEmptyRow = (id: number): InventoryCheckItemRow => ({
  id,
  itemCode: '',
  itemName: '',
  abnormalFlag: '',
  spec: '',
  category: '',
  unit1ActualQty: null,
  unit1: '',
  actualTotalQty: null,
  actualTotalUnit: '',
  bookQty: null,
  bookPrice: null,
  profitQty: null,
  lossQty: null,
  profitLossReason: '',
  actualAmount: null,
  bookAmount: null,
  profitInboundPrice: null,
  profitAmount: null,
  lossOutboundPrice: null,
  lossAmount: null,
  differenceReasonCode: '',
  remark: '',
});

const roundValue = (value: number | null, digits = 4) => {
  if (value == null) {
    return null;
  }
  return Number(value.toFixed(digits));
};

const syncRowDerived = (row: InventoryCheckItemRow) => {
  row.actualTotalQty = row.unit1ActualQty;
  row.actualTotalUnit = row.unit1;
  row.bookAmount = row.bookQty != null && row.bookPrice != null
    ? Number((row.bookQty * row.bookPrice).toFixed(2))
    : null;
  row.actualAmount = row.actualTotalQty != null && row.bookPrice != null
    ? Number((row.actualTotalQty * row.bookPrice).toFixed(2))
    : null;

  const diff = row.actualTotalQty != null && row.bookQty != null
    ? roundValue(row.actualTotalQty - row.bookQty)
    : null;

  row.profitQty = diff != null && diff > 0 ? diff : 0;
  row.lossQty = diff != null && diff < 0 ? Math.abs(diff) : 0;
  row.profitInboundPrice = row.profitQty ? row.bookPrice : null;
  row.lossOutboundPrice = row.lossQty ? row.bookPrice : null;
  row.profitAmount = row.profitQty && row.profitInboundPrice != null
    ? Number((row.profitQty * row.profitInboundPrice).toFixed(2))
    : 0;
  row.lossAmount = row.lossQty && row.lossOutboundPrice != null
    ? Number((row.lossQty * row.lossOutboundPrice).toFixed(2))
    : 0;

  if (diff == null) {
    row.abnormalFlag = '';
  } else if (diff === 0) {
    row.abnormalFlag = '正常';
    row.differenceReasonCode = '';
    row.profitLossReason = '';
  } else {
    row.abnormalFlag = '异常';
  }
};

const totalActualAmount = computed(() => rows.value.reduce((sum, row) => sum + (row.actualAmount ?? 0), 0));
const totalBookAmount = computed(() => rows.value.reduce((sum, row) => sum + (row.bookAmount ?? 0), 0));
const totalDiffQty = computed(() => rows.value.reduce((sum, row) => sum + (row.profitQty ?? 0) - (row.lossQty ?? 0), 0));
const autoFillLoading = ref(false);
const exportLoading = ref(false);

const resolveOrgId = () => {
  return currentOrgId.value;
};

const resetForm = () => {
  detailStatus.value = 'DRAFT';
  form.warehouseId = 0;
  form.warehouseName = '';
  form.checkDate = '';
  form.checkType = '指定物品';
  form.stocktakeFrequency = 'DAILY';
  form.summaryUnit = '库存单位';
  form.freezeStock = false;
  form.collaborativeFlag = false;
  form.thirdPartyDocument = '--';
  form.salesmanUserId = undefined;
  form.salesmanName = '';
  form.planName = '';
  form.remark = '';
  rowSeed.value = 2;
  rows.value = [createEmptyRow(1)];
};

const loadWarehouseOptions = async () => {
  if (!storeId.value) {
    warehouseOptions.value = [];
    return;
  }
  const result = await fetchStoreWarehousesApi(storeId.value, { status: 'ENABLED' });
  warehouseOptions.value = result
    .map((item: ApiWarehouseRow) => ({
      id: item.id,
      name: item.warehouseName,
      code: item.warehouseCode,
      label: `${item.warehouseName} / ${item.warehouseCode}`,
    }))
    .sort((left, right) => left.name.localeCompare(right.name, 'zh-Hans-CN'));
};

const loadSalesmanOptions = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
    salesmanOptions.value = [];
    return;
  }
  const [userList, roleList] = await Promise.all([
    fetchStoreSalesmenApi(orgId),
    fetchCurrentUserRolesApi(orgId),
  ]);
  const isSalesman = roleList.some((role) => role.roleCode === 'SALESMAN');
  const options = userList.map((item: SalesmanCandidateItem) => ({
    userId: item.userId,
    realName: item.realName,
    phone: item.phone,
    label: `${item.realName} / ${item.phone}`,
  }));
  const normalized = Array.from(new Map(options.map((item) => [item.userId, item])).values());
  salesmanOptions.value = isSalesman
    ? normalized.filter((item) => item.phone === sessionStore.userPhone)
    : normalized;
  if (isCreateMode.value && !form.salesmanUserId) {
    const selfCandidate = salesmanOptions.value.find((item) => item.phone === sessionStore.userPhone);
    if (selfCandidate) {
      form.salesmanUserId = selfCandidate.userId;
      form.salesmanName = selfCandidate.realName;
    }
  }
};

const normalizeItemTreeNodes = (nodes: ItemCategoryTreeNode[]): SelectorTreeNode[] => nodes.map((node) => ({
  id: String(node.label ?? 'all'),
  label: String(node.label ?? ''),
  children: Array.isArray(node.children) ? normalizeItemTreeNodes(node.children) : undefined,
}));

const loadItemTree = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
    itemTreeData.value = [{ id: 'all', label: '全部' }];
    return;
  }
  const tree = await fetchItemCategoryTreeApi(orgId);
  itemTreeData.value = tree.length
    ? [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }]
    : [{ id: 'all', label: '全部' }];
};

const mapItemCandidate = (row: ItemVO): ItemCandidate => ({
  id: row.id || row.code,
  code: row.code,
  name: row.name,
  spec: row.spec,
  category: row.category,
  stockUnit: row.stockUnit || row.purchaseUnit,
  productionCost: row.productionCost,
  status: row.status,
});

const loadItemCandidates = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
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
      stocktakeFrequency: form.stocktakeFrequency,
    }, orgId);
    itemCandidateSource.value = page.list.map(mapItemCandidate);
    itemSelectorTotal.value = Number(page.total ?? 0);
  } finally {
    itemSelectorLoading.value = false;
  }
};

const fetchAllPages = async <T>(loader: (pageNum: number, pageSize: number) => Promise<{ list: T[]; total: number; pageSize: number }>) => {
  const collected: T[] = [];
  let pageNum = 1;
  let totalCount: number;
  do {
    const page = await loader(pageNum, 200);
    const list = Array.isArray(page.list) ? page.list : [];
    collected.push(...list);
    totalCount = Number(page.total ?? collected.length);
    if (!list.length || Number(page.pageSize ?? 0) <= 0) {
      break;
    }
    pageNum += 1;
  } while (collected.length < totalCount);
  return collected;
};

const fetchAllItems = async () => fetchAllPages<ItemCandidate>(async (pageNum, pageSizeValue) => {
  const page = await fetchItemsApi({
    pageNo: pageNum,
    pageSize: pageSizeValue,
    status: 'ENABLED',
    stocktakeFrequency: form.stocktakeFrequency,
  }, resolveOrgId());
  return {
    list: page.list.map(mapItemCandidate),
    total: Number(page.total ?? 0),
    pageSize: Number(page.pageSize ?? pageSizeValue),
  };
});

const fetchAllBalances = async () => fetchAllPages<ApiInventoryBalanceRow>(async (pageNum, pageSizeValue) => {
  const page = await fetchInventoryBalancesApi({
    pageNum,
    pageSize: pageSizeValue,
    warehouse: form.warehouseName,
    checkDate: form.checkDate,
  }, resolveOrgId());
  return {
    list: page.list,
    total: Number(page.total ?? 0),
    pageSize: Number(page.pageSize ?? pageSizeValue),
  };
});

const parseNumberOrNull = (value: string | number | null | undefined) => {
  if (value == null) {
    return null;
  }
  const parsed = typeof value === 'number' ? value : Number.parseFloat(String(value));
  return Number.isFinite(parsed) ? parsed : null;
};

const loadBookSnapshot = async (item: ItemCandidate) => {
  const orgId = resolveOrgId();
  if (!orgId || !form.warehouseName || !form.checkDate) {
    return {
      bookQty: null as number | null,
      bookPrice: parseNumberOrNull(item.productionCost),
    };
  }
  const balances = await fetchAllBalances();
  const matched = balances.find((row: ApiInventoryBalanceRow) => row.itemCode === item.code);
  return {
    bookQty: parseNumberOrNull(matched?.quantity ?? null),
    bookPrice: parseNumberOrNull(item.productionCost),
  };
};

const loadFullWarehouseRows = async () => {
  if (form.checkType !== '全仓盘点') {
    return;
  }
  if (!form.warehouseName) {
    ElMessage.warning('请选择仓库');
    return;
  }
  if (!form.checkDate) {
    ElMessage.warning('请选择盘点日期');
    return;
  }
  autoFillLoading.value = true;
  try {
    const [balances, items] = await Promise.all([fetchAllBalances(), fetchAllItems()]);
    const itemMap = new Map(items.map((item) => [item.code, item]));
    const nextRows = balances.map((balance, index) => {
      const matched = itemMap.get(balance.itemCode);
      const row = createEmptyRow(index + 1);
      row.itemCode = balance.itemCode;
      row.itemName = balance.itemName || matched?.name || '';
      row.spec = matched?.spec || '';
      row.category = matched?.category || '';
      row.unit1 = matched?.stockUnit || '';
      row.actualTotalUnit = row.unit1;
      row.bookQty = parseNumberOrNull(balance.quantity);
      row.bookPrice = parseNumberOrNull(matched?.productionCost);
      row.unit1ActualQty = row.bookQty;
      syncRowDerived(row);
      return row;
    });
    rows.value = nextRows.length ? nextRows : [createEmptyRow(rowSeed.value++)];
    rowSeed.value = rows.value.length + 1;
    ElMessage.success('已按全仓盘点自动装载账面库存');
  } finally {
    autoFillLoading.value = false;
  }
};

const applyItemToRow = async (row: InventoryCheckItemRow, item: ItemCandidate) => {
  const snapshot = await loadBookSnapshot(item);
  row.itemCode = item.code;
  row.itemName = item.name;
  row.spec = item.spec;
  row.category = item.category;
  row.unit1 = item.stockUnit;
  row.actualTotalUnit = item.stockUnit;
  row.bookQty = snapshot.bookQty;
  row.bookPrice = snapshot.bookPrice;
  row.unit1ActualQty = null;
  syncRowDerived(row);
};

const refreshBookSnapshots = async () => {
  if (snapshotRefreshing.value || isReadonlyMode.value || hydratingDetail.value) {
    return;
  }
  if (!form.checkDate || !form.warehouseName) {
    return;
  }
  const targetRows = rows.value.filter((row) => row.itemCode);
  if (!targetRows.length) {
    return;
  }
  snapshotRefreshing.value = true;
  try {
    const balances = await fetchAllBalances();
    const balanceMap = new Map(balances.map((balance) => [balance.itemCode, balance]));
    rows.value.forEach((row) => {
      if (!row.itemCode) {
        return;
      }
      const matched = balanceMap.get(row.itemCode);
      row.bookQty = parseNumberOrNull(matched?.quantity ?? null);
      if (matched?.itemName) {
        row.itemName = matched.itemName;
      }
      syncRowDerived(row);
    });
  } finally {
    snapshotRefreshing.value = false;
  }
};

const toCsvCell = (value: unknown) => `"${String(value ?? '').replace(/"/g, '""')}"`;

const handleExportBookList = () => {
  if (!rows.value.length) {
    ElMessage.warning('请先加载盘点明细');
    return;
  }
  exportLoading.value = true;
  try {
    const lines = [
      ['物品编码', '物品名称', '规格型号', '物品类别', '单位', '账面数', '账面单价', '实盘数', '盘盈数量', '盘亏数量', '盈亏原因', '备注']
        .map(toCsvCell)
        .join(','),
    ];
    rows.value.forEach((row) => {
      lines.push([
        row.itemCode,
        row.itemName,
        row.spec,
        row.category,
        row.unit1,
        row.bookQty ?? '',
        row.bookPrice ?? '',
        row.unit1ActualQty ?? '',
        row.profitQty ?? '',
        row.lossQty ?? '',
        row.profitLossReason,
        row.remark,
      ].map(toCsvCell).join(','));
    });
    const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `盘点账面清单-${form.warehouseName || '未命名'}.csv`;
    document.body.appendChild(link);
    link.click();
    URL.revokeObjectURL(link.href);
    document.body.removeChild(link);
    ElMessage.success('导出成功');
  } finally {
    exportLoading.value = false;
  }
};

const appendItems = async (items: ItemCandidate[]) => {
  if (!items.length) {
    ElMessage.warning('请至少选择一个物品');
    return;
  }
  const existingCodes = new Set(rows.value.map((row) => row.itemCode).filter(Boolean));
  const appendable = items.filter((item) => !existingCodes.has(item.code));
  if (!appendable.length) {
    ElMessage.info('所选物品已存在');
    itemSelectorVisible.value = false;
    return;
  }
  const newRows = await Promise.all(appendable.map(async (item) => {
    const row = createEmptyRow(rowSeed.value++);
    await applyItemToRow(row, item);
    return row;
  }));
  rows.value.push(...newRows);
  itemSelectorVisible.value = false;
  ElMessage.success(`已添加 ${appendable.length} 条盘点物品`);
};

const applyDetail = (detail: InventoryCheckDetail) => {
  form.warehouseName = detail.warehouseName;
  form.warehouseId = warehouseOptions.value.find((item) => item.name === detail.warehouseName)?.id ?? 0;
  form.checkDate = detail.checkDate;
  form.checkType = checkRangeTypeLabelMap[detail.checkRangeType] ?? '指定物品';
  form.stocktakeFrequency = detail.stocktakeFrequency || 'DAILY';
  form.summaryUnit = '库存单位';
  form.freezeStock = detail.freezeStock;
  form.collaborativeFlag = detail.collaborativeFlag;
  form.thirdPartyDocument = detail.thirdPartyDocument || '--';
  form.salesmanName = detail.salesmanName;
  form.salesmanUserId = detail.salesmanUserId ?? salesmanOptions.value.find((item) => item.realName === detail.salesmanName)?.userId;
  form.planName = detail.planName;
  form.remark = detail.remark;
  detailStatus.value = detail.status;
  rows.value = detail.items.map((item, index) => {
    const row = {
      id: index + 1,
      itemCode: item.itemCode,
      itemName: item.itemName,
      abnormalFlag: item.abnormalFlag || '',
      spec: item.spec,
      category: item.category,
      unit1ActualQty: item.actualQty ?? item.bookQty ?? 0,
      unit1: item.unitName,
      actualTotalQty: item.actualQty ?? item.bookQty ?? 0,
      actualTotalUnit: item.unitName,
      bookQty: item.bookQty ?? 0,
      bookPrice: item.bookPrice ?? 0,
      profitQty: item.profitQty ?? 0,
      lossQty: item.lossQty ?? 0,
      profitLossReason: item.profitLossReason,
      actualAmount: item.actualAmount ?? null,
      bookAmount: item.bookAmount ?? null,
      profitInboundPrice: item.profitInboundPrice ?? null,
      profitAmount: item.profitAmount ?? null,
      lossOutboundPrice: item.lossOutboundPrice ?? null,
      lossAmount: item.lossAmount ?? null,
      differenceReasonCode: item.differenceReasonCode || '',
      remark: item.remark,
    } as InventoryCheckItemRow;
    syncRowDerived(row);
    return row;
  });
  rowSeed.value = rows.value.length + 1;
};

const loadPermissions = async () => {
  if (!currentOrgId.value) {
    canCreate.value = false;
    canUpdate.value = false;
    canApprove.value = false;
    canUnapprove.value = false;
    return;
  }
  const result = await fetchInventoryCheckPermissionApi('inventory-checks', currentOrgId.value);
  canCreate.value = Boolean(result.canCreate);
  canUpdate.value = Boolean(result.canUpdate);
  canApprove.value = Boolean(result.canApprove);
  canUnapprove.value = Boolean(result.canUnapprove);
};

const loadPageData = async () => {
  pageLoading.value = true;
  hydratingDetail.value = true;
  try {
    await Promise.all([
      loadPermissions(),
      loadWarehouseOptions(),
      loadSalesmanOptions(),
    ]);
    resetForm();
    if (!isCreateMode.value && routeId.value != null && currentOrgId.value) {
      const detail = await fetchInventoryCheckDetailApi('inventory-checks', routeId.value, currentOrgId.value);
      applyDetail(detail);
    }
  } finally {
    hydratingDetail.value = false;
    pageLoading.value = false;
  }
};

const handleBack = () => {
  router.push('/inventory/inventory-checks');
};

const scrollToSection = (key: string) => {
  activeNav.value = key;
  if (key === 'items') {
    itemSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    return;
  }
  basicSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleWarehouseChange = (warehouseId: number) => {
  const matched = warehouseOptions.value.find((item) => item.id === warehouseId);
  form.warehouseName = matched?.name ?? '';
  if (form.checkType === '全仓盘点') {
    void loadFullWarehouseRows();
  }
};

const handleCheckTypeChange = (checkType: CheckTypeOption) => {
  form.checkType = checkType;
  if (checkType === '全仓盘点' && form.warehouseName) {
    void loadFullWarehouseRows();
  }
};

const handleSalesmanChange = (salesmanUserId: number) => {
  const matched = salesmanOptions.value.find((item) => item.userId === salesmanUserId);
  form.salesmanName = matched?.realName ?? '';
};

const addRow = (index?: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  const row = createEmptyRow(rowSeed.value++);
  if (index == null || index < 0 || index >= rows.value.length) {
    rows.value.push(row);
    return;
  }
  rows.value.splice(index + 1, 0, row);
};

const removeRow = (index: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  if (rows.value.length <= 1) {
    rows.value = [createEmptyRow(1)];
    rowSeed.value = 2;
    return;
  }
  rows.value.splice(index, 1);
};

const openItemSelector = async (index?: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  if (!form.warehouseName) {
    ElMessage.warning('请选择仓库');
    return;
  }
  if (!form.checkDate) {
    ElMessage.warning('请选择盘点日期');
    return;
  }
  selectingItemRowIndex.value = typeof index === 'number' ? index : null;
  itemSelectorMode.value = typeof index === 'number' ? 'replace' : 'append';
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

const handleItemSelectorConfirm = async (selectedRows: Array<Record<string, unknown>>) => {
  const picked = selectedRows as ItemCandidate[];
  if (!picked.length) {
    ElMessage.warning('请至少选择一个物品');
    return;
  }
  if (itemSelectorMode.value === 'append') {
    await appendItems(picked);
    itemSelectorVisible.value = false;
    return;
  }
  if (picked.length > 1) {
    ElMessage.warning('当前仅支持选择一个物品');
    return;
  }
  const targetIndex = selectingItemRowIndex.value;
  if (targetIndex == null) {
    ElMessage.warning('未找到目标行，请重试');
    return;
  }
  const targetRow = rows.value[targetIndex];
  if (!targetRow) {
    ElMessage.warning('未找到目标行，请重试');
    return;
  }
  await applyItemToRow(targetRow, picked[0]);
  itemSelectorVisible.value = false;
};

const handleToolbarAction = async (action: string) => {
  if (action === '导出账面清单') {
    handleExportBookList();
    return;
  }
  if (isReadonlyMode.value) {
    return;
  }
  if (action === '选择盘点物品') {
    await openItemSelector();
    return;
  }
  if (action === '新增空行') {
    addRow();
    return;
  }
  if (action === '移除账面数为 0 的物品') {
    rows.value = rows.value.filter((row) => (row.bookQty ?? 0) !== 0);
    if (!rows.value.length) {
      rows.value = [createEmptyRow(rowSeed.value++)];
    }
    return;
  }
  if (action === '实盘数设置为账面数') {
    rows.value.forEach((row) => {
      row.unit1ActualQty = row.bookQty;
      syncRowDerived(row);
    });
    return;
  }
  if (action === '实盘数设置为 0') {
    rows.value.forEach((row) => {
      row.unit1ActualQty = 0;
      syncRowDerived(row);
    });
    return;
  }
  if (action === '排序') {
    rows.value = [...rows.value].sort((left, right) => left.itemCode.localeCompare(right.itemCode));
    return;
  }
};

const formatNumber = (value: number | null, digits: number) => {
  if (value == null) {
    return '-';
  }
  return value.toFixed(digits);
};

const validateForm = () => {
  if (!form.warehouseName) {
    ElMessage.warning('请选择仓库');
    return false;
  }
  if (!form.checkDate) {
    ElMessage.warning('请选择盘点日期');
    return false;
  }
  if (!form.salesmanUserId) {
    ElMessage.warning('请选择业务员');
    return false;
  }
  const validRows = rows.value.filter((row) => row.itemCode);
  if (!validRows.length) {
    ElMessage.warning('请添加盘点物品');
    return false;
  }
  const invalidRow = validRows.find((row) => row.unit1ActualQty == null || row.bookQty == null || row.bookPrice == null);
  if (invalidRow) {
    ElMessage.warning('请完善盘点明细（实盘数、账面数、账面单价）');
    return false;
  }
  const missingReasonRow = validRows.find((row) => {
    const diff = (row.unit1ActualQty ?? 0) - (row.bookQty ?? 0);
    return diff !== 0 && !row.differenceReasonCode;
  });
  if (missingReasonRow) {
    ElMessage.warning('存在盘点差异时请选择差异原因');
    return false;
  }
  return true;
};

const buildSavePayload = (submitted: boolean): InventoryCheckSavePayload => ({
  checkDate: form.checkDate,
  warehouseName: form.warehouseName,
  checkRangeType: checkRangeTypeCodeMap[form.checkType],
  stocktakeFrequency: form.stocktakeFrequency,
  freezeStock: form.freezeStock,
  collaborativeFlag: form.collaborativeFlag,
  planName: form.planName,
  thirdPartyDocument: form.thirdPartyDocument,
  salesmanUserId: form.salesmanUserId,
  salesmanName: form.salesmanName,
  remark: form.remark,
  submitted,
  items: rows.value
    .filter((row) => row.itemCode)
    .map((row) => ({
      itemCode: row.itemCode,
      itemName: row.itemName,
      spec: row.spec,
      category: row.category,
      unitName: row.unit1,
      availableQty: row.bookQty,
      bookQty: row.bookQty,
      actualQty: row.unit1ActualQty,
      bookPrice: row.bookPrice,
      profitLossReason: row.profitLossReason,
      differenceReasonCode: row.differenceReasonCode,
      remark: row.remark,
      extraFields: {},
    })),
});

const handleItemSelectorPageChange = (page: number) => {
  itemSelectorCurrentPage.value = page;
  void loadItemCandidates();
};

const handleItemSelectorPageSizeChange = (size: number) => {
  itemSelectorPageSize.value = size;
  itemSelectorCurrentPage.value = 1;
  void loadItemCandidates();
};

const handleSaveDraft = async () => {
  if (isReadonlyMode.value) {
    return;
  }
  if (!validateForm()) {
    return;
  }
  if (!currentOrgId.value) {
    ElMessage.warning('未选择机构');
    return;
  }
  const payload = buildSavePayload(false);
  if (isCreateMode.value) {
    await createInventoryCheckApi('inventory-checks', payload, currentOrgId.value);
  } else if (routeId.value != null) {
    await updateInventoryCheckApi('inventory-checks', routeId.value, payload, currentOrgId.value);
  }
  ElMessage.success('草稿已保存');
  router.push('/inventory/inventory-checks');
};

const handleSave = async () => {
  if (isReadonlyMode.value) {
    return;
  }
  if (!validateForm()) {
    return;
  }
  if (!currentOrgId.value) {
    ElMessage.warning('未选择机构');
    return;
  }
  const payload = buildSavePayload(true);
  if (isCreateMode.value) {
    await createInventoryCheckApi('inventory-checks', payload, currentOrgId.value);
  } else if (routeId.value != null) {
    await updateInventoryCheckApi('inventory-checks', routeId.value, payload, currentOrgId.value);
  }
  ElMessage.success(`${isEditMode.value ? '编辑' : '新增'}盘点单成功`);
  router.push('/inventory/inventory-checks');
};

watch(
  () => [route.name, route.params.id, sessionStore.currentOrgId],
  () => {
    loadPageData().catch(() => {
      // Global error message handled in http interceptor.
    });
  },
  { immediate: true },
);

watch(
  () => [form.checkDate, form.warehouseName],
  () => {
    if (!isCreateMode.value && !isEditMode.value) {
      return;
    }
    if (hydratingDetail.value) {
      return;
    }
    void refreshBookSnapshots();
  },
);
</script>

<template>
  <div class="item-create-page">
    <FixedActionBreadcrumb
      :navs="sectionNavs"
      :active-key="activeNav"
      :show-actions="!isReadonlyMode"
      @back="handleBack"
      @save-draft="handleSaveDraft"
      @save="handleSave"
      @navigate="scrollToSection"
    />

    <section v-loading="pageLoading" class="panel form-panel">
      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础信息</h3>
        <el-form label-width="150px" class="item-create-form inventory-check-form">
          <div class="item-form-grid inventory-check-basic-grid">
            <el-form-item label="仓库">
              <el-select
                v-model="form.warehouseId"
                placeholder="请选择"
                style="width: 100%"
                :disabled="isReadonlyMode"
                @change="handleWarehouseChange"
              >
                <el-option
                  v-for="option in warehouseOptions"
                  :key="option.id"
                  :label="option.label"
                  :value="option.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="盘点日期">
              <el-date-picker
                v-model="form.checkDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择盘点日期"
                style="width: 100%"
                :disabled="isReadonlyMode"
              />
            </el-form-item>
            <el-form-item label="盘点类型">
              <el-select v-model="form.checkType" style="width: 100%" :disabled="isReadonlyMode" @change="handleCheckTypeChange">
                <el-option
                  v-for="option in checkTypeOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="盘点频次">
              <el-select v-model="form.stocktakeFrequency" style="width: 100%" :disabled="isReadonlyMode">
                <el-option
                  v-for="option in stocktakeFrequencyOptions"
                  :key="option.itemCode"
                  :label="option.itemLabel"
                  :value="option.itemCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="实盘合计、盈亏数、账面数单位">
              <el-select v-model="form.summaryUnit" style="width: 100%" :disabled="isReadonlyMode">
                <el-option
                  v-for="option in summaryUnitOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="冻结库存">
              <el-switch v-model="form.freezeStock" :disabled="isReadonlyMode" active-text="冻结" inactive-text="不冻结" />
            </el-form-item>
            <el-form-item label="协同盘点">
              <el-switch v-model="form.collaborativeFlag" :disabled="isReadonlyMode" active-text="启用" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="第三方单据">
              <div class="readonly-field">{{ form.thirdPartyDocument }}</div>
            </el-form-item>
            <el-form-item label="业务员">
              <el-select
                v-model="form.salesmanUserId"
                placeholder="请选择"
                style="width: 100%"
                :disabled="isReadonlyMode"
                @change="handleSalesmanChange"
              >
                <el-option
                  v-for="option in salesmanSelectOptions"
                  :key="option.userId"
                  :label="option.label"
                  :value="option.userId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="盘点方案">
              <div class="readonly-field">{{ form.planName || '' }}</div>
            </el-form-item>
            <el-form-item label="备注" class="inventory-check-remark-item">
              <el-input v-model="form.remark" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">盘点明细</h3>
        <div class="table-toolbar">
          <el-button v-for="action in [
            '选择盘点物品',
            '新增空行',
            '移除账面数为 0 的物品',
            '实盘数设置为账面数',
            '实盘数设置为 0',
            '排序',
          ]" :key="action" :disabled="isReadonlyMode" @click="handleToolbarAction(action)">
            {{ action }}
          </el-button>
          <el-button :loading="exportLoading" @click="handleToolbarAction('导出账面清单')">
            <el-icon><Download /></el-icon>
            导出账面清单
          </el-button>
        </div>

        <el-table :data="rows" border stripe class="erp-table inventory-check-item-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="96" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" :disabled="isReadonlyMode" @click="addRow($index)">+</el-button>
              <el-button text :disabled="isReadonlyMode" @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="170">
            <template #default="{ row, $index }">
              <el-button text class="item-code-trigger" :disabled="isReadonlyMode" @click="openItemSelector($index)">
                {{ row.itemCode || '点击选择物品' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="130">
            <template #default="{ row }">{{ row.itemName || '-' }}</template>
          </el-table-column>
          <el-table-column label="盈亏异常" min-width="90">
            <template #default="{ row }">
              <span :class="['abnormal-flag', row.abnormalFlag === '异常' ? 'is-abnormal' : '']">
                {{ row.abnormalFlag || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="120">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="120">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column label="单位一实盘数" min-width="120">
            <template #default="{ row }">
              <CommonNumberInput
                v-model="row.unit1ActualQty"
                :min="0"
                :precision="4"
                :disabled="isReadonlyMode"
                @change="syncRowDerived(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="单位一" min-width="90">
            <template #default="{ row }">{{ row.unit1 || '-' }}</template>
          </el-table-column>
          <el-table-column label="实盘合计数" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.actualTotalQty, 4) }}</template>
          </el-table-column>
          <el-table-column label="实盘合计单位" min-width="110">
            <template #default="{ row }">{{ row.actualTotalUnit || '-' }}</template>
          </el-table-column>
          <el-table-column label="账面数" min-width="110">
            <template #default="{ row }">{{ formatNumber(row.bookQty, 4) }}</template>
          </el-table-column>
          <el-table-column label="账面单价" min-width="110">
            <template #default="{ row }">{{ formatNumber(row.bookPrice, 4) }}</template>
          </el-table-column>
          <el-table-column label="盘盈数量" min-width="110">
            <template #default="{ row }">{{ formatNumber(row.profitQty, 4) }}</template>
          </el-table-column>
          <el-table-column label="盘亏数量" min-width="110">
            <template #default="{ row }">{{ formatNumber(row.lossQty, 4) }}</template>
          </el-table-column>
          <el-table-column label="差异原因" min-width="160">
            <template #default="{ row }">
              <el-select
                v-model="row.differenceReasonCode"
                clearable
                placeholder="请选择"
                :disabled="isReadonlyMode || ((row.unit1ActualQty ?? 0) - (row.bookQty ?? 0) === 0)"
                style="width: 100%"
              >
                <el-option
                  v-for="option in differenceReasonOptions"
                  :key="option.itemCode"
                  :label="option.itemLabel"
                  :value="option.itemCode"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="实盘金额" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.actualAmount, 2) }}</template>
          </el-table-column>
          <el-table-column label="账面金额" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.bookAmount, 2) }}</template>
          </el-table-column>
          <el-table-column label="盘盈入库单价" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.profitInboundPrice, 4) }}</template>
          </el-table-column>
          <el-table-column label="盘盈金额" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.profitAmount, 2) }}</template>
          </el-table-column>
          <el-table-column label="盘亏出库单价" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.lossOutboundPrice, 4) }}</template>
          </el-table-column>
          <el-table-column label="盘亏金额" min-width="120">
            <template #default="{ row }">{{ formatNumber(row.lossAmount, 2) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <template #append>
            <div class="inventory-check-summary-row">
              <span class="summary-title">合计</span>
              <span class="summary-cell">实盘金额：{{ totalActualAmount.toFixed(2) }}</span>
              <span class="summary-cell">账面金额：{{ totalBookAmount.toFixed(2) }}</span>
              <span class="summary-cell">盈亏数：{{ totalDiffQty.toFixed(4) }}</span>
            </div>
          </template>
        </el-table>
      </div>
    </section>

    <CommonSelectorDialog
      v-model="itemSelectorVisible"
      title="选择盘点物品"
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
      @page-change="handleItemSelectorPageChange"
      @page-size-change="handleItemSelectorPageSizeChange"
      @confirm="handleItemSelectorConfirm"
    />
  </div>
</template>

<style scoped lang="scss">
.inventory-check-basic-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.inventory-check-remark-item {
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

.inventory-check-form :deep(.el-input__wrapper),
.inventory-check-form :deep(.el-select__wrapper),
.inventory-check-form :deep(.el-textarea__inner) {
  min-height: 22px;
}

.inventory-check-item-table :deep(.common-number-input) {
  width: 100%;
}

.inventory-check-item-table :deep(.el-input__wrapper),
.inventory-check-item-table :deep(.common-number-input),
.inventory-check-item-table :deep(.el-select__wrapper) {
  min-height: 24px;
}

.item-code-trigger {
  padding: 0;
  justify-content: flex-start;
  color: #2563eb;
  font-weight: 500;
}

.abnormal-flag {
  color: #475569;
}

.abnormal-flag.is-abnormal {
  color: #dc2626;
  font-weight: 600;
}

.inventory-check-summary-row {
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

@media (max-width: 1400px) {
  .inventory-check-basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .inventory-check-basic-grid {
    grid-template-columns: 1fr;
  }

  .inventory-check-remark-item {
    grid-column: auto;
  }
}
</style>
