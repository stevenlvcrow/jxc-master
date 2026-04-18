<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
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
import { useSessionStore } from '@/stores/session';

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

type CheckTypeOption = '指定物品' | '全仓盘点';
type UnitOption = '库存单位';

type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  stockUnit: string;
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
  remark: string;
};

type MockDetailLine = {
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  unit1ActualQty: number;
  unit1: string;
  bookQty: number;
  bookPrice: number;
  profitLossReason: string;
  remark: string;
};

type MockDetail = {
  warehouse: string;
  checkDate: string;
  checkType: CheckTypeOption;
  summaryUnit: UnitOption;
  salesmanName: string;
  remark: string;
  items: MockDetailLine[];
};

const checkTypeOptions: CheckTypeOption[] = ['指定物品', '全仓盘点'];
const summaryUnitOptions: UnitOption[] = ['库存单位'];

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);
const pageLoading = ref(false);

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
const isReadonlyMode = computed(() => isViewMode.value);

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
const itemSelectorStatus = ref('启用');
const activeItemTreeId = ref<string>('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);
const itemCandidateSource = ref<ItemCandidate[]>([]);

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
  summaryUnit: '库存单位' as UnitOption,
  thirdPartyDocument: '--',
  salesmanUserId: undefined as number | undefined,
  salesmanName: '',
  planName: '',
  remark: '',
});

const rowSeed = ref(2);
const rows = ref<InventoryCheckItemRow[]>([]);

const mockDetailMap: Record<number, MockDetail> = {
  1: {
    warehouse: '中央成品仓',
    checkDate: '2026-04-13',
    checkType: '指定物品',
    summaryUnit: '库存单位',
    salesmanName: '张敏',
    remark: '月度盘点',
    items: [
      {
        itemCode: 'ITEM-001',
        itemName: '鸡胸肉',
        spec: '2kg/袋',
        category: '肉类',
        unit1ActualQty: 32,
        unit1: '袋',
        bookQty: 30,
        bookPrice: 46.5,
        profitLossReason: '盘盈补录',
        remark: '冷库复盘',
      },
      {
        itemCode: 'ITEM-004',
        itemName: '酸梅汤',
        spec: '500ml*12瓶',
        category: '饮品',
        unit1ActualQty: 10,
        unit1: '箱',
        bookQty: 12,
        bookPrice: 72,
        profitLossReason: '破损报损',
        remark: '货架清点',
      },
    ],
  },
  2: {
    warehouse: '北区原料仓',
    checkDate: '2026-04-12',
    checkType: '指定物品',
    summaryUnit: '库存单位',
    salesmanName: '李娜',
    remark: '抽盘复核',
    items: [
      {
        itemCode: 'ITEM-009',
        itemName: '牛腩',
        spec: '5kg/箱',
        category: '冻品',
        unit1ActualQty: 8,
        unit1: '箱',
        bookQty: 9,
        bookPrice: 168,
        profitLossReason: '称重损耗',
        remark: '冻库盘点',
      },
    ],
  },
  3: {
    warehouse: '南区包材仓',
    checkDate: '2026-04-11',
    checkType: '指定物品',
    summaryUnit: '库存单位',
    salesmanName: '王磊',
    remark: '循环盘点',
    items: [
      {
        itemCode: 'ITEM-015',
        itemName: '包装盒',
        spec: '200只/箱',
        category: '包材',
        unit1ActualQty: 15,
        unit1: '箱',
        bookQty: 15,
        bookPrice: 68,
        profitLossReason: '',
        remark: '包装区复盘',
      },
    ],
  },
};

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
  } else {
    row.abnormalFlag = '异常';
  }
};

const totalActualAmount = computed(() => rows.value.reduce((sum, row) => sum + (row.actualAmount ?? 0), 0));
const totalBookAmount = computed(() => rows.value.reduce((sum, row) => sum + (row.bookAmount ?? 0), 0));
const totalDiffQty = computed(() => rows.value.reduce((sum, row) => sum + (row.profitQty ?? 0) - (row.lossQty ?? 0), 0));

const resolveOrgId = () => {
  const currentOrgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!currentOrgId || !currentOrgId.startsWith('store-')) {
    return undefined;
  }
  return currentOrgId;
};

const resolveWarehouseStoreId = () => {
  const currentOrgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!currentOrgId) {
    return undefined;
  }
  if (currentOrgId.startsWith('store-')) {
    const storeId = Number(currentOrgId.slice('store-'.length));
    return Number.isNaN(storeId) ? undefined : storeId;
  }
  const currentOrg = sessionStore.currentOrg;
  if (currentOrg?.type === 'group') {
    const firstStore = currentOrg.children?.[0];
    if (!firstStore) {
      return undefined;
    }
    const storeId = Number(String(firstStore.id).slice('store-'.length));
    return Number.isNaN(storeId) ? undefined : storeId;
  }
  const firstStore = sessionStore.flatOrgs.find((item) => item.type === 'store');
  if (!firstStore) {
    return undefined;
  }
  const storeId = Number(String(firstStore.id).slice('store-'.length));
  return Number.isNaN(storeId) ? undefined : storeId;
};

const resetForm = () => {
  form.warehouseId = 0;
  form.warehouseName = '';
  form.checkDate = '';
  form.checkType = '指定物品';
  form.summaryUnit = '库存单位';
  form.thirdPartyDocument = '--';
  form.salesmanUserId = undefined;
  form.salesmanName = '';
  form.planName = '';
  form.remark = '';
  rowSeed.value = 2;
  rows.value = [createEmptyRow(1)];
};

const loadWarehouseOptions = async () => {
  const storeId = resolveWarehouseStoreId();
  if (!storeId) {
    warehouseOptions.value = [];
    return;
  }
  const result = await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' });
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
    ? normalized.filter((item) => item.phone === sessionStore.loginAccount)
    : normalized;
  if (isCreateMode.value && !form.salesmanUserId) {
    const selfCandidate = salesmanOptions.value.find((item) => item.phone === sessionStore.loginAccount);
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
    }, orgId);
    itemCandidateSource.value = page.list.map(mapItemCandidate);
    itemSelectorTotal.value = Number(page.total ?? 0);
  } finally {
    itemSelectorLoading.value = false;
  }
};

const resolveMockBookSnapshot = (itemCode: string, rowIndex: number) => {
  const detailItems = Object.values(mockDetailMap).flatMap((detail) => detail.items);
  const matched = detailItems.find((item) => item.itemCode === itemCode);
  if (matched) {
    return {
      bookQty: matched.bookQty,
      bookPrice: matched.bookPrice,
    };
  }
  return {
    bookQty: 10 + rowIndex * 2,
    bookPrice: 20 + rowIndex * 5,
  };
};

const applyItemToRow = (row: InventoryCheckItemRow, item: ItemCandidate, rowIndex: number) => {
  const snapshot = resolveMockBookSnapshot(item.code, rowIndex);
  row.itemCode = item.code;
  row.itemName = item.name;
  row.spec = item.spec;
  row.category = item.category;
  row.unit1 = item.stockUnit;
  row.actualTotalUnit = item.stockUnit;
  row.bookQty = snapshot.bookQty;
  row.bookPrice = snapshot.bookPrice;
  syncRowDerived(row);
};

const appendItems = (items: ItemCandidate[]) => {
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
  appendable.forEach((item, index) => {
    const row = createEmptyRow(rowSeed.value++);
    applyItemToRow(row, item, rows.value.length + index);
    rows.value.push(row);
  });
  itemSelectorVisible.value = false;
  ElMessage.success(`已添加 ${appendable.length} 条盘点物品`);
};

const applyDetail = (detail: MockDetail) => {
  form.warehouseName = detail.warehouse;
  form.warehouseId = warehouseOptions.value.find((item) => item.name === detail.warehouse)?.id ?? 0;
  form.checkDate = detail.checkDate;
  form.checkType = detail.checkType;
  form.summaryUnit = detail.summaryUnit;
  form.salesmanName = detail.salesmanName;
  form.salesmanUserId = salesmanOptions.value.find((item) => item.realName === detail.salesmanName)?.userId;
  form.remark = detail.remark;
  rows.value = detail.items.map((item, index) => {
    const row = {
      id: index + 1,
      itemCode: item.itemCode,
      itemName: item.itemName,
      abnormalFlag: '',
      spec: item.spec,
      category: item.category,
      unit1ActualQty: item.unit1ActualQty,
      unit1: item.unit1,
      actualTotalQty: item.unit1ActualQty,
      actualTotalUnit: item.unit1,
      bookQty: item.bookQty,
      bookPrice: item.bookPrice,
      profitQty: 0,
      lossQty: 0,
      profitLossReason: item.profitLossReason,
      actualAmount: null,
      bookAmount: null,
      profitInboundPrice: null,
      profitAmount: null,
      lossOutboundPrice: null,
      lossAmount: null,
      remark: item.remark,
    } as InventoryCheckItemRow;
    syncRowDerived(row);
    return row;
  });
  rowSeed.value = rows.value.length + 1;
};

const loadPageData = async () => {
  pageLoading.value = true;
  try {
    await Promise.all([
      loadWarehouseOptions(),
      loadSalesmanOptions(),
    ]);
    resetForm();
    if (!isCreateMode.value && routeId.value != null) {
      const detail = mockDetailMap[routeId.value];
      if (detail) {
        applyDetail(detail);
      } else {
        ElMessage.warning('未找到对应盘点单，已返回列表');
        router.replace('/inventory/2/1');
      }
    }
  } finally {
    pageLoading.value = false;
  }
};

const handleBack = () => {
  router.push('/inventory/2/1');
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

const openItemSelector = async () => {
  if (isReadonlyMode.value) {
    return;
  }
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

const handleItemSelectorConfirm = (selectedRows: Array<Record<string, unknown>>) => {
  appendItems(selectedRows as ItemCandidate[]);
};

const handleToolbarAction = async (action: string) => {
  if (isReadonlyMode.value) {
    return;
  }
  if (action === '添加物品') {
    await openItemSelector();
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
  if (action === '添加有账未盘物品') {
    const candidates = itemCandidateSource.value.length ? itemCandidateSource.value : [
      { id: 'BOOK-001', code: 'BOOK-001', name: '有账未盘物品A', spec: '标准', category: '补盘', stockUnit: '个', status: '启用' },
      { id: 'BOOK-002', code: 'BOOK-002', name: '有账未盘物品B', spec: '标准', category: '补盘', stockUnit: '箱', status: '启用' },
    ];
    appendItems(candidates.slice(0, 2));
    return;
  }
  if (action === '排序') {
    rows.value = [...rows.value].sort((left, right) => left.itemCode.localeCompare(right.itemCode));
    return;
  }
  ElMessage.info(`${action}功能待接入`);
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
  return true;
};

const handleSaveDraft = () => {
  ElMessage.info('草稿功能待接口接入');
};

const handleSave = () => {
  if (isReadonlyMode.value) {
    return;
  }
  if (!validateForm()) {
    return;
  }
  const documentCode = `PD-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-001`;
  ElMessage.success(`${isEditMode.value ? '编辑' : '新增'}盘点单成功：${documentCode}`);
  router.push('/inventory/2/1');
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
              <el-select v-model="form.checkType" style="width: 100%" :disabled="isReadonlyMode">
                <el-option
                  v-for="option in checkTypeOptions"
                  :key="option"
                  :label="option"
                  :value="option"
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
            '添加物品',
            '导出盘点物品',
            '导入盘点结果',
            '通过模板新建',
            '移除账面数为 0 的物品',
            '实盘数设置为账面数',
            '实盘数设置为 0',
            '添加有账未盘物品',
            '加工品盘点',
            '加工品导入',
            '排序',
          ]" :key="action" :disabled="isReadonlyMode" @click="handleToolbarAction(action)">
            {{ action }}
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
            <template #default="{ row }">{{ row.itemCode || '-' }}</template>
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
              <el-input-number
                v-model="row.unit1ActualQty"
                :min="0"
                :precision="4"
                :step="1"
                controls-position="right"
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
          <el-table-column label="盈亏原因" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.profitLossReason" placeholder="请输入盈亏原因" :disabled="isReadonlyMode" />
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
      :status-options="[
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
      ]"
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

.inventory-check-item-table :deep(.el-input-number) {
  width: 100%;
}

.inventory-check-item-table :deep(.el-input__wrapper),
.inventory-check-item-table :deep(.el-input-number),
.inventory-check-item-table :deep(.el-select__wrapper) {
  min-height: 24px;
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
