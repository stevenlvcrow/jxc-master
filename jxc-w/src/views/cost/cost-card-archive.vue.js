/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
const defaultQuery = {
    costCardInfo: '',
    lotteryInfo: '',
    itemInfo: '',
};
const query = reactive({ ...defaultQuery });
const router = useRouter();
const currentPage = ref(1);
const pageSize = ref(10);
const selectedCount = ref(0);
const tableHeight = 420;
const toolbarButtons = [
    '新增成本卡',
    '批量替换物品信息',
    '批量设置替代料',
    '批量设置净料率',
    '批量修改',
    '批量导入',
    '批量导出',
    '自动关联菜品和成本卡',
    '自动关联记录',
];
const tableData = [
    {
        id: 1,
        costCardName: '宫保鸡丁标准卡',
        costCardCode: 'CBK-0001',
        costPriceTax: '18.60',
        itemCostTax: '16.90',
        otherCost: '1.70',
        dishType: '热菜',
        dishSpec: '标准份',
        costCardType: '标准成本卡',
        linkedDishCount: 12,
        operatedAt: '2026-04-08 16:12:00',
        lotteryInfo: 'CP-001 宫保鸡丁 GBCD',
        itemInfo: '鸡腿肉/花生米/干辣椒',
    },
    {
        id: 2,
        costCardName: '鱼香肉丝标准卡',
        costCardCode: 'CBK-0002',
        costPriceTax: '14.20',
        itemCostTax: '12.80',
        otherCost: '1.40',
        dishType: '热菜',
        dishSpec: '标准份',
        costCardType: '标准成本卡',
        linkedDishCount: 9,
        operatedAt: '2026-04-08 15:26:00',
        lotteryInfo: 'CP-002 鱼香肉丝 YSRS',
        itemInfo: '里脊肉/木耳/笋丝',
    },
    {
        id: 3,
        costCardName: '酸辣汤门店卡',
        costCardCode: 'CBK-0003',
        costPriceTax: '6.50',
        itemCostTax: '5.70',
        otherCost: '0.80',
        dishType: '汤羹',
        dishSpec: '中碗',
        costCardType: '门店成本卡',
        linkedDishCount: 6,
        operatedAt: '2026-04-07 20:08:00',
        lotteryInfo: 'CP-003 酸辣汤 SLT',
        itemInfo: '鸡蛋/豆腐/木耳',
    },
    {
        id: 4,
        costCardName: '清炒时蔬季节卡',
        costCardCode: 'CBK-0004',
        costPriceTax: '8.00',
        itemCostTax: '7.10',
        otherCost: '0.90',
        dishType: '素菜',
        dishSpec: '标准份',
        costCardType: '临时成本卡',
        linkedDishCount: 4,
        operatedAt: '2026-04-07 18:30:00',
        lotteryInfo: 'CP-004 清炒时蔬 QCSS',
        itemInfo: '青菜/蒜蓉/食用油',
    },
    {
        id: 5,
        costCardName: '番茄牛腩试制卡',
        costCardCode: 'CBK-0005',
        costPriceTax: '27.40',
        itemCostTax: '24.80',
        otherCost: '2.60',
        dishType: '炖菜',
        dishSpec: '标准份',
        costCardType: '试制成本卡',
        linkedDishCount: 3,
        operatedAt: '2026-04-06 14:02:00',
        lotteryInfo: 'CP-005 番茄牛腩 FQNN',
        itemInfo: '牛腩/番茄/洋葱',
    },
    {
        id: 6,
        costCardName: '红烧排骨标准卡',
        costCardCode: 'CBK-0006',
        costPriceTax: '23.30',
        itemCostTax: '21.20',
        otherCost: '2.10',
        dishType: '热菜',
        dishSpec: '大份',
        costCardType: '标准成本卡',
        linkedDishCount: 7,
        operatedAt: '2026-04-06 09:21:00',
        lotteryInfo: 'CP-006 红烧排骨 HSPG',
        itemInfo: '排骨/冰糖/生抽',
    },
    {
        id: 7,
        costCardName: '麻婆豆腐标准卡',
        costCardCode: 'CBK-0007',
        costPriceTax: '9.60',
        itemCostTax: '8.70',
        otherCost: '0.90',
        dishType: '热菜',
        dishSpec: '标准份',
        costCardType: '标准成本卡',
        linkedDishCount: 10,
        operatedAt: '2026-04-05 17:42:00',
        lotteryInfo: 'CP-007 麻婆豆腐 MPDF',
        itemInfo: '豆腐/肉末/郫县豆瓣',
    },
    {
        id: 8,
        costCardName: '香菇鸡汤门店卡',
        costCardCode: 'CBK-0008',
        costPriceTax: '12.80',
        itemCostTax: '11.40',
        otherCost: '1.40',
        dishType: '汤羹',
        dishSpec: '大碗',
        costCardType: '门店成本卡',
        linkedDishCount: 5,
        operatedAt: '2026-04-05 11:15:00',
        lotteryInfo: 'CP-008 香菇鸡汤 XGJT',
        itemInfo: '鸡架/香菇/姜片',
    },
    {
        id: 9,
        costCardName: '凉拌黄瓜标准卡',
        costCardCode: 'CBK-0009',
        costPriceTax: '5.20',
        itemCostTax: '4.50',
        otherCost: '0.70',
        dishType: '凉菜',
        dishSpec: '标准份',
        costCardType: '标准成本卡',
        linkedDishCount: 8,
        operatedAt: '2026-04-04 16:05:00',
        lotteryInfo: 'CP-009 凉拌黄瓜 LBHG',
        itemInfo: '黄瓜/蒜末/香醋',
    },
    {
        id: 10,
        costCardName: '蒜香排条临时卡',
        costCardCode: 'CBK-0010',
        costPriceTax: '19.70',
        itemCostTax: '17.90',
        otherCost: '1.80',
        dishType: '热菜',
        dishSpec: '小份',
        costCardType: '临时成本卡',
        linkedDishCount: 2,
        operatedAt: '2026-04-04 10:34:00',
        lotteryInfo: 'CP-010 蒜香排条 SXPT',
        itemInfo: '猪排条/蒜粉/椒盐',
    },
    {
        id: 11,
        costCardName: '糖醋里脊标准卡',
        costCardCode: 'CBK-0011',
        costPriceTax: '15.90',
        itemCostTax: '14.30',
        otherCost: '1.60',
        dishType: '热菜',
        dishSpec: '标准份',
        costCardType: '标准成本卡',
        linkedDishCount: 11,
        operatedAt: '2026-04-03 19:28:00',
        lotteryInfo: 'CP-011 糖醋里脊 TCLJ',
        itemInfo: '里脊肉/番茄酱/白醋',
    },
    {
        id: 12,
        costCardName: '玉米排骨汤标准卡',
        costCardCode: 'CBK-0012',
        costPriceTax: '13.40',
        itemCostTax: '11.90',
        otherCost: '1.50',
        dishType: '汤羹',
        dishSpec: '中碗',
        costCardType: '标准成本卡',
        linkedDishCount: 6,
        operatedAt: '2026-04-03 14:56:00',
        lotteryInfo: 'CP-012 玉米排骨汤 YMPGT',
        itemInfo: '排骨/玉米/胡萝卜',
    },
];
const filteredData = computed(() => {
    const keyword = query.costCardInfo.trim().toLowerCase();
    const lotteryKeyword = query.lotteryInfo.trim().toLowerCase();
    const itemKeyword = query.itemInfo.trim().toLowerCase();
    return tableData.filter((row) => {
        const matchedCostCard = !keyword
            || row.costCardCode.toLowerCase().includes(keyword)
            || row.costCardName.toLowerCase().includes(keyword);
        const matchedLottery = !lotteryKeyword || row.lotteryInfo.toLowerCase().includes(lotteryKeyword);
        const matchedItem = !itemKeyword || row.itemInfo.toLowerCase().includes(itemKeyword);
        return matchedCostCard && matchedLottery && matchedItem;
    });
});
const pagedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredData.value.slice(start, start + pageSize.value);
});
const handleSearch = () => {
    currentPage.value = 1;
};
const handleReset = () => {
    Object.assign(query, defaultQuery);
    currentPage.value = 1;
};
const handleToolbarAction = (action) => {
    if (action === '新增成本卡') {
        router.push('/archive/2/1/create');
        return;
    }
    ElMessage.info(`已触发：${action}`);
};
const handleSelectionChange = (rows) => {
    selectedCount.value = rows.length;
};
const handleCurrentChange = (page) => {
    currentPage.value = page;
};
const handleSizeChange = (size) => {
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
const __VLS_0 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "filter-bar compact-filter-bar" },
}));
const __VLS_2 = __VLS_1({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "filter-bar compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    label: "成本卡信息",
}));
const __VLS_6 = __VLS_5({
    label: "成本卡信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    modelValue: (__VLS_ctx.query.costCardInfo),
    placeholder: "成本卡名称/编码",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_10 = __VLS_9({
    modelValue: (__VLS_ctx.query.costCardInfo),
    placeholder: "成本卡名称/编码",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
var __VLS_7;
const __VLS_12 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    label: "彩票信息",
}));
const __VLS_14 = __VLS_13({
    label: "彩票信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
const __VLS_16 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    modelValue: (__VLS_ctx.query.lotteryInfo),
    placeholder: "根据编码/名称/助记码",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_18 = __VLS_17({
    modelValue: (__VLS_ctx.query.lotteryInfo),
    placeholder: "根据编码/名称/助记码",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
var __VLS_15;
const __VLS_20 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    label: "物品信息",
}));
const __VLS_22 = __VLS_21({
    label: "物品信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_23.slots.default;
const __VLS_24 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    modelValue: (__VLS_ctx.query.itemInfo),
    placeholder: "请输入物品信息",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_26 = __VLS_25({
    modelValue: (__VLS_ctx.query.itemInfo),
    placeholder: "请输入物品信息",
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
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
}));
const __VLS_58 = __VLS_57({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    height: (__VLS_ctx.tableHeight),
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
let __VLS_60;
let __VLS_61;
let __VLS_62;
const __VLS_63 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_59.slots.default;
const __VLS_64 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_66 = __VLS_65({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_65));
const __VLS_68 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_70 = __VLS_69({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_69));
const __VLS_72 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
    prop: "costCardCode",
    label: "成本卡编码",
    width: "110",
    showOverflowTooltip: true,
}));
const __VLS_74 = __VLS_73({
    prop: "costCardCode",
    label: "成本卡编码",
    width: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_73));
const __VLS_76 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
    prop: "costPriceTax",
    label: "成本价（含税）",
    width: "102",
}));
const __VLS_78 = __VLS_77({
    prop: "costPriceTax",
    label: "成本价（含税）",
    width: "102",
}, ...__VLS_functionalComponentArgsRest(__VLS_77));
const __VLS_80 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
    prop: "itemCostTax",
    label: "物品成本（含税）",
    width: "106",
}));
const __VLS_82 = __VLS_81({
    prop: "itemCostTax",
    label: "物品成本（含税）",
    width: "106",
}, ...__VLS_functionalComponentArgsRest(__VLS_81));
const __VLS_84 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
    prop: "otherCost",
    label: "其他成本",
    width: "86",
}));
const __VLS_86 = __VLS_85({
    prop: "otherCost",
    label: "其他成本",
    width: "86",
}, ...__VLS_functionalComponentArgsRest(__VLS_85));
const __VLS_88 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
    prop: "dishType",
    label: "适用菜品类型",
    width: "96",
    showOverflowTooltip: true,
}));
const __VLS_90 = __VLS_89({
    prop: "dishType",
    label: "适用菜品类型",
    width: "96",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_89));
const __VLS_92 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
    prop: "dishSpec",
    label: "适用菜品规格",
    width: "96",
    showOverflowTooltip: true,
}));
const __VLS_94 = __VLS_93({
    prop: "dishSpec",
    label: "适用菜品规格",
    width: "96",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_93));
const __VLS_96 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
    prop: "costCardType",
    label: "成本卡类型",
    width: "96",
    showOverflowTooltip: true,
}));
const __VLS_98 = __VLS_97({
    prop: "costCardType",
    label: "成本卡类型",
    width: "96",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_97));
const __VLS_100 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
    prop: "linkedDishCount",
    label: "关联菜品数",
    width: "86",
}));
const __VLS_102 = __VLS_101({
    prop: "linkedDishCount",
    label: "关联菜品数",
    width: "86",
}, ...__VLS_functionalComponentArgsRest(__VLS_101));
const __VLS_104 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
    prop: "operatedAt",
    label: "操作时间",
    width: "142",
    showOverflowTooltip: true,
}));
const __VLS_106 = __VLS_105({
    prop: "operatedAt",
    label: "操作时间",
    width: "142",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_105));
const __VLS_108 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
    label: "操作",
    width: "88",
    fixed: "right",
}));
const __VLS_110 = __VLS_109({
    label: "操作",
    width: "88",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_109));
__VLS_111.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_111.slots;
    const __VLS_112 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
        text: true,
        type: "primary",
    }));
    const __VLS_114 = __VLS_113({
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_113));
    __VLS_115.slots.default;
    var __VLS_115;
    const __VLS_116 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
        text: true,
    }));
    const __VLS_118 = __VLS_117({
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_117));
    __VLS_119.slots.default;
    var __VLS_119;
}
var __VLS_111;
var __VLS_59;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedCount);
const __VLS_120 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredData.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}));
const __VLS_122 = __VLS_121({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredData.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_121));
let __VLS_124;
let __VLS_125;
let __VLS_126;
const __VLS_127 = {
    onCurrentChange: (__VLS_ctx.handleCurrentChange)
};
const __VLS_128 = {
    onSizeChange: (__VLS_ctx.handleSizeChange)
};
var __VLS_123;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            selectedCount: selectedCount,
            tableHeight: tableHeight,
            toolbarButtons: toolbarButtons,
            filteredData: filteredData,
            pagedData: pagedData,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleSelectionChange: handleSelectionChange,
            handleCurrentChange: handleCurrentChange,
            handleSizeChange: handleSizeChange,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
