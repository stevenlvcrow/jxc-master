/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { ArrowDown, Delete, Plus, RefreshRight, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
const statusTree = [
    { value: '草稿', label: '草稿' },
    { value: '已提交', label: '已提交' },
    { value: '已审核', label: '已审核' },
];
const resultTree = [
    { value: '成功', label: '成功' },
    { value: '失败', label: '失败' },
    { value: '处理中', label: '处理中' },
];
const supplierTree = [
    {
        value: 'group-a',
        label: '华东供应商组',
        children: [
            { value: '鲜达食品', label: '鲜达食品' },
            { value: '优选农场', label: '优选农场' },
        ],
    },
];
const reasonTree = [
    { value: '合同调价', label: '合同调价' },
    { value: '市场波动', label: '市场波动' },
    { value: '活动临调', label: '活动临调' },
];
const itemTree = [
    {
        value: '原料',
        label: '原料',
        children: [
            { value: '鸡胸肉', label: '鸡胸肉' },
            { value: '牛腩', label: '牛腩' },
        ],
    },
    {
        value: '饮品',
        label: '饮品',
        children: [
            { value: '酸梅汤', label: '酸梅汤' },
        ],
    },
];
const creatorTree = [
    {
        value: 'pricing-team',
        label: '定价组',
        children: [
            { value: '张敏', label: '张敏' },
            { value: '李娜', label: '李娜' },
        ],
    },
];
const query = reactive({
    documentDate: '',
    adjustmentCode: '',
    documentStatus: '',
    adjustmentResult: '',
    remark: '',
    supplier: '',
    adjustmentReason: '',
    itemName: '',
    creator: '',
});
const tableData = [
    {
        id: 1,
        adjustmentCode: 'ADJ-202604-001',
        adjustmentReason: '合同调价',
        documentStatus: '已审核',
        adjustmentResult: '成功',
        documentDate: '2026-04-13',
        remark: '四月统配价格调整',
        creator: '张敏',
        createdAt: '2026-04-13 10:22:00',
    },
    {
        id: 2,
        adjustmentCode: 'ADJ-202604-002',
        adjustmentReason: '市场波动',
        documentStatus: '已提交',
        adjustmentResult: '处理中',
        documentDate: '2026-04-12',
        remark: '牛肉原料上浮',
        creator: '李娜',
        createdAt: '2026-04-12 14:18:00',
    },
    {
        id: 3,
        adjustmentCode: 'ADJ-202604-003',
        adjustmentReason: '活动临调',
        documentStatus: '草稿',
        adjustmentResult: '失败',
        documentDate: '2026-04-11',
        remark: '活动价未生效',
        creator: '王磊',
        createdAt: '2026-04-11 09:05:00',
    },
];
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref([]);
const filteredRows = computed(() => {
    const adjustmentCodeKeyword = query.adjustmentCode.trim().toLowerCase();
    const remarkKeyword = query.remark.trim().toLowerCase();
    return tableData.filter((row) => {
        const matchedDate = !query.documentDate || row.documentDate === query.documentDate;
        const matchedCode = !adjustmentCodeKeyword || row.adjustmentCode.toLowerCase().includes(adjustmentCodeKeyword);
        const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
        const matchedResult = !query.adjustmentResult || row.adjustmentResult === query.adjustmentResult;
        const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
        const matchedReason = !query.adjustmentReason || row.adjustmentReason === query.adjustmentReason;
        const matchedCreator = !query.creator || row.creator === query.creator;
        return matchedDate && matchedCode && matchedStatus && matchedResult && matchedRemark && matchedReason && matchedCreator;
    });
});
const pagedRows = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredRows.value.slice(start, start + pageSize.value);
});
const handleSearch = () => {
    currentPage.value = 1;
};
const handleReset = () => {
    query.documentDate = '';
    query.adjustmentCode = '';
    query.documentStatus = '';
    query.adjustmentResult = '';
    query.remark = '';
    query.supplier = '';
    query.adjustmentReason = '';
    query.itemName = '';
    query.creator = '';
    currentPage.value = 1;
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
};
const handleTableSettingCommand = (command) => {
    ElMessage.info(`表格设置：${String(command)}`);
};
const handleSelectionChange = (rows) => {
    selectedIds.value = rows.map((row) => row.id);
};
const handleView = (row) => {
    ElMessage.info(`查看：${row.adjustmentCode}`);
};
const handleEdit = (row) => {
    ElMessage.info(`编辑：${row.adjustmentCode}`);
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
/** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
    model: (__VLS_ctx.query),
}));
const __VLS_1 = __VLS_0({
    model: (__VLS_ctx.query),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
const __VLS_3 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
    label: "单据日期",
}));
const __VLS_5 = __VLS_4({
    label: "单据日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.documentDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择单据日期",
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.documentDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择单据日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
var __VLS_6;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "调整单号",
}));
const __VLS_13 = __VLS_12({
    label: "调整单号",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.query.adjustmentCode),
    placeholder: "请输入调整单号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.query.adjustmentCode),
    placeholder: "请输入调整单号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
var __VLS_14;
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "单据状态",
}));
const __VLS_21 = __VLS_20({
    label: "单据状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    modelValue: (__VLS_ctx.query.documentStatus),
    data: (__VLS_ctx.statusTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    ...{ style: {} },
}));
const __VLS_25 = __VLS_24({
    modelValue: (__VLS_ctx.query.documentStatus),
    data: (__VLS_ctx.statusTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
var __VLS_22;
const __VLS_27 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    label: "调整结果",
}));
const __VLS_29 = __VLS_28({
    label: "调整结果",
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    modelValue: (__VLS_ctx.query.adjustmentResult),
    data: (__VLS_ctx.resultTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    ...{ style: {} },
}));
const __VLS_33 = __VLS_32({
    modelValue: (__VLS_ctx.query.adjustmentResult),
    data: (__VLS_ctx.resultTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
var __VLS_30;
const __VLS_35 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    label: "备注",
}));
const __VLS_37 = __VLS_36({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
__VLS_38.slots.default;
const __VLS_39 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_41 = __VLS_40({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
var __VLS_38;
const __VLS_43 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    label: "供应商",
}));
const __VLS_45 = __VLS_44({
    label: "供应商",
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
__VLS_46.slots.default;
const __VLS_47 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({
    modelValue: (__VLS_ctx.query.supplier),
    data: (__VLS_ctx.supplierTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_49 = __VLS_48({
    modelValue: (__VLS_ctx.query.supplier),
    data: (__VLS_ctx.supplierTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_48));
var __VLS_46;
const __VLS_51 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
    label: "调整原因",
}));
const __VLS_53 = __VLS_52({
    label: "调整原因",
}, ...__VLS_functionalComponentArgsRest(__VLS_52));
__VLS_54.slots.default;
const __VLS_55 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({
    modelValue: (__VLS_ctx.query.adjustmentReason),
    data: (__VLS_ctx.reasonTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    ...{ style: {} },
}));
const __VLS_57 = __VLS_56({
    modelValue: (__VLS_ctx.query.adjustmentReason),
    data: (__VLS_ctx.reasonTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_56));
var __VLS_54;
const __VLS_59 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
    label: "物品",
}));
const __VLS_61 = __VLS_60({
    label: "物品",
}, ...__VLS_functionalComponentArgsRest(__VLS_60));
__VLS_62.slots.default;
const __VLS_63 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({
    modelValue: (__VLS_ctx.query.itemName),
    data: (__VLS_ctx.itemTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_65 = __VLS_64({
    modelValue: (__VLS_ctx.query.itemName),
    data: (__VLS_ctx.itemTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_64));
var __VLS_62;
const __VLS_67 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({
    label: "创建人",
}));
const __VLS_69 = __VLS_68({
    label: "创建人",
}, ...__VLS_functionalComponentArgsRest(__VLS_68));
__VLS_70.slots.default;
const __VLS_71 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
    modelValue: (__VLS_ctx.query.creator),
    data: (__VLS_ctx.creatorTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_73 = __VLS_72({
    modelValue: (__VLS_ctx.query.creator),
    data: (__VLS_ctx.creatorTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_72));
var __VLS_70;
const __VLS_75 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({}));
const __VLS_77 = __VLS_76({}, ...__VLS_functionalComponentArgsRest(__VLS_76));
__VLS_78.slots.default;
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
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_82.slots.default;
const __VLS_87 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({}));
const __VLS_89 = __VLS_88({}, ...__VLS_functionalComponentArgsRest(__VLS_88));
__VLS_90.slots.default;
const __VLS_91 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({}));
const __VLS_93 = __VLS_92({}, ...__VLS_functionalComponentArgsRest(__VLS_92));
var __VLS_90;
var __VLS_82;
const __VLS_95 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({
    ...{ 'onClick': {} },
}));
const __VLS_97 = __VLS_96({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_96));
let __VLS_99;
let __VLS_100;
let __VLS_101;
const __VLS_102 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_98.slots.default;
const __VLS_103 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({}));
const __VLS_105 = __VLS_104({}, ...__VLS_functionalComponentArgsRest(__VLS_104));
__VLS_106.slots.default;
const __VLS_107 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({}));
const __VLS_109 = __VLS_108({}, ...__VLS_functionalComponentArgsRest(__VLS_108));
var __VLS_106;
var __VLS_98;
var __VLS_78;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_111 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_113 = __VLS_112({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_112));
let __VLS_115;
let __VLS_116;
let __VLS_117;
const __VLS_118 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('新增');
    }
};
__VLS_114.slots.default;
const __VLS_119 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({}));
const __VLS_121 = __VLS_120({}, ...__VLS_functionalComponentArgsRest(__VLS_120));
__VLS_122.slots.default;
const __VLS_123 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({}));
const __VLS_125 = __VLS_124({}, ...__VLS_functionalComponentArgsRest(__VLS_124));
var __VLS_122;
var __VLS_114;
const __VLS_127 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
    ...{ 'onClick': {} },
}));
const __VLS_129 = __VLS_128({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_128));
let __VLS_131;
let __VLS_132;
let __VLS_133;
const __VLS_134 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导入');
    }
};
__VLS_130.slots.default;
const __VLS_135 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({}));
const __VLS_137 = __VLS_136({}, ...__VLS_functionalComponentArgsRest(__VLS_136));
__VLS_138.slots.default;
const __VLS_139 = {}.Upload;
/** @type {[typeof __VLS_components.Upload, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({}));
const __VLS_141 = __VLS_140({}, ...__VLS_functionalComponentArgsRest(__VLS_140));
var __VLS_138;
var __VLS_130;
const __VLS_143 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
    ...{ 'onClick': {} },
}));
const __VLS_145 = __VLS_144({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_144));
let __VLS_147;
let __VLS_148;
let __VLS_149;
const __VLS_150 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量提交');
    }
};
__VLS_146.slots.default;
var __VLS_146;
const __VLS_151 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({
    ...{ 'onClick': {} },
}));
const __VLS_153 = __VLS_152({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_152));
let __VLS_155;
let __VLS_156;
let __VLS_157;
const __VLS_158 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量删除');
    }
};
__VLS_154.slots.default;
const __VLS_159 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({}));
const __VLS_161 = __VLS_160({}, ...__VLS_functionalComponentArgsRest(__VLS_160));
__VLS_162.slots.default;
const __VLS_163 = {}.Delete;
/** @type {[typeof __VLS_components.Delete, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({}));
const __VLS_165 = __VLS_164({}, ...__VLS_functionalComponentArgsRest(__VLS_164));
var __VLS_162;
var __VLS_154;
const __VLS_167 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({
    ...{ 'onClick': {} },
}));
const __VLS_169 = __VLS_168({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_168));
let __VLS_171;
let __VLS_172;
let __VLS_173;
const __VLS_174 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量撤回');
    }
};
__VLS_170.slots.default;
var __VLS_170;
const __VLS_175 = {}.ElDropdown;
/** @type {[typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
    ...{ 'onCommand': {} },
}));
const __VLS_177 = __VLS_176({
    ...{ 'onCommand': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_176));
let __VLS_179;
let __VLS_180;
let __VLS_181;
const __VLS_182 = {
    onCommand: (__VLS_ctx.handleTableSettingCommand)
};
__VLS_178.slots.default;
const __VLS_183 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({}));
const __VLS_185 = __VLS_184({}, ...__VLS_functionalComponentArgsRest(__VLS_184));
__VLS_186.slots.default;
const __VLS_187 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({}));
const __VLS_189 = __VLS_188({}, ...__VLS_functionalComponentArgsRest(__VLS_188));
__VLS_190.slots.default;
const __VLS_191 = {}.ArrowDown;
/** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({}));
const __VLS_193 = __VLS_192({}, ...__VLS_functionalComponentArgsRest(__VLS_192));
var __VLS_190;
var __VLS_186;
{
    const { dropdown: __VLS_thisSlot } = __VLS_178.slots;
    const __VLS_195 = {}.ElDropdownMenu;
    /** @type {[typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, ]} */ ;
    // @ts-ignore
    const __VLS_196 = __VLS_asFunctionalComponent(__VLS_195, new __VLS_195({}));
    const __VLS_197 = __VLS_196({}, ...__VLS_functionalComponentArgsRest(__VLS_196));
    __VLS_198.slots.default;
    const __VLS_199 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
        command: "column",
    }));
    const __VLS_201 = __VLS_200({
        command: "column",
    }, ...__VLS_functionalComponentArgsRest(__VLS_200));
    __VLS_202.slots.default;
    var __VLS_202;
    const __VLS_203 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
        command: "density",
    }));
    const __VLS_205 = __VLS_204({
        command: "density",
    }, ...__VLS_functionalComponentArgsRest(__VLS_204));
    __VLS_206.slots.default;
    var __VLS_206;
    var __VLS_198;
}
var __VLS_178;
const __VLS_207 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_209 = __VLS_208({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
let __VLS_211;
let __VLS_212;
let __VLS_213;
const __VLS_214 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_210.slots.default;
const __VLS_215 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_217 = __VLS_216({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_216));
const __VLS_219 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_221 = __VLS_220({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_220));
const __VLS_223 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
    prop: "adjustmentCode",
    label: "调整单号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_225 = __VLS_224({
    prop: "adjustmentCode",
    label: "调整单号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_224));
const __VLS_227 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
    prop: "adjustmentReason",
    label: "调整原因",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_229 = __VLS_228({
    prop: "adjustmentReason",
    label: "调整原因",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_228));
const __VLS_231 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
    prop: "documentStatus",
    label: "单据状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_233 = __VLS_232({
    prop: "documentStatus",
    label: "单据状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_232));
const __VLS_235 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
    prop: "adjustmentResult",
    label: "调整结果",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_237 = __VLS_236({
    prop: "adjustmentResult",
    label: "调整结果",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_236));
const __VLS_239 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    prop: "documentDate",
    label: "单据日期",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_241 = __VLS_240({
    prop: "documentDate",
    label: "单据日期",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
const __VLS_243 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_244 = __VLS_asFunctionalComponent(__VLS_243, new __VLS_243({
    prop: "remark",
    label: "备注",
    minWidth: "180",
    showOverflowTooltip: true,
}));
const __VLS_245 = __VLS_244({
    prop: "remark",
    label: "备注",
    minWidth: "180",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_244));
const __VLS_247 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_249 = __VLS_248({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_248));
const __VLS_251 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_252 = __VLS_asFunctionalComponent(__VLS_251, new __VLS_251({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_253 = __VLS_252({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_252));
const __VLS_255 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
    label: "操作",
    width: "120",
    fixed: "right",
}));
const __VLS_257 = __VLS_256({
    label: "操作",
    width: "120",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_256));
__VLS_258.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_258.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_259 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_260 = __VLS_asFunctionalComponent(__VLS_259, new __VLS_259({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_261 = __VLS_260({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_260));
    let __VLS_263;
    let __VLS_264;
    let __VLS_265;
    const __VLS_266 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_262.slots.default;
    var __VLS_262;
    const __VLS_267 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_268 = __VLS_asFunctionalComponent(__VLS_267, new __VLS_267({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_269 = __VLS_268({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_268));
    let __VLS_271;
    let __VLS_272;
    let __VLS_273;
    const __VLS_274 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleEdit(row);
        }
    };
    __VLS_270.slots.default;
    var __VLS_270;
}
var __VLS_258;
var __VLS_210;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedIds.length);
const __VLS_275 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_276 = __VLS_asFunctionalComponent(__VLS_275, new __VLS_275({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}));
const __VLS_277 = __VLS_276({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_276));
let __VLS_279;
let __VLS_280;
let __VLS_281;
const __VLS_282 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_283 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_278;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ArrowDown: ArrowDown,
            Delete: Delete,
            Plus: Plus,
            RefreshRight: RefreshRight,
            Search: Search,
            Upload: Upload,
            CommonQuerySection: CommonQuerySection,
            statusTree: statusTree,
            resultTree: resultTree,
            supplierTree: supplierTree,
            reasonTree: reasonTree,
            itemTree: itemTree,
            creatorTree: creatorTree,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            selectedIds: selectedIds,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleTableSettingCommand: handleTableSettingCommand,
            handleSelectionChange: handleSelectionChange,
            handleView: handleView,
            handleEdit: handleEdit,
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
