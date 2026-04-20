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
  createPurchaseInboundApi,
  fetchPurchaseInboundDetailApi,
  updatePurchaseInboundApi,
  type PurchaseInboundDetail,
} from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { fetchCurrentUserRolesApi } from '@/api/modules/auth';
import { fetchSuppliersApi, type SupplierListRow } from '@/api/modules/supplier';
import { fetchStoreSalesmenApi, type SalesmanCandidateItem } from '@/api/modules/system-admin';
import { fetchStoreWarehousesApi, type WarehouseRow as ApiWarehouseRow } from '@/api/modules/warehouse';
import { fetchPurchaseInboundPermissionApi } from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';

type SupplierOption = {
  id: number;
  name: string;
  code: string;
  contact: string;
};

type ItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  warehouse: string;
  purchaseUnit: string;
  quantity: number | null;
  inboundPrice: number | null;
  amount: number | null;
  gift: boolean;
  remark: string;
};

type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  purchaseUnit: string;
  status: string;
};

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

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);

const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '物品信息' },
];

const inboundId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const isCreateMode = computed(() => route.name === 'PurchaseInboundCreate');
const isViewMode = computed(() => route.name === 'PurchaseInboundView');
const isEditMode = computed(() => route.name === 'PurchaseInboundEdit');
const detailStatus = ref('');
const canCreate = ref(false);
const canUpdate = ref(false);
const isReadonlyMode = computed(() => {
  if (isViewMode.value || detailStatus.value === '已审核') {
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

const supplierOptions = ref<SupplierOption[]>([]);
const warehouseOptions = ref<WarehouseOption[]>([]);
const purchaseUnitOptions = ['斤', '箱', '袋', '个', '瓶'];
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
const selectingItemRowIndex = ref<number | null>(null);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);
const itemCandidateSource = ref<ItemCandidate[]>([]);
const formLoading = ref(false);

const resolvePurchaseInboundOrgId = () => {
  const currentOrgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!currentOrgId || !currentOrgId.startsWith('store-')) {
    return undefined;
  }
  return currentOrgId;
};

const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'category', label: '物品类别', minWidth: 120 },
  { prop: 'purchaseUnit', label: '采购单位', minWidth: 100 },
  { prop: 'status', label: '状态', minWidth: 80 },
];

const form = reactive({
  supplierId: 0,
  supplierName: '',
  inboundDate: '',
  salesmanUserId: undefined as number | undefined,
  salesmanName: '',
  remark: '',
  rejectionReason: '',
});

const rowSeed = ref(2);
const rows = ref<ItemRow[]>([
  {
    id: 1,
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    warehouse: '',
    purchaseUnit: '',
    quantity: null,
    inboundPrice: null,
    amount: null,
    gift: false,
    remark: '',
  },
]);

const batchWarehouseDialogVisible = ref(false);
const batchWarehouse = ref('');

const createEmptyRow = (id: number): ItemRow => ({
  id,
  itemCode: '',
  itemName: '',
  spec: '',
  category: '',
  warehouse: '',
  purchaseUnit: '',
  quantity: null,
  inboundPrice: null,
  amount: null,
  gift: false,
  remark: '',
});

const resetForm = () => {
  detailStatus.value = '';
  form.supplierId = 0;
  form.supplierName = '';
  form.inboundDate = '';
  form.salesmanUserId = undefined;
  form.salesmanName = '';
  form.remark = '';
  form.rejectionReason = '';
  rowSeed.value = 2;
  rows.value = [createEmptyRow(1)];
};

const applyDetail = (detail: PurchaseInboundDetail) => {
  detailStatus.value = detail.status ?? '';
  form.inboundDate = detail.inboundDate ?? '';
  form.remark = detail.remark ?? '';
  form.rejectionReason = detail.rejectionReason ?? '';
  form.salesmanUserId = detail.salesmanUserId ?? undefined;
  form.salesmanName = detail.salesmanName ?? '';
  form.supplierName = detail.supplier ?? '';
  form.supplierId = supplierOptions.value.find((item) =>
    item.name === detail.supplier || `${item.name} / ${item.code}` === detail.supplier,
  )?.id ?? 0;
  rows.value = (detail.items?.length ? detail.items : [null]).map((item, index) => {
    if (!item) {
      return createEmptyRow(index + 1);
    }
    return {
      id: index + 1,
      itemCode: item.itemCode,
      itemName: item.itemName,
      spec: '',
      category: '',
      warehouse: detail.warehouse ?? '',
      purchaseUnit: '',
      quantity: item.quantity ?? null,
      inboundPrice: item.unitPrice ?? null,
      amount: null,
      gift: false,
      remark: '',
    } as ItemRow;
  });
};

const resolveOrgId = () => {
  return resolvePurchaseInboundOrgId();
};

const resolveWarehouseStoreId = () => {
  const currentOrgId = (sessionStore.currentOrgId ?? '').trim().toLowerCase();
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

const loadSupplierOptions = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
    supplierOptions.value = [];
    return;
  }
  const result = await fetchSuppliersApi({
    pageNo: 1,
    pageSize: 200,
  }, orgId);
  supplierOptions.value = (result.list ?? [])
    .map((item: SupplierListRow) => ({
      id: item.id,
      name: item.supplierName,
      code: item.supplierCode,
      contact: item.contactPerson ?? '',
    }))
    .sort((left, right) => left.name.localeCompare(right.name, 'zh-Hans-CN'));
};

const loadWarehouseOptions = async () => {
  const storeId = resolveWarehouseStoreId();
  if (!storeId) {
    warehouseOptions.value = [];
    return;
  }
  const rows = await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' });
  warehouseOptions.value = rows
    .map((item: ApiWarehouseRow) => ({
      id: item.id,
      name: item.warehouseName,
      code: item.warehouseCode,
      label: `${item.warehouseName} / ${item.warehouseCode}`,
    }))
    .sort((left, right) => left.name.localeCompare(right.name, 'zh-Hans-CN'));
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
  purchaseUnit: row.purchaseUnit,
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
  const options = userList.map((user: SalesmanCandidateItem) => ({
    userId: user.userId,
    realName: user.realName,
    phone: user.phone,
    label: `${user.realName} / ${user.phone}`,
  }));
  const normalizedOptions = Array.from(new Map(options.map((item) => [item.userId, item])).values());
  salesmanOptions.value = isSalesman
    ? normalizedOptions.filter((item) => item.phone === sessionStore.loginAccount)
    : normalizedOptions;
  if (isCreateMode.value && !isReadonlyMode.value && form.salesmanUserId == null) {
    const selfCandidate = salesmanOptions.value.find((item) => item.phone && item.phone === sessionStore.loginAccount);
    if (selfCandidate) {
      form.salesmanUserId = selfCandidate.userId;
      form.salesmanName = selfCandidate.realName;
      return;
    }
    if (isSalesman && salesmanOptions.value.length === 1) {
      form.salesmanUserId = salesmanOptions.value[0].userId;
      form.salesmanName = salesmanOptions.value[0].realName;
    }
  }
};

const loadPermission = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
    canCreate.value = false;
    canUpdate.value = false;
    return;
  }
  try {
    const result = await fetchPurchaseInboundPermissionApi(orgId);
    canCreate.value = Boolean(result.canCreate);
    canUpdate.value = Boolean(result.canUpdate);
  } catch {
    canCreate.value = false;
    canUpdate.value = false;
  }
};

const loadDetail = async () => {
  if (inboundId.value == null) {
    resetForm();
    return;
  }
  const orgId = resolveOrgId();
  if (!orgId) {
    resetForm();
    return;
  }
  formLoading.value = true;
  try {
    const detail = await fetchPurchaseInboundDetailApi(inboundId.value, orgId);
    applyDetail(detail);
  } finally {
    formLoading.value = false;
  }
};

const loadPageData = async () => {
  resetForm();
  await loadPermission();
  await Promise.all([
    loadSupplierOptions(),
    loadWarehouseOptions(),
    loadSalesmanOptions(),
    loadItemTree(),
  ]);
  await loadDetail();
};

const handleBack = () => {
  router.push('/inventory/1/2');
};

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleSupplierChange = (supplierId: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  const supplier = supplierOptions.value.find((item) => item.id === supplierId);
  if (!supplier) {
    form.supplierName = '';
    return;
  }
  form.supplierId = supplier.id;
  form.supplierName = supplier.name;
};

const openItemSelector = async (index: number) => {
  if (isReadonlyMode.value) {
    return;
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
  loadItemCandidates();
};

const handleItemNodeChange = (node: SelectorTreeNode | null) => {
  activeItemTreeId.value = String(node?.id ?? 'all');
  itemSelectorCurrentPage.value = 1;
  loadItemCandidates();
};

const handleItemSelectionChange = (rows: Array<Record<string, unknown>>) => {
  selectedItemCandidates.value = rows;
};

const handleItemClear = () => {
  selectedItemCandidates.value = [];
};

const applyItemToRow = (row: ItemRow, item: ItemCandidate) => {
  row.itemCode = item.code;
  row.itemName = item.name;
  row.spec = item.spec;
  row.category = item.category;
  if (!row.purchaseUnit) {
    row.purchaseUnit = item.purchaseUnit;
  }
};

const handleItemSelectorConfirm = (selectedRows: Array<Record<string, unknown>>) => {
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
  applyItemToRow(targetRow, picked[0]);
  itemSelectorVisible.value = false;
};

const handleSalesmanChange = (userId: number | undefined) => {
  if (isReadonlyMode.value) {
    return;
  }
  if (userId == null) {
    form.salesmanUserId = undefined;
    form.salesmanName = '';
    return;
  }
  const target = salesmanOptions.value.find((item) => item.userId === userId);
  form.salesmanUserId = userId;
  form.salesmanName = target?.realName ?? '';
};

const addRow = (index?: number) => {
  if (isReadonlyMode.value) {
    return;
  }
  const targetIndex = typeof index === 'number' ? index + 1 : rows.value.length;
  rows.value.splice(targetIndex, 0, {
    id: rowSeed.value,
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    warehouse: '',
    purchaseUnit: '',
    quantity: null,
    inboundPrice: null,
    amount: null,
    gift: false,
    remark: '',
  });
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

const totalQuantity = computed(() => rows.value.reduce((sum, row) => sum + (row.quantity ?? 0), 0));
const totalAmount = computed(() => rows.value.reduce((sum, row) => sum + (row.amount ?? 0), 0));

const handleToolbarAction = async (action: string) => {
  if (isReadonlyMode.value && action !== '返回') {
    ElMessage.info('当前单据为查看状态，不能编辑');
    return;
  }
  if (action === '添加物品') {
    addRow();
    return;
  }
  if (action === '通过模板新建') {
    const orgId = resolveOrgId();
    if (!orgId) {
      ElMessage.warning('请选择门店后再操作');
      return;
    }
    const page = await fetchItemsApi({
      pageNo: 1,
      pageSize: 2,
    }, orgId);
    const templateItems = page.list.map(mapItemCandidate);
    rows.value = [
      {
        id: rowSeed.value++,
        itemCode: templateItems[0]?.code ?? '',
        itemName: templateItems[0]?.name ?? '',
        spec: templateItems[0]?.spec ?? '',
        category: templateItems[0]?.category ?? '',
        warehouse: '中央成品仓',
        purchaseUnit: templateItems[0]?.purchaseUnit ?? '袋',
        quantity: 10,
        inboundPrice: 46.5,
        amount: 465,
        gift: false,
        remark: '',
      },
      {
        id: rowSeed.value++,
        itemCode: templateItems[1]?.code ?? '',
        itemName: templateItems[1]?.name ?? '',
        spec: templateItems[1]?.spec ?? '',
        category: templateItems[1]?.category ?? '',
        warehouse: '南区包材仓',
        purchaseUnit: templateItems[1]?.purchaseUnit ?? '箱',
        quantity: 6,
        inboundPrice: 68,
        amount: 408,
        gift: false,
        remark: '',
      },
    ];
    ElMessage.success('已通过模板填充示例物品');
    return;
  }
  if (action === '批量选择仓库') {
    batchWarehouse.value = '';
    batchWarehouseDialogVisible.value = true;
    return;
  }
  if (action === '批量导入物品') {
    const orgId = resolveOrgId();
    if (!orgId) {
      ElMessage.warning('请选择门店后再操作');
      return;
    }
    const page = await fetchItemsApi({
      pageNo: 1,
      pageSize: 2,
    }, orgId);
    const imported = page.list.map((item) => ({
      id: rowSeed.value++,
      itemCode: item.code,
      itemName: item.name,
      spec: item.spec,
      category: item.category,
      warehouse: '',
      purchaseUnit: item.purchaseUnit,
      quantity: null,
      inboundPrice: null,
      amount: null,
      gift: false,
      remark: '',
    }));
    rows.value.push(...imported);
    ElMessage.success(`已导入 ${imported.length} 条物品`);
  }
};

const confirmBatchWarehouse = () => {
  if (isReadonlyMode.value) {
    return;
  }
  if (!batchWarehouse.value) {
    ElMessage.warning('请选择仓库');
    return;
  }
  rows.value.forEach((row) => {
    row.warehouse = batchWarehouse.value;
  });
  batchWarehouseDialogVisible.value = false;
};

const resolveSingleWarehouse = () => {
  const warehouses = Array.from(new Set(rows.value.map((row) => row.warehouse).filter((value) => !!value)));
  if (!warehouses.length) {
    return '';
  }
  if (warehouses.length > 1) {
    return null;
  }
  return warehouses[0];
};

const validateForm = () => {
  if (!form.supplierName) {
    ElMessage.warning('请选择供应商');
    return false;
  }
  if (!form.inboundDate) {
    ElMessage.warning('请选择入库日期');
    return false;
  }
  if (isCreateMode.value && !form.salesmanUserId) {
    ElMessage.warning('请选择业务员');
    return false;
  }
  if (!rows.value.length) {
    ElMessage.warning('请添加物品');
    return false;
  }
  const invalidRow = rows.value.find((row) => !row.itemCode || !row.itemName || !row.warehouse || !row.purchaseUnit || !row.quantity);
  if (invalidRow) {
    ElMessage.warning('请完善物品信息（编码、名称、仓库、单位、数量）');
    return false;
  }
  if (resolveSingleWarehouse() === null) {
    ElMessage.warning('当前接口仅支持单仓入库，请将所有物品统一为同一仓库');
    return false;
  }
  return true;
};

const handleSaveDraft = () => {
  ElMessage.info('当前版本仅支持直接保存入库单，草稿功能待接入');
};

const handleSave = async () => {
  if (isReadonlyMode.value) {
    return;
  }
  if (!validateForm()) {
    return;
  }
  const orgId = resolveOrgId();
  if (!orgId) {
    ElMessage.warning('请选择门店后再保存');
    return;
  }
  const singleWarehouse = resolveSingleWarehouse();
  if (!singleWarehouse) {
    ElMessage.warning('请选择仓库');
    return;
  }
  const payload = {
    inboundDate: form.inboundDate,
    warehouse: singleWarehouse,
    supplier: form.supplierName,
    salesmanUserId: form.salesmanUserId,
    salesmanName: form.salesmanName,
    remark: form.remark || undefined,
    items: rows.value.map((row) => ({
      itemCode: row.itemCode,
      itemName: row.itemName,
      quantity: Number(row.quantity ?? 0),
      unitPrice: Number(row.inboundPrice ?? 0),
      taxRate: 13,
    })),
  };
  if (isEditMode.value && inboundId.value != null) {
    await updatePurchaseInboundApi(inboundId.value, payload, orgId);
    ElMessage.success('保存成功');
  } else {
    const created = await createPurchaseInboundApi(payload, orgId);
    ElMessage.success(`保存成功：${created.documentCode}`);
  }
  router.push('/inventory/1/2');
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
      :navs="navs"
      :active-key="activeNav"
      :show-actions="!isReadonlyMode"
      @back="handleBack"
      @save-draft="handleSaveDraft"
      @save="handleSave"
      @navigate="scrollToSection"
    />

    <section class="panel form-panel">
      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础信息</h3>
        <el-form label-width="96px" class="item-create-form purchase-inbound-create-form">
          <div class="item-form-grid purchase-inbound-basic-grid">
            <el-form-item label="单据编号">
              <div class="readonly-field">保存后生成</div>
            </el-form-item>
            <el-form-item label="供应商">
              <el-select
                v-model="form.supplierId"
                placeholder="请选择供应商"
                style="width: 100%"
                :disabled="isReadonlyMode"
                @change="handleSupplierChange"
              >
                <el-option
                  v-for="option in supplierOptions"
                  :key="option.id"
                  :label="`${option.name} / ${option.code}`"
                  :value="option.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="入库日期">
              <el-date-picker
                v-model="form.inboundDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择入库日期"
                style="width: 100%"
                :disabled="isReadonlyMode"
              />
            </el-form-item>
            <el-form-item label="业务员">
              <el-select
                v-model="form.salesmanUserId"
                placeholder="请选择业务员"
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
            <el-form-item label="备注" class="purchase-inbound-remark-item">
              <el-input v-model="form.remark" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </el-form-item>
            <el-form-item v-if="form.rejectionReason" label="拒审原因" class="purchase-inbound-remark-item">
              <el-input v-model="form.rejectionReason" readonly />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">物品信息</h3>
        <div class="table-toolbar">
          <el-button type="primary" plain :disabled="isReadonlyMode" @click="handleToolbarAction('添加物品')">添加物品</el-button>
          <el-button :disabled="isReadonlyMode" @click="handleToolbarAction('通过模板新建')">通过模板新建</el-button>
          <el-button :disabled="isReadonlyMode" @click="handleToolbarAction('批量选择仓库')">批量选择仓库</el-button>
          <el-button :disabled="isReadonlyMode" @click="handleToolbarAction('批量导入物品')">批量导入物品</el-button>
        </div>

        <el-table :data="rows" border stripe class="erp-table purchase-inbound-item-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="96" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" :disabled="isReadonlyMode" @click="addRow($index)">+</el-button>
              <el-button text :disabled="isReadonlyMode" @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="180">
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
          <el-table-column label="物品名称" min-width="130">
            <template #default="{ row }">{{ row.itemName || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="120">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="110">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column label="仓库" min-width="140">
            <template #default="{ row }">
              <el-select v-model="row.warehouse" placeholder="请选择仓库" :disabled="isReadonlyMode">
                <el-option
                  v-for="option in warehouseOptions"
                  :key="option.id"
                  :label="option.label"
                  :value="option.name"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="采购单位" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.purchaseUnit" placeholder="请选择单位" :disabled="isReadonlyMode">
                <el-option v-for="option in purchaseUnitOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" min-width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="4" :step="1" controls-position="right" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="入库单价" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.inboundPrice" :min="0" :precision="4" :step="1" controls-position="right" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="金额" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="0" :precision="2" :step="1" controls-position="right" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="是否赠品" min-width="96">
            <template #default="{ row }">
              <el-switch v-model="row.gift" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" :disabled="isReadonlyMode" />
            </template>
          </el-table-column>
          <template #append>
            <div class="purchase-inbound-summary-row">
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
      @page-change="(p) => { itemSelectorCurrentPage = p; loadItemCandidates(); }"
      @page-size-change="(s) => { itemSelectorPageSize = s; itemSelectorCurrentPage = 1; loadItemCandidates(); }"
      @confirm="handleItemSelectorConfirm"
    />

    <el-dialog
      v-model="batchWarehouseDialogVisible"
      title="批量选择仓库"
      width="420px"
      class="standard-form-dialog"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="仓库">
          <el-select v-model="batchWarehouse" placeholder="请选择仓库" style="width: 100%">
            <el-option
              v-for="option in warehouseOptions"
              :key="option.id"
              :label="option.label"
              :value="option.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchWarehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchWarehouse">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.purchase-inbound-basic-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.purchase-inbound-remark-item {
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

.purchase-inbound-create-form :deep(.el-input__wrapper),
.purchase-inbound-create-form :deep(.el-select__wrapper),
.purchase-inbound-create-form :deep(.el-textarea__inner) {
  min-height: 22px;
}

.purchase-inbound-create-form :deep(.el-form-item__label) {
  font-size: 11px;
}

.purchase-inbound-create-form :deep(.el-input__inner),
.purchase-inbound-create-form :deep(.el-select__selected-item),
.purchase-inbound-create-form :deep(.el-date-editor .el-input__inner) {
  font-size: 11px;
}

.purchase-inbound-item-table :deep(.el-input-number) {
  width: 100%;
}

.purchase-inbound-item-table :deep(.el-input__wrapper),
.purchase-inbound-item-table :deep(.el-select__wrapper) {
  min-height: 24px;
}

.purchase-inbound-item-table :deep(.item-code-picker .el-input__wrapper) {
  cursor: pointer;
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

@media (max-width: 1200px) {
  .purchase-inbound-basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .purchase-inbound-basic-grid {
    grid-template-columns: 1fr;
  }

  .purchase-inbound-remark-item {
    grid-column: auto;
  }
}
</style>
