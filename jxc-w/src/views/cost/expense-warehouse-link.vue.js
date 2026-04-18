/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
const logisticsModeOptions = ['统配', '直营网配', '供应商直送'];
const supplyOrgTree = [
    {
        value: 'east-region',
        label: '华东大区',
        children: [
            { value: 'hongqiao-store', label: '虹桥门店' },
            { value: 'pudong-store', label: '浦东门店' },
        ],
    },
    {
        value: 'direct-center',
        label: '上海直营中心',
        children: [
            { value: 'xuhui-store', label: '徐汇门店' },
            { value: 'jingan-store', label: '静安门店' },
        ],
    },
];
const query = reactive({
    expenseCode: '',
    expenseRuleName: '',
    supplyOrg: '',
    logisticsMode: '',
});
const tableData = [
    {
        id: 1,
        expenseCode: 'FEE-001',
        expenseRuleName: '虹桥门店统配基础物流费',
        supplyOrg: '虹桥门店',
        logisticsMode: '统配',
        expenseType: '物流费',
    },
    {
        id: 2,
        expenseCode: 'FEE-002',
        expenseRuleName: '浦东门店直营运输费',
        supplyOrg: '浦东门店',
        logisticsMode: '直营网配',
        expenseType: '配送费',
    },
    {
        id: 3,
        expenseCode: 'FEE-003',
        expenseRuleName: '徐汇门店直送附加费',
        supplyOrg: '徐汇门店',
        logisticsMode: '供应商直送',
        expenseType: '运输附加费',
    },
    {
        id: 4,
        expenseCode: 'FEE-004',
        expenseRuleName: '静安门店统配夜配费',
        supplyOrg: '静安门店',
        logisticsMode: '统配',
        expenseType: '配送费',
    },
];
const currentPage = ref(1);
const pageSize = ref(10);
const orgLabelMap = computed(() => {
    const map = new Map();
    const walk = (nodes) => {
        nodes.forEach((node) => {
            map.set(node.value, node.label);
            if (node.children?.length) {
                walk(node.children);
            }
        });
    };
    walk(supplyOrgTree);
    return map;
});
const filteredRows = computed(() => {
    const expenseCodeKeyword = query.expenseCode.trim().toLowerCase();
    const ruleNameKeyword = query.expenseRuleName.trim().toLowerCase();
    const supplyOrgLabel = query.supplyOrg ? orgLabelMap.value.get(query.supplyOrg) ?? query.supplyOrg : '';
    return tableData.filter((row) => {
        const matchedCode = !expenseCodeKeyword || row.expenseCode.toLowerCase().includes(expenseCodeKeyword);
        const matchedName = !ruleNameKeyword || row.expenseRuleName.toLowerCase().includes(ruleNameKeyword);
        const matchedSupplyOrg = !supplyOrgLabel || row.supplyOrg === supplyOrgLabel;
        const matchedLogisticsMode = !query.logisticsMode || row.logisticsMode === query.logisticsMode;
        return matchedCode && matchedName && matchedSupplyOrg && matchedLogisticsMode;
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
    query.expenseCode = '';
    query.expenseRuleName = '';
    query.supplyOrg = '';
    query.logisticsMode = '';
    currentPage.value = 1;
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
    label: "费用编号",
}));
const __VLS_5 = __VLS_4({
    label: "费用编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.expenseCode),
    placeholder: "请输入费用编号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.expenseCode),
    placeholder: "请输入费用编号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
var __VLS_6;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "费用规则名称",
}));
const __VLS_13 = __VLS_12({
    label: "费用规则名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.query.expenseRuleName),
    placeholder: "请输入费用规则名称",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.query.expenseRuleName),
    placeholder: "请输入费用规则名称",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
var __VLS_14;
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "供货机构",
}));
const __VLS_21 = __VLS_20({
    label: "供货机构",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    modelValue: (__VLS_ctx.query.supplyOrg),
    data: (__VLS_ctx.supplyOrgTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    clearable: true,
    placeholder: "请选择供货机构",
    ...{ style: {} },
}));
const __VLS_25 = __VLS_24({
    modelValue: (__VLS_ctx.query.supplyOrg),
    data: (__VLS_ctx.supplyOrgTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    clearable: true,
    placeholder: "请选择供货机构",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
var __VLS_22;
const __VLS_27 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    label: "物流方式",
}));
const __VLS_29 = __VLS_28({
    label: "物流方式",
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    modelValue: (__VLS_ctx.query.logisticsMode),
    clearable: true,
    placeholder: "请选择物流方式",
    ...{ style: {} },
}));
const __VLS_33 = __VLS_32({
    modelValue: (__VLS_ctx.query.logisticsMode),
    clearable: true,
    placeholder: "请选择物流方式",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
__VLS_34.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.logisticsModeOptions))) {
    const __VLS_35 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_37 = __VLS_36({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_36));
}
var __VLS_34;
var __VLS_30;
const __VLS_39 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({}));
const __VLS_41 = __VLS_40({}, ...__VLS_functionalComponentArgsRest(__VLS_40));
__VLS_42.slots.default;
const __VLS_43 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_45 = __VLS_44({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
let __VLS_47;
let __VLS_48;
let __VLS_49;
const __VLS_50 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_46.slots.default;
const __VLS_51 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({}));
const __VLS_53 = __VLS_52({}, ...__VLS_functionalComponentArgsRest(__VLS_52));
__VLS_54.slots.default;
const __VLS_55 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({}));
const __VLS_57 = __VLS_56({}, ...__VLS_functionalComponentArgsRest(__VLS_56));
var __VLS_54;
var __VLS_46;
const __VLS_59 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
    ...{ 'onClick': {} },
}));
const __VLS_61 = __VLS_60({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_60));
let __VLS_63;
let __VLS_64;
let __VLS_65;
const __VLS_66 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_62.slots.default;
const __VLS_67 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({}));
const __VLS_69 = __VLS_68({}, ...__VLS_functionalComponentArgsRest(__VLS_68));
__VLS_70.slots.default;
const __VLS_71 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({}));
const __VLS_73 = __VLS_72({}, ...__VLS_functionalComponentArgsRest(__VLS_72));
var __VLS_70;
var __VLS_62;
var __VLS_42;
var __VLS_2;
const __VLS_75 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_77 = __VLS_76({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_76));
__VLS_78.slots.default;
const __VLS_79 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_81 = __VLS_80({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_80));
const __VLS_83 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
    prop: "expenseCode",
    label: "费用编号",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_85 = __VLS_84({
    prop: "expenseCode",
    label: "费用编号",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_84));
const __VLS_87 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({
    prop: "expenseRuleName",
    label: "费用规则名称",
    minWidth: "220",
    showOverflowTooltip: true,
}));
const __VLS_89 = __VLS_88({
    prop: "expenseRuleName",
    label: "费用规则名称",
    minWidth: "220",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_88));
const __VLS_91 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({
    prop: "supplyOrg",
    label: "供货机构",
    minWidth: "160",
    showOverflowTooltip: true,
}));
const __VLS_93 = __VLS_92({
    prop: "supplyOrg",
    label: "供货机构",
    minWidth: "160",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_92));
const __VLS_95 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({
    prop: "logisticsMode",
    label: "物流方式",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_97 = __VLS_96({
    prop: "logisticsMode",
    label: "物流方式",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_96));
const __VLS_99 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({
    prop: "expenseType",
    label: "费用类型",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_101 = __VLS_100({
    prop: "expenseType",
    label: "费用类型",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_100));
var __VLS_78;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.filteredRows.length);
const __VLS_103 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({
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
const __VLS_105 = __VLS_104({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_104));
let __VLS_107;
let __VLS_108;
let __VLS_109;
const __VLS_110 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_111 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_106;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            logisticsModeOptions: logisticsModeOptions,
            supplyOrgTree: supplyOrgTree,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
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
