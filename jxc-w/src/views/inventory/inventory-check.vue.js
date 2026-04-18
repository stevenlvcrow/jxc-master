/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
const timeTypeOptions = ['盘点日期', '创建时间'];
const documentStatusOptions = ['草稿', '已提交', '已审核'];
const printStatusOptions = ['全部', '未打印', '已打印'];
const diffStatusOptions = ['全部', '盘盈', '盘亏', '无差异'];
const checkTypeOptions = ['常规盘点', '抽盘', '循环盘点'];
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const itemTree = [
    {
        value: 'food',
        label: '食品',
        children: [
            { value: '鸡胸肉', label: '鸡胸肉' },
            { value: '牛腩', label: '牛腩' },
        ],
    },
    {
        value: 'pack',
        label: '包材',
        children: [
            { value: '包装盒', label: '包装盒' },
            { value: '封口膜', label: '封口膜' },
        ],
    },
];
const query = reactive({
    timeType: '盘点日期',
    startDate: '',
    endDate: '',
    documentCode: '',
    warehouse: '',
    diffStatus: '全部',
    itemName: '',
    documentStatus: '',
    checkType: '',
    printStatus: '全部',
    remark: '',
    planCode: '',
});
const tableData = [
    {
        id: 1,
        documentCode: 'PD-202604-001',
        checkDate: '2026-04-13',
        warehouse: '中央成品仓',
        itemCount: 128,
        bookAmount: '86,200.00',
        actualAmount: '86,980.00',
        diffAmount: '+780.00',
        checkType: '常规盘点',
        status: '已审核',
        diffStatus: '盘盈',
        auditDate: '2026-04-13',
        printStatus: '已打印',
        createdAt: '2026-04-13 10:18:00',
        creator: '张敏',
    },
    {
        id: 2,
        documentCode: 'PD-202604-002',
        checkDate: '2026-04-12',
        warehouse: '北区原料仓',
        itemCount: 86,
        bookAmount: '42,100.00',
        actualAmount: '41,620.00',
        diffAmount: '-480.00',
        checkType: '抽盘',
        status: '已提交',
        diffStatus: '盘亏',
        auditDate: '-',
        printStatus: '未打印',
        createdAt: '2026-04-12 16:40:00',
        creator: '李娜',
    },
    {
        id: 3,
        documentCode: 'PD-202604-003',
        checkDate: '2026-04-11',
        warehouse: '南区包材仓',
        itemCount: 42,
        bookAmount: '12,600.00',
        actualAmount: '12,600.00',
        diffAmount: '0.00',
        checkType: '循环盘点',
        status: '草稿',
        diffStatus: '无差异',
        auditDate: '-',
        printStatus: '未打印',
        createdAt: '2026-04-11 09:46:00',
        creator: '王磊',
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
const selectedIds = ref([]);
const filteredRows = computed(() => {
    const codeKeyword = query.documentCode.trim().toLowerCase();
    const remarkKeyword = query.remark.trim().toLowerCase();
    return tableData.filter((row) => {
        const dateField = query.timeType === '盘点日期' ? row.checkDate : row.createdAt.slice(0, 10);
        const matchedStartDate = !query.startDate || dateField >= query.startDate;
        const matchedEndDate = !query.endDate || dateField <= query.endDate;
        const matchedCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
        const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
        const matchedDiffStatus = query.diffStatus === '全部' || row.diffStatus === query.diffStatus;
        const matchedItem = !query.itemName || row.documentCode.includes(query.itemName);
        const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
        const matchedCheckType = !query.checkType || row.checkType === query.checkType;
        const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
        const matchedRemark = !remarkKeyword || row.documentCode.toLowerCase().includes(remarkKeyword);
        const matchedPlanCode = !query.planCode || row.documentCode.toLowerCase().includes(query.planCode.toLowerCase());
        return matchedStartDate
            && matchedEndDate
            && matchedCode
            && matchedWarehouse
            && matchedDiffStatus
            && matchedItem
            && matchedStatus
            && matchedCheckType
            && matchedPrintStatus
            && matchedRemark
            && matchedPlanCode;
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
    query.timeType = '盘点日期';
    query.startDate = '';
    query.endDate = '';
    query.documentCode = '';
    query.warehouse = '';
    query.diffStatus = '全部';
    query.itemName = '';
    query.documentStatus = '';
    query.checkType = '';
    query.printStatus = '全部';
    query.remark = '';
    query.planCode = '';
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
    label: "时间类型",
}));
const __VLS_5 = __VLS_4({
    label: "时间类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.timeType),
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.timeType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
__VLS_10.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.timeTypeOptions))) {
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
    label: "单据编号",
}));
const __VLS_33 = __VLS_32({
    label: "单据编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
__VLS_34.slots.default;
const __VLS_35 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    modelValue: (__VLS_ctx.query.documentCode),
    placeholder: "请输入单据编号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_37 = __VLS_36({
    modelValue: (__VLS_ctx.query.documentCode),
    placeholder: "请输入单据编号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
var __VLS_34;
const __VLS_39 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({
    label: "仓库",
}));
const __VLS_41 = __VLS_40({
    label: "仓库",
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
__VLS_42.slots.default;
const __VLS_43 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_45 = __VLS_44({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
var __VLS_42;
const __VLS_47 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({
    label: "盘点差异",
}));
const __VLS_49 = __VLS_48({
    label: "盘点差异",
}, ...__VLS_functionalComponentArgsRest(__VLS_48));
__VLS_50.slots.default;
const __VLS_51 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
    modelValue: (__VLS_ctx.query.diffStatus),
    ...{ style: {} },
}));
const __VLS_53 = __VLS_52({
    modelValue: (__VLS_ctx.query.diffStatus),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_52));
__VLS_54.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.diffStatusOptions))) {
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
    label: "单据状态",
}));
const __VLS_69 = __VLS_68({
    label: "单据状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_68));
__VLS_70.slots.default;
const __VLS_71 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
    modelValue: (__VLS_ctx.query.documentStatus),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_73 = __VLS_72({
    modelValue: (__VLS_ctx.query.documentStatus),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_72));
__VLS_74.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.documentStatusOptions))) {
    const __VLS_75 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_77 = __VLS_76({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_76));
}
var __VLS_74;
var __VLS_70;
const __VLS_79 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
    label: "盘点类型",
}));
const __VLS_81 = __VLS_80({
    label: "盘点类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_80));
__VLS_82.slots.default;
const __VLS_83 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
    modelValue: (__VLS_ctx.query.checkType),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_85 = __VLS_84({
    modelValue: (__VLS_ctx.query.checkType),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_84));
__VLS_86.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.checkTypeOptions))) {
    const __VLS_87 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_89 = __VLS_88({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_88));
}
var __VLS_86;
var __VLS_82;
const __VLS_91 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({
    label: "打印状态",
}));
const __VLS_93 = __VLS_92({
    label: "打印状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_92));
__VLS_94.slots.default;
const __VLS_95 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({
    modelValue: (__VLS_ctx.query.printStatus),
    ...{ style: {} },
}));
const __VLS_97 = __VLS_96({
    modelValue: (__VLS_ctx.query.printStatus),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_96));
__VLS_98.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.printStatusOptions))) {
    const __VLS_99 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_101 = __VLS_100({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_100));
}
var __VLS_98;
var __VLS_94;
const __VLS_103 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({
    label: "备注",
}));
const __VLS_105 = __VLS_104({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_104));
__VLS_106.slots.default;
const __VLS_107 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_109 = __VLS_108({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_108));
var __VLS_106;
const __VLS_111 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({
    label: "盘点方案编号",
}));
const __VLS_113 = __VLS_112({
    label: "盘点方案编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_112));
__VLS_114.slots.default;
const __VLS_115 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({
    modelValue: (__VLS_ctx.query.planCode),
    placeholder: "请输入盘点方案编号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_117 = __VLS_116({
    modelValue: (__VLS_ctx.query.planCode),
    placeholder: "请输入盘点方案编号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_116));
var __VLS_114;
const __VLS_119 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({}));
const __VLS_121 = __VLS_120({}, ...__VLS_functionalComponentArgsRest(__VLS_120));
__VLS_122.slots.default;
const __VLS_123 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_125 = __VLS_124({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_124));
let __VLS_127;
let __VLS_128;
let __VLS_129;
const __VLS_130 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_126.slots.default;
const __VLS_131 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({}));
const __VLS_133 = __VLS_132({}, ...__VLS_functionalComponentArgsRest(__VLS_132));
__VLS_134.slots.default;
const __VLS_135 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({}));
const __VLS_137 = __VLS_136({}, ...__VLS_functionalComponentArgsRest(__VLS_136));
var __VLS_134;
var __VLS_126;
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
    onClick: (__VLS_ctx.handleReset)
};
__VLS_142.slots.default;
const __VLS_147 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({}));
const __VLS_149 = __VLS_148({}, ...__VLS_functionalComponentArgsRest(__VLS_148));
__VLS_150.slots.default;
const __VLS_151 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({}));
const __VLS_153 = __VLS_152({}, ...__VLS_functionalComponentArgsRest(__VLS_152));
var __VLS_150;
var __VLS_142;
var __VLS_122;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
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
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('新增');
    }
};
__VLS_158.slots.default;
const __VLS_163 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({}));
const __VLS_165 = __VLS_164({}, ...__VLS_functionalComponentArgsRest(__VLS_164));
__VLS_166.slots.default;
const __VLS_167 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({}));
const __VLS_169 = __VLS_168({}, ...__VLS_functionalComponentArgsRest(__VLS_168));
var __VLS_166;
var __VLS_158;
const __VLS_171 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({
    ...{ 'onClick': {} },
}));
const __VLS_173 = __VLS_172({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_172));
let __VLS_175;
let __VLS_176;
let __VLS_177;
const __VLS_178 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量打印');
    }
};
__VLS_174.slots.default;
const __VLS_179 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({}));
const __VLS_181 = __VLS_180({}, ...__VLS_functionalComponentArgsRest(__VLS_180));
__VLS_182.slots.default;
const __VLS_183 = {}.Printer;
/** @type {[typeof __VLS_components.Printer, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({}));
const __VLS_185 = __VLS_184({}, ...__VLS_functionalComponentArgsRest(__VLS_184));
var __VLS_182;
var __VLS_174;
const __VLS_187 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({
    ...{ 'onClick': {} },
}));
const __VLS_189 = __VLS_188({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_188));
let __VLS_191;
let __VLS_192;
let __VLS_193;
const __VLS_194 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量删除');
    }
};
__VLS_190.slots.default;
const __VLS_195 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_196 = __VLS_asFunctionalComponent(__VLS_195, new __VLS_195({}));
const __VLS_197 = __VLS_196({}, ...__VLS_functionalComponentArgsRest(__VLS_196));
__VLS_198.slots.default;
const __VLS_199 = {}.Delete;
/** @type {[typeof __VLS_components.Delete, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({}));
const __VLS_201 = __VLS_200({}, ...__VLS_functionalComponentArgsRest(__VLS_200));
var __VLS_198;
var __VLS_190;
const __VLS_203 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
    ...{ 'onClick': {} },
}));
const __VLS_205 = __VLS_204({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_204));
let __VLS_207;
let __VLS_208;
let __VLS_209;
const __VLS_210 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量提交');
    }
};
__VLS_206.slots.default;
var __VLS_206;
const __VLS_211 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_212 = __VLS_asFunctionalComponent(__VLS_211, new __VLS_211({
    ...{ 'onClick': {} },
}));
const __VLS_213 = __VLS_212({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_212));
let __VLS_215;
let __VLS_216;
let __VLS_217;
const __VLS_218 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量审核');
    }
};
__VLS_214.slots.default;
var __VLS_214;
const __VLS_219 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({
    ...{ 'onClick': {} },
}));
const __VLS_221 = __VLS_220({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_220));
let __VLS_223;
let __VLS_224;
let __VLS_225;
const __VLS_226 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量反审核');
    }
};
__VLS_222.slots.default;
var __VLS_222;
const __VLS_227 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
    ...{ 'onClick': {} },
}));
const __VLS_229 = __VLS_228({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_228));
let __VLS_231;
let __VLS_232;
let __VLS_233;
const __VLS_234 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导出');
    }
};
__VLS_230.slots.default;
var __VLS_230;
const __VLS_235 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
    ...{ 'onClick': {} },
}));
const __VLS_237 = __VLS_236({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_236));
let __VLS_239;
let __VLS_240;
let __VLS_241;
const __VLS_242 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导出单据列表');
    }
};
__VLS_238.slots.default;
var __VLS_238;
const __VLS_243 = {}.ElDropdown;
/** @type {[typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, ]} */ ;
// @ts-ignore
const __VLS_244 = __VLS_asFunctionalComponent(__VLS_243, new __VLS_243({
    ...{ 'onCommand': {} },
}));
const __VLS_245 = __VLS_244({
    ...{ 'onCommand': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_244));
let __VLS_247;
let __VLS_248;
let __VLS_249;
const __VLS_250 = {
    onCommand: (__VLS_ctx.handleTableSettingCommand)
};
__VLS_246.slots.default;
const __VLS_251 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_252 = __VLS_asFunctionalComponent(__VLS_251, new __VLS_251({}));
const __VLS_253 = __VLS_252({}, ...__VLS_functionalComponentArgsRest(__VLS_252));
__VLS_254.slots.default;
const __VLS_255 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({}));
const __VLS_257 = __VLS_256({}, ...__VLS_functionalComponentArgsRest(__VLS_256));
__VLS_258.slots.default;
const __VLS_259 = {}.ArrowDown;
/** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
// @ts-ignore
const __VLS_260 = __VLS_asFunctionalComponent(__VLS_259, new __VLS_259({}));
const __VLS_261 = __VLS_260({}, ...__VLS_functionalComponentArgsRest(__VLS_260));
var __VLS_258;
var __VLS_254;
{
    const { dropdown: __VLS_thisSlot } = __VLS_246.slots;
    const __VLS_263 = {}.ElDropdownMenu;
    /** @type {[typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, ]} */ ;
    // @ts-ignore
    const __VLS_264 = __VLS_asFunctionalComponent(__VLS_263, new __VLS_263({}));
    const __VLS_265 = __VLS_264({}, ...__VLS_functionalComponentArgsRest(__VLS_264));
    __VLS_266.slots.default;
    const __VLS_267 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_268 = __VLS_asFunctionalComponent(__VLS_267, new __VLS_267({
        command: "column",
    }));
    const __VLS_269 = __VLS_268({
        command: "column",
    }, ...__VLS_functionalComponentArgsRest(__VLS_268));
    __VLS_270.slots.default;
    var __VLS_270;
    const __VLS_271 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_272 = __VLS_asFunctionalComponent(__VLS_271, new __VLS_271({
        command: "density",
    }));
    const __VLS_273 = __VLS_272({
        command: "density",
    }, ...__VLS_functionalComponentArgsRest(__VLS_272));
    __VLS_274.slots.default;
    var __VLS_274;
    var __VLS_266;
}
var __VLS_246;
const __VLS_275 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_276 = __VLS_asFunctionalComponent(__VLS_275, new __VLS_275({
    ...{ 'onClick': {} },
}));
const __VLS_277 = __VLS_276({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_276));
let __VLS_279;
let __VLS_280;
let __VLS_281;
const __VLS_282 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('操作日志');
    }
};
__VLS_278.slots.default;
var __VLS_278;
const __VLS_283 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_284 = __VLS_asFunctionalComponent(__VLS_283, new __VLS_283({
    ...{ 'onClick': {} },
}));
const __VLS_285 = __VLS_284({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_284));
let __VLS_287;
let __VLS_288;
let __VLS_289;
const __VLS_290 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('常见问题');
    }
};
__VLS_286.slots.default;
var __VLS_286;
const __VLS_291 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_292 = __VLS_asFunctionalComponent(__VLS_291, new __VLS_291({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_293 = __VLS_292({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_292));
let __VLS_295;
let __VLS_296;
let __VLS_297;
const __VLS_298 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_294.slots.default;
const __VLS_299 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_300 = __VLS_asFunctionalComponent(__VLS_299, new __VLS_299({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_301 = __VLS_300({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_300));
const __VLS_303 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_304 = __VLS_asFunctionalComponent(__VLS_303, new __VLS_303({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_305 = __VLS_304({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_304));
const __VLS_307 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_308 = __VLS_asFunctionalComponent(__VLS_307, new __VLS_307({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_309 = __VLS_308({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_308));
const __VLS_311 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_312 = __VLS_asFunctionalComponent(__VLS_311, new __VLS_311({
    prop: "checkDate",
    label: "盘点日期",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_313 = __VLS_312({
    prop: "checkDate",
    label: "盘点日期",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_312));
const __VLS_315 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_316 = __VLS_asFunctionalComponent(__VLS_315, new __VLS_315({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_317 = __VLS_316({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_316));
const __VLS_319 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_320 = __VLS_asFunctionalComponent(__VLS_319, new __VLS_319({
    prop: "itemCount",
    label: "物品数",
    minWidth: "90",
    showOverflowTooltip: true,
}));
const __VLS_321 = __VLS_320({
    prop: "itemCount",
    label: "物品数",
    minWidth: "90",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_320));
const __VLS_323 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_324 = __VLS_asFunctionalComponent(__VLS_323, new __VLS_323({
    prop: "bookAmount",
    label: "账面金额",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_325 = __VLS_324({
    prop: "bookAmount",
    label: "账面金额",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_324));
const __VLS_327 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_328 = __VLS_asFunctionalComponent(__VLS_327, new __VLS_327({
    prop: "actualAmount",
    label: "实盘金额",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_329 = __VLS_328({
    prop: "actualAmount",
    label: "实盘金额",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_328));
const __VLS_331 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_332 = __VLS_asFunctionalComponent(__VLS_331, new __VLS_331({
    prop: "diffAmount",
    label: "盈亏金额",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_333 = __VLS_332({
    prop: "diffAmount",
    label: "盈亏金额",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_332));
const __VLS_335 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_336 = __VLS_asFunctionalComponent(__VLS_335, new __VLS_335({
    prop: "checkType",
    label: "盘点类型",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_337 = __VLS_336({
    prop: "checkType",
    label: "盘点类型",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_336));
const __VLS_339 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_340 = __VLS_asFunctionalComponent(__VLS_339, new __VLS_339({
    prop: "status",
    label: "状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_341 = __VLS_340({
    prop: "status",
    label: "状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_340));
const __VLS_343 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_344 = __VLS_asFunctionalComponent(__VLS_343, new __VLS_343({
    prop: "diffStatus",
    label: "盘点差异",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_345 = __VLS_344({
    prop: "diffStatus",
    label: "盘点差异",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_344));
const __VLS_347 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_348 = __VLS_asFunctionalComponent(__VLS_347, new __VLS_347({
    prop: "auditDate",
    label: "审核日期",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_349 = __VLS_348({
    prop: "auditDate",
    label: "审核日期",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_348));
const __VLS_351 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_352 = __VLS_asFunctionalComponent(__VLS_351, new __VLS_351({
    prop: "printStatus",
    label: "打印状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_353 = __VLS_352({
    prop: "printStatus",
    label: "打印状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_352));
const __VLS_355 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_356 = __VLS_asFunctionalComponent(__VLS_355, new __VLS_355({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_357 = __VLS_356({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_356));
const __VLS_359 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_360 = __VLS_asFunctionalComponent(__VLS_359, new __VLS_359({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_361 = __VLS_360({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_360));
const __VLS_363 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_364 = __VLS_asFunctionalComponent(__VLS_363, new __VLS_363({
    label: "操作",
    width: "120",
    fixed: "right",
}));
const __VLS_365 = __VLS_364({
    label: "操作",
    width: "120",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_364));
__VLS_366.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_366.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_367 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_368 = __VLS_asFunctionalComponent(__VLS_367, new __VLS_367({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_369 = __VLS_368({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_368));
    let __VLS_371;
    let __VLS_372;
    let __VLS_373;
    const __VLS_374 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_370.slots.default;
    var __VLS_370;
    const __VLS_375 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_376 = __VLS_asFunctionalComponent(__VLS_375, new __VLS_375({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_377 = __VLS_376({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_376));
    let __VLS_379;
    let __VLS_380;
    let __VLS_381;
    const __VLS_382 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleEdit(row);
        }
    };
    __VLS_378.slots.default;
    var __VLS_378;
}
var __VLS_366;
var __VLS_294;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedIds.length);
const __VLS_383 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_384 = __VLS_asFunctionalComponent(__VLS_383, new __VLS_383({
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
const __VLS_385 = __VLS_384({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_384));
let __VLS_387;
let __VLS_388;
let __VLS_389;
const __VLS_390 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_391 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_386;
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
            Printer: Printer,
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            timeTypeOptions: timeTypeOptions,
            documentStatusOptions: documentStatusOptions,
            printStatusOptions: printStatusOptions,
            diffStatusOptions: diffStatusOptions,
            checkTypeOptions: checkTypeOptions,
            warehouseTree: warehouseTree,
            itemTree: itemTree,
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
