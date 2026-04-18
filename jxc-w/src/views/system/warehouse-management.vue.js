/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import { useSessionStore } from '@/stores/session';
import { fetchStoreWarehousesApi, createStoreWarehouseApi, updateWarehouseApi, deleteWarehouseApi, setWarehouseDefaultApi, updateWarehouseStatusApi, } from '@/api/modules/warehouse';
const sessionStore = useSessionStore();
const toolbarButtons = [
    { key: '新增', label: '新增', type: 'primary' },
];
const statusOptions = ['全部', '启用', '停用'];
const warehouseTypeOptions = ['全部', '出品及生产部门', '行政部门', '普通仓库'];
const warehouseFormTypeOptions = ['出品及生产部门', '行政部门', '普通仓库'];
const departmentOptions = ['供应链中心', '采购部', '营运部', '仓储部', '出品部', '生产部'];
const regionOptions = [
    {
        value: '上海市',
        label: '上海市',
        children: [
            { value: '闵行区', label: '闵行区', children: [{ value: '七宝镇', label: '七宝镇' }, { value: '华漕镇', label: '华漕镇' }] },
            { value: '浦东新区', label: '浦东新区', children: [{ value: '川沙新镇', label: '川沙新镇' }, { value: '张江镇', label: '张江镇' }] },
        ],
    },
];
// Dialog state
const dialogVisible = ref(false);
const dialogTitle = ref('新增仓库');
const isEdit = ref(false);
const editingId = ref(null);
// View detail dialog state
const viewDialogVisible = ref(false);
const viewingRow = ref(null);
const formRef = ref();
const form = reactive({
    warehouseType: '出品及生产部门',
    warehouseCode: '',
    warehouseName: '',
    department: '',
    enabled: true,
    contactName: '',
    contactPhone: '',
    region: [],
    address: '',
    targetGrossMargin: '',
    idealPurchaseSaleRatio: '',
});
const formRules = {
    warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
    warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
    contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
    contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
    region: [{ required: true, message: '请选择区域', trigger: 'change' }],
    targetGrossMargin: [{ required: true, message: '请输入目标毛利率', trigger: 'blur' }],
    idealPurchaseSaleRatio: [{ required: true, message: '请输入理想采销比', trigger: 'blur' }],
};
// Query state
const query = reactive({
    warehouseInfo: '',
    status: '全部',
    warehouseType: '全部',
});
// Store / Table state
const storeOptions = computed(() => {
    const currentOrg = sessionStore.currentOrg;
    if (currentOrg?.type === 'store') {
        return [currentOrg];
    }
    if (currentOrg?.type === 'group') {
        return currentOrg.children ?? [];
    }
    return sessionStore.flatOrgs.filter((item) => item.type === 'store');
});
const selectedStoreId = ref();
const tableData = ref([]);
const selectedIds = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
// Computed (all data shown, server-side filtering)
const totalItems = computed(() => tableData.value.length);
const isStatusEnabled = (status) => status === 'ENABLED';
const formatStatusLabel = (status) => {
    if (!status) {
        return '-';
    }
    return status === 'ENABLED' ? '启用' : '停用';
};
const resolveStoreIdFromCurrentOrg = () => {
    const currentOrgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
    if (!currentOrgId) {
        return undefined;
    }
    if (currentOrgId.startsWith('store-')) {
        const currentId = Number(currentOrgId.slice('store-'.length));
        return Number.isNaN(currentId) ? undefined : currentId;
    }
    const currentOrg = sessionStore.currentOrg;
    if (currentOrg?.type === 'group') {
        const firstStore = currentOrg.children?.[0];
        if (!firstStore) {
            return undefined;
        }
        const storeId = Number(String(firstStore.id).slice('store-'.length));
        return Number.isNaN(storeId) ? undefined : storeId;
    }
    const firstStore = sessionStore.flatOrgs.find((item) => item.type === 'store');
    if (!firstStore) {
        return undefined;
    }
    const storeId = Number(String(firstStore.id).slice('store-'.length));
    return Number.isNaN(storeId) ? undefined : storeId;
};
const resolveActiveStoreId = () => selectedStoreId.value ?? resolveStoreIdFromCurrentOrg();
/** Load warehouses from backend */
const loadData = async () => {
    const storeId = resolveActiveStoreId();
    if (!storeId) {
        tableData.value = [];
        return;
    }
    loading.value = true;
    try {
        const rows = await fetchStoreWarehousesApi(storeId, {
            keyword: query.warehouseInfo.trim() || undefined,
            status: query.status !== '全部' ? query.status : undefined,
            warehouseType: query.warehouseType !== '全部' ? query.warehouseType : undefined,
        });
        tableData.value = rows;
    }
    catch (err) {
        console.error('[warehouse] loadData error:', err);
        ElMessage.error('加载数据失败');
    }
    finally {
        loading.value = false;
    }
};
onMounted(async () => {
    loading.value = true;
    try {
        selectedStoreId.value = resolveStoreIdFromCurrentOrg();
        await loadData();
    }
    finally {
        loading.value = false;
    }
});
watch(() => sessionStore.currentOrgId, async () => {
    selectedStoreId.value = resolveStoreIdFromCurrentOrg();
    await loadData();
});
const handleSearch = () => {
    currentPage.value = 1;
    loadData();
};
const handleReset = () => {
    query.warehouseInfo = '';
    query.status = '全部';
    query.warehouseType = '全部';
    currentPage.value = 1;
    loadData();
};
const handleToolbarAction = (action) => {
    if (action === '新增') {
        if (!resolveActiveStoreId()) {
            ElMessage.warning('请先选择门店');
            return;
        }
        openCreateDialog();
    }
};
const handleStoreChange = async () => {
    currentPage.value = 1;
    await loadData();
};
const handleSelectionChange = (rows) => {
    selectedIds.value = rows.map((row) => row.id);
};
/** Map UI status to API status */
const toApiStatus = (enabled) => enabled ? 'ENABLED' : 'DISABLED';
/** Build payload from form */
const buildPayload = () => ({
    warehouseName: form.warehouseName.trim(),
    department: form.department.trim() || undefined,
    status: toApiStatus(form.enabled),
    warehouseType: form.warehouseType,
    contactName: form.contactName.trim(),
    contactPhone: form.contactPhone.trim(),
    regionPath: form.region.length > 0 ? form.region.join('/') : undefined,
    address: form.address.trim(),
    targetGrossMargin: form.targetGrossMargin.trim(),
    idealPurchaseSaleRatio: form.idealPurchaseSaleRatio.trim(),
});
/** Format datetime for display */
const formatDateTime = (iso) => {
    if (!iso)
        return '-';
    try {
        const d = new Date(iso);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const h = String(d.getHours()).padStart(2, '0');
        const min = String(d.getMinutes()).padStart(2, '0');
        const s = String(d.getSeconds()).padStart(2, '0');
        return `${y}-${m}-${day} ${h}:${min}:${s}`;
    }
    catch {
        return String(iso);
    }
};
/** Open create dialog */
const openCreateDialog = () => {
    isEdit.value = false;
    editingId.value = null;
    dialogTitle.value = '新增仓库';
    resetForm();
    dialogVisible.value = true;
};
/** Open edit dialog */
const handleEdit = async (row) => {
    isEdit.value = true;
    editingId.value = row.id;
    dialogTitle.value = '编辑仓库';
    form.warehouseType = row.warehouseType || '普通仓库';
    form.warehouseCode = row.warehouseCode;
    form.warehouseName = row.warehouseName;
    form.department = row.department || '';
    form.enabled = isStatusEnabled(row.status);
    form.contactName = row.contactName || '';
    form.contactPhone = row.contactPhone || '';
    // Parse regionPath back into array
    if (row.address) {
        const parts = row.address.split(' ').filter(Boolean);
        // Try to match known region prefixes
        let regionStr = '';
        let addrPart = row.address;
        for (const opt of regionOptions[0].children ?? []) {
            if (row.address.includes(opt.value)) {
                for (const sub of (opt.children ?? [])) {
                    if (row.address.includes(sub.value)) {
                        regionStr = [regionOptions[0].value, opt.value, sub.value].join(',');
                        addrPart = row.address.replace(regionStr.replace(/,/g, '/'), '').trim();
                        break;
                    }
                }
                break;
            }
        }
        form.region = regionStr ? regionStr.split(',') : [];
        form.address = addrPart;
    }
    else {
        form.region = [];
        form.address = '';
    }
    form.targetGrossMargin = row.targetGrossMargin?.replace('%', '') || '';
    form.idealPurchaseSaleRatio = row.idealPurchaseSaleRatio || '';
    dialogVisible.value = true;
};
const handleView = (row) => {
    viewingRow.value = row;
    viewDialogVisible.value = true;
};
const handleSetDefault = async (row) => {
    try {
        await setWarehouseDefaultApi(row.id);
        ElMessage.success('已设为默认');
        loadData();
    }
    catch {
        // error handled by http-client
    }
};
const handleDelete = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除仓库「${row.warehouseName}」？`, '提示', { type: 'warning' });
        await deleteWarehouseApi(row.id);
        ElMessage.success('删除成功');
        loadData();
    }
    catch (e) {
        if (e !== 'cancel') {
            // error handled by http-client or cancelled
        }
    }
};
const handleToggleStatus = async (row) => {
    const newStatus = isStatusEnabled(row.status) ? 'DISABLED' : 'ENABLED';
    try {
        await updateWarehouseStatusApi(row.id, newStatus);
        ElMessage.success(newStatus === 'ENABLED' ? '已启用' : '已停用');
        loadData();
    }
    catch {
        // error handled by http-client
    }
};
const resetForm = () => {
    form.warehouseType = '出品及生产部门';
    form.warehouseCode = '';
    form.warehouseName = '';
    form.department = '';
    form.enabled = true;
    form.contactName = '';
    form.contactPhone = '';
    form.region = [];
    form.address = '';
    form.targetGrossMargin = '';
    form.idealPurchaseSaleRatio = '';
};
const closeDialog = () => {
    dialogVisible.value = false;
    formRef.value?.clearValidate();
    resetForm();
};
const handleSubmit = async () => {
    if (!formRef.value) {
        ElMessage.warning('表单未就绪，请重试');
        return;
    }
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid)
        return;
    const storeId = resolveActiveStoreId();
    if (!storeId) {
        ElMessage.warning('请先选择门店');
        return;
    }
    try {
        if (isEdit.value && editingId.value != null) {
            await updateWarehouseApi(editingId.value, buildPayload());
            ElMessage.success('编辑成功');
        }
        else {
            await createStoreWarehouseApi(storeId, buildPayload());
            ElMessage.success('新增成功');
        }
        closeDialog();
        loadData();
    }
    catch {
        // error handled by http-client
    }
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
if (__VLS_ctx.storeOptions.length > 1) {
    const __VLS_3 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
        label: "所属门店",
    }));
    const __VLS_5 = __VLS_4({
        label: "所属门店",
    }, ...__VLS_functionalComponentArgsRest(__VLS_4));
    __VLS_6.slots.default;
    const __VLS_7 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
        ...{ 'onChange': {} },
        modelValue: (__VLS_ctx.selectedStoreId),
        ...{ style: {} },
    }));
    const __VLS_9 = __VLS_8({
        ...{ 'onChange': {} },
        modelValue: (__VLS_ctx.selectedStoreId),
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_8));
    let __VLS_11;
    let __VLS_12;
    let __VLS_13;
    const __VLS_14 = {
        onChange: (__VLS_ctx.handleStoreChange)
    };
    __VLS_10.slots.default;
    for (const [store] of __VLS_getVForSourceType((__VLS_ctx.storeOptions))) {
        const __VLS_15 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
            key: (store.id),
            label: (`${store.name} (${store.code})`),
            value: (Number(String(store.id).slice(String(store.id).lastIndexOf('-') + 1))),
        }));
        const __VLS_17 = __VLS_16({
            key: (store.id),
            label: (`${store.name} (${store.code})`),
            value: (Number(String(store.id).slice(String(store.id).lastIndexOf('-') + 1))),
        }, ...__VLS_functionalComponentArgsRest(__VLS_16));
    }
    var __VLS_10;
    var __VLS_6;
}
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "仓库信息",
}));
const __VLS_21 = __VLS_20({
    label: "仓库信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    modelValue: (__VLS_ctx.query.warehouseInfo),
    placeholder: "请输入仓库编码、名称或联系人",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_25 = __VLS_24({
    modelValue: (__VLS_ctx.query.warehouseInfo),
    placeholder: "请输入仓库编码、名称或联系人",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
var __VLS_22;
const __VLS_27 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    label: "状态",
}));
const __VLS_29 = __VLS_28({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    modelValue: (__VLS_ctx.query.status),
    ...{ style: {} },
}));
const __VLS_33 = __VLS_32({
    modelValue: (__VLS_ctx.query.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
__VLS_34.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.statusOptions))) {
    const __VLS_35 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_37 = __VLS_36({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_36));
}
var __VLS_34;
var __VLS_30;
const __VLS_39 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({
    label: "仓库类型",
}));
const __VLS_41 = __VLS_40({
    label: "仓库类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
__VLS_42.slots.default;
const __VLS_43 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
    modelValue: (__VLS_ctx.query.warehouseType),
    ...{ style: {} },
}));
const __VLS_45 = __VLS_44({
    modelValue: (__VLS_ctx.query.warehouseType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
__VLS_46.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.warehouseTypeOptions))) {
    const __VLS_47 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_49 = __VLS_48({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_48));
}
var __VLS_46;
var __VLS_42;
const __VLS_51 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({}));
const __VLS_53 = __VLS_52({}, ...__VLS_functionalComponentArgsRest(__VLS_52));
__VLS_54.slots.default;
const __VLS_55 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_56 = __VLS_asFunctionalComponent(__VLS_55, new __VLS_55({
    ...{ 'onClick': {} },
    type: "primary",
    loading: (__VLS_ctx.loading),
}));
const __VLS_57 = __VLS_56({
    ...{ 'onClick': {} },
    type: "primary",
    loading: (__VLS_ctx.loading),
}, ...__VLS_functionalComponentArgsRest(__VLS_56));
let __VLS_59;
let __VLS_60;
let __VLS_61;
const __VLS_62 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_58.slots.default;
var __VLS_58;
const __VLS_63 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({
    ...{ 'onClick': {} },
}));
const __VLS_65 = __VLS_64({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_64));
let __VLS_67;
let __VLS_68;
let __VLS_69;
const __VLS_70 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_66.slots.default;
var __VLS_66;
var __VLS_54;
var __VLS_2;
/** @type {[typeof CommonToolbarSection, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(CommonToolbarSection, new CommonToolbarSection({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.toolbarButtons),
}));
const __VLS_72 = __VLS_71({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.toolbarButtons),
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
let __VLS_74;
let __VLS_75;
let __VLS_76;
const __VLS_77 = {
    onAction: (__VLS_ctx.handleToolbarAction)
};
var __VLS_73;
const __VLS_78 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.tableData),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_80 = __VLS_79({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.tableData),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
let __VLS_82;
let __VLS_83;
let __VLS_84;
const __VLS_85 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_81.slots.default;
const __VLS_86 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_88 = __VLS_87({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_87));
const __VLS_90 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_92 = __VLS_91({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_91));
const __VLS_94 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
    prop: "warehouseCode",
    label: "仓库编码",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_96 = __VLS_95({
    prop: "warehouseCode",
    label: "仓库编码",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_95));
const __VLS_98 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    prop: "warehouseName",
    label: "仓库名称",
    minWidth: "150",
    showOverflowTooltip: true,
}));
const __VLS_100 = __VLS_99({
    prop: "warehouseName",
    label: "仓库名称",
    minWidth: "150",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
const __VLS_102 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
    prop: "department",
    label: "所属部门",
    minWidth: "120",
    showOverflowTooltip: true,
}));
const __VLS_104 = __VLS_103({
    prop: "department",
    label: "所属部门",
    minWidth: "120",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_103));
const __VLS_106 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    label: "状态",
    width: "90",
    align: "center",
}));
const __VLS_108 = __VLS_107({
    label: "状态",
    width: "90",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
__VLS_109.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_109.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_110 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
        ...{ 'onChange': {} },
        modelValue: (__VLS_ctx.isStatusEnabled(row.status)),
    }));
    const __VLS_112 = __VLS_111({
        ...{ 'onChange': {} },
        modelValue: (__VLS_ctx.isStatusEnabled(row.status)),
    }, ...__VLS_functionalComponentArgsRest(__VLS_111));
    let __VLS_114;
    let __VLS_115;
    let __VLS_116;
    const __VLS_117 = {
        onChange: (...[$event]) => {
            __VLS_ctx.handleToggleStatus(row);
        }
    };
    var __VLS_113;
}
var __VLS_109;
const __VLS_118 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    prop: "warehouseType",
    label: "仓库类型",
    minWidth: "110",
    showOverflowTooltip: true,
}));
const __VLS_120 = __VLS_119({
    prop: "warehouseType",
    label: "仓库类型",
    minWidth: "110",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
const __VLS_122 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
    prop: "contactName",
    label: "联系人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_124 = __VLS_123({
    prop: "contactName",
    label: "联系人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_123));
const __VLS_126 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    prop: "contactPhone",
    label: "联系电话",
    minWidth: "130",
    showOverflowTooltip: true,
}));
const __VLS_128 = __VLS_127({
    prop: "contactPhone",
    label: "联系电话",
    minWidth: "130",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
const __VLS_130 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
    prop: "address",
    label: "详细地址",
    minWidth: "220",
    showOverflowTooltip: true,
}));
const __VLS_132 = __VLS_131({
    prop: "address",
    label: "详细地址",
    minWidth: "220",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_131));
const __VLS_134 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
    prop: "targetGrossMargin",
    label: "目标毛利率",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_136 = __VLS_135({
    prop: "targetGrossMargin",
    label: "目标毛利率",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_135));
__VLS_137.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_137.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.targetGrossMargin);
}
var __VLS_137;
const __VLS_138 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    prop: "idealPurchaseSaleRatio",
    label: "理想采销比",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_140 = __VLS_139({
    prop: "idealPurchaseSaleRatio",
    label: "理想采销比",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
const __VLS_142 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    prop: "updatedAt",
    label: "操作时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_144 = __VLS_143({
    prop: "updatedAt",
    label: "操作时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
__VLS_145.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_145.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatDateTime(row.updatedAt));
}
var __VLS_145;
const __VLS_146 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    label: "操作",
    width: "220",
    fixed: "right",
}));
const __VLS_148 = __VLS_147({
    label: "操作",
    width: "220",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
__VLS_149.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_149.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_150 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_152 = __VLS_151({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_151));
    let __VLS_154;
    let __VLS_155;
    let __VLS_156;
    const __VLS_157 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_153.slots.default;
    var __VLS_153;
    const __VLS_158 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_160 = __VLS_159({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_159));
    let __VLS_162;
    let __VLS_163;
    let __VLS_164;
    const __VLS_165 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleEdit(row);
        }
    };
    __VLS_161.slots.default;
    var __VLS_161;
    const __VLS_166 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
        ...{ 'onClick': {} },
        text: true,
        disabled: (row.isDefault),
    }));
    const __VLS_168 = __VLS_167({
        ...{ 'onClick': {} },
        text: true,
        disabled: (row.isDefault),
    }, ...__VLS_functionalComponentArgsRest(__VLS_167));
    let __VLS_170;
    let __VLS_171;
    let __VLS_172;
    const __VLS_173 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleSetDefault(row);
        }
    };
    __VLS_169.slots.default;
    var __VLS_169;
    const __VLS_174 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }));
    const __VLS_176 = __VLS_175({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_175));
    let __VLS_178;
    let __VLS_179;
    let __VLS_180;
    const __VLS_181 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDelete(row);
        }
    };
    __VLS_177.slots.default;
    var __VLS_177;
}
var __VLS_149;
var __VLS_81;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.selectedIds.length);
const __VLS_182 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.totalItems),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}));
const __VLS_184 = __VLS_183({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.totalItems),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_183));
let __VLS_186;
let __VLS_187;
let __VLS_188;
const __VLS_189 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_190 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_185;
const __VLS_191 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.dialogTitle),
    width: "560px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_193 = __VLS_192({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.dialogTitle),
    width: "560px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_192));
let __VLS_195;
let __VLS_196;
let __VLS_197;
const __VLS_198 = {
    onClosed: (__VLS_ctx.closeDialog)
};
__VLS_194.slots.default;
const __VLS_199 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
    ref: "formRef",
    model: (__VLS_ctx.form),
    rules: (__VLS_ctx.formRules),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_201 = __VLS_200({
    ref: "formRef",
    model: (__VLS_ctx.form),
    rules: (__VLS_ctx.formRules),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_200));
/** @type {typeof __VLS_ctx.formRef} */ ;
var __VLS_203 = {};
__VLS_202.slots.default;
const __VLS_205 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_206 = __VLS_asFunctionalComponent(__VLS_205, new __VLS_205({
    label: "仓库类型",
    prop: "warehouseType",
}));
const __VLS_207 = __VLS_206({
    label: "仓库类型",
    prop: "warehouseType",
}, ...__VLS_functionalComponentArgsRest(__VLS_206));
__VLS_208.slots.default;
const __VLS_209 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_210 = __VLS_asFunctionalComponent(__VLS_209, new __VLS_209({
    modelValue: (__VLS_ctx.form.warehouseType),
    ...{ style: {} },
}));
const __VLS_211 = __VLS_210({
    modelValue: (__VLS_ctx.form.warehouseType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_210));
__VLS_212.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.warehouseFormTypeOptions))) {
    const __VLS_213 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_214 = __VLS_asFunctionalComponent(__VLS_213, new __VLS_213({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_215 = __VLS_214({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_214));
}
var __VLS_212;
var __VLS_208;
const __VLS_217 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_218 = __VLS_asFunctionalComponent(__VLS_217, new __VLS_217({
    label: "仓库名称",
    prop: "warehouseName",
}));
const __VLS_219 = __VLS_218({
    label: "仓库名称",
    prop: "warehouseName",
}, ...__VLS_functionalComponentArgsRest(__VLS_218));
__VLS_220.slots.default;
const __VLS_221 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_222 = __VLS_asFunctionalComponent(__VLS_221, new __VLS_221({
    modelValue: (__VLS_ctx.form.warehouseName),
    placeholder: "请输入仓库名称",
}));
const __VLS_223 = __VLS_222({
    modelValue: (__VLS_ctx.form.warehouseName),
    placeholder: "请输入仓库名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_222));
var __VLS_220;
const __VLS_225 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_226 = __VLS_asFunctionalComponent(__VLS_225, new __VLS_225({
    label: "所属部门",
    prop: "department",
}));
const __VLS_227 = __VLS_226({
    label: "所属部门",
    prop: "department",
}, ...__VLS_functionalComponentArgsRest(__VLS_226));
__VLS_228.slots.default;
const __VLS_229 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_230 = __VLS_asFunctionalComponent(__VLS_229, new __VLS_229({
    modelValue: (__VLS_ctx.form.department),
    clearable: true,
    placeholder: "请选择所属部门",
    ...{ style: {} },
}));
const __VLS_231 = __VLS_230({
    modelValue: (__VLS_ctx.form.department),
    clearable: true,
    placeholder: "请选择所属部门",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_230));
__VLS_232.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.departmentOptions))) {
    const __VLS_233 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_234 = __VLS_asFunctionalComponent(__VLS_233, new __VLS_233({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_235 = __VLS_234({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_234));
}
var __VLS_232;
var __VLS_228;
const __VLS_237 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_238 = __VLS_asFunctionalComponent(__VLS_237, new __VLS_237({
    label: "状态",
}));
const __VLS_239 = __VLS_238({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_238));
__VLS_240.slots.default;
const __VLS_241 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_242 = __VLS_asFunctionalComponent(__VLS_241, new __VLS_241({
    modelValue: (__VLS_ctx.form.enabled),
    inlinePrompt: true,
    activeText: "启用",
    inactiveText: "停用",
}));
const __VLS_243 = __VLS_242({
    modelValue: (__VLS_ctx.form.enabled),
    inlinePrompt: true,
    activeText: "启用",
    inactiveText: "停用",
}, ...__VLS_functionalComponentArgsRest(__VLS_242));
var __VLS_240;
const __VLS_245 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_246 = __VLS_asFunctionalComponent(__VLS_245, new __VLS_245({
    label: "联系人",
    prop: "contactName",
}));
const __VLS_247 = __VLS_246({
    label: "联系人",
    prop: "contactName",
}, ...__VLS_functionalComponentArgsRest(__VLS_246));
__VLS_248.slots.default;
const __VLS_249 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_250 = __VLS_asFunctionalComponent(__VLS_249, new __VLS_249({
    modelValue: (__VLS_ctx.form.contactName),
    placeholder: "请输入联系人",
}));
const __VLS_251 = __VLS_250({
    modelValue: (__VLS_ctx.form.contactName),
    placeholder: "请输入联系人",
}, ...__VLS_functionalComponentArgsRest(__VLS_250));
var __VLS_248;
const __VLS_253 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_254 = __VLS_asFunctionalComponent(__VLS_253, new __VLS_253({
    label: "联系电话",
    prop: "contactPhone",
}));
const __VLS_255 = __VLS_254({
    label: "联系电话",
    prop: "contactPhone",
}, ...__VLS_functionalComponentArgsRest(__VLS_254));
__VLS_256.slots.default;
const __VLS_257 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_258 = __VLS_asFunctionalComponent(__VLS_257, new __VLS_257({
    modelValue: (__VLS_ctx.form.contactPhone),
    placeholder: "请输入联系电话",
}));
const __VLS_259 = __VLS_258({
    modelValue: (__VLS_ctx.form.contactPhone),
    placeholder: "请输入联系电话",
}, ...__VLS_functionalComponentArgsRest(__VLS_258));
var __VLS_256;
const __VLS_261 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_262 = __VLS_asFunctionalComponent(__VLS_261, new __VLS_261({
    label: "区域",
    prop: "region",
}));
const __VLS_263 = __VLS_262({
    label: "区域",
    prop: "region",
}, ...__VLS_functionalComponentArgsRest(__VLS_262));
__VLS_264.slots.default;
const __VLS_265 = {}.ElCascader;
/** @type {[typeof __VLS_components.ElCascader, typeof __VLS_components.elCascader, ]} */ ;
// @ts-ignore
const __VLS_266 = __VLS_asFunctionalComponent(__VLS_265, new __VLS_265({
    modelValue: (__VLS_ctx.form.region),
    options: (__VLS_ctx.regionOptions),
    clearable: true,
    ...{ style: {} },
    placeholder: "请选择区域",
}));
const __VLS_267 = __VLS_266({
    modelValue: (__VLS_ctx.form.region),
    options: (__VLS_ctx.regionOptions),
    clearable: true,
    ...{ style: {} },
    placeholder: "请选择区域",
}, ...__VLS_functionalComponentArgsRest(__VLS_266));
var __VLS_264;
const __VLS_269 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_270 = __VLS_asFunctionalComponent(__VLS_269, new __VLS_269({
    label: "详细地址",
    prop: "address",
}));
const __VLS_271 = __VLS_270({
    label: "详细地址",
    prop: "address",
}, ...__VLS_functionalComponentArgsRest(__VLS_270));
__VLS_272.slots.default;
const __VLS_273 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_274 = __VLS_asFunctionalComponent(__VLS_273, new __VLS_273({
    modelValue: (__VLS_ctx.form.address),
    type: "textarea",
    rows: (3),
    placeholder: "请输入详细地址",
}));
const __VLS_275 = __VLS_274({
    modelValue: (__VLS_ctx.form.address),
    type: "textarea",
    rows: (3),
    placeholder: "请输入详细地址",
}, ...__VLS_functionalComponentArgsRest(__VLS_274));
var __VLS_272;
const __VLS_277 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_278 = __VLS_asFunctionalComponent(__VLS_277, new __VLS_277({
    label: "目标毛利率",
    prop: "targetGrossMargin",
}));
const __VLS_279 = __VLS_278({
    label: "目标毛利率",
    prop: "targetGrossMargin",
}, ...__VLS_functionalComponentArgsRest(__VLS_278));
__VLS_280.slots.default;
const __VLS_281 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_282 = __VLS_asFunctionalComponent(__VLS_281, new __VLS_281({
    modelValue: (__VLS_ctx.form.targetGrossMargin),
    placeholder: "请输入目标毛利率",
}));
const __VLS_283 = __VLS_282({
    modelValue: (__VLS_ctx.form.targetGrossMargin),
    placeholder: "请输入目标毛利率",
}, ...__VLS_functionalComponentArgsRest(__VLS_282));
__VLS_284.slots.default;
{
    const { suffix: __VLS_thisSlot } = __VLS_284.slots;
}
var __VLS_284;
var __VLS_280;
const __VLS_285 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_286 = __VLS_asFunctionalComponent(__VLS_285, new __VLS_285({
    label: "理想采销比",
    prop: "idealPurchaseSaleRatio",
}));
const __VLS_287 = __VLS_286({
    label: "理想采销比",
    prop: "idealPurchaseSaleRatio",
}, ...__VLS_functionalComponentArgsRest(__VLS_286));
__VLS_288.slots.default;
const __VLS_289 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_290 = __VLS_asFunctionalComponent(__VLS_289, new __VLS_289({
    modelValue: (__VLS_ctx.form.idealPurchaseSaleRatio),
    placeholder: "请输入理想采销比",
}));
const __VLS_291 = __VLS_290({
    modelValue: (__VLS_ctx.form.idealPurchaseSaleRatio),
    placeholder: "请输入理想采销比",
}, ...__VLS_functionalComponentArgsRest(__VLS_290));
__VLS_292.slots.default;
{
    const { suffix: __VLS_thisSlot } = __VLS_292.slots;
}
var __VLS_292;
var __VLS_288;
var __VLS_202;
{
    const { footer: __VLS_thisSlot } = __VLS_194.slots;
    const __VLS_293 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_294 = __VLS_asFunctionalComponent(__VLS_293, new __VLS_293({
        ...{ 'onClick': {} },
    }));
    const __VLS_295 = __VLS_294({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_294));
    let __VLS_297;
    let __VLS_298;
    let __VLS_299;
    const __VLS_300 = {
        onClick: (__VLS_ctx.closeDialog)
    };
    __VLS_296.slots.default;
    var __VLS_296;
    const __VLS_301 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_302 = __VLS_asFunctionalComponent(__VLS_301, new __VLS_301({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_303 = __VLS_302({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_302));
    let __VLS_305;
    let __VLS_306;
    let __VLS_307;
    const __VLS_308 = {
        onClick: (__VLS_ctx.handleSubmit)
    };
    __VLS_304.slots.default;
    var __VLS_304;
}
var __VLS_194;
const __VLS_309 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_310 = __VLS_asFunctionalComponent(__VLS_309, new __VLS_309({
    modelValue: (__VLS_ctx.viewDialogVisible),
    title: "仓库详情",
    width: "480px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_311 = __VLS_310({
    modelValue: (__VLS_ctx.viewDialogVisible),
    title: "仓库详情",
    width: "480px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_310));
__VLS_312.slots.default;
const __VLS_313 = {}.ElDescriptions;
/** @type {[typeof __VLS_components.ElDescriptions, typeof __VLS_components.elDescriptions, typeof __VLS_components.ElDescriptions, typeof __VLS_components.elDescriptions, ]} */ ;
// @ts-ignore
const __VLS_314 = __VLS_asFunctionalComponent(__VLS_313, new __VLS_313({
    column: (2),
    border: true,
    size: "small",
    labelClassName: "detail-label",
}));
const __VLS_315 = __VLS_314({
    column: (2),
    border: true,
    size: "small",
    labelClassName: "detail-label",
}, ...__VLS_functionalComponentArgsRest(__VLS_314));
__VLS_316.slots.default;
const __VLS_317 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_318 = __VLS_asFunctionalComponent(__VLS_317, new __VLS_317({
    label: "仓库编码",
}));
const __VLS_319 = __VLS_318({
    label: "仓库编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_318));
__VLS_320.slots.default;
(__VLS_ctx.viewingRow?.warehouseCode);
var __VLS_320;
const __VLS_321 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_322 = __VLS_asFunctionalComponent(__VLS_321, new __VLS_321({
    label: "仓库名称",
}));
const __VLS_323 = __VLS_322({
    label: "仓库名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_322));
__VLS_324.slots.default;
(__VLS_ctx.viewingRow?.warehouseName);
var __VLS_324;
const __VLS_325 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_326 = __VLS_asFunctionalComponent(__VLS_325, new __VLS_325({
    label: "仓库类型",
}));
const __VLS_327 = __VLS_326({
    label: "仓库类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_326));
__VLS_328.slots.default;
(__VLS_ctx.viewingRow?.warehouseType);
var __VLS_328;
const __VLS_329 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_330 = __VLS_asFunctionalComponent(__VLS_329, new __VLS_329({
    label: "所属部门",
}));
const __VLS_331 = __VLS_330({
    label: "所属部门",
}, ...__VLS_functionalComponentArgsRest(__VLS_330));
__VLS_332.slots.default;
(__VLS_ctx.viewingRow?.department || '-');
var __VLS_332;
const __VLS_333 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_334 = __VLS_asFunctionalComponent(__VLS_333, new __VLS_333({
    label: "状态",
}));
const __VLS_335 = __VLS_334({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_334));
__VLS_336.slots.default;
const __VLS_337 = {}.ElTag;
/** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
// @ts-ignore
const __VLS_338 = __VLS_asFunctionalComponent(__VLS_337, new __VLS_337({
    type: (__VLS_ctx.isStatusEnabled(__VLS_ctx.viewingRow?.status) ? 'success' : 'danger'),
    size: "small",
}));
const __VLS_339 = __VLS_338({
    type: (__VLS_ctx.isStatusEnabled(__VLS_ctx.viewingRow?.status) ? 'success' : 'danger'),
    size: "small",
}, ...__VLS_functionalComponentArgsRest(__VLS_338));
__VLS_340.slots.default;
(__VLS_ctx.formatStatusLabel(__VLS_ctx.viewingRow?.status));
var __VLS_340;
var __VLS_336;
const __VLS_341 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_342 = __VLS_asFunctionalComponent(__VLS_341, new __VLS_341({
    label: "是否默认",
}));
const __VLS_343 = __VLS_342({
    label: "是否默认",
}, ...__VLS_functionalComponentArgsRest(__VLS_342));
__VLS_344.slots.default;
const __VLS_345 = {}.ElTag;
/** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
// @ts-ignore
const __VLS_346 = __VLS_asFunctionalComponent(__VLS_345, new __VLS_345({
    type: (__VLS_ctx.viewingRow?.isDefault ? '' : 'info'),
    size: "small",
}));
const __VLS_347 = __VLS_346({
    type: (__VLS_ctx.viewingRow?.isDefault ? '' : 'info'),
    size: "small",
}, ...__VLS_functionalComponentArgsRest(__VLS_346));
__VLS_348.slots.default;
(__VLS_ctx.viewingRow?.isDefault ? '是' : '否');
var __VLS_348;
var __VLS_344;
const __VLS_349 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_350 = __VLS_asFunctionalComponent(__VLS_349, new __VLS_349({
    label: "联系人",
}));
const __VLS_351 = __VLS_350({
    label: "联系人",
}, ...__VLS_functionalComponentArgsRest(__VLS_350));
__VLS_352.slots.default;
(__VLS_ctx.viewingRow?.contactName || '-');
var __VLS_352;
const __VLS_353 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_354 = __VLS_asFunctionalComponent(__VLS_353, new __VLS_353({
    label: "联系电话",
}));
const __VLS_355 = __VLS_354({
    label: "联系电话",
}, ...__VLS_functionalComponentArgsRest(__VLS_354));
__VLS_356.slots.default;
(__VLS_ctx.viewingRow?.contactPhone || '-');
var __VLS_356;
const __VLS_357 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_358 = __VLS_asFunctionalComponent(__VLS_357, new __VLS_357({
    label: "详细地址",
    span: (2),
}));
const __VLS_359 = __VLS_358({
    label: "详细地址",
    span: (2),
}, ...__VLS_functionalComponentArgsRest(__VLS_358));
__VLS_360.slots.default;
(__VLS_ctx.viewingRow?.address || '-');
var __VLS_360;
const __VLS_361 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_362 = __VLS_asFunctionalComponent(__VLS_361, new __VLS_361({
    label: "目标毛利率",
}));
const __VLS_363 = __VLS_362({
    label: "目标毛利率",
}, ...__VLS_functionalComponentArgsRest(__VLS_362));
__VLS_364.slots.default;
(__VLS_ctx.viewingRow?.targetGrossMargin ? `${__VLS_ctx.viewingRow.targetGrossMargin}%` : '-');
var __VLS_364;
const __VLS_365 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_366 = __VLS_asFunctionalComponent(__VLS_365, new __VLS_365({
    label: "理想采销比",
}));
const __VLS_367 = __VLS_366({
    label: "理想采销比",
}, ...__VLS_functionalComponentArgsRest(__VLS_366));
__VLS_368.slots.default;
(__VLS_ctx.viewingRow?.idealPurchaseSaleRatio ? `${__VLS_ctx.viewingRow.idealPurchaseSaleRatio}%` : '-');
var __VLS_368;
const __VLS_369 = {}.ElDescriptionsItem;
/** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
// @ts-ignore
const __VLS_370 = __VLS_asFunctionalComponent(__VLS_369, new __VLS_369({
    label: "操作时间",
    span: (2),
}));
const __VLS_371 = __VLS_370({
    label: "操作时间",
    span: (2),
}, ...__VLS_functionalComponentArgsRest(__VLS_370));
__VLS_372.slots.default;
(__VLS_ctx.formatDateTime(__VLS_ctx.viewingRow?.updatedAt));
var __VLS_372;
var __VLS_316;
{
    const { footer: __VLS_thisSlot } = __VLS_312.slots;
    const __VLS_373 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_374 = __VLS_asFunctionalComponent(__VLS_373, new __VLS_373({
        ...{ 'onClick': {} },
    }));
    const __VLS_375 = __VLS_374({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_374));
    let __VLS_377;
    let __VLS_378;
    let __VLS_379;
    const __VLS_380 = {
        onClick: (...[$event]) => {
            __VLS_ctx.viewDialogVisible = false;
        }
    };
    __VLS_376.slots.default;
    var __VLS_376;
}
var __VLS_312;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
// @ts-ignore
var __VLS_204 = __VLS_203;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CommonQuerySection: CommonQuerySection,
            CommonToolbarSection: CommonToolbarSection,
            toolbarButtons: toolbarButtons,
            statusOptions: statusOptions,
            warehouseTypeOptions: warehouseTypeOptions,
            warehouseFormTypeOptions: warehouseFormTypeOptions,
            departmentOptions: departmentOptions,
            regionOptions: regionOptions,
            dialogVisible: dialogVisible,
            dialogTitle: dialogTitle,
            viewDialogVisible: viewDialogVisible,
            viewingRow: viewingRow,
            formRef: formRef,
            form: form,
            formRules: formRules,
            query: query,
            storeOptions: storeOptions,
            selectedStoreId: selectedStoreId,
            tableData: tableData,
            selectedIds: selectedIds,
            currentPage: currentPage,
            pageSize: pageSize,
            loading: loading,
            totalItems: totalItems,
            isStatusEnabled: isStatusEnabled,
            formatStatusLabel: formatStatusLabel,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleStoreChange: handleStoreChange,
            handleSelectionChange: handleSelectionChange,
            formatDateTime: formatDateTime,
            handleEdit: handleEdit,
            handleView: handleView,
            handleSetDefault: handleSetDefault,
            handleDelete: handleDelete,
            handleToggleStatus: handleToggleStatus,
            closeDialog: closeDialog,
            handleSubmit: handleSubmit,
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
