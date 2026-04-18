/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Download, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
const dateTypeOptions = ['调整日期', '创建时间'];
const documentStatusOptions = ['草稿', '已提交', '已审核'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const sessionStore = useSessionStore();
const query = reactive({
    dateType: '调整日期',
    startDate: '',
    endDate: '',
    warehouse: '',
    documentCode: '',
    documentStatus: '',
    itemName: '',
    remark: '',
});
const tableData = [
    {
        id: 1,
        documentCode: 'BA-202604-001',
        adjustDate: '2026-04-13',
        warehouse: '中央成品仓',
        itemCount: 8,
        status: '已审核',
        creator: '张敏',
        createdAt: '2026-04-13 12:20:00',
        remark: '批次调整',
    },
    {
        id: 2,
        documentCode: 'BA-202604-002',
        adjustDate: '2026-04-12',
        warehouse: '北区原料仓',
        itemCount: 5,
        status: '已提交',
        creator: '李娜',
        createdAt: '2026-04-12 15:40:00',
        remark: '',
    },
    {
        id: 3,
        documentCode: 'BA-202604-003',
        adjustDate: '2026-04-11',
        warehouse: '南区包材仓',
        itemCount: 3,
        status: '草稿',
        creator: '王磊',
        createdAt: '2026-04-11 09:22:00',
        remark: '批次回溯调整',
    },
];
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref([]);
const filteredRows = computed(() => {
    const codeKeyword = query.documentCode.trim().toLowerCase();
    const remarkKeyword = query.remark.trim().toLowerCase();
    return tableData.filter((row) => {
        const dateField = query.dateType === '调整日期' ? row.adjustDate : row.createdAt.slice(0, 10);
        const matchedStartDate = !query.startDate || dateField >= query.startDate;
        const matchedEndDate = !query.endDate || dateField <= query.endDate;
        const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
        const matchedCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
        const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
        const matchedItem = !query.itemName || row.documentCode.includes(query.itemName);
        const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
        return matchedStartDate
            && matchedEndDate
            && matchedWarehouse
            && matchedCode
            && matchedStatus
            && matchedItem
            && matchedRemark;
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
    query.dateType = '调整日期';
    query.startDate = '';
    query.endDate = '';
    query.warehouse = '';
    query.documentCode = '';
    query.documentStatus = '';
    query.itemName = '';
    query.remark = '';
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
    ElMessage.info(`查看：${row.documentCode}`);
};
const handleEdit = (row) => {
    ElMessage.info(`编辑：${row.documentCode}`);
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
onMounted(loadWarehouseTree);
watch(() => sessionStore.currentOrgId, () => {
    loadWarehouseTree();
});
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
    label: "日期类型",
}));
const __VLS_5 = __VLS_4({
    label: "日期类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.dateType),
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.dateType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
__VLS_10.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.dateTypeOptions))) {
    const __VLS_11 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_13 = __VLS_12({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_12));
}
var __VLS_10;
var __VLS_6;
const __VLS_15 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    label: "开始日期",
}));
const __VLS_17 = __VLS_16({
    label: "开始日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
__VLS_18.slots.default;
const __VLS_19 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    modelValue: (__VLS_ctx.query.startDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择开始日期",
    ...{ style: {} },
}));
const __VLS_21 = __VLS_20({
    modelValue: (__VLS_ctx.query.startDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择开始日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
var __VLS_18;
const __VLS_23 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    label: "结束日期",
}));
const __VLS_25 = __VLS_24({
    label: "结束日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
const __VLS_27 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    modelValue: (__VLS_ctx.query.endDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择结束日期",
    ...{ style: {} },
}));
const __VLS_29 = __VLS_28({
    modelValue: (__VLS_ctx.query.endDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择结束日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
var __VLS_26;
const __VLS_31 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    label: "仓库",
}));
const __VLS_33 = __VLS_32({
    label: "仓库",
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
__VLS_34.slots.default;
const __VLS_35 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_37 = __VLS_36({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
var __VLS_34;
const __VLS_39 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({
    label: "单据编号",
}));
const __VLS_41 = __VLS_40({
    label: "单据编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
__VLS_42.slots.default;
const __VLS_43 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    modelValue: (__VLS_ctx.query.documentCode),
    placeholder: "请输入单据编号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_45 = __VLS_44({
    modelValue: (__VLS_ctx.query.documentCode),
    placeholder: "请输入单据编号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
var __VLS_42;
const __VLS_47 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({
    label: "单据状态",
}));
const __VLS_49 = __VLS_48({
    label: "单据状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_48));
__VLS_50.slots.default;
const __VLS_51 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
    modelValue: (__VLS_ctx.query.documentStatus),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_53 = __VLS_52({
    modelValue: (__VLS_ctx.query.documentStatus),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_52));
__VLS_54.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.documentStatusOptions))) {
    const __VLS_55 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_57 = __VLS_56({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_56));
}
var __VLS_54;
var __VLS_50;
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
const __VLS_63 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({
    modelValue: (__VLS_ctx.query.itemName),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_65 = __VLS_64({
    modelValue: (__VLS_ctx.query.itemName),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_64));
__VLS_66.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
    const __VLS_67 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_69 = __VLS_68({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_68));
}
var __VLS_66;
var __VLS_62;
const __VLS_71 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
    label: "备注",
}));
const __VLS_73 = __VLS_72({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_72));
__VLS_74.slots.default;
const __VLS_75 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_77 = __VLS_76({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_76));
var __VLS_74;
const __VLS_79 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({}));
const __VLS_81 = __VLS_80({}, ...__VLS_functionalComponentArgsRest(__VLS_80));
__VLS_82.slots.default;
const __VLS_83 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_85 = __VLS_84({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_84));
let __VLS_87;
let __VLS_88;
let __VLS_89;
const __VLS_90 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_86.slots.default;
const __VLS_91 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({}));
const __VLS_93 = __VLS_92({}, ...__VLS_functionalComponentArgsRest(__VLS_92));
__VLS_94.slots.default;
const __VLS_95 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({}));
const __VLS_97 = __VLS_96({}, ...__VLS_functionalComponentArgsRest(__VLS_96));
var __VLS_94;
var __VLS_86;
const __VLS_99 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({
    ...{ 'onClick': {} },
}));
const __VLS_101 = __VLS_100({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_100));
let __VLS_103;
let __VLS_104;
let __VLS_105;
const __VLS_106 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_102.slots.default;
const __VLS_107 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({}));
const __VLS_109 = __VLS_108({}, ...__VLS_functionalComponentArgsRest(__VLS_108));
__VLS_110.slots.default;
const __VLS_111 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({}));
const __VLS_113 = __VLS_112({}, ...__VLS_functionalComponentArgsRest(__VLS_112));
var __VLS_110;
var __VLS_102;
var __VLS_82;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_115 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_117 = __VLS_116({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_116));
let __VLS_119;
let __VLS_120;
let __VLS_121;
const __VLS_122 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('新增');
    }
};
__VLS_118.slots.default;
const __VLS_123 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({}));
const __VLS_125 = __VLS_124({}, ...__VLS_functionalComponentArgsRest(__VLS_124));
__VLS_126.slots.default;
const __VLS_127 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({}));
const __VLS_129 = __VLS_128({}, ...__VLS_functionalComponentArgsRest(__VLS_128));
var __VLS_126;
var __VLS_118;
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
        __VLS_ctx.handleToolbarAction('批量删除');
    }
};
__VLS_134.slots.default;
const __VLS_139 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({}));
const __VLS_141 = __VLS_140({}, ...__VLS_functionalComponentArgsRest(__VLS_140));
__VLS_142.slots.default;
const __VLS_143 = {}.Delete;
/** @type {[typeof __VLS_components.Delete, ]} */ ;
// @ts-ignore
const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({}));
const __VLS_145 = __VLS_144({}, ...__VLS_functionalComponentArgsRest(__VLS_144));
var __VLS_142;
var __VLS_134;
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
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量审核');
    }
};
__VLS_150.slots.default;
var __VLS_150;
const __VLS_155 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
    ...{ 'onClick': {} },
}));
const __VLS_157 = __VLS_156({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_156));
let __VLS_159;
let __VLS_160;
let __VLS_161;
const __VLS_162 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量反审核');
    }
};
__VLS_158.slots.default;
var __VLS_158;
const __VLS_163 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({
    ...{ 'onClick': {} },
}));
const __VLS_165 = __VLS_164({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_164));
let __VLS_167;
let __VLS_168;
let __VLS_169;
const __VLS_170 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导出单据列表');
    }
};
__VLS_166.slots.default;
const __VLS_171 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({}));
const __VLS_173 = __VLS_172({}, ...__VLS_functionalComponentArgsRest(__VLS_172));
__VLS_174.slots.default;
const __VLS_175 = {}.Download;
/** @type {[typeof __VLS_components.Download, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({}));
const __VLS_177 = __VLS_176({}, ...__VLS_functionalComponentArgsRest(__VLS_176));
var __VLS_174;
var __VLS_166;
const __VLS_179 = {}.ElDropdown;
/** @type {[typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, ]} */ ;
// @ts-ignore
const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({
    ...{ 'onCommand': {} },
}));
const __VLS_181 = __VLS_180({
    ...{ 'onCommand': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_180));
let __VLS_183;
let __VLS_184;
let __VLS_185;
const __VLS_186 = {
    onCommand: (__VLS_ctx.handleTableSettingCommand)
};
__VLS_182.slots.default;
const __VLS_187 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({}));
const __VLS_189 = __VLS_188({}, ...__VLS_functionalComponentArgsRest(__VLS_188));
__VLS_190.slots.default;
const __VLS_191 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({}));
const __VLS_193 = __VLS_192({}, ...__VLS_functionalComponentArgsRest(__VLS_192));
__VLS_194.slots.default;
const __VLS_195 = {}.ArrowDown;
/** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
// @ts-ignore
const __VLS_196 = __VLS_asFunctionalComponent(__VLS_195, new __VLS_195({}));
const __VLS_197 = __VLS_196({}, ...__VLS_functionalComponentArgsRest(__VLS_196));
var __VLS_194;
var __VLS_190;
{
    const { dropdown: __VLS_thisSlot } = __VLS_182.slots;
    const __VLS_199 = {}.ElDropdownMenu;
    /** @type {[typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, ]} */ ;
    // @ts-ignore
    const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({}));
    const __VLS_201 = __VLS_200({}, ...__VLS_functionalComponentArgsRest(__VLS_200));
    __VLS_202.slots.default;
    const __VLS_203 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
        command: "单据编号",
    }));
    const __VLS_205 = __VLS_204({
        command: "单据编号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_204));
    __VLS_206.slots.default;
    var __VLS_206;
    const __VLS_207 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
        command: "调整日期",
    }));
    const __VLS_209 = __VLS_208({
        command: "调整日期",
    }, ...__VLS_functionalComponentArgsRest(__VLS_208));
    __VLS_210.slots.default;
    var __VLS_210;
    const __VLS_211 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_212 = __VLS_asFunctionalComponent(__VLS_211, new __VLS_211({
        command: "仓库",
    }));
    const __VLS_213 = __VLS_212({
        command: "仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_212));
    __VLS_214.slots.default;
    var __VLS_214;
    const __VLS_215 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
        command: "物品（项）",
    }));
    const __VLS_217 = __VLS_216({
        command: "物品（项）",
    }, ...__VLS_functionalComponentArgsRest(__VLS_216));
    __VLS_218.slots.default;
    var __VLS_218;
    const __VLS_219 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({
        command: "状态",
    }));
    const __VLS_221 = __VLS_220({
        command: "状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_220));
    __VLS_222.slots.default;
    var __VLS_222;
    const __VLS_223 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
        command: "创建人",
    }));
    const __VLS_225 = __VLS_224({
        command: "创建人",
    }, ...__VLS_functionalComponentArgsRest(__VLS_224));
    __VLS_226.slots.default;
    var __VLS_226;
    const __VLS_227 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
        command: "创建时间",
    }));
    const __VLS_229 = __VLS_228({
        command: "创建时间",
    }, ...__VLS_functionalComponentArgsRest(__VLS_228));
    __VLS_230.slots.default;
    var __VLS_230;
    const __VLS_231 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
        command: "备注",
    }));
    const __VLS_233 = __VLS_232({
        command: "备注",
    }, ...__VLS_functionalComponentArgsRest(__VLS_232));
    __VLS_234.slots.default;
    var __VLS_234;
    const __VLS_235 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
        command: "操作",
    }));
    const __VLS_237 = __VLS_236({
        command: "操作",
    }, ...__VLS_functionalComponentArgsRest(__VLS_236));
    __VLS_238.slots.default;
    var __VLS_238;
    var __VLS_202;
}
var __VLS_182;
const __VLS_239 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_241 = __VLS_240({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
let __VLS_243;
let __VLS_244;
let __VLS_245;
const __VLS_246 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_242.slots.default;
const __VLS_247 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_249 = __VLS_248({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_248));
const __VLS_251 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_252 = __VLS_asFunctionalComponent(__VLS_251, new __VLS_251({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_253 = __VLS_252({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_252));
const __VLS_255 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_257 = __VLS_256({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_256));
const __VLS_259 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_260 = __VLS_asFunctionalComponent(__VLS_259, new __VLS_259({
    prop: "adjustDate",
    label: "调整日期",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_261 = __VLS_260({
    prop: "adjustDate",
    label: "调整日期",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_260));
const __VLS_263 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_264 = __VLS_asFunctionalComponent(__VLS_263, new __VLS_263({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_265 = __VLS_264({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_264));
const __VLS_267 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_268 = __VLS_asFunctionalComponent(__VLS_267, new __VLS_267({
    prop: "itemCount",
    label: "物品（项）",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_269 = __VLS_268({
    prop: "itemCount",
    label: "物品（项）",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_268));
const __VLS_271 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_272 = __VLS_asFunctionalComponent(__VLS_271, new __VLS_271({
    prop: "status",
    label: "状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_273 = __VLS_272({
    prop: "status",
    label: "状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_272));
const __VLS_275 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_276 = __VLS_asFunctionalComponent(__VLS_275, new __VLS_275({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_277 = __VLS_276({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_276));
const __VLS_279 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_280 = __VLS_asFunctionalComponent(__VLS_279, new __VLS_279({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_281 = __VLS_280({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_280));
const __VLS_283 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_284 = __VLS_asFunctionalComponent(__VLS_283, new __VLS_283({
    prop: "remark",
    label: "备注",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_285 = __VLS_284({
    prop: "remark",
    label: "备注",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_284));
const __VLS_287 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_288 = __VLS_asFunctionalComponent(__VLS_287, new __VLS_287({
    label: "操作",
    width: "120",
    fixed: "right",
}));
const __VLS_289 = __VLS_288({
    label: "操作",
    width: "120",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_288));
__VLS_290.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_290.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_291 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_292 = __VLS_asFunctionalComponent(__VLS_291, new __VLS_291({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_293 = __VLS_292({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_292));
    let __VLS_295;
    let __VLS_296;
    let __VLS_297;
    const __VLS_298 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_294.slots.default;
    var __VLS_294;
    const __VLS_299 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_300 = __VLS_asFunctionalComponent(__VLS_299, new __VLS_299({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_301 = __VLS_300({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_300));
    let __VLS_303;
    let __VLS_304;
    let __VLS_305;
    const __VLS_306 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleEdit(row);
        }
    };
    __VLS_302.slots.default;
    var __VLS_302;
}
var __VLS_290;
var __VLS_242;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedIds.length);
const __VLS_307 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_308 = __VLS_asFunctionalComponent(__VLS_307, new __VLS_307({
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
const __VLS_309 = __VLS_308({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_308));
let __VLS_311;
let __VLS_312;
let __VLS_313;
const __VLS_314 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_315 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_310;
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
            Download: Download,
            Plus: Plus,
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            dateTypeOptions: dateTypeOptions,
            documentStatusOptions: documentStatusOptions,
            itemOptions: itemOptions,
            warehouseTree: warehouseTree,
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
