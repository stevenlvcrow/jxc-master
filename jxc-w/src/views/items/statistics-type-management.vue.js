/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import ItemPaginationSection from './components/ItemPaginationSection.vue';
import { createItemStatisticsTypeApi, exportItemStatisticsTypesApi, fetchItemStatisticsTypeDetailApi, fetchItemStatisticsTypesApi, } from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
const sessionStore = useSessionStore();
const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const tableHeight = 360;
const tableLoading = ref(false);
const createSubmitting = ref(false);
const detailLoading = ref(false);
const exportLoading = ref(false);
const toolbarButtons = ['新增统计类型', '批量导出'];
const createDialogVisible = ref(false);
const createFormRef = ref();
const createForm = reactive({
    name: '',
    statisticsCategory: '成本类',
});
const createFormRules = {
    name: [{ required: true, message: '请输入统计类型名称', trigger: 'blur' }],
    statisticsCategory: [{ required: true, message: '请选择统计分类', trigger: 'change' }],
};
const detailDialogVisible = ref(false);
const detailData = ref(null);
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
        const result = await fetchItemStatisticsTypesApi({
            pageNo: currentPage.value,
            pageSize: pageSize.value,
        }, resolveItemOrgId());
        tableData.value = result.list ?? [];
        total.value = result.total ?? 0;
    }
    finally {
        tableLoading.value = false;
    }
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
const openCreateDialog = () => {
    createForm.name = '';
    createForm.statisticsCategory = '成本类';
    createDialogVisible.value = true;
};
const submitCreate = async () => {
    if (!createFormRef.value) {
        return;
    }
    const valid = await createFormRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }
    createSubmitting.value = true;
    try {
        await createItemStatisticsTypeApi({
            name: createForm.name.trim(),
            statisticsCategory: createForm.statisticsCategory,
        }, resolveItemOrgId());
        ElMessage.success('新增统计类型成功');
        createDialogVisible.value = false;
        currentPage.value = 1;
        await fetchList();
    }
    finally {
        createSubmitting.value = false;
    }
};
const openDetailDialog = async (row) => {
    detailDialogVisible.value = true;
    detailLoading.value = true;
    detailData.value = null;
    try {
        detailData.value = await fetchItemStatisticsTypeDetailApi(row.id, resolveItemOrgId());
    }
    finally {
        detailLoading.value = false;
    }
};
const toCsvCell = (value) => {
    const text = String(value ?? '');
    const escaped = text.replace(/"/g, '""');
    return `"${escaped}"`;
};
const handleExport = async () => {
    exportLoading.value = true;
    try {
        const result = await exportItemStatisticsTypesApi(undefined, resolveItemOrgId());
        const header = ['编码', '名称', '统计分类', '创建类型', '修改时间'];
        const lines = [header.map(toCsvCell).join(',')];
        (result.rows ?? []).forEach((row) => {
            lines.push([
                row.code,
                row.name,
                row.statisticsCategory,
                row.createType,
                row.modifiedTime,
            ].map(toCsvCell).join(','));
        });
        const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = result.fileName || 'statistics-types.csv';
        document.body.appendChild(link);
        link.click();
        URL.revokeObjectURL(link.href);
        document.body.removeChild(link);
        ElMessage.success('导出成功');
    }
    finally {
        exportLoading.value = false;
    }
};
const handleToolbarAction = async (action) => {
    if (action === '新增统计类型') {
        openCreateDialog();
        return;
    }
    if (action === '批量导出') {
        await handleExport();
        return;
    }
};
watch(() => sessionStore.currentOrgId, async () => {
    currentPage.value = 1;
    await fetchList();
});
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
for (const [button, index] of __VLS_getVForSourceType((__VLS_ctx.toolbarButtons))) {
    const __VLS_0 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
        loading: (button === '批量导出' ? __VLS_ctx.exportLoading : false),
    }));
    const __VLS_2 = __VLS_1({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
        loading: (button === '批量导出' ? __VLS_ctx.exportLoading : false),
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    let __VLS_4;
    let __VLS_5;
    let __VLS_6;
    const __VLS_7 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleToolbarAction(button);
        }
    };
    __VLS_3.slots.default;
    (button);
    var __VLS_3;
}
const __VLS_8 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    data: (__VLS_ctx.tableData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
}));
const __VLS_10 = __VLS_9({
    data: (__VLS_ctx.tableData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.tableLoading) }, null, null);
__VLS_11.slots.default;
const __VLS_12 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    prop: "index",
    label: "序号",
    width: "60",
    align: "center",
}));
const __VLS_14 = __VLS_13({
    prop: "index",
    label: "序号",
    width: "60",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
const __VLS_16 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    prop: "code",
    label: "编码",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_18 = __VLS_17({
    prop: "code",
    label: "编码",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
const __VLS_20 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    prop: "name",
    label: "名称",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_22 = __VLS_21({
    prop: "name",
    label: "名称",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
const __VLS_24 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    prop: "statisticsCategory",
    label: "统计分类",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_26 = __VLS_25({
    prop: "statisticsCategory",
    label: "统计分类",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
const __VLS_28 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    prop: "createType",
    label: "创建类型",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_30 = __VLS_29({
    prop: "createType",
    label: "创建类型",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
const __VLS_32 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    prop: "modifiedTime",
    label: "修改时间",
    minWidth: "150",
    align: "center",
}));
const __VLS_34 = __VLS_33({
    prop: "modifiedTime",
    label: "修改时间",
    minWidth: "150",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
const __VLS_36 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    label: "操作",
    width: "80",
    align: "center",
    fixed: "right",
}));
const __VLS_38 = __VLS_37({
    label: "操作",
    width: "80",
    align: "center",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
__VLS_39.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_39.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_40 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        ...{ 'onClick': {} },
        link: true,
        type: "primary",
    }));
    const __VLS_42 = __VLS_41({
        ...{ 'onClick': {} },
        link: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
    let __VLS_44;
    let __VLS_45;
    let __VLS_46;
    const __VLS_47 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openDetailDialog(row);
        }
    };
    __VLS_43.slots.default;
    var __VLS_43;
}
var __VLS_39;
var __VLS_11;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_48 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}));
const __VLS_49 = __VLS_48({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
}, ...__VLS_functionalComponentArgsRest(__VLS_48));
let __VLS_51;
let __VLS_52;
let __VLS_53;
const __VLS_54 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_55 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_50;
const __VLS_56 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    modelValue: (__VLS_ctx.createDialogVisible),
    title: "新增统计类型",
    width: "520px",
    destroyOnClose: true,
}));
const __VLS_58 = __VLS_57({
    modelValue: (__VLS_ctx.createDialogVisible),
    title: "新增统计类型",
    width: "520px",
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
__VLS_59.slots.default;
const __VLS_60 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
    ref: "createFormRef",
    model: (__VLS_ctx.createForm),
    rules: (__VLS_ctx.createFormRules),
    labelWidth: "100px",
}));
const __VLS_62 = __VLS_61({
    ref: "createFormRef",
    model: (__VLS_ctx.createForm),
    rules: (__VLS_ctx.createFormRules),
    labelWidth: "100px",
}, ...__VLS_functionalComponentArgsRest(__VLS_61));
/** @type {typeof __VLS_ctx.createFormRef} */ ;
var __VLS_64 = {};
__VLS_63.slots.default;
const __VLS_66 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "名称",
    prop: "name",
}));
const __VLS_68 = __VLS_67({
    label: "名称",
    prop: "name",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
const __VLS_70 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    modelValue: (__VLS_ctx.createForm.name),
    maxlength: "64",
    placeholder: "请输入统计类型名称",
}));
const __VLS_72 = __VLS_71({
    modelValue: (__VLS_ctx.createForm.name),
    maxlength: "64",
    placeholder: "请输入统计类型名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
var __VLS_69;
const __VLS_74 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    label: "统计分类",
    prop: "statisticsCategory",
}));
const __VLS_76 = __VLS_75({
    label: "统计分类",
    prop: "statisticsCategory",
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
__VLS_77.slots.default;
const __VLS_78 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    modelValue: (__VLS_ctx.createForm.statisticsCategory),
    ...{ style: {} },
}));
const __VLS_80 = __VLS_79({
    modelValue: (__VLS_ctx.createForm.statisticsCategory),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
__VLS_81.slots.default;
const __VLS_82 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
    label: "成本类",
    value: "成本类",
}));
const __VLS_84 = __VLS_83({
    label: "成本类",
    value: "成本类",
}, ...__VLS_functionalComponentArgsRest(__VLS_83));
const __VLS_86 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
    label: "费用类",
    value: "费用类",
}));
const __VLS_88 = __VLS_87({
    label: "费用类",
    value: "费用类",
}, ...__VLS_functionalComponentArgsRest(__VLS_87));
var __VLS_81;
var __VLS_77;
var __VLS_63;
{
    const { footer: __VLS_thisSlot } = __VLS_59.slots;
    const __VLS_90 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
        ...{ 'onClick': {} },
    }));
    const __VLS_92 = __VLS_91({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_91));
    let __VLS_94;
    let __VLS_95;
    let __VLS_96;
    const __VLS_97 = {
        onClick: (...[$event]) => {
            __VLS_ctx.createDialogVisible = false;
        }
    };
    __VLS_93.slots.default;
    var __VLS_93;
    const __VLS_98 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.createSubmitting),
    }));
    const __VLS_100 = __VLS_99({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.createSubmitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_99));
    let __VLS_102;
    let __VLS_103;
    let __VLS_104;
    const __VLS_105 = {
        onClick: (__VLS_ctx.submitCreate)
    };
    __VLS_101.slots.default;
    var __VLS_101;
}
var __VLS_59;
const __VLS_106 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    modelValue: (__VLS_ctx.detailDialogVisible),
    title: "统计类型详情",
    width: "560px",
    destroyOnClose: true,
}));
const __VLS_108 = __VLS_107({
    modelValue: (__VLS_ctx.detailDialogVisible),
    title: "统计类型详情",
    width: "560px",
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
__VLS_109.slots.default;
const __VLS_110 = {}.ElSkeleton;
/** @type {[typeof __VLS_components.ElSkeleton, typeof __VLS_components.elSkeleton, typeof __VLS_components.ElSkeleton, typeof __VLS_components.elSkeleton, ]} */ ;
// @ts-ignore
const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
    rows: (5),
    animated: true,
    loading: (__VLS_ctx.detailLoading),
}));
const __VLS_112 = __VLS_111({
    rows: (5),
    animated: true,
    loading: (__VLS_ctx.detailLoading),
}, ...__VLS_functionalComponentArgsRest(__VLS_111));
__VLS_113.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_113.slots;
    if (__VLS_ctx.detailData) {
        const __VLS_114 = {}.ElDescriptions;
        /** @type {[typeof __VLS_components.ElDescriptions, typeof __VLS_components.elDescriptions, typeof __VLS_components.ElDescriptions, typeof __VLS_components.elDescriptions, ]} */ ;
        // @ts-ignore
        const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
            column: (1),
            border: true,
        }));
        const __VLS_116 = __VLS_115({
            column: (1),
            border: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_115));
        __VLS_117.slots.default;
        const __VLS_118 = {}.ElDescriptionsItem;
        /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
        // @ts-ignore
        const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
            label: "编码",
        }));
        const __VLS_120 = __VLS_119({
            label: "编码",
        }, ...__VLS_functionalComponentArgsRest(__VLS_119));
        __VLS_121.slots.default;
        (__VLS_ctx.detailData.code);
        var __VLS_121;
        const __VLS_122 = {}.ElDescriptionsItem;
        /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
        // @ts-ignore
        const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
            label: "名称",
        }));
        const __VLS_124 = __VLS_123({
            label: "名称",
        }, ...__VLS_functionalComponentArgsRest(__VLS_123));
        __VLS_125.slots.default;
        (__VLS_ctx.detailData.name);
        var __VLS_125;
        const __VLS_126 = {}.ElDescriptionsItem;
        /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
        // @ts-ignore
        const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
            label: "统计分类",
        }));
        const __VLS_128 = __VLS_127({
            label: "统计分类",
        }, ...__VLS_functionalComponentArgsRest(__VLS_127));
        __VLS_129.slots.default;
        (__VLS_ctx.detailData.statisticsCategory);
        var __VLS_129;
        const __VLS_130 = {}.ElDescriptionsItem;
        /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
        // @ts-ignore
        const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
            label: "创建类型",
        }));
        const __VLS_132 = __VLS_131({
            label: "创建类型",
        }, ...__VLS_functionalComponentArgsRest(__VLS_131));
        __VLS_133.slots.default;
        (__VLS_ctx.detailData.createType);
        var __VLS_133;
        const __VLS_134 = {}.ElDescriptionsItem;
        /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
        // @ts-ignore
        const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
            label: "修改时间",
        }));
        const __VLS_136 = __VLS_135({
            label: "修改时间",
        }, ...__VLS_functionalComponentArgsRest(__VLS_135));
        __VLS_137.slots.default;
        (__VLS_ctx.detailData.modifiedTime);
        var __VLS_137;
        var __VLS_117;
    }
}
var __VLS_113;
var __VLS_109;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
// @ts-ignore
var __VLS_65 = __VLS_64;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ItemPaginationSection: ItemPaginationSection,
            tableData: tableData,
            total: total,
            currentPage: currentPage,
            pageSize: pageSize,
            tableHeight: tableHeight,
            tableLoading: tableLoading,
            createSubmitting: createSubmitting,
            detailLoading: detailLoading,
            exportLoading: exportLoading,
            toolbarButtons: toolbarButtons,
            createDialogVisible: createDialogVisible,
            createFormRef: createFormRef,
            createForm: createForm,
            createFormRules: createFormRules,
            detailDialogVisible: detailDialogVisible,
            detailData: detailData,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            submitCreate: submitCreate,
            openDetailDialog: openDetailDialog,
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
