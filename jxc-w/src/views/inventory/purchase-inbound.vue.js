/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { batchApprovePurchaseInboundApi, batchDeletePurchaseInboundApi, batchUnapprovePurchaseInboundApi, deletePurchaseInboundApi, fetchPurchaseInboundPageApi, fetchPurchaseInboundPermissionApi, } from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';
const timeTypeOptions = ['入库日期', '创建时间'];
const documentStatusOptions = ['草稿', '已提交', '已审核'];
const reviewStatusOptions = ['未复审', '已复审'];
const reconciliationStatusOptions = ['未对账', '部分对账', '已对账'];
const splitStatusOptions = ['未分账', '已分账'];
const invoiceStatusOptions = ['未开票', '部分开票', '已开票'];
const printStatusOptions = ['全部', '未打印', '已打印'];
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const supplierTree = [
    {
        value: 'supplier-group',
        label: '供应商组',
        children: [
            { value: '鲜达食品', label: '鲜达食品' },
            { value: '优选农场', label: '优选农场' },
            { value: '盒马包材', label: '盒马包材' },
        ],
    },
];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();
const router = useRouter();
const query = reactive({
    timeType: '入库日期',
    dateRange: [],
    warehouse: '',
    documentCode: '',
    supplier: '',
    itemName: '',
    documentStatus: '',
    reviewStatus: '',
    reconciliationStatus: '',
    splitStatus: '',
    upstreamCode: '',
    invoiceStatus: '',
    inspectionCount: '',
    printStatus: '全部',
    remark: '',
});
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedIds = ref([]);
const tableData = ref([]);
const loading = ref(false);
const canCreate = ref(false);
const canUpdate = ref(false);
const canDelete = ref(false);
const canApprove = ref(false);
const canUnapprove = ref(false);
onMounted(() => {
    void loadWarehouseTree();
});
watch(() => sessionStore.currentOrgId, () => {
    void loadWarehouseTree();
});
const resolveOrgId = () => {
    const orgId = (sessionStore.currentOrgId ?? '').trim();
    if (!orgId) {
        return undefined;
    }
    if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
        return orgId;
    }
    return undefined;
};
const fetchTableData = async () => {
    loading.value = true;
    try {
        const result = await fetchPurchaseInboundPageApi({
            pageNo: currentPage.value,
            pageSize: pageSize.value,
            timeType: query.timeType,
            startDate: query.dateRange[0],
            endDate: query.dateRange[1],
            warehouse: query.warehouse || undefined,
            documentCode: query.documentCode || undefined,
            supplier: query.supplier || undefined,
            itemName: query.itemName || undefined,
            documentStatus: query.documentStatus || undefined,
            reviewStatus: query.reviewStatus || undefined,
            reconciliationStatus: query.reconciliationStatus || undefined,
            splitStatus: query.splitStatus || undefined,
            upstreamCode: query.upstreamCode || undefined,
            invoiceStatus: query.invoiceStatus || undefined,
            inspectionCount: query.inspectionCount || undefined,
            printStatus: query.printStatus === '全部' ? undefined : query.printStatus,
            remark: query.remark || undefined,
        }, resolveOrgId());
        tableData.value = result.list;
        total.value = result.total;
        selectedIds.value = [];
    }
    finally {
        loading.value = false;
    }
};
const loadPermission = async () => {
    try {
        const result = await fetchPurchaseInboundPermissionApi(resolveOrgId());
        canCreate.value = Boolean(result.canCreate);
        canUpdate.value = Boolean(result.canUpdate);
        canDelete.value = Boolean(result.canDelete);
        canApprove.value = Boolean(result.canApprove);
        canUnapprove.value = Boolean(result.canUnapprove);
    }
    catch {
        canCreate.value = false;
        canUpdate.value = false;
        canDelete.value = false;
        canApprove.value = false;
        canUnapprove.value = false;
    }
};
const handleSearch = async () => {
    currentPage.value = 1;
    await fetchTableData();
};
const handleReset = async () => {
    query.timeType = '入库日期';
    query.dateRange = [];
    query.warehouse = '';
    query.documentCode = '';
    query.supplier = '';
    query.itemName = '';
    query.documentStatus = '';
    query.reviewStatus = '';
    query.reconciliationStatus = '';
    query.splitStatus = '';
    query.upstreamCode = '';
    query.invoiceStatus = '';
    query.inspectionCount = '';
    query.printStatus = '全部';
    query.remark = '';
    currentPage.value = 1;
    await fetchTableData();
};
const handleToolbarAction = async (action) => {
    if (action === '新增') {
        router.push('/inventory/1/2/create');
        return;
    }
    if (action === '批量删除') {
        if (!selectedIds.value.length) {
            ElMessage.warning('请先选择单据');
            return;
        }
        try {
            await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条单据吗？`, '删除确认', {
                confirmButtonText: '删除',
                cancelButtonText: '取消',
                type: 'warning',
            });
        }
        catch {
            return;
        }
        await batchDeletePurchaseInboundApi(selectedIds.value, resolveOrgId());
        ElMessage.success('批量删除成功');
        await fetchTableData();
        return;
    }
    if (action === '批量审核') {
        if (!selectedIds.value.length) {
            ElMessage.warning('请先选择单据');
            return;
        }
        await batchApprovePurchaseInboundApi(selectedIds.value, resolveOrgId());
        ElMessage.success('批量审核成功');
        await fetchTableData();
        return;
    }
    if (action === '批量取消审核') {
        if (!selectedIds.value.length) {
            ElMessage.warning('请先选择单据');
            return;
        }
        await batchUnapprovePurchaseInboundApi(selectedIds.value, resolveOrgId());
        ElMessage.success('批量取消审核成功');
        await fetchTableData();
        return;
    }
    ElMessage.info(`${action}功能待接入`);
};
const handleDelete = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除单据“${row.documentCode}”吗？`, '删除确认', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning',
        });
    }
    catch {
        return;
    }
    await deletePurchaseInboundApi(row.id, resolveOrgId());
    ElMessage.success('删除成功');
    await fetchTableData();
};
const handleSelectionChange = (rows) => {
    selectedIds.value = rows.map((row) => row.id);
};
const handleView = (row) => {
    router.push(`/inventory/1/2/view/${row.id}`);
};
const handleEdit = (row) => {
    router.push(`/inventory/1/2/edit/${row.id}`);
};
const handlePageChange = async (page) => {
    currentPage.value = page;
    await fetchTableData();
};
const handlePageSizeChange = async (size) => {
    pageSize.value = size;
    currentPage.value = 1;
    await fetchTableData();
};
onMounted(async () => {
    await loadPermission();
    await fetchTableData();
});
watch(() => sessionStore.currentOrgId, async () => {
    await loadPermission();
    await fetchTableData();
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
    label: "开始日期~结束日期",
}));
const __VLS_17 = __VLS_16({
    label: "开始日期~结束日期",
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
    label: "仓库",
}));
const __VLS_25 = __VLS_24({
    label: "仓库",
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
const __VLS_27 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_29 = __VLS_28({
    modelValue: (__VLS_ctx.query.warehouse),
    data: (__VLS_ctx.warehouseTree),
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
    label: "供应商",
}));
const __VLS_41 = __VLS_40({
    label: "供应商",
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
__VLS_42.slots.default;
const __VLS_43 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    modelValue: (__VLS_ctx.query.supplier),
    data: (__VLS_ctx.supplierTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    clearable: true,
    checkStrictly: true,
    defaultExpandAll: true,
    ...{ style: {} },
}));
const __VLS_45 = __VLS_44({
    modelValue: (__VLS_ctx.query.supplier),
    data: (__VLS_ctx.supplierTree),
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
    clearable: true,
    ...{ style: {} },
}));
const __VLS_77 = __VLS_76({
    modelValue: (__VLS_ctx.query.reviewStatus),
    clearable: true,
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
    label: "对账状态",
}));
const __VLS_85 = __VLS_84({
    label: "对账状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_84));
__VLS_86.slots.default;
const __VLS_87 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({
    modelValue: (__VLS_ctx.query.reconciliationStatus),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_89 = __VLS_88({
    modelValue: (__VLS_ctx.query.reconciliationStatus),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_88));
__VLS_90.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.reconciliationStatusOptions))) {
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
    label: "供应商分账",
}));
const __VLS_97 = __VLS_96({
    label: "供应商分账",
}, ...__VLS_functionalComponentArgsRest(__VLS_96));
__VLS_98.slots.default;
const __VLS_99 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({
    modelValue: (__VLS_ctx.query.splitStatus),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_101 = __VLS_100({
    modelValue: (__VLS_ctx.query.splitStatus),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_100));
__VLS_102.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.splitStatusOptions))) {
    const __VLS_103 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_105 = __VLS_104({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_104));
}
var __VLS_102;
var __VLS_98;
const __VLS_107 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
    label: "上游单据号",
}));
const __VLS_109 = __VLS_108({
    label: "上游单据号",
}, ...__VLS_functionalComponentArgsRest(__VLS_108));
__VLS_110.slots.default;
const __VLS_111 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({
    modelValue: (__VLS_ctx.query.upstreamCode),
    placeholder: "请输入上游单据号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_113 = __VLS_112({
    modelValue: (__VLS_ctx.query.upstreamCode),
    placeholder: "请输入上游单据号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_112));
var __VLS_110;
const __VLS_115 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({
    label: "发票状态",
}));
const __VLS_117 = __VLS_116({
    label: "发票状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_116));
__VLS_118.slots.default;
const __VLS_119 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({
    modelValue: (__VLS_ctx.query.invoiceStatus),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_121 = __VLS_120({
    modelValue: (__VLS_ctx.query.invoiceStatus),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_120));
__VLS_122.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.invoiceStatusOptions))) {
    const __VLS_123 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_125 = __VLS_124({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_124));
}
var __VLS_122;
var __VLS_118;
const __VLS_127 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
    label: "质检次数",
}));
const __VLS_129 = __VLS_128({
    label: "质检次数",
}, ...__VLS_functionalComponentArgsRest(__VLS_128));
__VLS_130.slots.default;
const __VLS_131 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({
    modelValue: (__VLS_ctx.query.inspectionCount),
    placeholder: "请输入质检次数",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_133 = __VLS_132({
    modelValue: (__VLS_ctx.query.inspectionCount),
    placeholder: "请输入质检次数",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_132));
var __VLS_130;
const __VLS_135 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({
    label: "打印状态",
}));
const __VLS_137 = __VLS_136({
    label: "打印状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_136));
__VLS_138.slots.default;
const __VLS_139 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
    modelValue: (__VLS_ctx.query.printStatus),
    ...{ style: {} },
}));
const __VLS_141 = __VLS_140({
    modelValue: (__VLS_ctx.query.printStatus),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_140));
__VLS_142.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.printStatusOptions))) {
    const __VLS_143 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_145 = __VLS_144({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_144));
}
var __VLS_142;
var __VLS_138;
const __VLS_147 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({
    label: "备注",
}));
const __VLS_149 = __VLS_148({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_148));
__VLS_150.slots.default;
const __VLS_151 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_153 = __VLS_152({
    modelValue: (__VLS_ctx.query.remark),
    placeholder: "请输入备注",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_152));
var __VLS_150;
const __VLS_155 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({}));
const __VLS_157 = __VLS_156({}, ...__VLS_functionalComponentArgsRest(__VLS_156));
__VLS_158.slots.default;
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
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_162.slots.default;
const __VLS_167 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({}));
const __VLS_169 = __VLS_168({}, ...__VLS_functionalComponentArgsRest(__VLS_168));
__VLS_170.slots.default;
const __VLS_171 = {}.Search;
/** @type {[typeof __VLS_components.Search, ]} */ ;
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
    onClick: (__VLS_ctx.handleReset)
};
__VLS_178.slots.default;
const __VLS_183 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({}));
const __VLS_185 = __VLS_184({}, ...__VLS_functionalComponentArgsRest(__VLS_184));
__VLS_186.slots.default;
const __VLS_187 = {}.RefreshRight;
/** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({}));
const __VLS_189 = __VLS_188({}, ...__VLS_functionalComponentArgsRest(__VLS_188));
var __VLS_186;
var __VLS_178;
var __VLS_158;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
if (__VLS_ctx.canCreate) {
    const __VLS_191 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_193 = __VLS_192({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_192));
    let __VLS_195;
    let __VLS_196;
    let __VLS_197;
    const __VLS_198 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.canCreate))
                return;
            __VLS_ctx.handleToolbarAction('新增');
        }
    };
    __VLS_194.slots.default;
    const __VLS_199 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({}));
    const __VLS_201 = __VLS_200({}, ...__VLS_functionalComponentArgsRest(__VLS_200));
    __VLS_202.slots.default;
    const __VLS_203 = {}.Plus;
    /** @type {[typeof __VLS_components.Plus, ]} */ ;
    // @ts-ignore
    const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({}));
    const __VLS_205 = __VLS_204({}, ...__VLS_functionalComponentArgsRest(__VLS_204));
    var __VLS_202;
    var __VLS_194;
}
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
        __VLS_ctx.handleToolbarAction('批量打印');
    }
};
__VLS_210.slots.default;
const __VLS_215 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({}));
const __VLS_217 = __VLS_216({}, ...__VLS_functionalComponentArgsRest(__VLS_216));
__VLS_218.slots.default;
const __VLS_219 = {}.Printer;
/** @type {[typeof __VLS_components.Printer, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({}));
const __VLS_221 = __VLS_220({}, ...__VLS_functionalComponentArgsRest(__VLS_220));
var __VLS_218;
var __VLS_210;
if (__VLS_ctx.canDelete) {
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
            if (!(__VLS_ctx.canDelete))
                return;
            __VLS_ctx.handleToolbarAction('批量删除');
        }
    };
    __VLS_226.slots.default;
    const __VLS_231 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({}));
    const __VLS_233 = __VLS_232({}, ...__VLS_functionalComponentArgsRest(__VLS_232));
    __VLS_234.slots.default;
    const __VLS_235 = {}.Delete;
    /** @type {[typeof __VLS_components.Delete, ]} */ ;
    // @ts-ignore
    const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({}));
    const __VLS_237 = __VLS_236({}, ...__VLS_functionalComponentArgsRest(__VLS_236));
    var __VLS_234;
    var __VLS_226;
}
if (__VLS_ctx.canApprove) {
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
            if (!(__VLS_ctx.canApprove))
                return;
            __VLS_ctx.handleToolbarAction('批量审核');
        }
    };
    __VLS_242.slots.default;
    var __VLS_242;
}
if (__VLS_ctx.canUnapprove) {
    const __VLS_247 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
        ...{ 'onClick': {} },
    }));
    const __VLS_249 = __VLS_248({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_248));
    let __VLS_251;
    let __VLS_252;
    let __VLS_253;
    const __VLS_254 = {
        onClick: (...[$event]) => {
            if (!(__VLS_ctx.canUnapprove))
                return;
            __VLS_ctx.handleToolbarAction('批量取消审核');
        }
    };
    __VLS_250.slots.default;
    var __VLS_250;
}
const __VLS_255 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
    ...{ 'onClick': {} },
}));
const __VLS_257 = __VLS_256({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_256));
let __VLS_259;
let __VLS_260;
let __VLS_261;
const __VLS_262 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量导出单据列表');
    }
};
__VLS_258.slots.default;
var __VLS_258;
const __VLS_263 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_264 = __VLS_asFunctionalComponent(__VLS_263, new __VLS_263({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.tableData),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_265 = __VLS_264({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.tableData),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_264));
let __VLS_267;
let __VLS_268;
let __VLS_269;
const __VLS_270 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_266.slots.default;
const __VLS_271 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_272 = __VLS_asFunctionalComponent(__VLS_271, new __VLS_271({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_273 = __VLS_272({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_272));
const __VLS_275 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_276 = __VLS_asFunctionalComponent(__VLS_275, new __VLS_275({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_277 = __VLS_276({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_276));
const __VLS_279 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_280 = __VLS_asFunctionalComponent(__VLS_279, new __VLS_279({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_281 = __VLS_280({
    prop: "documentCode",
    label: "单据编号",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_280));
const __VLS_283 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_284 = __VLS_asFunctionalComponent(__VLS_283, new __VLS_283({
    prop: "inboundDate",
    label: "入库日期",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_285 = __VLS_284({
    prop: "inboundDate",
    label: "入库日期",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_284));
const __VLS_287 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_288 = __VLS_asFunctionalComponent(__VLS_287, new __VLS_287({
    prop: "upstreamCode",
    label: "上游单据号",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_289 = __VLS_288({
    prop: "upstreamCode",
    label: "上游单据号",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_288));
const __VLS_291 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_292 = __VLS_asFunctionalComponent(__VLS_291, new __VLS_291({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_293 = __VLS_292({
    prop: "warehouse",
    label: "仓库",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_292));
const __VLS_295 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_296 = __VLS_asFunctionalComponent(__VLS_295, new __VLS_295({
    prop: "supplier",
    label: "供应商",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_297 = __VLS_296({
    prop: "supplier",
    label: "供应商",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_296));
const __VLS_299 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_300 = __VLS_asFunctionalComponent(__VLS_299, new __VLS_299({
    prop: "amountTaxIncluded",
    label: "金额（含税）",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_301 = __VLS_300({
    prop: "amountTaxIncluded",
    label: "金额（含税）",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_300));
const __VLS_303 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_304 = __VLS_asFunctionalComponent(__VLS_303, new __VLS_303({
    prop: "status",
    label: "状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_305 = __VLS_304({
    prop: "status",
    label: "状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_304));
const __VLS_307 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_308 = __VLS_asFunctionalComponent(__VLS_307, new __VLS_307({
    prop: "reviewStatus",
    label: "复审状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_309 = __VLS_308({
    prop: "reviewStatus",
    label: "复审状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_308));
const __VLS_311 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_312 = __VLS_asFunctionalComponent(__VLS_311, new __VLS_311({
    prop: "reconciliationStatus",
    label: "对账状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_313 = __VLS_312({
    prop: "reconciliationStatus",
    label: "对账状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_312));
const __VLS_315 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_316 = __VLS_asFunctionalComponent(__VLS_315, new __VLS_315({
    prop: "invoiceStatus",
    label: "发票状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_317 = __VLS_316({
    prop: "invoiceStatus",
    label: "发票状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_316));
const __VLS_319 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_320 = __VLS_asFunctionalComponent(__VLS_319, new __VLS_319({
    prop: "printStatus",
    label: "打印状态",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_321 = __VLS_320({
    prop: "printStatus",
    label: "打印状态",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_320));
const __VLS_323 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_324 = __VLS_asFunctionalComponent(__VLS_323, new __VLS_323({
    prop: "inspectionCount",
    label: "质检次数",
    minWidth: "90",
    showOverflowTooltip: true,
}));
const __VLS_325 = __VLS_324({
    prop: "inspectionCount",
    label: "质检次数",
    minWidth: "90",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_324));
const __VLS_327 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_328 = __VLS_asFunctionalComponent(__VLS_327, new __VLS_327({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_329 = __VLS_328({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_328));
const __VLS_331 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_332 = __VLS_asFunctionalComponent(__VLS_331, new __VLS_331({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_333 = __VLS_332({
    prop: "creator",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_332));
const __VLS_335 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_336 = __VLS_asFunctionalComponent(__VLS_335, new __VLS_335({
    prop: "remark",
    label: "备注",
    minWidth: "180",
    showOverflowTooltip: true,
}));
const __VLS_337 = __VLS_336({
    prop: "remark",
    label: "备注",
    minWidth: "180",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_336));
const __VLS_339 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_340 = __VLS_asFunctionalComponent(__VLS_339, new __VLS_339({
    label: "操作",
    width: "160",
    fixed: "right",
}));
const __VLS_341 = __VLS_340({
    label: "操作",
    width: "160",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_340));
__VLS_342.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_342.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_343 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_344 = __VLS_asFunctionalComponent(__VLS_343, new __VLS_343({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_345 = __VLS_344({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_344));
    let __VLS_347;
    let __VLS_348;
    let __VLS_349;
    const __VLS_350 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_346.slots.default;
    var __VLS_346;
    if (__VLS_ctx.canUpdate) {
        const __VLS_351 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_352 = __VLS_asFunctionalComponent(__VLS_351, new __VLS_351({
            ...{ 'onClick': {} },
            text: true,
        }));
        const __VLS_353 = __VLS_352({
            ...{ 'onClick': {} },
            text: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_352));
        let __VLS_355;
        let __VLS_356;
        let __VLS_357;
        const __VLS_358 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.canUpdate))
                    return;
                __VLS_ctx.handleEdit(row);
            }
        };
        __VLS_354.slots.default;
        var __VLS_354;
    }
    if (__VLS_ctx.canDelete) {
        const __VLS_359 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_360 = __VLS_asFunctionalComponent(__VLS_359, new __VLS_359({
            ...{ 'onClick': {} },
            text: true,
            type: "danger",
        }));
        const __VLS_361 = __VLS_360({
            ...{ 'onClick': {} },
            text: true,
            type: "danger",
        }, ...__VLS_functionalComponentArgsRest(__VLS_360));
        let __VLS_363;
        let __VLS_364;
        let __VLS_365;
        const __VLS_366 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.canDelete))
                    return;
                __VLS_ctx.handleDelete(row);
            }
        };
        __VLS_362.slots.default;
        var __VLS_362;
    }
}
var __VLS_342;
var __VLS_266;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedIds.length);
const __VLS_367 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_368 = __VLS_asFunctionalComponent(__VLS_367, new __VLS_367({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.total),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}));
const __VLS_369 = __VLS_368({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.total),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_368));
let __VLS_371;
let __VLS_372;
let __VLS_373;
const __VLS_374 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_375 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_370;
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
            Delete: Delete,
            Plus: Plus,
            Printer: Printer,
            RefreshRight: RefreshRight,
            Search: Search,
            CommonQuerySection: CommonQuerySection,
            timeTypeOptions: timeTypeOptions,
            documentStatusOptions: documentStatusOptions,
            reviewStatusOptions: reviewStatusOptions,
            reconciliationStatusOptions: reconciliationStatusOptions,
            splitStatusOptions: splitStatusOptions,
            invoiceStatusOptions: invoiceStatusOptions,
            printStatusOptions: printStatusOptions,
            warehouseTree: warehouseTree,
            supplierTree: supplierTree,
            itemOptions: itemOptions,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            total: total,
            selectedIds: selectedIds,
            tableData: tableData,
            loading: loading,
            canCreate: canCreate,
            canUpdate: canUpdate,
            canDelete: canDelete,
            canApprove: canApprove,
            canUnapprove: canUnapprove,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleDelete: handleDelete,
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
