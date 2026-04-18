/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonSelectorDialog from '@/components/CommonSelectorDialog.vue';
import { useSessionStore } from '@/stores/session';
import { fetchStoreWarehousesApi } from '@/api/modules/warehouse';
import { createItemRuleApi } from '@/api/modules/warehouse-item-rule';
import { fetchItemCategoryTreeApi, fetchItemCategoriesApi, fetchItemsApi, } from '@/api/modules/item';
const router = useRouter();
const sessionStore = useSessionStore();
const formRef = ref();
const activeSection = ref('basic');
const itemTab = ref('item');
const saving = ref(false);
const navs = [
    { key: 'basic', label: '基础信息' },
    { key: 'items', label: '适用物品' },
    { key: 'warehouse', label: '适用仓库' },
];
const businessScopeOptions = [
    '管控适用机构订货',
    '管控适用机构采购入库',
    '管控适用机构调拨入库',
];
const form = reactive({
    ruleName: '',
    businessControl: false,
    businessScopes: [],
    creator: sessionStore.userName,
    createdAt: '-',
    updatedAt: '-',
});
const formRules = {
    ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
};
const sectionRefs = ref({
    basic: null,
    items: null,
    warehouse: null,
});
const itemRows = ref([]);
const categoryRows = ref([]);
const warehouseRows = ref([]);
const warehouseOptions = ref([]);
const itemSelectorVisible = ref(false);
const itemSelectorKeyword = ref('');
const itemSelectorStatus = ref('');
const activeItemTreeId = ref('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingItemRowIndex = ref(null);
const selectedItemCandidates = ref([]);
const itemTreeData = ref([]);
const itemCandidateSource = ref([]);
const categorySelectorVisible = ref(false);
const categorySelectorKeyword = ref('');
const categorySelectorStatus = ref('');
const activeCategoryTreeId = ref('all');
const categorySelectorCurrentPage = ref(1);
const categorySelectorPageSize = ref(10);
const categorySelectorLoading = ref(false);
const categorySelectorTotal = ref(0);
const selectingCategoryRowIndex = ref(null);
const selectedCategoryCandidates = ref([]);
const categoryTreeData = ref([]);
const categoryCandidateSource = ref([]);
const itemTableColumns = [
    { prop: 'code', label: '物品编码', minWidth: 130 },
    { prop: 'name', label: '物品名称', minWidth: 130 },
    { prop: 'spec', label: '规格型号', minWidth: 130 },
    { prop: 'category', label: '物品类别', minWidth: 130 },
    { prop: 'type', label: '物品类型', minWidth: 120 },
];
const categoryTableColumns = [
    { prop: 'categoryCode', label: '分类编码', minWidth: 130 },
    { prop: 'categoryName', label: '分类名称', minWidth: 130 },
    { prop: 'parentCategory', label: '上级分类', minWidth: 130 },
    { prop: 'status', label: '状态', minWidth: 90 },
];
let seed = 0;
const nextId = () => `row-${++seed}`;
const resolveGroupId = () => {
    const currentOrg = sessionStore.currentOrg;
    if (!currentOrg) {
        return null;
    }
    if (currentOrg.type === 'group') {
        const groupId = Number(String(currentOrg.id).slice('group-'.length));
        return Number.isNaN(groupId) ? null : groupId;
    }
    if (currentOrg.type === 'store') {
        const parentGroup = sessionStore.rootGroups.find((group) => group.children?.some((child) => child.id === currentOrg.id));
        if (!parentGroup) {
            return null;
        }
        const groupId = Number(String(parentGroup.id).slice('group-'.length));
        return Number.isNaN(groupId) ? null : groupId;
    }
    return null;
};
const resolveStoreId = () => {
    const currentOrg = sessionStore.currentOrg;
    if (!currentOrg) {
        return null;
    }
    if (currentOrg.type === 'store') {
        const storeId = Number(String(currentOrg.id).slice('store-'.length));
        return Number.isNaN(storeId) ? null : storeId;
    }
    if (currentOrg.type === 'group') {
        const firstStore = currentOrg.children?.[0];
        if (!firstStore) {
            return null;
        }
        const storeId = Number(String(firstStore.id).slice('store-'.length));
        return Number.isNaN(storeId) ? null : storeId;
    }
    return null;
};
const nowTime = () => {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    const s = String(d.getSeconds()).padStart(2, '0');
    return `${y}-${m}-${day} ${h}:${min}:${s}`;
};
const registerSectionRef = (key) => (el) => {
    if (!el) {
        sectionRefs.value[key] = null;
        return;
    }
    if ('$el' in el) {
        sectionRefs.value[key] = el.$el ?? null;
        return;
    }
    sectionRefs.value[key] = el;
};
const scrollToSection = (key) => {
    const sectionKey = key;
    activeSection.value = sectionKey;
    sectionRefs.value[sectionKey]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
const addItemRow = (index) => {
    const target = typeof index === 'number' ? index + 1 : itemRows.value.length;
    itemRows.value.splice(target, 0, {
        id: nextId(),
        itemCode: '',
        itemName: '',
        specModel: '',
        itemCategory: '',
    });
};
const removeItemRow = (index) => {
    if (itemRows.value.length <= 1) {
        ElMessage.warning('至少保留一条物品数据');
        return;
    }
    itemRows.value.splice(index, 1);
};
const addCategoryRow = (index) => {
    const target = typeof index === 'number' ? index + 1 : categoryRows.value.length;
    categoryRows.value.splice(target, 0, {
        id: nextId(),
        categoryCode: '',
        categoryName: '',
        parentCategory: '',
        childCategory: '',
    });
};
const removeCategoryRow = (index) => {
    if (categoryRows.value.length <= 1) {
        ElMessage.warning('至少保留一条分类数据');
        return;
    }
    categoryRows.value.splice(index, 1);
};
const addWarehouseRow = (index) => {
    const target = typeof index === 'number' ? index + 1 : warehouseRows.value.length;
    warehouseRows.value.splice(target, 0, {
        id: nextId(),
        warehouseId: null,
    });
};
const removeWarehouseRow = (index) => {
    if (warehouseRows.value.length <= 1) {
        ElMessage.warning('至少保留一条仓库数据');
        return;
    }
    warehouseRows.value.splice(index, 1);
};
const handleSaveDraft = () => {
    ElMessage.success('草稿已保存');
};
const handleBack = () => {
    router.back();
};
const loadWarehouses = async () => {
    const storeId = resolveStoreId();
    if (!storeId) {
        warehouseOptions.value = [];
        return;
    }
    warehouseOptions.value = await fetchStoreWarehousesApi(storeId);
};
const normalizeItemTreeNodes = (nodes) => nodes.map((node) => ({
    id: String(node.label ?? ''),
    label: String(node.label ?? ''),
    children: Array.isArray(node.children) ? normalizeItemTreeNodes(node.children) : undefined,
}));
const loadItemTree = async () => {
    const orgId = sessionStore.currentOrgId || undefined;
    const tree = await fetchItemCategoryTreeApi(orgId);
    if (!Array.isArray(tree) || !tree.length) {
        itemTreeData.value = [{ id: 'all', label: '全部' }];
        return;
    }
    itemTreeData.value = [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }];
};
const loadItemCandidates = async () => {
    itemSelectorLoading.value = true;
    try {
        const orgId = sessionStore.currentOrgId || undefined;
        const page = await fetchItemsApi({
            pageNo: itemSelectorCurrentPage.value,
            pageSize: itemSelectorPageSize.value,
            keyword: itemSelectorKeyword.value.trim() || undefined,
            category: activeItemTreeId.value === 'all' ? undefined : activeItemTreeId.value,
            status: itemSelectorStatus.value || undefined,
        }, orgId);
        itemCandidateSource.value = (page.list ?? []).map(mapItemSuggestion);
        itemSelectorTotal.value = Number(page.total ?? 0);
    }
    finally {
        itemSelectorLoading.value = false;
    }
};
const mapItemSuggestion = (item) => ({
    id: item.id,
    code: item.code,
    name: item.name,
    spec: item.spec,
    category: item.category,
    type: item.type,
});
const mapCategoryCandidate = (item) => ({
    id: String(item.id),
    categoryCode: item.categoryCode,
    categoryName: item.categoryName,
    parentCategory: item.parentCategory,
    status: item.status,
});
const applySelectedItem = (row, item) => {
    row.itemCode = item.code || '';
    row.itemName = item.name || '';
    row.specModel = item.spec || '';
    row.itemCategory = item.category || item.type || '';
};
const openItemSelector = async (index) => {
    selectingItemRowIndex.value = index;
    selectedItemCandidates.value = [];
    if (!itemTreeData.value.length) {
        await loadItemTree();
    }
    await loadItemCandidates();
    itemSelectorVisible.value = true;
};
const handleItemSelectorSearch = (payload) => {
    itemSelectorKeyword.value = payload.keyword;
    itemSelectorStatus.value = payload.status;
    itemSelectorCurrentPage.value = 1;
    loadItemCandidates();
};
const handleItemNodeChange = (node) => {
    activeItemTreeId.value = String(node?.id ?? 'all');
    itemSelectorCurrentPage.value = 1;
    loadItemCandidates();
};
const handleItemSelectionChange = (rows) => {
    selectedItemCandidates.value = rows;
};
const handleItemClear = () => {
    selectedItemCandidates.value = [];
};
const handleItemSelectorConfirm = (rows) => {
    const picked = rows;
    if (!picked.length) {
        ElMessage.warning('请至少选择一个物品');
        return;
    }
    const targetIndex = selectingItemRowIndex.value ?? 0;
    const targetRow = itemRows.value[targetIndex];
    if (!targetRow) {
        ElMessage.warning('未找到目标行，请重试');
        return;
    }
    targetRow.itemCode = picked[0].code || '';
    targetRow.itemName = picked[0].name || '';
    targetRow.specModel = picked[0].spec || '';
    targetRow.itemCategory = picked[0].category || picked[0].type || '';
    itemSelectorVisible.value = false;
};
const loadCategoryTree = async () => {
    const orgId = sessionStore.currentOrgId || undefined;
    const tree = await fetchItemCategoryTreeApi(orgId);
    if (!Array.isArray(tree) || !tree.length) {
        categoryTreeData.value = [{ id: 'all', label: '全部' }];
        return;
    }
    categoryTreeData.value = [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }];
};
const loadCategoryCandidates = async () => {
    categorySelectorLoading.value = true;
    try {
        const orgId = sessionStore.currentOrgId || undefined;
        const page = await fetchItemCategoriesApi({
            pageNo: categorySelectorCurrentPage.value,
            pageSize: categorySelectorPageSize.value,
            categoryInfo: categorySelectorKeyword.value.trim() || undefined,
            status: (categorySelectorStatus.value || undefined),
            treeNode: activeCategoryTreeId.value === 'all' ? undefined : activeCategoryTreeId.value,
        }, orgId);
        categoryCandidateSource.value = (page.list ?? []).map(mapCategoryCandidate);
        categorySelectorTotal.value = Number(page.total ?? 0);
    }
    finally {
        categorySelectorLoading.value = false;
    }
};
const openCategorySelector = async (index) => {
    selectingCategoryRowIndex.value = index;
    selectedCategoryCandidates.value = [];
    if (!categoryTreeData.value.length) {
        await loadCategoryTree();
    }
    await loadCategoryCandidates();
    categorySelectorVisible.value = true;
};
const handleCategorySelectorSearch = (payload) => {
    categorySelectorKeyword.value = payload.keyword;
    categorySelectorStatus.value = payload.status;
    categorySelectorCurrentPage.value = 1;
    loadCategoryCandidates();
};
const handleCategoryNodeChange = (node) => {
    activeCategoryTreeId.value = String(node?.id ?? 'all');
    categorySelectorCurrentPage.value = 1;
    loadCategoryCandidates();
};
const handleCategorySelectionChange = (rows) => {
    selectedCategoryCandidates.value = rows;
};
const handleCategoryClear = () => {
    selectedCategoryCandidates.value = [];
};
const handleCategorySelectorConfirm = (rows) => {
    const picked = rows;
    if (!picked.length) {
        ElMessage.warning('请至少选择一个分类');
        return;
    }
    const targetIndex = selectingCategoryRowIndex.value ?? 0;
    const targetRow = categoryRows.value[targetIndex];
    if (!targetRow) {
        ElMessage.warning('未找到目标行，请重试');
        return;
    }
    const selected = picked[0];
    targetRow.categoryCode = selected.categoryCode || '';
    targetRow.categoryName = selected.categoryName || '';
    targetRow.parentCategory = selected.parentCategory || '';
    targetRow.childCategory = selected.categoryName || '';
    categorySelectorVisible.value = false;
};
const buildCreatePayload = () => {
    const controlOrder = form.businessScopes.includes('管控适用机构订货');
    const controlPurchaseInbound = form.businessScopes.includes('管控适用机构采购入库');
    const controlTransferInbound = form.businessScopes.includes('管控适用机构调拨入库');
    return {
        ruleName: form.ruleName.trim(),
        businessControl: form.businessControl,
        controlOrder: form.businessControl ? controlOrder : false,
        controlPurchaseInbound: form.businessControl ? controlPurchaseInbound : false,
        controlTransferInbound: form.businessControl ? controlTransferInbound : false,
        items: itemRows.value
            .map((r) => ({
            itemCode: r.itemCode.trim() || undefined,
            itemName: r.itemName.trim() || undefined,
            specModel: r.specModel.trim() || undefined,
            itemCategory: r.itemCategory.trim() || undefined,
        }))
            .filter((r) => r.itemCode || r.itemName || r.specModel || r.itemCategory),
        categories: categoryRows.value
            .map((r) => ({
            categoryCode: r.categoryCode.trim() || undefined,
            categoryName: r.categoryName.trim() || undefined,
            parentCategory: r.parentCategory.trim() || undefined,
            childCategory: r.childCategory.trim() || undefined,
        }))
            .filter((r) => r.categoryCode || r.categoryName || r.parentCategory || r.childCategory),
        warehouses: warehouseRows.value
            .filter((r) => r.warehouseId)
            .map((r) => ({ warehouseId: r.warehouseId })),
    };
};
const handleSave = async () => {
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) {
        activeSection.value = 'basic';
        return;
    }
    const payload = buildCreatePayload();
    if (!payload.items.length && !payload.categories.length) {
        ElMessage.warning('请至少维护一条适用物品或分类');
        activeSection.value = 'items';
        return;
    }
    if (!payload.warehouses.length) {
        ElMessage.warning('请至少选择一个适用仓库');
        activeSection.value = 'warehouse';
        return;
    }
    const groupId = resolveGroupId();
    if (!groupId) {
        ElMessage.warning('未选择集团，无法保存');
        return;
    }
    saving.value = true;
    try {
        await createItemRuleApi(groupId, payload);
        form.updatedAt = nowTime();
        ElMessage.success('仓库物品规则创建成功');
        router.push({ path: '/archive/7/2', query: { _t: String(Date.now()) } });
    }
    finally {
        saving.value = false;
    }
};
form.createdAt = nowTime();
addItemRow();
addCategoryRow();
addWarehouseRow();
loadWarehouses();
watch(() => sessionStore.currentOrgId, () => {
    loadWarehouses();
    itemTreeData.value = [];
    itemCandidateSource.value = [];
    itemSelectorTotal.value = 0;
    activeItemTreeId.value = 'all';
    categoryTreeData.value = [];
    categoryCandidateSource.value = [];
    categorySelectorTotal.value = 0;
    activeCategoryTreeId.value = 'all';
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['warehouse-item-rule-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['warehouse-item-rule-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['basic-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-create-page warehouse-item-rule-create-page" },
});
/** @type {[typeof FixedActionBreadcrumb, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(FixedActionBreadcrumb, new FixedActionBreadcrumb({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.navs),
    activeKey: (__VLS_ctx.activeSection),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.navs),
    activeKey: (__VLS_ctx.activeSection),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onBack: (__VLS_ctx.handleBack)
};
const __VLS_7 = {
    onSaveDraft: (__VLS_ctx.handleSaveDraft)
};
const __VLS_8 = {
    onSave: (__VLS_ctx.handleSave)
};
const __VLS_9 = {
    onNavigate: (__VLS_ctx.scrollToSection)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel form-panel" },
});
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.saving) }, null, null);
const __VLS_10 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
    ref: "formRef",
    model: (__VLS_ctx.form),
    rules: (__VLS_ctx.formRules),
    labelWidth: "96px",
    ...{ class: "item-create-form" },
}));
const __VLS_12 = __VLS_11({
    ref: "formRef",
    model: (__VLS_ctx.form),
    rules: (__VLS_ctx.formRules),
    labelWidth: "96px",
    ...{ class: "item-create-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_11));
/** @type {typeof __VLS_ctx.formRef} */ ;
var __VLS_14 = {};
__VLS_13.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('basic')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid basic-grid" },
});
const __VLS_16 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    label: "规则名称",
    prop: "ruleName",
}));
const __VLS_18 = __VLS_17({
    label: "规则名称",
    prop: "ruleName",
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
const __VLS_20 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    modelValue: (__VLS_ctx.form.ruleName),
    placeholder: "请输入规则名称",
}));
const __VLS_22 = __VLS_21({
    modelValue: (__VLS_ctx.form.ruleName),
    placeholder: "请输入规则名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
var __VLS_19;
const __VLS_24 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    label: "是否启用业务管控",
}));
const __VLS_26 = __VLS_25({
    label: "是否启用业务管控",
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
__VLS_27.slots.default;
const __VLS_28 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    modelValue: (__VLS_ctx.form.businessControl),
    inlinePrompt: true,
    activeText: "开",
    inactiveText: "关",
}));
const __VLS_30 = __VLS_29({
    modelValue: (__VLS_ctx.form.businessControl),
    inlinePrompt: true,
    activeText: "开",
    inactiveText: "关",
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
var __VLS_27;
if (__VLS_ctx.form.businessControl) {
    const __VLS_32 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
        label: "业务管控范围",
        ...{ class: "full-span" },
    }));
    const __VLS_34 = __VLS_33({
        label: "业务管控范围",
        ...{ class: "full-span" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_33));
    __VLS_35.slots.default;
    const __VLS_36 = {}.ElCheckboxGroup;
    /** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
    // @ts-ignore
    const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
        modelValue: (__VLS_ctx.form.businessScopes),
        ...{ class: "scope-group" },
    }));
    const __VLS_38 = __VLS_37({
        modelValue: (__VLS_ctx.form.businessScopes),
        ...{ class: "scope-group" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_37));
    __VLS_39.slots.default;
    for (const [scope] of __VLS_getVForSourceType((__VLS_ctx.businessScopeOptions))) {
        const __VLS_40 = {}.ElCheckbox;
        /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
        // @ts-ignore
        const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
            key: (scope),
            value: (scope),
        }));
        const __VLS_42 = __VLS_41({
            key: (scope),
            value: (scope),
        }, ...__VLS_functionalComponentArgsRest(__VLS_41));
        __VLS_43.slots.default;
        (scope);
        var __VLS_43;
    }
    var __VLS_39;
    var __VLS_35;
}
const __VLS_44 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
    label: "创建人",
}));
const __VLS_46 = __VLS_45({
    label: "创建人",
}, ...__VLS_functionalComponentArgsRest(__VLS_45));
__VLS_47.slots.default;
const __VLS_48 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
    modelValue: (__VLS_ctx.form.creator),
    disabled: true,
}));
const __VLS_50 = __VLS_49({
    modelValue: (__VLS_ctx.form.creator),
    disabled: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_49));
var __VLS_47;
const __VLS_52 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
    label: "创建时间",
}));
const __VLS_54 = __VLS_53({
    label: "创建时间",
}, ...__VLS_functionalComponentArgsRest(__VLS_53));
__VLS_55.slots.default;
const __VLS_56 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    modelValue: (__VLS_ctx.form.createdAt),
    disabled: true,
}));
const __VLS_58 = __VLS_57({
    modelValue: (__VLS_ctx.form.createdAt),
    disabled: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
var __VLS_55;
const __VLS_60 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
    label: "最后修改时间",
}));
const __VLS_62 = __VLS_61({
    label: "最后修改时间",
}, ...__VLS_functionalComponentArgsRest(__VLS_61));
__VLS_63.slots.default;
const __VLS_64 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
    modelValue: (__VLS_ctx.form.updatedAt),
    disabled: true,
}));
const __VLS_66 = __VLS_65({
    modelValue: (__VLS_ctx.form.updatedAt),
    disabled: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_65));
var __VLS_63;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('items')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_68 = {}.ElTabs;
/** @type {[typeof __VLS_components.ElTabs, typeof __VLS_components.elTabs, typeof __VLS_components.ElTabs, typeof __VLS_components.elTabs, ]} */ ;
// @ts-ignore
const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
    modelValue: (__VLS_ctx.itemTab),
    ...{ class: "rule-item-tabs" },
}));
const __VLS_70 = __VLS_69({
    modelValue: (__VLS_ctx.itemTab),
    ...{ class: "rule-item-tabs" },
}, ...__VLS_functionalComponentArgsRest(__VLS_69));
__VLS_71.slots.default;
const __VLS_72 = {}.ElTabPane;
/** @type {[typeof __VLS_components.ElTabPane, typeof __VLS_components.elTabPane, typeof __VLS_components.ElTabPane, typeof __VLS_components.elTabPane, ]} */ ;
// @ts-ignore
const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
    label: "物品",
    name: "item",
}));
const __VLS_74 = __VLS_73({
    label: "物品",
    name: "item",
}, ...__VLS_functionalComponentArgsRest(__VLS_73));
__VLS_75.slots.default;
const __VLS_76 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
    data: (__VLS_ctx.itemRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}));
const __VLS_78 = __VLS_77({
    data: (__VLS_ctx.itemRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_77));
__VLS_79.slots.default;
const __VLS_80 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_82 = __VLS_81({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_81));
const __VLS_84 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
    label: "操作",
    width: "80",
    fixed: "left",
}));
const __VLS_86 = __VLS_85({
    label: "操作",
    width: "80",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_85));
__VLS_87.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_87.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_88 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_90 = __VLS_89({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_89));
    let __VLS_92;
    let __VLS_93;
    let __VLS_94;
    const __VLS_95 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addItemRow($index);
        }
    };
    __VLS_91.slots.default;
    var __VLS_91;
    const __VLS_96 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_98 = __VLS_97({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_97));
    let __VLS_100;
    let __VLS_101;
    let __VLS_102;
    const __VLS_103 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeItemRow($index);
        }
    };
    __VLS_99.slots.default;
    var __VLS_99;
}
var __VLS_87;
const __VLS_104 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
    label: "物品编码",
    minWidth: "130",
}));
const __VLS_106 = __VLS_105({
    label: "物品编码",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_105));
__VLS_107.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_107.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openItemSelector($index);
            } },
    });
    (row.itemCode || '点击选择物品');
}
var __VLS_107;
const __VLS_108 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
    label: "物品名称",
    minWidth: "130",
}));
const __VLS_110 = __VLS_109({
    label: "物品名称",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_109));
__VLS_111.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_111.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.itemName || '-');
}
var __VLS_111;
const __VLS_112 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
    label: "规格型号",
    minWidth: "130",
}));
const __VLS_114 = __VLS_113({
    label: "规格型号",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_113));
__VLS_115.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_115.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.specModel || '-');
}
var __VLS_115;
const __VLS_116 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
    label: "物品类别",
    minWidth: "130",
}));
const __VLS_118 = __VLS_117({
    label: "物品类别",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_117));
__VLS_119.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_119.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.itemCategory || '-');
}
var __VLS_119;
var __VLS_79;
var __VLS_75;
const __VLS_120 = {}.ElTabPane;
/** @type {[typeof __VLS_components.ElTabPane, typeof __VLS_components.elTabPane, typeof __VLS_components.ElTabPane, typeof __VLS_components.elTabPane, ]} */ ;
// @ts-ignore
const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({
    label: "分类",
    name: "category",
}));
const __VLS_122 = __VLS_121({
    label: "分类",
    name: "category",
}, ...__VLS_functionalComponentArgsRest(__VLS_121));
__VLS_123.slots.default;
const __VLS_124 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_125 = __VLS_asFunctionalComponent(__VLS_124, new __VLS_124({
    data: (__VLS_ctx.categoryRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}));
const __VLS_126 = __VLS_125({
    data: (__VLS_ctx.categoryRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_125));
__VLS_127.slots.default;
const __VLS_128 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_129 = __VLS_asFunctionalComponent(__VLS_128, new __VLS_128({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_130 = __VLS_129({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_129));
const __VLS_132 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_133 = __VLS_asFunctionalComponent(__VLS_132, new __VLS_132({
    label: "操作",
    width: "80",
    fixed: "left",
}));
const __VLS_134 = __VLS_133({
    label: "操作",
    width: "80",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_133));
__VLS_135.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_135.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_136 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_137 = __VLS_asFunctionalComponent(__VLS_136, new __VLS_136({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_138 = __VLS_137({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_137));
    let __VLS_140;
    let __VLS_141;
    let __VLS_142;
    const __VLS_143 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addCategoryRow($index);
        }
    };
    __VLS_139.slots.default;
    var __VLS_139;
    const __VLS_144 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_145 = __VLS_asFunctionalComponent(__VLS_144, new __VLS_144({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_146 = __VLS_145({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_145));
    let __VLS_148;
    let __VLS_149;
    let __VLS_150;
    const __VLS_151 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeCategoryRow($index);
        }
    };
    __VLS_147.slots.default;
    var __VLS_147;
}
var __VLS_135;
const __VLS_152 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_153 = __VLS_asFunctionalComponent(__VLS_152, new __VLS_152({
    label: "分类编码",
    minWidth: "130",
}));
const __VLS_154 = __VLS_153({
    label: "分类编码",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_153));
__VLS_155.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_155.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openCategorySelector($index);
            } },
    });
    (row.categoryCode || '点击选择分类');
}
var __VLS_155;
const __VLS_156 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_157 = __VLS_asFunctionalComponent(__VLS_156, new __VLS_156({
    label: "分类名称",
    minWidth: "130",
}));
const __VLS_158 = __VLS_157({
    label: "分类名称",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_157));
__VLS_159.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_159.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_160 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_161 = __VLS_asFunctionalComponent(__VLS_160, new __VLS_160({
        modelValue: (row.categoryName),
    }));
    const __VLS_162 = __VLS_161({
        modelValue: (row.categoryName),
    }, ...__VLS_functionalComponentArgsRest(__VLS_161));
}
var __VLS_159;
const __VLS_164 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_165 = __VLS_asFunctionalComponent(__VLS_164, new __VLS_164({
    label: "上级分类",
    minWidth: "130",
}));
const __VLS_166 = __VLS_165({
    label: "上级分类",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_165));
__VLS_167.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_167.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_168 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_169 = __VLS_asFunctionalComponent(__VLS_168, new __VLS_168({
        modelValue: (row.parentCategory),
    }));
    const __VLS_170 = __VLS_169({
        modelValue: (row.parentCategory),
    }, ...__VLS_functionalComponentArgsRest(__VLS_169));
}
var __VLS_167;
const __VLS_172 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_173 = __VLS_asFunctionalComponent(__VLS_172, new __VLS_172({
    label: "下级分类",
    minWidth: "130",
}));
const __VLS_174 = __VLS_173({
    label: "下级分类",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_173));
__VLS_175.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_175.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_176 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_177 = __VLS_asFunctionalComponent(__VLS_176, new __VLS_176({
        modelValue: (row.childCategory),
    }));
    const __VLS_178 = __VLS_177({
        modelValue: (row.childCategory),
    }, ...__VLS_functionalComponentArgsRest(__VLS_177));
}
var __VLS_175;
var __VLS_127;
var __VLS_123;
var __VLS_71;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('warehouse')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_180 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_181 = __VLS_asFunctionalComponent(__VLS_180, new __VLS_180({
    data: (__VLS_ctx.warehouseRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}));
const __VLS_182 = __VLS_181({
    data: (__VLS_ctx.warehouseRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_181));
__VLS_183.slots.default;
const __VLS_184 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_185 = __VLS_asFunctionalComponent(__VLS_184, new __VLS_184({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_186 = __VLS_185({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_185));
const __VLS_188 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_189 = __VLS_asFunctionalComponent(__VLS_188, new __VLS_188({
    label: "操作",
    width: "80",
    fixed: "left",
}));
const __VLS_190 = __VLS_189({
    label: "操作",
    width: "80",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_189));
__VLS_191.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_191.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_192 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_193 = __VLS_asFunctionalComponent(__VLS_192, new __VLS_192({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_194 = __VLS_193({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_193));
    let __VLS_196;
    let __VLS_197;
    let __VLS_198;
    const __VLS_199 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addWarehouseRow($index);
        }
    };
    __VLS_195.slots.default;
    var __VLS_195;
    const __VLS_200 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_201 = __VLS_asFunctionalComponent(__VLS_200, new __VLS_200({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_202 = __VLS_201({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_201));
    let __VLS_204;
    let __VLS_205;
    let __VLS_206;
    const __VLS_207 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeWarehouseRow($index);
        }
    };
    __VLS_203.slots.default;
    var __VLS_203;
}
var __VLS_191;
const __VLS_208 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_209 = __VLS_asFunctionalComponent(__VLS_208, new __VLS_208({
    label: "仓库/档口",
    minWidth: "220",
}));
const __VLS_210 = __VLS_209({
    label: "仓库/档口",
    minWidth: "220",
}, ...__VLS_functionalComponentArgsRest(__VLS_209));
__VLS_211.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_211.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_212 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_213 = __VLS_asFunctionalComponent(__VLS_212, new __VLS_212({
        modelValue: (row.warehouseId),
        clearable: true,
        filterable: true,
        placeholder: "请选择仓库/档口",
        ...{ style: {} },
    }));
    const __VLS_214 = __VLS_213({
        modelValue: (row.warehouseId),
        clearable: true,
        filterable: true,
        placeholder: "请选择仓库/档口",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_213));
    __VLS_215.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.warehouseOptions))) {
        const __VLS_216 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_217 = __VLS_asFunctionalComponent(__VLS_216, new __VLS_216({
            key: (option.id),
            label: (`${option.warehouseName} (${option.warehouseCode})`),
            value: (option.id),
        }));
        const __VLS_218 = __VLS_217({
            key: (option.id),
            label: (`${option.warehouseName} (${option.warehouseCode})`),
            value: (option.id),
        }, ...__VLS_functionalComponentArgsRest(__VLS_217));
    }
    var __VLS_215;
}
var __VLS_211;
var __VLS_183;
var __VLS_13;
/** @type {[typeof CommonSelectorDialog, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(CommonSelectorDialog, new CommonSelectorDialog({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.itemSelectorVisible),
    title: "选择物品",
    treeData: (__VLS_ctx.itemTreeData),
    tableData: (__VLS_ctx.itemCandidateSource),
    loading: (__VLS_ctx.itemSelectorLoading),
    columns: (__VLS_ctx.itemTableColumns),
    rowKey: "id",
    selectedLabelKey: "name",
    selectedRows: (__VLS_ctx.selectedItemCandidates),
    keywordValue: (__VLS_ctx.itemSelectorKeyword),
    statusValue: (__VLS_ctx.itemSelectorStatus),
    keywordLabel: "物品",
    keywordPlaceholder: "支持按物品编码和名称查询...",
    statusLabel: "启用状态",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
    ]),
    total: (__VLS_ctx.itemSelectorTotal),
    currentPage: (__VLS_ctx.itemSelectorCurrentPage),
    pageSize: (__VLS_ctx.itemSelectorPageSize),
}));
const __VLS_221 = __VLS_220({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.itemSelectorVisible),
    title: "选择物品",
    treeData: (__VLS_ctx.itemTreeData),
    tableData: (__VLS_ctx.itemCandidateSource),
    loading: (__VLS_ctx.itemSelectorLoading),
    columns: (__VLS_ctx.itemTableColumns),
    rowKey: "id",
    selectedLabelKey: "name",
    selectedRows: (__VLS_ctx.selectedItemCandidates),
    keywordValue: (__VLS_ctx.itemSelectorKeyword),
    statusValue: (__VLS_ctx.itemSelectorStatus),
    keywordLabel: "物品",
    keywordPlaceholder: "支持按物品编码和名称查询...",
    statusLabel: "启用状态",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
    ]),
    total: (__VLS_ctx.itemSelectorTotal),
    currentPage: (__VLS_ctx.itemSelectorCurrentPage),
    pageSize: (__VLS_ctx.itemSelectorPageSize),
}, ...__VLS_functionalComponentArgsRest(__VLS_220));
let __VLS_223;
let __VLS_224;
let __VLS_225;
const __VLS_226 = {
    onSearch: (__VLS_ctx.handleItemSelectorSearch)
};
const __VLS_227 = {
    onNodeChange: (__VLS_ctx.handleItemNodeChange)
};
const __VLS_228 = {
    onSelectionChange: (__VLS_ctx.handleItemSelectionChange)
};
const __VLS_229 = {
    onClearSelection: (__VLS_ctx.handleItemClear)
};
const __VLS_230 = {
    onPageChange: ((p) => { __VLS_ctx.itemSelectorCurrentPage = p; __VLS_ctx.loadItemCandidates(); })
};
const __VLS_231 = {
    onPageSizeChange: ((s) => { __VLS_ctx.itemSelectorPageSize = s; __VLS_ctx.itemSelectorCurrentPage = 1; __VLS_ctx.loadItemCandidates(); })
};
const __VLS_232 = {
    onConfirm: (__VLS_ctx.handleItemSelectorConfirm)
};
var __VLS_222;
/** @type {[typeof CommonSelectorDialog, ]} */ ;
// @ts-ignore
const __VLS_233 = __VLS_asFunctionalComponent(CommonSelectorDialog, new CommonSelectorDialog({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.categorySelectorVisible),
    title: "选择分类",
    treeData: (__VLS_ctx.categoryTreeData),
    tableData: (__VLS_ctx.categoryCandidateSource),
    loading: (__VLS_ctx.categorySelectorLoading),
    columns: (__VLS_ctx.categoryTableColumns),
    rowKey: "id",
    selectedLabelKey: "categoryName",
    selectedRows: (__VLS_ctx.selectedCategoryCandidates),
    keywordValue: (__VLS_ctx.categorySelectorKeyword),
    statusValue: (__VLS_ctx.categorySelectorStatus),
    keywordLabel: "分类",
    keywordPlaceholder: "支持按分类编码和名称查询...",
    statusLabel: "启用状态",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
    ]),
    total: (__VLS_ctx.categorySelectorTotal),
    currentPage: (__VLS_ctx.categorySelectorCurrentPage),
    pageSize: (__VLS_ctx.categorySelectorPageSize),
}));
const __VLS_234 = __VLS_233({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.categorySelectorVisible),
    title: "选择分类",
    treeData: (__VLS_ctx.categoryTreeData),
    tableData: (__VLS_ctx.categoryCandidateSource),
    loading: (__VLS_ctx.categorySelectorLoading),
    columns: (__VLS_ctx.categoryTableColumns),
    rowKey: "id",
    selectedLabelKey: "categoryName",
    selectedRows: (__VLS_ctx.selectedCategoryCandidates),
    keywordValue: (__VLS_ctx.categorySelectorKeyword),
    statusValue: (__VLS_ctx.categorySelectorStatus),
    keywordLabel: "分类",
    keywordPlaceholder: "支持按分类编码和名称查询...",
    statusLabel: "启用状态",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
    ]),
    total: (__VLS_ctx.categorySelectorTotal),
    currentPage: (__VLS_ctx.categorySelectorCurrentPage),
    pageSize: (__VLS_ctx.categorySelectorPageSize),
}, ...__VLS_functionalComponentArgsRest(__VLS_233));
let __VLS_236;
let __VLS_237;
let __VLS_238;
const __VLS_239 = {
    onSearch: (__VLS_ctx.handleCategorySelectorSearch)
};
const __VLS_240 = {
    onNodeChange: (__VLS_ctx.handleCategoryNodeChange)
};
const __VLS_241 = {
    onSelectionChange: (__VLS_ctx.handleCategorySelectionChange)
};
const __VLS_242 = {
    onClearSelection: (__VLS_ctx.handleCategoryClear)
};
const __VLS_243 = {
    onPageChange: ((p) => { __VLS_ctx.categorySelectorCurrentPage = p; __VLS_ctx.loadCategoryCandidates(); })
};
const __VLS_244 = {
    onPageSizeChange: ((s) => { __VLS_ctx.categorySelectorPageSize = s; __VLS_ctx.categorySelectorCurrentPage = 1; __VLS_ctx.loadCategoryCandidates(); })
};
const __VLS_245 = {
    onConfirm: (__VLS_ctx.handleCategorySelectorConfirm)
};
var __VLS_235;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['warehouse-item-rule-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['full-span']} */ ;
/** @type {__VLS_StyleScopedClasses['scope-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['rule-item-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
// @ts-ignore
var __VLS_15 = __VLS_14;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            CommonSelectorDialog: CommonSelectorDialog,
            formRef: formRef,
            activeSection: activeSection,
            itemTab: itemTab,
            saving: saving,
            navs: navs,
            businessScopeOptions: businessScopeOptions,
            form: form,
            formRules: formRules,
            itemRows: itemRows,
            categoryRows: categoryRows,
            warehouseRows: warehouseRows,
            warehouseOptions: warehouseOptions,
            itemSelectorVisible: itemSelectorVisible,
            itemSelectorKeyword: itemSelectorKeyword,
            itemSelectorStatus: itemSelectorStatus,
            itemSelectorCurrentPage: itemSelectorCurrentPage,
            itemSelectorPageSize: itemSelectorPageSize,
            itemSelectorLoading: itemSelectorLoading,
            itemSelectorTotal: itemSelectorTotal,
            selectedItemCandidates: selectedItemCandidates,
            itemTreeData: itemTreeData,
            itemCandidateSource: itemCandidateSource,
            categorySelectorVisible: categorySelectorVisible,
            categorySelectorKeyword: categorySelectorKeyword,
            categorySelectorStatus: categorySelectorStatus,
            categorySelectorCurrentPage: categorySelectorCurrentPage,
            categorySelectorPageSize: categorySelectorPageSize,
            categorySelectorLoading: categorySelectorLoading,
            categorySelectorTotal: categorySelectorTotal,
            selectedCategoryCandidates: selectedCategoryCandidates,
            categoryTreeData: categoryTreeData,
            categoryCandidateSource: categoryCandidateSource,
            itemTableColumns: itemTableColumns,
            categoryTableColumns: categoryTableColumns,
            registerSectionRef: registerSectionRef,
            scrollToSection: scrollToSection,
            addItemRow: addItemRow,
            removeItemRow: removeItemRow,
            addCategoryRow: addCategoryRow,
            removeCategoryRow: removeCategoryRow,
            addWarehouseRow: addWarehouseRow,
            removeWarehouseRow: removeWarehouseRow,
            handleSaveDraft: handleSaveDraft,
            handleBack: handleBack,
            loadItemCandidates: loadItemCandidates,
            openItemSelector: openItemSelector,
            handleItemSelectorSearch: handleItemSelectorSearch,
            handleItemNodeChange: handleItemNodeChange,
            handleItemSelectionChange: handleItemSelectionChange,
            handleItemClear: handleItemClear,
            handleItemSelectorConfirm: handleItemSelectorConfirm,
            loadCategoryCandidates: loadCategoryCandidates,
            openCategorySelector: openCategorySelector,
            handleCategorySelectorSearch: handleCategorySelectorSearch,
            handleCategoryNodeChange: handleCategoryNodeChange,
            handleCategorySelectionChange: handleCategorySelectionChange,
            handleCategoryClear: handleCategoryClear,
            handleCategorySelectorConfirm: handleCategorySelectorConfirm,
            handleSave: handleSave,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
