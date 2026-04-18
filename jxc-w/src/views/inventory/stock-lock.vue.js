/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Download, Lock, RefreshRight, Search, Unlock } from '@element-plus/icons-vue';
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
const query = reactive({
    lockDateRange: [],
    warehouse: '',
    documentCode: '',
    upstreamCode: '',
    itemName: '',
    lockReason: '',
});
const tableData = [
    {
        id: 1,
        documentCode: 'LK-202604-001',
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
        lockReason: '盘点冻结',
        lockDate: '2026-04-13',
        remark: '',
    },
    {
        id: 2,
        documentCode: 'LK-202604-002',
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
        lockReason: '品质异常',
        lockDate: '2026-04-12',
        remark: '待复检',
    },
    {
        id: 3,
        documentCode: 'LK-202604-003',
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
        lockReason: '外部稽核',
        lockDate: '2026-04-11',
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
    const [startDate, endDate] = query.lockDateRange;
    return tableData.filter((row) => {
        const matchedStart = !startDate || row.lockDate >= startDate;
        const matchedEnd = !endDate || row.lockDate <= endDate;
        const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
        const matchedDocument = !query.documentCode || row.documentCode.includes(query.documentCode);
        const matchedUpstream = !query.upstreamCode || row.upstreamCode.includes(query.upstreamCode);
        const matchedItem = !query.itemName || row.itemName === query.itemName;
        const matchedReason = !query.lockReason || row.lockReason === query.lockReason;
        return matchedStart
            && matchedEnd
            && matchedWarehouse
            && matchedDocument
            && matchedUpstream
            && matchedItem
            && matchedReason;
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
    query.lockDateRange = [];
    query.warehouse = '';
    query.documentCode = '';
    query.upstreamCode = '';
    query.itemName = '';
    query.lockReason = '';
    currentPage.value = 1;
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
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
    label: "锁库日期",
}));
const __VLS_5 = __VLS_4({
    label: "锁库日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.lockDateRange),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
    rangeSeparator: "~",
    startPlaceholder: "开始日期",
    endPlaceholder: "结束日期",
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.lockDateRange),
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
    ...{ style: {} },
}));
const __VLS_41 = __VLS_40({
    modelValue: (__VLS_ctx.query.itemName),
    clearable: true,
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
const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({}));
const __VLS_57 = __VLS_56({}, ...__VLS_functionalComponentArgsRest(__VLS_56));
__VLS_58.slots.default;
const __VLS_59 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_61 = __VLS_60({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_60));
let __VLS_63;
let __VLS_64;
let __VLS_65;
const __VLS_66 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_62.slots.default;
const __VLS_67 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({}));
const __VLS_69 = __VLS_68({}, ...__VLS_functionalComponentArgsRest(__VLS_68));
__VLS_70.slots.default;
const __VLS_71 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({}));
const __VLS_73 = __VLS_72({}, ...__VLS_functionalComponentArgsRest(__VLS_72));
var __VLS_70;
var __VLS_62;
const __VLS_75 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
    ...{ 'onClick': {} },
}));
const __VLS_77 = __VLS_76({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_76));
let __VLS_79;
let __VLS_80;
let __VLS_81;
const __VLS_82 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_78.slots.default;
const __VLS_83 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({}));
const __VLS_85 = __VLS_84({}, ...__VLS_functionalComponentArgsRest(__VLS_84));
__VLS_86.slots.default;
const __VLS_87 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({}));
const __VLS_89 = __VLS_88({}, ...__VLS_functionalComponentArgsRest(__VLS_88));
var __VLS_86;
var __VLS_78;
var __VLS_58;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_91 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_93 = __VLS_92({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_92));
let __VLS_95;
let __VLS_96;
let __VLS_97;
const __VLS_98 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('锁库');
    }
};
__VLS_94.slots.default;
const __VLS_99 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({}));
const __VLS_101 = __VLS_100({}, ...__VLS_functionalComponentArgsRest(__VLS_100));
__VLS_102.slots.default;
const __VLS_103 = {}.Lock;
/** @type {[typeof __VLS_components.Lock, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({}));
const __VLS_105 = __VLS_104({}, ...__VLS_functionalComponentArgsRest(__VLS_104));
var __VLS_102;
var __VLS_94;
const __VLS_107 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
    ...{ 'onClick': {} },
}));
const __VLS_109 = __VLS_108({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_108));
let __VLS_111;
let __VLS_112;
let __VLS_113;
const __VLS_114 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('解锁');
    }
};
__VLS_110.slots.default;
const __VLS_115 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({}));
const __VLS_117 = __VLS_116({}, ...__VLS_functionalComponentArgsRest(__VLS_116));
__VLS_118.slots.default;
const __VLS_119 = {}.Unlock;
/** @type {[typeof __VLS_components.Unlock, ]} */ ;
// @ts-ignore
const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({}));
const __VLS_121 = __VLS_120({}, ...__VLS_functionalComponentArgsRest(__VLS_120));
var __VLS_118;
var __VLS_110;
const __VLS_123 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
    ...{ 'onClick': {} },
}));
const __VLS_125 = __VLS_124({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_124));
let __VLS_127;
let __VLS_128;
let __VLS_129;
const __VLS_130 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量锁库');
    }
};
__VLS_126.slots.default;
var __VLS_126;
const __VLS_131 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({
    ...{ 'onClick': {} },
}));
const __VLS_133 = __VLS_132({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_132));
let __VLS_135;
let __VLS_136;
let __VLS_137;
const __VLS_138 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量解锁');
    }
};
__VLS_134.slots.default;
var __VLS_134;
const __VLS_139 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
    ...{ 'onClick': {} },
}));
const __VLS_141 = __VLS_140({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_140));
let __VLS_143;
let __VLS_144;
let __VLS_145;
const __VLS_146 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导出单据列表');
    }
};
__VLS_142.slots.default;
const __VLS_147 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({}));
const __VLS_149 = __VLS_148({}, ...__VLS_functionalComponentArgsRest(__VLS_148));
__VLS_150.slots.default;
const __VLS_151 = {}.Download;
/** @type {[typeof __VLS_components.Download, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({}));
const __VLS_153 = __VLS_152({}, ...__VLS_functionalComponentArgsRest(__VLS_152));
var __VLS_150;
var __VLS_142;
const __VLS_155 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (400),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_157 = __VLS_156({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (400),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_156));
__VLS_158.slots.default;
const __VLS_159 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_161 = __VLS_160({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_160));
const __VLS_163 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_165 = __VLS_164({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_164));
const __VLS_167 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({
    prop: "upstreamCode",
    label: "上游单据号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_169 = __VLS_168({
    prop: "upstreamCode",
    label: "上游单据号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_168));
const __VLS_171 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({
    prop: "upstreamType",
    label: "上游单据类型",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_173 = __VLS_172({
    prop: "upstreamType",
    label: "上游单据类型",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_172));
const __VLS_175 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
    prop: "orgName",
    label: "机构",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_177 = __VLS_176({
    prop: "orgName",
    label: "机构",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_176));
const __VLS_179 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_181 = __VLS_180({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_180));
const __VLS_183 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({
    prop: "itemCode",
    label: "物品编码",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_185 = __VLS_184({
    prop: "itemCode",
    label: "物品编码",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_184));
const __VLS_187 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({
    prop: "itemName",
    label: "物品名称",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_189 = __VLS_188({
    prop: "itemName",
    label: "物品名称",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_188));
const __VLS_191 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
    prop: "spec",
    label: "规格型号",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_193 = __VLS_192({
    prop: "spec",
    label: "规格型号",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_192));
const __VLS_195 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_196 = __VLS_asFunctionalComponent(__VLS_195, new __VLS_195({
    prop: "stockUnit",
    label: "库存单位",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_197 = __VLS_196({
    prop: "stockUnit",
    label: "库存单位",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_196));
const __VLS_199 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
    prop: "stockUnitQty",
    label: "库存单位锁库数量",
    minWidth: "160",
    showOverflowTooltip: true,
}));
const __VLS_201 = __VLS_200({
    prop: "stockUnitQty",
    label: "库存单位锁库数量",
    minWidth: "160",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_200));
const __VLS_203 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
    prop: "baseUnit",
    label: "基准单位",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_205 = __VLS_204({
    prop: "baseUnit",
    label: "基准单位",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_204));
const __VLS_207 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    prop: "baseUnitQty",
    label: "基准单位锁库数量",
    minWidth: "160",
    showOverflowTooltip: true,
}));
const __VLS_209 = __VLS_208({
    prop: "baseUnitQty",
    label: "基准单位锁库数量",
    minWidth: "160",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
const __VLS_211 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_212 = __VLS_asFunctionalComponent(__VLS_211, new __VLS_211({
    prop: "lockReason",
    label: "锁库原因",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_213 = __VLS_212({
    prop: "lockReason",
    label: "锁库原因",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_212));
const __VLS_215 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
    prop: "lockDate",
    label: "锁库日期",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_217 = __VLS_216({
    prop: "lockDate",
    label: "锁库日期",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_216));
const __VLS_219 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({
    prop: "remark",
    label: "备注",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_221 = __VLS_220({
    prop: "remark",
    label: "备注",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_220));
var __VLS_158;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
const __VLS_223 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
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
const __VLS_225 = __VLS_224({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_224));
let __VLS_227;
let __VLS_228;
let __VLS_229;
const __VLS_230 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_231 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_226;
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
            Lock: Lock,
            RefreshRight: RefreshRight,
            Search: Search,
            Unlock: Unlock,
            CommonQuerySection: CommonQuerySection,
            warehouseTree: warehouseTree,
            itemOptions: itemOptions,
            reasonTree: reasonTree,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
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
