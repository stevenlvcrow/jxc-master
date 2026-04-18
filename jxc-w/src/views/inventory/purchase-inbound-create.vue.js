/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonSelectorDialog from '@/components/CommonSelectorDialog.vue';
import { createPurchaseInboundApi, fetchPurchaseInboundDetailApi, updatePurchaseInboundApi, } from '@/api/modules/inventory';
import { fetchItemCategoryTreeApi, fetchItemsApi, } from '@/api/modules/item';
import { fetchCurrentUserRolesApi } from '@/api/modules/auth';
import { fetchSuppliersApi } from '@/api/modules/supplier';
import { fetchStoreSalesmenApi } from '@/api/modules/system-admin';
import { fetchStoreWarehousesApi } from '@/api/modules/warehouse';
import { fetchPurchaseInboundPermissionApi } from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';
const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const activeNav = ref('basic');
const basicSectionRef = ref(null);
const itemSectionRef = ref(null);
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
const supplierOptions = ref([]);
const warehouseOptions = ref([]);
const purchaseUnitOptions = ['斤', '箱', '袋', '个', '瓶'];
const salesmanOptions = ref([]);
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
const activeItemTreeId = ref('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingItemRowIndex = ref(null);
const selectedItemCandidates = ref([]);
const itemTreeData = ref([]);
const itemCandidateSource = ref([]);
const formLoading = ref(false);
const itemTableColumns = [
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
    salesmanUserId: undefined,
    salesmanName: '',
    remark: '',
});
const rowSeed = ref(2);
const rows = ref([
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
const createEmptyRow = (id) => ({
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
    rowSeed.value = 2;
    rows.value = [createEmptyRow(1)];
};
const applyDetail = (detail) => {
    detailStatus.value = detail.status ?? '';
    form.inboundDate = detail.inboundDate ?? '';
    form.remark = detail.remark ?? '';
    form.salesmanUserId = detail.salesmanUserId ?? undefined;
    form.salesmanName = detail.salesmanName ?? '';
    form.supplierName = detail.supplier ?? '';
    form.supplierId = supplierOptions.value.find((item) => item.name === detail.supplier || `${item.name} / ${item.code}` === detail.supplier)?.id ?? 0;
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
        };
    });
};
const resolveOrgId = () => {
    const orgId = (sessionStore.currentOrgId ?? '').trim();
    if (!orgId) {
        return undefined;
    }
    if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
        return orgId;
    }
    return undefined;
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
    const result = await fetchSuppliersApi({
        pageNo: 1,
        pageSize: 200,
    }, resolveOrgId());
    supplierOptions.value = (result.list ?? [])
        .map((item) => ({
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
        .map((item) => ({
        id: item.id,
        name: item.warehouseName,
        code: item.warehouseCode,
        label: `${item.warehouseName} / ${item.warehouseCode}`,
    }))
        .sort((left, right) => left.name.localeCompare(right.name, 'zh-Hans-CN'));
};
const normalizeItemTreeNodes = (nodes) => nodes.map((node) => ({
    id: String(node.label ?? 'all'),
    label: String(node.label ?? ''),
    children: Array.isArray(node.children) ? normalizeItemTreeNodes(node.children) : undefined,
}));
const loadItemTree = async () => {
    const tree = await fetchItemCategoryTreeApi(resolveOrgId());
    if (!Array.isArray(tree) || !tree.length) {
        itemTreeData.value = [{ id: 'all', label: '全部' }];
        return;
    }
    itemTreeData.value = [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }];
};
const mapItemCandidate = (row) => ({
    id: row.id || row.code,
    code: row.code,
    name: row.name,
    spec: row.spec,
    category: row.category,
    purchaseUnit: row.purchaseUnit,
    status: row.status,
});
const loadItemCandidates = async () => {
    itemSelectorLoading.value = true;
    try {
        const page = await fetchItemsApi({
            pageNo: itemSelectorCurrentPage.value,
            pageSize: itemSelectorPageSize.value,
            keyword: itemSelectorKeyword.value.trim() || undefined,
            category: activeItemTreeId.value === 'all' ? undefined : activeItemTreeId.value,
            status: itemSelectorStatus.value || undefined,
        }, resolveOrgId());
        itemCandidateSource.value = page.list.map(mapItemCandidate);
        itemSelectorTotal.value = Number(page.total ?? 0);
    }
    finally {
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
    const options = userList.map((user) => ({
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
    try {
        const result = await fetchPurchaseInboundPermissionApi(resolveOrgId());
        canCreate.value = Boolean(result.canCreate);
        canUpdate.value = Boolean(result.canUpdate);
    }
    catch {
        canCreate.value = false;
        canUpdate.value = false;
    }
};
const loadDetail = async () => {
    if (inboundId.value == null) {
        resetForm();
        return;
    }
    formLoading.value = true;
    try {
        const detail = await fetchPurchaseInboundDetailApi(inboundId.value, resolveOrgId());
        applyDetail(detail);
    }
    finally {
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
const scrollToSection = (key) => {
    activeNav.value = key;
    const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
const handleSupplierChange = (supplierId) => {
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
const openItemSelector = async (index) => {
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
const handleItemSelectorSearch = (payload) => {
    itemSelectorKeyword.value = payload.keyword;
    itemSelectorStatus.value = payload.status;
    itemSelectorCurrentPage.value = 1;
    loadItemCandidates();
};
const handleItemNodeChange = (node) => {
    activeItemTreeId.value = String(node?.id ?? 'all');
    itemSelectorCurrentPage.value = 1;
    loadItemCandidates();
};
const handleItemSelectionChange = (rows) => {
    selectedItemCandidates.value = rows;
};
const handleItemClear = () => {
    selectedItemCandidates.value = [];
};
const applyItemToRow = (row, item) => {
    row.itemCode = item.code;
    row.itemName = item.name;
    row.spec = item.spec;
    row.category = item.category;
    if (!row.purchaseUnit) {
        row.purchaseUnit = item.purchaseUnit;
    }
};
const handleItemSelectorConfirm = (selectedRows) => {
    const picked = selectedRows;
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
const handleSalesmanChange = (userId) => {
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
const addRow = (index) => {
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
const removeRow = (index) => {
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
const handleToolbarAction = async (action) => {
    if (isReadonlyMode.value && action !== '返回') {
        ElMessage.info('当前单据为查看状态，不能编辑');
        return;
    }
    if (action === '添加物品') {
        addRow();
        return;
    }
    if (action === '通过模板新建') {
        const page = await fetchItemsApi({
            pageNo: 1,
            pageSize: 2,
        }, resolveOrgId());
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
        const page = await fetchItemsApi({
            pageNo: 1,
            pageSize: 2,
        }, resolveOrgId());
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
        await updatePurchaseInboundApi(inboundId.value, payload, resolveOrgId());
        ElMessage.success('保存成功');
    }
    else {
        const created = await createPurchaseInboundApi(payload, resolveOrgId());
        ElMessage.success(`保存成功：${created.documentCode}`);
    }
    router.push('/inventory/1/2');
};
watch(() => [route.name, route.params.id, sessionStore.currentOrgId], () => {
    loadPageData().catch(() => {
        // Global error message handled in http interceptor.
    });
}, { immediate: true });
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['el-input__inner']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-item-table']} */ ;
/** @type {__VLS_StyleScopedClasses['el-input__wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-item-table']} */ ;
/** @type {__VLS_StyleScopedClasses['el-select__wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-item-table']} */ ;
/** @type {__VLS_StyleScopedClasses['el-input__wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-remark-item']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-create-page" },
});
/** @type {[typeof FixedActionBreadcrumb, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(FixedActionBreadcrumb, new FixedActionBreadcrumb({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.navs),
    activeKey: (__VLS_ctx.activeNav),
    showActions: (!__VLS_ctx.isReadonlyMode),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.navs),
    activeKey: (__VLS_ctx.activeNav),
    showActions: (!__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onBack: (__VLS_ctx.handleBack)
};
const __VLS_7 = {
    onSaveDraft: (__VLS_ctx.handleSaveDraft)
};
const __VLS_8 = {
    onSave: (__VLS_ctx.handleSave)
};
const __VLS_9 = {
    onNavigate: (__VLS_ctx.scrollToSection)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel form-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "basicSectionRef",
    ...{ class: "form-section-block" },
});
/** @type {typeof __VLS_ctx.basicSectionRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "form-section-title" },
});
const __VLS_10 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
    labelWidth: "96px",
    ...{ class: "item-create-form purchase-inbound-create-form" },
}));
const __VLS_12 = __VLS_11({
    labelWidth: "96px",
    ...{ class: "item-create-form purchase-inbound-create-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_11));
__VLS_13.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid purchase-inbound-basic-grid" },
});
const __VLS_14 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
    label: "单据编号",
}));
const __VLS_16 = __VLS_15({
    label: "单据编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_15));
__VLS_17.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_17;
const __VLS_18 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
    label: "供应商",
}));
const __VLS_20 = __VLS_19({
    label: "供应商",
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
__VLS_21.slots.default;
const __VLS_22 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.form.supplierId),
    placeholder: "请选择供应商",
    ...{ style: {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_24 = __VLS_23({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.form.supplierId),
    placeholder: "请选择供应商",
    ...{ style: {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_23));
let __VLS_26;
let __VLS_27;
let __VLS_28;
const __VLS_29 = {
    onChange: (__VLS_ctx.handleSupplierChange)
};
__VLS_25.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.supplierOptions))) {
    const __VLS_30 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
        key: (option.id),
        label: (`${option.name} / ${option.code}`),
        value: (option.id),
    }));
    const __VLS_32 = __VLS_31({
        key: (option.id),
        label: (`${option.name} / ${option.code}`),
        value: (option.id),
    }, ...__VLS_functionalComponentArgsRest(__VLS_31));
}
var __VLS_25;
var __VLS_21;
const __VLS_34 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
    label: "入库日期",
}));
const __VLS_36 = __VLS_35({
    label: "入库日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_35));
__VLS_37.slots.default;
const __VLS_38 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
    modelValue: (__VLS_ctx.form.inboundDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择入库日期",
    ...{ style: {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_40 = __VLS_39({
    modelValue: (__VLS_ctx.form.inboundDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择入库日期",
    ...{ style: {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_39));
var __VLS_37;
const __VLS_42 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
    label: "业务员",
}));
const __VLS_44 = __VLS_43({
    label: "业务员",
}, ...__VLS_functionalComponentArgsRest(__VLS_43));
__VLS_45.slots.default;
const __VLS_46 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.form.salesmanUserId),
    placeholder: "请选择业务员",
    ...{ style: {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_48 = __VLS_47({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.form.salesmanUserId),
    placeholder: "请选择业务员",
    ...{ style: {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
let __VLS_50;
let __VLS_51;
let __VLS_52;
const __VLS_53 = {
    onChange: (__VLS_ctx.handleSalesmanChange)
};
__VLS_49.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.salesmanSelectOptions))) {
    const __VLS_54 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
        key: (option.userId),
        label: (option.label),
        value: (option.userId),
    }));
    const __VLS_56 = __VLS_55({
        key: (option.userId),
        label: (option.label),
        value: (option.userId),
    }, ...__VLS_functionalComponentArgsRest(__VLS_55));
}
var __VLS_49;
var __VLS_45;
const __VLS_58 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    label: "备注",
    ...{ class: "purchase-inbound-remark-item" },
}));
const __VLS_60 = __VLS_59({
    label: "备注",
    ...{ class: "purchase-inbound-remark-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
__VLS_61.slots.default;
const __VLS_62 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    modelValue: (__VLS_ctx.form.remark),
    placeholder: "请输入备注",
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_64 = __VLS_63({
    modelValue: (__VLS_ctx.form.remark),
    placeholder: "请输入备注",
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
var __VLS_61;
var __VLS_13;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "itemSectionRef",
    ...{ class: "form-section-block" },
});
/** @type {typeof __VLS_ctx.itemSectionRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_66 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    ...{ 'onClick': {} },
    type: "primary",
    plain: true,
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_68 = __VLS_67({
    ...{ 'onClick': {} },
    type: "primary",
    plain: true,
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
let __VLS_70;
let __VLS_71;
let __VLS_72;
const __VLS_73 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('添加物品');
    }
};
__VLS_69.slots.default;
var __VLS_69;
const __VLS_74 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    ...{ 'onClick': {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_76 = __VLS_75({
    ...{ 'onClick': {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
let __VLS_78;
let __VLS_79;
let __VLS_80;
const __VLS_81 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('通过模板新建');
    }
};
__VLS_77.slots.default;
var __VLS_77;
const __VLS_82 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
    ...{ 'onClick': {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_84 = __VLS_83({
    ...{ 'onClick': {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_83));
let __VLS_86;
let __VLS_87;
let __VLS_88;
const __VLS_89 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量选择仓库');
    }
};
__VLS_85.slots.default;
var __VLS_85;
const __VLS_90 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
    ...{ 'onClick': {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}));
const __VLS_92 = __VLS_91({
    ...{ 'onClick': {} },
    disabled: (__VLS_ctx.isReadonlyMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_91));
let __VLS_94;
let __VLS_95;
let __VLS_96;
const __VLS_97 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导入物品');
    }
};
__VLS_93.slots.default;
var __VLS_93;
const __VLS_98 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    data: (__VLS_ctx.rows),
    border: true,
    stripe: true,
    ...{ class: "erp-table purchase-inbound-item-table" },
    fit: (false),
}));
const __VLS_100 = __VLS_99({
    data: (__VLS_ctx.rows),
    border: true,
    stripe: true,
    ...{ class: "erp-table purchase-inbound-item-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
__VLS_101.slots.default;
const __VLS_102 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_104 = __VLS_103({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_103));
const __VLS_106 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    label: "操作",
    width: "96",
    fixed: "left",
}));
const __VLS_108 = __VLS_107({
    label: "操作",
    width: "96",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
__VLS_109.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_109.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_110 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_112 = __VLS_111({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_111));
    let __VLS_114;
    let __VLS_115;
    let __VLS_116;
    const __VLS_117 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addRow($index);
        }
    };
    __VLS_113.slots.default;
    var __VLS_113;
    const __VLS_118 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
        ...{ 'onClick': {} },
        text: true,
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_120 = __VLS_119({
        ...{ 'onClick': {} },
        text: true,
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_119));
    let __VLS_122;
    let __VLS_123;
    let __VLS_124;
    const __VLS_125 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeRow($index);
        }
    };
    __VLS_121.slots.default;
    var __VLS_121;
}
var __VLS_109;
const __VLS_126 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    label: "物品编码",
    minWidth: "180",
}));
const __VLS_128 = __VLS_127({
    label: "物品编码",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
__VLS_129.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_129.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_130 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
        ...{ 'onClick': {} },
        modelValue: (row.itemCode),
        placeholder: "点击选择物品",
        readonly: true,
        disabled: (__VLS_ctx.isReadonlyMode),
        ...{ class: "item-code-picker" },
    }));
    const __VLS_132 = __VLS_131({
        ...{ 'onClick': {} },
        modelValue: (row.itemCode),
        placeholder: "点击选择物品",
        readonly: true,
        disabled: (__VLS_ctx.isReadonlyMode),
        ...{ class: "item-code-picker" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_131));
    let __VLS_134;
    let __VLS_135;
    let __VLS_136;
    const __VLS_137 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openItemSelector($index);
        }
    };
    var __VLS_133;
}
var __VLS_129;
const __VLS_138 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    label: "物品名称",
    minWidth: "130",
}));
const __VLS_140 = __VLS_139({
    label: "物品名称",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
__VLS_141.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_141.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.itemName || '-');
}
var __VLS_141;
const __VLS_142 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    label: "规格型号",
    minWidth: "120",
}));
const __VLS_144 = __VLS_143({
    label: "规格型号",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
__VLS_145.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_145.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.spec || '-');
}
var __VLS_145;
const __VLS_146 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    label: "物品类别",
    minWidth: "110",
}));
const __VLS_148 = __VLS_147({
    label: "物品类别",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
__VLS_149.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_149.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.category || '-');
}
var __VLS_149;
const __VLS_150 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
    label: "仓库",
    minWidth: "140",
}));
const __VLS_152 = __VLS_151({
    label: "仓库",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_151));
__VLS_153.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_153.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_154 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
        modelValue: (row.warehouse),
        placeholder: "请选择仓库",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_156 = __VLS_155({
        modelValue: (row.warehouse),
        placeholder: "请选择仓库",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_155));
    __VLS_157.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.warehouseOptions))) {
        const __VLS_158 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
            key: (option.id),
            label: (option.label),
            value: (option.name),
        }));
        const __VLS_160 = __VLS_159({
            key: (option.id),
            label: (option.label),
            value: (option.name),
        }, ...__VLS_functionalComponentArgsRest(__VLS_159));
    }
    var __VLS_157;
}
var __VLS_153;
const __VLS_162 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    label: "采购单位",
    minWidth: "120",
}));
const __VLS_164 = __VLS_163({
    label: "采购单位",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
__VLS_165.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_165.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_166 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
        modelValue: (row.purchaseUnit),
        placeholder: "请选择单位",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_168 = __VLS_167({
        modelValue: (row.purchaseUnit),
        placeholder: "请选择单位",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_167));
    __VLS_169.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.purchaseUnitOptions))) {
        const __VLS_170 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_172 = __VLS_171({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_171));
    }
    var __VLS_169;
}
var __VLS_165;
const __VLS_174 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
    label: "数量",
    minWidth: "110",
}));
const __VLS_176 = __VLS_175({
    label: "数量",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_175));
__VLS_177.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_177.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_178 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({
        modelValue: (row.quantity),
        min: (0),
        precision: (4),
        step: (1),
        controlsPosition: "right",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_180 = __VLS_179({
        modelValue: (row.quantity),
        min: (0),
        precision: (4),
        step: (1),
        controlsPosition: "right",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_179));
}
var __VLS_177;
const __VLS_182 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
    label: "入库单价",
    minWidth: "120",
}));
const __VLS_184 = __VLS_183({
    label: "入库单价",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_183));
__VLS_185.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_185.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_186 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
        modelValue: (row.inboundPrice),
        min: (0),
        precision: (4),
        step: (1),
        controlsPosition: "right",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_188 = __VLS_187({
        modelValue: (row.inboundPrice),
        min: (0),
        precision: (4),
        step: (1),
        controlsPosition: "right",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_187));
}
var __VLS_185;
const __VLS_190 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
    label: "金额",
    minWidth: "120",
}));
const __VLS_192 = __VLS_191({
    label: "金额",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_191));
__VLS_193.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_193.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_194 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_195 = __VLS_asFunctionalComponent(__VLS_194, new __VLS_194({
        modelValue: (row.amount),
        min: (0),
        precision: (2),
        step: (1),
        controlsPosition: "right",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_196 = __VLS_195({
        modelValue: (row.amount),
        min: (0),
        precision: (2),
        step: (1),
        controlsPosition: "right",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_195));
}
var __VLS_193;
const __VLS_198 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
    label: "是否赠品",
    minWidth: "96",
}));
const __VLS_200 = __VLS_199({
    label: "是否赠品",
    minWidth: "96",
}, ...__VLS_functionalComponentArgsRest(__VLS_199));
__VLS_201.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_201.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_202 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
        modelValue: (row.gift),
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_204 = __VLS_203({
        modelValue: (row.gift),
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_203));
}
var __VLS_201;
const __VLS_206 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_207 = __VLS_asFunctionalComponent(__VLS_206, new __VLS_206({
    label: "备注",
    minWidth: "180",
}));
const __VLS_208 = __VLS_207({
    label: "备注",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_207));
__VLS_209.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_209.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_210 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
        modelValue: (row.remark),
        placeholder: "请输入备注",
        disabled: (__VLS_ctx.isReadonlyMode),
    }));
    const __VLS_212 = __VLS_211({
        modelValue: (row.remark),
        placeholder: "请输入备注",
        disabled: (__VLS_ctx.isReadonlyMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_211));
}
var __VLS_209;
{
    const { append: __VLS_thisSlot } = __VLS_101.slots;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "purchase-inbound-summary-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "summary-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "summary-cell" },
    });
    (__VLS_ctx.totalQuantity.toFixed(4));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "summary-cell" },
    });
    (__VLS_ctx.totalAmount.toFixed(2));
}
var __VLS_101;
/** @type {[typeof CommonSelectorDialog, ]} */ ;
// @ts-ignore
const __VLS_214 = __VLS_asFunctionalComponent(CommonSelectorDialog, new CommonSelectorDialog({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.itemSelectorVisible),
    title: "选择物品",
    treeData: (__VLS_ctx.itemTreeData),
    tableData: (__VLS_ctx.itemCandidateSource),
    loading: (__VLS_ctx.itemSelectorLoading),
    columns: (__VLS_ctx.itemTableColumns),
    rowKey: "id",
    selectedLabelKey: "name",
    selectedRows: (__VLS_ctx.selectedItemCandidates),
    keywordValue: (__VLS_ctx.itemSelectorKeyword),
    statusValue: (__VLS_ctx.itemSelectorStatus),
    keywordLabel: "物品",
    keywordPlaceholder: "支持按物品编码和名称查询...",
    statusLabel: "启用状态",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
    ]),
    total: (__VLS_ctx.itemSelectorTotal),
    currentPage: (__VLS_ctx.itemSelectorCurrentPage),
    pageSize: (__VLS_ctx.itemSelectorPageSize),
}));
const __VLS_215 = __VLS_214({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.itemSelectorVisible),
    title: "选择物品",
    treeData: (__VLS_ctx.itemTreeData),
    tableData: (__VLS_ctx.itemCandidateSource),
    loading: (__VLS_ctx.itemSelectorLoading),
    columns: (__VLS_ctx.itemTableColumns),
    rowKey: "id",
    selectedLabelKey: "name",
    selectedRows: (__VLS_ctx.selectedItemCandidates),
    keywordValue: (__VLS_ctx.itemSelectorKeyword),
    statusValue: (__VLS_ctx.itemSelectorStatus),
    keywordLabel: "物品",
    keywordPlaceholder: "支持按物品编码和名称查询...",
    statusLabel: "启用状态",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
    ]),
    total: (__VLS_ctx.itemSelectorTotal),
    currentPage: (__VLS_ctx.itemSelectorCurrentPage),
    pageSize: (__VLS_ctx.itemSelectorPageSize),
}, ...__VLS_functionalComponentArgsRest(__VLS_214));
let __VLS_217;
let __VLS_218;
let __VLS_219;
const __VLS_220 = {
    onSearch: (__VLS_ctx.handleItemSelectorSearch)
};
const __VLS_221 = {
    onNodeChange: (__VLS_ctx.handleItemNodeChange)
};
const __VLS_222 = {
    onSelectionChange: (__VLS_ctx.handleItemSelectionChange)
};
const __VLS_223 = {
    onClearSelection: (__VLS_ctx.handleItemClear)
};
const __VLS_224 = {
    onPageChange: ((p) => { __VLS_ctx.itemSelectorCurrentPage = p; __VLS_ctx.loadItemCandidates(); })
};
const __VLS_225 = {
    onPageSizeChange: ((s) => { __VLS_ctx.itemSelectorPageSize = s; __VLS_ctx.itemSelectorCurrentPage = 1; __VLS_ctx.loadItemCandidates(); })
};
const __VLS_226 = {
    onConfirm: (__VLS_ctx.handleItemSelectorConfirm)
};
var __VLS_216;
const __VLS_227 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
    modelValue: (__VLS_ctx.batchWarehouseDialogVisible),
    title: "批量选择仓库",
    width: "420px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_229 = __VLS_228({
    modelValue: (__VLS_ctx.batchWarehouseDialogVisible),
    title: "批量选择仓库",
    width: "420px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_228));
__VLS_230.slots.default;
const __VLS_231 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
    labelWidth: "90px",
}));
const __VLS_233 = __VLS_232({
    labelWidth: "90px",
}, ...__VLS_functionalComponentArgsRest(__VLS_232));
__VLS_234.slots.default;
const __VLS_235 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
    label: "仓库",
}));
const __VLS_237 = __VLS_236({
    label: "仓库",
}, ...__VLS_functionalComponentArgsRest(__VLS_236));
__VLS_238.slots.default;
const __VLS_239 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    modelValue: (__VLS_ctx.batchWarehouse),
    placeholder: "请选择仓库",
    ...{ style: {} },
}));
const __VLS_241 = __VLS_240({
    modelValue: (__VLS_ctx.batchWarehouse),
    placeholder: "请选择仓库",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
__VLS_242.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.warehouseOptions))) {
    const __VLS_243 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_244 = __VLS_asFunctionalComponent(__VLS_243, new __VLS_243({
        key: (option.id),
        label: (option.label),
        value: (option.name),
    }));
    const __VLS_245 = __VLS_244({
        key: (option.id),
        label: (option.label),
        value: (option.name),
    }, ...__VLS_functionalComponentArgsRest(__VLS_244));
}
var __VLS_242;
var __VLS_238;
var __VLS_234;
{
    const { footer: __VLS_thisSlot } = __VLS_230.slots;
    const __VLS_247 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
        ...{ 'onClick': {} },
    }));
    const __VLS_249 = __VLS_248({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_248));
    let __VLS_251;
    let __VLS_252;
    let __VLS_253;
    const __VLS_254 = {
        onClick: (...[$event]) => {
            __VLS_ctx.batchWarehouseDialogVisible = false;
        }
    };
    __VLS_250.slots.default;
    var __VLS_250;
    const __VLS_255 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_257 = __VLS_256({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_256));
    let __VLS_259;
    let __VLS_260;
    let __VLS_261;
    const __VLS_262 = {
        onClick: (__VLS_ctx.confirmBatchWarehouse)
    };
    __VLS_258.slots.default;
    var __VLS_258;
}
var __VLS_230;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-remark-item']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-item-table']} */ ;
/** @type {__VLS_StyleScopedClasses['item-code-picker']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-inbound-summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-title']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            CommonSelectorDialog: CommonSelectorDialog,
            activeNav: activeNav,
            basicSectionRef: basicSectionRef,
            itemSectionRef: itemSectionRef,
            navs: navs,
            isReadonlyMode: isReadonlyMode,
            supplierOptions: supplierOptions,
            warehouseOptions: warehouseOptions,
            purchaseUnitOptions: purchaseUnitOptions,
            salesmanSelectOptions: salesmanSelectOptions,
            itemSelectorVisible: itemSelectorVisible,
            itemSelectorKeyword: itemSelectorKeyword,
            itemSelectorStatus: itemSelectorStatus,
            itemSelectorCurrentPage: itemSelectorCurrentPage,
            itemSelectorPageSize: itemSelectorPageSize,
            itemSelectorLoading: itemSelectorLoading,
            itemSelectorTotal: itemSelectorTotal,
            selectedItemCandidates: selectedItemCandidates,
            itemTreeData: itemTreeData,
            itemCandidateSource: itemCandidateSource,
            itemTableColumns: itemTableColumns,
            form: form,
            rows: rows,
            batchWarehouseDialogVisible: batchWarehouseDialogVisible,
            batchWarehouse: batchWarehouse,
            loadItemCandidates: loadItemCandidates,
            handleBack: handleBack,
            scrollToSection: scrollToSection,
            handleSupplierChange: handleSupplierChange,
            openItemSelector: openItemSelector,
            handleItemSelectorSearch: handleItemSelectorSearch,
            handleItemNodeChange: handleItemNodeChange,
            handleItemSelectionChange: handleItemSelectionChange,
            handleItemClear: handleItemClear,
            handleItemSelectorConfirm: handleItemSelectorConfirm,
            handleSalesmanChange: handleSalesmanChange,
            addRow: addRow,
            removeRow: removeRow,
            totalQuantity: totalQuantity,
            totalAmount: totalAmount,
            handleToolbarAction: handleToolbarAction,
            confirmBatchWarehouse: confirmBatchWarehouse,
            handleSaveDraft: handleSaveDraft,
            handleSave: handleSave,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
