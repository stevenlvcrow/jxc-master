/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import ItemCategoryTree from './components/ItemCategoryTree.vue';
import ItemPaginationSection from './components/ItemPaginationSection.vue';
import { batchCreateItemCategoriesApi, createItemCategoryApi, deleteItemCategoryApi, fetchItemCategoriesApi, fetchItemCategoryTreeApi, updateItemCategoryApi, } from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();
const total = ref(0);
const tableLoading = ref(false);
const emptyText = '当前机构暂无数据';
const selectedCount = ref(0);
const tableHeight = 400;
const statusOptions = ['全部', '启用', '停用'];
const toolbarButtons = ['新增', '批量新增', '批量导入', '分类排序'];
const query = reactive({
    categoryInfo: '',
    status: '全部',
});
const createDialogVisible = ref(false);
const batchCreateDialogVisible = ref(false);
const createFormRef = ref();
const rootCategoryName = '物品类别';
const selectedTreeNode = ref(rootCategoryName);
const sortByParentCategory = ref(false);
const editingCategoryId = ref(null);
const isEditMode = computed(() => editingCategoryId.value !== null);
const createDialogTitle = computed(() => (isEditMode.value ? '编辑物品类别' : '新增物品类别'));
const batchCreateForm = reactive({
    parentCategory: rootCategoryName,
});
const batchCreateRows = ref([
    { categoryName: '' },
]);
const createForm = reactive({
    categoryName: '',
    parentCategory: rootCategoryName,
    status: '启用',
    remark: '',
});
const categoryColumns = [
    { prop: 'index', label: '序号', width: 56, fixed: 'left' },
    { prop: 'categoryCode', label: '类别编码', width: 140, fixed: 'left' },
    { prop: 'categoryName', label: '物品类别', width: 180 },
    { prop: 'parentCategory', label: '上级类别', width: 180 },
    { prop: 'status', label: '状态', width: 96 },
    { prop: 'createdAt', label: '创建时间', width: 180 },
];
const categoryTableData = ref([]);
const categoryTree = ref([{ label: rootCategoryName, children: [] }]);
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
const categorySelectTree = computed(() => {
    const toSelectNodes = (nodes) => nodes.map((node) => ({
        label: node.label,
        value: node.label,
        children: node.children?.length ? toSelectNodes(node.children) : undefined,
    }));
    return toSelectNodes(categoryTree.value);
});
const createFormRules = {
    categoryName: [{ required: true, message: '请输入物品类别', trigger: 'blur' }],
    parentCategory: [{ required: true, message: '请选择上级类别', trigger: 'change' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};
const fetchCategoryList = async () => {
    tableLoading.value = true;
    try {
        const result = await fetchItemCategoriesApi({
            pageNo: currentPage.value,
            pageSize: pageSize.value,
            categoryInfo: query.categoryInfo.trim() || undefined,
            status: query.status === '全部' ? undefined : query.status,
            treeNode: selectedTreeNode.value === rootCategoryName ? undefined : selectedTreeNode.value,
            sortBy: sortByParentCategory.value ? 'parentCategory' : undefined,
        }, resolveItemOrgId());
        categoryTableData.value = result.list ?? [];
        total.value = result.total ?? 0;
        selectedCount.value = 0;
    }
    finally {
        tableLoading.value = false;
    }
};
const fetchCategoryTree = async () => {
    const tree = await fetchItemCategoryTreeApi(resolveItemOrgId());
    categoryTree.value = tree.length ? tree : [{ label: rootCategoryName, children: [] }];
};
const handleSelectionChange = (rows) => {
    selectedCount.value = rows.length;
};
const handleSearch = async () => {
    currentPage.value = 1;
    await fetchCategoryList();
};
const handleReset = async () => {
    query.categoryInfo = '';
    query.status = '全部';
    selectedTreeNode.value = rootCategoryName;
    sortByParentCategory.value = false;
    currentPage.value = 1;
    await fetchCategoryList();
};
const handleTreeSelect = async (label) => {
    selectedTreeNode.value = label;
    currentPage.value = 1;
    await fetchCategoryList();
};
const handlePageChange = async (page) => {
    currentPage.value = page;
    await fetchCategoryList();
};
const handlePageSizeChange = async (size) => {
    pageSize.value = size;
    currentPage.value = 1;
    await fetchCategoryList();
};
const resetCreateForm = () => {
    editingCategoryId.value = null;
    createForm.categoryName = '';
    createForm.parentCategory = rootCategoryName;
    createForm.status = '启用';
    createForm.remark = '';
};
const openCreateDialog = () => {
    resetCreateForm();
    createDialogVisible.value = true;
};
const openEditDialog = (row) => {
    editingCategoryId.value = row.id;
    createForm.categoryName = row.categoryName;
    createForm.parentCategory = row.parentCategory;
    createForm.status = row.status;
    createForm.remark = row.remark ?? '';
    createDialogVisible.value = true;
};
const closeCreateDialog = () => {
    createDialogVisible.value = false;
    createFormRef.value?.clearValidate();
};
const resetBatchCreateForm = () => {
    batchCreateForm.parentCategory = rootCategoryName;
    batchCreateRows.value = [{ categoryName: '' }];
};
const openBatchCreateDialog = () => {
    resetBatchCreateForm();
    batchCreateDialogVisible.value = true;
};
const closeBatchCreateDialog = () => {
    batchCreateDialogVisible.value = false;
};
const addBatchRow = (index) => {
    batchCreateRows.value.splice(index + 1, 0, { categoryName: '' });
};
const removeBatchRow = (index) => {
    if (batchCreateRows.value.length === 1) {
        ElMessage.warning('至少保留一行');
        return;
    }
    batchCreateRows.value.splice(index, 1);
};
const handleCreateSubmit = async () => {
    if (!createFormRef.value) {
        return;
    }
    const valid = await createFormRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }
    const payload = {
        categoryName: createForm.categoryName.trim(),
        parentCategory: createForm.parentCategory,
        status: createForm.status,
        remark: createForm.remark.trim() || undefined,
    };
    if (isEditMode.value && editingCategoryId.value) {
        await updateItemCategoryApi(editingCategoryId.value, payload, resolveItemOrgId());
        ElMessage.success('编辑类别成功');
    }
    else {
        await createItemCategoryApi(payload, resolveItemOrgId());
        ElMessage.success('新增类别成功');
    }
    await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
    closeCreateDialog();
};
const handleBatchCreateSubmit = async () => {
    const rows = batchCreateRows.value.map((row) => ({
        categoryName: row.categoryName.trim(),
    }));
    const emptyNameIndex = rows.findIndex((row) => !row.categoryName);
    if (emptyNameIndex !== -1) {
        ElMessage.warning(`第 ${emptyNameIndex + 1} 行类别名称不能为空`);
        return;
    }
    const duplicateName = rows.find((row, index) => rows.findIndex((item) => item.categoryName === row.categoryName) !== index);
    if (duplicateName) {
        ElMessage.warning(`批量新增中类别名称重复：${duplicateName.categoryName}`);
        return;
    }
    await batchCreateItemCategoriesApi({
        parentCategory: batchCreateForm.parentCategory,
        status: '启用',
        items: rows.map((row) => ({
            categoryName: row.categoryName,
        })),
    }, resolveItemOrgId());
    await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
    closeBatchCreateDialog();
    ElMessage.success(`批量新增成功，共 ${rows.length} 条`);
};
const handleDelete = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除类别“${row.categoryName}”吗？`, '删除确认', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
        await deleteItemCategoryApi(row.id, resolveItemOrgId());
        await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
        ElMessage.success('删除类别成功');
    }
    catch {
        // 用户取消删除时忽略
    }
};
const handleToolbarAction = async (action) => {
    if (action === '新增') {
        openCreateDialog();
        return;
    }
    if (action === '批量新增') {
        openBatchCreateDialog();
        return;
    }
    if (action === '分类排序') {
        sortByParentCategory.value = true;
        currentPage.value = 1;
        await fetchCategoryList();
        ElMessage.success('已按上级类别排序');
        return;
    }
    ElMessage.info(`${action}功能待接入`);
};
onMounted(async () => {
    await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
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
const __VLS_7 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "compact-filter-bar" },
}));
const __VLS_9 = __VLS_8({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
__VLS_10.slots.default;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "类别信息",
}));
const __VLS_13 = __VLS_12({
    label: "类别信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.query.categoryInfo),
    placeholder: "请输入类别编码或名称",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.query.categoryInfo),
    placeholder: "请输入类别编码或名称",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
var __VLS_14;
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "状态",
}));
const __VLS_21 = __VLS_20({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    modelValue: (__VLS_ctx.query.status),
    ...{ style: {} },
}));
const __VLS_25 = __VLS_24({
    modelValue: (__VLS_ctx.query.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.statusOptions))) {
    const __VLS_27 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_29 = __VLS_28({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_28));
}
var __VLS_26;
var __VLS_22;
const __VLS_31 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({}));
const __VLS_33 = __VLS_32({}, ...__VLS_functionalComponentArgsRest(__VLS_32));
__VLS_34.slots.default;
const __VLS_35 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_37 = __VLS_36({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
let __VLS_39;
let __VLS_40;
let __VLS_41;
const __VLS_42 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_38.slots.default;
var __VLS_38;
const __VLS_43 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    ...{ 'onClick': {} },
}));
const __VLS_45 = __VLS_44({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
let __VLS_47;
let __VLS_48;
let __VLS_49;
const __VLS_50 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_46.slots.default;
var __VLS_46;
var __VLS_34;
var __VLS_10;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
for (const [button, index] of __VLS_getVForSourceType((__VLS_ctx.toolbarButtons))) {
    const __VLS_51 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
    }));
    const __VLS_53 = __VLS_52({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_52));
    let __VLS_55;
    let __VLS_56;
    let __VLS_57;
    const __VLS_58 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleToolbarAction(button);
        }
    };
    __VLS_54.slots.default;
    (button);
    var __VLS_54;
}
const __VLS_59 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.categoryTableData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
    emptyText: (__VLS_ctx.emptyText),
}));
const __VLS_61 = __VLS_60({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.categoryTableData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
    emptyText: (__VLS_ctx.emptyText),
}, ...__VLS_functionalComponentArgsRest(__VLS_60));
let __VLS_63;
let __VLS_64;
let __VLS_65;
const __VLS_66 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.tableLoading) }, null, null);
__VLS_62.slots.default;
const __VLS_67 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_69 = __VLS_68({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_68));
for (const [column] of __VLS_getVForSourceType((__VLS_ctx.categoryColumns))) {
    const __VLS_71 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
        key: (column.prop),
        prop: (column.prop),
        label: (column.label),
        width: (column.width),
        fixed: (column.fixed),
        showOverflowTooltip: true,
    }));
    const __VLS_73 = __VLS_72({
        key: (column.prop),
        prop: (column.prop),
        label: (column.label),
        width: (column.width),
        fixed: (column.fixed),
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_72));
    __VLS_74.slots.default;
    if (column.prop === 'status') {
        {
            const { default: __VLS_thisSlot } = __VLS_74.slots;
            const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
            const __VLS_75 = {}.ElTag;
            /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
            // @ts-ignore
            const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
                type: (row.status === '启用' ? 'success' : 'info'),
                size: "small",
            }));
            const __VLS_77 = __VLS_76({
                type: (row.status === '启用' ? 'success' : 'info'),
                size: "small",
            }, ...__VLS_functionalComponentArgsRest(__VLS_76));
            __VLS_78.slots.default;
            (row.status);
            var __VLS_78;
        }
    }
    var __VLS_74;
}
const __VLS_79 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
    label: "操作",
    width: "120",
    fixed: "right",
}));
const __VLS_81 = __VLS_80({
    label: "操作",
    width: "120",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_80));
__VLS_82.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_82.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_83 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_85 = __VLS_84({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_84));
    let __VLS_87;
    let __VLS_88;
    let __VLS_89;
    const __VLS_90 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditDialog(row);
        }
    };
    __VLS_86.slots.default;
    var __VLS_86;
    const __VLS_91 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_93 = __VLS_92({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_92));
    let __VLS_95;
    let __VLS_96;
    let __VLS_97;
    const __VLS_98 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDelete(row);
        }
    };
    __VLS_94.slots.default;
    var __VLS_94;
}
var __VLS_82;
var __VLS_62;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedCount),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}));
const __VLS_100 = __VLS_99({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedCount),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
let __VLS_102;
let __VLS_103;
let __VLS_104;
const __VLS_105 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_106 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_101;
const __VLS_107 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.createDialogTitle),
    width: "560px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_109 = __VLS_108({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.createDialogTitle),
    width: "560px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_108));
let __VLS_111;
let __VLS_112;
let __VLS_113;
const __VLS_114 = {
    onClosed: (__VLS_ctx.closeCreateDialog)
};
__VLS_110.slots.default;
const __VLS_115 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({
    ref: "createFormRef",
    ...{ class: "standard-dialog-form category-create-form" },
    model: (__VLS_ctx.createForm),
    rules: (__VLS_ctx.createFormRules),
    labelWidth: "90px",
}));
const __VLS_117 = __VLS_116({
    ref: "createFormRef",
    ...{ class: "standard-dialog-form category-create-form" },
    model: (__VLS_ctx.createForm),
    rules: (__VLS_ctx.createFormRules),
    labelWidth: "90px",
}, ...__VLS_functionalComponentArgsRest(__VLS_116));
/** @type {typeof __VLS_ctx.createFormRef} */ ;
var __VLS_119 = {};
__VLS_118.slots.default;
const __VLS_121 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_122 = __VLS_asFunctionalComponent(__VLS_121, new __VLS_121({
    label: "物品类别",
    prop: "categoryName",
}));
const __VLS_123 = __VLS_122({
    label: "物品类别",
    prop: "categoryName",
}, ...__VLS_functionalComponentArgsRest(__VLS_122));
__VLS_124.slots.default;
const __VLS_125 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_126 = __VLS_asFunctionalComponent(__VLS_125, new __VLS_125({
    modelValue: (__VLS_ctx.createForm.categoryName),
    placeholder: "请输入物品类别名称",
}));
const __VLS_127 = __VLS_126({
    modelValue: (__VLS_ctx.createForm.categoryName),
    placeholder: "请输入物品类别名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_126));
var __VLS_124;
const __VLS_129 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_130 = __VLS_asFunctionalComponent(__VLS_129, new __VLS_129({
    label: "上级类别",
    prop: "parentCategory",
}));
const __VLS_131 = __VLS_130({
    label: "上级类别",
    prop: "parentCategory",
}, ...__VLS_functionalComponentArgsRest(__VLS_130));
__VLS_132.slots.default;
const __VLS_133 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_134 = __VLS_asFunctionalComponent(__VLS_133, new __VLS_133({
    modelValue: (__VLS_ctx.createForm.parentCategory),
    ...{ class: "category-tree-select" },
    data: (__VLS_ctx.categorySelectTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    popperClass: "category-tree-select-popper",
    ...{ style: {} },
}));
const __VLS_135 = __VLS_134({
    modelValue: (__VLS_ctx.createForm.parentCategory),
    ...{ class: "category-tree-select" },
    data: (__VLS_ctx.categorySelectTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    popperClass: "category-tree-select-popper",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_134));
var __VLS_132;
const __VLS_137 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_138 = __VLS_asFunctionalComponent(__VLS_137, new __VLS_137({
    label: "状态",
    prop: "status",
}));
const __VLS_139 = __VLS_138({
    label: "状态",
    prop: "status",
}, ...__VLS_functionalComponentArgsRest(__VLS_138));
__VLS_140.slots.default;
const __VLS_141 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_142 = __VLS_asFunctionalComponent(__VLS_141, new __VLS_141({
    modelValue: (__VLS_ctx.createForm.status),
}));
const __VLS_143 = __VLS_142({
    modelValue: (__VLS_ctx.createForm.status),
}, ...__VLS_functionalComponentArgsRest(__VLS_142));
__VLS_144.slots.default;
const __VLS_145 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_146 = __VLS_asFunctionalComponent(__VLS_145, new __VLS_145({
    label: "启用",
}));
const __VLS_147 = __VLS_146({
    label: "启用",
}, ...__VLS_functionalComponentArgsRest(__VLS_146));
__VLS_148.slots.default;
var __VLS_148;
const __VLS_149 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_150 = __VLS_asFunctionalComponent(__VLS_149, new __VLS_149({
    label: "停用",
}));
const __VLS_151 = __VLS_150({
    label: "停用",
}, ...__VLS_functionalComponentArgsRest(__VLS_150));
__VLS_152.slots.default;
var __VLS_152;
var __VLS_144;
var __VLS_140;
const __VLS_153 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_154 = __VLS_asFunctionalComponent(__VLS_153, new __VLS_153({
    label: "备注",
    prop: "remark",
}));
const __VLS_155 = __VLS_154({
    label: "备注",
    prop: "remark",
}, ...__VLS_functionalComponentArgsRest(__VLS_154));
__VLS_156.slots.default;
const __VLS_157 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_158 = __VLS_asFunctionalComponent(__VLS_157, new __VLS_157({
    modelValue: (__VLS_ctx.createForm.remark),
    type: "textarea",
    rows: (3),
    maxlength: "200",
    showWordLimit: true,
    placeholder: "请输入备注",
}));
const __VLS_159 = __VLS_158({
    modelValue: (__VLS_ctx.createForm.remark),
    type: "textarea",
    rows: (3),
    maxlength: "200",
    showWordLimit: true,
    placeholder: "请输入备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_158));
var __VLS_156;
var __VLS_118;
{
    const { footer: __VLS_thisSlot } = __VLS_110.slots;
    const __VLS_161 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_162 = __VLS_asFunctionalComponent(__VLS_161, new __VLS_161({
        ...{ 'onClick': {} },
    }));
    const __VLS_163 = __VLS_162({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_162));
    let __VLS_165;
    let __VLS_166;
    let __VLS_167;
    const __VLS_168 = {
        onClick: (__VLS_ctx.closeCreateDialog)
    };
    __VLS_164.slots.default;
    var __VLS_164;
    const __VLS_169 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_170 = __VLS_asFunctionalComponent(__VLS_169, new __VLS_169({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_171 = __VLS_170({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_170));
    let __VLS_173;
    let __VLS_174;
    let __VLS_175;
    const __VLS_176 = {
        onClick: (__VLS_ctx.handleCreateSubmit)
    };
    __VLS_172.slots.default;
    (__VLS_ctx.isEditMode ? '保存' : '确定');
    var __VLS_172;
}
var __VLS_110;
const __VLS_177 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_178 = __VLS_asFunctionalComponent(__VLS_177, new __VLS_177({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.batchCreateDialogVisible),
    title: "批量新增物品类别",
    width: "760px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_179 = __VLS_178({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.batchCreateDialogVisible),
    title: "批量新增物品类别",
    width: "760px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_178));
let __VLS_181;
let __VLS_182;
let __VLS_183;
const __VLS_184 = {
    onClosed: (__VLS_ctx.closeBatchCreateDialog)
};
__VLS_180.slots.default;
const __VLS_185 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_186 = __VLS_asFunctionalComponent(__VLS_185, new __VLS_185({
    model: (__VLS_ctx.batchCreateForm),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form category-create-form" },
}));
const __VLS_187 = __VLS_186({
    model: (__VLS_ctx.batchCreateForm),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form category-create-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_186));
__VLS_188.slots.default;
const __VLS_189 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_190 = __VLS_asFunctionalComponent(__VLS_189, new __VLS_189({
    label: "上级类别",
}));
const __VLS_191 = __VLS_190({
    label: "上级类别",
}, ...__VLS_functionalComponentArgsRest(__VLS_190));
__VLS_192.slots.default;
const __VLS_193 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_194 = __VLS_asFunctionalComponent(__VLS_193, new __VLS_193({
    modelValue: (__VLS_ctx.batchCreateForm.parentCategory),
    ...{ class: "category-tree-select" },
    data: (__VLS_ctx.categorySelectTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    popperClass: "category-tree-select-popper",
    ...{ style: {} },
}));
const __VLS_195 = __VLS_194({
    modelValue: (__VLS_ctx.batchCreateForm.parentCategory),
    ...{ class: "category-tree-select" },
    data: (__VLS_ctx.categorySelectTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    popperClass: "category-tree-select-popper",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_194));
var __VLS_192;
var __VLS_188;
const __VLS_197 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_198 = __VLS_asFunctionalComponent(__VLS_197, new __VLS_197({
    data: (__VLS_ctx.batchCreateRows),
    border: true,
    ...{ class: "erp-table" },
}));
const __VLS_199 = __VLS_198({
    data: (__VLS_ctx.batchCreateRows),
    border: true,
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_198));
__VLS_200.slots.default;
const __VLS_201 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_202 = __VLS_asFunctionalComponent(__VLS_201, new __VLS_201({
    label: "序号",
    width: "70",
}));
const __VLS_203 = __VLS_202({
    label: "序号",
    width: "70",
}, ...__VLS_functionalComponentArgsRest(__VLS_202));
__VLS_204.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_204.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    ($index + 1);
}
var __VLS_204;
const __VLS_205 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_206 = __VLS_asFunctionalComponent(__VLS_205, new __VLS_205({
    label: "操作",
    width: "120",
}));
const __VLS_207 = __VLS_206({
    label: "操作",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_206));
__VLS_208.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_208.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_209 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_210 = __VLS_asFunctionalComponent(__VLS_209, new __VLS_209({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_211 = __VLS_210({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_210));
    let __VLS_213;
    let __VLS_214;
    let __VLS_215;
    const __VLS_216 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addBatchRow($index);
        }
    };
    __VLS_212.slots.default;
    var __VLS_212;
    const __VLS_217 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_218 = __VLS_asFunctionalComponent(__VLS_217, new __VLS_217({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_219 = __VLS_218({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_218));
    let __VLS_221;
    let __VLS_222;
    let __VLS_223;
    const __VLS_224 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeBatchRow($index);
        }
    };
    __VLS_220.slots.default;
    var __VLS_220;
}
var __VLS_208;
const __VLS_225 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_226 = __VLS_asFunctionalComponent(__VLS_225, new __VLS_225({
    label: "类别名称",
}));
const __VLS_227 = __VLS_226({
    label: "类别名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_226));
__VLS_228.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_228.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_229 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_230 = __VLS_asFunctionalComponent(__VLS_229, new __VLS_229({
        modelValue: (row.categoryName),
        placeholder: "请输入类别名称",
    }));
    const __VLS_231 = __VLS_230({
        modelValue: (row.categoryName),
        placeholder: "请输入类别名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_230));
}
var __VLS_228;
var __VLS_200;
{
    const { footer: __VLS_thisSlot } = __VLS_180.slots;
    const __VLS_233 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_234 = __VLS_asFunctionalComponent(__VLS_233, new __VLS_233({
        ...{ 'onClick': {} },
    }));
    const __VLS_235 = __VLS_234({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_234));
    let __VLS_237;
    let __VLS_238;
    let __VLS_239;
    const __VLS_240 = {
        onClick: (__VLS_ctx.closeBatchCreateDialog)
    };
    __VLS_236.slots.default;
    var __VLS_236;
    const __VLS_241 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_242 = __VLS_asFunctionalComponent(__VLS_241, new __VLS_241({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_243 = __VLS_242({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_242));
    let __VLS_245;
    let __VLS_246;
    let __VLS_247;
    const __VLS_248 = {
        onClick: (__VLS_ctx.handleBatchCreateSubmit)
    };
    __VLS_244.slots.default;
    var __VLS_244;
}
var __VLS_180;
/** @type {__VLS_StyleScopedClasses['item-management-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['category-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['category-tree-select']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['category-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['category-tree-select']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
// @ts-ignore
var __VLS_120 = __VLS_119;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ItemCategoryTree: ItemCategoryTree,
            ItemPaginationSection: ItemPaginationSection,
            currentPage: currentPage,
            pageSize: pageSize,
            total: total,
            tableLoading: tableLoading,
            emptyText: emptyText,
            selectedCount: selectedCount,
            tableHeight: tableHeight,
            statusOptions: statusOptions,
            toolbarButtons: toolbarButtons,
            query: query,
            createDialogVisible: createDialogVisible,
            batchCreateDialogVisible: batchCreateDialogVisible,
            createFormRef: createFormRef,
            selectedTreeNode: selectedTreeNode,
            isEditMode: isEditMode,
            createDialogTitle: createDialogTitle,
            batchCreateForm: batchCreateForm,
            batchCreateRows: batchCreateRows,
            createForm: createForm,
            categoryColumns: categoryColumns,
            categoryTableData: categoryTableData,
            categoryTree: categoryTree,
            categorySelectTree: categorySelectTree,
            createFormRules: createFormRules,
            handleSelectionChange: handleSelectionChange,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleTreeSelect: handleTreeSelect,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            openEditDialog: openEditDialog,
            closeCreateDialog: closeCreateDialog,
            closeBatchCreateDialog: closeBatchCreateDialog,
            addBatchRow: addBatchRow,
            removeBatchRow: removeBatchRow,
            handleCreateSubmit: handleCreateSubmit,
            handleBatchCreateSubmit: handleBatchCreateSubmit,
            handleDelete: handleDelete,
            handleToolbarAction: handleToolbarAction,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
