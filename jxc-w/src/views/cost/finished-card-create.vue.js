/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonSelectorDialog from '@/components/CommonSelectorDialog.vue';
import { fetchDishCategoryTreeApi, fetchDishesApi, } from '@/api/modules/dish';
import { fetchItemCategoryTreeApi, fetchItemsApi, } from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
const router = useRouter();
const sessionStore = useSessionStore();
const baseForm = reactive({
    name: '',
    processPortions: 1,
    usageDishType: '全部',
    usageDishSpec: '',
    itemCostTax: 0,
    otherCost: 0,
    remark: '',
});
const dishRows = ref([
    {
        spuCode: '',
        dishName: '',
        spec: '',
        category: '',
        dishType: '',
    },
]);
const materialRows = ref([
    {
        itemCode: '',
        itemName: '',
        specModel: '',
        costUnit: '',
        netAmount: 0,
        netRate: 100,
        grossAmount: 0,
        taxPrice: 0,
        taxAmount: 0,
        isMainMaterial: true,
        assistDeductMode: '否',
        baseUnit: '',
        baseGrossAmount: 0,
        baseTaxPrice: 0,
        substituteMaterial: '',
        remark: '',
    },
]);
const totalCostTax = computed(() => Number(baseForm.itemCostTax || 0) + Number(baseForm.otherCost || 0));
const sectionNavs = [
    { key: 'basic', label: '基础信息' },
    { key: 'dish', label: '适用菜品' },
    { key: 'material', label: '物料明细' },
];
const activeSectionKey = ref('basic');
const sectionRefs = ref({});
const dishSelectorVisible = ref(false);
const dishSelectorKeyword = ref('');
const dishSelectorDeleted = ref('');
const activeDishTreeId = ref('all');
const dishSelectorCurrentPage = ref(1);
const dishSelectorPageSize = ref(10);
const dishSelectorLoading = ref(false);
const dishSelectorTotal = ref(0);
const selectingDishRowIndex = ref(null);
const selectedDishCandidates = ref([]);
const dishTreeData = ref([]);
const dishTableColumns = [
    { prop: 'spuCode', label: '菜品SPU编码', minWidth: 130 },
    { prop: 'dishName', label: '菜品名称', minWidth: 130 },
    { prop: 'spec', label: '规格', minWidth: 100 },
    { prop: 'category', label: '菜品分类', minWidth: 120 },
    { prop: 'dishType', label: '菜品类型', minWidth: 120 },
    { prop: 'linkedCostCard', label: '是否关联成本卡', minWidth: 130 },
];
const itemSelectorVisible = ref(false);
const itemSelectorKeyword = ref('');
const itemSelectorStatus = ref('');
const activeItemTreeId = ref('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingMaterialRowIndex = ref(null);
const selectedItemCandidates = ref([]);
const itemTreeData = ref([]);
const itemTableColumns = [
    { prop: 'code', label: '物品编码', minWidth: 130 },
    { prop: 'name', label: '物品名称', minWidth: 130 },
    { prop: 'spec', label: '规格型号', minWidth: 120 },
    { prop: 'category', label: '物品类别', minWidth: 120 },
    { prop: 'type', label: '物品类型', minWidth: 100 },
    { prop: 'costUnit', label: '成本单位', minWidth: 100 },
];
const dishCandidateSource = ref([]);
const itemCandidateSource = ref([]);
const resolveOrgId = () => {
    const orgId = (sessionStore.currentOrgId ?? '').trim();
    return orgId || undefined;
};
const normalizeTreeNodes = (nodes) => nodes.map((node) => ({
    id: node.id,
    label: String(node.label ?? ''),
    children: Array.isArray(node.children) ? normalizeTreeNodes(node.children) : undefined,
}));
const normalizeItemTreeNodes = (nodes) => nodes.map((node) => ({
    id: String(node.label ?? 'all'),
    label: String(node.label ?? ''),
    children: Array.isArray(node.children) ? normalizeItemTreeNodes(node.children) : undefined,
}));
const loadDishTree = async () => {
    const tree = await fetchDishCategoryTreeApi(resolveOrgId());
    if (!Array.isArray(tree) || !tree.length) {
        dishTreeData.value = [{ id: 'all', label: '全部' }];
        return;
    }
    dishTreeData.value = normalizeTreeNodes(tree);
};
const mapDishCandidate = (row) => ({
    id: row.dishId || String(row.id),
    spuCode: row.spuCode,
    dishName: row.dishName,
    spec: row.spec,
    category: row.category,
    dishType: row.dishType,
    deleted: row.deleted,
    linkedCostCard: row.linkedCostCard,
});
const loadDishCandidates = async () => {
    dishSelectorLoading.value = true;
    try {
        const page = await fetchDishesApi({
            pageNo: dishSelectorCurrentPage.value,
            pageSize: dishSelectorPageSize.value,
            keyword: dishSelectorKeyword.value.trim() || undefined,
            deleted: (dishSelectorDeleted.value || undefined),
            categoryId: activeDishTreeId.value === 'all' ? undefined : activeDishTreeId.value,
        }, resolveOrgId());
        dishCandidateSource.value = page.list.map(mapDishCandidate);
        dishSelectorTotal.value = Number(page.total ?? 0);
    }
    finally {
        dishSelectorLoading.value = false;
    }
};
const loadItemTree = async () => {
    const tree = await fetchItemCategoryTreeApi(resolveOrgId());
    if (!Array.isArray(tree) || !tree.length) {
        itemTreeData.value = [{ id: 'all', label: '全部' }];
        return;
    }
    itemTreeData.value = [{ id: 'all', label: '全部', children: normalizeItemTreeNodes(tree) }];
};
const mapItemCandidate = (row) => ({
    id: row.id || row.code,
    code: row.code,
    name: row.name,
    spec: row.spec,
    category: row.category,
    type: row.type,
    costUnit: row.costUnit,
    baseUnit: row.baseUnit,
    status: row.status,
});
const loadItemCandidates = async () => {
    itemSelectorLoading.value = true;
    try {
        const page = await fetchItemsApi({
            pageNo: itemSelectorCurrentPage.value,
            pageSize: itemSelectorPageSize.value,
            keyword: itemSelectorKeyword.value.trim() || undefined,
            category: activeItemTreeId.value === 'all' ? undefined : activeItemTreeId.value,
            status: itemSelectorStatus.value || undefined,
        }, resolveOrgId());
        itemCandidateSource.value = page.list.map(mapItemCandidate);
        itemSelectorTotal.value = Number(page.total ?? 0);
    }
    finally {
        itemSelectorLoading.value = false;
    }
};
const addDishRow = () => {
    dishRows.value.push({
        spuCode: '',
        dishName: '',
        spec: '',
        category: '',
        dishType: '',
    });
};
const removeDishRow = (index) => {
    if (dishRows.value.length === 1) {
        return;
    }
    dishRows.value.splice(index, 1);
};
const openDishSelector = async (index) => {
    selectingDishRowIndex.value = index;
    selectedDishCandidates.value = [];
    if (!dishTreeData.value.length) {
        await loadDishTree();
    }
    await loadDishCandidates();
    dishSelectorVisible.value = true;
};
const handleDishSelectorSearch = (payload) => {
    dishSelectorKeyword.value = payload.keyword;
    dishSelectorDeleted.value = payload.status;
    dishSelectorCurrentPage.value = 1;
    loadDishCandidates();
};
const handleDishNodeChange = (node) => {
    activeDishTreeId.value = String(node?.id ?? 'all');
    dishSelectorCurrentPage.value = 1;
    loadDishCandidates();
};
const handleDishSelectionChange = (rows) => {
    selectedDishCandidates.value = rows;
};
const handleDishClear = () => {
    selectedDishCandidates.value = [];
};
const applyDishToRow = (row, dish) => {
    row.spuCode = dish.spuCode;
    row.dishName = dish.dishName;
    row.spec = dish.spec;
    row.category = dish.category;
    row.dishType = dish.dishType;
};
const handleDishSelectorConfirm = (rows) => {
    const picked = rows;
    if (!picked.length) {
        ElMessage.warning('请至少选择一个菜品');
        return;
    }
    const targetIndex = selectingDishRowIndex.value ?? 0;
    const targetRow = dishRows.value[targetIndex];
    if (!targetRow) {
        ElMessage.warning('未找到目标行，请重试');
        return;
    }
    applyDishToRow(targetRow, picked[0]);
    if (picked.length > 1) {
        const extras = picked.slice(1);
        extras.forEach((dish) => {
            dishRows.value.push({
                spuCode: dish.spuCode,
                dishName: dish.dishName,
                spec: dish.spec,
                category: dish.category,
                dishType: dish.dishType,
            });
        });
        ElMessage.success(`已选择${picked.length}个菜品，额外新增${extras.length}行`);
    }
    dishSelectorVisible.value = false;
};
const openItemSelector = async (index) => {
    selectingMaterialRowIndex.value = index;
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
const applyItemToRow = (row, item) => {
    row.itemCode = item.code;
    row.itemName = item.name;
    row.specModel = item.spec;
    row.costUnit = item.costUnit;
    row.baseUnit = item.baseUnit;
};
const handleItemSelectorConfirm = (rows) => {
    const picked = rows;
    if (!picked.length) {
        ElMessage.warning('请至少选择一个物品');
        return;
    }
    const targetIndex = selectingMaterialRowIndex.value ?? 0;
    const targetRow = materialRows.value[targetIndex];
    if (!targetRow) {
        ElMessage.warning('未找到目标行，请重试');
        return;
    }
    applyItemToRow(targetRow, picked[0]);
    if (picked.length > 1) {
        const extras = picked.slice(1);
        extras.forEach((item) => {
            materialRows.value.push({
                itemCode: item.code,
                itemName: item.name,
                specModel: item.spec,
                costUnit: item.costUnit,
                netAmount: 0,
                netRate: 100,
                grossAmount: 0,
                taxPrice: 0,
                taxAmount: 0,
                isMainMaterial: false,
                assistDeductMode: '否',
                baseUnit: item.baseUnit,
                baseGrossAmount: 0,
                baseTaxPrice: 0,
                substituteMaterial: '',
                remark: '',
            });
        });
        ElMessage.success(`已选择${picked.length}个物品，额外新增${extras.length}行`);
    }
    itemSelectorVisible.value = false;
};
const addMaterialRow = () => {
    materialRows.value.push({
        itemCode: '',
        itemName: '',
        specModel: '',
        costUnit: '',
        netAmount: 0,
        netRate: 100,
        grossAmount: 0,
        taxPrice: 0,
        taxAmount: 0,
        isMainMaterial: false,
        assistDeductMode: '否',
        baseUnit: '',
        baseGrossAmount: 0,
        baseTaxPrice: 0,
        substituteMaterial: '',
        remark: '',
    });
};
const removeMaterialRow = (index) => {
    if (materialRows.value.length === 1) {
        return;
    }
    materialRows.value.splice(index, 1);
};
const backToArchive = () => {
    router.push('/archive/2/1');
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
const scrollToSection = (key) => {
    const target = sectionRefs.value[key];
    if (!target) {
        return;
    }
    activeSectionKey.value = key;
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
const saveDraft = () => {
    ElMessage.success('草稿已保存（示例）');
};
const saveCard = () => {
    ElMessage.success('成品卡已保存（示例）');
};
watch(() => sessionStore.currentOrgId, () => {
    dishTreeData.value = [];
    dishCandidateSource.value = [];
    dishSelectorTotal.value = 0;
    activeDishTreeId.value = 'all';
    itemTreeData.value = [];
    itemCandidateSource.value = [];
    itemSelectorTotal.value = 0;
    activeItemTreeId.value = 'all';
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
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
    onBack: (__VLS_ctx.backToArchive)
};
const __VLS_7 = {
    onSaveDraft: (__VLS_ctx.saveDraft)
};
const __VLS_8 = {
    onSave: (__VLS_ctx.saveCard)
};
const __VLS_9 = {
    onNavigate: (__VLS_ctx.scrollToSection)
};
var __VLS_2;
const __VLS_10 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
    labelWidth: "120px",
    ...{ class: "item-create-form" },
}));
const __VLS_12 = __VLS_11({
    labelWidth: "120px",
    ...{ class: "item-create-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_11));
__VLS_13.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('basic')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_14 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
    label: "成本卡名称",
}));
const __VLS_16 = __VLS_15({
    label: "成本卡名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_15));
__VLS_17.slots.default;
const __VLS_18 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
    modelValue: (__VLS_ctx.baseForm.name),
    placeholder: "请输入成本卡名称",
}));
const __VLS_20 = __VLS_19({
    modelValue: (__VLS_ctx.baseForm.name),
    placeholder: "请输入成本卡名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
var __VLS_17;
const __VLS_22 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
    label: "加工份数",
}));
const __VLS_24 = __VLS_23({
    label: "加工份数",
}, ...__VLS_functionalComponentArgsRest(__VLS_23));
__VLS_25.slots.default;
const __VLS_26 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
    modelValue: (__VLS_ctx.baseForm.processPortions),
    min: (1),
    step: (1),
    controlsPosition: "right",
}));
const __VLS_28 = __VLS_27({
    modelValue: (__VLS_ctx.baseForm.processPortions),
    min: (1),
    step: (1),
    controlsPosition: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_27));
var __VLS_25;
const __VLS_30 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
    label: "使用菜品类型",
}));
const __VLS_32 = __VLS_31({
    label: "使用菜品类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_31));
__VLS_33.slots.default;
const __VLS_34 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
    modelValue: (__VLS_ctx.baseForm.usageDishType),
}));
const __VLS_36 = __VLS_35({
    modelValue: (__VLS_ctx.baseForm.usageDishType),
}, ...__VLS_functionalComponentArgsRest(__VLS_35));
__VLS_37.slots.default;
const __VLS_38 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
    value: "全部",
}));
const __VLS_40 = __VLS_39({
    value: "全部",
}, ...__VLS_functionalComponentArgsRest(__VLS_39));
__VLS_41.slots.default;
var __VLS_41;
const __VLS_42 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
    value: "堂食",
}));
const __VLS_44 = __VLS_43({
    value: "堂食",
}, ...__VLS_functionalComponentArgsRest(__VLS_43));
__VLS_45.slots.default;
var __VLS_45;
const __VLS_46 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    value: "外卖",
}));
const __VLS_48 = __VLS_47({
    value: "外卖",
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
__VLS_49.slots.default;
var __VLS_49;
var __VLS_37;
var __VLS_33;
const __VLS_50 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
    label: "使用菜品规格",
}));
const __VLS_52 = __VLS_51({
    label: "使用菜品规格",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
__VLS_53.slots.default;
const __VLS_54 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    modelValue: (__VLS_ctx.baseForm.usageDishSpec),
    placeholder: "请输入使用菜品规格",
}));
const __VLS_56 = __VLS_55({
    modelValue: (__VLS_ctx.baseForm.usageDishSpec),
    placeholder: "请输入使用菜品规格",
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
var __VLS_53;
const __VLS_58 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    label: "物品成本（含税）",
}));
const __VLS_60 = __VLS_59({
    label: "物品成本（含税）",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
__VLS_61.slots.default;
const __VLS_62 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    modelValue: (__VLS_ctx.baseForm.itemCostTax),
    min: (0),
    precision: (2),
    controlsPosition: "right",
}));
const __VLS_64 = __VLS_63({
    modelValue: (__VLS_ctx.baseForm.itemCostTax),
    min: (0),
    precision: (2),
    controlsPosition: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
var __VLS_61;
const __VLS_66 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "其他成本",
}));
const __VLS_68 = __VLS_67({
    label: "其他成本",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
const __VLS_70 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    modelValue: (__VLS_ctx.baseForm.otherCost),
    min: (0),
    precision: (2),
    controlsPosition: "right",
}));
const __VLS_72 = __VLS_71({
    modelValue: (__VLS_ctx.baseForm.otherCost),
    min: (0),
    precision: (2),
    controlsPosition: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
var __VLS_69;
const __VLS_74 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    label: "合计成本（含税）",
}));
const __VLS_76 = __VLS_75({
    label: "合计成本（含税）",
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
__VLS_77.slots.default;
const __VLS_78 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    modelValue: (__VLS_ctx.totalCostTax.toFixed(2)),
    readonly: true,
}));
const __VLS_80 = __VLS_79({
    modelValue: (__VLS_ctx.totalCostTax.toFixed(2)),
    readonly: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
var __VLS_77;
const __VLS_82 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
    label: "备注",
    ...{ class: "item-intro-wide-form-item" },
}));
const __VLS_84 = __VLS_83({
    label: "备注",
    ...{ class: "item-intro-wide-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_83));
__VLS_85.slots.default;
const __VLS_86 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
    modelValue: (__VLS_ctx.baseForm.remark),
    type: "textarea",
    rows: (2),
    placeholder: "请输入备注",
}));
const __VLS_88 = __VLS_87({
    modelValue: (__VLS_ctx.baseForm.remark),
    type: "textarea",
    rows: (2),
    placeholder: "请输入备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_87));
var __VLS_85;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('dish')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_90 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
    data: (__VLS_ctx.dishRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}));
const __VLS_92 = __VLS_91({
    data: (__VLS_ctx.dishRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_91));
__VLS_93.slots.default;
const __VLS_94 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_96 = __VLS_95({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_95));
const __VLS_98 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    label: "操作",
    width: "72",
}));
const __VLS_100 = __VLS_99({
    label: "操作",
    width: "72",
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
__VLS_101.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_101.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_102 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_104 = __VLS_103({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_103));
    let __VLS_106;
    let __VLS_107;
    let __VLS_108;
    const __VLS_109 = {
        onClick: (__VLS_ctx.addDishRow)
    };
    __VLS_105.slots.default;
    var __VLS_105;
    const __VLS_110 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.dishRows.length === 1),
    }));
    const __VLS_112 = __VLS_111({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.dishRows.length === 1),
    }, ...__VLS_functionalComponentArgsRest(__VLS_111));
    let __VLS_114;
    let __VLS_115;
    let __VLS_116;
    const __VLS_117 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeDishRow($index);
        }
    };
    __VLS_113.slots.default;
    var __VLS_113;
}
var __VLS_101;
const __VLS_118 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    label: "菜品SPU编码",
    minWidth: "130",
}));
const __VLS_120 = __VLS_119({
    label: "菜品SPU编码",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
__VLS_121.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_121.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openDishSelector($index);
            } },
    });
    (row.spuCode || '点击选择菜品');
}
var __VLS_121;
const __VLS_122 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
    label: "菜品名称",
    minWidth: "120",
}));
const __VLS_124 = __VLS_123({
    label: "菜品名称",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_123));
__VLS_125.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_125.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.dishName || '-');
}
var __VLS_125;
const __VLS_126 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    label: "规格",
    minWidth: "100",
}));
const __VLS_128 = __VLS_127({
    label: "规格",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
__VLS_129.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_129.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.spec || '-');
}
var __VLS_129;
const __VLS_130 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
    label: "菜品分类",
    minWidth: "110",
}));
const __VLS_132 = __VLS_131({
    label: "菜品分类",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_131));
__VLS_133.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_133.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.category || '-');
}
var __VLS_133;
const __VLS_134 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
    label: "菜品类型",
    minWidth: "100",
}));
const __VLS_136 = __VLS_135({
    label: "菜品类型",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_135));
__VLS_137.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_137.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.dishType || '-');
}
var __VLS_137;
var __VLS_93;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('material')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
const __VLS_138 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    data: (__VLS_ctx.materialRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}));
const __VLS_140 = __VLS_139({
    data: (__VLS_ctx.materialRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
__VLS_141.slots.default;
const __VLS_142 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_144 = __VLS_143({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
const __VLS_146 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    label: "操作",
    width: "72",
    fixed: "left",
}));
const __VLS_148 = __VLS_147({
    label: "操作",
    width: "72",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
__VLS_149.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_149.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_150 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_152 = __VLS_151({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_151));
    let __VLS_154;
    let __VLS_155;
    let __VLS_156;
    const __VLS_157 = {
        onClick: (__VLS_ctx.addMaterialRow)
    };
    __VLS_153.slots.default;
    var __VLS_153;
    const __VLS_158 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.materialRows.length === 1),
    }));
    const __VLS_160 = __VLS_159({
        ...{ 'onClick': {} },
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.materialRows.length === 1),
    }, ...__VLS_functionalComponentArgsRest(__VLS_159));
    let __VLS_162;
    let __VLS_163;
    let __VLS_164;
    const __VLS_165 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeMaterialRow($index);
        }
    };
    __VLS_161.slots.default;
    var __VLS_161;
}
var __VLS_149;
const __VLS_166 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
    label: "物品编码",
    width: "120",
}));
const __VLS_168 = __VLS_167({
    label: "物品编码",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_167));
__VLS_169.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_169.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openItemSelector($index);
            } },
    });
    (row.itemCode || '点击选择物品');
}
var __VLS_169;
const __VLS_170 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
    label: "物品名称",
    width: "120",
}));
const __VLS_172 = __VLS_171({
    label: "物品名称",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_171));
__VLS_173.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_173.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.itemName || '-');
}
var __VLS_173;
const __VLS_174 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
    label: "规格型号",
    width: "120",
}));
const __VLS_176 = __VLS_175({
    label: "规格型号",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_175));
__VLS_177.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_177.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.specModel || '-');
}
var __VLS_177;
const __VLS_178 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({
    label: "成本单位",
    width: "96",
}));
const __VLS_180 = __VLS_179({
    label: "成本单位",
    width: "96",
}, ...__VLS_functionalComponentArgsRest(__VLS_179));
__VLS_181.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_181.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.costUnit || '-');
}
var __VLS_181;
const __VLS_182 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
    label: "净料量",
    width: "90",
}));
const __VLS_184 = __VLS_183({
    label: "净料量",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_183));
__VLS_185.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_185.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_186 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
        modelValue: (row.netAmount),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }));
    const __VLS_188 = __VLS_187({
        modelValue: (row.netAmount),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_187));
}
var __VLS_185;
const __VLS_190 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
    label: "净料率",
    width: "90",
}));
const __VLS_192 = __VLS_191({
    label: "净料率",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_191));
__VLS_193.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_193.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_194 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_195 = __VLS_asFunctionalComponent(__VLS_194, new __VLS_194({
        modelValue: (row.netRate),
        min: (0),
        max: (100),
        precision: (2),
        controlsPosition: "right",
    }));
    const __VLS_196 = __VLS_195({
        modelValue: (row.netRate),
        min: (0),
        max: (100),
        precision: (2),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_195));
}
var __VLS_193;
const __VLS_198 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
    label: "毛料量",
    width: "90",
}));
const __VLS_200 = __VLS_199({
    label: "毛料量",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_199));
__VLS_201.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_201.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_202 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
        modelValue: (row.grossAmount),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }));
    const __VLS_204 = __VLS_203({
        modelValue: (row.grossAmount),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_203));
}
var __VLS_201;
const __VLS_206 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_207 = __VLS_asFunctionalComponent(__VLS_206, new __VLS_206({
    label: "消耗单价（含税）",
    width: "120",
}));
const __VLS_208 = __VLS_207({
    label: "消耗单价（含税）",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_207));
__VLS_209.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_209.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_210 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
        modelValue: (row.taxPrice),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }));
    const __VLS_212 = __VLS_211({
        modelValue: (row.taxPrice),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_211));
}
var __VLS_209;
const __VLS_214 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({
    label: "消耗金额(含税)",
    width: "110",
}));
const __VLS_216 = __VLS_215({
    label: "消耗金额(含税)",
    width: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_215));
__VLS_217.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_217.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_218 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
        modelValue: (row.taxAmount),
        min: (0),
        precision: (2),
        controlsPosition: "right",
    }));
    const __VLS_220 = __VLS_219({
        modelValue: (row.taxAmount),
        min: (0),
        precision: (2),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_219));
}
var __VLS_217;
const __VLS_222 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_223 = __VLS_asFunctionalComponent(__VLS_222, new __VLS_222({
    label: "是否主料",
    width: "90",
}));
const __VLS_224 = __VLS_223({
    label: "是否主料",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_223));
__VLS_225.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_225.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_226 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({
        modelValue: (row.isMainMaterial),
        inlinePrompt: true,
        activeText: "是",
        inactiveText: "否",
    }));
    const __VLS_228 = __VLS_227({
        modelValue: (row.isMainMaterial),
        inlinePrompt: true,
        activeText: "是",
        inactiveText: "否",
    }, ...__VLS_functionalComponentArgsRest(__VLS_227));
}
var __VLS_225;
const __VLS_230 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_231 = __VLS_asFunctionalComponent(__VLS_230, new __VLS_230({
    label: "是否辅助单位扣减料",
    width: "136",
}));
const __VLS_232 = __VLS_231({
    label: "是否辅助单位扣减料",
    width: "136",
}, ...__VLS_functionalComponentArgsRest(__VLS_231));
__VLS_233.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_233.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_234 = {}.ElRadioGroup;
    /** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
    // @ts-ignore
    const __VLS_235 = __VLS_asFunctionalComponent(__VLS_234, new __VLS_234({
        modelValue: (row.assistDeductMode),
    }));
    const __VLS_236 = __VLS_235({
        modelValue: (row.assistDeductMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_235));
    __VLS_237.slots.default;
    const __VLS_238 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_239 = __VLS_asFunctionalComponent(__VLS_238, new __VLS_238({
        value: "是",
    }));
    const __VLS_240 = __VLS_239({
        value: "是",
    }, ...__VLS_functionalComponentArgsRest(__VLS_239));
    __VLS_241.slots.default;
    var __VLS_241;
    const __VLS_242 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_243 = __VLS_asFunctionalComponent(__VLS_242, new __VLS_242({
        value: "否",
    }));
    const __VLS_244 = __VLS_243({
        value: "否",
    }, ...__VLS_functionalComponentArgsRest(__VLS_243));
    __VLS_245.slots.default;
    var __VLS_245;
    var __VLS_237;
}
var __VLS_233;
const __VLS_246 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_247 = __VLS_asFunctionalComponent(__VLS_246, new __VLS_246({
    label: "基准单位",
    width: "96",
}));
const __VLS_248 = __VLS_247({
    label: "基准单位",
    width: "96",
}, ...__VLS_functionalComponentArgsRest(__VLS_247));
__VLS_249.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_249.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.baseUnit || '-');
}
var __VLS_249;
const __VLS_250 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_251 = __VLS_asFunctionalComponent(__VLS_250, new __VLS_250({
    label: "基准单位毛料量",
    width: "126",
}));
const __VLS_252 = __VLS_251({
    label: "基准单位毛料量",
    width: "126",
}, ...__VLS_functionalComponentArgsRest(__VLS_251));
__VLS_253.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_253.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_254 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_255 = __VLS_asFunctionalComponent(__VLS_254, new __VLS_254({
        modelValue: (row.baseGrossAmount),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }));
    const __VLS_256 = __VLS_255({
        modelValue: (row.baseGrossAmount),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_255));
}
var __VLS_253;
const __VLS_258 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_259 = __VLS_asFunctionalComponent(__VLS_258, new __VLS_258({
    label: "基准单位消耗单价（含税）",
    width: "160",
}));
const __VLS_260 = __VLS_259({
    label: "基准单位消耗单价（含税）",
    width: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_259));
__VLS_261.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_261.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_262 = {}.ElInputNumber;
    /** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
    // @ts-ignore
    const __VLS_263 = __VLS_asFunctionalComponent(__VLS_262, new __VLS_262({
        modelValue: (row.baseTaxPrice),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }));
    const __VLS_264 = __VLS_263({
        modelValue: (row.baseTaxPrice),
        min: (0),
        precision: (4),
        controlsPosition: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_263));
}
var __VLS_261;
const __VLS_266 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_267 = __VLS_asFunctionalComponent(__VLS_266, new __VLS_266({
    label: "替代料",
    width: "100",
}));
const __VLS_268 = __VLS_267({
    label: "替代料",
    width: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_267));
__VLS_269.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_269.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_270 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_271 = __VLS_asFunctionalComponent(__VLS_270, new __VLS_270({
        modelValue: (row.substituteMaterial),
    }));
    const __VLS_272 = __VLS_271({
        modelValue: (row.substituteMaterial),
    }, ...__VLS_functionalComponentArgsRest(__VLS_271));
}
var __VLS_269;
const __VLS_274 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_275 = __VLS_asFunctionalComponent(__VLS_274, new __VLS_274({
    label: "备注",
    width: "140",
}));
const __VLS_276 = __VLS_275({
    label: "备注",
    width: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_275));
__VLS_277.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_277.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_278 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_279 = __VLS_asFunctionalComponent(__VLS_278, new __VLS_278({
        modelValue: (row.remark),
    }));
    const __VLS_280 = __VLS_279({
        modelValue: (row.remark),
    }, ...__VLS_functionalComponentArgsRest(__VLS_279));
}
var __VLS_277;
var __VLS_141;
var __VLS_13;
/** @type {[typeof CommonSelectorDialog, ]} */ ;
// @ts-ignore
const __VLS_282 = __VLS_asFunctionalComponent(CommonSelectorDialog, new CommonSelectorDialog({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.dishSelectorVisible),
    title: "选择菜品",
    treeData: (__VLS_ctx.dishTreeData),
    tableData: (__VLS_ctx.dishCandidateSource),
    loading: (__VLS_ctx.dishSelectorLoading),
    columns: (__VLS_ctx.dishTableColumns),
    rowKey: "id",
    selectedLabelKey: "dishName",
    selectedRows: (__VLS_ctx.selectedDishCandidates),
    keywordValue: (__VLS_ctx.dishSelectorKeyword),
    statusValue: (__VLS_ctx.dishSelectorDeleted),
    keywordLabel: "菜品",
    keywordPlaceholder: "支持按名称和拼音助记码...",
    statusLabel: "菜品是否删除",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '否', value: 'N' },
        { label: '是', value: 'Y' },
    ]),
    total: (__VLS_ctx.dishSelectorTotal),
    currentPage: (__VLS_ctx.dishSelectorCurrentPage),
    pageSize: (__VLS_ctx.dishSelectorPageSize),
}));
const __VLS_283 = __VLS_282({
    ...{ 'onSearch': {} },
    ...{ 'onNodeChange': {} },
    ...{ 'onSelectionChange': {} },
    ...{ 'onClearSelection': {} },
    ...{ 'onPageChange': {} },
    ...{ 'onPageSizeChange': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.dishSelectorVisible),
    title: "选择菜品",
    treeData: (__VLS_ctx.dishTreeData),
    tableData: (__VLS_ctx.dishCandidateSource),
    loading: (__VLS_ctx.dishSelectorLoading),
    columns: (__VLS_ctx.dishTableColumns),
    rowKey: "id",
    selectedLabelKey: "dishName",
    selectedRows: (__VLS_ctx.selectedDishCandidates),
    keywordValue: (__VLS_ctx.dishSelectorKeyword),
    statusValue: (__VLS_ctx.dishSelectorDeleted),
    keywordLabel: "菜品",
    keywordPlaceholder: "支持按名称和拼音助记码...",
    statusLabel: "菜品是否删除",
    statusOptions: ([
        { label: '全部', value: '' },
        { label: '否', value: 'N' },
        { label: '是', value: 'Y' },
    ]),
    total: (__VLS_ctx.dishSelectorTotal),
    currentPage: (__VLS_ctx.dishSelectorCurrentPage),
    pageSize: (__VLS_ctx.dishSelectorPageSize),
}, ...__VLS_functionalComponentArgsRest(__VLS_282));
let __VLS_285;
let __VLS_286;
let __VLS_287;
const __VLS_288 = {
    onSearch: (__VLS_ctx.handleDishSelectorSearch)
};
const __VLS_289 = {
    onNodeChange: (__VLS_ctx.handleDishNodeChange)
};
const __VLS_290 = {
    onSelectionChange: (__VLS_ctx.handleDishSelectionChange)
};
const __VLS_291 = {
    onClearSelection: (__VLS_ctx.handleDishClear)
};
const __VLS_292 = {
    onPageChange: ((p) => { __VLS_ctx.dishSelectorCurrentPage = p; __VLS_ctx.loadDishCandidates(); })
};
const __VLS_293 = {
    onPageSizeChange: ((s) => { __VLS_ctx.dishSelectorPageSize = s; __VLS_ctx.dishSelectorCurrentPage = 1; __VLS_ctx.loadDishCandidates(); })
};
const __VLS_294 = {
    onConfirm: (__VLS_ctx.handleDishSelectorConfirm)
};
var __VLS_284;
/** @type {[typeof CommonSelectorDialog, ]} */ ;
// @ts-ignore
const __VLS_295 = __VLS_asFunctionalComponent(CommonSelectorDialog, new CommonSelectorDialog({
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
const __VLS_296 = __VLS_295({
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
}, ...__VLS_functionalComponentArgsRest(__VLS_295));
let __VLS_298;
let __VLS_299;
let __VLS_300;
const __VLS_301 = {
    onSearch: (__VLS_ctx.handleItemSelectorSearch)
};
const __VLS_302 = {
    onNodeChange: (__VLS_ctx.handleItemNodeChange)
};
const __VLS_303 = {
    onSelectionChange: (__VLS_ctx.handleItemSelectionChange)
};
const __VLS_304 = {
    onClearSelection: (__VLS_ctx.handleItemClear)
};
const __VLS_305 = {
    onPageChange: ((p) => { __VLS_ctx.itemSelectorCurrentPage = p; __VLS_ctx.loadItemCandidates(); })
};
const __VLS_306 = {
    onPageSizeChange: ((s) => { __VLS_ctx.itemSelectorPageSize = s; __VLS_ctx.itemSelectorCurrentPage = 1; __VLS_ctx.loadItemCandidates(); })
};
const __VLS_307 = {
    onConfirm: (__VLS_ctx.handleItemSelectorConfirm)
};
var __VLS_297;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-wide-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            CommonSelectorDialog: CommonSelectorDialog,
            baseForm: baseForm,
            dishRows: dishRows,
            materialRows: materialRows,
            totalCostTax: totalCostTax,
            sectionNavs: sectionNavs,
            activeSectionKey: activeSectionKey,
            dishSelectorVisible: dishSelectorVisible,
            dishSelectorKeyword: dishSelectorKeyword,
            dishSelectorDeleted: dishSelectorDeleted,
            dishSelectorCurrentPage: dishSelectorCurrentPage,
            dishSelectorPageSize: dishSelectorPageSize,
            dishSelectorLoading: dishSelectorLoading,
            dishSelectorTotal: dishSelectorTotal,
            selectedDishCandidates: selectedDishCandidates,
            dishTreeData: dishTreeData,
            dishTableColumns: dishTableColumns,
            itemSelectorVisible: itemSelectorVisible,
            itemSelectorKeyword: itemSelectorKeyword,
            itemSelectorStatus: itemSelectorStatus,
            itemSelectorCurrentPage: itemSelectorCurrentPage,
            itemSelectorPageSize: itemSelectorPageSize,
            itemSelectorLoading: itemSelectorLoading,
            itemSelectorTotal: itemSelectorTotal,
            selectedItemCandidates: selectedItemCandidates,
            itemTreeData: itemTreeData,
            itemTableColumns: itemTableColumns,
            dishCandidateSource: dishCandidateSource,
            itemCandidateSource: itemCandidateSource,
            loadDishCandidates: loadDishCandidates,
            loadItemCandidates: loadItemCandidates,
            addDishRow: addDishRow,
            removeDishRow: removeDishRow,
            openDishSelector: openDishSelector,
            handleDishSelectorSearch: handleDishSelectorSearch,
            handleDishNodeChange: handleDishNodeChange,
            handleDishSelectionChange: handleDishSelectionChange,
            handleDishClear: handleDishClear,
            handleDishSelectorConfirm: handleDishSelectorConfirm,
            openItemSelector: openItemSelector,
            handleItemSelectorSearch: handleItemSelectorSearch,
            handleItemNodeChange: handleItemNodeChange,
            handleItemSelectionChange: handleItemSelectionChange,
            handleItemClear: handleItemClear,
            handleItemSelectorConfirm: handleItemSelectorConfirm,
            addMaterialRow: addMaterialRow,
            removeMaterialRow: removeMaterialRow,
            backToArchive: backToArchive,
            registerSectionRef: registerSectionRef,
            scrollToSection: scrollToSection,
            saveDraft: saveDraft,
            saveCard: saveCard,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
