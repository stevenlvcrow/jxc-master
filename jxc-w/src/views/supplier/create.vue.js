/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { createSupplierApi, fetchSupplierDetailApi, fetchSupplierCategoryTreeApi, updateSupplierApi, } from '@/api/modules/supplier';
import { useSessionStore } from '@/stores/session';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonCodeField from '@/components/CommonCodeField.vue';
import CommonMnemonicField from '@/components/CommonMnemonicField.vue';
const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const supplierId = computed(() => {
    const raw = route.params.id;
    const value = Array.isArray(raw) ? raw[0] : raw;
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});
const isEditMode = computed(() => supplierId.value !== null);
const sectionNavs = [
    { key: 'basic', label: '基础信息' },
    { key: 'params', label: '参数设置' },
    { key: 'supply', label: '供货关系' },
    { key: 'qualification', label: '资质管理' },
    { key: 'contract', label: '合同管理' },
    { key: 'finance', label: '财务信息' },
    { key: 'invoice', label: '开票信息' },
];
const activeSectionKey = ref('basic');
const contentScrollEl = ref(null);
const sectionRefs = ref({
    basic: null,
    params: null,
    supply: null,
    qualification: null,
    contract: null,
    finance: null,
    invoice: null,
});
const supplierCategoryTree = ref([]);
const settlementMethods = ['预付款', '货到付款', '日结', '月结'];
const qualificationTypeOptions = ['营业执照', '食品经营许可证', '餐饮服务许可证', '生产许可证', '卫生许可证', '其他'];
const reconciliationModeOptions = [
    '模式1：不允许供应商在系统发起对账',
    '模式2：允许供应商查看与我往来单据，并发起明对账申请',
    '模式3：允许供应商发起盲对账申请，支撑查看对账差异',
    '模式4：允许供应商发起盲对账申请，不支持查看对账差异',
];
const form = reactive({
    supplierCode: '',
    supplierName: '',
    supplierShortName: '',
    supplierMnemonic: '',
    supplierCategory: '',
    taxRate: 13,
    enabled: true,
    contactPerson: '',
    contactPhone: '',
    email: '',
    contactAddress: '',
    remark: '',
    settlementMethod: '月结',
    orderSummaryRule: '按机构',
    inputBatchWhenDelivery: false,
    syncReceiptData: true,
    purchaseReceiptDependShipping: '不依赖',
    deliveryDependShipping: '不依赖',
    supplierManageInventory: false,
    controlOrderTime: false,
    allowCloseOrder: true,
    reconciliationMode: reconciliationModeOptions[0],
    scopeControl: '开启',
    invoiceCompanyName: '',
    taxpayerId: '',
    invoicePhone: '',
    invoiceAddress: '',
});
const qualificationSeed = ref(1);
const contractSeed = ref(1);
const financeSeed = ref(1);
const createQualificationRow = () => ({
    id: `qualification-${qualificationSeed.value++}`,
    files: [],
    qualificationType: '营业执照',
    validTo: '',
    remark: '',
});
const createContractRow = () => ({
    id: `contract-${contractSeed.value++}`,
    files: [],
    contractName: '',
    contractCode: '',
    validTo: '',
});
const createFinanceRow = () => ({
    id: `finance-${financeSeed.value++}`,
    bankAccount: '',
    accountName: '',
    bankName: '',
});
const qualificationRows = ref([createQualificationRow()]);
const contractRows = ref([createContractRow()]);
const financeRows = ref([createFinanceRow()]);
const financeDefaultId = ref(financeRows.value[0].id);
const addQualificationRow = (index) => {
    qualificationRows.value.splice(index + 1, 0, createQualificationRow());
};
const removeQualificationRow = (index) => {
    if (qualificationRows.value.length <= 1) {
        ElMessage.warning('至少保留一条资质记录');
        return;
    }
    qualificationRows.value.splice(index, 1);
};
const addContractRow = (index) => {
    contractRows.value.splice(index + 1, 0, createContractRow());
};
const removeContractRow = (index) => {
    if (contractRows.value.length <= 1) {
        ElMessage.warning('至少保留一条合同记录');
        return;
    }
    contractRows.value.splice(index, 1);
};
const addFinanceRow = (index) => {
    financeRows.value.splice(index + 1, 0, createFinanceRow());
};
const removeFinanceRow = (index) => {
    if (financeRows.value.length <= 1) {
        ElMessage.warning('至少保留一条财务记录');
        return;
    }
    const target = financeRows.value[index];
    financeRows.value.splice(index, 1);
    if (financeDefaultId.value === target.id) {
        financeDefaultId.value = financeRows.value[0].id;
    }
};
const resolveContractStatus = (validTo) => {
    if (!validTo) {
        return '待维护';
    }
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const target = new Date(`${validTo}T00:00:00`);
    const diffDays = Math.floor((target.getTime() - now.getTime()) / (24 * 60 * 60 * 1000));
    if (diffDays < 0) {
        return '已过期';
    }
    if (diffDays <= 30) {
        return '临期';
    }
    return '有效';
};
const registerSectionRef = (key) => (el) => {
    if (!el) {
        sectionRefs.value[key] = null;
        return;
    }
    if ('$el' in el) {
        sectionRefs.value[key] = el.$el;
        return;
    }
    sectionRefs.value[key] = el;
};
const updateActiveSectionByScroll = () => {
    const container = contentScrollEl.value;
    if (!container) {
        return;
    }
    const containerTop = container.getBoundingClientRect().top;
    const offset = 74;
    let current = sectionNavs[0].key;
    sectionNavs.forEach((nav) => {
        const sectionEl = sectionRefs.value[nav.key];
        if (!sectionEl) {
            return;
        }
        const top = sectionEl.getBoundingClientRect().top - containerTop;
        if (top <= offset) {
            current = nav.key;
        }
    });
    activeSectionKey.value = current;
};
const scrollToSection = (key) => {
    if (!sectionNavs.some((item) => item.key === key)) {
        return;
    }
    const sectionKey = key;
    const target = sectionRefs.value[sectionKey];
    if (!target) {
        return;
    }
    activeSectionKey.value = sectionKey;
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
onMounted(async () => {
    contentScrollEl.value = document.querySelector('.content');
    contentScrollEl.value?.addEventListener('scroll', updateActiveSectionByScroll, { passive: true });
    updateActiveSectionByScroll();
    await fetchCategoryTree();
    if (isEditMode.value && supplierId.value) {
        try {
            await loadSupplierDetail(supplierId.value);
        }
        catch {
            router.push('/archive/3/1');
        }
    }
});
onBeforeUnmount(() => {
    contentScrollEl.value?.removeEventListener('scroll', updateActiveSectionByScroll);
});
const goBack = () => {
    router.push('/archive/3/1');
};
const saveDraft = () => {
    ElMessage.success('草稿保存成功');
};
const resolveSectionByField = (fieldPath) => {
    const normalized = fieldPath.replace(/\[\d+\]/g, '');
    if ([
        'supplierCode', 'supplierName', 'supplierShortName', 'supplierMnemonic', 'supplierCategory', 'taxRate',
        'status', 'contactPerson', 'contactPhone', 'email', 'contactAddress', 'remark',
    ].some((key) => normalized.startsWith(key))) {
        return 'basic';
    }
    if ([
        'settlementMethod', 'orderSummaryRule', 'inputBatchWhenDelivery', 'syncReceiptData',
        'purchaseReceiptDependShipping', 'deliveryDependShipping', 'supplierManageInventory',
        'controlOrderTime', 'allowCloseOrder', 'reconciliationMode',
    ].some((key) => normalized.startsWith(key))) {
        return 'params';
    }
    if (normalized.startsWith('scopeControl')) {
        return 'supply';
    }
    if (normalized.startsWith('qualificationList')) {
        return 'qualification';
    }
    if (normalized.startsWith('contractList')) {
        return 'contract';
    }
    if (normalized.startsWith('financeList')) {
        return 'finance';
    }
    if ([
        'invoiceCompanyName', 'taxpayerId', 'invoicePhone', 'invoiceAddress',
    ].some((key) => normalized.startsWith(key))) {
        return 'invoice';
    }
    return null;
};
const resolveFirstValidationField = (message) => {
    if (!message) {
        return null;
    }
    const parts = message.split(';').map((item) => item.trim()).filter(Boolean);
    for (const part of parts) {
        const index = part.indexOf(':');
        if (index > 0) {
            const field = part.slice(0, index).replace(/^请求参数校验失败/, '').replace(/^[:：]\s*/, '').trim();
            if (field && !field.includes('请求参数校验失败')) {
                return field;
            }
        }
    }
    const match = message.match(/([a-zA-Z][\w.[\]]*)\s*[:：]/);
    return match?.[1] ?? null;
};
const focusFieldByPath = async (fieldPath) => {
    await nextTick();
    const root = document.querySelector('.item-create-page');
    if (!root) {
        return;
    }
    const containers = Array.from(root.querySelectorAll('[data-field]'));
    const container = containers.find((item) => item.getAttribute('data-field') === fieldPath);
    if (!container) {
        return;
    }
    const focusTarget = container.querySelector('input, textarea, .el-input__inner, .el-textarea__inner, button, .el-select__wrapper, .el-date-editor') ?? container;
    if (typeof focusTarget.focus === 'function') {
        focusTarget.focus();
    }
};
const resolveSupplierOrgId = () => {
    const orgId = (sessionStore.currentOrgId ?? '').trim();
    if (!orgId) {
        return undefined;
    }
    if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
        return orgId;
    }
    return undefined;
};
const categoryExists = (nodes, value) => {
    for (const node of nodes) {
        if (node.label === value) {
            return true;
        }
        if (node.children?.length && categoryExists(node.children, value)) {
            return true;
        }
    }
    return false;
};
const fetchCategoryTree = async () => {
    try {
        supplierCategoryTree.value = await fetchSupplierCategoryTreeApi(resolveSupplierOrgId());
    }
    catch {
        supplierCategoryTree.value = [];
        ElMessage.error('供应商类别加载失败');
    }
    if (form.supplierCategory && !categoryExists(supplierCategoryTree.value, form.supplierCategory)) {
        form.supplierCategory = '';
    }
};
const buildUploadFiles = (name, url) => {
    if (!name && !url) {
        return [];
    }
    return [{
            uid: Date.now(),
            name: name || '已上传文件',
            url,
            status: 'success',
        }];
};
const loadSupplierDetail = async (id) => {
    const detail = await fetchSupplierDetailApi(id, resolveSupplierOrgId());
    form.supplierCode = detail.supplierCode ?? '';
    form.supplierName = detail.supplierName ?? '';
    form.supplierShortName = detail.supplierShortName ?? '';
    form.supplierMnemonic = detail.supplierMnemonic ?? '';
    form.supplierCategory = detail.supplierCategory ?? '';
    form.taxRate = Number(detail.taxRate ?? 0);
    form.enabled = detail.status !== '停用';
    form.contactPerson = detail.contactPerson ?? '';
    form.contactPhone = detail.contactPhone ?? '';
    form.email = detail.email ?? '';
    form.contactAddress = detail.contactAddress ?? '';
    form.remark = detail.remark ?? '';
    form.settlementMethod = (detail.settlementMethod || '月结');
    form.orderSummaryRule = (detail.orderSummaryRule || '按机构');
    form.inputBatchWhenDelivery = Boolean(detail.inputBatchWhenDelivery);
    form.syncReceiptData = Boolean(detail.syncReceiptData);
    form.purchaseReceiptDependShipping = (detail.purchaseReceiptDependShipping || '不依赖');
    form.deliveryDependShipping = (detail.deliveryDependShipping || '不依赖');
    form.supplierManageInventory = Boolean(detail.supplierManageInventory);
    form.controlOrderTime = Boolean(detail.controlOrderTime);
    form.allowCloseOrder = Boolean(detail.allowCloseOrder);
    form.reconciliationMode = detail.reconciliationMode || reconciliationModeOptions[0];
    form.scopeControl = (detail.scopeControl || '开启');
    form.invoiceCompanyName = detail.invoiceCompanyName ?? '';
    form.taxpayerId = detail.taxpayerId ?? '';
    form.invoicePhone = detail.invoicePhone ?? '';
    form.invoiceAddress = detail.invoiceAddress ?? '';
    qualificationRows.value = (detail.qualificationList?.length ? detail.qualificationList : [{
            qualificationType: '营业执照',
        }]).map((item) => ({
        id: `qualification-${qualificationSeed.value++}`,
        files: buildUploadFiles(item.fileName, item.fileUrl),
        qualificationType: item.qualificationType || '营业执照',
        validTo: item.validTo || '',
        remark: item.remark || '',
    }));
    contractRows.value = (detail.contractList?.length ? detail.contractList : [{}]).map((item) => ({
        id: `contract-${contractSeed.value++}`,
        files: buildUploadFiles(item.attachmentName, item.attachmentUrl),
        contractName: item.contractName || '',
        contractCode: item.contractCode || '',
        validTo: item.validTo || '',
    }));
    const financeList = detail.financeList ?? [];
    financeRows.value = (financeList.length ? financeList : [{
            bankAccount: '',
            accountName: '',
            bankName: '',
            defaultAccount: true,
        }]).map((item) => ({
        id: `finance-${financeSeed.value++}`,
        bankAccount: item.bankAccount || '',
        accountName: item.accountName || '',
        bankName: item.bankName || '',
    }));
    const defaultIndex = financeList.findIndex((item) => item.defaultAccount);
    financeDefaultId.value = financeRows.value[Math.max(defaultIndex, 0)]?.id || financeRows.value[0].id;
};
const buildPayload = () => ({
    supplierCode: isEditMode.value ? form.supplierCode.trim() : undefined,
    supplierName: form.supplierName.trim(),
    supplierShortName: form.supplierShortName.trim() || undefined,
    supplierMnemonic: form.supplierMnemonic.trim() || undefined,
    supplierCategory: form.supplierCategory.trim(),
    taxRate: Number(form.taxRate),
    status: form.enabled ? '启用' : '停用',
    contactPerson: form.contactPerson.trim() || undefined,
    contactPhone: form.contactPhone.trim() || undefined,
    email: form.email.trim() || undefined,
    contactAddress: form.contactAddress.trim() || undefined,
    remark: form.remark.trim() || undefined,
    settlementMethod: form.settlementMethod,
    orderSummaryRule: form.orderSummaryRule,
    inputBatchWhenDelivery: form.inputBatchWhenDelivery,
    syncReceiptData: form.syncReceiptData,
    purchaseReceiptDependShipping: form.purchaseReceiptDependShipping,
    deliveryDependShipping: form.deliveryDependShipping,
    supplierManageInventory: form.supplierManageInventory,
    controlOrderTime: form.controlOrderTime,
    allowCloseOrder: form.allowCloseOrder,
    reconciliationMode: form.reconciliationMode,
    scopeControl: form.scopeControl,
    qualificationList: qualificationRows.value.map((row) => ({
        fileName: row.files?.map((file) => file.name).join(',') || undefined,
        fileUrl: row.files?.[0]?.url || undefined,
        qualificationType: row.qualificationType,
        validTo: row.validTo || undefined,
        status: '临期',
        remark: row.remark.trim() || undefined,
    })),
    contractList: contractRows.value.map((row) => ({
        attachmentName: row.files?.map((file) => file.name).join(',') || undefined,
        attachmentUrl: row.files?.[0]?.url || undefined,
        contractName: row.contractName.trim() || undefined,
        contractCode: row.contractCode.trim() || undefined,
        validTo: row.validTo || undefined,
        status: resolveContractStatus(row.validTo),
    })),
    financeList: financeRows.value.map((row) => ({
        bankAccount: row.bankAccount.trim(),
        accountName: row.accountName.trim(),
        bankName: row.bankName.trim(),
        defaultAccount: row.id === financeDefaultId.value,
    })),
    invoiceCompanyName: form.invoiceCompanyName.trim() || undefined,
    taxpayerId: form.taxpayerId.trim() || undefined,
    invoicePhone: form.invoicePhone.trim() || undefined,
    invoiceAddress: form.invoiceAddress.trim() || undefined,
});
const saveSupplier = async () => {
    const hasDefaultAccount = financeRows.value.some((item) => item.id === financeDefaultId.value);
    if (!hasDefaultAccount) {
        ElMessage.warning('请设置默认银行账号');
        return;
    }
    try {
        const payload = buildPayload();
        if (isEditMode.value && supplierId.value) {
            await updateSupplierApi(supplierId.value, payload, resolveSupplierOrgId());
        }
        else {
            await createSupplierApi(payload, resolveSupplierOrgId());
        }
        ElMessage.success(isEditMode.value ? '供应商更新成功' : '供应商保存成功');
        router.push('/archive/3/1');
    }
    catch (error) {
        const message = error instanceof Error ? error.message : '';
        const fieldPath = resolveFirstValidationField(message);
        if (fieldPath) {
            const section = resolveSectionByField(fieldPath);
            if (section) {
                scrollToSection(section);
            }
            await focusFieldByPath(fieldPath);
            ElMessage.warning(`校验失败字段：${fieldPath}`);
        }
    }
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['supplier-cell-center']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-create-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel form-panel" },
});
/** @type {[typeof FixedActionBreadcrumb, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(FixedActionBreadcrumb, new FixedActionBreadcrumb({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.sectionNavs),
    activeKey: (__VLS_ctx.activeSectionKey),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.sectionNavs),
    activeKey: (__VLS_ctx.activeSectionKey),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onBack: (__VLS_ctx.goBack)
};
const __VLS_7 = {
    onSaveDraft: (__VLS_ctx.saveDraft)
};
const __VLS_8 = {
    onSave: (__VLS_ctx.saveSupplier)
};
const __VLS_9 = {
    onNavigate: (__VLS_ctx.scrollToSection)
};
var __VLS_2;
const __VLS_10 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
    model: (__VLS_ctx.form),
    labelWidth: "100px",
    ...{ class: "item-create-form" },
}));
const __VLS_12 = __VLS_11({
    model: (__VLS_ctx.form),
    labelWidth: "100px",
    ...{ class: "item-create-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_11));
__VLS_13.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('basic')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
/** @type {[typeof CommonCodeField, ]} */ ;
// @ts-ignore
const __VLS_14 = __VLS_asFunctionalComponent(CommonCodeField, new CommonCodeField({
    label: "供货商编码",
    value: (__VLS_ctx.form.supplierCode),
    placeholder: "保存后生成",
}));
const __VLS_15 = __VLS_14({
    label: "供货商编码",
    value: (__VLS_ctx.form.supplierCode),
    placeholder: "保存后生成",
}, ...__VLS_functionalComponentArgsRest(__VLS_14));
const __VLS_17 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
    label: "供货商名称",
    dataField: "supplierName",
}));
const __VLS_19 = __VLS_18({
    label: "供货商名称",
    dataField: "supplierName",
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
__VLS_20.slots.default;
const __VLS_21 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_22 = __VLS_asFunctionalComponent(__VLS_21, new __VLS_21({
    modelValue: (__VLS_ctx.form.supplierName),
    placeholder: "请输入供货商名称",
}));
const __VLS_23 = __VLS_22({
    modelValue: (__VLS_ctx.form.supplierName),
    placeholder: "请输入供货商名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_22));
var __VLS_20;
/** @type {[typeof CommonMnemonicField, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(CommonMnemonicField, new CommonMnemonicField({
    sourceValue: (__VLS_ctx.form.supplierShortName),
    modelValue: (__VLS_ctx.form.supplierMnemonic),
    sourceLabel: "供货商简称",
    mnemonicLabel: "供货商助记码",
    sourceField: "supplierShortName",
    mnemonicField: "supplierMnemonic",
    sourcePlaceholder: "请输入供货商简称",
    mnemonicPlaceholder: "根据供货商简称自动生成",
}));
const __VLS_26 = __VLS_25({
    sourceValue: (__VLS_ctx.form.supplierShortName),
    modelValue: (__VLS_ctx.form.supplierMnemonic),
    sourceLabel: "供货商简称",
    mnemonicLabel: "供货商助记码",
    sourceField: "supplierShortName",
    mnemonicField: "supplierMnemonic",
    sourcePlaceholder: "请输入供货商简称",
    mnemonicPlaceholder: "根据供货商简称自动生成",
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
const __VLS_28 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    label: "供货商类别",
    dataField: "supplierCategory",
}));
const __VLS_30 = __VLS_29({
    label: "供货商类别",
    dataField: "supplierCategory",
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
__VLS_31.slots.default;
const __VLS_32 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    modelValue: (__VLS_ctx.form.supplierCategory),
    data: (__VLS_ctx.supplierCategoryTree),
    nodeKey: "id",
    props: ({ label: 'label', children: 'children', value: 'label' }),
    checkStrictly: true,
    clearable: true,
    placeholder: "请选择供货商类别",
}));
const __VLS_34 = __VLS_33({
    modelValue: (__VLS_ctx.form.supplierCategory),
    data: (__VLS_ctx.supplierCategoryTree),
    nodeKey: "id",
    props: ({ label: 'label', children: 'children', value: 'label' }),
    checkStrictly: true,
    clearable: true,
    placeholder: "请选择供货商类别",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
var __VLS_31;
const __VLS_36 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    label: "税率(%)",
    dataField: "taxRate",
}));
const __VLS_38 = __VLS_37({
    label: "税率(%)",
    dataField: "taxRate",
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
__VLS_39.slots.default;
const __VLS_40 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
    modelValue: (__VLS_ctx.form.taxRate),
    min: (0),
    max: (100),
    precision: (2),
    step: (0.5),
}));
const __VLS_42 = __VLS_41({
    modelValue: (__VLS_ctx.form.taxRate),
    min: (0),
    max: (100),
    precision: (2),
    step: (0.5),
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
var __VLS_39;
const __VLS_44 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
    label: "启用状态",
    dataField: "status",
}));
const __VLS_46 = __VLS_45({
    label: "启用状态",
    dataField: "status",
}, ...__VLS_functionalComponentArgsRest(__VLS_45));
__VLS_47.slots.default;
const __VLS_48 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
    modelValue: (__VLS_ctx.form.enabled),
    activeText: "启用",
    inactiveText: "停用",
}));
const __VLS_50 = __VLS_49({
    modelValue: (__VLS_ctx.form.enabled),
    activeText: "启用",
    inactiveText: "停用",
}, ...__VLS_functionalComponentArgsRest(__VLS_49));
var __VLS_47;
const __VLS_52 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
    label: "联系人",
    dataField: "contactPerson",
}));
const __VLS_54 = __VLS_53({
    label: "联系人",
    dataField: "contactPerson",
}, ...__VLS_functionalComponentArgsRest(__VLS_53));
__VLS_55.slots.default;
const __VLS_56 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    modelValue: (__VLS_ctx.form.contactPerson),
    placeholder: "请输入联系人",
}));
const __VLS_58 = __VLS_57({
    modelValue: (__VLS_ctx.form.contactPerson),
    placeholder: "请输入联系人",
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
var __VLS_55;
const __VLS_60 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
    label: "联系电话",
    dataField: "contactPhone",
}));
const __VLS_62 = __VLS_61({
    label: "联系电话",
    dataField: "contactPhone",
}, ...__VLS_functionalComponentArgsRest(__VLS_61));
__VLS_63.slots.default;
const __VLS_64 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
    modelValue: (__VLS_ctx.form.contactPhone),
    placeholder: "请输入联系电话",
}));
const __VLS_66 = __VLS_65({
    modelValue: (__VLS_ctx.form.contactPhone),
    placeholder: "请输入联系电话",
}, ...__VLS_functionalComponentArgsRest(__VLS_65));
var __VLS_63;
const __VLS_68 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
    label: "邮箱地址",
    dataField: "email",
}));
const __VLS_70 = __VLS_69({
    label: "邮箱地址",
    dataField: "email",
}, ...__VLS_functionalComponentArgsRest(__VLS_69));
__VLS_71.slots.default;
const __VLS_72 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
    modelValue: (__VLS_ctx.form.email),
    placeholder: "请输入邮箱地址",
}));
const __VLS_74 = __VLS_73({
    modelValue: (__VLS_ctx.form.email),
    placeholder: "请输入邮箱地址",
}, ...__VLS_functionalComponentArgsRest(__VLS_73));
var __VLS_71;
const __VLS_76 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
    label: "联系地址",
    ...{ class: "supplier-form-half" },
    dataField: "contactAddress",
}));
const __VLS_78 = __VLS_77({
    label: "联系地址",
    ...{ class: "supplier-form-half" },
    dataField: "contactAddress",
}, ...__VLS_functionalComponentArgsRest(__VLS_77));
__VLS_79.slots.default;
const __VLS_80 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
    modelValue: (__VLS_ctx.form.contactAddress),
    placeholder: "请输入联系地址",
}));
const __VLS_82 = __VLS_81({
    modelValue: (__VLS_ctx.form.contactAddress),
    placeholder: "请输入联系地址",
}, ...__VLS_functionalComponentArgsRest(__VLS_81));
var __VLS_79;
const __VLS_84 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
    label: "备注",
    ...{ class: "supplier-form-full" },
    dataField: "remark",
}));
const __VLS_86 = __VLS_85({
    label: "备注",
    ...{ class: "supplier-form-full" },
    dataField: "remark",
}, ...__VLS_functionalComponentArgsRest(__VLS_85));
__VLS_87.slots.default;
const __VLS_88 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
    modelValue: (__VLS_ctx.form.remark),
    type: "textarea",
    rows: (3),
    placeholder: "请输入备注",
}));
const __VLS_90 = __VLS_89({
    modelValue: (__VLS_ctx.form.remark),
    type: "textarea",
    rows: (3),
    placeholder: "请输入备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_89));
var __VLS_87;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('params')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_92 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
    label: "结算方式",
    dataField: "settlementMethod",
}));
const __VLS_94 = __VLS_93({
    label: "结算方式",
    dataField: "settlementMethod",
}, ...__VLS_functionalComponentArgsRest(__VLS_93));
__VLS_95.slots.default;
const __VLS_96 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
    modelValue: (__VLS_ctx.form.settlementMethod),
}));
const __VLS_98 = __VLS_97({
    modelValue: (__VLS_ctx.form.settlementMethod),
}, ...__VLS_functionalComponentArgsRest(__VLS_97));
__VLS_99.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.settlementMethods))) {
    const __VLS_100 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
        key: (item),
        label: (item),
        value: (item),
    }));
    const __VLS_102 = __VLS_101({
        key: (item),
        label: (item),
        value: (item),
    }, ...__VLS_functionalComponentArgsRest(__VLS_101));
}
var __VLS_99;
var __VLS_95;
const __VLS_104 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
    label: "订单汇总规则",
    ...{ class: "supplier-form-half" },
    dataField: "orderSummaryRule",
}));
const __VLS_106 = __VLS_105({
    label: "订单汇总规则",
    ...{ class: "supplier-form-half" },
    dataField: "orderSummaryRule",
}, ...__VLS_functionalComponentArgsRest(__VLS_105));
__VLS_107.slots.default;
const __VLS_108 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
    modelValue: (__VLS_ctx.form.orderSummaryRule),
}));
const __VLS_110 = __VLS_109({
    modelValue: (__VLS_ctx.form.orderSummaryRule),
}, ...__VLS_functionalComponentArgsRest(__VLS_109));
__VLS_111.slots.default;
const __VLS_112 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
    label: "按机构",
}));
const __VLS_114 = __VLS_113({
    label: "按机构",
}, ...__VLS_functionalComponentArgsRest(__VLS_113));
__VLS_115.slots.default;
var __VLS_115;
const __VLS_116 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
    label: "按仓库",
}));
const __VLS_118 = __VLS_117({
    label: "按仓库",
}, ...__VLS_functionalComponentArgsRest(__VLS_117));
__VLS_119.slots.default;
var __VLS_119;
var __VLS_111;
var __VLS_107;
const __VLS_120 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({
    label: "发货时录入批次",
    dataField: "inputBatchWhenDelivery",
}));
const __VLS_122 = __VLS_121({
    label: "发货时录入批次",
    dataField: "inputBatchWhenDelivery",
}, ...__VLS_functionalComponentArgsRest(__VLS_121));
__VLS_123.slots.default;
const __VLS_124 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_125 = __VLS_asFunctionalComponent(__VLS_124, new __VLS_124({
    modelValue: (__VLS_ctx.form.inputBatchWhenDelivery),
}));
const __VLS_126 = __VLS_125({
    modelValue: (__VLS_ctx.form.inputBatchWhenDelivery),
}, ...__VLS_functionalComponentArgsRest(__VLS_125));
var __VLS_123;
const __VLS_128 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_129 = __VLS_asFunctionalComponent(__VLS_128, new __VLS_128({
    label: "同步收货数据",
    dataField: "syncReceiptData",
}));
const __VLS_130 = __VLS_129({
    label: "同步收货数据",
    dataField: "syncReceiptData",
}, ...__VLS_functionalComponentArgsRest(__VLS_129));
__VLS_131.slots.default;
const __VLS_132 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_133 = __VLS_asFunctionalComponent(__VLS_132, new __VLS_132({
    modelValue: (__VLS_ctx.form.syncReceiptData),
}));
const __VLS_134 = __VLS_133({
    modelValue: (__VLS_ctx.form.syncReceiptData),
}, ...__VLS_functionalComponentArgsRest(__VLS_133));
var __VLS_131;
const __VLS_136 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_137 = __VLS_asFunctionalComponent(__VLS_136, new __VLS_136({
    label: "采购收货依赖供货商发货",
    ...{ class: "supplier-form-half" },
    dataField: "purchaseReceiptDependShipping",
}));
const __VLS_138 = __VLS_137({
    label: "采购收货依赖供货商发货",
    ...{ class: "supplier-form-half" },
    dataField: "purchaseReceiptDependShipping",
}, ...__VLS_functionalComponentArgsRest(__VLS_137));
__VLS_139.slots.default;
const __VLS_140 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_141 = __VLS_asFunctionalComponent(__VLS_140, new __VLS_140({
    modelValue: (__VLS_ctx.form.purchaseReceiptDependShipping),
}));
const __VLS_142 = __VLS_141({
    modelValue: (__VLS_ctx.form.purchaseReceiptDependShipping),
}, ...__VLS_functionalComponentArgsRest(__VLS_141));
__VLS_143.slots.default;
const __VLS_144 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_145 = __VLS_asFunctionalComponent(__VLS_144, new __VLS_144({
    label: "不依赖",
}));
const __VLS_146 = __VLS_145({
    label: "不依赖",
}, ...__VLS_functionalComponentArgsRest(__VLS_145));
__VLS_147.slots.default;
var __VLS_147;
const __VLS_148 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_149 = __VLS_asFunctionalComponent(__VLS_148, new __VLS_148({
    label: "依赖",
}));
const __VLS_150 = __VLS_149({
    label: "依赖",
}, ...__VLS_functionalComponentArgsRest(__VLS_149));
__VLS_151.slots.default;
var __VLS_151;
var __VLS_143;
var __VLS_139;
const __VLS_152 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_153 = __VLS_asFunctionalComponent(__VLS_152, new __VLS_152({
    label: "配送依赖供应商发货",
    ...{ class: "supplier-form-half" },
    dataField: "deliveryDependShipping",
}));
const __VLS_154 = __VLS_153({
    label: "配送依赖供应商发货",
    ...{ class: "supplier-form-half" },
    dataField: "deliveryDependShipping",
}, ...__VLS_functionalComponentArgsRest(__VLS_153));
__VLS_155.slots.default;
const __VLS_156 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_157 = __VLS_asFunctionalComponent(__VLS_156, new __VLS_156({
    modelValue: (__VLS_ctx.form.deliveryDependShipping),
}));
const __VLS_158 = __VLS_157({
    modelValue: (__VLS_ctx.form.deliveryDependShipping),
}, ...__VLS_functionalComponentArgsRest(__VLS_157));
__VLS_159.slots.default;
const __VLS_160 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_161 = __VLS_asFunctionalComponent(__VLS_160, new __VLS_160({
    label: "不依赖",
}));
const __VLS_162 = __VLS_161({
    label: "不依赖",
}, ...__VLS_functionalComponentArgsRest(__VLS_161));
__VLS_163.slots.default;
var __VLS_163;
const __VLS_164 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_165 = __VLS_asFunctionalComponent(__VLS_164, new __VLS_164({
    label: "依赖",
}));
const __VLS_166 = __VLS_165({
    label: "依赖",
}, ...__VLS_functionalComponentArgsRest(__VLS_165));
__VLS_167.slots.default;
var __VLS_167;
var __VLS_159;
var __VLS_155;
const __VLS_168 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_169 = __VLS_asFunctionalComponent(__VLS_168, new __VLS_168({
    label: "供货商管理库存",
    dataField: "supplierManageInventory",
}));
const __VLS_170 = __VLS_169({
    label: "供货商管理库存",
    dataField: "supplierManageInventory",
}, ...__VLS_functionalComponentArgsRest(__VLS_169));
__VLS_171.slots.default;
const __VLS_172 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_173 = __VLS_asFunctionalComponent(__VLS_172, new __VLS_172({
    modelValue: (__VLS_ctx.form.supplierManageInventory),
}));
const __VLS_174 = __VLS_173({
    modelValue: (__VLS_ctx.form.supplierManageInventory),
}, ...__VLS_functionalComponentArgsRest(__VLS_173));
var __VLS_171;
const __VLS_176 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_177 = __VLS_asFunctionalComponent(__VLS_176, new __VLS_176({
    label: "控制下单时间",
    dataField: "controlOrderTime",
}));
const __VLS_178 = __VLS_177({
    label: "控制下单时间",
    dataField: "controlOrderTime",
}, ...__VLS_functionalComponentArgsRest(__VLS_177));
__VLS_179.slots.default;
const __VLS_180 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_181 = __VLS_asFunctionalComponent(__VLS_180, new __VLS_180({
    modelValue: (__VLS_ctx.form.controlOrderTime),
}));
const __VLS_182 = __VLS_181({
    modelValue: (__VLS_ctx.form.controlOrderTime),
}, ...__VLS_functionalComponentArgsRest(__VLS_181));
var __VLS_179;
const __VLS_184 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_185 = __VLS_asFunctionalComponent(__VLS_184, new __VLS_184({
    label: "允许关闭订单",
    dataField: "allowCloseOrder",
}));
const __VLS_186 = __VLS_185({
    label: "允许关闭订单",
    dataField: "allowCloseOrder",
}, ...__VLS_functionalComponentArgsRest(__VLS_185));
__VLS_187.slots.default;
const __VLS_188 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_189 = __VLS_asFunctionalComponent(__VLS_188, new __VLS_188({
    modelValue: (__VLS_ctx.form.allowCloseOrder),
}));
const __VLS_190 = __VLS_189({
    modelValue: (__VLS_ctx.form.allowCloseOrder),
}, ...__VLS_functionalComponentArgsRest(__VLS_189));
var __VLS_187;
const __VLS_192 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_193 = __VLS_asFunctionalComponent(__VLS_192, new __VLS_192({
    label: "供应商对账模式",
    ...{ class: "supplier-form-full" },
    dataField: "reconciliationMode",
}));
const __VLS_194 = __VLS_193({
    label: "供应商对账模式",
    ...{ class: "supplier-form-full" },
    dataField: "reconciliationMode",
}, ...__VLS_functionalComponentArgsRest(__VLS_193));
__VLS_195.slots.default;
const __VLS_196 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_197 = __VLS_asFunctionalComponent(__VLS_196, new __VLS_196({
    modelValue: (__VLS_ctx.form.reconciliationMode),
    ...{ style: {} },
}));
const __VLS_198 = __VLS_197({
    modelValue: (__VLS_ctx.form.reconciliationMode),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_197));
__VLS_199.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.reconciliationModeOptions))) {
    const __VLS_200 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_201 = __VLS_asFunctionalComponent(__VLS_200, new __VLS_200({
        key: (item),
        label: (item),
        value: (item),
    }));
    const __VLS_202 = __VLS_201({
        key: (item),
        label: (item),
        value: (item),
    }, ...__VLS_functionalComponentArgsRest(__VLS_201));
}
var __VLS_199;
var __VLS_195;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('supply')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_204 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_205 = __VLS_asFunctionalComponent(__VLS_204, new __VLS_204({
    label: "范围控制",
    ...{ class: "supplier-form-half" },
    dataField: "scopeControl",
}));
const __VLS_206 = __VLS_205({
    label: "范围控制",
    ...{ class: "supplier-form-half" },
    dataField: "scopeControl",
}, ...__VLS_functionalComponentArgsRest(__VLS_205));
__VLS_207.slots.default;
const __VLS_208 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_209 = __VLS_asFunctionalComponent(__VLS_208, new __VLS_208({
    modelValue: (__VLS_ctx.form.scopeControl),
}));
const __VLS_210 = __VLS_209({
    modelValue: (__VLS_ctx.form.scopeControl),
}, ...__VLS_functionalComponentArgsRest(__VLS_209));
__VLS_211.slots.default;
const __VLS_212 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_213 = __VLS_asFunctionalComponent(__VLS_212, new __VLS_212({
    label: "开启",
}));
const __VLS_214 = __VLS_213({
    label: "开启",
}, ...__VLS_functionalComponentArgsRest(__VLS_213));
__VLS_215.slots.default;
var __VLS_215;
const __VLS_216 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_217 = __VLS_asFunctionalComponent(__VLS_216, new __VLS_216({
    label: "关闭",
}));
const __VLS_218 = __VLS_217({
    label: "关闭",
}, ...__VLS_functionalComponentArgsRest(__VLS_217));
__VLS_219.slots.default;
var __VLS_219;
var __VLS_211;
var __VLS_207;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('qualification')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_220 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_221 = __VLS_asFunctionalComponent(__VLS_220, new __VLS_220({
    data: (__VLS_ctx.qualificationRows),
    border: true,
    ...{ class: "erp-table supplier-dynamic-table" },
    fit: (false),
}));
const __VLS_222 = __VLS_221({
    data: (__VLS_ctx.qualificationRows),
    border: true,
    ...{ class: "erp-table supplier-dynamic-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_221));
__VLS_223.slots.default;
const __VLS_224 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_225 = __VLS_asFunctionalComponent(__VLS_224, new __VLS_224({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_226 = __VLS_225({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_225));
const __VLS_228 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_229 = __VLS_asFunctionalComponent(__VLS_228, new __VLS_228({
    label: "操作",
    width: "78",
}));
const __VLS_230 = __VLS_229({
    label: "操作",
    width: "78",
}, ...__VLS_functionalComponentArgsRest(__VLS_229));
__VLS_231.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_231.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "supplier-cell-center" },
    });
    const __VLS_232 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_233 = __VLS_asFunctionalComponent(__VLS_232, new __VLS_232({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_234 = __VLS_233({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_233));
    let __VLS_236;
    let __VLS_237;
    let __VLS_238;
    const __VLS_239 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addQualificationRow($index);
        }
    };
    __VLS_235.slots.default;
    var __VLS_235;
    const __VLS_240 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_241 = __VLS_asFunctionalComponent(__VLS_240, new __VLS_240({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_242 = __VLS_241({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_241));
    let __VLS_244;
    let __VLS_245;
    let __VLS_246;
    const __VLS_247 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeQualificationRow($index);
        }
    };
    __VLS_243.slots.default;
    var __VLS_243;
}
var __VLS_231;
const __VLS_248 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_249 = __VLS_asFunctionalComponent(__VLS_248, new __VLS_248({
    label: "资质文件",
    minWidth: "140",
    align: "center",
}));
const __VLS_250 = __VLS_249({
    label: "资质文件",
    minWidth: "140",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_249));
__VLS_251.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_251.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "supplier-cell-center" },
        'data-field': (`qualificationList[${$index}].fileUrl`),
    });
    const __VLS_252 = {}.ElUpload;
    /** @type {[typeof __VLS_components.ElUpload, typeof __VLS_components.elUpload, typeof __VLS_components.ElUpload, typeof __VLS_components.elUpload, ]} */ ;
    // @ts-ignore
    const __VLS_253 = __VLS_asFunctionalComponent(__VLS_252, new __VLS_252({
        fileList: (row.files),
        autoUpload: (false),
        limit: (1),
        showFileList: (false),
    }));
    const __VLS_254 = __VLS_253({
        fileList: (row.files),
        autoUpload: (false),
        limit: (1),
        showFileList: (false),
    }, ...__VLS_functionalComponentArgsRest(__VLS_253));
    __VLS_255.slots.default;
    const __VLS_256 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_257 = __VLS_asFunctionalComponent(__VLS_256, new __VLS_256({
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_258 = __VLS_257({
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_257));
    __VLS_259.slots.default;
    var __VLS_259;
    var __VLS_255;
}
var __VLS_251;
const __VLS_260 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_261 = __VLS_asFunctionalComponent(__VLS_260, new __VLS_260({
    label: "资质类型",
    minWidth: "160",
}));
const __VLS_262 = __VLS_261({
    label: "资质类型",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_261));
__VLS_263.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_263.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`qualificationList[${$index}].qualificationType`),
    });
    const __VLS_264 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_265 = __VLS_asFunctionalComponent(__VLS_264, new __VLS_264({
        modelValue: (row.qualificationType),
    }));
    const __VLS_266 = __VLS_265({
        modelValue: (row.qualificationType),
    }, ...__VLS_functionalComponentArgsRest(__VLS_265));
    __VLS_267.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.qualificationTypeOptions))) {
        const __VLS_268 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_269 = __VLS_asFunctionalComponent(__VLS_268, new __VLS_268({
            key: (item),
            label: (item),
            value: (item),
        }));
        const __VLS_270 = __VLS_269({
            key: (item),
            label: (item),
            value: (item),
        }, ...__VLS_functionalComponentArgsRest(__VLS_269));
    }
    var __VLS_267;
}
var __VLS_263;
const __VLS_272 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_273 = __VLS_asFunctionalComponent(__VLS_272, new __VLS_272({
    label: "有效期至",
    width: "130",
}));
const __VLS_274 = __VLS_273({
    label: "有效期至",
    width: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_273));
__VLS_275.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_275.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`qualificationList[${$index}].validTo`),
    });
    const __VLS_276 = {}.ElDatePicker;
    /** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
    // @ts-ignore
    const __VLS_277 = __VLS_asFunctionalComponent(__VLS_276, new __VLS_276({
        modelValue: (row.validTo),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        format: "YYYY-MM-DD",
        placeholder: "请选择",
    }));
    const __VLS_278 = __VLS_277({
        modelValue: (row.validTo),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        format: "YYYY-MM-DD",
        placeholder: "请选择",
    }, ...__VLS_functionalComponentArgsRest(__VLS_277));
}
var __VLS_275;
const __VLS_280 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_281 = __VLS_asFunctionalComponent(__VLS_280, new __VLS_280({
    label: "状态",
    width: "88",
}));
const __VLS_282 = __VLS_281({
    label: "状态",
    width: "88",
}, ...__VLS_functionalComponentArgsRest(__VLS_281));
__VLS_283.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_283.slots;
}
var __VLS_283;
const __VLS_284 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_285 = __VLS_asFunctionalComponent(__VLS_284, new __VLS_284({
    label: "备注",
    minWidth: "180",
}));
const __VLS_286 = __VLS_285({
    label: "备注",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_285));
__VLS_287.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_287.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`qualificationList[${$index}].remark`),
    });
    const __VLS_288 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_289 = __VLS_asFunctionalComponent(__VLS_288, new __VLS_288({
        modelValue: (row.remark),
        placeholder: "请输入备注",
    }));
    const __VLS_290 = __VLS_289({
        modelValue: (row.remark),
        placeholder: "请输入备注",
    }, ...__VLS_functionalComponentArgsRest(__VLS_289));
}
var __VLS_287;
var __VLS_223;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('contract')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_292 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_293 = __VLS_asFunctionalComponent(__VLS_292, new __VLS_292({
    data: (__VLS_ctx.contractRows),
    border: true,
    ...{ class: "erp-table supplier-dynamic-table" },
    fit: (false),
}));
const __VLS_294 = __VLS_293({
    data: (__VLS_ctx.contractRows),
    border: true,
    ...{ class: "erp-table supplier-dynamic-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_293));
__VLS_295.slots.default;
const __VLS_296 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_297 = __VLS_asFunctionalComponent(__VLS_296, new __VLS_296({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_298 = __VLS_297({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_297));
const __VLS_300 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_301 = __VLS_asFunctionalComponent(__VLS_300, new __VLS_300({
    label: "操作",
    width: "78",
}));
const __VLS_302 = __VLS_301({
    label: "操作",
    width: "78",
}, ...__VLS_functionalComponentArgsRest(__VLS_301));
__VLS_303.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_303.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "supplier-cell-center" },
    });
    const __VLS_304 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_305 = __VLS_asFunctionalComponent(__VLS_304, new __VLS_304({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_306 = __VLS_305({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_305));
    let __VLS_308;
    let __VLS_309;
    let __VLS_310;
    const __VLS_311 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addContractRow($index);
        }
    };
    __VLS_307.slots.default;
    var __VLS_307;
    const __VLS_312 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_313 = __VLS_asFunctionalComponent(__VLS_312, new __VLS_312({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_314 = __VLS_313({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_313));
    let __VLS_316;
    let __VLS_317;
    let __VLS_318;
    const __VLS_319 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeContractRow($index);
        }
    };
    __VLS_315.slots.default;
    var __VLS_315;
}
var __VLS_303;
const __VLS_320 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_321 = __VLS_asFunctionalComponent(__VLS_320, new __VLS_320({
    label: "合同附件",
    minWidth: "140",
    align: "center",
}));
const __VLS_322 = __VLS_321({
    label: "合同附件",
    minWidth: "140",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_321));
__VLS_323.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_323.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "supplier-cell-center" },
        'data-field': (`contractList[${$index}].attachmentUrl`),
    });
    const __VLS_324 = {}.ElUpload;
    /** @type {[typeof __VLS_components.ElUpload, typeof __VLS_components.elUpload, typeof __VLS_components.ElUpload, typeof __VLS_components.elUpload, ]} */ ;
    // @ts-ignore
    const __VLS_325 = __VLS_asFunctionalComponent(__VLS_324, new __VLS_324({
        fileList: (row.files),
        autoUpload: (false),
        limit: (1),
        showFileList: (false),
    }));
    const __VLS_326 = __VLS_325({
        fileList: (row.files),
        autoUpload: (false),
        limit: (1),
        showFileList: (false),
    }, ...__VLS_functionalComponentArgsRest(__VLS_325));
    __VLS_327.slots.default;
    const __VLS_328 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_329 = __VLS_asFunctionalComponent(__VLS_328, new __VLS_328({
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_330 = __VLS_329({
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_329));
    __VLS_331.slots.default;
    var __VLS_331;
    var __VLS_327;
}
var __VLS_323;
const __VLS_332 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_333 = __VLS_asFunctionalComponent(__VLS_332, new __VLS_332({
    label: "合同名称",
    minWidth: "160",
}));
const __VLS_334 = __VLS_333({
    label: "合同名称",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_333));
__VLS_335.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_335.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`contractList[${$index}].contractName`),
    });
    const __VLS_336 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_337 = __VLS_asFunctionalComponent(__VLS_336, new __VLS_336({
        modelValue: (row.contractName),
        placeholder: "请输入合同名称",
    }));
    const __VLS_338 = __VLS_337({
        modelValue: (row.contractName),
        placeholder: "请输入合同名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_337));
}
var __VLS_335;
const __VLS_340 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_341 = __VLS_asFunctionalComponent(__VLS_340, new __VLS_340({
    label: "合同编号",
    minWidth: "140",
}));
const __VLS_342 = __VLS_341({
    label: "合同编号",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_341));
__VLS_343.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_343.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`contractList[${$index}].contractCode`),
    });
    const __VLS_344 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_345 = __VLS_asFunctionalComponent(__VLS_344, new __VLS_344({
        modelValue: (row.contractCode),
        placeholder: "请输入合同编号",
    }));
    const __VLS_346 = __VLS_345({
        modelValue: (row.contractCode),
        placeholder: "请输入合同编号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_345));
}
var __VLS_343;
const __VLS_348 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_349 = __VLS_asFunctionalComponent(__VLS_348, new __VLS_348({
    label: "有效期至",
    width: "130",
}));
const __VLS_350 = __VLS_349({
    label: "有效期至",
    width: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_349));
__VLS_351.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_351.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`contractList[${$index}].validTo`),
    });
    const __VLS_352 = {}.ElDatePicker;
    /** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
    // @ts-ignore
    const __VLS_353 = __VLS_asFunctionalComponent(__VLS_352, new __VLS_352({
        modelValue: (row.validTo),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        format: "YYYY-MM-DD",
        placeholder: "请选择",
    }));
    const __VLS_354 = __VLS_353({
        modelValue: (row.validTo),
        type: "date",
        valueFormat: "YYYY-MM-DD",
        format: "YYYY-MM-DD",
        placeholder: "请选择",
    }, ...__VLS_functionalComponentArgsRest(__VLS_353));
}
var __VLS_351;
const __VLS_356 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_357 = __VLS_asFunctionalComponent(__VLS_356, new __VLS_356({
    label: "状态",
    width: "88",
}));
const __VLS_358 = __VLS_357({
    label: "状态",
    width: "88",
}, ...__VLS_functionalComponentArgsRest(__VLS_357));
__VLS_359.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_359.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.resolveContractStatus(row.validTo));
}
var __VLS_359;
var __VLS_295;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('finance')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_360 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_361 = __VLS_asFunctionalComponent(__VLS_360, new __VLS_360({
    data: (__VLS_ctx.financeRows),
    border: true,
    ...{ class: "erp-table supplier-dynamic-table" },
    fit: (false),
}));
const __VLS_362 = __VLS_361({
    data: (__VLS_ctx.financeRows),
    border: true,
    ...{ class: "erp-table supplier-dynamic-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_361));
__VLS_363.slots.default;
const __VLS_364 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_365 = __VLS_asFunctionalComponent(__VLS_364, new __VLS_364({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_366 = __VLS_365({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_365));
const __VLS_368 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_369 = __VLS_asFunctionalComponent(__VLS_368, new __VLS_368({
    label: "操作",
    width: "78",
}));
const __VLS_370 = __VLS_369({
    label: "操作",
    width: "78",
}, ...__VLS_functionalComponentArgsRest(__VLS_369));
__VLS_371.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_371.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "supplier-cell-center" },
    });
    const __VLS_372 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_373 = __VLS_asFunctionalComponent(__VLS_372, new __VLS_372({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_374 = __VLS_373({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_373));
    let __VLS_376;
    let __VLS_377;
    let __VLS_378;
    const __VLS_379 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addFinanceRow($index);
        }
    };
    __VLS_375.slots.default;
    var __VLS_375;
    const __VLS_380 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_381 = __VLS_asFunctionalComponent(__VLS_380, new __VLS_380({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_382 = __VLS_381({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_381));
    let __VLS_384;
    let __VLS_385;
    let __VLS_386;
    const __VLS_387 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeFinanceRow($index);
        }
    };
    __VLS_383.slots.default;
    var __VLS_383;
}
var __VLS_371;
const __VLS_388 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_389 = __VLS_asFunctionalComponent(__VLS_388, new __VLS_388({
    label: "银行账号",
    minWidth: "180",
}));
const __VLS_390 = __VLS_389({
    label: "银行账号",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_389));
__VLS_391.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_391.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`financeList[${$index}].bankAccount`),
    });
    const __VLS_392 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_393 = __VLS_asFunctionalComponent(__VLS_392, new __VLS_392({
        modelValue: (row.bankAccount),
        placeholder: "请输入银行账号",
    }));
    const __VLS_394 = __VLS_393({
        modelValue: (row.bankAccount),
        placeholder: "请输入银行账号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_393));
}
var __VLS_391;
const __VLS_396 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_397 = __VLS_asFunctionalComponent(__VLS_396, new __VLS_396({
    label: "账户名称",
    minWidth: "160",
}));
const __VLS_398 = __VLS_397({
    label: "账户名称",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_397));
__VLS_399.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_399.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`financeList[${$index}].accountName`),
    });
    const __VLS_400 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_401 = __VLS_asFunctionalComponent(__VLS_400, new __VLS_400({
        modelValue: (row.accountName),
        placeholder: "请输入账户名称",
    }));
    const __VLS_402 = __VLS_401({
        modelValue: (row.accountName),
        placeholder: "请输入账户名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_401));
}
var __VLS_399;
const __VLS_404 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_405 = __VLS_asFunctionalComponent(__VLS_404, new __VLS_404({
    label: "银行名称",
    minWidth: "160",
}));
const __VLS_406 = __VLS_405({
    label: "银行名称",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_405));
__VLS_407.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_407.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`financeList[${$index}].bankName`),
    });
    const __VLS_408 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_409 = __VLS_asFunctionalComponent(__VLS_408, new __VLS_408({
        modelValue: (row.bankName),
        placeholder: "请输入银行名称",
    }));
    const __VLS_410 = __VLS_409({
        modelValue: (row.bankName),
        placeholder: "请输入银行名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_409));
}
var __VLS_407;
const __VLS_412 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_413 = __VLS_asFunctionalComponent(__VLS_412, new __VLS_412({
    label: "默认银行账号",
    width: "120",
    align: "center",
}));
const __VLS_414 = __VLS_413({
    label: "默认银行账号",
    width: "120",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_413));
__VLS_415.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_415.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        'data-field': (`financeList[${$index}].defaultAccount`),
    });
    const __VLS_416 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_417 = __VLS_asFunctionalComponent(__VLS_416, new __VLS_416({
        modelValue: (__VLS_ctx.financeDefaultId),
        label: (row.id),
    }));
    const __VLS_418 = __VLS_417({
        modelValue: (__VLS_ctx.financeDefaultId),
        label: (row.id),
    }, ...__VLS_functionalComponentArgsRest(__VLS_417));
    __VLS_419.slots.default;
    var __VLS_419;
}
var __VLS_415;
var __VLS_363;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('invoice')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_420 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_421 = __VLS_asFunctionalComponent(__VLS_420, new __VLS_420({
    label: "单位名称",
    dataField: "invoiceCompanyName",
}));
const __VLS_422 = __VLS_421({
    label: "单位名称",
    dataField: "invoiceCompanyName",
}, ...__VLS_functionalComponentArgsRest(__VLS_421));
__VLS_423.slots.default;
const __VLS_424 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_425 = __VLS_asFunctionalComponent(__VLS_424, new __VLS_424({
    modelValue: (__VLS_ctx.form.invoiceCompanyName),
    placeholder: "请输入单位名称",
}));
const __VLS_426 = __VLS_425({
    modelValue: (__VLS_ctx.form.invoiceCompanyName),
    placeholder: "请输入单位名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_425));
var __VLS_423;
const __VLS_428 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_429 = __VLS_asFunctionalComponent(__VLS_428, new __VLS_428({
    label: "纳税人识别号",
    dataField: "taxpayerId",
}));
const __VLS_430 = __VLS_429({
    label: "纳税人识别号",
    dataField: "taxpayerId",
}, ...__VLS_functionalComponentArgsRest(__VLS_429));
__VLS_431.slots.default;
const __VLS_432 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_433 = __VLS_asFunctionalComponent(__VLS_432, new __VLS_432({
    modelValue: (__VLS_ctx.form.taxpayerId),
    placeholder: "请输入纳税人识别号",
}));
const __VLS_434 = __VLS_433({
    modelValue: (__VLS_ctx.form.taxpayerId),
    placeholder: "请输入纳税人识别号",
}, ...__VLS_functionalComponentArgsRest(__VLS_433));
var __VLS_431;
const __VLS_436 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_437 = __VLS_asFunctionalComponent(__VLS_436, new __VLS_436({
    label: "电话",
    dataField: "invoicePhone",
}));
const __VLS_438 = __VLS_437({
    label: "电话",
    dataField: "invoicePhone",
}, ...__VLS_functionalComponentArgsRest(__VLS_437));
__VLS_439.slots.default;
const __VLS_440 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_441 = __VLS_asFunctionalComponent(__VLS_440, new __VLS_440({
    modelValue: (__VLS_ctx.form.invoicePhone),
    placeholder: "请输入电话",
}));
const __VLS_442 = __VLS_441({
    modelValue: (__VLS_ctx.form.invoicePhone),
    placeholder: "请输入电话",
}, ...__VLS_functionalComponentArgsRest(__VLS_441));
var __VLS_439;
const __VLS_444 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_445 = __VLS_asFunctionalComponent(__VLS_444, new __VLS_444({
    label: "单位地址",
    ...{ class: "supplier-form-half" },
    dataField: "invoiceAddress",
}));
const __VLS_446 = __VLS_445({
    label: "单位地址",
    ...{ class: "supplier-form-half" },
    dataField: "invoiceAddress",
}, ...__VLS_functionalComponentArgsRest(__VLS_445));
__VLS_447.slots.default;
const __VLS_448 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_449 = __VLS_asFunctionalComponent(__VLS_448, new __VLS_448({
    modelValue: (__VLS_ctx.form.invoiceAddress),
    placeholder: "请输入单位地址",
}));
const __VLS_450 = __VLS_449({
    modelValue: (__VLS_ctx.form.invoiceAddress),
    placeholder: "请输入单位地址",
}, ...__VLS_functionalComponentArgsRest(__VLS_449));
var __VLS_447;
var __VLS_13;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-half']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-full']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-half']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-half']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-half']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-full']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-half']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-dynamic-table']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-cell-center']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-cell-center']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-dynamic-table']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-cell-center']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-cell-center']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-dynamic-table']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-cell-center']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-form-half']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            CommonCodeField: CommonCodeField,
            CommonMnemonicField: CommonMnemonicField,
            sectionNavs: sectionNavs,
            activeSectionKey: activeSectionKey,
            supplierCategoryTree: supplierCategoryTree,
            settlementMethods: settlementMethods,
            qualificationTypeOptions: qualificationTypeOptions,
            reconciliationModeOptions: reconciliationModeOptions,
            form: form,
            qualificationRows: qualificationRows,
            contractRows: contractRows,
            financeRows: financeRows,
            financeDefaultId: financeDefaultId,
            addQualificationRow: addQualificationRow,
            removeQualificationRow: removeQualificationRow,
            addContractRow: addContractRow,
            removeContractRow: removeContractRow,
            addFinanceRow: addFinanceRow,
            removeFinanceRow: removeFinanceRow,
            resolveContractStatus: resolveContractStatus,
            registerSectionRef: registerSectionRef,
            scrollToSection: scrollToSection,
            goBack: goBack,
            saveDraft: saveDraft,
            saveSupplier: saveSupplier,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
