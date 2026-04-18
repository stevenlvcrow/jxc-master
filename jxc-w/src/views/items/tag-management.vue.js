/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { batchImportItemTagsApi, createItemTagApi, deleteItemTagApi, fetchItemTagsApi, updateItemTagApi, } from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
const sessionStore = useSessionStore();
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableHeight = 360;
const tableLoading = ref(false);
const emptyText = '当前机构暂无数据';
const tableData = ref([]);
const toolbarButtons = ['新增标签', '批量导入'];
const query = reactive({
    tagCode: '',
    tagName: '',
    itemName: '',
});
const createDialogVisible = ref(false);
const batchImportDialogVisible = ref(false);
const editingTagId = ref(null);
const dialogTitle = ref('新增标签');
const formRef = ref();
const form = reactive({
    tagCode: '',
    tagName: '',
    itemName: '',
});
const batchRows = ref([
    { tagName: '', itemName: '' },
]);
const formRules = {
    tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
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
const fetchList = async () => {
    tableLoading.value = true;
    try {
        const data = await fetchItemTagsApi({
            pageNo: currentPage.value,
            pageSize: pageSize.value,
            tagCode: query.tagCode.trim() || undefined,
            tagName: query.tagName.trim() || undefined,
            itemName: query.itemName.trim() || undefined,
        }, resolveItemOrgId());
        tableData.value = data.list ?? [];
        total.value = data.total ?? 0;
    }
    finally {
        tableLoading.value = false;
    }
};
const handleSearch = async () => {
    currentPage.value = 1;
    await fetchList();
};
const handleReset = async () => {
    query.tagCode = '';
    query.tagName = '';
    query.itemName = '';
    currentPage.value = 1;
    await fetchList();
};
const handlePageChange = async (page) => {
    currentPage.value = page;
    await fetchList();
};
const handlePageSizeChange = async (size) => {
    pageSize.value = size;
    currentPage.value = 1;
    await fetchList();
};
const resetForm = () => {
    editingTagId.value = null;
    form.tagCode = '';
    form.tagName = '';
    form.itemName = '';
};
const openCreateDialog = () => {
    resetForm();
    dialogTitle.value = '新增标签';
    createDialogVisible.value = true;
};
const openEditDialog = (row) => {
    editingTagId.value = row.id;
    form.tagCode = row.tagCode;
    form.tagName = row.tagName;
    form.itemName = row.itemName ?? '';
    dialogTitle.value = '编辑标签';
    createDialogVisible.value = true;
};
const closeCreateDialog = () => {
    createDialogVisible.value = false;
    formRef.value?.clearValidate();
};
const handleSubmit = async () => {
    if (!formRef.value) {
        return;
    }
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }
    const payload = {
        tagCode: editingTagId.value !== null ? form.tagCode.trim() : undefined,
        tagName: form.tagName.trim(),
        itemName: form.itemName.trim() || undefined,
    };
    if (editingTagId.value) {
        await updateItemTagApi(editingTagId.value, payload, resolveItemOrgId());
        ElMessage.success('编辑标签成功');
    }
    else {
        await createItemTagApi(payload, resolveItemOrgId());
        ElMessage.success('新增标签成功');
    }
    await fetchList();
    closeCreateDialog();
};
const handleDelete = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除标签“${row.tagName}”吗？`, '删除确认', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
        await deleteItemTagApi(row.id, resolveItemOrgId());
        ElMessage.success('删除标签成功');
        await fetchList();
    }
    catch {
        // 用户取消删除
    }
};
const resetBatchRows = () => {
    batchRows.value = [{ tagName: '', itemName: '' }];
};
const openBatchDialog = () => {
    resetBatchRows();
    batchImportDialogVisible.value = true;
};
const closeBatchDialog = () => {
    batchImportDialogVisible.value = false;
};
const addBatchRow = (index) => {
    batchRows.value.splice(index + 1, 0, { tagName: '', itemName: '' });
};
const removeBatchRow = (index) => {
    if (batchRows.value.length === 1) {
        ElMessage.warning('至少保留一行');
        return;
    }
    batchRows.value.splice(index, 1);
};
const handleBatchImportSubmit = async () => {
    const rows = batchRows.value.map((row) => ({
        tagName: row.tagName.trim(),
        itemName: row.itemName.trim(),
    }));
    const emptyName = rows.findIndex((row) => !row.tagName);
    if (emptyName !== -1) {
        ElMessage.warning(`第 ${emptyName + 1} 行标签名称不能为空`);
        return;
    }
    await batchImportItemTagsApi({
        items: rows.map((row) => ({
            tagName: row.tagName,
            itemName: row.itemName || undefined,
        })),
    }, resolveItemOrgId());
    ElMessage.success('批量导入完成');
    closeBatchDialog();
    await fetchList();
};
const handleToolbarAction = (action) => {
    if (action === '新增标签') {
        openCreateDialog();
        return;
    }
    if (action === '批量导入') {
        openBatchDialog();
        return;
    }
    ElMessage.info(`${action}功能待接入`);
};
onMounted(async () => {
    await fetchList();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
const __VLS_0 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "compact-filter-bar" },
}));
const __VLS_2 = __VLS_1({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    label: "标签编码",
}));
const __VLS_6 = __VLS_5({
    label: "标签编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    modelValue: (__VLS_ctx.query.tagCode),
    placeholder: "请输入标签编码",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_10 = __VLS_9({
    modelValue: (__VLS_ctx.query.tagCode),
    placeholder: "请输入标签编码",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
var __VLS_7;
const __VLS_12 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    label: "标签名称",
}));
const __VLS_14 = __VLS_13({
    label: "标签名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
const __VLS_16 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    modelValue: (__VLS_ctx.query.tagName),
    placeholder: "请输入标签名称",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_18 = __VLS_17({
    modelValue: (__VLS_ctx.query.tagName),
    placeholder: "请输入标签名称",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
var __VLS_15;
const __VLS_20 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    label: "物品",
}));
const __VLS_22 = __VLS_21({
    label: "物品",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_23.slots.default;
const __VLS_24 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    modelValue: (__VLS_ctx.query.itemName),
    placeholder: "请输入物品",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_26 = __VLS_25({
    modelValue: (__VLS_ctx.query.itemName),
    placeholder: "请输入物品",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
var __VLS_23;
const __VLS_28 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({}));
const __VLS_30 = __VLS_29({}, ...__VLS_functionalComponentArgsRest(__VLS_29));
__VLS_31.slots.default;
const __VLS_32 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_34 = __VLS_33({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
let __VLS_36;
let __VLS_37;
let __VLS_38;
const __VLS_39 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_35.slots.default;
var __VLS_35;
const __VLS_40 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
    ...{ 'onClick': {} },
}));
const __VLS_42 = __VLS_41({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
let __VLS_44;
let __VLS_45;
let __VLS_46;
const __VLS_47 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_43.slots.default;
var __VLS_43;
var __VLS_31;
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
for (const [button, index] of __VLS_getVForSourceType((__VLS_ctx.toolbarButtons))) {
    const __VLS_48 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
    }));
    const __VLS_50 = __VLS_49({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_49));
    let __VLS_52;
    let __VLS_53;
    let __VLS_54;
    const __VLS_55 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleToolbarAction(button);
        }
    };
    __VLS_51.slots.default;
    (button);
    var __VLS_51;
}
const __VLS_56 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    data: (__VLS_ctx.tableData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
    emptyText: (__VLS_ctx.emptyText),
}));
const __VLS_58 = __VLS_57({
    data: (__VLS_ctx.tableData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
    emptyText: (__VLS_ctx.emptyText),
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.tableLoading) }, null, null);
__VLS_59.slots.default;
const __VLS_60 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
    prop: "index",
    label: "序号",
    width: "60",
    align: "center",
}));
const __VLS_62 = __VLS_61({
    prop: "index",
    label: "序号",
    width: "60",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_61));
const __VLS_64 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
    prop: "tagCode",
    label: "标签编码",
    minWidth: "160",
    showOverflowTooltip: true,
}));
const __VLS_66 = __VLS_65({
    prop: "tagCode",
    label: "标签编码",
    minWidth: "160",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_65));
const __VLS_68 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
    prop: "tagName",
    label: "标签名称",
    minWidth: "180",
    showOverflowTooltip: true,
}));
const __VLS_70 = __VLS_69({
    prop: "tagName",
    label: "标签名称",
    minWidth: "180",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_69));
const __VLS_72 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_74 = __VLS_73({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_73));
const __VLS_76 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
    prop: "updatedAt",
    label: "最后修改时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_78 = __VLS_77({
    prop: "updatedAt",
    label: "最后修改时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_77));
const __VLS_80 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
    label: "操作",
    width: "120",
    align: "center",
    fixed: "right",
}));
const __VLS_82 = __VLS_81({
    label: "操作",
    width: "120",
    align: "center",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_81));
__VLS_83.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_83.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_84 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_86 = __VLS_85({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_85));
    let __VLS_88;
    let __VLS_89;
    let __VLS_90;
    const __VLS_91 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditDialog(row);
        }
    };
    __VLS_87.slots.default;
    var __VLS_87;
    const __VLS_92 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_94 = __VLS_93({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_93));
    let __VLS_96;
    let __VLS_97;
    let __VLS_98;
    const __VLS_99 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDelete(row);
        }
    };
    __VLS_95.slots.default;
    var __VLS_95;
}
var __VLS_83;
var __VLS_59;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.total);
const __VLS_100 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.total),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}));
const __VLS_102 = __VLS_101({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.total),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_101));
let __VLS_104;
let __VLS_105;
let __VLS_106;
const __VLS_107 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_108 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_103;
const __VLS_109 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_110 = __VLS_asFunctionalComponent(__VLS_109, new __VLS_109({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.dialogTitle),
    width: "520px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_111 = __VLS_110({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.dialogTitle),
    width: "520px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_110));
let __VLS_113;
let __VLS_114;
let __VLS_115;
const __VLS_116 = {
    onClosed: (__VLS_ctx.closeCreateDialog)
};
__VLS_112.slots.default;
const __VLS_117 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_118 = __VLS_asFunctionalComponent(__VLS_117, new __VLS_117({
    ref: "formRef",
    model: (__VLS_ctx.form),
    rules: (__VLS_ctx.formRules),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_119 = __VLS_118({
    ref: "formRef",
    model: (__VLS_ctx.form),
    rules: (__VLS_ctx.formRules),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_118));
/** @type {typeof __VLS_ctx.formRef} */ ;
var __VLS_121 = {};
__VLS_120.slots.default;
if (__VLS_ctx.editingTagId !== null) {
    const __VLS_123 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
        label: "标签编码",
        prop: "tagCode",
    }));
    const __VLS_125 = __VLS_124({
        label: "标签编码",
        prop: "tagCode",
    }, ...__VLS_functionalComponentArgsRest(__VLS_124));
    __VLS_126.slots.default;
    const __VLS_127 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
        modelValue: (__VLS_ctx.form.tagCode),
        placeholder: "请输入标签编码",
    }));
    const __VLS_129 = __VLS_128({
        modelValue: (__VLS_ctx.form.tagCode),
        placeholder: "请输入标签编码",
    }, ...__VLS_functionalComponentArgsRest(__VLS_128));
    var __VLS_126;
}
const __VLS_131 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({
    label: "标签名称",
    prop: "tagName",
}));
const __VLS_133 = __VLS_132({
    label: "标签名称",
    prop: "tagName",
}, ...__VLS_functionalComponentArgsRest(__VLS_132));
__VLS_134.slots.default;
const __VLS_135 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({
    modelValue: (__VLS_ctx.form.tagName),
    placeholder: "请输入标签名称",
}));
const __VLS_137 = __VLS_136({
    modelValue: (__VLS_ctx.form.tagName),
    placeholder: "请输入标签名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_136));
var __VLS_134;
const __VLS_139 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
    label: "物品",
}));
const __VLS_141 = __VLS_140({
    label: "物品",
}, ...__VLS_functionalComponentArgsRest(__VLS_140));
__VLS_142.slots.default;
const __VLS_143 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
    modelValue: (__VLS_ctx.form.itemName),
    placeholder: "请输入物品名称",
}));
const __VLS_145 = __VLS_144({
    modelValue: (__VLS_ctx.form.itemName),
    placeholder: "请输入物品名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_144));
var __VLS_142;
var __VLS_120;
{
    const { footer: __VLS_thisSlot } = __VLS_112.slots;
    const __VLS_147 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({
        ...{ 'onClick': {} },
    }));
    const __VLS_149 = __VLS_148({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_148));
    let __VLS_151;
    let __VLS_152;
    let __VLS_153;
    const __VLS_154 = {
        onClick: (__VLS_ctx.closeCreateDialog)
    };
    __VLS_150.slots.default;
    var __VLS_150;
    const __VLS_155 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_157 = __VLS_156({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_156));
    let __VLS_159;
    let __VLS_160;
    let __VLS_161;
    const __VLS_162 = {
        onClick: (__VLS_ctx.handleSubmit)
    };
    __VLS_158.slots.default;
    var __VLS_158;
}
var __VLS_112;
const __VLS_163 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.batchImportDialogVisible),
    title: "批量导入标签",
    width: "760px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_165 = __VLS_164({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.batchImportDialogVisible),
    title: "批量导入标签",
    width: "760px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_164));
let __VLS_167;
let __VLS_168;
let __VLS_169;
const __VLS_170 = {
    onClosed: (__VLS_ctx.closeBatchDialog)
};
__VLS_166.slots.default;
const __VLS_171 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({
    data: (__VLS_ctx.batchRows),
    border: true,
    ...{ class: "erp-table" },
}));
const __VLS_173 = __VLS_172({
    data: (__VLS_ctx.batchRows),
    border: true,
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_172));
__VLS_174.slots.default;
const __VLS_175 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
    label: "序号",
    width: "70",
}));
const __VLS_177 = __VLS_176({
    label: "序号",
    width: "70",
}, ...__VLS_functionalComponentArgsRest(__VLS_176));
__VLS_178.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_178.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    ($index + 1);
}
var __VLS_178;
const __VLS_179 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({
    label: "操作",
    width: "120",
}));
const __VLS_181 = __VLS_180({
    label: "操作",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_180));
__VLS_182.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_182.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_183 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_185 = __VLS_184({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_184));
    let __VLS_187;
    let __VLS_188;
    let __VLS_189;
    const __VLS_190 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addBatchRow($index);
        }
    };
    __VLS_186.slots.default;
    var __VLS_186;
    const __VLS_191 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_193 = __VLS_192({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_192));
    let __VLS_195;
    let __VLS_196;
    let __VLS_197;
    const __VLS_198 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeBatchRow($index);
        }
    };
    __VLS_194.slots.default;
    var __VLS_194;
}
var __VLS_182;
const __VLS_199 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
    label: "标签名称",
    width: "180",
}));
const __VLS_201 = __VLS_200({
    label: "标签名称",
    width: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_200));
__VLS_202.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_202.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_203 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
        modelValue: (row.tagName),
        placeholder: "请输入标签名称",
    }));
    const __VLS_205 = __VLS_204({
        modelValue: (row.tagName),
        placeholder: "请输入标签名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_204));
}
var __VLS_202;
const __VLS_207 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    label: "物品",
}));
const __VLS_209 = __VLS_208({
    label: "物品",
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
__VLS_210.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_210.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_211 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_212 = __VLS_asFunctionalComponent(__VLS_211, new __VLS_211({
        modelValue: (row.itemName),
        placeholder: "请输入物品名称",
    }));
    const __VLS_213 = __VLS_212({
        modelValue: (row.itemName),
        placeholder: "请输入物品名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_212));
}
var __VLS_210;
var __VLS_174;
{
    const { footer: __VLS_thisSlot } = __VLS_166.slots;
    const __VLS_215 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
        ...{ 'onClick': {} },
    }));
    const __VLS_217 = __VLS_216({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_216));
    let __VLS_219;
    let __VLS_220;
    let __VLS_221;
    const __VLS_222 = {
        onClick: (__VLS_ctx.closeBatchDialog)
    };
    __VLS_218.slots.default;
    var __VLS_218;
    const __VLS_223 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_225 = __VLS_224({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_224));
    let __VLS_227;
    let __VLS_228;
    let __VLS_229;
    const __VLS_230 = {
        onClick: (__VLS_ctx.handleBatchImportSubmit)
    };
    __VLS_226.slots.default;
    var __VLS_226;
}
var __VLS_166;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
// @ts-ignore
var __VLS_122 = __VLS_121;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            currentPage: currentPage,
            pageSize: pageSize,
            total: total,
            tableHeight: tableHeight,
            tableLoading: tableLoading,
            emptyText: emptyText,
            tableData: tableData,
            toolbarButtons: toolbarButtons,
            query: query,
            createDialogVisible: createDialogVisible,
            batchImportDialogVisible: batchImportDialogVisible,
            editingTagId: editingTagId,
            dialogTitle: dialogTitle,
            formRef: formRef,
            form: form,
            batchRows: batchRows,
            formRules: formRules,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            openEditDialog: openEditDialog,
            closeCreateDialog: closeCreateDialog,
            handleSubmit: handleSubmit,
            handleDelete: handleDelete,
            closeBatchDialog: closeBatchDialog,
            addBatchRow: addBatchRow,
            removeBatchRow: removeBatchRow,
            handleBatchImportSubmit: handleBatchImportSubmit,
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
