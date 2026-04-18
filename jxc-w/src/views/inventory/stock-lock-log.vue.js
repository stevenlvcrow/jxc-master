/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const reasonTree = [
    {
        value: 'reason-root',
        label: '锁库原因',
        children: [
            { value: '品质异常', label: '品质异常' },
            { value: '盘点冻结', label: '盘点冻结' },
            { value: '外部稽核', label: '外部稽核' },
        ],
    },
];
const operationTypeOptions = ['全部', '锁库', '解锁'];
const query = reactive({
    operationDateRange: [],
    warehouse: '',
    documentCode: '',
    upstreamCode: '',
    itemName: '',
    lockReason: '',
    operationType: '全部',
});
const tableData = [
    {
        id: 1,
        documentType: '锁库单',
        documentCode: 'LK-202604-001',
        lockReason: '盘点冻结',
        upstreamCode: 'PD-202604-021',
        upstreamType: '盘点单',
        orgName: '总部',
        warehouse: '中央成品仓',
        itemCode: 'IT-0001',
        itemName: '鸡胸肉',
        spec: '1kg/包',
        stockUnit: '包',
        stockUnitQty: 18,
        baseUnit: '千克',
        baseUnitQty: 18,
        operationDate: '2026-04-13 09:12:00',
        remark: '',
    },
    {
        id: 2,
        documentType: '解锁单',
        documentCode: 'UL-202604-003',
        lockReason: '品质异常',
        upstreamCode: 'QA-202604-005',
        upstreamType: '质检单',
        orgName: '华东分公司',
        warehouse: '北区原料仓',
        itemCode: 'IT-0002',
        itemName: '牛腩',
        spec: '2kg/包',
        stockUnit: '包',
        stockUnitQty: 6,
        baseUnit: '千克',
        baseUnitQty: 12,
        operationDate: '2026-04-12 16:40:00',
        remark: '复检通过',
    },
    {
        id: 3,
        documentType: '锁库单',
        documentCode: 'LK-202604-003',
        lockReason: '外部稽核',
        upstreamCode: 'AD-202604-013',
        upstreamType: '调整单',
        orgName: '华南分公司',
        warehouse: '南区包材仓',
        itemCode: 'IT-0003',
        itemName: '包装盒',
        spec: '50个/箱',
        stockUnit: '箱',
        stockUnitQty: 4,
        baseUnit: '个',
        baseUnitQty: 200,
        operationDate: '2026-04-11 10:05:00',
        remark: '稽核冻结',
    },
];
onMounted(() => {
    void loadWarehouseTree();
});
watch(() => sessionStore.currentOrgId, () => {
    void loadWarehouseTree();
});
const currentPage = ref(1);
const pageSize = ref(10);
const filteredRows = computed(() => {
    const [startDate, endDate] = query.operationDateRange;
    return tableData.filter((row) => {
        const matchedStart = !startDate || row.operationDate.slice(0, 10) >= startDate;
        const matchedEnd = !endDate || row.operationDate.slice(0, 10) <= endDate;
        const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
        const matchedDocument = !query.documentCode || row.documentCode.includes(query.documentCode);
        const matchedUpstream = !query.upstreamCode || row.upstreamCode.includes(query.upstreamCode);
        const matchedItem = !query.itemName || row.itemName === query.itemName;
        const matchedReason = !query.lockReason || row.lockReason === query.lockReason;
        const matchedOperation = query.operationType === '全部'
            || (query.operationType === '锁库' && row.documentType === '锁库单')
            || (query.operationType === '解锁' && row.documentType === '解锁单');
        return matchedStart
            && matchedEnd
            && matchedWarehouse
            && matchedDocument
            && matchedUpstream
            && matchedItem
            && matchedReason
            && matchedOperation;
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
    query.operationDateRange = [];
    query.warehouse = '';
    query.documentCode = '';
    query.upstreamCode = '';
    query.itemName = '';
    query.lockReason = '';
    query.operationType = '全部';
    currentPage.value = 1;
};
const handleExport = () => {
    ElMessage.info('批量导出单据列表功能待接入');
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
    label: "操作日期",
}));
const __VLS_5 = __VLS_4({
    label: "操作日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.operationDateRange),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
    rangeSeparator: "~",
    startPlaceholder: "开始日期",
    endPlaceholder: "结束日期",
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.operationDateRange),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
    rangeSeparator: "~",
    startPlaceholder: "开始日期",
    endPlaceholder: "结束日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
var __VLS_6;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "仓库",
}));
const __VLS_13 = __VLS_12({
    label: "仓库",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
var __VLS_14;
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "单据编号",
}));
const __VLS_21 = __VLS_20({
    label: "单据编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    modelValue: (__VLS_ctx.query.documentCode),
    placeholder: "请输入单据编号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_25 = __VLS_24({
    modelValue: (__VLS_ctx.query.documentCode),
    placeholder: "请输入单据编号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
var __VLS_22;
const __VLS_27 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    label: "上游单据号",
}));
const __VLS_29 = __VLS_28({
    label: "上游单据号",
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    modelValue: (__VLS_ctx.query.upstreamCode),
    placeholder: "请输入上游单据号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_33 = __VLS_32({
    modelValue: (__VLS_ctx.query.upstreamCode),
    placeholder: "请输入上游单据号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
var __VLS_30;
const __VLS_35 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    label: "物品",
}));
const __VLS_37 = __VLS_36({
    label: "物品",
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
__VLS_38.slots.default;
const __VLS_39 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({
    modelValue: (__VLS_ctx.query.itemName),
    clearable: true,
    placeholder: "请选择",
    ...{ style: {} },
}));
const __VLS_41 = __VLS_40({
    modelValue: (__VLS_ctx.query.itemName),
    clearable: true,
    placeholder: "请选择",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
__VLS_42.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
    const __VLS_43 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_45 = __VLS_44({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_44));
}
var __VLS_42;
var __VLS_38;
const __VLS_47 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({
    label: "锁库原因",
}));
const __VLS_49 = __VLS_48({
    label: "锁库原因",
}, ...__VLS_functionalComponentArgsRest(__VLS_48));
__VLS_50.slots.default;
const __VLS_51 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
    modelValue: (__VLS_ctx.query.lockReason),
    data: (__VLS_ctx.reasonTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    placeholder: "请选择锁库原因",
    ...{ style: {} },
}));
const __VLS_53 = __VLS_52({
    modelValue: (__VLS_ctx.query.lockReason),
    data: (__VLS_ctx.reasonTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    placeholder: "请选择锁库原因",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_52));
var __VLS_50;
const __VLS_55 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({
    label: "操作类型",
}));
const __VLS_57 = __VLS_56({
    label: "操作类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_56));
__VLS_58.slots.default;
const __VLS_59 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
    modelValue: (__VLS_ctx.query.operationType),
    ...{ style: {} },
}));
const __VLS_61 = __VLS_60({
    modelValue: (__VLS_ctx.query.operationType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_60));
__VLS_62.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.operationTypeOptions))) {
    const __VLS_63 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_65 = __VLS_64({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_64));
}
var __VLS_62;
var __VLS_58;
const __VLS_67 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({}));
const __VLS_69 = __VLS_68({}, ...__VLS_functionalComponentArgsRest(__VLS_68));
__VLS_70.slots.default;
const __VLS_71 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_73 = __VLS_72({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_72));
let __VLS_75;
let __VLS_76;
let __VLS_77;
const __VLS_78 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_74.slots.default;
const __VLS_79 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({}));
const __VLS_81 = __VLS_80({}, ...__VLS_functionalComponentArgsRest(__VLS_80));
__VLS_82.slots.default;
const __VLS_83 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({}));
const __VLS_85 = __VLS_84({}, ...__VLS_functionalComponentArgsRest(__VLS_84));
var __VLS_82;
var __VLS_74;
const __VLS_87 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({
    ...{ 'onClick': {} },
}));
const __VLS_89 = __VLS_88({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_88));
let __VLS_91;
let __VLS_92;
let __VLS_93;
const __VLS_94 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_90.slots.default;
const __VLS_95 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({}));
const __VLS_97 = __VLS_96({}, ...__VLS_functionalComponentArgsRest(__VLS_96));
__VLS_98.slots.default;
const __VLS_99 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({}));
const __VLS_101 = __VLS_100({}, ...__VLS_functionalComponentArgsRest(__VLS_100));
var __VLS_98;
var __VLS_90;
var __VLS_70;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_103 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({
    ...{ 'onClick': {} },
}));
const __VLS_105 = __VLS_104({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_104));
let __VLS_107;
let __VLS_108;
let __VLS_109;
const __VLS_110 = {
    onClick: (__VLS_ctx.handleExport)
};
__VLS_106.slots.default;
const __VLS_111 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({}));
const __VLS_113 = __VLS_112({}, ...__VLS_functionalComponentArgsRest(__VLS_112));
__VLS_114.slots.default;
const __VLS_115 = {}.Download;
/** @type {[typeof __VLS_components.Download, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({}));
const __VLS_117 = __VLS_116({}, ...__VLS_functionalComponentArgsRest(__VLS_116));
var __VLS_114;
var __VLS_106;
const __VLS_119 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (400),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_121 = __VLS_120({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (400),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_120));
__VLS_122.slots.default;
const __VLS_123 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_125 = __VLS_124({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_124));
const __VLS_127 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
    prop: "documentType",
    label: "单据类型",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_129 = __VLS_128({
    prop: "documentType",
    label: "单据类型",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_128));
const __VLS_131 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_133 = __VLS_132({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_132));
const __VLS_135 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({
    prop: "lockReason",
    label: "锁库原因",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_137 = __VLS_136({
    prop: "lockReason",
    label: "锁库原因",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_136));
const __VLS_139 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
    prop: "upstreamCode",
    label: "上游单据号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_141 = __VLS_140({
    prop: "upstreamCode",
    label: "上游单据号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_140));
const __VLS_143 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
    prop: "upstreamType",
    label: "上游单据类型",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_145 = __VLS_144({
    prop: "upstreamType",
    label: "上游单据类型",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_144));
const __VLS_147 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({
    prop: "orgName",
    label: "机构",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_149 = __VLS_148({
    prop: "orgName",
    label: "机构",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_148));
const __VLS_151 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_153 = __VLS_152({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_152));
const __VLS_155 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
    prop: "itemCode",
    label: "物品编码",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_157 = __VLS_156({
    prop: "itemCode",
    label: "物品编码",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_156));
const __VLS_159 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({
    prop: "itemName",
    label: "物品名称",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_161 = __VLS_160({
    prop: "itemName",
    label: "物品名称",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_160));
const __VLS_163 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({
    prop: "spec",
    label: "规格型号",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_165 = __VLS_164({
    prop: "spec",
    label: "规格型号",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_164));
const __VLS_167 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({
    prop: "stockUnit",
    label: "库存单位",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_169 = __VLS_168({
    prop: "stockUnit",
    label: "库存单位",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_168));
const __VLS_171 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({
    prop: "stockUnitQty",
    label: "库存单位锁库/解锁数量",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_173 = __VLS_172({
    prop: "stockUnitQty",
    label: "库存单位锁库/解锁数量",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_172));
const __VLS_175 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
    prop: "baseUnit",
    label: "基准单位",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_177 = __VLS_176({
    prop: "baseUnit",
    label: "基准单位",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_176));
const __VLS_179 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({
    prop: "baseUnitQty",
    label: "基准单位锁库/解锁数量",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_181 = __VLS_180({
    prop: "baseUnitQty",
    label: "基准单位锁库/解锁数量",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_180));
const __VLS_183 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({
    prop: "operationDate",
    label: "操作日期",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_185 = __VLS_184({
    prop: "operationDate",
    label: "操作日期",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_184));
const __VLS_187 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({
    prop: "remark",
    label: "备注",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_189 = __VLS_188({
    prop: "remark",
    label: "备注",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_188));
var __VLS_122;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
const __VLS_191 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
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
const __VLS_193 = __VLS_192({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_192));
let __VLS_195;
let __VLS_196;
let __VLS_197;
const __VLS_198 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_199 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_194;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            Download: Download,
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            warehouseTree: warehouseTree,
            itemOptions: itemOptions,
            reasonTree: reasonTree,
            operationTypeOptions: operationTypeOptions,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleExport: handleExport,
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
