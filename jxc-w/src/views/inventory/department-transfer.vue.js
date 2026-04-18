/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { ArrowDown, ArrowUp, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
const timeTypeOptions = ['调拨日期', '创建时间'];
const documentStatusOptions = ['草稿', '已提交', '已审核'];
const reviewStatusOptions = ['全部', '未复审', '已复审'];
const printStatusOptions = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const departmentTree = [
    {
        value: 'dept-root',
        label: '部门中心',
        children: [
            { value: '中央厨房', label: '中央厨房' },
            { value: '热菜档口', label: '热菜档口' },
            { value: '饮品吧台', label: '饮品吧台' },
            { value: '冷菜间', label: '冷菜间' },
        ],
    },
];
const query = reactive({
    timeType: '调拨日期',
    dateRange: [],
    outboundDepartment: '',
    inboundDepartment: '',
    documentCode: '',
    itemName: '',
    documentStatus: '',
    reviewStatus: '全部',
    printStatus: '全部',
    remark: '',
});
const tableData = [
    {
        id: 1,
        documentCode: 'BMDB-202604-001',
        transferDate: '2026-04-13',
        outboundDepartment: '中央厨房',
        inboundDepartment: '热菜档口',
        amount: '3,860.00',
        documentStatus: '已审核',
        reviewStatus: '已复审',
        printStatus: '已打印',
        createdAt: '2026-04-13 09:18:00',
        creator: '张敏',
        remark: '午市菜品调拨',
    },
    {
        id: 2,
        documentCode: 'BMDB-202604-002',
        transferDate: '2026-04-12',
        outboundDepartment: '饮品吧台',
        inboundDepartment: '冷菜间',
        amount: '1,260.00',
        documentStatus: '已提交',
        reviewStatus: '未复审',
        printStatus: '未打印',
        createdAt: '2026-04-12 15:42:00',
        creator: '李娜',
        remark: '联动活动调拨',
    },
    {
        id: 3,
        documentCode: 'BMDB-202604-003',
        transferDate: '2026-04-11',
        outboundDepartment: '热菜档口',
        inboundDepartment: '中央厨房',
        amount: '920.00',
        documentStatus: '草稿',
        reviewStatus: '未复审',
        printStatus: '未打印',
        createdAt: '2026-04-11 10:36:00',
        creator: '王磊',
        remark: '物料回调',
    },
];
const filtersCollapsed = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref([]);
const filteredRows = computed(() => {
    const codeKeyword = query.documentCode.trim().toLowerCase();
    const remarkKeyword = query.remark.trim().toLowerCase();
    return tableData.filter((row) => {
        const dateField = query.timeType === '调拨日期' ? row.transferDate : row.createdAt.slice(0, 10);
        const matchedDateRange = query.dateRange.length !== 2 || (dateField >= query.dateRange[0] && dateField <= query.dateRange[1]);
        const matchedOutboundDepartment = !query.outboundDepartment || row.outboundDepartment === query.outboundDepartment;
        const matchedInboundDepartment = !query.inboundDepartment || row.inboundDepartment === query.inboundDepartment;
        const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
        const matchedItem = !query.itemName || row.remark.includes(query.itemName);
        const matchedDocumentStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
        const matchedReviewStatus = query.reviewStatus === '全部' || row.reviewStatus === query.reviewStatus;
        const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
        const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
        return matchedDateRange
            && matchedOutboundDepartment
            && matchedInboundDepartment
            && matchedDocumentCode
            && matchedItem
            && matchedDocumentStatus
            && matchedReviewStatus
            && matchedPrintStatus
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
    query.timeType = '调拨日期';
    query.dateRange = [];
    query.outboundDepartment = '';
    query.inboundDepartment = '';
    query.documentCode = '';
    query.itemName = '';
    query.documentStatus = '';
    query.reviewStatus = '全部';
    query.printStatus = '全部';
    query.remark = '';
    filtersCollapsed.value = true;
    currentPage.value = 1;
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
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
    label: "日期范围",
}));
const __VLS_17 = __VLS_16({
    label: "日期范围",
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
__VLS_18.slots.default;
const __VLS_19 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    modelValue: (__VLS_ctx.query.dateRange),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
    rangeSeparator: "~",
    startPlaceholder: "开始日期",
    endPlaceholder: "结束日期",
    ...{ style: {} },
}));
const __VLS_21 = __VLS_20({
    modelValue: (__VLS_ctx.query.dateRange),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
    rangeSeparator: "~",
    startPlaceholder: "开始日期",
    endPlaceholder: "结束日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
var __VLS_18;
const __VLS_23 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    label: "出库部门",
}));
const __VLS_25 = __VLS_24({
    label: "出库部门",
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
const __VLS_27 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    modelValue: (__VLS_ctx.query.outboundDepartment),
    data: (__VLS_ctx.departmentTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_29 = __VLS_28({
    modelValue: (__VLS_ctx.query.outboundDepartment),
    data: (__VLS_ctx.departmentTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
var __VLS_26;
const __VLS_31 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    label: "入库部门",
}));
const __VLS_33 = __VLS_32({
    label: "入库部门",
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
__VLS_34.slots.default;
const __VLS_35 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    modelValue: (__VLS_ctx.query.inboundDepartment),
    data: (__VLS_ctx.departmentTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_37 = __VLS_36({
    modelValue: (__VLS_ctx.query.inboundDepartment),
    data: (__VLS_ctx.departmentTree),
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
if (!__VLS_ctx.filtersCollapsed) {
    const __VLS_47 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({
        label: "物品",
    }));
    const __VLS_49 = __VLS_48({
        label: "物品",
    }, ...__VLS_functionalComponentArgsRest(__VLS_48));
    __VLS_50.slots.default;
    const __VLS_51 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
        modelValue: (__VLS_ctx.query.itemName),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_53 = __VLS_52({
        modelValue: (__VLS_ctx.query.itemName),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_52));
    __VLS_54.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
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
        label: "单据状态",
    }));
    const __VLS_61 = __VLS_60({
        label: "单据状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_60));
    __VLS_62.slots.default;
    const __VLS_63 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({
        modelValue: (__VLS_ctx.query.documentStatus),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_65 = __VLS_64({
        modelValue: (__VLS_ctx.query.documentStatus),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_64));
    __VLS_66.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.documentStatusOptions))) {
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
        label: "复审状态",
    }));
    const __VLS_73 = __VLS_72({
        label: "复审状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_72));
    __VLS_74.slots.default;
    const __VLS_75 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
        modelValue: (__VLS_ctx.query.reviewStatus),
        ...{ style: {} },
    }));
    const __VLS_77 = __VLS_76({
        modelValue: (__VLS_ctx.query.reviewStatus),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_76));
    __VLS_78.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.reviewStatusOptions))) {
        const __VLS_79 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_81 = __VLS_80({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_80));
    }
    var __VLS_78;
    var __VLS_74;
    const __VLS_83 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
        label: "打印状态",
    }));
    const __VLS_85 = __VLS_84({
        label: "打印状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_84));
    __VLS_86.slots.default;
    const __VLS_87 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({
        modelValue: (__VLS_ctx.query.printStatus),
        ...{ style: {} },
    }));
    const __VLS_89 = __VLS_88({
        modelValue: (__VLS_ctx.query.printStatus),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_88));
    __VLS_90.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.printStatusOptions))) {
        const __VLS_91 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_93 = __VLS_92({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_92));
    }
    var __VLS_90;
    var __VLS_86;
    const __VLS_95 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({
        label: "备注",
    }));
    const __VLS_97 = __VLS_96({
        label: "备注",
    }, ...__VLS_functionalComponentArgsRest(__VLS_96));
    __VLS_98.slots.default;
    const __VLS_99 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({
        modelValue: (__VLS_ctx.query.remark),
        placeholder: "请输入备注",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_101 = __VLS_100({
        modelValue: (__VLS_ctx.query.remark),
        placeholder: "请输入备注",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_100));
    var __VLS_98;
}
const __VLS_103 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({}));
const __VLS_105 = __VLS_104({}, ...__VLS_functionalComponentArgsRest(__VLS_104));
__VLS_106.slots.default;
const __VLS_107 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
    ...{ 'onClick': {} },
    text: true,
    type: "primary",
}));
const __VLS_109 = __VLS_108({
    ...{ 'onClick': {} },
    text: true,
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_108));
let __VLS_111;
let __VLS_112;
let __VLS_113;
const __VLS_114 = {
    onClick: (...[$event]) => {
        __VLS_ctx.filtersCollapsed = !__VLS_ctx.filtersCollapsed;
    }
};
__VLS_110.slots.default;
const __VLS_115 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({}));
const __VLS_117 = __VLS_116({}, ...__VLS_functionalComponentArgsRest(__VLS_116));
__VLS_118.slots.default;
if (__VLS_ctx.filtersCollapsed) {
    const __VLS_119 = {}.ArrowDown;
    /** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
    // @ts-ignore
    const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({}));
    const __VLS_121 = __VLS_120({}, ...__VLS_functionalComponentArgsRest(__VLS_120));
}
else {
    const __VLS_123 = {}.ArrowUp;
    /** @type {[typeof __VLS_components.ArrowUp, ]} */ ;
    // @ts-ignore
    const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({}));
    const __VLS_125 = __VLS_124({}, ...__VLS_functionalComponentArgsRest(__VLS_124));
}
var __VLS_118;
(__VLS_ctx.filtersCollapsed ? '展开筛选' : '收起筛选');
var __VLS_110;
const __VLS_127 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_129 = __VLS_128({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_128));
let __VLS_131;
let __VLS_132;
let __VLS_133;
const __VLS_134 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_130.slots.default;
const __VLS_135 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({}));
const __VLS_137 = __VLS_136({}, ...__VLS_functionalComponentArgsRest(__VLS_136));
__VLS_138.slots.default;
const __VLS_139 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
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
    onClick: (__VLS_ctx.handleReset)
};
__VLS_146.slots.default;
const __VLS_151 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({}));
const __VLS_153 = __VLS_152({}, ...__VLS_functionalComponentArgsRest(__VLS_152));
__VLS_154.slots.default;
const __VLS_155 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({}));
const __VLS_157 = __VLS_156({}, ...__VLS_functionalComponentArgsRest(__VLS_156));
var __VLS_154;
var __VLS_146;
var __VLS_106;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_159 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_161 = __VLS_160({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_160));
let __VLS_163;
let __VLS_164;
let __VLS_165;
const __VLS_166 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('新增');
    }
};
__VLS_162.slots.default;
const __VLS_167 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({}));
const __VLS_169 = __VLS_168({}, ...__VLS_functionalComponentArgsRest(__VLS_168));
__VLS_170.slots.default;
const __VLS_171 = {}.Plus;
/** @type {[typeof __VLS_components.Plus, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({}));
const __VLS_173 = __VLS_172({}, ...__VLS_functionalComponentArgsRest(__VLS_172));
var __VLS_170;
var __VLS_162;
const __VLS_175 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
    ...{ 'onClick': {} },
}));
const __VLS_177 = __VLS_176({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_176));
let __VLS_179;
let __VLS_180;
let __VLS_181;
const __VLS_182 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量打印');
    }
};
__VLS_178.slots.default;
const __VLS_183 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({}));
const __VLS_185 = __VLS_184({}, ...__VLS_functionalComponentArgsRest(__VLS_184));
__VLS_186.slots.default;
const __VLS_187 = {}.Printer;
/** @type {[typeof __VLS_components.Printer, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({}));
const __VLS_189 = __VLS_188({}, ...__VLS_functionalComponentArgsRest(__VLS_188));
var __VLS_186;
var __VLS_178;
const __VLS_191 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
    ...{ 'onClick': {} },
}));
const __VLS_193 = __VLS_192({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_192));
let __VLS_195;
let __VLS_196;
let __VLS_197;
const __VLS_198 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量删除');
    }
};
__VLS_194.slots.default;
const __VLS_199 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({}));
const __VLS_201 = __VLS_200({}, ...__VLS_functionalComponentArgsRest(__VLS_200));
__VLS_202.slots.default;
const __VLS_203 = {}.Delete;
/** @type {[typeof __VLS_components.Delete, ]} */ ;
// @ts-ignore
const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({}));
const __VLS_205 = __VLS_204({}, ...__VLS_functionalComponentArgsRest(__VLS_204));
var __VLS_202;
var __VLS_194;
const __VLS_207 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    ...{ 'onClick': {} },
}));
const __VLS_209 = __VLS_208({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
let __VLS_211;
let __VLS_212;
let __VLS_213;
const __VLS_214 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量提交');
    }
};
__VLS_210.slots.default;
var __VLS_210;
const __VLS_215 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
    ...{ 'onClick': {} },
}));
const __VLS_217 = __VLS_216({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_216));
let __VLS_219;
let __VLS_220;
let __VLS_221;
const __VLS_222 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量审核');
    }
};
__VLS_218.slots.default;
var __VLS_218;
const __VLS_223 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
    ...{ 'onClick': {} },
}));
const __VLS_225 = __VLS_224({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_224));
let __VLS_227;
let __VLS_228;
let __VLS_229;
const __VLS_230 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量反审核');
    }
};
__VLS_226.slots.default;
var __VLS_226;
const __VLS_231 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
    ...{ 'onClick': {} },
}));
const __VLS_233 = __VLS_232({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_232));
let __VLS_235;
let __VLS_236;
let __VLS_237;
const __VLS_238 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量复审');
    }
};
__VLS_234.slots.default;
var __VLS_234;
const __VLS_239 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    ...{ 'onClick': {} },
}));
const __VLS_241 = __VLS_240({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
let __VLS_243;
let __VLS_244;
let __VLS_245;
const __VLS_246 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量取消复审');
    }
};
__VLS_242.slots.default;
var __VLS_242;
const __VLS_247 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_249 = __VLS_248({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_248));
let __VLS_251;
let __VLS_252;
let __VLS_253;
const __VLS_254 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_250.slots.default;
const __VLS_255 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_257 = __VLS_256({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_256));
const __VLS_259 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_260 = __VLS_asFunctionalComponent(__VLS_259, new __VLS_259({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_261 = __VLS_260({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_260));
const __VLS_263 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_264 = __VLS_asFunctionalComponent(__VLS_263, new __VLS_263({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_265 = __VLS_264({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_264));
const __VLS_267 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_268 = __VLS_asFunctionalComponent(__VLS_267, new __VLS_267({
    prop: "transferDate",
    label: "调拨日期",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_269 = __VLS_268({
    prop: "transferDate",
    label: "调拨日期",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_268));
const __VLS_271 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_272 = __VLS_asFunctionalComponent(__VLS_271, new __VLS_271({
    prop: "outboundDepartment",
    label: "出库部门",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_273 = __VLS_272({
    prop: "outboundDepartment",
    label: "出库部门",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_272));
const __VLS_275 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_276 = __VLS_asFunctionalComponent(__VLS_275, new __VLS_275({
    prop: "inboundDepartment",
    label: "入库部门",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_277 = __VLS_276({
    prop: "inboundDepartment",
    label: "入库部门",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_276));
const __VLS_279 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_280 = __VLS_asFunctionalComponent(__VLS_279, new __VLS_279({
    prop: "amount",
    label: "金额",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_281 = __VLS_280({
    prop: "amount",
    label: "金额",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_280));
const __VLS_283 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_284 = __VLS_asFunctionalComponent(__VLS_283, new __VLS_283({
    prop: "documentStatus",
    label: "单据状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_285 = __VLS_284({
    prop: "documentStatus",
    label: "单据状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_284));
const __VLS_287 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_288 = __VLS_asFunctionalComponent(__VLS_287, new __VLS_287({
    prop: "reviewStatus",
    label: "复审状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_289 = __VLS_288({
    prop: "reviewStatus",
    label: "复审状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_288));
const __VLS_291 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_292 = __VLS_asFunctionalComponent(__VLS_291, new __VLS_291({
    prop: "printStatus",
    label: "打印状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_293 = __VLS_292({
    prop: "printStatus",
    label: "打印状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_292));
const __VLS_295 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_296 = __VLS_asFunctionalComponent(__VLS_295, new __VLS_295({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_297 = __VLS_296({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_296));
const __VLS_299 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_300 = __VLS_asFunctionalComponent(__VLS_299, new __VLS_299({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_301 = __VLS_300({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_300));
const __VLS_303 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_304 = __VLS_asFunctionalComponent(__VLS_303, new __VLS_303({
    prop: "remark",
    label: "备注",
    minWidth: "180",
    showOverflowTooltip: true,
}));
const __VLS_305 = __VLS_304({
    prop: "remark",
    label: "备注",
    minWidth: "180",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_304));
const __VLS_307 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_308 = __VLS_asFunctionalComponent(__VLS_307, new __VLS_307({
    label: "操作",
    width: "120",
    fixed: "right",
}));
const __VLS_309 = __VLS_308({
    label: "操作",
    width: "120",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_308));
__VLS_310.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_310.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_311 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_312 = __VLS_asFunctionalComponent(__VLS_311, new __VLS_311({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_313 = __VLS_312({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_312));
    let __VLS_315;
    let __VLS_316;
    let __VLS_317;
    const __VLS_318 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_314.slots.default;
    var __VLS_314;
    const __VLS_319 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_320 = __VLS_asFunctionalComponent(__VLS_319, new __VLS_319({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_321 = __VLS_320({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_320));
    let __VLS_323;
    let __VLS_324;
    let __VLS_325;
    const __VLS_326 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleEdit(row);
        }
    };
    __VLS_322.slots.default;
    var __VLS_322;
}
var __VLS_310;
var __VLS_250;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedIds.length);
const __VLS_327 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_328 = __VLS_asFunctionalComponent(__VLS_327, new __VLS_327({
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
const __VLS_329 = __VLS_328({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_328));
let __VLS_331;
let __VLS_332;
let __VLS_333;
const __VLS_334 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_335 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_330;
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
            ArrowUp: ArrowUp,
            Delete: Delete,
            Plus: Plus,
            Printer: Printer,
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            timeTypeOptions: timeTypeOptions,
            documentStatusOptions: documentStatusOptions,
            reviewStatusOptions: reviewStatusOptions,
            printStatusOptions: printStatusOptions,
            itemOptions: itemOptions,
            departmentTree: departmentTree,
            query: query,
            filtersCollapsed: filtersCollapsed,
            currentPage: currentPage,
            pageSize: pageSize,
            selectedIds: selectedIds,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
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
