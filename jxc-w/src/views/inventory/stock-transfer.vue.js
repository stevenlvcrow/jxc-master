/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, ArrowUp, Delete, Plus, Printer, RefreshRight, Search, Upload, } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout from '@/components/PageTabsLayout.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
const tabs = [
    { key: 'document', label: '单据维度' },
    { key: 'item', label: '物品维度' },
];
const timeTypeOptions = ['移库日期', '创建时间'];
const documentStatusOptions = ['草稿', '已提交', '已审核'];
const transferTypeOptions = ['全部', '普通移库', '退库移库', '紧急调拨'];
const inboundStatusOptions = ['全部', '未入库', '部分入库', '已入库'];
const printStatusOptions = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const activeTab = ref('document');
const itemFiltersCollapsed = ref(false);
const documentCurrentPage = ref(1);
const itemCurrentPage = ref(1);
const pageSize = ref(10);
const documentSelectedIds = ref([]);
const itemSelectedIds = ref([]);
const documentQuery = reactive({
    timeType: '移库日期',
    dateRange: [],
    outboundWarehouse: '',
    inboundWarehouse: '',
    documentCode: '',
    itemName: '',
    documentStatus: '',
    transferType: '全部',
    inboundStatus: '全部',
    printStatus: '全部',
    remark: '',
});
const itemQuery = reactive({
    timeType: '移库日期',
    dateRange: [],
    outboundWarehouse: '',
    inboundWarehouse: '',
    documentCode: '',
    itemName: '',
    documentStatus: '',
    transferType: '全部',
    inboundStatus: '全部',
    printStatus: '全部',
    remark: '',
});
const documentRows = [
    {
        id: 1,
        documentCode: 'YK-202604-001',
        transferDate: '2026-04-13',
        outboundWarehouse: '北区原料仓',
        inboundWarehouse: '中央成品仓',
        amount: '8,620.00',
        documentStatus: '已审核',
        transferType: '普通移库',
        inboundStatus: '已入库',
        printStatus: '已打印',
        createdAt: '2026-04-13 09:28:00',
        creator: '张敏',
        remark: '中央厨房补货',
    },
    {
        id: 2,
        documentCode: 'YK-202604-002',
        transferDate: '2026-04-12',
        outboundWarehouse: '南区包材仓',
        inboundWarehouse: '东区备货仓',
        amount: '2,480.00',
        documentStatus: '已提交',
        transferType: '紧急调拨',
        inboundStatus: '部分入库',
        printStatus: '未打印',
        createdAt: '2026-04-12 14:35:00',
        creator: '李娜',
        remark: '外卖高峰备货',
    },
    {
        id: 3,
        documentCode: 'YK-202604-003',
        transferDate: '2026-04-11',
        outboundWarehouse: '中央成品仓',
        inboundWarehouse: '北区原料仓',
        amount: '1,960.00',
        documentStatus: '草稿',
        transferType: '退库移库',
        inboundStatus: '未入库',
        printStatus: '未打印',
        createdAt: '2026-04-11 10:08:00',
        creator: '王磊',
        remark: '半成品回库',
    },
];
const itemRows = [
    {
        id: 1,
        documentCode: 'YK-202604-001',
        transferDate: '2026-04-13',
        outboundWarehouse: '北区原料仓',
        inboundWarehouse: '中央成品仓',
        documentStatus: '已审核',
        transferType: '普通移库',
        inboundStatus: '已入库',
        printStatus: '已打印',
        itemCode: 'I0001',
        itemName: '鸡胸肉',
        spec: '2kg/袋',
        category: '生鲜原料',
        unit: '袋',
        quantity: '16',
        price: '186.00',
        amount: '2,976.00',
        createdAt: '2026-04-13 09:28:00',
        creator: '张敏',
        remark: '中央厨房补货',
    },
    {
        id: 2,
        documentCode: 'YK-202604-001',
        transferDate: '2026-04-13',
        outboundWarehouse: '北区原料仓',
        inboundWarehouse: '中央成品仓',
        documentStatus: '已审核',
        transferType: '普通移库',
        inboundStatus: '已入库',
        printStatus: '已打印',
        itemCode: 'I0002',
        itemName: '牛腩',
        spec: '1kg/袋',
        category: '生鲜原料',
        unit: '袋',
        quantity: '8',
        price: '218.00',
        amount: '1,744.00',
        createdAt: '2026-04-13 09:28:00',
        creator: '张敏',
        remark: '中央厨房补货',
    },
    {
        id: 3,
        documentCode: 'YK-202604-002',
        transferDate: '2026-04-12',
        outboundWarehouse: '南区包材仓',
        inboundWarehouse: '东区备货仓',
        documentStatus: '已提交',
        transferType: '紧急调拨',
        inboundStatus: '部分入库',
        printStatus: '未打印',
        itemCode: 'I0101',
        itemName: '包装盒',
        spec: '1000ml',
        category: '包材',
        unit: '箱',
        quantity: '12',
        price: '68.00',
        amount: '816.00',
        createdAt: '2026-04-12 14:35:00',
        creator: '李娜',
        remark: '外卖高峰备货',
    },
    {
        id: 4,
        documentCode: 'YK-202604-003',
        transferDate: '2026-04-11',
        outboundWarehouse: '中央成品仓',
        inboundWarehouse: '北区原料仓',
        documentStatus: '草稿',
        transferType: '退库移库',
        inboundStatus: '未入库',
        printStatus: '未打印',
        itemCode: 'I0203',
        itemName: '酸梅汤',
        spec: '1L/瓶',
        category: '饮品',
        unit: '瓶',
        quantity: '10',
        price: '15.60',
        amount: '156.00',
        createdAt: '2026-04-11 10:08:00',
        creator: '王磊',
        remark: '半成品回库',
    },
];
onMounted(() => {
    void loadWarehouseTree();
});
watch(() => sessionStore.currentOrgId, () => {
    void loadWarehouseTree();
});
const matchesDateRange = (dateRange, transferDate, createdAt, timeType) => {
    if (dateRange.length !== 2) {
        return true;
    }
    const current = timeType === '移库日期' ? transferDate : createdAt;
    return current >= dateRange[0] && current <= dateRange[1];
};
const documentFilteredRows = computed(() => {
    const codeKeyword = documentQuery.documentCode.trim().toLowerCase();
    const remarkKeyword = documentQuery.remark.trim().toLowerCase();
    return documentRows.filter((row) => {
        const matchedDateRange = matchesDateRange(documentQuery.dateRange, row.transferDate, row.createdAt.slice(0, 10), documentQuery.timeType);
        const matchedOutboundWarehouse = !documentQuery.outboundWarehouse || row.outboundWarehouse === documentQuery.outboundWarehouse;
        const matchedInboundWarehouse = !documentQuery.inboundWarehouse || row.inboundWarehouse === documentQuery.inboundWarehouse;
        const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
        const matchedItem = !documentQuery.itemName || row.remark.includes(documentQuery.itemName);
        const matchedDocumentStatus = !documentQuery.documentStatus || row.documentStatus === documentQuery.documentStatus;
        const matchedTransferType = documentQuery.transferType === '全部' || row.transferType === documentQuery.transferType;
        const matchedInboundStatus = documentQuery.inboundStatus === '全部' || row.inboundStatus === documentQuery.inboundStatus;
        const matchedPrintStatus = documentQuery.printStatus === '全部' || row.printStatus === documentQuery.printStatus;
        const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
        return matchedDateRange
            && matchedOutboundWarehouse
            && matchedInboundWarehouse
            && matchedDocumentCode
            && matchedItem
            && matchedDocumentStatus
            && matchedTransferType
            && matchedInboundStatus
            && matchedPrintStatus
            && matchedRemark;
    });
});
const itemFilteredRows = computed(() => {
    const codeKeyword = itemQuery.documentCode.trim().toLowerCase();
    const remarkKeyword = itemQuery.remark.trim().toLowerCase();
    return itemRows.filter((row) => {
        const matchedDateRange = matchesDateRange(itemQuery.dateRange, row.transferDate, row.createdAt.slice(0, 10), itemQuery.timeType);
        const matchedOutboundWarehouse = !itemQuery.outboundWarehouse || row.outboundWarehouse === itemQuery.outboundWarehouse;
        const matchedInboundWarehouse = !itemQuery.inboundWarehouse || row.inboundWarehouse === itemQuery.inboundWarehouse;
        const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
        const matchedItem = !itemQuery.itemName || row.itemName === itemQuery.itemName;
        const matchedDocumentStatus = !itemQuery.documentStatus || row.documentStatus === itemQuery.documentStatus;
        const matchedTransferType = itemQuery.transferType === '全部' || row.transferType === itemQuery.transferType;
        const matchedInboundStatus = itemQuery.inboundStatus === '全部' || row.inboundStatus === itemQuery.inboundStatus;
        const matchedPrintStatus = itemQuery.printStatus === '全部' || row.printStatus === itemQuery.printStatus;
        const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
        return matchedDateRange
            && matchedOutboundWarehouse
            && matchedInboundWarehouse
            && matchedDocumentCode
            && matchedItem
            && matchedDocumentStatus
            && matchedTransferType
            && matchedInboundStatus
            && matchedPrintStatus
            && matchedRemark;
    });
});
const documentPagedRows = computed(() => {
    const start = (documentCurrentPage.value - 1) * pageSize.value;
    return documentFilteredRows.value.slice(start, start + pageSize.value);
});
const itemPagedRows = computed(() => {
    const start = (itemCurrentPage.value - 1) * pageSize.value;
    return itemFilteredRows.value.slice(start, start + pageSize.value);
});
const handleDocumentSearch = () => {
    documentCurrentPage.value = 1;
};
const handleItemSearch = () => {
    itemCurrentPage.value = 1;
};
const handleDocumentReset = () => {
    documentQuery.timeType = '移库日期';
    documentQuery.dateRange = [];
    documentQuery.outboundWarehouse = '';
    documentQuery.inboundWarehouse = '';
    documentQuery.documentCode = '';
    documentQuery.itemName = '';
    documentQuery.documentStatus = '';
    documentQuery.transferType = '全部';
    documentQuery.inboundStatus = '全部';
    documentQuery.printStatus = '全部';
    documentQuery.remark = '';
    documentCurrentPage.value = 1;
};
const handleItemReset = () => {
    itemQuery.timeType = '移库日期';
    itemQuery.dateRange = [];
    itemQuery.outboundWarehouse = '';
    itemQuery.inboundWarehouse = '';
    itemQuery.documentCode = '';
    itemQuery.itemName = '';
    itemQuery.documentStatus = '';
    itemQuery.transferType = '全部';
    itemQuery.inboundStatus = '全部';
    itemQuery.printStatus = '全部';
    itemQuery.remark = '';
    itemFiltersCollapsed.value = false;
    itemCurrentPage.value = 1;
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
};
const handleDocumentSelectionChange = (rows) => {
    documentSelectedIds.value = rows.map((row) => row.id);
};
const handleItemSelectionChange = (rows) => {
    itemSelectedIds.value = rows.map((row) => row.id);
};
const handleDocumentPageChange = (page) => {
    documentCurrentPage.value = page;
};
const handleItemPageChange = (page) => {
    itemCurrentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    documentCurrentPage.value = 1;
    itemCurrentPage.value = 1;
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel stock-transfer-page" },
});
/** @type {[typeof PageTabsLayout, typeof PageTabsLayout, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(PageTabsLayout, new PageTabsLayout({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
    bodyClass: "stock-transfer-page__body",
}));
const __VLS_1 = __VLS_0({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
    bodyClass: "stock-transfer-page__body",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
if (__VLS_ctx.activeTab === 'document') {
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_3 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.documentQuery),
    }));
    const __VLS_4 = __VLS_3({
        model: (__VLS_ctx.documentQuery),
    }, ...__VLS_functionalComponentArgsRest(__VLS_3));
    __VLS_5.slots.default;
    const __VLS_6 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(__VLS_6, new __VLS_6({
        label: "时间类型",
    }));
    const __VLS_8 = __VLS_7({
        label: "时间类型",
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    __VLS_9.slots.default;
    const __VLS_10 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
        modelValue: (__VLS_ctx.documentQuery.timeType),
        ...{ style: {} },
    }));
    const __VLS_12 = __VLS_11({
        modelValue: (__VLS_ctx.documentQuery.timeType),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_11));
    __VLS_13.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.timeTypeOptions))) {
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
        label: "日期范围",
    }));
    const __VLS_20 = __VLS_19({
        label: "日期范围",
    }, ...__VLS_functionalComponentArgsRest(__VLS_19));
    __VLS_21.slots.default;
    const __VLS_22 = {}.ElDatePicker;
    /** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
    // @ts-ignore
    const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
        modelValue: (__VLS_ctx.documentQuery.dateRange),
        type: "daterange",
        valueFormat: "YYYY-MM-DD",
        rangeSeparator: "~",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
        ...{ style: {} },
    }));
    const __VLS_24 = __VLS_23({
        modelValue: (__VLS_ctx.documentQuery.dateRange),
        type: "daterange",
        valueFormat: "YYYY-MM-DD",
        rangeSeparator: "~",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_23));
    var __VLS_21;
    const __VLS_26 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
        label: "出库仓库",
    }));
    const __VLS_28 = __VLS_27({
        label: "出库仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_27));
    __VLS_29.slots.default;
    const __VLS_30 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
        modelValue: (__VLS_ctx.documentQuery.outboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }));
    const __VLS_32 = __VLS_31({
        modelValue: (__VLS_ctx.documentQuery.outboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_31));
    var __VLS_29;
    const __VLS_34 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
        label: "入库仓库",
    }));
    const __VLS_36 = __VLS_35({
        label: "入库仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_35));
    __VLS_37.slots.default;
    const __VLS_38 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
        modelValue: (__VLS_ctx.documentQuery.inboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }));
    const __VLS_40 = __VLS_39({
        modelValue: (__VLS_ctx.documentQuery.inboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_39));
    var __VLS_37;
    const __VLS_42 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
        label: "单据编号",
    }));
    const __VLS_44 = __VLS_43({
        label: "单据编号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_43));
    __VLS_45.slots.default;
    const __VLS_46 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
        modelValue: (__VLS_ctx.documentQuery.documentCode),
        placeholder: "请输入单据编号",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_48 = __VLS_47({
        modelValue: (__VLS_ctx.documentQuery.documentCode),
        placeholder: "请输入单据编号",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_47));
    var __VLS_45;
    const __VLS_50 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
        label: "物品",
    }));
    const __VLS_52 = __VLS_51({
        label: "物品",
    }, ...__VLS_functionalComponentArgsRest(__VLS_51));
    __VLS_53.slots.default;
    const __VLS_54 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
        modelValue: (__VLS_ctx.documentQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_56 = __VLS_55({
        modelValue: (__VLS_ctx.documentQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_55));
    __VLS_57.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
        const __VLS_58 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_60 = __VLS_59({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_59));
    }
    var __VLS_57;
    var __VLS_53;
    const __VLS_62 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
        label: "单据状态",
    }));
    const __VLS_64 = __VLS_63({
        label: "单据状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_63));
    __VLS_65.slots.default;
    const __VLS_66 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
        modelValue: (__VLS_ctx.documentQuery.documentStatus),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_68 = __VLS_67({
        modelValue: (__VLS_ctx.documentQuery.documentStatus),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_67));
    __VLS_69.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.documentStatusOptions))) {
        const __VLS_70 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_72 = __VLS_71({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_71));
    }
    var __VLS_69;
    var __VLS_65;
    const __VLS_74 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
        label: "移库类型",
    }));
    const __VLS_76 = __VLS_75({
        label: "移库类型",
    }, ...__VLS_functionalComponentArgsRest(__VLS_75));
    __VLS_77.slots.default;
    const __VLS_78 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
        modelValue: (__VLS_ctx.documentQuery.transferType),
        ...{ style: {} },
    }));
    const __VLS_80 = __VLS_79({
        modelValue: (__VLS_ctx.documentQuery.transferType),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_79));
    __VLS_81.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.transferTypeOptions))) {
        const __VLS_82 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_84 = __VLS_83({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_83));
    }
    var __VLS_81;
    var __VLS_77;
    const __VLS_86 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
        label: "入库状态",
    }));
    const __VLS_88 = __VLS_87({
        label: "入库状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_87));
    __VLS_89.slots.default;
    const __VLS_90 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
        modelValue: (__VLS_ctx.documentQuery.inboundStatus),
        ...{ style: {} },
    }));
    const __VLS_92 = __VLS_91({
        modelValue: (__VLS_ctx.documentQuery.inboundStatus),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_91));
    __VLS_93.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.inboundStatusOptions))) {
        const __VLS_94 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_96 = __VLS_95({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_95));
    }
    var __VLS_93;
    var __VLS_89;
    const __VLS_98 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
        label: "打印状态",
    }));
    const __VLS_100 = __VLS_99({
        label: "打印状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_99));
    __VLS_101.slots.default;
    const __VLS_102 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
        modelValue: (__VLS_ctx.documentQuery.printStatus),
        ...{ style: {} },
    }));
    const __VLS_104 = __VLS_103({
        modelValue: (__VLS_ctx.documentQuery.printStatus),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_103));
    __VLS_105.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.printStatusOptions))) {
        const __VLS_106 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_108 = __VLS_107({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_107));
    }
    var __VLS_105;
    var __VLS_101;
    const __VLS_110 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
        label: "备注",
    }));
    const __VLS_112 = __VLS_111({
        label: "备注",
    }, ...__VLS_functionalComponentArgsRest(__VLS_111));
    __VLS_113.slots.default;
    const __VLS_114 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
        modelValue: (__VLS_ctx.documentQuery.remark),
        placeholder: "请输入备注",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_116 = __VLS_115({
        modelValue: (__VLS_ctx.documentQuery.remark),
        placeholder: "请输入备注",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_115));
    var __VLS_113;
    const __VLS_118 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({}));
    const __VLS_120 = __VLS_119({}, ...__VLS_functionalComponentArgsRest(__VLS_119));
    __VLS_121.slots.default;
    const __VLS_122 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_124 = __VLS_123({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_123));
    let __VLS_126;
    let __VLS_127;
    let __VLS_128;
    const __VLS_129 = {
        onClick: (__VLS_ctx.handleDocumentSearch)
    };
    __VLS_125.slots.default;
    const __VLS_130 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({}));
    const __VLS_132 = __VLS_131({}, ...__VLS_functionalComponentArgsRest(__VLS_131));
    __VLS_133.slots.default;
    const __VLS_134 = {}.Search;
    /** @type {[typeof __VLS_components.Search, ]} */ ;
    // @ts-ignore
    const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({}));
    const __VLS_136 = __VLS_135({}, ...__VLS_functionalComponentArgsRest(__VLS_135));
    var __VLS_133;
    var __VLS_125;
    const __VLS_138 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
        ...{ 'onClick': {} },
    }));
    const __VLS_140 = __VLS_139({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_139));
    let __VLS_142;
    let __VLS_143;
    let __VLS_144;
    const __VLS_145 = {
        onClick: (__VLS_ctx.handleDocumentReset)
    };
    __VLS_141.slots.default;
    const __VLS_146 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({}));
    const __VLS_148 = __VLS_147({}, ...__VLS_functionalComponentArgsRest(__VLS_147));
    __VLS_149.slots.default;
    const __VLS_150 = {}.RefreshRight;
    /** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({}));
    const __VLS_152 = __VLS_151({}, ...__VLS_functionalComponentArgsRest(__VLS_151));
    var __VLS_149;
    var __VLS_141;
    var __VLS_121;
    var __VLS_5;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-toolbar" },
    });
    const __VLS_154 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_156 = __VLS_155({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_155));
    let __VLS_158;
    let __VLS_159;
    let __VLS_160;
    const __VLS_161 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('新增');
        }
    };
    __VLS_157.slots.default;
    const __VLS_162 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({}));
    const __VLS_164 = __VLS_163({}, ...__VLS_functionalComponentArgsRest(__VLS_163));
    __VLS_165.slots.default;
    const __VLS_166 = {}.Plus;
    /** @type {[typeof __VLS_components.Plus, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({}));
    const __VLS_168 = __VLS_167({}, ...__VLS_functionalComponentArgsRest(__VLS_167));
    var __VLS_165;
    var __VLS_157;
    const __VLS_170 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
        ...{ 'onClick': {} },
    }));
    const __VLS_172 = __VLS_171({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_171));
    let __VLS_174;
    let __VLS_175;
    let __VLS_176;
    const __VLS_177 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量打印');
        }
    };
    __VLS_173.slots.default;
    const __VLS_178 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({}));
    const __VLS_180 = __VLS_179({}, ...__VLS_functionalComponentArgsRest(__VLS_179));
    __VLS_181.slots.default;
    const __VLS_182 = {}.Printer;
    /** @type {[typeof __VLS_components.Printer, ]} */ ;
    // @ts-ignore
    const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({}));
    const __VLS_184 = __VLS_183({}, ...__VLS_functionalComponentArgsRest(__VLS_183));
    var __VLS_181;
    var __VLS_173;
    const __VLS_186 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
        ...{ 'onClick': {} },
    }));
    const __VLS_188 = __VLS_187({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_187));
    let __VLS_190;
    let __VLS_191;
    let __VLS_192;
    const __VLS_193 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量删除');
        }
    };
    __VLS_189.slots.default;
    const __VLS_194 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_195 = __VLS_asFunctionalComponent(__VLS_194, new __VLS_194({}));
    const __VLS_196 = __VLS_195({}, ...__VLS_functionalComponentArgsRest(__VLS_195));
    __VLS_197.slots.default;
    const __VLS_198 = {}.Delete;
    /** @type {[typeof __VLS_components.Delete, ]} */ ;
    // @ts-ignore
    const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({}));
    const __VLS_200 = __VLS_199({}, ...__VLS_functionalComponentArgsRest(__VLS_199));
    var __VLS_197;
    var __VLS_189;
    const __VLS_202 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
        ...{ 'onClick': {} },
    }));
    const __VLS_204 = __VLS_203({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_203));
    let __VLS_206;
    let __VLS_207;
    let __VLS_208;
    const __VLS_209 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量提交');
        }
    };
    __VLS_205.slots.default;
    var __VLS_205;
    const __VLS_210 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
        ...{ 'onClick': {} },
    }));
    const __VLS_212 = __VLS_211({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_211));
    let __VLS_214;
    let __VLS_215;
    let __VLS_216;
    const __VLS_217 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量审核');
        }
    };
    __VLS_213.slots.default;
    var __VLS_213;
    const __VLS_218 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
        ...{ 'onClick': {} },
    }));
    const __VLS_220 = __VLS_219({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_219));
    let __VLS_222;
    let __VLS_223;
    let __VLS_224;
    const __VLS_225 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量反审核');
        }
    };
    __VLS_221.slots.default;
    var __VLS_221;
    const __VLS_226 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({
        ...{ 'onClick': {} },
    }));
    const __VLS_228 = __VLS_227({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_227));
    let __VLS_230;
    let __VLS_231;
    let __VLS_232;
    const __VLS_233 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量导入');
        }
    };
    __VLS_229.slots.default;
    const __VLS_234 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_235 = __VLS_asFunctionalComponent(__VLS_234, new __VLS_234({}));
    const __VLS_236 = __VLS_235({}, ...__VLS_functionalComponentArgsRest(__VLS_235));
    __VLS_237.slots.default;
    const __VLS_238 = {}.Upload;
    /** @type {[typeof __VLS_components.Upload, ]} */ ;
    // @ts-ignore
    const __VLS_239 = __VLS_asFunctionalComponent(__VLS_238, new __VLS_238({}));
    const __VLS_240 = __VLS_239({}, ...__VLS_functionalComponentArgsRest(__VLS_239));
    var __VLS_237;
    var __VLS_229;
    const __VLS_242 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_243 = __VLS_asFunctionalComponent(__VLS_242, new __VLS_242({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.documentPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_244 = __VLS_243({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.documentPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_243));
    let __VLS_246;
    let __VLS_247;
    let __VLS_248;
    const __VLS_249 = {
        onSelectionChange: (__VLS_ctx.handleDocumentSelectionChange)
    };
    __VLS_245.slots.default;
    const __VLS_250 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_251 = __VLS_asFunctionalComponent(__VLS_250, new __VLS_250({
        type: "selection",
        width: "44",
        fixed: "left",
    }));
    const __VLS_252 = __VLS_251({
        type: "selection",
        width: "44",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_251));
    const __VLS_254 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_255 = __VLS_asFunctionalComponent(__VLS_254, new __VLS_254({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }));
    const __VLS_256 = __VLS_255({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_255));
    const __VLS_258 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_259 = __VLS_asFunctionalComponent(__VLS_258, new __VLS_258({
        prop: "documentCode",
        label: "单据编号",
        minWidth: "150",
        showOverflowTooltip: true,
    }));
    const __VLS_260 = __VLS_259({
        prop: "documentCode",
        label: "单据编号",
        minWidth: "150",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_259));
    const __VLS_262 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_263 = __VLS_asFunctionalComponent(__VLS_262, new __VLS_262({
        prop: "transferDate",
        label: "移库日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }));
    const __VLS_264 = __VLS_263({
        prop: "transferDate",
        label: "移库日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_263));
    const __VLS_266 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_267 = __VLS_asFunctionalComponent(__VLS_266, new __VLS_266({
        prop: "outboundWarehouse",
        label: "出库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_268 = __VLS_267({
        prop: "outboundWarehouse",
        label: "出库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_267));
    const __VLS_270 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_271 = __VLS_asFunctionalComponent(__VLS_270, new __VLS_270({
        prop: "inboundWarehouse",
        label: "入库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_272 = __VLS_271({
        prop: "inboundWarehouse",
        label: "入库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_271));
    const __VLS_274 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_275 = __VLS_asFunctionalComponent(__VLS_274, new __VLS_274({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_276 = __VLS_275({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_275));
    const __VLS_278 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_279 = __VLS_asFunctionalComponent(__VLS_278, new __VLS_278({
        prop: "documentStatus",
        label: "单据状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_280 = __VLS_279({
        prop: "documentStatus",
        label: "单据状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_279));
    const __VLS_282 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_283 = __VLS_asFunctionalComponent(__VLS_282, new __VLS_282({
        prop: "transferType",
        label: "移库类型",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_284 = __VLS_283({
        prop: "transferType",
        label: "移库类型",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_283));
    const __VLS_286 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_287 = __VLS_asFunctionalComponent(__VLS_286, new __VLS_286({
        prop: "printStatus",
        label: "打印状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_288 = __VLS_287({
        prop: "printStatus",
        label: "打印状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_287));
    const __VLS_290 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_291 = __VLS_asFunctionalComponent(__VLS_290, new __VLS_290({
        prop: "createdAt",
        label: "创建时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }));
    const __VLS_292 = __VLS_291({
        prop: "createdAt",
        label: "创建时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_291));
    const __VLS_294 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_295 = __VLS_asFunctionalComponent(__VLS_294, new __VLS_294({
        prop: "creator",
        label: "创建人",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_296 = __VLS_295({
        prop: "creator",
        label: "创建人",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_295));
    const __VLS_298 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_299 = __VLS_asFunctionalComponent(__VLS_298, new __VLS_298({
        prop: "remark",
        label: "备注",
        minWidth: "160",
        showOverflowTooltip: true,
    }));
    const __VLS_300 = __VLS_299({
        prop: "remark",
        label: "备注",
        minWidth: "160",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_299));
    const __VLS_302 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_303 = __VLS_asFunctionalComponent(__VLS_302, new __VLS_302({
        label: "操作",
        width: "120",
        fixed: "right",
    }));
    const __VLS_304 = __VLS_303({
        label: "操作",
        width: "120",
        fixed: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_303));
    __VLS_305.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_305.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_306 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_307 = __VLS_asFunctionalComponent(__VLS_306, new __VLS_306({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }));
        const __VLS_308 = __VLS_307({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }, ...__VLS_functionalComponentArgsRest(__VLS_307));
        let __VLS_310;
        let __VLS_311;
        let __VLS_312;
        const __VLS_313 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.activeTab === 'document'))
                    return;
                __VLS_ctx.handleToolbarAction(`查看：${row.documentCode}`);
            }
        };
        __VLS_309.slots.default;
        var __VLS_309;
        const __VLS_314 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_315 = __VLS_asFunctionalComponent(__VLS_314, new __VLS_314({
            ...{ 'onClick': {} },
            text: true,
        }));
        const __VLS_316 = __VLS_315({
            ...{ 'onClick': {} },
            text: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_315));
        let __VLS_318;
        let __VLS_319;
        let __VLS_320;
        const __VLS_321 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.activeTab === 'document'))
                    return;
                __VLS_ctx.handleToolbarAction(`编辑：${row.documentCode}`);
            }
        };
        __VLS_317.slots.default;
        var __VLS_317;
    }
    var __VLS_305;
    var __VLS_245;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.documentSelectedIds.length);
    const __VLS_322 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_323 = __VLS_asFunctionalComponent(__VLS_322, new __VLS_322({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.documentCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.documentFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }));
    const __VLS_324 = __VLS_323({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.documentCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.documentFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_323));
    let __VLS_326;
    let __VLS_327;
    let __VLS_328;
    const __VLS_329 = {
        onCurrentChange: (__VLS_ctx.handleDocumentPageChange)
    };
    const __VLS_330 = {
        onSizeChange: (__VLS_ctx.handlePageSizeChange)
    };
    var __VLS_325;
}
else {
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_331 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.itemQuery),
    }));
    const __VLS_332 = __VLS_331({
        model: (__VLS_ctx.itemQuery),
    }, ...__VLS_functionalComponentArgsRest(__VLS_331));
    __VLS_333.slots.default;
    const __VLS_334 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_335 = __VLS_asFunctionalComponent(__VLS_334, new __VLS_334({
        label: "时间类型",
    }));
    const __VLS_336 = __VLS_335({
        label: "时间类型",
    }, ...__VLS_functionalComponentArgsRest(__VLS_335));
    __VLS_337.slots.default;
    const __VLS_338 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_339 = __VLS_asFunctionalComponent(__VLS_338, new __VLS_338({
        modelValue: (__VLS_ctx.itemQuery.timeType),
        ...{ style: {} },
    }));
    const __VLS_340 = __VLS_339({
        modelValue: (__VLS_ctx.itemQuery.timeType),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_339));
    __VLS_341.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.timeTypeOptions))) {
        const __VLS_342 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_343 = __VLS_asFunctionalComponent(__VLS_342, new __VLS_342({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_344 = __VLS_343({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_343));
    }
    var __VLS_341;
    var __VLS_337;
    const __VLS_346 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_347 = __VLS_asFunctionalComponent(__VLS_346, new __VLS_346({
        label: "日期范围",
    }));
    const __VLS_348 = __VLS_347({
        label: "日期范围",
    }, ...__VLS_functionalComponentArgsRest(__VLS_347));
    __VLS_349.slots.default;
    const __VLS_350 = {}.ElDatePicker;
    /** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
    // @ts-ignore
    const __VLS_351 = __VLS_asFunctionalComponent(__VLS_350, new __VLS_350({
        modelValue: (__VLS_ctx.itemQuery.dateRange),
        type: "daterange",
        valueFormat: "YYYY-MM-DD",
        rangeSeparator: "~",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
        ...{ style: {} },
    }));
    const __VLS_352 = __VLS_351({
        modelValue: (__VLS_ctx.itemQuery.dateRange),
        type: "daterange",
        valueFormat: "YYYY-MM-DD",
        rangeSeparator: "~",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_351));
    var __VLS_349;
    const __VLS_354 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_355 = __VLS_asFunctionalComponent(__VLS_354, new __VLS_354({
        label: "出库仓库",
    }));
    const __VLS_356 = __VLS_355({
        label: "出库仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_355));
    __VLS_357.slots.default;
    const __VLS_358 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_359 = __VLS_asFunctionalComponent(__VLS_358, new __VLS_358({
        modelValue: (__VLS_ctx.itemQuery.outboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }));
    const __VLS_360 = __VLS_359({
        modelValue: (__VLS_ctx.itemQuery.outboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_359));
    var __VLS_357;
    const __VLS_362 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_363 = __VLS_asFunctionalComponent(__VLS_362, new __VLS_362({
        label: "入库仓库",
    }));
    const __VLS_364 = __VLS_363({
        label: "入库仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_363));
    __VLS_365.slots.default;
    const __VLS_366 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_367 = __VLS_asFunctionalComponent(__VLS_366, new __VLS_366({
        modelValue: (__VLS_ctx.itemQuery.inboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }));
    const __VLS_368 = __VLS_367({
        modelValue: (__VLS_ctx.itemQuery.inboundWarehouse),
        data: (__VLS_ctx.warehouseTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_367));
    var __VLS_365;
    const __VLS_370 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_371 = __VLS_asFunctionalComponent(__VLS_370, new __VLS_370({
        label: "单据编号",
    }));
    const __VLS_372 = __VLS_371({
        label: "单据编号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_371));
    __VLS_373.slots.default;
    const __VLS_374 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_375 = __VLS_asFunctionalComponent(__VLS_374, new __VLS_374({
        modelValue: (__VLS_ctx.itemQuery.documentCode),
        placeholder: "请输入单据编号",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_376 = __VLS_375({
        modelValue: (__VLS_ctx.itemQuery.documentCode),
        placeholder: "请输入单据编号",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_375));
    var __VLS_373;
    const __VLS_378 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_379 = __VLS_asFunctionalComponent(__VLS_378, new __VLS_378({
        label: "物品",
    }));
    const __VLS_380 = __VLS_379({
        label: "物品",
    }, ...__VLS_functionalComponentArgsRest(__VLS_379));
    __VLS_381.slots.default;
    const __VLS_382 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_383 = __VLS_asFunctionalComponent(__VLS_382, new __VLS_382({
        modelValue: (__VLS_ctx.itemQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_384 = __VLS_383({
        modelValue: (__VLS_ctx.itemQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_383));
    __VLS_385.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
        const __VLS_386 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_387 = __VLS_asFunctionalComponent(__VLS_386, new __VLS_386({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_388 = __VLS_387({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_387));
    }
    var __VLS_385;
    var __VLS_381;
    const __VLS_390 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_391 = __VLS_asFunctionalComponent(__VLS_390, new __VLS_390({
        label: "单据状态",
    }));
    const __VLS_392 = __VLS_391({
        label: "单据状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_391));
    __VLS_393.slots.default;
    const __VLS_394 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_395 = __VLS_asFunctionalComponent(__VLS_394, new __VLS_394({
        modelValue: (__VLS_ctx.itemQuery.documentStatus),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_396 = __VLS_395({
        modelValue: (__VLS_ctx.itemQuery.documentStatus),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_395));
    __VLS_397.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.documentStatusOptions))) {
        const __VLS_398 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_399 = __VLS_asFunctionalComponent(__VLS_398, new __VLS_398({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_400 = __VLS_399({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_399));
    }
    var __VLS_397;
    var __VLS_393;
    if (!__VLS_ctx.itemFiltersCollapsed) {
        const __VLS_402 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_403 = __VLS_asFunctionalComponent(__VLS_402, new __VLS_402({
            label: "移库类型",
        }));
        const __VLS_404 = __VLS_403({
            label: "移库类型",
        }, ...__VLS_functionalComponentArgsRest(__VLS_403));
        __VLS_405.slots.default;
        const __VLS_406 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_407 = __VLS_asFunctionalComponent(__VLS_406, new __VLS_406({
            modelValue: (__VLS_ctx.itemQuery.transferType),
            ...{ style: {} },
        }));
        const __VLS_408 = __VLS_407({
            modelValue: (__VLS_ctx.itemQuery.transferType),
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_407));
        __VLS_409.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.transferTypeOptions))) {
            const __VLS_410 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_411 = __VLS_asFunctionalComponent(__VLS_410, new __VLS_410({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_412 = __VLS_411({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_411));
        }
        var __VLS_409;
        var __VLS_405;
        const __VLS_414 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_415 = __VLS_asFunctionalComponent(__VLS_414, new __VLS_414({
            label: "入库状态",
        }));
        const __VLS_416 = __VLS_415({
            label: "入库状态",
        }, ...__VLS_functionalComponentArgsRest(__VLS_415));
        __VLS_417.slots.default;
        const __VLS_418 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_419 = __VLS_asFunctionalComponent(__VLS_418, new __VLS_418({
            modelValue: (__VLS_ctx.itemQuery.inboundStatus),
            ...{ style: {} },
        }));
        const __VLS_420 = __VLS_419({
            modelValue: (__VLS_ctx.itemQuery.inboundStatus),
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_419));
        __VLS_421.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.inboundStatusOptions))) {
            const __VLS_422 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_423 = __VLS_asFunctionalComponent(__VLS_422, new __VLS_422({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_424 = __VLS_423({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_423));
        }
        var __VLS_421;
        var __VLS_417;
        const __VLS_426 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_427 = __VLS_asFunctionalComponent(__VLS_426, new __VLS_426({
            label: "打印状态",
        }));
        const __VLS_428 = __VLS_427({
            label: "打印状态",
        }, ...__VLS_functionalComponentArgsRest(__VLS_427));
        __VLS_429.slots.default;
        const __VLS_430 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_431 = __VLS_asFunctionalComponent(__VLS_430, new __VLS_430({
            modelValue: (__VLS_ctx.itemQuery.printStatus),
            ...{ style: {} },
        }));
        const __VLS_432 = __VLS_431({
            modelValue: (__VLS_ctx.itemQuery.printStatus),
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_431));
        __VLS_433.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.printStatusOptions))) {
            const __VLS_434 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_435 = __VLS_asFunctionalComponent(__VLS_434, new __VLS_434({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_436 = __VLS_435({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_435));
        }
        var __VLS_433;
        var __VLS_429;
        const __VLS_438 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_439 = __VLS_asFunctionalComponent(__VLS_438, new __VLS_438({
            label: "备注",
        }));
        const __VLS_440 = __VLS_439({
            label: "备注",
        }, ...__VLS_functionalComponentArgsRest(__VLS_439));
        __VLS_441.slots.default;
        const __VLS_442 = {}.ElInput;
        /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
        // @ts-ignore
        const __VLS_443 = __VLS_asFunctionalComponent(__VLS_442, new __VLS_442({
            modelValue: (__VLS_ctx.itemQuery.remark),
            placeholder: "请输入备注",
            clearable: true,
            ...{ style: {} },
        }));
        const __VLS_444 = __VLS_443({
            modelValue: (__VLS_ctx.itemQuery.remark),
            placeholder: "请输入备注",
            clearable: true,
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_443));
        var __VLS_441;
    }
    const __VLS_446 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_447 = __VLS_asFunctionalComponent(__VLS_446, new __VLS_446({}));
    const __VLS_448 = __VLS_447({}, ...__VLS_functionalComponentArgsRest(__VLS_447));
    __VLS_449.slots.default;
    const __VLS_450 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_451 = __VLS_asFunctionalComponent(__VLS_450, new __VLS_450({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_452 = __VLS_451({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_451));
    let __VLS_454;
    let __VLS_455;
    let __VLS_456;
    const __VLS_457 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.itemFiltersCollapsed = !__VLS_ctx.itemFiltersCollapsed;
        }
    };
    __VLS_453.slots.default;
    const __VLS_458 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_459 = __VLS_asFunctionalComponent(__VLS_458, new __VLS_458({}));
    const __VLS_460 = __VLS_459({}, ...__VLS_functionalComponentArgsRest(__VLS_459));
    __VLS_461.slots.default;
    if (__VLS_ctx.itemFiltersCollapsed) {
        const __VLS_462 = {}.ArrowDown;
        /** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
        // @ts-ignore
        const __VLS_463 = __VLS_asFunctionalComponent(__VLS_462, new __VLS_462({}));
        const __VLS_464 = __VLS_463({}, ...__VLS_functionalComponentArgsRest(__VLS_463));
    }
    else {
        const __VLS_466 = {}.ArrowUp;
        /** @type {[typeof __VLS_components.ArrowUp, ]} */ ;
        // @ts-ignore
        const __VLS_467 = __VLS_asFunctionalComponent(__VLS_466, new __VLS_466({}));
        const __VLS_468 = __VLS_467({}, ...__VLS_functionalComponentArgsRest(__VLS_467));
    }
    var __VLS_461;
    (__VLS_ctx.itemFiltersCollapsed ? '展开筛选' : '收起筛选');
    var __VLS_453;
    const __VLS_470 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_471 = __VLS_asFunctionalComponent(__VLS_470, new __VLS_470({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_472 = __VLS_471({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_471));
    let __VLS_474;
    let __VLS_475;
    let __VLS_476;
    const __VLS_477 = {
        onClick: (__VLS_ctx.handleItemSearch)
    };
    __VLS_473.slots.default;
    const __VLS_478 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_479 = __VLS_asFunctionalComponent(__VLS_478, new __VLS_478({}));
    const __VLS_480 = __VLS_479({}, ...__VLS_functionalComponentArgsRest(__VLS_479));
    __VLS_481.slots.default;
    const __VLS_482 = {}.Search;
    /** @type {[typeof __VLS_components.Search, ]} */ ;
    // @ts-ignore
    const __VLS_483 = __VLS_asFunctionalComponent(__VLS_482, new __VLS_482({}));
    const __VLS_484 = __VLS_483({}, ...__VLS_functionalComponentArgsRest(__VLS_483));
    var __VLS_481;
    var __VLS_473;
    const __VLS_486 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_487 = __VLS_asFunctionalComponent(__VLS_486, new __VLS_486({
        ...{ 'onClick': {} },
    }));
    const __VLS_488 = __VLS_487({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_487));
    let __VLS_490;
    let __VLS_491;
    let __VLS_492;
    const __VLS_493 = {
        onClick: (__VLS_ctx.handleItemReset)
    };
    __VLS_489.slots.default;
    const __VLS_494 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_495 = __VLS_asFunctionalComponent(__VLS_494, new __VLS_494({}));
    const __VLS_496 = __VLS_495({}, ...__VLS_functionalComponentArgsRest(__VLS_495));
    __VLS_497.slots.default;
    const __VLS_498 = {}.RefreshRight;
    /** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
    // @ts-ignore
    const __VLS_499 = __VLS_asFunctionalComponent(__VLS_498, new __VLS_498({}));
    const __VLS_500 = __VLS_499({}, ...__VLS_functionalComponentArgsRest(__VLS_499));
    var __VLS_497;
    var __VLS_489;
    var __VLS_449;
    var __VLS_333;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-toolbar" },
    });
    const __VLS_502 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_503 = __VLS_asFunctionalComponent(__VLS_502, new __VLS_502({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_504 = __VLS_503({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_503));
    let __VLS_506;
    let __VLS_507;
    let __VLS_508;
    const __VLS_509 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('新增');
        }
    };
    __VLS_505.slots.default;
    const __VLS_510 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_511 = __VLS_asFunctionalComponent(__VLS_510, new __VLS_510({}));
    const __VLS_512 = __VLS_511({}, ...__VLS_functionalComponentArgsRest(__VLS_511));
    __VLS_513.slots.default;
    const __VLS_514 = {}.Plus;
    /** @type {[typeof __VLS_components.Plus, ]} */ ;
    // @ts-ignore
    const __VLS_515 = __VLS_asFunctionalComponent(__VLS_514, new __VLS_514({}));
    const __VLS_516 = __VLS_515({}, ...__VLS_functionalComponentArgsRest(__VLS_515));
    var __VLS_513;
    var __VLS_505;
    const __VLS_518 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_519 = __VLS_asFunctionalComponent(__VLS_518, new __VLS_518({
        ...{ 'onClick': {} },
    }));
    const __VLS_520 = __VLS_519({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_519));
    let __VLS_522;
    let __VLS_523;
    let __VLS_524;
    const __VLS_525 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量打印');
        }
    };
    __VLS_521.slots.default;
    const __VLS_526 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_527 = __VLS_asFunctionalComponent(__VLS_526, new __VLS_526({}));
    const __VLS_528 = __VLS_527({}, ...__VLS_functionalComponentArgsRest(__VLS_527));
    __VLS_529.slots.default;
    const __VLS_530 = {}.Printer;
    /** @type {[typeof __VLS_components.Printer, ]} */ ;
    // @ts-ignore
    const __VLS_531 = __VLS_asFunctionalComponent(__VLS_530, new __VLS_530({}));
    const __VLS_532 = __VLS_531({}, ...__VLS_functionalComponentArgsRest(__VLS_531));
    var __VLS_529;
    var __VLS_521;
    const __VLS_534 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_535 = __VLS_asFunctionalComponent(__VLS_534, new __VLS_534({
        ...{ 'onClick': {} },
    }));
    const __VLS_536 = __VLS_535({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_535));
    let __VLS_538;
    let __VLS_539;
    let __VLS_540;
    const __VLS_541 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量删除');
        }
    };
    __VLS_537.slots.default;
    const __VLS_542 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_543 = __VLS_asFunctionalComponent(__VLS_542, new __VLS_542({}));
    const __VLS_544 = __VLS_543({}, ...__VLS_functionalComponentArgsRest(__VLS_543));
    __VLS_545.slots.default;
    const __VLS_546 = {}.Delete;
    /** @type {[typeof __VLS_components.Delete, ]} */ ;
    // @ts-ignore
    const __VLS_547 = __VLS_asFunctionalComponent(__VLS_546, new __VLS_546({}));
    const __VLS_548 = __VLS_547({}, ...__VLS_functionalComponentArgsRest(__VLS_547));
    var __VLS_545;
    var __VLS_537;
    const __VLS_550 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_551 = __VLS_asFunctionalComponent(__VLS_550, new __VLS_550({
        ...{ 'onClick': {} },
    }));
    const __VLS_552 = __VLS_551({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_551));
    let __VLS_554;
    let __VLS_555;
    let __VLS_556;
    const __VLS_557 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量提交');
        }
    };
    __VLS_553.slots.default;
    var __VLS_553;
    const __VLS_558 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_559 = __VLS_asFunctionalComponent(__VLS_558, new __VLS_558({
        ...{ 'onClick': {} },
    }));
    const __VLS_560 = __VLS_559({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_559));
    let __VLS_562;
    let __VLS_563;
    let __VLS_564;
    const __VLS_565 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量审核');
        }
    };
    __VLS_561.slots.default;
    var __VLS_561;
    const __VLS_566 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_567 = __VLS_asFunctionalComponent(__VLS_566, new __VLS_566({
        ...{ 'onClick': {} },
    }));
    const __VLS_568 = __VLS_567({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_567));
    let __VLS_570;
    let __VLS_571;
    let __VLS_572;
    const __VLS_573 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量反审核');
        }
    };
    __VLS_569.slots.default;
    var __VLS_569;
    const __VLS_574 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_575 = __VLS_asFunctionalComponent(__VLS_574, new __VLS_574({
        ...{ 'onClick': {} },
    }));
    const __VLS_576 = __VLS_575({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_575));
    let __VLS_578;
    let __VLS_579;
    let __VLS_580;
    const __VLS_581 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'document'))
                return;
            __VLS_ctx.handleToolbarAction('批量导出物品明细');
        }
    };
    __VLS_577.slots.default;
    var __VLS_577;
    const __VLS_582 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_583 = __VLS_asFunctionalComponent(__VLS_582, new __VLS_582({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.itemPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_584 = __VLS_583({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.itemPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_583));
    let __VLS_586;
    let __VLS_587;
    let __VLS_588;
    const __VLS_589 = {
        onSelectionChange: (__VLS_ctx.handleItemSelectionChange)
    };
    __VLS_585.slots.default;
    const __VLS_590 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_591 = __VLS_asFunctionalComponent(__VLS_590, new __VLS_590({
        type: "selection",
        width: "44",
        fixed: "left",
    }));
    const __VLS_592 = __VLS_591({
        type: "selection",
        width: "44",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_591));
    const __VLS_594 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_595 = __VLS_asFunctionalComponent(__VLS_594, new __VLS_594({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }));
    const __VLS_596 = __VLS_595({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_595));
    const __VLS_598 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_599 = __VLS_asFunctionalComponent(__VLS_598, new __VLS_598({
        prop: "documentCode",
        label: "单据编号",
        minWidth: "140",
        showOverflowTooltip: true,
    }));
    const __VLS_600 = __VLS_599({
        prop: "documentCode",
        label: "单据编号",
        minWidth: "140",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_599));
    const __VLS_602 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_603 = __VLS_asFunctionalComponent(__VLS_602, new __VLS_602({
        prop: "transferDate",
        label: "移库日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }));
    const __VLS_604 = __VLS_603({
        prop: "transferDate",
        label: "移库日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_603));
    const __VLS_606 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_607 = __VLS_asFunctionalComponent(__VLS_606, new __VLS_606({
        prop: "outboundWarehouse",
        label: "出库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_608 = __VLS_607({
        prop: "outboundWarehouse",
        label: "出库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_607));
    const __VLS_610 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_611 = __VLS_asFunctionalComponent(__VLS_610, new __VLS_610({
        prop: "inboundWarehouse",
        label: "入库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_612 = __VLS_611({
        prop: "inboundWarehouse",
        label: "入库仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_611));
    const __VLS_614 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_615 = __VLS_asFunctionalComponent(__VLS_614, new __VLS_614({
        prop: "documentStatus",
        label: "单据状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_616 = __VLS_615({
        prop: "documentStatus",
        label: "单据状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_615));
    const __VLS_618 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_619 = __VLS_asFunctionalComponent(__VLS_618, new __VLS_618({
        prop: "itemCode",
        label: "物品编码",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_620 = __VLS_619({
        prop: "itemCode",
        label: "物品编码",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_619));
    const __VLS_622 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_623 = __VLS_asFunctionalComponent(__VLS_622, new __VLS_622({
        prop: "itemName",
        label: "物品名称",
        minWidth: "130",
        showOverflowTooltip: true,
    }));
    const __VLS_624 = __VLS_623({
        prop: "itemName",
        label: "物品名称",
        minWidth: "130",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_623));
    const __VLS_626 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_627 = __VLS_asFunctionalComponent(__VLS_626, new __VLS_626({
        prop: "spec",
        label: "规格型号",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_628 = __VLS_627({
        prop: "spec",
        label: "规格型号",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_627));
    const __VLS_630 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_631 = __VLS_asFunctionalComponent(__VLS_630, new __VLS_630({
        prop: "category",
        label: "物品类别",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_632 = __VLS_631({
        prop: "category",
        label: "物品类别",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_631));
    const __VLS_634 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_635 = __VLS_asFunctionalComponent(__VLS_634, new __VLS_634({
        prop: "unit",
        label: "单位",
        minWidth: "80",
        showOverflowTooltip: true,
    }));
    const __VLS_636 = __VLS_635({
        prop: "unit",
        label: "单位",
        minWidth: "80",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_635));
    const __VLS_638 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_639 = __VLS_asFunctionalComponent(__VLS_638, new __VLS_638({
        prop: "quantity",
        label: "数量",
        minWidth: "80",
        showOverflowTooltip: true,
    }));
    const __VLS_640 = __VLS_639({
        prop: "quantity",
        label: "数量",
        minWidth: "80",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_639));
    const __VLS_642 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_643 = __VLS_asFunctionalComponent(__VLS_642, new __VLS_642({
        prop: "price",
        label: "单价",
        minWidth: "90",
        showOverflowTooltip: true,
    }));
    const __VLS_644 = __VLS_643({
        prop: "price",
        label: "单价",
        minWidth: "90",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_643));
    const __VLS_646 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_647 = __VLS_asFunctionalComponent(__VLS_646, new __VLS_646({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_648 = __VLS_647({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_647));
    var __VLS_585;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.itemSelectedIds.length);
    const __VLS_650 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_651 = __VLS_asFunctionalComponent(__VLS_650, new __VLS_650({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.itemCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.itemFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }));
    const __VLS_652 = __VLS_651({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.itemCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.itemFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_651));
    let __VLS_654;
    let __VLS_655;
    let __VLS_656;
    const __VLS_657 = {
        onCurrentChange: (__VLS_ctx.handleItemPageChange)
    };
    const __VLS_658 = {
        onSizeChange: (__VLS_ctx.handlePageSizeChange)
    };
    var __VLS_653;
}
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['stock-transfer-page']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
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
            Upload: Upload,
            CommonQuerySection: CommonQuerySection,
            PageTabsLayout: PageTabsLayout,
            tabs: tabs,
            timeTypeOptions: timeTypeOptions,
            documentStatusOptions: documentStatusOptions,
            transferTypeOptions: transferTypeOptions,
            inboundStatusOptions: inboundStatusOptions,
            printStatusOptions: printStatusOptions,
            itemOptions: itemOptions,
            warehouseTree: warehouseTree,
            activeTab: activeTab,
            itemFiltersCollapsed: itemFiltersCollapsed,
            documentCurrentPage: documentCurrentPage,
            itemCurrentPage: itemCurrentPage,
            pageSize: pageSize,
            documentSelectedIds: documentSelectedIds,
            itemSelectedIds: itemSelectedIds,
            documentQuery: documentQuery,
            itemQuery: itemQuery,
            documentFilteredRows: documentFilteredRows,
            itemFilteredRows: itemFilteredRows,
            documentPagedRows: documentPagedRows,
            itemPagedRows: itemPagedRows,
            handleDocumentSearch: handleDocumentSearch,
            handleItemSearch: handleItemSearch,
            handleDocumentReset: handleDocumentReset,
            handleItemReset: handleItemReset,
            handleToolbarAction: handleToolbarAction,
            handleDocumentSelectionChange: handleDocumentSelectionChange,
            handleItemSelectionChange: handleItemSelectionChange,
            handleDocumentPageChange: handleDocumentPageChange,
            handleItemPageChange: handleItemPageChange,
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
