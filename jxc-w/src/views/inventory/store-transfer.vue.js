/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout from '@/components/PageTabsLayout.vue';
const tabs = [
    { key: 'outbound', label: '调出' },
    { key: 'inbound', label: '调入' },
];
const activeTab = ref('outbound');
const statusOptions = ['', '全部', '草稿', '已提交', '已发货', '已收货'];
const reviewStatusOptions = ['', '未复审', '已复审'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const storeTree = [
    {
        value: 'store-root',
        label: '门店中心',
        children: [
            { value: '国贸店', label: '国贸店' },
            { value: '望京店', label: '望京店' },
            { value: '三里屯店', label: '三里屯店' },
            { value: '五道口店', label: '五道口店' },
        ],
    },
];
const outboundQuery = reactive({
    inboundStore: '',
    status: '',
    reviewStatus: '',
    transferCode: '',
    transferDate: '',
    itemName: '',
});
const inboundQuery = reactive({
    outboundStore: '',
    status: '全部',
    reviewStatus: '',
    transferCode: '',
    transferDate: '',
    itemName: '',
});
const outboundRows = [
    {
        id: 1,
        transferCode: 'DJDB-202604-001',
        transferDate: '2026-04-13',
        inboundStore: '望京店',
        amount: '6,820.00',
        status: '已发货',
        reviewStatus: '已复审',
        createdAt: '2026-04-13 09:16:00',
        creator: '张敏',
    },
    {
        id: 2,
        transferCode: 'DJDB-202604-002',
        transferDate: '2026-04-12',
        inboundStore: '三里屯店',
        amount: '3,420.00',
        status: '已提交',
        reviewStatus: '未复审',
        createdAt: '2026-04-12 15:08:00',
        creator: '李娜',
    },
    {
        id: 3,
        transferCode: 'DJDB-202604-003',
        transferDate: '2026-04-11',
        inboundStore: '五道口店',
        amount: '1,960.00',
        status: '草稿',
        reviewStatus: '未复审',
        createdAt: '2026-04-11 10:22:00',
        creator: '王磊',
    },
];
const inboundRows = [
    {
        id: 1,
        transferCode: 'DJDB-202604-001',
        transferDate: '2026-04-13',
        outboundStore: '国贸店',
        amount: '6,820.00',
        status: '已收货',
        reviewStatus: '已复审',
        createdAt: '2026-04-13 09:16:00',
        creator: '张敏',
    },
    {
        id: 2,
        transferCode: 'DJDB-202604-004',
        transferDate: '2026-04-12',
        outboundStore: '望京店',
        amount: '2,760.00',
        status: '已发货',
        reviewStatus: '未复审',
        createdAt: '2026-04-12 16:28:00',
        creator: '李娜',
    },
    {
        id: 3,
        transferCode: 'DJDB-202604-005',
        transferDate: '2026-04-11',
        outboundStore: '三里屯店',
        amount: '1,340.00',
        status: '已提交',
        reviewStatus: '未复审',
        createdAt: '2026-04-11 11:06:00',
        creator: '王磊',
    },
];
const outboundCurrentPage = ref(1);
const inboundCurrentPage = ref(1);
const pageSize = ref(10);
const outboundSelectedIds = ref([]);
const inboundSelectedIds = ref([]);
const outboundFilteredRows = computed(() => {
    const transferCodeKeyword = outboundQuery.transferCode.trim().toLowerCase();
    return outboundRows.filter((row) => {
        const matchedInboundStore = !outboundQuery.inboundStore || row.inboundStore === outboundQuery.inboundStore;
        const matchedStatus = !outboundQuery.status || outboundQuery.status === '全部' || row.status === outboundQuery.status;
        const matchedReviewStatus = !outboundQuery.reviewStatus || row.reviewStatus === outboundQuery.reviewStatus;
        const matchedTransferCode = !transferCodeKeyword || row.transferCode.toLowerCase().includes(transferCodeKeyword);
        const matchedTransferDate = !outboundQuery.transferDate || row.transferDate === outboundQuery.transferDate;
        const matchedItem = !outboundQuery.itemName || row.transferCode.includes(outboundQuery.itemName) || row.inboundStore.includes(outboundQuery.itemName);
        return matchedInboundStore && matchedStatus && matchedReviewStatus && matchedTransferCode && matchedTransferDate && matchedItem;
    });
});
const inboundFilteredRows = computed(() => {
    const transferCodeKeyword = inboundQuery.transferCode.trim().toLowerCase();
    return inboundRows.filter((row) => {
        const matchedOutboundStore = !inboundQuery.outboundStore || row.outboundStore === inboundQuery.outboundStore;
        const matchedStatus = !inboundQuery.status || inboundQuery.status === '全部' || row.status === inboundQuery.status;
        const matchedReviewStatus = !inboundQuery.reviewStatus || row.reviewStatus === inboundQuery.reviewStatus;
        const matchedTransferCode = !transferCodeKeyword || row.transferCode.toLowerCase().includes(transferCodeKeyword);
        const matchedTransferDate = !inboundQuery.transferDate || row.transferDate === inboundQuery.transferDate;
        const matchedItem = !inboundQuery.itemName || row.transferCode.includes(inboundQuery.itemName) || row.outboundStore.includes(inboundQuery.itemName);
        return matchedOutboundStore && matchedStatus && matchedReviewStatus && matchedTransferCode && matchedTransferDate && matchedItem;
    });
});
const outboundPagedRows = computed(() => {
    const start = (outboundCurrentPage.value - 1) * pageSize.value;
    return outboundFilteredRows.value.slice(start, start + pageSize.value);
});
const inboundPagedRows = computed(() => {
    const start = (inboundCurrentPage.value - 1) * pageSize.value;
    return inboundFilteredRows.value.slice(start, start + pageSize.value);
});
const handleOutboundSearch = () => {
    outboundCurrentPage.value = 1;
};
const handleInboundSearch = () => {
    inboundCurrentPage.value = 1;
};
const handleOutboundReset = () => {
    outboundQuery.inboundStore = '';
    outboundQuery.status = '';
    outboundQuery.reviewStatus = '';
    outboundQuery.transferCode = '';
    outboundQuery.transferDate = '';
    outboundQuery.itemName = '';
    outboundCurrentPage.value = 1;
};
const handleInboundReset = () => {
    inboundQuery.outboundStore = '';
    inboundQuery.status = '全部';
    inboundQuery.reviewStatus = '';
    inboundQuery.transferCode = '';
    inboundQuery.transferDate = '';
    inboundQuery.itemName = '';
    inboundCurrentPage.value = 1;
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
};
const handleOutboundSelectionChange = (rows) => {
    outboundSelectedIds.value = rows.map((row) => row.id);
};
const handleInboundSelectionChange = (rows) => {
    inboundSelectedIds.value = rows.map((row) => row.id);
};
const handleOutboundPageChange = (page) => {
    outboundCurrentPage.value = page;
};
const handleInboundPageChange = (page) => {
    inboundCurrentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    outboundCurrentPage.value = 1;
    inboundCurrentPage.value = 1;
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel store-transfer-page" },
});
/** @type {[typeof PageTabsLayout, typeof PageTabsLayout, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(PageTabsLayout, new PageTabsLayout({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
    bodyClass: "store-transfer-page__body",
}));
const __VLS_1 = __VLS_0({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
    bodyClass: "store-transfer-page__body",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
if (__VLS_ctx.activeTab === 'outbound') {
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_3 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.outboundQuery),
    }));
    const __VLS_4 = __VLS_3({
        model: (__VLS_ctx.outboundQuery),
    }, ...__VLS_functionalComponentArgsRest(__VLS_3));
    __VLS_5.slots.default;
    const __VLS_6 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(__VLS_6, new __VLS_6({
        label: "调入门店",
    }));
    const __VLS_8 = __VLS_7({
        label: "调入门店",
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    __VLS_9.slots.default;
    const __VLS_10 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
        modelValue: (__VLS_ctx.outboundQuery.inboundStore),
        data: (__VLS_ctx.storeTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }));
    const __VLS_12 = __VLS_11({
        modelValue: (__VLS_ctx.outboundQuery.inboundStore),
        data: (__VLS_ctx.storeTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_11));
    var __VLS_9;
    const __VLS_14 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
        label: "单据状态",
    }));
    const __VLS_16 = __VLS_15({
        label: "单据状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_15));
    __VLS_17.slots.default;
    const __VLS_18 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
        modelValue: (__VLS_ctx.outboundQuery.status),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_20 = __VLS_19({
        modelValue: (__VLS_ctx.outboundQuery.status),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_19));
    __VLS_21.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.statusOptions))) {
        const __VLS_22 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
            key: (option || 'empty'),
            label: (option || '请选择'),
            value: (option),
        }));
        const __VLS_24 = __VLS_23({
            key: (option || 'empty'),
            label: (option || '请选择'),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_23));
    }
    var __VLS_21;
    var __VLS_17;
    const __VLS_26 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
        label: "复审状态",
    }));
    const __VLS_28 = __VLS_27({
        label: "复审状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_27));
    __VLS_29.slots.default;
    const __VLS_30 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
        modelValue: (__VLS_ctx.outboundQuery.reviewStatus),
        clearable: true,
        placeholder: "请选择复审状态",
        ...{ style: {} },
    }));
    const __VLS_32 = __VLS_31({
        modelValue: (__VLS_ctx.outboundQuery.reviewStatus),
        clearable: true,
        placeholder: "请选择复审状态",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_31));
    __VLS_33.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.reviewStatusOptions))) {
        const __VLS_34 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
            key: (option || 'empty'),
            label: (option || '请选择复审状态'),
            value: (option),
        }));
        const __VLS_36 = __VLS_35({
            key: (option || 'empty'),
            label: (option || '请选择复审状态'),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_35));
    }
    var __VLS_33;
    var __VLS_29;
    const __VLS_38 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
        label: "调拨单号",
    }));
    const __VLS_40 = __VLS_39({
        label: "调拨单号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_39));
    __VLS_41.slots.default;
    const __VLS_42 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
        modelValue: (__VLS_ctx.outboundQuery.transferCode),
        placeholder: "请输入调拨单号",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_44 = __VLS_43({
        modelValue: (__VLS_ctx.outboundQuery.transferCode),
        placeholder: "请输入调拨单号",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_43));
    var __VLS_41;
    const __VLS_46 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
        label: "调拨日期",
    }));
    const __VLS_48 = __VLS_47({
        label: "调拨日期",
    }, ...__VLS_functionalComponentArgsRest(__VLS_47));
    __VLS_49.slots.default;
    const __VLS_50 = {}.ElDatePicker;
    /** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
    // @ts-ignore
    const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
        modelValue: (__VLS_ctx.outboundQuery.transferDate),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        placeholder: "请选择调拨日期",
        ...{ style: {} },
    }));
    const __VLS_52 = __VLS_51({
        modelValue: (__VLS_ctx.outboundQuery.transferDate),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        placeholder: "请选择调拨日期",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_51));
    var __VLS_49;
    const __VLS_54 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
        label: "物品",
    }));
    const __VLS_56 = __VLS_55({
        label: "物品",
    }, ...__VLS_functionalComponentArgsRest(__VLS_55));
    __VLS_57.slots.default;
    const __VLS_58 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
        modelValue: (__VLS_ctx.outboundQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_60 = __VLS_59({
        modelValue: (__VLS_ctx.outboundQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_59));
    __VLS_61.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
        const __VLS_62 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_64 = __VLS_63({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_63));
    }
    var __VLS_61;
    var __VLS_57;
    const __VLS_66 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({}));
    const __VLS_68 = __VLS_67({}, ...__VLS_functionalComponentArgsRest(__VLS_67));
    __VLS_69.slots.default;
    const __VLS_70 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_72 = __VLS_71({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_71));
    let __VLS_74;
    let __VLS_75;
    let __VLS_76;
    const __VLS_77 = {
        onClick: (__VLS_ctx.handleOutboundSearch)
    };
    __VLS_73.slots.default;
    const __VLS_78 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({}));
    const __VLS_80 = __VLS_79({}, ...__VLS_functionalComponentArgsRest(__VLS_79));
    __VLS_81.slots.default;
    const __VLS_82 = {}.Search;
    /** @type {[typeof __VLS_components.Search, ]} */ ;
    // @ts-ignore
    const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({}));
    const __VLS_84 = __VLS_83({}, ...__VLS_functionalComponentArgsRest(__VLS_83));
    var __VLS_81;
    var __VLS_73;
    const __VLS_86 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
        ...{ 'onClick': {} },
    }));
    const __VLS_88 = __VLS_87({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_87));
    let __VLS_90;
    let __VLS_91;
    let __VLS_92;
    const __VLS_93 = {
        onClick: (__VLS_ctx.handleOutboundReset)
    };
    __VLS_89.slots.default;
    const __VLS_94 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({}));
    const __VLS_96 = __VLS_95({}, ...__VLS_functionalComponentArgsRest(__VLS_95));
    __VLS_97.slots.default;
    const __VLS_98 = {}.RefreshRight;
    /** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
    // @ts-ignore
    const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({}));
    const __VLS_100 = __VLS_99({}, ...__VLS_functionalComponentArgsRest(__VLS_99));
    var __VLS_97;
    var __VLS_89;
    var __VLS_69;
    var __VLS_5;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-toolbar" },
    });
    const __VLS_102 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_104 = __VLS_103({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_103));
    let __VLS_106;
    let __VLS_107;
    let __VLS_108;
    const __VLS_109 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('新增');
        }
    };
    __VLS_105.slots.default;
    const __VLS_110 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({}));
    const __VLS_112 = __VLS_111({}, ...__VLS_functionalComponentArgsRest(__VLS_111));
    __VLS_113.slots.default;
    const __VLS_114 = {}.Plus;
    /** @type {[typeof __VLS_components.Plus, ]} */ ;
    // @ts-ignore
    const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({}));
    const __VLS_116 = __VLS_115({}, ...__VLS_functionalComponentArgsRest(__VLS_115));
    var __VLS_113;
    var __VLS_105;
    const __VLS_118 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
        ...{ 'onClick': {} },
    }));
    const __VLS_120 = __VLS_119({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_119));
    let __VLS_122;
    let __VLS_123;
    let __VLS_124;
    const __VLS_125 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量打印');
        }
    };
    __VLS_121.slots.default;
    const __VLS_126 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({}));
    const __VLS_128 = __VLS_127({}, ...__VLS_functionalComponentArgsRest(__VLS_127));
    __VLS_129.slots.default;
    const __VLS_130 = {}.Printer;
    /** @type {[typeof __VLS_components.Printer, ]} */ ;
    // @ts-ignore
    const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({}));
    const __VLS_132 = __VLS_131({}, ...__VLS_functionalComponentArgsRest(__VLS_131));
    var __VLS_129;
    var __VLS_121;
    const __VLS_134 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
        ...{ 'onClick': {} },
    }));
    const __VLS_136 = __VLS_135({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_135));
    let __VLS_138;
    let __VLS_139;
    let __VLS_140;
    const __VLS_141 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量删除');
        }
    };
    __VLS_137.slots.default;
    const __VLS_142 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({}));
    const __VLS_144 = __VLS_143({}, ...__VLS_functionalComponentArgsRest(__VLS_143));
    __VLS_145.slots.default;
    const __VLS_146 = {}.Delete;
    /** @type {[typeof __VLS_components.Delete, ]} */ ;
    // @ts-ignore
    const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({}));
    const __VLS_148 = __VLS_147({}, ...__VLS_functionalComponentArgsRest(__VLS_147));
    var __VLS_145;
    var __VLS_137;
    const __VLS_150 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
        ...{ 'onClick': {} },
    }));
    const __VLS_152 = __VLS_151({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_151));
    let __VLS_154;
    let __VLS_155;
    let __VLS_156;
    const __VLS_157 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量提交');
        }
    };
    __VLS_153.slots.default;
    var __VLS_153;
    const __VLS_158 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
        ...{ 'onClick': {} },
    }));
    const __VLS_160 = __VLS_159({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_159));
    let __VLS_162;
    let __VLS_163;
    let __VLS_164;
    const __VLS_165 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量撤回');
        }
    };
    __VLS_161.slots.default;
    var __VLS_161;
    const __VLS_166 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
        ...{ 'onClick': {} },
    }));
    const __VLS_168 = __VLS_167({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_167));
    let __VLS_170;
    let __VLS_171;
    let __VLS_172;
    const __VLS_173 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量发货');
        }
    };
    __VLS_169.slots.default;
    var __VLS_169;
    const __VLS_174 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
        ...{ 'onClick': {} },
    }));
    const __VLS_176 = __VLS_175({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_175));
    let __VLS_178;
    let __VLS_179;
    let __VLS_180;
    const __VLS_181 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量取消发货');
        }
    };
    __VLS_177.slots.default;
    var __VLS_177;
    const __VLS_182 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
        ...{ 'onClick': {} },
    }));
    const __VLS_184 = __VLS_183({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_183));
    let __VLS_186;
    let __VLS_187;
    let __VLS_188;
    const __VLS_189 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量复审');
        }
    };
    __VLS_185.slots.default;
    var __VLS_185;
    const __VLS_190 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
        ...{ 'onClick': {} },
    }));
    const __VLS_192 = __VLS_191({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_191));
    let __VLS_194;
    let __VLS_195;
    let __VLS_196;
    const __VLS_197 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量取消复审');
        }
    };
    __VLS_193.slots.default;
    var __VLS_193;
    const __VLS_198 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.outboundPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_200 = __VLS_199({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.outboundPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_199));
    let __VLS_202;
    let __VLS_203;
    let __VLS_204;
    const __VLS_205 = {
        onSelectionChange: (__VLS_ctx.handleOutboundSelectionChange)
    };
    __VLS_201.slots.default;
    const __VLS_206 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_207 = __VLS_asFunctionalComponent(__VLS_206, new __VLS_206({
        type: "selection",
        width: "44",
        fixed: "left",
    }));
    const __VLS_208 = __VLS_207({
        type: "selection",
        width: "44",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_207));
    const __VLS_210 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }));
    const __VLS_212 = __VLS_211({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_211));
    const __VLS_214 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({
        prop: "transferCode",
        label: "调拨单号",
        minWidth: "150",
        showOverflowTooltip: true,
    }));
    const __VLS_216 = __VLS_215({
        prop: "transferCode",
        label: "调拨单号",
        minWidth: "150",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_215));
    const __VLS_218 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
        prop: "transferDate",
        label: "调拨日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }));
    const __VLS_220 = __VLS_219({
        prop: "transferDate",
        label: "调拨日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_219));
    const __VLS_222 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_223 = __VLS_asFunctionalComponent(__VLS_222, new __VLS_222({
        prop: "inboundStore",
        label: "调入门店",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_224 = __VLS_223({
        prop: "inboundStore",
        label: "调入门店",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_223));
    const __VLS_226 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_228 = __VLS_227({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_227));
    const __VLS_230 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_231 = __VLS_asFunctionalComponent(__VLS_230, new __VLS_230({
        prop: "status",
        label: "状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_232 = __VLS_231({
        prop: "status",
        label: "状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_231));
    const __VLS_234 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_235 = __VLS_asFunctionalComponent(__VLS_234, new __VLS_234({
        prop: "reviewStatus",
        label: "复审状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_236 = __VLS_235({
        prop: "reviewStatus",
        label: "复审状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_235));
    const __VLS_238 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_239 = __VLS_asFunctionalComponent(__VLS_238, new __VLS_238({
        prop: "createdAt",
        label: "创建时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }));
    const __VLS_240 = __VLS_239({
        prop: "createdAt",
        label: "创建时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_239));
    const __VLS_242 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_243 = __VLS_asFunctionalComponent(__VLS_242, new __VLS_242({
        prop: "creator",
        label: "创建人",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_244 = __VLS_243({
        prop: "creator",
        label: "创建人",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_243));
    const __VLS_246 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_247 = __VLS_asFunctionalComponent(__VLS_246, new __VLS_246({
        label: "操作",
        width: "120",
        fixed: "right",
    }));
    const __VLS_248 = __VLS_247({
        label: "操作",
        width: "120",
        fixed: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_247));
    __VLS_249.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_249.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_250 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_251 = __VLS_asFunctionalComponent(__VLS_250, new __VLS_250({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }));
        const __VLS_252 = __VLS_251({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }, ...__VLS_functionalComponentArgsRest(__VLS_251));
        let __VLS_254;
        let __VLS_255;
        let __VLS_256;
        const __VLS_257 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.activeTab === 'outbound'))
                    return;
                __VLS_ctx.handleToolbarAction(`查看：${row.transferCode}`);
            }
        };
        __VLS_253.slots.default;
        var __VLS_253;
        const __VLS_258 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_259 = __VLS_asFunctionalComponent(__VLS_258, new __VLS_258({
            ...{ 'onClick': {} },
            text: true,
        }));
        const __VLS_260 = __VLS_259({
            ...{ 'onClick': {} },
            text: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_259));
        let __VLS_262;
        let __VLS_263;
        let __VLS_264;
        const __VLS_265 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.activeTab === 'outbound'))
                    return;
                __VLS_ctx.handleToolbarAction(`编辑：${row.transferCode}`);
            }
        };
        __VLS_261.slots.default;
        var __VLS_261;
    }
    var __VLS_249;
    var __VLS_201;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.outboundSelectedIds.length);
    const __VLS_266 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_267 = __VLS_asFunctionalComponent(__VLS_266, new __VLS_266({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.outboundCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.outboundFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }));
    const __VLS_268 = __VLS_267({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.outboundCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.outboundFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_267));
    let __VLS_270;
    let __VLS_271;
    let __VLS_272;
    const __VLS_273 = {
        onCurrentChange: (__VLS_ctx.handleOutboundPageChange)
    };
    const __VLS_274 = {
        onSizeChange: (__VLS_ctx.handlePageSizeChange)
    };
    var __VLS_269;
}
else {
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_275 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.inboundQuery),
    }));
    const __VLS_276 = __VLS_275({
        model: (__VLS_ctx.inboundQuery),
    }, ...__VLS_functionalComponentArgsRest(__VLS_275));
    __VLS_277.slots.default;
    const __VLS_278 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_279 = __VLS_asFunctionalComponent(__VLS_278, new __VLS_278({
        label: "调出门店",
    }));
    const __VLS_280 = __VLS_279({
        label: "调出门店",
    }, ...__VLS_functionalComponentArgsRest(__VLS_279));
    __VLS_281.slots.default;
    const __VLS_282 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_283 = __VLS_asFunctionalComponent(__VLS_282, new __VLS_282({
        modelValue: (__VLS_ctx.inboundQuery.outboundStore),
        data: (__VLS_ctx.storeTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }));
    const __VLS_284 = __VLS_283({
        modelValue: (__VLS_ctx.inboundQuery.outboundStore),
        data: (__VLS_ctx.storeTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        clearable: true,
        checkStrictly: true,
        defaultExpandAll: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_283));
    var __VLS_281;
    const __VLS_286 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_287 = __VLS_asFunctionalComponent(__VLS_286, new __VLS_286({
        label: "单据状态",
    }));
    const __VLS_288 = __VLS_287({
        label: "单据状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_287));
    __VLS_289.slots.default;
    const __VLS_290 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_291 = __VLS_asFunctionalComponent(__VLS_290, new __VLS_290({
        modelValue: (__VLS_ctx.inboundQuery.status),
        ...{ style: {} },
    }));
    const __VLS_292 = __VLS_291({
        modelValue: (__VLS_ctx.inboundQuery.status),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_291));
    __VLS_293.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.statusOptions))) {
        const __VLS_294 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_295 = __VLS_asFunctionalComponent(__VLS_294, new __VLS_294({
            key: (option || 'empty'),
            label: (option || '请选择'),
            value: (option),
        }));
        const __VLS_296 = __VLS_295({
            key: (option || 'empty'),
            label: (option || '请选择'),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_295));
    }
    var __VLS_293;
    var __VLS_289;
    const __VLS_298 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_299 = __VLS_asFunctionalComponent(__VLS_298, new __VLS_298({
        label: "复审状态",
    }));
    const __VLS_300 = __VLS_299({
        label: "复审状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_299));
    __VLS_301.slots.default;
    const __VLS_302 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_303 = __VLS_asFunctionalComponent(__VLS_302, new __VLS_302({
        modelValue: (__VLS_ctx.inboundQuery.reviewStatus),
        clearable: true,
        placeholder: "请选择复审状态",
        ...{ style: {} },
    }));
    const __VLS_304 = __VLS_303({
        modelValue: (__VLS_ctx.inboundQuery.reviewStatus),
        clearable: true,
        placeholder: "请选择复审状态",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_303));
    __VLS_305.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.reviewStatusOptions))) {
        const __VLS_306 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_307 = __VLS_asFunctionalComponent(__VLS_306, new __VLS_306({
            key: (option || 'empty'),
            label: (option || '请选择复审状态'),
            value: (option),
        }));
        const __VLS_308 = __VLS_307({
            key: (option || 'empty'),
            label: (option || '请选择复审状态'),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_307));
    }
    var __VLS_305;
    var __VLS_301;
    const __VLS_310 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_311 = __VLS_asFunctionalComponent(__VLS_310, new __VLS_310({
        label: "调拨单号",
    }));
    const __VLS_312 = __VLS_311({
        label: "调拨单号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_311));
    __VLS_313.slots.default;
    const __VLS_314 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_315 = __VLS_asFunctionalComponent(__VLS_314, new __VLS_314({
        modelValue: (__VLS_ctx.inboundQuery.transferCode),
        placeholder: "请输入调拨单号",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_316 = __VLS_315({
        modelValue: (__VLS_ctx.inboundQuery.transferCode),
        placeholder: "请输入调拨单号",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_315));
    var __VLS_313;
    const __VLS_318 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_319 = __VLS_asFunctionalComponent(__VLS_318, new __VLS_318({
        label: "调拨日期",
    }));
    const __VLS_320 = __VLS_319({
        label: "调拨日期",
    }, ...__VLS_functionalComponentArgsRest(__VLS_319));
    __VLS_321.slots.default;
    const __VLS_322 = {}.ElDatePicker;
    /** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
    // @ts-ignore
    const __VLS_323 = __VLS_asFunctionalComponent(__VLS_322, new __VLS_322({
        modelValue: (__VLS_ctx.inboundQuery.transferDate),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        placeholder: "请选择调拨日期",
        ...{ style: {} },
    }));
    const __VLS_324 = __VLS_323({
        modelValue: (__VLS_ctx.inboundQuery.transferDate),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        placeholder: "请选择调拨日期",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_323));
    var __VLS_321;
    const __VLS_326 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_327 = __VLS_asFunctionalComponent(__VLS_326, new __VLS_326({
        label: "物品",
    }));
    const __VLS_328 = __VLS_327({
        label: "物品",
    }, ...__VLS_functionalComponentArgsRest(__VLS_327));
    __VLS_329.slots.default;
    const __VLS_330 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_331 = __VLS_asFunctionalComponent(__VLS_330, new __VLS_330({
        modelValue: (__VLS_ctx.inboundQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_332 = __VLS_331({
        modelValue: (__VLS_ctx.inboundQuery.itemName),
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_331));
    __VLS_333.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
        const __VLS_334 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_335 = __VLS_asFunctionalComponent(__VLS_334, new __VLS_334({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_336 = __VLS_335({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_335));
    }
    var __VLS_333;
    var __VLS_329;
    const __VLS_338 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_339 = __VLS_asFunctionalComponent(__VLS_338, new __VLS_338({}));
    const __VLS_340 = __VLS_339({}, ...__VLS_functionalComponentArgsRest(__VLS_339));
    __VLS_341.slots.default;
    const __VLS_342 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_343 = __VLS_asFunctionalComponent(__VLS_342, new __VLS_342({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_344 = __VLS_343({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_343));
    let __VLS_346;
    let __VLS_347;
    let __VLS_348;
    const __VLS_349 = {
        onClick: (__VLS_ctx.handleInboundSearch)
    };
    __VLS_345.slots.default;
    const __VLS_350 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_351 = __VLS_asFunctionalComponent(__VLS_350, new __VLS_350({}));
    const __VLS_352 = __VLS_351({}, ...__VLS_functionalComponentArgsRest(__VLS_351));
    __VLS_353.slots.default;
    const __VLS_354 = {}.Search;
    /** @type {[typeof __VLS_components.Search, ]} */ ;
    // @ts-ignore
    const __VLS_355 = __VLS_asFunctionalComponent(__VLS_354, new __VLS_354({}));
    const __VLS_356 = __VLS_355({}, ...__VLS_functionalComponentArgsRest(__VLS_355));
    var __VLS_353;
    var __VLS_345;
    const __VLS_358 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_359 = __VLS_asFunctionalComponent(__VLS_358, new __VLS_358({
        ...{ 'onClick': {} },
    }));
    const __VLS_360 = __VLS_359({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_359));
    let __VLS_362;
    let __VLS_363;
    let __VLS_364;
    const __VLS_365 = {
        onClick: (__VLS_ctx.handleInboundReset)
    };
    __VLS_361.slots.default;
    const __VLS_366 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_367 = __VLS_asFunctionalComponent(__VLS_366, new __VLS_366({}));
    const __VLS_368 = __VLS_367({}, ...__VLS_functionalComponentArgsRest(__VLS_367));
    __VLS_369.slots.default;
    const __VLS_370 = {}.RefreshRight;
    /** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
    // @ts-ignore
    const __VLS_371 = __VLS_asFunctionalComponent(__VLS_370, new __VLS_370({}));
    const __VLS_372 = __VLS_371({}, ...__VLS_functionalComponentArgsRest(__VLS_371));
    var __VLS_369;
    var __VLS_361;
    var __VLS_341;
    var __VLS_277;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-toolbar" },
    });
    const __VLS_374 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_375 = __VLS_asFunctionalComponent(__VLS_374, new __VLS_374({
        ...{ 'onClick': {} },
    }));
    const __VLS_376 = __VLS_375({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_375));
    let __VLS_378;
    let __VLS_379;
    let __VLS_380;
    const __VLS_381 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量打印');
        }
    };
    __VLS_377.slots.default;
    const __VLS_382 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_383 = __VLS_asFunctionalComponent(__VLS_382, new __VLS_382({}));
    const __VLS_384 = __VLS_383({}, ...__VLS_functionalComponentArgsRest(__VLS_383));
    __VLS_385.slots.default;
    const __VLS_386 = {}.Printer;
    /** @type {[typeof __VLS_components.Printer, ]} */ ;
    // @ts-ignore
    const __VLS_387 = __VLS_asFunctionalComponent(__VLS_386, new __VLS_386({}));
    const __VLS_388 = __VLS_387({}, ...__VLS_functionalComponentArgsRest(__VLS_387));
    var __VLS_385;
    var __VLS_377;
    const __VLS_390 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_391 = __VLS_asFunctionalComponent(__VLS_390, new __VLS_390({
        ...{ 'onClick': {} },
    }));
    const __VLS_392 = __VLS_391({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_391));
    let __VLS_394;
    let __VLS_395;
    let __VLS_396;
    const __VLS_397 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量收货');
        }
    };
    __VLS_393.slots.default;
    var __VLS_393;
    const __VLS_398 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_399 = __VLS_asFunctionalComponent(__VLS_398, new __VLS_398({
        ...{ 'onClick': {} },
    }));
    const __VLS_400 = __VLS_399({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_399));
    let __VLS_402;
    let __VLS_403;
    let __VLS_404;
    const __VLS_405 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'outbound'))
                return;
            __VLS_ctx.handleToolbarAction('批量取消收货');
        }
    };
    __VLS_401.slots.default;
    var __VLS_401;
    const __VLS_406 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_407 = __VLS_asFunctionalComponent(__VLS_406, new __VLS_406({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.inboundPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_408 = __VLS_407({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.inboundPagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_407));
    let __VLS_410;
    let __VLS_411;
    let __VLS_412;
    const __VLS_413 = {
        onSelectionChange: (__VLS_ctx.handleInboundSelectionChange)
    };
    __VLS_409.slots.default;
    const __VLS_414 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_415 = __VLS_asFunctionalComponent(__VLS_414, new __VLS_414({
        type: "selection",
        width: "44",
        fixed: "left",
    }));
    const __VLS_416 = __VLS_415({
        type: "selection",
        width: "44",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_415));
    const __VLS_418 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_419 = __VLS_asFunctionalComponent(__VLS_418, new __VLS_418({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }));
    const __VLS_420 = __VLS_419({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_419));
    const __VLS_422 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_423 = __VLS_asFunctionalComponent(__VLS_422, new __VLS_422({
        prop: "transferCode",
        label: "调拨单号",
        minWidth: "150",
        showOverflowTooltip: true,
    }));
    const __VLS_424 = __VLS_423({
        prop: "transferCode",
        label: "调拨单号",
        minWidth: "150",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_423));
    const __VLS_426 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_427 = __VLS_asFunctionalComponent(__VLS_426, new __VLS_426({
        prop: "transferDate",
        label: "调拨日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }));
    const __VLS_428 = __VLS_427({
        prop: "transferDate",
        label: "调拨日期",
        minWidth: "110",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_427));
    const __VLS_430 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_431 = __VLS_asFunctionalComponent(__VLS_430, new __VLS_430({
        prop: "outboundStore",
        label: "调出门店",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_432 = __VLS_431({
        prop: "outboundStore",
        label: "调出门店",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_431));
    const __VLS_434 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_435 = __VLS_asFunctionalComponent(__VLS_434, new __VLS_434({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_436 = __VLS_435({
        prop: "amount",
        label: "金额",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_435));
    const __VLS_438 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_439 = __VLS_asFunctionalComponent(__VLS_438, new __VLS_438({
        prop: "status",
        label: "状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_440 = __VLS_439({
        prop: "status",
        label: "状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_439));
    const __VLS_442 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_443 = __VLS_asFunctionalComponent(__VLS_442, new __VLS_442({
        prop: "reviewStatus",
        label: "复审状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_444 = __VLS_443({
        prop: "reviewStatus",
        label: "复审状态",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_443));
    const __VLS_446 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_447 = __VLS_asFunctionalComponent(__VLS_446, new __VLS_446({
        prop: "createdAt",
        label: "创建时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }));
    const __VLS_448 = __VLS_447({
        prop: "createdAt",
        label: "创建时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_447));
    const __VLS_450 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_451 = __VLS_asFunctionalComponent(__VLS_450, new __VLS_450({
        prop: "creator",
        label: "创建人",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_452 = __VLS_451({
        prop: "creator",
        label: "创建人",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_451));
    const __VLS_454 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_455 = __VLS_asFunctionalComponent(__VLS_454, new __VLS_454({
        label: "操作",
        width: "120",
        fixed: "right",
    }));
    const __VLS_456 = __VLS_455({
        label: "操作",
        width: "120",
        fixed: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_455));
    __VLS_457.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_457.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_458 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_459 = __VLS_asFunctionalComponent(__VLS_458, new __VLS_458({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }));
        const __VLS_460 = __VLS_459({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }, ...__VLS_functionalComponentArgsRest(__VLS_459));
        let __VLS_462;
        let __VLS_463;
        let __VLS_464;
        const __VLS_465 = {
            onClick: (...[$event]) => {
                if (!!(__VLS_ctx.activeTab === 'outbound'))
                    return;
                __VLS_ctx.handleToolbarAction(`查看：${row.transferCode}`);
            }
        };
        __VLS_461.slots.default;
        var __VLS_461;
        const __VLS_466 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_467 = __VLS_asFunctionalComponent(__VLS_466, new __VLS_466({
            ...{ 'onClick': {} },
            text: true,
        }));
        const __VLS_468 = __VLS_467({
            ...{ 'onClick': {} },
            text: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_467));
        let __VLS_470;
        let __VLS_471;
        let __VLS_472;
        const __VLS_473 = {
            onClick: (...[$event]) => {
                if (!!(__VLS_ctx.activeTab === 'outbound'))
                    return;
                __VLS_ctx.handleToolbarAction(`编辑：${row.transferCode}`);
            }
        };
        __VLS_469.slots.default;
        var __VLS_469;
    }
    var __VLS_457;
    var __VLS_409;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.inboundSelectedIds.length);
    const __VLS_474 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_475 = __VLS_asFunctionalComponent(__VLS_474, new __VLS_474({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.inboundCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.inboundFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }));
    const __VLS_476 = __VLS_475({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.inboundCurrentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.inboundFilteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_475));
    let __VLS_478;
    let __VLS_479;
    let __VLS_480;
    const __VLS_481 = {
        onCurrentChange: (__VLS_ctx.handleInboundPageChange)
    };
    const __VLS_482 = {
        onSizeChange: (__VLS_ctx.handlePageSizeChange)
    };
    var __VLS_477;
}
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['store-transfer-page']} */ ;
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
            Delete: Delete,
            Plus: Plus,
            Printer: Printer,
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            PageTabsLayout: PageTabsLayout,
            tabs: tabs,
            activeTab: activeTab,
            statusOptions: statusOptions,
            reviewStatusOptions: reviewStatusOptions,
            itemOptions: itemOptions,
            storeTree: storeTree,
            outboundQuery: outboundQuery,
            inboundQuery: inboundQuery,
            outboundCurrentPage: outboundCurrentPage,
            inboundCurrentPage: inboundCurrentPage,
            pageSize: pageSize,
            outboundSelectedIds: outboundSelectedIds,
            inboundSelectedIds: inboundSelectedIds,
            outboundFilteredRows: outboundFilteredRows,
            inboundFilteredRows: inboundFilteredRows,
            outboundPagedRows: outboundPagedRows,
            inboundPagedRows: inboundPagedRows,
            handleOutboundSearch: handleOutboundSearch,
            handleInboundSearch: handleInboundSearch,
            handleOutboundReset: handleOutboundReset,
            handleInboundReset: handleInboundReset,
            handleToolbarAction: handleToolbarAction,
            handleOutboundSelectionChange: handleOutboundSelectionChange,
            handleInboundSelectionChange: handleInboundSelectionChange,
            handleOutboundPageChange: handleOutboundPageChange,
            handleInboundPageChange: handleInboundPageChange,
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
