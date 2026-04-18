/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { createSupplierCategoryApi, fetchSupplierCategoryTreeApi, fetchSuppliersApi, } from '@/api/modules/supplier';
import { useSessionStore } from '@/stores/session';
import SupplierCategoryTree from './components/SupplierCategoryTree.vue';
import SupplierPaginationSection from './components/SupplierPaginationSection.vue';
import SupplierQuerySection from './components/SupplierQuerySection.vue';
import SupplierTableSection from './components/SupplierTableSection.vue';
import SupplierToolbarSection from './components/SupplierToolbarSection.vue';
import { bindStatusOptions, defaultSupplierQuery, sourceOptions, statusOptions, supplierColumns, supplierToolbarButtons, supplyRelationOptions, } from './management-data';
const router = useRouter();
const sessionStore = useSessionStore();
const query = reactive({ ...defaultSupplierQuery });
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableHeight = 360;
const emptyText = '当前机构暂无数据';
const selectedRows = ref([]);
const selectedCount = ref(0);
const selectedCategoryId = ref('all');
const selectedCategoryLabel = ref('供应商类别');
const loading = ref(false);
const tableData = ref([]);
const supplierCategoryTree = ref([]);
const categoryDialogVisible = ref(false);
const categoryFormRef = ref();
const categoryForm = reactive({
    categoryName: '',
    parentCategory: '供应商类别',
});
const categoryFormRules = {
    categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
    parentCategory: [{ required: true, message: '请选择上级类别', trigger: 'change' }],
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
const categorySelectTree = computed(() => supplierCategoryTree.value);
const fetchCategoryTree = async () => {
    try {
        supplierCategoryTree.value = await fetchSupplierCategoryTreeApi(resolveSupplierOrgId());
    }
    catch {
        supplierCategoryTree.value = [];
        ElMessage.error('供应商类别加载失败');
    }
    if (!findCategoryById(supplierCategoryTree.value, selectedCategoryId.value)) {
        selectedCategoryId.value = 'all';
        selectedCategoryLabel.value = '供应商类别';
    }
};
const fetchTableData = async () => {
    loading.value = true;
    try {
        const result = await fetchSuppliersApi({
            pageNo: currentPage.value,
            pageSize: pageSize.value,
            supplierInfo: query.supplierInfo || undefined,
            status: query.status === '全部' ? undefined : query.status,
            bindStatus: query.bindStatus === '全部' ? undefined : query.bindStatus,
            source: query.source === '全部' ? undefined : query.source,
            supplyRelation: query.supplyRelation === '全部' ? undefined : query.supplyRelation,
            treeNode: selectedCategoryId.value === 'all' ? undefined : selectedCategoryId.value,
        }, resolveSupplierOrgId());
        tableData.value = (result.list ?? []);
        total.value = result.total ?? 0;
        selectedRows.value = [];
        selectedCount.value = 0;
    }
    catch {
        tableData.value = [];
        total.value = 0;
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
    Object.assign(query, defaultSupplierQuery);
    selectedCategoryId.value = 'all';
    selectedCategoryLabel.value = '供应商类别';
    currentPage.value = 1;
    await fetchTableData();
};
const ensureSelection = () => {
    if (!selectedRows.value.length) {
        ElMessage.warning('请先选择供应商');
        return false;
    }
    return true;
};
const handleToolbarAction = async (action) => {
    if (action === '新增') {
        router.push('/archive/3/1/create');
        return;
    }
    if (['批量启用', '批量停用', '批量删除', '批量注册绑定', '批量修改'].includes(action) && !ensureSelection()) {
        return;
    }
    ElMessage.info(`${action}功能待接入`);
};
const handleSelectionChange = (selection) => {
    selectedRows.value = selection;
    selectedCount.value = selection.length;
};
const handleEdit = (row) => {
    router.push(`/archive/3/1/edit/${row.id}`);
};
const handleBind = (row) => {
    ElMessage.info(`绑定：${row.supplierName}`);
};
const handleDelete = (row) => {
    ElMessage.info(`删除：${row.supplierName}`);
};
const findCategoryById = (nodes, id) => {
    for (const node of nodes) {
        if (node.id === id) {
            return node;
        }
        if (node.children?.length) {
            const target = findCategoryById(node.children, id);
            if (target) {
                return target;
            }
        }
    }
    return null;
};
const handleCategorySelect = async (id) => {
    selectedCategoryId.value = id;
    const node = findCategoryById(supplierCategoryTree.value, id);
    selectedCategoryLabel.value = node?.label ?? '供应商类别';
    currentPage.value = 1;
    await fetchTableData();
};
const openAddCategoryDialog = () => {
    categoryForm.categoryName = '';
    categoryForm.parentCategory = selectedCategoryId.value === 'all' ? '供应商类别' : selectedCategoryLabel.value;
    categoryDialogVisible.value = true;
};
const closeAddCategoryDialog = () => {
    categoryDialogVisible.value = false;
    categoryFormRef.value?.clearValidate();
};
const handleAddCategorySubmit = async () => {
    if (!categoryFormRef.value) {
        return;
    }
    const valid = await categoryFormRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }
    await createSupplierCategoryApi({
        categoryCode: undefined,
        categoryName: categoryForm.categoryName.trim(),
        parentCategory: categoryForm.parentCategory,
    }, resolveSupplierOrgId());
    await fetchCategoryTree();
    ElMessage.success('新增子类成功');
    closeAddCategoryDialog();
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
onMounted(async () => {
    await fetchCategoryTree();
    await fetchTableData();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-management-layout" },
});
/** @type {[typeof SupplierCategoryTree, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(SupplierCategoryTree, new SupplierCategoryTree({
    ...{ 'onSelect': {} },
    ...{ 'onAddSubcategory': {} },
    treeData: (__VLS_ctx.supplierCategoryTree),
    selectedId: (__VLS_ctx.selectedCategoryId),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onSelect': {} },
    ...{ 'onAddSubcategory': {} },
    treeData: (__VLS_ctx.supplierCategoryTree),
    selectedId: (__VLS_ctx.selectedCategoryId),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onSelect: (__VLS_ctx.handleCategorySelect)
};
const __VLS_7 = {
    onAddSubcategory: (__VLS_ctx.openAddCategoryDialog)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
/** @type {[typeof SupplierQuerySection, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(SupplierQuerySection, new SupplierQuerySection({
    ...{ 'onSearch': {} },
    ...{ 'onReset': {} },
    modelValue: (__VLS_ctx.query),
    supplierInfoTree: (__VLS_ctx.supplierCategoryTree),
    statusOptions: (__VLS_ctx.statusOptions),
    bindStatusOptions: (__VLS_ctx.bindStatusOptions),
    sourceOptions: (__VLS_ctx.sourceOptions),
    supplyRelationOptions: (__VLS_ctx.supplyRelationOptions),
}));
const __VLS_9 = __VLS_8({
    ...{ 'onSearch': {} },
    ...{ 'onReset': {} },
    modelValue: (__VLS_ctx.query),
    supplierInfoTree: (__VLS_ctx.supplierCategoryTree),
    statusOptions: (__VLS_ctx.statusOptions),
    bindStatusOptions: (__VLS_ctx.bindStatusOptions),
    sourceOptions: (__VLS_ctx.sourceOptions),
    supplyRelationOptions: (__VLS_ctx.supplyRelationOptions),
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
let __VLS_11;
let __VLS_12;
let __VLS_13;
const __VLS_14 = {
    onSearch: (__VLS_ctx.handleSearch)
};
const __VLS_15 = {
    onReset: (__VLS_ctx.handleReset)
};
var __VLS_10;
/** @type {[typeof SupplierToolbarSection, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(SupplierToolbarSection, new SupplierToolbarSection({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.supplierToolbarButtons),
}));
const __VLS_17 = __VLS_16({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.supplierToolbarButtons),
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
let __VLS_19;
let __VLS_20;
let __VLS_21;
const __VLS_22 = {
    onAction: (__VLS_ctx.handleToolbarAction)
};
var __VLS_18;
/** @type {[typeof SupplierTableSection, ]} */ ;
// @ts-ignore
const __VLS_23 = __VLS_asFunctionalComponent(SupplierTableSection, new SupplierTableSection({
    ...{ 'onSelectionChange': {} },
    ...{ 'onEdit': {} },
    ...{ 'onBind': {} },
    ...{ 'onDelete': {} },
    data: (__VLS_ctx.tableData),
    columns: (__VLS_ctx.supplierColumns),
    height: (__VLS_ctx.tableHeight),
    loading: (__VLS_ctx.loading),
    emptyText: (__VLS_ctx.emptyText),
}));
const __VLS_24 = __VLS_23({
    ...{ 'onSelectionChange': {} },
    ...{ 'onEdit': {} },
    ...{ 'onBind': {} },
    ...{ 'onDelete': {} },
    data: (__VLS_ctx.tableData),
    columns: (__VLS_ctx.supplierColumns),
    height: (__VLS_ctx.tableHeight),
    loading: (__VLS_ctx.loading),
    emptyText: (__VLS_ctx.emptyText),
}, ...__VLS_functionalComponentArgsRest(__VLS_23));
let __VLS_26;
let __VLS_27;
let __VLS_28;
const __VLS_29 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
const __VLS_30 = {
    onEdit: (__VLS_ctx.handleEdit)
};
const __VLS_31 = {
    onBind: (__VLS_ctx.handleBind)
};
const __VLS_32 = {
    onDelete: (__VLS_ctx.handleDelete)
};
var __VLS_25;
/** @type {[typeof SupplierPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(SupplierPaginationSection, new SupplierPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedCount),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}));
const __VLS_34 = __VLS_33({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedCount),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
let __VLS_36;
let __VLS_37;
let __VLS_38;
const __VLS_39 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_40 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_35;
const __VLS_41 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_42 = __VLS_asFunctionalComponent(__VLS_41, new __VLS_41({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.categoryDialogVisible),
    title: "新增子类",
    width: "560px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_43 = __VLS_42({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.categoryDialogVisible),
    title: "新增子类",
    width: "560px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_42));
let __VLS_45;
let __VLS_46;
let __VLS_47;
const __VLS_48 = {
    onClosed: (__VLS_ctx.closeAddCategoryDialog)
};
__VLS_44.slots.default;
const __VLS_49 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_50 = __VLS_asFunctionalComponent(__VLS_49, new __VLS_49({
    ref: "categoryFormRef",
    ...{ class: "standard-dialog-form" },
    model: (__VLS_ctx.categoryForm),
    rules: (__VLS_ctx.categoryFormRules),
    labelWidth: "90px",
}));
const __VLS_51 = __VLS_50({
    ref: "categoryFormRef",
    ...{ class: "standard-dialog-form" },
    model: (__VLS_ctx.categoryForm),
    rules: (__VLS_ctx.categoryFormRules),
    labelWidth: "90px",
}, ...__VLS_functionalComponentArgsRest(__VLS_50));
/** @type {typeof __VLS_ctx.categoryFormRef} */ ;
var __VLS_53 = {};
__VLS_52.slots.default;
const __VLS_55 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({
    label: "类别名称",
    prop: "categoryName",
}));
const __VLS_57 = __VLS_56({
    label: "类别名称",
    prop: "categoryName",
}, ...__VLS_functionalComponentArgsRest(__VLS_56));
__VLS_58.slots.default;
const __VLS_59 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
    modelValue: (__VLS_ctx.categoryForm.categoryName),
    placeholder: "请输入类别名称",
}));
const __VLS_61 = __VLS_60({
    modelValue: (__VLS_ctx.categoryForm.categoryName),
    placeholder: "请输入类别名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_60));
var __VLS_58;
const __VLS_63 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({
    label: "上级类别",
    prop: "parentCategory",
}));
const __VLS_65 = __VLS_64({
    label: "上级类别",
    prop: "parentCategory",
}, ...__VLS_functionalComponentArgsRest(__VLS_64));
__VLS_66.slots.default;
const __VLS_67 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({
    modelValue: (__VLS_ctx.categoryForm.parentCategory),
    data: (__VLS_ctx.categorySelectTree),
    props: ({ label: 'label', value: 'label', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_69 = __VLS_68({
    modelValue: (__VLS_ctx.categoryForm.parentCategory),
    data: (__VLS_ctx.categorySelectTree),
    props: ({ label: 'label', value: 'label', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_68));
var __VLS_66;
var __VLS_52;
{
    const { footer: __VLS_thisSlot } = __VLS_44.slots;
    const __VLS_71 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
        ...{ 'onClick': {} },
    }));
    const __VLS_73 = __VLS_72({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_72));
    let __VLS_75;
    let __VLS_76;
    let __VLS_77;
    const __VLS_78 = {
        onClick: (__VLS_ctx.closeAddCategoryDialog)
    };
    __VLS_74.slots.default;
    var __VLS_74;
    const __VLS_79 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_81 = __VLS_80({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_80));
    let __VLS_83;
    let __VLS_84;
    let __VLS_85;
    const __VLS_86 = {
        onClick: (__VLS_ctx.handleAddCategorySubmit)
    };
    __VLS_82.slots.default;
    var __VLS_82;
}
var __VLS_44;
/** @type {__VLS_StyleScopedClasses['item-management-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
// @ts-ignore
var __VLS_54 = __VLS_53;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            SupplierCategoryTree: SupplierCategoryTree,
            SupplierPaginationSection: SupplierPaginationSection,
            SupplierQuerySection: SupplierQuerySection,
            SupplierTableSection: SupplierTableSection,
            SupplierToolbarSection: SupplierToolbarSection,
            bindStatusOptions: bindStatusOptions,
            sourceOptions: sourceOptions,
            statusOptions: statusOptions,
            supplierColumns: supplierColumns,
            supplierToolbarButtons: supplierToolbarButtons,
            supplyRelationOptions: supplyRelationOptions,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            total: total,
            tableHeight: tableHeight,
            emptyText: emptyText,
            selectedCount: selectedCount,
            selectedCategoryId: selectedCategoryId,
            loading: loading,
            tableData: tableData,
            supplierCategoryTree: supplierCategoryTree,
            categoryDialogVisible: categoryDialogVisible,
            categoryFormRef: categoryFormRef,
            categoryForm: categoryForm,
            categoryFormRules: categoryFormRules,
            categorySelectTree: categorySelectTree,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleSelectionChange: handleSelectionChange,
            handleEdit: handleEdit,
            handleBind: handleBind,
            handleDelete: handleDelete,
            handleCategorySelect: handleCategorySelect,
            openAddCategoryDialog: openAddCategoryDialog,
            closeAddCategoryDialog: closeAddCategoryDialog,
            handleAddCategorySubmit: handleAddCategorySubmit,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
