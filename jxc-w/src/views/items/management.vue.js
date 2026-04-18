/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { batchDeleteItemsApi, batchUpdateItemStatusApi, fetchItemCategoryTreeApi, fetchItemsApi, } from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
import ItemCategoryTree from './components/ItemCategoryTree.vue';
import ItemPaginationSection from './components/ItemPaginationSection.vue';
import ItemQuerySection from './components/ItemQuerySection.vue';
import ItemTableSection from './components/ItemTableSection.vue';
import ItemToolbarSection from './components/ItemToolbarSection.vue';
import { defaultItemQuery, itemColumns, itemToolbarButtons, itemTypeOptions, statTypeOptions, statusOptions, storageModeOptions, } from './management-data';
const router = useRouter();
const sessionStore = useSessionStore();
const query = reactive({ ...defaultItemQuery });
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableHeight = 360;
const tableData = ref([]);
const selectedRows = ref([]);
const loading = ref(false);
const emptyText = ref('当前机构暂无数据');
const selectedCount = ref(0);
const rootCategoryName = '物品类别';
const selectedTreeNode = ref(rootCategoryName);
const categoryTree = ref([{ label: rootCategoryName, children: [] }]);
const selectedTreeCategories = ref([]);
const isCancelError = (error) => {
    if (error === 'cancel' || error === 'close') {
        return true;
    }
    if (error instanceof Error) {
        return error.message.includes('cancel');
    }
    return false;
};
const resolveItemOrgId = () => {
    const orgId = (sessionStore.currentOrgId ?? '').trim();
    if (!orgId) {
        return undefined;
    }
    if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
        return orgId;
    }
    return undefined;
};
const fetchCategoryTree = async () => {
    const tree = await fetchItemCategoryTreeApi(resolveItemOrgId());
    categoryTree.value = tree.length ? tree : [{ label: rootCategoryName, children: [] }];
};
const fetchTableData = async () => {
    loading.value = true;
    try {
        const res = await fetchItemsApi({
            pageNo: currentPage.value,
            pageSize: pageSize.value,
            keyword: query.keyword,
            category: selectedTreeCategories.value.length ? selectedTreeCategories.value.join(',') : undefined,
            status: query.status,
            itemType: query.itemType,
            statType: query.statType,
            storageMode: query.storageMode,
            tag: query.tag,
        }, resolveItemOrgId());
        tableData.value = res.list;
        total.value = res.total;
        selectedRows.value = [];
        selectedCount.value = 0;
        emptyText.value = '当前机构暂无数据';
    }
    catch {
        tableData.value = [];
        total.value = 0;
        emptyText.value = '当前机构暂无数据';
    }
    finally {
        loading.value = false;
    }
};
const handleSearch = async () => {
    currentPage.value = 1;
    await fetchTableData();
};
const handleReset = async () => {
    Object.assign(query, defaultItemQuery);
    selectedTreeNode.value = rootCategoryName;
    selectedTreeCategories.value = [];
    currentPage.value = 1;
    await fetchTableData();
};
const collectCategoryLabels = (nodes) => {
    const labels = [];
    const walk = (currentNodes) => {
        currentNodes.forEach((node) => {
            labels.push(node.label);
            if (node.children?.length) {
                walk(node.children);
            }
        });
    };
    walk(nodes);
    return labels.filter((label) => label !== rootCategoryName);
};
const findNodeByLabel = (nodes, label) => {
    for (const node of nodes) {
        if (node.label === label) {
            return node;
        }
        if (node.children?.length) {
            const child = findNodeByLabel(node.children, label);
            if (child) {
                return child;
            }
        }
    }
    return null;
};
const resolveTreeCategories = (label) => {
    if (label === rootCategoryName) {
        return [];
    }
    const selectedNode = findNodeByLabel(categoryTree.value, label);
    if (!selectedNode) {
        return [label];
    }
    const categories = collectCategoryLabels([selectedNode]);
    return categories.length ? categories : [label];
};
const handleTreeSelect = async (label) => {
    selectedTreeNode.value = label;
    selectedTreeCategories.value = resolveTreeCategories(label);
    currentPage.value = 1;
    await fetchTableData();
};
const batchUpdateStatus = async (status) => {
    const ids = selectedRows.value.map((item) => item.id);
    if (!ids.length) {
        ElMessage.warning('请先选择物品');
        return;
    }
    await batchUpdateItemStatusApi(ids, status, resolveItemOrgId());
    ElMessage.success(`批量${status}成功`);
    await fetchTableData();
};
const batchDelete = async () => {
    const ids = selectedRows.value.map((item) => item.id);
    if (!ids.length) {
        ElMessage.warning('请先选择物品');
        return;
    }
    try {
        await ElMessageBox.confirm(`确认删除已选 ${ids.length} 条物品吗？`, '批量删除', {
            type: 'warning',
        });
    }
    catch (error) {
        if (isCancelError(error)) {
            return;
        }
        throw error;
    }
    await batchDeleteItemsApi(ids, resolveItemOrgId());
    ElMessage.success('批量删除成功');
    if ((currentPage.value - 1) * pageSize.value >= Math.max(total.value - ids.length, 0) && currentPage.value > 1) {
        currentPage.value -= 1;
    }
    await fetchTableData();
};
const handleToolbarAction = async (action) => {
    try {
        if (action === '新增物品') {
            router.push('/archive/1/1/create');
            return;
        }
        if (action === '批量启用') {
            await batchUpdateStatus('启用');
            return;
        }
        if (action === '批量停用') {
            await batchUpdateStatus('停用');
            return;
        }
        if (action === '批量删除') {
            await batchDelete();
            return;
        }
        ElMessage.info(`${action}功能待接入`);
    }
    catch {
        // Global error message handled in http interceptor.
    }
};
const handlePageChange = async (page) => {
    currentPage.value = page;
    await fetchTableData();
};
const handlePageSizeChange = async (size) => {
    pageSize.value = size;
    currentPage.value = 1;
    await fetchTableData();
};
const handleSelectionChange = (rows) => {
    selectedRows.value = rows;
    selectedCount.value = rows.length;
};
const handleToggleStatus = async (row) => {
    try {
        const targetStatus = row.status === '启用' ? '停用' : '启用';
        await batchUpdateItemStatusApi([row.id], targetStatus, resolveItemOrgId());
        ElMessage.success(`${row.name}已${targetStatus}`);
        await fetchTableData();
    }
    catch {
        // Global error message handled in http interceptor.
    }
};
const handleEditOne = (row) => {
    router.push({
        path: '/archive/1/1/create',
        query: {
            id: row.id,
            mode: 'edit',
        },
    });
};
const handleDeleteOne = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除物品“${row.name}”吗？`, '删除确认', {
            type: 'warning',
        });
        await batchDeleteItemsApi([row.id], resolveItemOrgId());
        ElMessage.success('删除成功');
        if ((currentPage.value - 1) * pageSize.value >= Math.max(total.value - 1, 0) && currentPage.value > 1) {
            currentPage.value -= 1;
        }
        await fetchTableData();
    }
    catch (error) {
        if (isCancelError(error)) {
            return;
        }
    }
};
onMounted(async () => {
    await Promise.all([fetchTableData(), fetchCategoryTree()]);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-management-layout" },
});
/** @type {[typeof ItemCategoryTree, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(ItemCategoryTree, new ItemCategoryTree({
    ...{ 'onSelect': {} },
    treeData: (__VLS_ctx.categoryTree),
    selectedLabel: (__VLS_ctx.selectedTreeNode),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onSelect': {} },
    treeData: (__VLS_ctx.categoryTree),
    selectedLabel: (__VLS_ctx.selectedTreeNode),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onSelect: (__VLS_ctx.handleTreeSelect)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
/** @type {[typeof ItemQuerySection, ]} */ ;
// @ts-ignore
const __VLS_7 = __VLS_asFunctionalComponent(ItemQuerySection, new ItemQuerySection({
    ...{ 'onSearch': {} },
    ...{ 'onReset': {} },
    modelValue: (__VLS_ctx.query),
    statusOptions: (__VLS_ctx.statusOptions),
    itemTypeOptions: (__VLS_ctx.itemTypeOptions),
    statTypeOptions: (__VLS_ctx.statTypeOptions),
    storageModeOptions: (__VLS_ctx.storageModeOptions),
}));
const __VLS_8 = __VLS_7({
    ...{ 'onSearch': {} },
    ...{ 'onReset': {} },
    modelValue: (__VLS_ctx.query),
    statusOptions: (__VLS_ctx.statusOptions),
    itemTypeOptions: (__VLS_ctx.itemTypeOptions),
    statTypeOptions: (__VLS_ctx.statTypeOptions),
    storageModeOptions: (__VLS_ctx.storageModeOptions),
}, ...__VLS_functionalComponentArgsRest(__VLS_7));
let __VLS_10;
let __VLS_11;
let __VLS_12;
const __VLS_13 = {
    onSearch: (__VLS_ctx.handleSearch)
};
const __VLS_14 = {
    onReset: (__VLS_ctx.handleReset)
};
var __VLS_9;
/** @type {[typeof ItemToolbarSection, ]} */ ;
// @ts-ignore
const __VLS_15 = __VLS_asFunctionalComponent(ItemToolbarSection, new ItemToolbarSection({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.itemToolbarButtons),
}));
const __VLS_16 = __VLS_15({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.itemToolbarButtons),
}, ...__VLS_functionalComponentArgsRest(__VLS_15));
let __VLS_18;
let __VLS_19;
let __VLS_20;
const __VLS_21 = {
    onAction: (__VLS_ctx.handleToolbarAction)
};
var __VLS_17;
/** @type {[typeof ItemTableSection, ]} */ ;
// @ts-ignore
const __VLS_22 = __VLS_asFunctionalComponent(ItemTableSection, new ItemTableSection({
    ...{ 'onSelectionChange': {} },
    ...{ 'onEdit': {} },
    ...{ 'onToggleStatus': {} },
    ...{ 'onDelete': {} },
    data: (__VLS_ctx.tableData),
    columns: (__VLS_ctx.itemColumns),
    height: (__VLS_ctx.tableHeight),
    loading: (__VLS_ctx.loading),
    emptyText: (__VLS_ctx.emptyText),
}));
const __VLS_23 = __VLS_22({
    ...{ 'onSelectionChange': {} },
    ...{ 'onEdit': {} },
    ...{ 'onToggleStatus': {} },
    ...{ 'onDelete': {} },
    data: (__VLS_ctx.tableData),
    columns: (__VLS_ctx.itemColumns),
    height: (__VLS_ctx.tableHeight),
    loading: (__VLS_ctx.loading),
    emptyText: (__VLS_ctx.emptyText),
}, ...__VLS_functionalComponentArgsRest(__VLS_22));
let __VLS_25;
let __VLS_26;
let __VLS_27;
const __VLS_28 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
const __VLS_29 = {
    onEdit: (__VLS_ctx.handleEditOne)
};
const __VLS_30 = {
    onToggleStatus: (__VLS_ctx.handleToggleStatus)
};
const __VLS_31 = {
    onDelete: (__VLS_ctx.handleDeleteOne)
};
var __VLS_24;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedCount),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}));
const __VLS_33 = __VLS_32({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedCount),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
let __VLS_35;
let __VLS_36;
let __VLS_37;
const __VLS_38 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_39 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_34;
/** @type {__VLS_StyleScopedClasses['item-management-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ItemCategoryTree: ItemCategoryTree,
            ItemPaginationSection: ItemPaginationSection,
            ItemQuerySection: ItemQuerySection,
            ItemTableSection: ItemTableSection,
            ItemToolbarSection: ItemToolbarSection,
            itemColumns: itemColumns,
            itemToolbarButtons: itemToolbarButtons,
            itemTypeOptions: itemTypeOptions,
            statTypeOptions: statTypeOptions,
            statusOptions: statusOptions,
            storageModeOptions: storageModeOptions,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            total: total,
            tableHeight: tableHeight,
            tableData: tableData,
            loading: loading,
            emptyText: emptyText,
            selectedCount: selectedCount,
            selectedTreeNode: selectedTreeNode,
            categoryTree: categoryTree,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleTreeSelect: handleTreeSelect,
            handleToolbarAction: handleToolbarAction,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            handleSelectionChange: handleSelectionChange,
            handleToggleStatus: handleToggleStatus,
            handleEditOne: handleEditOne,
            handleDeleteOne: handleDeleteOne,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
