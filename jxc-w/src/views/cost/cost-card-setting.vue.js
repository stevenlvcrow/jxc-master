/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
const defaultQuery = {
    dishInfo: '',
    configuredCostCard: '全部',
    costCardType: '全部',
    itemType: '全部',
    hasSubstitute: '全部',
    dishDeleted: '全部',
    showTemporaryDish: '否',
};
const query = reactive({ ...defaultQuery });
const currentPage = ref(1);
const pageSize = ref(10);
const selectedCount = ref(0);
const selectedTreeNode = ref('all');
const tableHeight = ref(260);
const mainPanelRef = ref(null);
const queryRef = ref(null);
const toolbarRef = ref(null);
const paginationRef = ref(null);
const itemTypeOptions = ['全部', '成品', '半成品', '套餐', '饮品'];
const toolbarButtons = [
    '批量替换物品信息',
    '批量设置替代料',
    '批量设置净料率',
    '批量导入',
    '批量导出',
    '自动关联菜品和成本卡',
    '自动关联记录',
];
const dishTreeData = [
    {
        id: 'all',
        label: '全部菜品',
        children: [
            {
                id: 'hot-dish',
                label: '热菜',
                children: [
                    { id: 'sichuan', label: '川湘菜' },
                    { id: 'stir-fry', label: '小炒' },
                ],
            },
            {
                id: 'cold-dish',
                label: '凉菜',
            },
            {
                id: 'soup',
                label: '汤羹',
            },
            {
                id: 'drink',
                label: '饮品',
            },
            {
                id: 'temp',
                label: '临时菜',
            },
        ],
    },
];
const treeCategoryMap = {
    all: [],
    'hot-dish': ['川湘菜', '小炒', '炖菜'],
    sichuan: ['川湘菜'],
    'stir-fry': ['小炒'],
    'cold-dish': ['凉菜', '小吃'],
    soup: ['汤羹'],
    drink: ['饮品'],
    temp: [],
};
const tableData = [
    {
        id: 1,
        dishSpuCode: 'SPU-0001',
        dishSkuCode: 'SKU-0001-01',
        dishName: '宫保鸡丁',
        spec: '标准份',
        category: '川湘菜',
        sellPrice: 38,
        costPriceTax: 16.8,
        grossProfitTax: 55.79,
        targetGrossProfit: 58,
        configuredCostCard: '是',
        costCardType: '集团管控',
        itemType: '成品',
        hasSubstitute: '是',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-08 16:12:00',
    },
    {
        id: 2,
        dishSpuCode: 'SPU-0002',
        dishSkuCode: 'SKU-0002-01',
        dishName: '鱼香肉丝',
        spec: '标准份',
        category: '川湘菜',
        sellPrice: 32,
        costPriceTax: 14.2,
        grossProfitTax: 55.63,
        targetGrossProfit: 56,
        configuredCostCard: '是',
        costCardType: '集团创建',
        itemType: '成品',
        hasSubstitute: '否',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-08 15:36:00',
    },
    {
        id: 3,
        dishSpuCode: 'SPU-0003',
        dishSkuCode: 'SKU-0003-01',
        dishName: '红油抄手',
        spec: '大份',
        category: '小吃',
        sellPrice: 22,
        costPriceTax: 10.4,
        grossProfitTax: 52.73,
        targetGrossProfit: 55,
        configuredCostCard: '是',
        costCardType: '门店自建',
        itemType: '半成品',
        hasSubstitute: '是',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-07 20:08:00',
    },
    {
        id: 4,
        dishSpuCode: 'SPU-0004',
        dishSkuCode: 'SKU-0004-01',
        dishName: '凉拌黄瓜',
        spec: '标准份',
        category: '凉菜',
        sellPrice: 12,
        costPriceTax: 4.8,
        grossProfitTax: 60,
        targetGrossProfit: 62,
        configuredCostCard: '否',
        costCardType: '门店自建',
        itemType: '成品',
        hasSubstitute: '否',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-07 18:30:00',
    },
    {
        id: 5,
        dishSpuCode: 'SPU-0005',
        dishSkuCode: 'SKU-0005-02',
        dishName: '番茄牛腩',
        spec: '标准份',
        category: '炖菜',
        sellPrice: 48,
        costPriceTax: 24.8,
        grossProfitTax: 48.33,
        targetGrossProfit: 52,
        configuredCostCard: '是',
        costCardType: '集团管控',
        itemType: '成品',
        hasSubstitute: '是',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-06 14:02:00',
    },
    {
        id: 6,
        dishSpuCode: 'SPU-0006',
        dishSkuCode: 'SKU-0006-01',
        dishName: '蒜香排条',
        spec: '小份',
        category: '小炒',
        sellPrice: 28,
        costPriceTax: 17.9,
        grossProfitTax: 36.07,
        targetGrossProfit: 42,
        configuredCostCard: '否',
        costCardType: '门店自建',
        itemType: '成品',
        hasSubstitute: '否',
        dishDeleted: '否',
        isTemporaryDish: '是',
        operatedAt: '2026-04-04 10:34:00',
    },
    {
        id: 7,
        dishSpuCode: 'SPU-0007',
        dishSkuCode: 'SKU-0007-01',
        dishName: '香菇鸡汤',
        spec: '大碗',
        category: '汤羹',
        sellPrice: 26,
        costPriceTax: 11.4,
        grossProfitTax: 56.15,
        targetGrossProfit: 58,
        configuredCostCard: '是',
        costCardType: '集团创建',
        itemType: '成品',
        hasSubstitute: '否',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-05 11:15:00',
    },
    {
        id: 8,
        dishSpuCode: 'SPU-0008',
        dishSkuCode: 'SKU-0008-01',
        dishName: '柠檬茶',
        spec: '500ml',
        category: '饮品',
        sellPrice: 16,
        costPriceTax: 4.6,
        grossProfitTax: 71.25,
        targetGrossProfit: 68,
        configuredCostCard: '是',
        costCardType: '集团创建',
        itemType: '饮品',
        hasSubstitute: '是',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-03 13:18:00',
    },
    {
        id: 9,
        dishSpuCode: 'SPU-0009',
        dishSkuCode: 'SKU-0009-01',
        dishName: '麻婆豆腐',
        spec: '标准份',
        category: '川湘菜',
        sellPrice: 22,
        costPriceTax: 8.7,
        grossProfitTax: 60.45,
        targetGrossProfit: 61,
        configuredCostCard: '是',
        costCardType: '集团管控',
        itemType: '成品',
        hasSubstitute: '否',
        dishDeleted: '是',
        isTemporaryDish: '否',
        operatedAt: '2026-04-03 09:22:00',
    },
    {
        id: 10,
        dishSpuCode: 'SPU-0010',
        dishSkuCode: 'SKU-0010-01',
        dishName: '糖醋里脊',
        spec: '标准份',
        category: '小炒',
        sellPrice: 34,
        costPriceTax: 14.3,
        grossProfitTax: 57.94,
        targetGrossProfit: 58,
        configuredCostCard: '是',
        costCardType: '集团管控',
        itemType: '成品',
        hasSubstitute: '是',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-02 19:28:00',
    },
    {
        id: 11,
        dishSpuCode: 'SPU-0011',
        dishSkuCode: 'SKU-0011-01',
        dishName: '青瓜沙拉',
        spec: '标准份',
        category: '凉菜',
        sellPrice: 18,
        costPriceTax: 6.2,
        grossProfitTax: 65.56,
        targetGrossProfit: 63,
        configuredCostCard: '否',
        costCardType: '门店自建',
        itemType: '套餐',
        hasSubstitute: '否',
        dishDeleted: '否',
        isTemporaryDish: '是',
        operatedAt: '2026-04-02 11:56:00',
    },
    {
        id: 12,
        dishSpuCode: 'SPU-0012',
        dishSkuCode: 'SKU-0012-01',
        dishName: '玉米排骨汤',
        spec: '中碗',
        category: '汤羹',
        sellPrice: 24,
        costPriceTax: 11.9,
        grossProfitTax: 50.42,
        targetGrossProfit: 53,
        configuredCostCard: '是',
        costCardType: '集团创建',
        itemType: '成品',
        hasSubstitute: '否',
        dishDeleted: '否',
        isTemporaryDish: '否',
        operatedAt: '2026-04-01 14:56:00',
    },
];
const filteredData = computed(() => {
    const keyword = query.dishInfo.trim().toLowerCase();
    return tableData.filter((row) => {
        const matchedKeyword = !keyword
            || row.dishSpuCode.toLowerCase().includes(keyword)
            || row.dishSkuCode.toLowerCase().includes(keyword)
            || row.dishName.toLowerCase().includes(keyword);
        const matchedConfigured = query.configuredCostCard === '全部' || row.configuredCostCard === query.configuredCostCard;
        const matchedCardType = query.costCardType === '全部' || row.costCardType === query.costCardType;
        const matchedItemType = query.itemType === '全部' || row.itemType === query.itemType;
        const matchedSubstitute = query.hasSubstitute === '全部' || row.hasSubstitute === query.hasSubstitute;
        const matchedDeleted = query.dishDeleted === '全部' || row.dishDeleted === query.dishDeleted;
        const matchedTempDish = row.isTemporaryDish === query.showTemporaryDish;
        const matchedTree = (() => {
            if (selectedTreeNode.value === 'all') {
                return true;
            }
            if (selectedTreeNode.value === 'temp') {
                return row.isTemporaryDish === '是';
            }
            const categories = treeCategoryMap[selectedTreeNode.value] ?? [];
            return categories.includes(row.category);
        })();
        return matchedKeyword
            && matchedConfigured
            && matchedCardType
            && matchedItemType
            && matchedSubstitute
            && matchedDeleted
            && matchedTempDish
            && matchedTree;
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
    selectedTreeNode.value = 'all';
    currentPage.value = 1;
};
const handleToolbarAction = (action) => {
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
const handleTreeNodeClick = (data) => {
    selectedTreeNode.value = data.id;
    currentPage.value = 1;
};
const formatPercent = (value) => `${value.toFixed(2)}%`;
const formatPrice = (value) => value.toFixed(2);
const updateTableHeight = () => {
    const mainPanel = mainPanelRef.value;
    if (!mainPanel) {
        return;
    }
    const panelTop = mainPanel.getBoundingClientRect().top;
    const viewportBottomGap = 24;
    const availableHeight = window.innerHeight - panelTop - viewportBottomGap;
    const queryHeight = queryRef.value?.offsetHeight ?? 0;
    const toolbarHeight = toolbarRef.value?.offsetHeight ?? 0;
    const paginationHeight = paginationRef.value?.offsetHeight ?? 0;
    const panelInnerPadding = 16;
    const sectionGaps = 12;
    const nextHeight = availableHeight - queryHeight - toolbarHeight - paginationHeight - panelInnerPadding - sectionGaps;
    tableHeight.value = Math.max(120, Math.floor(nextHeight));
};
onMounted(() => {
    nextTick(() => {
        updateTableHeight();
        window.addEventListener('resize', updateTableHeight);
    });
});
onBeforeUnmount(() => {
    window.removeEventListener('resize', updateTableHeight);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-management-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel category-panel" },
});
const __VLS_0 = {}.ElTree;
/** @type {[typeof __VLS_components.ElTree, typeof __VLS_components.elTree, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onNodeClick': {} },
    data: (__VLS_ctx.dishTreeData),
    nodeKey: "id",
    defaultExpandAll: true,
    ...{ class: "category-tree" },
}));
const __VLS_2 = __VLS_1({
    ...{ 'onNodeClick': {} },
    data: (__VLS_ctx.dishTreeData),
    nodeKey: "id",
    defaultExpandAll: true,
    ...{ class: "category-tree" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onNodeClick: (__VLS_ctx.handleTreeNodeClick)
};
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ref: "mainPanelRef",
    ...{ class: "panel item-main-panel" },
});
/** @type {typeof __VLS_ctx.mainPanelRef} */ ;
const __VLS_8 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    ref: "queryRef",
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "filter-bar compact-filter-bar" },
}));
const __VLS_10 = __VLS_9({
    ref: "queryRef",
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "filter-bar compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
/** @type {typeof __VLS_ctx.queryRef} */ ;
var __VLS_12 = {};
__VLS_11.slots.default;
const __VLS_14 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
    label: "菜品信息",
}));
const __VLS_16 = __VLS_15({
    label: "菜品信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_15));
__VLS_17.slots.default;
const __VLS_18 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
    modelValue: (__VLS_ctx.query.dishInfo),
    placeholder: "编码/名称",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_20 = __VLS_19({
    modelValue: (__VLS_ctx.query.dishInfo),
    placeholder: "编码/名称",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
var __VLS_17;
const __VLS_22 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
    label: "配置成本卡",
}));
const __VLS_24 = __VLS_23({
    label: "配置成本卡",
}, ...__VLS_functionalComponentArgsRest(__VLS_23));
__VLS_25.slots.default;
const __VLS_26 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
    modelValue: (__VLS_ctx.query.configuredCostCard),
    ...{ style: {} },
}));
const __VLS_28 = __VLS_27({
    modelValue: (__VLS_ctx.query.configuredCostCard),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_27));
__VLS_29.slots.default;
const __VLS_30 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
    label: "全部",
    value: "全部",
}));
const __VLS_32 = __VLS_31({
    label: "全部",
    value: "全部",
}, ...__VLS_functionalComponentArgsRest(__VLS_31));
const __VLS_34 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
    label: "是",
    value: "是",
}));
const __VLS_36 = __VLS_35({
    label: "是",
    value: "是",
}, ...__VLS_functionalComponentArgsRest(__VLS_35));
const __VLS_38 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
    label: "否",
    value: "否",
}));
const __VLS_40 = __VLS_39({
    label: "否",
    value: "否",
}, ...__VLS_functionalComponentArgsRest(__VLS_39));
var __VLS_29;
var __VLS_25;
const __VLS_42 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
    label: "成本卡类型",
}));
const __VLS_44 = __VLS_43({
    label: "成本卡类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_43));
__VLS_45.slots.default;
const __VLS_46 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    modelValue: (__VLS_ctx.query.costCardType),
    ...{ style: {} },
}));
const __VLS_48 = __VLS_47({
    modelValue: (__VLS_ctx.query.costCardType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
__VLS_49.slots.default;
const __VLS_50 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
    label: "全部",
    value: "全部",
}));
const __VLS_52 = __VLS_51({
    label: "全部",
    value: "全部",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
const __VLS_54 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    label: "门店自建",
    value: "门店自建",
}));
const __VLS_56 = __VLS_55({
    label: "门店自建",
    value: "门店自建",
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
const __VLS_58 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    label: "集团创建",
    value: "集团创建",
}));
const __VLS_60 = __VLS_59({
    label: "集团创建",
    value: "集团创建",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
const __VLS_62 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    label: "集团管控",
    value: "集团管控",
}));
const __VLS_64 = __VLS_63({
    label: "集团管控",
    value: "集团管控",
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
var __VLS_49;
var __VLS_45;
const __VLS_66 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "物品类型",
}));
const __VLS_68 = __VLS_67({
    label: "物品类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
const __VLS_70 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    modelValue: (__VLS_ctx.query.itemType),
    ...{ style: {} },
}));
const __VLS_72 = __VLS_71({
    modelValue: (__VLS_ctx.query.itemType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
__VLS_73.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemTypeOptions))) {
    const __VLS_74 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_76 = __VLS_75({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_75));
}
var __VLS_73;
var __VLS_69;
const __VLS_78 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    label: "设置替代料",
}));
const __VLS_80 = __VLS_79({
    label: "设置替代料",
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
__VLS_81.slots.default;
const __VLS_82 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
    modelValue: (__VLS_ctx.query.hasSubstitute),
    ...{ style: {} },
}));
const __VLS_84 = __VLS_83({
    modelValue: (__VLS_ctx.query.hasSubstitute),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_83));
__VLS_85.slots.default;
const __VLS_86 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
    label: "全部",
    value: "全部",
}));
const __VLS_88 = __VLS_87({
    label: "全部",
    value: "全部",
}, ...__VLS_functionalComponentArgsRest(__VLS_87));
const __VLS_90 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
    label: "是",
    value: "是",
}));
const __VLS_92 = __VLS_91({
    label: "是",
    value: "是",
}, ...__VLS_functionalComponentArgsRest(__VLS_91));
const __VLS_94 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
    label: "否",
    value: "否",
}));
const __VLS_96 = __VLS_95({
    label: "否",
    value: "否",
}, ...__VLS_functionalComponentArgsRest(__VLS_95));
var __VLS_85;
var __VLS_81;
const __VLS_98 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    label: "菜品是否删除",
}));
const __VLS_100 = __VLS_99({
    label: "菜品是否删除",
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
__VLS_101.slots.default;
const __VLS_102 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
    modelValue: (__VLS_ctx.query.dishDeleted),
    ...{ style: {} },
}));
const __VLS_104 = __VLS_103({
    modelValue: (__VLS_ctx.query.dishDeleted),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_103));
__VLS_105.slots.default;
const __VLS_106 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    label: "全部",
    value: "全部",
}));
const __VLS_108 = __VLS_107({
    label: "全部",
    value: "全部",
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
const __VLS_110 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
    label: "是",
    value: "是",
}));
const __VLS_112 = __VLS_111({
    label: "是",
    value: "是",
}, ...__VLS_functionalComponentArgsRest(__VLS_111));
const __VLS_114 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
    label: "否",
    value: "否",
}));
const __VLS_116 = __VLS_115({
    label: "否",
    value: "否",
}, ...__VLS_functionalComponentArgsRest(__VLS_115));
var __VLS_105;
var __VLS_101;
const __VLS_118 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    label: "是否展示临时菜",
}));
const __VLS_120 = __VLS_119({
    label: "是否展示临时菜",
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
__VLS_121.slots.default;
const __VLS_122 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
    modelValue: (__VLS_ctx.query.showTemporaryDish),
}));
const __VLS_124 = __VLS_123({
    modelValue: (__VLS_ctx.query.showTemporaryDish),
}, ...__VLS_functionalComponentArgsRest(__VLS_123));
__VLS_125.slots.default;
const __VLS_126 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    value: "是",
}));
const __VLS_128 = __VLS_127({
    value: "是",
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
__VLS_129.slots.default;
var __VLS_129;
const __VLS_130 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
    value: "否",
}));
const __VLS_132 = __VLS_131({
    value: "否",
}, ...__VLS_functionalComponentArgsRest(__VLS_131));
__VLS_133.slots.default;
var __VLS_133;
var __VLS_125;
var __VLS_121;
const __VLS_134 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({}));
const __VLS_136 = __VLS_135({}, ...__VLS_functionalComponentArgsRest(__VLS_135));
__VLS_137.slots.default;
const __VLS_138 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_140 = __VLS_139({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
let __VLS_142;
let __VLS_143;
let __VLS_144;
const __VLS_145 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_141.slots.default;
var __VLS_141;
const __VLS_146 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    ...{ 'onClick': {} },
}));
const __VLS_148 = __VLS_147({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
let __VLS_150;
let __VLS_151;
let __VLS_152;
const __VLS_153 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_149.slots.default;
var __VLS_149;
var __VLS_137;
var __VLS_11;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "toolbarRef",
    ...{ class: "table-toolbar" },
});
/** @type {typeof __VLS_ctx.toolbarRef} */ ;
for (const [button, index] of __VLS_getVForSourceType((__VLS_ctx.toolbarButtons))) {
    const __VLS_154 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
    }));
    const __VLS_156 = __VLS_155({
        ...{ 'onClick': {} },
        key: (button),
        type: (index === 0 ? 'primary' : 'default'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_155));
    let __VLS_158;
    let __VLS_159;
    let __VLS_160;
    const __VLS_161 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleToolbarAction(button);
        }
    };
    __VLS_157.slots.default;
    (button);
    var __VLS_157;
}
const __VLS_162 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    maxHeight: (__VLS_ctx.tableHeight),
}));
const __VLS_164 = __VLS_163({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedData),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    maxHeight: (__VLS_ctx.tableHeight),
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
let __VLS_166;
let __VLS_167;
let __VLS_168;
const __VLS_169 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_165.slots.default;
const __VLS_170 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_172 = __VLS_171({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_171));
const __VLS_174 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_176 = __VLS_175({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_175));
const __VLS_178 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({
    prop: "dishSpuCode",
    label: "菜品SPU编码",
    width: "120",
    showOverflowTooltip: true,
}));
const __VLS_180 = __VLS_179({
    prop: "dishSpuCode",
    label: "菜品SPU编码",
    width: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_179));
const __VLS_182 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
    prop: "dishSkuCode",
    label: "菜品SKU编码",
    width: "120",
    showOverflowTooltip: true,
}));
const __VLS_184 = __VLS_183({
    prop: "dishSkuCode",
    label: "菜品SKU编码",
    width: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_183));
const __VLS_186 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
    prop: "dishName",
    label: "菜品名称",
    width: "120",
    showOverflowTooltip: true,
}));
const __VLS_188 = __VLS_187({
    prop: "dishName",
    label: "菜品名称",
    width: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_187));
const __VLS_190 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
    prop: "spec",
    label: "规格",
    width: "92",
    showOverflowTooltip: true,
}));
const __VLS_192 = __VLS_191({
    prop: "spec",
    label: "规格",
    width: "92",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_191));
const __VLS_194 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_195 = __VLS_asFunctionalComponent(__VLS_194, new __VLS_194({
    prop: "category",
    label: "菜品分类",
    width: "96",
    showOverflowTooltip: true,
}));
const __VLS_196 = __VLS_195({
    prop: "category",
    label: "菜品分类",
    width: "96",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_195));
const __VLS_198 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
    label: "售卖价",
    width: "86",
    align: "right",
}));
const __VLS_200 = __VLS_199({
    label: "售卖价",
    width: "86",
    align: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_199));
__VLS_201.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_201.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatPrice(row.sellPrice));
}
var __VLS_201;
const __VLS_202 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
    label: "成本价（含税）",
    width: "96",
    align: "right",
}));
const __VLS_204 = __VLS_203({
    label: "成本价（含税）",
    width: "96",
    align: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_203));
__VLS_205.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_205.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatPrice(row.costPriceTax));
}
var __VLS_205;
const __VLS_206 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_207 = __VLS_asFunctionalComponent(__VLS_206, new __VLS_206({
    label: "成本卡毛利率（含税）",
    width: "130",
    align: "right",
}));
const __VLS_208 = __VLS_207({
    label: "成本卡毛利率（含税）",
    width: "130",
    align: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_207));
__VLS_209.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_209.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatPercent(row.grossProfitTax));
}
var __VLS_209;
const __VLS_210 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
    label: "目标毛利率",
    width: "92",
    align: "right",
}));
const __VLS_212 = __VLS_211({
    label: "目标毛利率",
    width: "92",
    align: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_211));
__VLS_213.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_213.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatPercent(row.targetGrossProfit));
}
var __VLS_213;
const __VLS_214 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({
    prop: "configuredCostCard",
    label: "配置成本卡",
    width: "88",
}));
const __VLS_216 = __VLS_215({
    prop: "configuredCostCard",
    label: "配置成本卡",
    width: "88",
}, ...__VLS_functionalComponentArgsRest(__VLS_215));
const __VLS_218 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
    prop: "dishDeleted",
    label: "菜品是否删除",
    width: "96",
}));
const __VLS_220 = __VLS_219({
    prop: "dishDeleted",
    label: "菜品是否删除",
    width: "96",
}, ...__VLS_functionalComponentArgsRest(__VLS_219));
const __VLS_222 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_223 = __VLS_asFunctionalComponent(__VLS_222, new __VLS_222({
    prop: "operatedAt",
    label: "操作时间",
    width: "142",
    showOverflowTooltip: true,
}));
const __VLS_224 = __VLS_223({
    prop: "operatedAt",
    label: "操作时间",
    width: "142",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_223));
const __VLS_226 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({
    label: "操作",
    width: "110",
    fixed: "right",
}));
const __VLS_228 = __VLS_227({
    label: "操作",
    width: "110",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_227));
__VLS_229.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_229.slots;
    const __VLS_230 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_231 = __VLS_asFunctionalComponent(__VLS_230, new __VLS_230({
        text: true,
        type: "primary",
    }));
    const __VLS_232 = __VLS_231({
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_231));
    __VLS_233.slots.default;
    var __VLS_233;
    const __VLS_234 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_235 = __VLS_asFunctionalComponent(__VLS_234, new __VLS_234({
        text: true,
    }));
    const __VLS_236 = __VLS_235({
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_235));
    __VLS_237.slots.default;
    var __VLS_237;
}
var __VLS_229;
var __VLS_165;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "paginationRef",
    ...{ class: "table-pagination" },
});
/** @type {typeof __VLS_ctx.paginationRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedCount);
const __VLS_238 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_239 = __VLS_asFunctionalComponent(__VLS_238, new __VLS_238({
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
const __VLS_240 = __VLS_239({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredData.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_239));
let __VLS_242;
let __VLS_243;
let __VLS_244;
const __VLS_245 = {
    onCurrentChange: (__VLS_ctx.handleCurrentChange)
};
const __VLS_246 = {
    onSizeChange: (__VLS_ctx.handleSizeChange)
};
var __VLS_241;
/** @type {__VLS_StyleScopedClasses['item-management-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['category-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['category-tree']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
// @ts-ignore
var __VLS_13 = __VLS_12;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            selectedCount: selectedCount,
            tableHeight: tableHeight,
            mainPanelRef: mainPanelRef,
            queryRef: queryRef,
            toolbarRef: toolbarRef,
            paginationRef: paginationRef,
            itemTypeOptions: itemTypeOptions,
            toolbarButtons: toolbarButtons,
            dishTreeData: dishTreeData,
            filteredData: filteredData,
            pagedData: pagedData,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleSelectionChange: handleSelectionChange,
            handleCurrentChange: handleCurrentChange,
            handleSizeChange: handleSizeChange,
            handleTreeNodeClick: handleTreeNodeClick,
            formatPercent: formatPercent,
            formatPrice: formatPrice,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
