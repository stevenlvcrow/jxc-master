/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { bindWorkflowProcessStoresApi, createWorkflowProcessApi, deleteWorkflowProcessApi, deleteWorkflowConfigApi, fetchWorkflowProcessStoresApi, fetchWorkflowProcessesApi, fetchWorkflowPublishHistoryManageApi, publishWorkflowConfigApi, updateWorkflowProcessApi, } from '@/api/modules/workflow';
import { useSessionStore } from '@/stores/session';
const sessionStore = useSessionStore();
const router = useRouter();
const loading = ref(false);
const rows = ref([]);
const histories = ref([]);
const query = reactive({
    keyword: '',
});
const processDialogVisible = ref(false);
const processSubmitting = ref(false);
const editingBusinessId = ref(null);
const workflowBusinessOptions = [
    { processCode: 'PURCHASE_INBOUND', businessName: '采购入库流程' },
];
const processForm = reactive({
    processCode: '',
    businessName: '',
});
const adoptingBusinessCode = ref('');
const publishingConfigId = ref(null);
const deletingConfigId = ref(null);
const adoptedTemplateMap = reactive({});
const expandedRowKeys = ref([]);
const bindStoreDialogVisible = ref(false);
const bindStoreSubmitting = ref(false);
const storeLoadWarned = ref(false);
const bindingStoreBusinessId = ref(null);
const storeOptions = ref([]);
const bindStoreForm = reactive({
    storeIds: [],
});
const bindStoreSearch = ref('');
const bindStoreSelectedRows = ref([]);
const bindStoreTableRef = ref(null);
const filteredStoreOptions = computed(() => {
    const kw = bindStoreSearch.value.trim().toLowerCase();
    if (!kw)
        return storeOptions.value;
    return storeOptions.value.filter((item) => `${item.storeName}（${item.storeCode}）`.toLowerCase().includes(kw));
});
const historyMap = computed(() => {
    const grouped = new Map();
    histories.value.forEach((item) => {
        const list = grouped.get(item.businessCode) ?? [];
        list.push(item);
        grouped.set(item.businessCode, list);
    });
    grouped.forEach((list) => {
        list.sort((a, b) => {
            const byVersion = (b.versionNo ?? 0) - (a.versionNo ?? 0);
            if (byVersion !== 0) {
                return byVersion;
            }
            return String(b.savedAt).localeCompare(String(a.savedAt));
        });
    });
    return grouped;
});
const filteredRows = computed(() => {
    const keyword = query.keyword.trim().toLowerCase();
    if (!keyword) {
        return rows.value;
    }
    return rows.value.filter((item) => {
        const baseMatched = `${item.processId}${item.businessName}${item.createdAt}`.toLowerCase().includes(keyword);
        if (baseMatched) {
            return true;
        }
        const childRows = historyMap.value.get(item.processId) ?? [];
        return childRows.some((child) => `${child.workflowCode}${child.workflowName}${child.savedAt}${child.versionNo}`.toLowerCase().includes(keyword));
    });
});
const getHistoryRows = (businessCode) => historyMap.value.get(businessCode) ?? [];
const resetProcessForm = () => {
    processForm.processCode = '';
    processForm.businessName = '';
    editingBusinessId.value = null;
};
const syncBusinessNameFromCode = () => {
    const selected = workflowBusinessOptions.find((item) => item.processCode === processForm.processCode);
    processForm.businessName = selected?.businessName ?? '';
};
const openCreateBusiness = () => {
    resetProcessForm();
    processForm.processCode = workflowBusinessOptions[0]?.processCode ?? '';
    syncBusinessNameFromCode();
    processDialogVisible.value = true;
};
const openEditBusiness = (business) => {
    editingBusinessId.value = business.id;
    processForm.processCode = business.processId;
    processForm.businessName = business.businessName;
    processDialogVisible.value = true;
};
const submitBusinessForm = async () => {
    if (!processForm.processCode.trim()) {
        ElMessage.warning('请选择业务ID');
        return;
    }
    const selectedBusiness = workflowBusinessOptions.find((item) => item.processCode === processForm.processCode);
    if (!selectedBusiness) {
        ElMessage.warning('请选择有效的业务名称');
        return;
    }
    if (!selectedBusiness.businessName.trim()) {
        ElMessage.warning('请填写业务名称');
        return;
    }
    processSubmitting.value = true;
    try {
        if (editingBusinessId.value) {
            const target = rows.value.find((item) => item.id === editingBusinessId.value);
            if (!target) {
                ElMessage.warning('业务不存在');
                return;
            }
            await updateWorkflowProcessApi(target.id, {
                orgId: sessionStore.currentOrgId,
                processCode: target.processId,
                businessName: selectedBusiness.businessName,
                templateId: target.templateId,
            });
            ElMessage.success('业务更新成功');
        }
        else {
            await createWorkflowProcessApi({
                orgId: sessionStore.currentOrgId,
                processCode: selectedBusiness.processCode,
                businessName: selectedBusiness.businessName,
            });
            ElMessage.success('业务新增成功');
        }
        processDialogVisible.value = false;
        resetProcessForm();
        await loadRows();
    }
    finally {
        processSubmitting.value = false;
    }
};
const removeBusiness = async (business) => {
    try {
        await ElMessageBox.confirm(`确认删除业务“${business.businessName}”吗？`, '删除确认', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
    }
    catch {
        return;
    }
    await deleteWorkflowProcessApi(business.id, sessionStore.currentOrgId);
    delete adoptedTemplateMap[business.processId];
    ElMessage.success('业务删除成功');
    await loadRows();
};
const loadRows = async () => {
    loading.value = true;
    try {
        const [processRows, historyRows] = await Promise.all([
            fetchWorkflowProcessesApi(sessionStore.currentOrgId),
            fetchWorkflowPublishHistoryManageApi(sessionStore.currentOrgId, 500),
        ]);
        rows.value = processRows;
        histories.value = historyRows;
        const publishedWorkflowMap = new Map();
        historyRows.forEach((item) => {
            if (item.status !== 'PUBLISHED') {
                return;
            }
            const workflowSet = publishedWorkflowMap.get(item.businessCode) ?? new Set();
            workflowSet.add(item.workflowCode);
            publishedWorkflowMap.set(item.businessCode, workflowSet);
        });
        processRows.forEach((item) => {
            const templateId = item.templateId ?? '';
            const publishedWorkflowSet = publishedWorkflowMap.get(item.processId) ?? new Set();
            if (templateId && !publishedWorkflowSet.has(templateId)) {
                adoptedTemplateMap[item.processId] = '';
                item.templateId = undefined;
                return;
            }
            adoptedTemplateMap[item.processId] = templateId;
        });
        expandedRowKeys.value = processRows.length ? [processRows[0].id] : [];
    }
    finally {
        loading.value = false;
    }
};
const loadStoreOptions = async () => {
    try {
        storeOptions.value = await fetchWorkflowProcessStoresApi(sessionStore.currentOrgId);
    }
    catch {
        storeOptions.value = [];
        if (!storeLoadWarned.value) {
            ElMessage.warning('门店列表加载失败，请确认登录状态或联系管理员初始化数据');
            storeLoadWarned.value = true;
        }
    }
};
const openWorkflowConfig = (payload) => {
    router.push({
        path: '/group/workflow-config',
        query: payload?.businessCode
            ? {
                businessCode: payload.businessCode,
                copyFromWorkflowCode: payload.copyFromWorkflowCode,
                viewWorkflowCode: payload.viewWorkflowCode,
            }
            : undefined,
    });
};
const publishConfig = async (business, row) => {
    if (row.status === 'PUBLISHED') {
        return;
    }
    try {
        await ElMessageBox.confirm(`确认发布流程版本“${row.workflowCode}”吗？`, '发布确认', {
            type: 'warning',
            confirmButtonText: '发布',
            cancelButtonText: '取消',
        });
    }
    catch {
        return;
    }
    publishingConfigId.value = row.id;
    try {
        await publishWorkflowConfigApi({
            orgId: sessionStore.currentOrgId,
            businessCode: business.processId,
            workflowCode: row.workflowCode,
        });
        ElMessage.success('流程发布成功');
        await loadRows();
    }
    finally {
        publishingConfigId.value = null;
    }
};
const openBindStores = async (business) => {
    bindingStoreBusinessId.value = business.id;
    bindStoreSearch.value = '';
    if (!storeOptions.value.length) {
        await loadStoreOptions();
    }
    const existingIds = Array.isArray(business.storeIds) ? [...business.storeIds] : [];
    const selected = storeOptions.value.filter((s) => existingIds.includes(s.storeId));
    bindStoreSelectedRows.value = [...selected];
    bindStoreDialogVisible.value = true;
    await nextTick();
    if (bindStoreTableRef.value) {
        storeOptions.value.forEach((row) => {
            bindStoreTableRef.value.toggleRowSelection(row, selected.some((s) => s.storeId === row.storeId));
        });
    }
};
const onBindStoreSelectionChange = (rows) => {
    bindStoreSelectedRows.value = rows;
};
const submitBindStores = async () => {
    if (!bindingStoreBusinessId.value) {
        return;
    }
    const storeIds = bindStoreSelectedRows.value.map((s) => s.storeId);
    bindStoreSubmitting.value = true;
    try {
        await bindWorkflowProcessStoresApi(bindingStoreBusinessId.value, storeIds, sessionStore.currentOrgId);
        ElMessage.success('门店绑定成功');
        bindStoreDialogVisible.value = false;
        await loadRows();
    }
    finally {
        bindStoreSubmitting.value = false;
    }
};
const adoptTemplate = async (business, workflowCode) => {
    if (adoptedTemplateMap[business.processId] === workflowCode) {
        return;
    }
    const target = getHistoryRows(business.processId).find((item) => item.workflowCode === workflowCode);
    if (!target || target.status !== 'PUBLISHED') {
        ElMessage.warning('未发布版本不能使用，请先发布流程版本');
        return;
    }
    adoptingBusinessCode.value = business.processId;
    try {
        await updateWorkflowProcessApi(business.id, {
            orgId: sessionStore.currentOrgId,
            processCode: business.processId,
            businessName: business.businessName,
            templateId: workflowCode,
        });
        adoptedTemplateMap[business.processId] = workflowCode;
        business.templateId = workflowCode;
        ElMessage.success('使用状态已更新');
    }
    finally {
        adoptingBusinessCode.value = '';
    }
};
const removeConfig = async (business, row) => {
    try {
        await ElMessageBox.confirm(`确认删除流程模板“${row.workflowCode}”吗？`, '删除确认', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
    }
    catch {
        return;
    }
    deletingConfigId.value = row.id;
    try {
        await deleteWorkflowConfigApi(row.id, sessionStore.currentOrgId);
        if ((adoptedTemplateMap[business.processId] ?? '') === row.workflowCode) {
            await updateWorkflowProcessApi(business.id, {
                orgId: sessionStore.currentOrgId,
                processCode: business.processId,
                businessName: business.businessName,
                templateId: undefined,
            });
            adoptedTemplateMap[business.processId] = '';
            business.templateId = undefined;
        }
        ElMessage.success('流程模板删除成功');
        await loadRows();
    }
    finally {
        deletingConfigId.value = null;
    }
};
onMounted(() => {
    void loadRows();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['expand-table']} */ ;
/** @type {__VLS_StyleScopedClasses['expand-table']} */ ;
/** @type {__VLS_StyleScopedClasses['el-table__cell']} */ ;
/** @type {__VLS_StyleScopedClasses['publish-status']} */ ;
/** @type {__VLS_StyleScopedClasses['publish-status']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-grid single" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar" },
});
const __VLS_0 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    modelValue: (__VLS_ctx.query.keyword),
    placeholder: "搜索业务ID/业务名称/流程ID/流程版本",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_2 = __VLS_1({
    modelValue: (__VLS_ctx.query.keyword),
    placeholder: "搜索业务ID/业务名称/流程ID/流程版本",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar-actions" },
});
const __VLS_4 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_6 = __VLS_5({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
let __VLS_8;
let __VLS_9;
let __VLS_10;
const __VLS_11 = {
    onClick: (__VLS_ctx.openCreateBusiness)
};
__VLS_7.slots.default;
var __VLS_7;
const __VLS_12 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    ...{ 'onClick': {} },
}));
const __VLS_14 = __VLS_13({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
let __VLS_16;
let __VLS_17;
let __VLS_18;
const __VLS_19 = {
    onClick: (__VLS_ctx.loadRows)
};
__VLS_15.slots.default;
var __VLS_15;
const __VLS_20 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    data: (__VLS_ctx.filteredRows),
    expandRowKeys: (__VLS_ctx.expandedRowKeys),
    border: true,
    stripe: true,
    rowKey: "id",
    ...{ class: "erp-table" },
}));
const __VLS_22 = __VLS_21({
    data: (__VLS_ctx.filteredRows),
    expandRowKeys: (__VLS_ctx.expandedRowKeys),
    border: true,
    stripe: true,
    rowKey: "id",
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_23.slots.default;
const __VLS_24 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    type: "expand",
    width: "48",
}));
const __VLS_26 = __VLS_25({
    type: "expand",
    width: "48",
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
__VLS_27.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_27.slots;
    const { row: business } = __VLS_getSlotParam(__VLS_thisSlot);
    const __VLS_28 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
        data: (__VLS_ctx.getHistoryRows(business.processId)),
        border: true,
        size: "small",
        ...{ class: "expand-table" },
    }));
    const __VLS_30 = __VLS_29({
        data: (__VLS_ctx.getHistoryRows(business.processId)),
        border: true,
        size: "small",
        ...{ class: "expand-table" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_29));
    __VLS_31.slots.default;
    const __VLS_32 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
        prop: "workflowCode",
        label: "流程版本",
        width: "150",
        showOverflowTooltip: true,
    }));
    const __VLS_34 = __VLS_33({
        prop: "workflowCode",
        label: "流程版本",
        width: "150",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_33));
    const __VLS_36 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
        prop: "savedAt",
        label: "流程发布时间",
        width: "170",
    }));
    const __VLS_38 = __VLS_37({
        prop: "savedAt",
        label: "流程发布时间",
        width: "170",
    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
    const __VLS_40 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        label: "是否发布",
        width: "92",
        align: "center",
    }));
    const __VLS_42 = __VLS_41({
        label: "是否发布",
        width: "92",
        align: "center",
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
    __VLS_43.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_43.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "publish-status" },
            ...{ class: (row.status === 'PUBLISHED' ? 'is-published' : 'is-draft') },
        });
        (row.status === 'PUBLISHED' ? '已发布' : '未发布');
    }
    var __VLS_43;
    const __VLS_44 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
        label: "是否使用",
        width: "92",
        align: "center",
    }));
    const __VLS_46 = __VLS_45({
        label: "是否使用",
        width: "92",
        align: "center",
    }, ...__VLS_functionalComponentArgsRest(__VLS_45));
    __VLS_47.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_47.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "use-radio-cell" },
        });
        const __VLS_48 = {}.ElRadio;
        /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
        // @ts-ignore
        const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
            ...{ 'onChange': {} },
            value: (row.workflowCode),
            modelValue: (__VLS_ctx.adoptedTemplateMap[business.processId]),
            disabled: (__VLS_ctx.adoptingBusinessCode === business.processId || row.status !== 'PUBLISHED'),
        }));
        const __VLS_50 = __VLS_49({
            ...{ 'onChange': {} },
            value: (row.workflowCode),
            modelValue: (__VLS_ctx.adoptedTemplateMap[business.processId]),
            disabled: (__VLS_ctx.adoptingBusinessCode === business.processId || row.status !== 'PUBLISHED'),
        }, ...__VLS_functionalComponentArgsRest(__VLS_49));
        let __VLS_52;
        let __VLS_53;
        let __VLS_54;
        const __VLS_55 = {
            onChange: (...[$event]) => {
                __VLS_ctx.adoptTemplate(business, row.workflowCode);
            }
        };
        var __VLS_51;
    }
    var __VLS_47;
    const __VLS_56 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
        label: "操作",
        width: "210",
        fixed: "right",
    }));
    const __VLS_58 = __VLS_57({
        label: "操作",
        width: "210",
        fixed: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_57));
    __VLS_59.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_59.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_60 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }));
        const __VLS_62 = __VLS_61({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }, ...__VLS_functionalComponentArgsRest(__VLS_61));
        let __VLS_64;
        let __VLS_65;
        let __VLS_66;
        const __VLS_67 = {
            onClick: (...[$event]) => {
                __VLS_ctx.openWorkflowConfig({ businessCode: business.processId, viewWorkflowCode: row.workflowCode });
            }
        };
        __VLS_63.slots.default;
        var __VLS_63;
        const __VLS_68 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }));
        const __VLS_70 = __VLS_69({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }, ...__VLS_functionalComponentArgsRest(__VLS_69));
        let __VLS_72;
        let __VLS_73;
        let __VLS_74;
        const __VLS_75 = {
            onClick: (...[$event]) => {
                __VLS_ctx.openWorkflowConfig({ businessCode: business.processId, copyFromWorkflowCode: row.workflowCode });
            }
        };
        __VLS_71.slots.default;
        var __VLS_71;
        if (row.status !== 'PUBLISHED') {
            const __VLS_76 = {}.ElButton;
            /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
            // @ts-ignore
            const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
                ...{ 'onClick': {} },
                text: true,
                type: "primary",
                loading: (__VLS_ctx.publishingConfigId === row.id),
            }));
            const __VLS_78 = __VLS_77({
                ...{ 'onClick': {} },
                text: true,
                type: "primary",
                loading: (__VLS_ctx.publishingConfigId === row.id),
            }, ...__VLS_functionalComponentArgsRest(__VLS_77));
            let __VLS_80;
            let __VLS_81;
            let __VLS_82;
            const __VLS_83 = {
                onClick: (...[$event]) => {
                    if (!(row.status !== 'PUBLISHED'))
                        return;
                    __VLS_ctx.publishConfig(business, row);
                }
            };
            __VLS_79.slots.default;
            var __VLS_79;
        }
        const __VLS_84 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
            ...{ 'onClick': {} },
            text: true,
            type: "danger",
            loading: (__VLS_ctx.deletingConfigId === row.id),
        }));
        const __VLS_86 = __VLS_85({
            ...{ 'onClick': {} },
            text: true,
            type: "danger",
            loading: (__VLS_ctx.deletingConfigId === row.id),
        }, ...__VLS_functionalComponentArgsRest(__VLS_85));
        let __VLS_88;
        let __VLS_89;
        let __VLS_90;
        const __VLS_91 = {
            onClick: (...[$event]) => {
                __VLS_ctx.removeConfig(business, row);
            }
        };
        __VLS_87.slots.default;
        var __VLS_87;
    }
    var __VLS_59;
    {
        const { empty: __VLS_thisSlot } = __VLS_31.slots;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "expand-empty" },
        });
    }
    var __VLS_31;
}
var __VLS_27;
const __VLS_92 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
    prop: "processId",
    label: "业务ID",
    width: "180",
    showOverflowTooltip: true,
}));
const __VLS_94 = __VLS_93({
    prop: "processId",
    label: "业务ID",
    width: "180",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_93));
const __VLS_96 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
    prop: "businessName",
    label: "业务名称",
    width: "180",
    showOverflowTooltip: true,
}));
const __VLS_98 = __VLS_97({
    prop: "businessName",
    label: "业务名称",
    width: "180",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_97));
const __VLS_100 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
    prop: "storeNames",
    label: "关联门店",
    width: "220",
    showOverflowTooltip: true,
}));
const __VLS_102 = __VLS_101({
    prop: "storeNames",
    label: "关联门店",
    width: "220",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_101));
__VLS_103.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_103.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.storeNames || '-');
}
var __VLS_103;
const __VLS_104 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
    prop: "createdAt",
    label: "业务添加时间",
    width: "160",
}));
const __VLS_106 = __VLS_105({
    prop: "createdAt",
    label: "业务添加时间",
    width: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_105));
const __VLS_108 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
    label: "操作",
    width: "320",
    fixed: "right",
}));
const __VLS_110 = __VLS_109({
    label: "操作",
    width: "320",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_109));
__VLS_111.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_111.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_112 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_114 = __VLS_113({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_113));
    let __VLS_116;
    let __VLS_117;
    let __VLS_118;
    const __VLS_119 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openWorkflowConfig({ businessCode: row.processId });
        }
    };
    __VLS_115.slots.default;
    var __VLS_115;
    const __VLS_120 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_122 = __VLS_121({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_121));
    let __VLS_124;
    let __VLS_125;
    let __VLS_126;
    const __VLS_127 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditBusiness(row);
        }
    };
    __VLS_123.slots.default;
    var __VLS_123;
    const __VLS_128 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_129 = __VLS_asFunctionalComponent(__VLS_128, new __VLS_128({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_130 = __VLS_129({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_129));
    let __VLS_132;
    let __VLS_133;
    let __VLS_134;
    const __VLS_135 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openBindStores(row);
        }
    };
    __VLS_131.slots.default;
    var __VLS_131;
    const __VLS_136 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_137 = __VLS_asFunctionalComponent(__VLS_136, new __VLS_136({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }));
    const __VLS_138 = __VLS_137({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_137));
    let __VLS_140;
    let __VLS_141;
    let __VLS_142;
    const __VLS_143 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeBusiness(row);
        }
    };
    __VLS_139.slots.default;
    var __VLS_139;
}
var __VLS_111;
var __VLS_23;
const __VLS_144 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_145 = __VLS_asFunctionalComponent(__VLS_144, new __VLS_144({
    modelValue: (__VLS_ctx.processDialogVisible),
    title: (__VLS_ctx.editingBusinessId ? '编辑业务' : '新增业务'),
    width: "460px",
    appendToBody: true,
    destroyOnClose: true,
}));
const __VLS_146 = __VLS_145({
    modelValue: (__VLS_ctx.processDialogVisible),
    title: (__VLS_ctx.editingBusinessId ? '编辑业务' : '新增业务'),
    width: "460px",
    appendToBody: true,
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_145));
__VLS_147.slots.default;
const __VLS_148 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_149 = __VLS_asFunctionalComponent(__VLS_148, new __VLS_148({
    labelWidth: "86px",
}));
const __VLS_150 = __VLS_149({
    labelWidth: "86px",
}, ...__VLS_functionalComponentArgsRest(__VLS_149));
__VLS_151.slots.default;
const __VLS_152 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_153 = __VLS_asFunctionalComponent(__VLS_152, new __VLS_152({
    label: "业务ID",
}));
const __VLS_154 = __VLS_153({
    label: "业务ID",
}, ...__VLS_functionalComponentArgsRest(__VLS_153));
__VLS_155.slots.default;
const __VLS_156 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_157 = __VLS_asFunctionalComponent(__VLS_156, new __VLS_156({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.processForm.processCode),
    placeholder: "请选择业务ID",
    ...{ style: {} },
    disabled: (Boolean(__VLS_ctx.editingBusinessId)),
}));
const __VLS_158 = __VLS_157({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.processForm.processCode),
    placeholder: "请选择业务ID",
    ...{ style: {} },
    disabled: (Boolean(__VLS_ctx.editingBusinessId)),
}, ...__VLS_functionalComponentArgsRest(__VLS_157));
let __VLS_160;
let __VLS_161;
let __VLS_162;
const __VLS_163 = {
    onChange: (__VLS_ctx.syncBusinessNameFromCode)
};
__VLS_159.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.workflowBusinessOptions))) {
    const __VLS_164 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_165 = __VLS_asFunctionalComponent(__VLS_164, new __VLS_164({
        key: (option.processCode),
        label: (`${option.businessName}（${option.processCode}）`),
        value: (option.processCode),
    }));
    const __VLS_166 = __VLS_165({
        key: (option.processCode),
        label: (`${option.businessName}（${option.processCode}）`),
        value: (option.processCode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_165));
}
var __VLS_159;
var __VLS_155;
var __VLS_151;
{
    const { footer: __VLS_thisSlot } = __VLS_147.slots;
    const __VLS_168 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_169 = __VLS_asFunctionalComponent(__VLS_168, new __VLS_168({
        ...{ 'onClick': {} },
    }));
    const __VLS_170 = __VLS_169({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_169));
    let __VLS_172;
    let __VLS_173;
    let __VLS_174;
    const __VLS_175 = {
        onClick: (...[$event]) => {
            __VLS_ctx.processDialogVisible = false;
        }
    };
    __VLS_171.slots.default;
    var __VLS_171;
    const __VLS_176 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_177 = __VLS_asFunctionalComponent(__VLS_176, new __VLS_176({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.processSubmitting),
    }));
    const __VLS_178 = __VLS_177({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.processSubmitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_177));
    let __VLS_180;
    let __VLS_181;
    let __VLS_182;
    const __VLS_183 = {
        onClick: (__VLS_ctx.submitBusinessForm)
    };
    __VLS_179.slots.default;
    var __VLS_179;
}
var __VLS_147;
const __VLS_184 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_185 = __VLS_asFunctionalComponent(__VLS_184, new __VLS_184({
    modelValue: (__VLS_ctx.bindStoreDialogVisible),
    title: "绑定门店",
    width: "680px",
    appendToBody: true,
    destroyOnClose: true,
}));
const __VLS_186 = __VLS_185({
    modelValue: (__VLS_ctx.bindStoreDialogVisible),
    title: "绑定门店",
    width: "680px",
    appendToBody: true,
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_185));
__VLS_187.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_188 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_189 = __VLS_asFunctionalComponent(__VLS_188, new __VLS_188({
    modelValue: (__VLS_ctx.bindStoreSearch),
    placeholder: "搜索门店名称/编码",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_190 = __VLS_189({
    modelValue: (__VLS_ctx.bindStoreSearch),
    placeholder: "搜索门店名称/编码",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_189));
const __VLS_192 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_193 = __VLS_asFunctionalComponent(__VLS_192, new __VLS_192({
    ...{ 'onSelectionChange': {} },
    ref: "bindStoreTableRef",
    data: (__VLS_ctx.filteredStoreOptions),
    border: true,
    stripe: true,
    size: "small",
    maxHeight: "400",
}));
const __VLS_194 = __VLS_193({
    ...{ 'onSelectionChange': {} },
    ref: "bindStoreTableRef",
    data: (__VLS_ctx.filteredStoreOptions),
    border: true,
    stripe: true,
    size: "small",
    maxHeight: "400",
}, ...__VLS_functionalComponentArgsRest(__VLS_193));
let __VLS_196;
let __VLS_197;
let __VLS_198;
const __VLS_199 = {
    onSelectionChange: (__VLS_ctx.onBindStoreSelectionChange)
};
/** @type {typeof __VLS_ctx.bindStoreTableRef} */ ;
var __VLS_200 = {};
__VLS_195.slots.default;
const __VLS_202 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
    type: "selection",
    width: "50",
    align: "center",
}));
const __VLS_204 = __VLS_203({
    type: "selection",
    width: "50",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_203));
const __VLS_206 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_207 = __VLS_asFunctionalComponent(__VLS_206, new __VLS_206({
    prop: "storeCode",
    label: "门店编码",
    width: "140",
    showOverflowTooltip: true,
}));
const __VLS_208 = __VLS_207({
    prop: "storeCode",
    label: "门店编码",
    width: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_207));
const __VLS_210 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
    prop: "storeName",
    label: "门店名称",
    minWidth: "200",
    showOverflowTooltip: true,
}));
const __VLS_212 = __VLS_211({
    prop: "storeName",
    label: "门店名称",
    minWidth: "200",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_211));
var __VLS_195;
{
    const { footer: __VLS_thisSlot } = __VLS_187.slots;
    const __VLS_214 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({
        ...{ 'onClick': {} },
    }));
    const __VLS_216 = __VLS_215({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_215));
    let __VLS_218;
    let __VLS_219;
    let __VLS_220;
    const __VLS_221 = {
        onClick: (...[$event]) => {
            __VLS_ctx.bindStoreDialogVisible = false;
        }
    };
    __VLS_217.slots.default;
    var __VLS_217;
    const __VLS_222 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_223 = __VLS_asFunctionalComponent(__VLS_222, new __VLS_222({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.bindStoreSubmitting),
    }));
    const __VLS_224 = __VLS_223({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.bindStoreSubmitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_223));
    let __VLS_226;
    let __VLS_227;
    let __VLS_228;
    const __VLS_229 = {
        onClick: (__VLS_ctx.submitBindStores)
    };
    __VLS_225.slots.default;
    var __VLS_225;
}
var __VLS_187;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['expand-table']} */ ;
/** @type {__VLS_StyleScopedClasses['publish-status']} */ ;
/** @type {__VLS_StyleScopedClasses['use-radio-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['expand-empty']} */ ;
// @ts-ignore
var __VLS_201 = __VLS_200;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            loading: loading,
            query: query,
            processDialogVisible: processDialogVisible,
            processSubmitting: processSubmitting,
            editingBusinessId: editingBusinessId,
            workflowBusinessOptions: workflowBusinessOptions,
            processForm: processForm,
            adoptingBusinessCode: adoptingBusinessCode,
            publishingConfigId: publishingConfigId,
            deletingConfigId: deletingConfigId,
            adoptedTemplateMap: adoptedTemplateMap,
            expandedRowKeys: expandedRowKeys,
            bindStoreDialogVisible: bindStoreDialogVisible,
            bindStoreSubmitting: bindStoreSubmitting,
            bindStoreSearch: bindStoreSearch,
            bindStoreTableRef: bindStoreTableRef,
            filteredStoreOptions: filteredStoreOptions,
            filteredRows: filteredRows,
            getHistoryRows: getHistoryRows,
            syncBusinessNameFromCode: syncBusinessNameFromCode,
            openCreateBusiness: openCreateBusiness,
            openEditBusiness: openEditBusiness,
            submitBusinessForm: submitBusinessForm,
            removeBusiness: removeBusiness,
            loadRows: loadRows,
            openWorkflowConfig: openWorkflowConfig,
            publishConfig: publishConfig,
            openBindStores: openBindStores,
            onBindStoreSelectionChange: onBindStoreSelectionChange,
            submitBindStores: submitBindStores,
            adoptTemplate: adoptTemplate,
            removeConfig: removeConfig,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
