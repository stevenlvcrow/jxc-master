/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import PageTabsLayout from '@/components/PageTabsLayout.vue';
const tabs = [
    { key: 'category', label: '菜品分类关联仓库' },
    { key: 'dish', label: '菜品关联仓库' },
];
const toolbarButtons = [
    { key: '批量关联仓库', label: '批量关联仓库', type: 'primary' },
    { key: '批量取消关联仓库', label: '批量取消关联仓库' },
];
const activeTab = ref('category');
const dishTypeOptions = ['堂食', '加料', '餐盒'];
const manageRangeOptions = ['全部'];
const deleteStatusOptions = ['未删除', '已删除'];
const categoryOptions = [
    '堂食',
    '必点菜',
    '推出新品',
    '下饭菜',
    '家常菜',
    '汤类',
    '蔬菜类',
    '酒水饮料',
    '其他',
    '美团团购套餐',
    '店内套餐',
    '美团外卖',
    '饿了么外卖',
    '京东秒送菜品',
    '加料',
    '餐盒',
];
const warehouseTree = [
    {
        value: 'all-warehouse',
        label: '全部仓库',
        children: [
            {
                value: 'north-warehouse',
                label: '北区仓',
                children: [
                    { value: 'north-cold', label: '北区冷藏仓' },
                    { value: 'north-dry', label: '北区干货仓' },
                ],
            },
            {
                value: 'south-warehouse',
                label: '南区仓',
                children: [
                    { value: 'south-fresh', label: '南区生鲜仓' },
                    { value: 'south-pack', label: '南区包材仓' },
                ],
            },
        ],
    },
];
const dishTreeData = [
    {
        value: '全部',
        label: '全部',
        children: [
            { value: '堂食', label: '堂食' },
            { value: '必点菜', label: '必点菜' },
            { value: '推出新品', label: '推出新品' },
            { value: '下饭菜', label: '下饭菜' },
            { value: '家常菜', label: '家常菜' },
            { value: '汤类', label: '汤类' },
            { value: '蔬菜类', label: '蔬菜类' },
            { value: '酒水饮料', label: '酒水饮料' },
            { value: '其他', label: '其他' },
            { value: '美团团购套餐', label: '美团团购套餐' },
            { value: '店内套餐', label: '店内套餐' },
            { value: '美团外卖', label: '美团外卖' },
            { value: '饿了么外卖', label: '饿了么外卖' },
            { value: '京东秒送菜品', label: '京东秒送菜品' },
            { value: '加料', label: '加料' },
            { value: '餐盒', label: '餐盒' },
        ],
    },
];
const categoryWarehouseRows = [
    { id: 1, dishType: '堂食', categoryName: '必点菜', remark: '热销菜默认关联冷藏仓', warehouses: ['北区冷藏仓'] },
    { id: 2, dishType: '堂食', categoryName: '推出新品', remark: '新品单独补货', warehouses: ['北区干货仓', '南区生鲜仓'] },
    { id: 3, dishType: '堂食', categoryName: '下饭菜', remark: '', warehouses: ['南区生鲜仓'] },
    { id: 4, dishType: '堂食', categoryName: '家常菜', remark: '', warehouses: ['北区冷藏仓', '南区生鲜仓'] },
    { id: 5, dishType: '堂食', categoryName: '汤类', remark: '', warehouses: ['南区生鲜仓'] },
    { id: 6, dishType: '加料', categoryName: '加料', remark: '统一走包材仓', warehouses: ['南区包材仓'] },
    { id: 7, dishType: '餐盒', categoryName: '餐盒', remark: '独立包材仓管理', warehouses: ['南区包材仓'] },
];
const dishWarehouseRows = [
    { id: 1, dishCode: 'D0001', dishName: '宫保鸡丁', categoryName: '必点菜', spec: '标准份', warehouses: ['北区冷藏仓'], dishType: '堂食', linkedAt: '2026-04-13 10:10:00', deletedAt: '-', deletedStatus: '未删除' },
    { id: 2, dishCode: 'D0002', dishName: '鱼香肉丝', categoryName: '下饭菜', spec: '标准份', warehouses: ['北区冷藏仓', '南区生鲜仓'], dishType: '堂食', linkedAt: '2026-04-12 16:20:00', deletedAt: '-', deletedStatus: '未删除' },
    { id: 3, dishCode: 'D0003', dishName: '番茄牛腩', categoryName: '家常菜', spec: '大份', warehouses: ['南区生鲜仓'], dishType: '堂食', linkedAt: '2026-04-11 09:42:00', deletedAt: '-', deletedStatus: '未删除' },
    { id: 4, dishCode: 'D0004', dishName: '老坛酸菜', categoryName: '推出新品', spec: '标准份', warehouses: ['北区干货仓'], dishType: '堂食', linkedAt: '2026-04-10 14:05:00', deletedAt: '-', deletedStatus: '未删除' },
    { id: 5, dishCode: 'D0005', dishName: '脆爽萝卜', categoryName: '加料', spec: '加量', warehouses: ['南区包材仓'], dishType: '加料', linkedAt: '2026-04-09 12:18:00', deletedAt: '-', deletedStatus: '未删除' },
    { id: 6, dishCode: 'D0006', dishName: '外卖餐盒', categoryName: '餐盒', spec: '1000ml', warehouses: ['南区包材仓'], dishType: '餐盒', linkedAt: '2026-04-08 18:30:00', deletedAt: '-', deletedStatus: '未删除' },
    { id: 7, dishCode: 'D0007', dishName: '酸梅汤', categoryName: '酒水饮料', spec: '杯', warehouses: ['北区干货仓'], dishType: '堂食', linkedAt: '2026-04-08 10:22:00', deletedAt: '2026-04-12 09:00:00', deletedStatus: '已删除' },
    { id: 8, dishCode: 'D0008', dishName: '鲜蔬拼盘', categoryName: '蔬菜类', spec: '标准份', warehouses: ['南区生鲜仓'], dishType: '堂食', linkedAt: '2026-04-07 15:40:00', deletedAt: '-', deletedStatus: '未删除' },
];
const categoryQuery = reactive({
    dishType: '',
    categoryNames: [],
    warehouses: [],
});
const dishQuery = reactive({
    manageRange: '全部',
    dishInfo: '',
    warehouses: [],
    deleteStatus: '未删除',
});
const categorySelectedIds = ref([]);
const dishSelectedIds = ref([]);
const categoryCurrentPage = ref(1);
const dishCurrentPage = ref(1);
const categoryPageSize = ref(10);
const dishPageSize = ref(10);
const selectedTreeNode = ref('全部');
const warehouseLabelMap = computed(() => {
    const map = new Map();
    const walk = (nodes) => {
        nodes.forEach((node) => {
            map.set(node.value, node.label);
            if (node.children?.length) {
                walk(node.children);
            }
        });
    };
    walk(warehouseTree);
    return map;
});
const selectedWarehouseLabels = (values) => values
    .map((value) => warehouseLabelMap.value.get(value) ?? value)
    .filter((label) => label !== '全部仓库');
const categoryFilteredRows = computed(() => {
    const warehouseLabels = selectedWarehouseLabels(categoryQuery.warehouses);
    return categoryWarehouseRows.filter((row) => {
        const matchedDishType = !categoryQuery.dishType || row.dishType === categoryQuery.dishType;
        const matchedCategory = !categoryQuery.categoryNames.length || categoryQuery.categoryNames.includes(row.categoryName);
        const matchedWarehouse = !warehouseLabels.length || warehouseLabels.some((label) => row.warehouses.includes(label));
        return matchedDishType && matchedCategory && matchedWarehouse;
    });
});
const dishFilteredRows = computed(() => {
    const keyword = dishQuery.dishInfo.trim().toLowerCase();
    const warehouseLabels = selectedWarehouseLabels(dishQuery.warehouses);
    return dishWarehouseRows.filter((row) => {
        const matchedManageRange = dishQuery.manageRange === '全部';
        const matchedKeyword = !keyword
            || row.dishCode.toLowerCase().includes(keyword)
            || row.dishName.toLowerCase().includes(keyword);
        const matchedWarehouse = !warehouseLabels.length || warehouseLabels.some((label) => row.warehouses.includes(label));
        const matchedDeleteStatus = row.deletedStatus === dishQuery.deleteStatus;
        const matchedTree = selectedTreeNode.value === '全部' || row.categoryName === selectedTreeNode.value || row.dishType === selectedTreeNode.value;
        return matchedManageRange && matchedKeyword && matchedWarehouse && matchedDeleteStatus && matchedTree;
    });
});
const categoryPagedRows = computed(() => {
    const start = (categoryCurrentPage.value - 1) * categoryPageSize.value;
    return categoryFilteredRows.value.slice(start, start + categoryPageSize.value);
});
const dishPagedRows = computed(() => {
    const start = (dishCurrentPage.value - 1) * dishPageSize.value;
    return dishFilteredRows.value.slice(start, start + dishPageSize.value);
});
const handleCategorySearch = () => {
    categoryCurrentPage.value = 1;
};
const handleCategoryReset = () => {
    categoryQuery.dishType = '';
    categoryQuery.categoryNames = [];
    categoryQuery.warehouses = [];
    categoryCurrentPage.value = 1;
};
const handleDishSearch = () => {
    dishCurrentPage.value = 1;
};
const handleDishReset = () => {
    dishQuery.manageRange = '全部';
    dishQuery.dishInfo = '';
    dishQuery.warehouses = [];
    dishQuery.deleteStatus = '未删除';
    selectedTreeNode.value = '全部';
    dishCurrentPage.value = 1;
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
};
const handleCategorySelectionChange = (rows) => {
    categorySelectedIds.value = rows.map((row) => row.id);
};
const handleDishSelectionChange = (rows) => {
    dishSelectedIds.value = rows.map((row) => row.id);
};
const handleCategoryPageChange = (page) => {
    categoryCurrentPage.value = page;
};
const handleCategoryPageSizeChange = (size) => {
    categoryPageSize.value = size;
    categoryCurrentPage.value = 1;
};
const handleDishPageChange = (page) => {
    dishCurrentPage.value = page;
};
const handleDishPageSizeChange = (size) => {
    dishPageSize.value = size;
    dishCurrentPage.value = 1;
};
const handleDishTreeSelect = (node) => {
    selectedTreeNode.value = node.label;
    dishCurrentPage.value = 1;
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['dish-warehouse-page__content']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "dish-warehouse-page panel item-main-panel" },
});
/** @type {[typeof PageTabsLayout, typeof PageTabsLayout, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(PageTabsLayout, new PageTabsLayout({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
    bodyClass: "dish-warehouse-page__body",
}));
const __VLS_1 = __VLS_0({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
    bodyClass: "dish-warehouse-page__body",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
if (__VLS_ctx.activeTab === 'category') {
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_3 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.categoryQuery),
    }));
    const __VLS_4 = __VLS_3({
        model: (__VLS_ctx.categoryQuery),
    }, ...__VLS_functionalComponentArgsRest(__VLS_3));
    __VLS_5.slots.default;
    const __VLS_6 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(__VLS_6, new __VLS_6({
        label: "菜品类型",
    }));
    const __VLS_8 = __VLS_7({
        label: "菜品类型",
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    __VLS_9.slots.default;
    const __VLS_10 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
        modelValue: (__VLS_ctx.categoryQuery.dishType),
        placeholder: "请选择",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_12 = __VLS_11({
        modelValue: (__VLS_ctx.categoryQuery.dishType),
        placeholder: "请选择",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_11));
    __VLS_13.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.dishTypeOptions))) {
        const __VLS_14 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_16 = __VLS_15({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_15));
    }
    var __VLS_13;
    var __VLS_9;
    const __VLS_18 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
        label: "分类名称",
    }));
    const __VLS_20 = __VLS_19({
        label: "分类名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_19));
    __VLS_21.slots.default;
    const __VLS_22 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
        modelValue: (__VLS_ctx.categoryQuery.categoryNames),
        multiple: true,
        collapseTags: true,
        collapseTagsTooltip: true,
        placeholder: "请选择",
        ...{ style: {} },
    }));
    const __VLS_24 = __VLS_23({
        modelValue: (__VLS_ctx.categoryQuery.categoryNames),
        multiple: true,
        collapseTags: true,
        collapseTagsTooltip: true,
        placeholder: "请选择",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_23));
    __VLS_25.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.categoryOptions))) {
        const __VLS_26 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_28 = __VLS_27({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_27));
    }
    var __VLS_25;
    var __VLS_21;
    const __VLS_30 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
        label: "仓库",
    }));
    const __VLS_32 = __VLS_31({
        label: "仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_31));
    __VLS_33.slots.default;
    const __VLS_34 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
        modelValue: (__VLS_ctx.categoryQuery.warehouses),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        multiple: true,
        showCheckbox: true,
        checkStrictly: true,
        defaultExpandAll: true,
        collapseTags: true,
        collapseTagsTooltip: true,
        placeholder: "请选择",
        ...{ style: {} },
    }));
    const __VLS_36 = __VLS_35({
        modelValue: (__VLS_ctx.categoryQuery.warehouses),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        multiple: true,
        showCheckbox: true,
        checkStrictly: true,
        defaultExpandAll: true,
        collapseTags: true,
        collapseTagsTooltip: true,
        placeholder: "请选择",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_35));
    var __VLS_33;
    const __VLS_38 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({}));
    const __VLS_40 = __VLS_39({}, ...__VLS_functionalComponentArgsRest(__VLS_39));
    __VLS_41.slots.default;
    const __VLS_42 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_44 = __VLS_43({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_43));
    let __VLS_46;
    let __VLS_47;
    let __VLS_48;
    const __VLS_49 = {
        onClick: (__VLS_ctx.handleCategorySearch)
    };
    __VLS_45.slots.default;
    var __VLS_45;
    const __VLS_50 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
        ...{ 'onClick': {} },
    }));
    const __VLS_52 = __VLS_51({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_51));
    let __VLS_54;
    let __VLS_55;
    let __VLS_56;
    const __VLS_57 = {
        onClick: (__VLS_ctx.handleCategoryReset)
    };
    __VLS_53.slots.default;
    var __VLS_53;
    var __VLS_41;
    var __VLS_5;
    /** @type {[typeof CommonToolbarSection, ]} */ ;
    // @ts-ignore
    const __VLS_58 = __VLS_asFunctionalComponent(CommonToolbarSection, new CommonToolbarSection({
        ...{ 'onAction': {} },
        buttons: (__VLS_ctx.toolbarButtons),
    }));
    const __VLS_59 = __VLS_58({
        ...{ 'onAction': {} },
        buttons: (__VLS_ctx.toolbarButtons),
    }, ...__VLS_functionalComponentArgsRest(__VLS_58));
    let __VLS_61;
    let __VLS_62;
    let __VLS_63;
    const __VLS_64 = {
        onAction: (__VLS_ctx.handleToolbarAction)
    };
    var __VLS_60;
    const __VLS_65 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_66 = __VLS_asFunctionalComponent(__VLS_65, new __VLS_65({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.categoryPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_67 = __VLS_66({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.categoryPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_66));
    let __VLS_69;
    let __VLS_70;
    let __VLS_71;
    const __VLS_72 = {
        onSelectionChange: (__VLS_ctx.handleCategorySelectionChange)
    };
    __VLS_68.slots.default;
    const __VLS_73 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_74 = __VLS_asFunctionalComponent(__VLS_73, new __VLS_73({
        type: "selection",
        width: "44",
    }));
    const __VLS_75 = __VLS_74({
        type: "selection",
        width: "44",
    }, ...__VLS_functionalComponentArgsRest(__VLS_74));
    const __VLS_77 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_78 = __VLS_asFunctionalComponent(__VLS_77, new __VLS_77({
        type: "index",
        label: "序号",
        width: "60",
    }));
    const __VLS_79 = __VLS_78({
        type: "index",
        label: "序号",
        width: "60",
    }, ...__VLS_functionalComponentArgsRest(__VLS_78));
    const __VLS_81 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_82 = __VLS_asFunctionalComponent(__VLS_81, new __VLS_81({
        prop: "dishType",
        label: "菜品类型",
        minWidth: "120",
    }));
    const __VLS_83 = __VLS_82({
        prop: "dishType",
        label: "菜品类型",
        minWidth: "120",
    }, ...__VLS_functionalComponentArgsRest(__VLS_82));
    const __VLS_85 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_86 = __VLS_asFunctionalComponent(__VLS_85, new __VLS_85({
        prop: "categoryName",
        label: "分类名称",
        minWidth: "180",
        showOverflowTooltip: true,
    }));
    const __VLS_87 = __VLS_86({
        prop: "categoryName",
        label: "分类名称",
        minWidth: "180",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_86));
    const __VLS_89 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_90 = __VLS_asFunctionalComponent(__VLS_89, new __VLS_89({
        prop: "remark",
        label: "备注",
        minWidth: "220",
        showOverflowTooltip: true,
    }));
    const __VLS_91 = __VLS_90({
        prop: "remark",
        label: "备注",
        minWidth: "220",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_90));
    const __VLS_93 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_94 = __VLS_asFunctionalComponent(__VLS_93, new __VLS_93({
        label: "关联仓库",
        minWidth: "260",
        showOverflowTooltip: true,
    }));
    const __VLS_95 = __VLS_94({
        label: "关联仓库",
        minWidth: "260",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_94));
    __VLS_96.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_96.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        (row.warehouses.join('，') || '-');
    }
    var __VLS_96;
    var __VLS_68;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.categorySelectedIds.length);
    const __VLS_97 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_98 = __VLS_asFunctionalComponent(__VLS_97, new __VLS_97({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.categoryCurrentPage),
        pageSize: (__VLS_ctx.categoryPageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.categoryFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }));
    const __VLS_99 = __VLS_98({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.categoryCurrentPage),
        pageSize: (__VLS_ctx.categoryPageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.categoryFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_98));
    let __VLS_101;
    let __VLS_102;
    let __VLS_103;
    const __VLS_104 = {
        onCurrentChange: (__VLS_ctx.handleCategoryPageChange)
    };
    const __VLS_105 = {
        onSizeChange: (__VLS_ctx.handleCategoryPageSizeChange)
    };
    var __VLS_100;
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dish-warehouse-page__content" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
        ...{ class: "dish-warehouse-page__tree panel category-panel" },
    });
    const __VLS_106 = {}.ElTree;
    /** @type {[typeof __VLS_components.ElTree, typeof __VLS_components.elTree, ]} */ ;
    // @ts-ignore
    const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
        ...{ 'onNodeClick': {} },
        data: (__VLS_ctx.dishTreeData),
        nodeKey: "value",
        defaultExpandAll: true,
        ...{ class: "category-tree" },
        expandOnClickNode: (false),
    }));
    const __VLS_108 = __VLS_107({
        ...{ 'onNodeClick': {} },
        data: (__VLS_ctx.dishTreeData),
        nodeKey: "value",
        defaultExpandAll: true,
        ...{ class: "category-tree" },
        expandOnClickNode: (false),
    }, ...__VLS_functionalComponentArgsRest(__VLS_107));
    let __VLS_110;
    let __VLS_111;
    let __VLS_112;
    const __VLS_113 = {
        onNodeClick: (__VLS_ctx.handleDishTreeSelect)
    };
    var __VLS_109;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dish-warehouse-page__main" },
    });
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_114 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.dishQuery),
    }));
    const __VLS_115 = __VLS_114({
        model: (__VLS_ctx.dishQuery),
    }, ...__VLS_functionalComponentArgsRest(__VLS_114));
    __VLS_116.slots.default;
    const __VLS_117 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_118 = __VLS_asFunctionalComponent(__VLS_117, new __VLS_117({
        label: "管理菜品范围",
    }));
    const __VLS_119 = __VLS_118({
        label: "管理菜品范围",
    }, ...__VLS_functionalComponentArgsRest(__VLS_118));
    __VLS_120.slots.default;
    const __VLS_121 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_122 = __VLS_asFunctionalComponent(__VLS_121, new __VLS_121({
        modelValue: (__VLS_ctx.dishQuery.manageRange),
        ...{ style: {} },
    }));
    const __VLS_123 = __VLS_122({
        modelValue: (__VLS_ctx.dishQuery.manageRange),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_122));
    __VLS_124.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.manageRangeOptions))) {
        const __VLS_125 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_126 = __VLS_asFunctionalComponent(__VLS_125, new __VLS_125({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_127 = __VLS_126({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_126));
    }
    var __VLS_124;
    var __VLS_120;
    const __VLS_129 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_130 = __VLS_asFunctionalComponent(__VLS_129, new __VLS_129({
        label: "菜品信息",
    }));
    const __VLS_131 = __VLS_130({
        label: "菜品信息",
    }, ...__VLS_functionalComponentArgsRest(__VLS_130));
    __VLS_132.slots.default;
    const __VLS_133 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_134 = __VLS_asFunctionalComponent(__VLS_133, new __VLS_133({
        modelValue: (__VLS_ctx.dishQuery.dishInfo),
        placeholder: "请输入菜品编码或名称",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_135 = __VLS_134({
        modelValue: (__VLS_ctx.dishQuery.dishInfo),
        placeholder: "请输入菜品编码或名称",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_134));
    var __VLS_132;
    const __VLS_137 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_138 = __VLS_asFunctionalComponent(__VLS_137, new __VLS_137({
        label: "仓库",
    }));
    const __VLS_139 = __VLS_138({
        label: "仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_138));
    __VLS_140.slots.default;
    const __VLS_141 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_142 = __VLS_asFunctionalComponent(__VLS_141, new __VLS_141({
        modelValue: (__VLS_ctx.dishQuery.warehouses),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        multiple: true,
        showCheckbox: true,
        checkStrictly: true,
        defaultExpandAll: true,
        collapseTags: true,
        collapseTagsTooltip: true,
        placeholder: "请选择",
        ...{ style: {} },
    }));
    const __VLS_143 = __VLS_142({
        modelValue: (__VLS_ctx.dishQuery.warehouses),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        multiple: true,
        showCheckbox: true,
        checkStrictly: true,
        defaultExpandAll: true,
        collapseTags: true,
        collapseTagsTooltip: true,
        placeholder: "请选择",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_142));
    var __VLS_140;
    const __VLS_145 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_146 = __VLS_asFunctionalComponent(__VLS_145, new __VLS_145({
        label: "菜品是否删除",
    }));
    const __VLS_147 = __VLS_146({
        label: "菜品是否删除",
    }, ...__VLS_functionalComponentArgsRest(__VLS_146));
    __VLS_148.slots.default;
    const __VLS_149 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_150 = __VLS_asFunctionalComponent(__VLS_149, new __VLS_149({
        modelValue: (__VLS_ctx.dishQuery.deleteStatus),
        ...{ style: {} },
    }));
    const __VLS_151 = __VLS_150({
        modelValue: (__VLS_ctx.dishQuery.deleteStatus),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_150));
    __VLS_152.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.deleteStatusOptions))) {
        const __VLS_153 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_154 = __VLS_asFunctionalComponent(__VLS_153, new __VLS_153({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_155 = __VLS_154({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_154));
    }
    var __VLS_152;
    var __VLS_148;
    const __VLS_157 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_158 = __VLS_asFunctionalComponent(__VLS_157, new __VLS_157({}));
    const __VLS_159 = __VLS_158({}, ...__VLS_functionalComponentArgsRest(__VLS_158));
    __VLS_160.slots.default;
    const __VLS_161 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_162 = __VLS_asFunctionalComponent(__VLS_161, new __VLS_161({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_163 = __VLS_162({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_162));
    let __VLS_165;
    let __VLS_166;
    let __VLS_167;
    const __VLS_168 = {
        onClick: (__VLS_ctx.handleDishSearch)
    };
    __VLS_164.slots.default;
    var __VLS_164;
    const __VLS_169 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_170 = __VLS_asFunctionalComponent(__VLS_169, new __VLS_169({
        ...{ 'onClick': {} },
    }));
    const __VLS_171 = __VLS_170({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_170));
    let __VLS_173;
    let __VLS_174;
    let __VLS_175;
    const __VLS_176 = {
        onClick: (__VLS_ctx.handleDishReset)
    };
    __VLS_172.slots.default;
    var __VLS_172;
    var __VLS_160;
    var __VLS_116;
    /** @type {[typeof CommonToolbarSection, ]} */ ;
    // @ts-ignore
    const __VLS_177 = __VLS_asFunctionalComponent(CommonToolbarSection, new CommonToolbarSection({
        ...{ 'onAction': {} },
        buttons: (__VLS_ctx.toolbarButtons),
    }));
    const __VLS_178 = __VLS_177({
        ...{ 'onAction': {} },
        buttons: (__VLS_ctx.toolbarButtons),
    }, ...__VLS_functionalComponentArgsRest(__VLS_177));
    let __VLS_180;
    let __VLS_181;
    let __VLS_182;
    const __VLS_183 = {
        onAction: (__VLS_ctx.handleToolbarAction)
    };
    var __VLS_179;
    const __VLS_184 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_185 = __VLS_asFunctionalComponent(__VLS_184, new __VLS_184({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.dishPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_186 = __VLS_185({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.dishPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_185));
    let __VLS_188;
    let __VLS_189;
    let __VLS_190;
    const __VLS_191 = {
        onSelectionChange: (__VLS_ctx.handleDishSelectionChange)
    };
    __VLS_187.slots.default;
    const __VLS_192 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_193 = __VLS_asFunctionalComponent(__VLS_192, new __VLS_192({
        type: "selection",
        width: "44",
        fixed: "left",
    }));
    const __VLS_194 = __VLS_193({
        type: "selection",
        width: "44",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_193));
    const __VLS_196 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_197 = __VLS_asFunctionalComponent(__VLS_196, new __VLS_196({
        type: "index",
        label: "序号",
        width: "60",
        fixed: "left",
    }));
    const __VLS_198 = __VLS_197({
        type: "index",
        label: "序号",
        width: "60",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_197));
    const __VLS_200 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_201 = __VLS_asFunctionalComponent(__VLS_200, new __VLS_200({
        prop: "dishCode",
        label: "菜品编码",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_202 = __VLS_201({
        prop: "dishCode",
        label: "菜品编码",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_201));
    const __VLS_204 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_205 = __VLS_asFunctionalComponent(__VLS_204, new __VLS_204({
        prop: "dishName",
        label: "菜品名称",
        minWidth: "150",
        showOverflowTooltip: true,
    }));
    const __VLS_206 = __VLS_205({
        prop: "dishName",
        label: "菜品名称",
        minWidth: "150",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_205));
    const __VLS_208 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_209 = __VLS_asFunctionalComponent(__VLS_208, new __VLS_208({
        prop: "categoryName",
        label: "菜品分类",
        minWidth: "140",
        showOverflowTooltip: true,
    }));
    const __VLS_210 = __VLS_209({
        prop: "categoryName",
        label: "菜品分类",
        minWidth: "140",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_209));
    const __VLS_212 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_213 = __VLS_asFunctionalComponent(__VLS_212, new __VLS_212({
        prop: "spec",
        label: "规格",
        minWidth: "110",
        showOverflowTooltip: true,
    }));
    const __VLS_214 = __VLS_213({
        prop: "spec",
        label: "规格",
        minWidth: "110",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_213));
    const __VLS_216 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_217 = __VLS_asFunctionalComponent(__VLS_216, new __VLS_216({
        label: "关联仓库",
        minWidth: "240",
        showOverflowTooltip: true,
    }));
    const __VLS_218 = __VLS_217({
        label: "关联仓库",
        minWidth: "240",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_217));
    __VLS_219.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_219.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        (row.warehouses.join('，') || '-');
    }
    var __VLS_219;
    const __VLS_220 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_221 = __VLS_asFunctionalComponent(__VLS_220, new __VLS_220({
        prop: "dishType",
        label: "菜品类型",
        minWidth: "100",
    }));
    const __VLS_222 = __VLS_221({
        prop: "dishType",
        label: "菜品类型",
        minWidth: "100",
    }, ...__VLS_functionalComponentArgsRest(__VLS_221));
    const __VLS_224 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_225 = __VLS_asFunctionalComponent(__VLS_224, new __VLS_224({
        prop: "linkedAt",
        label: "最新关联时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }));
    const __VLS_226 = __VLS_225({
        prop: "linkedAt",
        label: "最新关联时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_225));
    const __VLS_228 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_229 = __VLS_asFunctionalComponent(__VLS_228, new __VLS_228({
        prop: "deletedAt",
        label: "删除时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }));
    const __VLS_230 = __VLS_229({
        prop: "deletedAt",
        label: "删除时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_229));
    var __VLS_187;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.dishSelectedIds.length);
    const __VLS_232 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_233 = __VLS_asFunctionalComponent(__VLS_232, new __VLS_232({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.dishCurrentPage),
        pageSize: (__VLS_ctx.dishPageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.dishFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }));
    const __VLS_234 = __VLS_233({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.dishCurrentPage),
        pageSize: (__VLS_ctx.dishPageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.dishFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_233));
    let __VLS_236;
    let __VLS_237;
    let __VLS_238;
    const __VLS_239 = {
        onCurrentChange: (__VLS_ctx.handleDishPageChange)
    };
    const __VLS_240 = {
        onSizeChange: (__VLS_ctx.handleDishPageSizeChange)
    };
    var __VLS_235;
}
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['dish-warehouse-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['dish-warehouse-page__content']} */ ;
/** @type {__VLS_StyleScopedClasses['dish-warehouse-page__tree']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['category-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['category-tree']} */ ;
/** @type {__VLS_StyleScopedClasses['dish-warehouse-page__main']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CommonQuerySection: CommonQuerySection,
            CommonToolbarSection: CommonToolbarSection,
            PageTabsLayout: PageTabsLayout,
            tabs: tabs,
            toolbarButtons: toolbarButtons,
            activeTab: activeTab,
            dishTypeOptions: dishTypeOptions,
            manageRangeOptions: manageRangeOptions,
            deleteStatusOptions: deleteStatusOptions,
            categoryOptions: categoryOptions,
            warehouseTree: warehouseTree,
            dishTreeData: dishTreeData,
            categoryQuery: categoryQuery,
            dishQuery: dishQuery,
            categorySelectedIds: categorySelectedIds,
            dishSelectedIds: dishSelectedIds,
            categoryCurrentPage: categoryCurrentPage,
            dishCurrentPage: dishCurrentPage,
            categoryPageSize: categoryPageSize,
            dishPageSize: dishPageSize,
            categoryFilteredRows: categoryFilteredRows,
            dishFilteredRows: dishFilteredRows,
            categoryPagedRows: categoryPagedRows,
            dishPagedRows: dishPagedRows,
            handleCategorySearch: handleCategorySearch,
            handleCategoryReset: handleCategoryReset,
            handleDishSearch: handleDishSearch,
            handleDishReset: handleDishReset,
            handleToolbarAction: handleToolbarAction,
            handleCategorySelectionChange: handleCategorySelectionChange,
            handleDishSelectionChange: handleDishSelectionChange,
            handleCategoryPageChange: handleCategoryPageChange,
            handleCategoryPageSizeChange: handleCategoryPageSizeChange,
            handleDishPageChange: handleDishPageChange,
            handleDishPageSizeChange: handleDishPageSizeChange,
            handleDishTreeSelect: handleDishTreeSelect,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
