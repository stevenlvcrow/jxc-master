/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSessionStore } from '@/stores/session';
import { createItemApi, fetchItemDetailApi, saveItemDraftApi, updateItemApi, } from '@/api/modules/item';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonMnemonicField from '@/components/CommonMnemonicField.vue';
const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const form = reactive({
    name: '',
    code: '',
    category: '',
    spec: '',
    status: '启用',
    brand: '',
    mnemonicCode: '',
    barcode: '',
    thirdPartyCode: '',
    defaultPurchaseUnit: '斤',
    defaultOrderUnit: '斤',
    defaultStockUnit: '斤',
    defaultCostUnit: '斤',
    stocktakeUnits: ['个', '袋'],
    assistUnitEnabled: false,
    productionRefCost: '',
    monthEndUpdate: true,
    suggestPurchasePrice: '',
    storageMode: '常温',
    statType: '',
    stockMin: '',
    stockMax: '',
    safeStock: '',
    taxCode: '',
    taxName: '',
    taxRate: '',
    taxBenefit: '',
    consumeOnInbound: '否',
    disableStocktake: '否',
    defaultNoStocktake: '否',
    stocktakeTypes: [],
    purchaseReceiptRule: '不限制',
    purchaseRuleMaxRatio: '',
    purchaseRuleMinRatio: '',
    purchaseRuleBetweenStart: '',
    purchaseRuleBetweenEnd: '',
    requireAssemblyProcess: false,
    requireSplitProcess: false,
    requireBatchReport: false,
    allowLossReport: true,
    allowTransfer: true,
    enablePrepare: false,
    productCategory: '',
    netContent: '',
    ingredients: '',
    itemDescription: '',
    remark: '',
    alias: '',
    abcClass: '',
    batchManagement: false,
    shelfLifeEnabled: false,
    shelfLifeDays: undefined,
    warningDays: undefined,
    stagnantDays: undefined,
    tag: '',
});
const categoryOptions = ['蔬菜', '奶茶', '肉类', '调料', '面点', '熟食', '日用百货', '水产', '河粉', '面食', '预制菜', '酒水', '一次性用品', '前厅类'];
const abcOptions = ['A', 'B', 'C'];
const storageModeOptions = ['冷藏', '冰冻', '常温'];
const statTypeOptions = [
    '原料类 (成本类)',
    '酒水类 (成本类)',
    '调料料类 (成本类)',
    '半成品类 (成本类)',
    '低值易耗品类 (费用类)',
    '固定资产类 (费用类)',
];
const taxBenefitOptions = ['无', '免税'];
const stocktakeTypeOptions = ['日盘点', '周盘点'];
const purchaseReceiptRuleOptions = [
    '不限制',
    '等于采购数量',
    '不大于采购数量的一定比例',
    '不小于采购数量的一定比例',
    '介于采购数量的一定比例',
];
const unitOptions = [
    '捆', '桶', '坛', '组', '瓶', '袋', '件', '包', '条', '只', '根', '个',
    '厘米', '对', '米', '两', 'L', '升', '次', '提', '板', '杯', '扎', '片', '听', 'mL',
    '双', '块', '本', '卷', '串', '毫升', '公斤', '张', '罐', '把', '支', '套', 'kg', '台',
    '千克', '克', '盒', '箱', '辆',
];
const volumeUnitOptions = ['cm³', 'dm³', 'm³'];
const weightUnitOptions = ['克', '斤', '公斤', '吨'];
const extensionInfoFieldNames = ['致癌物质提示', '使用方法', '生产许可证号', '产品标准号'];
const unitSettingRows = ref([
    { unit: '斤', convertFrom: '1', convertTo: '', volume: '0.002', volumeUnit: 'dm³', weight: '1.000', weightUnit: '斤', barcode: 'U-0001' },
    { unit: '个', convertFrom: '1', convertTo: '', volume: '0.001', volumeUnit: 'dm³', weight: '0.500', weightUnit: '斤', barcode: 'U-0002' },
    { unit: '袋', convertFrom: '1', convertTo: '', volume: '0.004', volumeUnit: 'dm³', weight: '2.000', weightUnit: '斤', barcode: 'U-0003' },
]);
const unitSettingTip = '1.基准单位一般是最小的【包装】单位；成本单位是最细粒度单位，可能比基准单位小；若生产单位很小，建议基准单位也设小，以防单位换算后生产数据失真。2.单位设置举例：速冻青芒汁（1kg*12瓶/箱），基准单位设置瓶，库存单位设置箱，成本单位设置克，采购单位和订货单位建议与库存单位一致。';
const baseUnitLabel = computed(() => unitSettingRows.value[0]?.unit || '斤');
const defaultStockUnitLabel = computed(() => form.defaultStockUnit || '未设置');
const stocktakeUnitOptions = computed(() => (Array.from(new Set(unitSettingRows.value
    .slice(1)
    .map((row) => row.unit.trim())
    .filter(Boolean)))));
const defaultUnitOptions = computed(() => stocktakeUnitOptions.value);
const supplierRowSeed = ref(2);
const defaultSupplierRowKey = ref(1);
const supplierRelationRows = ref([
    { key: 1, supplier: '', contact: '', phone: '' },
]);
const introImages = ref([]);
const introImageSeed = ref(1);
const nutritionSeed = ref(3);
const nutritionRows = ref([
    { id: 1, item: '能量', per100g: '', nrv: '' },
    { id: 2, item: '蛋白质', per100g: '', nrv: '' },
]);
const nutritionHeaders = reactive({
    item: '项目',
    per100g: '每100g',
    nrv: 'NRV%',
});
const buildDefaultExtensionInfoRows = () => (extensionInfoFieldNames.map((name, index) => ({ id: index + 1, name, value: '' })));
const extensionInfoRows = ref(buildDefaultExtensionInfoRows());
const sectionNavs = [
    { key: 'basic', label: '基础信息' },
    { key: 'unit', label: '单位价格' },
    { key: 'storage', label: '存储信息' },
    { key: 'supplier', label: '供货关系' },
    { key: 'business', label: '业务信息' },
    { key: 'intro', label: '物品介绍' },
    { key: 'extension', label: '扩展信息' },
];
const defaultUnitFields = [
    { key: 'defaultPurchaseUnit', label: '默认采购单位', optionKey: 'purchase' },
    { key: 'defaultOrderUnit', label: '默认订货单位', optionKey: 'order' },
    { key: 'defaultStockUnit', label: '默认库存单位', optionKey: 'stock' },
    { key: 'defaultCostUnit', label: '默认成本单位', optionKey: 'cost' },
];
const yesNoRadioFields = [
    { key: 'consumeOnInbound', label: '入库即耗用' },
    { key: 'disableStocktake', label: '是否禁盘' },
    { key: 'defaultNoStocktake', label: '默认不盘点' },
];
const businessSwitchFields = [
    { key: 'requireAssemblyProcess', label: '是否需组合加工' },
    { key: 'requireSplitProcess', label: '是否需拆分加工' },
    { key: 'requireBatchReport', label: '是否需上传批检报告' },
    { key: 'allowLossReport', label: '是否可报损' },
    { key: 'allowTransfer', label: '是否可调拨' },
    { key: 'enablePrepare', label: '是否制备' },
];
const sectionRefs = ref({});
const activeSectionKey = ref('basic');
const contentScrollEl = ref(null);
const draggingIntroImageId = ref(null);
const cropDialogVisible = ref(false);
const saving = ref(false);
const detailLoading = ref(false);
const editItemId = computed(() => {
    const id = route.query.id;
    return typeof id === 'string' && id.trim() ? id.trim() : '';
});
const isEditMode = computed(() => route.query.mode === 'edit' && !!editItemId.value);
const cropImageSrc = ref('');
const cropImageName = ref('');
const cropImageElement = ref(null);
const cropImageNaturalWidth = ref(0);
const cropImageNaturalHeight = ref(0);
const cropImageScale = ref(1);
const cropBaseScale = ref(1);
const cropZoomLevel = ref(0);
const cropOffsetX = ref(0);
const cropOffsetY = ref(0);
const cropDragging = ref(false);
const cropDragStartX = ref(0);
const cropDragStartY = ref(0);
const cropDragOriginX = ref(0);
const cropDragOriginY = ref(0);
const CROP_FRAME_SIZE = 480;
const CROP_ZOOM_MIN = -100;
const CROP_ZOOM_MAX = 100;
const cropDisplayWidth = computed(() => cropImageNaturalWidth.value * cropImageScale.value);
const cropDisplayHeight = computed(() => cropImageNaturalHeight.value * cropImageScale.value);
const resolveItemOrgId = () => {
    const orgId = (sessionStore.currentOrgId ?? '').trim();
    if (!orgId) {
        return undefined;
    }
    if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
        return orgId;
    }
    return undefined;
};
const addUnitRow = (index) => {
    unitSettingRows.value.splice(index + 1, 0, {
        unit: '',
        convertFrom: '1',
        convertTo: '',
        volume: '',
        volumeUnit: 'dm³',
        weight: '',
        weightUnit: '斤',
        barcode: '',
    });
};
const removeUnitRow = (index) => {
    if (index === 0 || unitSettingRows.value.length <= 1) {
        return;
    }
    unitSettingRows.value.splice(index, 1);
};
const addSupplierRelationRow = (index) => {
    supplierRelationRows.value.splice(index + 1, 0, {
        key: supplierRowSeed.value,
        supplier: '',
        contact: '',
        phone: '',
    });
    supplierRowSeed.value += 1;
};
const removeSupplierRelationRow = (index) => {
    if (supplierRelationRows.value.length <= 1) {
        return;
    }
    const removedRow = supplierRelationRows.value[index];
    supplierRelationRows.value.splice(index, 1);
    if (removedRow && removedRow.key === defaultSupplierRowKey.value) {
        defaultSupplierRowKey.value = supplierRelationRows.value[0]?.key ?? 0;
    }
};
const readFileAsDataUrl = (file) => (new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('读取图片失败'));
    reader.readAsDataURL(file);
}));
const loadImageElement = (src) => (new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('加载图片失败'));
    image.src = src;
}));
const clampCropOffset = (x, y) => {
    const minX = cropDisplayWidth.value <= CROP_FRAME_SIZE ? 0 : CROP_FRAME_SIZE - cropDisplayWidth.value;
    const maxX = cropDisplayWidth.value <= CROP_FRAME_SIZE ? CROP_FRAME_SIZE - cropDisplayWidth.value : 0;
    const minY = cropDisplayHeight.value <= CROP_FRAME_SIZE ? 0 : CROP_FRAME_SIZE - cropDisplayHeight.value;
    const maxY = cropDisplayHeight.value <= CROP_FRAME_SIZE ? CROP_FRAME_SIZE - cropDisplayHeight.value : 0;
    return {
        x: Math.min(maxX, Math.max(minX, x)),
        y: Math.min(maxY, Math.max(minY, y)),
    };
};
const zoomLevelToScale = (level) => cropBaseScale.value * (2 ** (level / 50));
const initCropPosition = () => {
    const baseScale = Math.max(CROP_FRAME_SIZE / cropImageNaturalWidth.value, CROP_FRAME_SIZE / cropImageNaturalHeight.value);
    cropBaseScale.value = baseScale;
    cropZoomLevel.value = 0;
    cropImageScale.value = baseScale;
    cropOffsetX.value = (CROP_FRAME_SIZE - cropDisplayWidth.value) / 2;
    cropOffsetY.value = (CROP_FRAME_SIZE - cropDisplayHeight.value) / 2;
};
const onCropZoomLevelChange = (nextLevel) => {
    if (!cropImageElement.value) {
        return;
    }
    const limitedLevel = Math.min(CROP_ZOOM_MAX, Math.max(CROP_ZOOM_MIN, nextLevel));
    const limitedScale = zoomLevelToScale(limitedLevel);
    const frameCenterX = CROP_FRAME_SIZE / 2;
    const frameCenterY = CROP_FRAME_SIZE / 2;
    const imagePointX = (frameCenterX - cropOffsetX.value) / cropImageScale.value;
    const imagePointY = (frameCenterY - cropOffsetY.value) / cropImageScale.value;
    cropZoomLevel.value = limitedLevel;
    cropImageScale.value = limitedScale;
    const nextX = frameCenterX - imagePointX * limitedScale;
    const nextY = frameCenterY - imagePointY * limitedScale;
    const clamped = clampCropOffset(nextX, nextY);
    cropOffsetX.value = clamped.x;
    cropOffsetY.value = clamped.y;
};
const zoomCropImage = (delta) => {
    onCropZoomLevelChange(cropZoomLevel.value + delta);
};
const openCropDialog = async (file, fileName) => {
    const src = await readFileAsDataUrl(file);
    const image = await loadImageElement(src);
    cropImageSrc.value = src;
    cropImageName.value = fileName;
    cropImageElement.value = image;
    cropImageNaturalWidth.value = image.naturalWidth;
    cropImageNaturalHeight.value = image.naturalHeight;
    initCropPosition();
    cropDialogVisible.value = true;
};
const onIntroImageChange = async (uploadFile) => {
    const raw = uploadFile.raw;
    if (!raw) {
        return;
    }
    await openCropDialog(raw, uploadFile.name || raw.name || `图片${introImageSeed.value}`);
};
const onCropPointerMove = (event) => {
    if (!cropDragging.value) {
        return;
    }
    const nextX = cropDragOriginX.value + (event.clientX - cropDragStartX.value);
    const nextY = cropDragOriginY.value + (event.clientY - cropDragStartY.value);
    const clamped = clampCropOffset(nextX, nextY);
    cropOffsetX.value = clamped.x;
    cropOffsetY.value = clamped.y;
};
const stopCropDragging = () => {
    cropDragging.value = false;
    window.removeEventListener('mousemove', onCropPointerMove);
    window.removeEventListener('mouseup', stopCropDragging);
};
const onCropPointerDown = (event) => {
    if (!cropImageElement.value) {
        return;
    }
    cropDragging.value = true;
    cropDragStartX.value = event.clientX;
    cropDragStartY.value = event.clientY;
    cropDragOriginX.value = cropOffsetX.value;
    cropDragOriginY.value = cropOffsetY.value;
    window.addEventListener('mousemove', onCropPointerMove);
    window.addEventListener('mouseup', stopCropDragging);
};
const closeCropDialog = () => {
    stopCropDragging();
    cropDialogVisible.value = false;
    cropImageSrc.value = '';
    cropImageName.value = '';
    cropImageElement.value = null;
    cropImageNaturalWidth.value = 0;
    cropImageNaturalHeight.value = 0;
    cropBaseScale.value = 1;
    cropZoomLevel.value = 0;
};
const confirmCropImage = () => {
    if (!cropImageElement.value) {
        return;
    }
    const canvas = document.createElement('canvas');
    canvas.width = CROP_FRAME_SIZE;
    canvas.height = CROP_FRAME_SIZE;
    const context = canvas.getContext('2d');
    if (!context) {
        return;
    }
    const sourceX = (-cropOffsetX.value) / cropImageScale.value;
    const sourceY = (-cropOffsetY.value) / cropImageScale.value;
    const sourceWidth = CROP_FRAME_SIZE / cropImageScale.value;
    const sourceHeight = CROP_FRAME_SIZE / cropImageScale.value;
    context.drawImage(cropImageElement.value, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, CROP_FRAME_SIZE, CROP_FRAME_SIZE);
    introImages.value.push({
        id: introImageSeed.value,
        name: cropImageName.value || `图片${introImageSeed.value}`,
        url: canvas.toDataURL('image/jpeg', 0.92),
    });
    introImageSeed.value += 1;
    closeCropDialog();
};
const removeIntroImage = (id) => {
    introImages.value = introImages.value.filter((item) => item.id !== id);
};
const addNutritionRow = (index) => {
    nutritionRows.value.splice(index + 1, 0, {
        id: nutritionSeed.value,
        item: '',
        per100g: '',
        nrv: '',
    });
    nutritionSeed.value += 1;
};
const removeNutritionRow = (index) => {
    if (nutritionRows.value.length <= 1) {
        return;
    }
    nutritionRows.value.splice(index, 1);
};
const onIntroImageDragStart = (id) => {
    draggingIntroImageId.value = id;
};
const onIntroImageDrop = (targetId) => {
    const dragId = draggingIntroImageId.value;
    if (!dragId || dragId === targetId) {
        return;
    }
    const fromIndex = introImages.value.findIndex((item) => item.id === dragId);
    const targetIndex = introImages.value.findIndex((item) => item.id === targetId);
    if (fromIndex < 0 || targetIndex < 0) {
        return;
    }
    const [moved] = introImages.value.splice(fromIndex, 1);
    introImages.value.splice(targetIndex, 0, moved);
    draggingIntroImageId.value = null;
};
const onIntroImageDragEnd = () => {
    draggingIntroImageId.value = null;
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
    let current = sectionNavs[0]?.key ?? 'basic';
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
    const target = sectionRefs.value[key];
    if (!target) {
        return;
    }
    activeSectionKey.value = key;
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
const isUnitOptionDisabled = (option, currentUnit) => (option !== currentUnit && unitSettingRows.value.some((row) => row.unit === option));
watch(stocktakeUnitOptions, (options) => {
    form.stocktakeUnits = form.stocktakeUnits.filter((item) => options.includes(item));
    const normalizeSingle = (value) => (options.includes(value) ? value : '');
    form.defaultPurchaseUnit = normalizeSingle(form.defaultPurchaseUnit);
    form.defaultOrderUnit = normalizeSingle(form.defaultOrderUnit);
    form.defaultStockUnit = normalizeSingle(form.defaultStockUnit);
    form.defaultCostUnit = normalizeSingle(form.defaultCostUnit);
}, { immediate: true });
onMounted(() => {
    contentScrollEl.value = document.querySelector('.content');
    contentScrollEl.value?.addEventListener('scroll', updateActiveSectionByScroll, { passive: true });
    updateActiveSectionByScroll();
    loadDetailIfEditMode();
});
onBeforeUnmount(() => {
    contentScrollEl.value?.removeEventListener('scroll', updateActiveSectionByScroll);
});
const goBack = () => {
    router.push('/archive/1/1');
};
const buildCreatePayload = () => ({
    ...form,
    code: undefined,
    unitSettingRows: unitSettingRows.value.map((row) => ({ ...row })),
    supplierRelationRows: supplierRelationRows.value.map((row) => ({ ...row })),
    defaultSupplierRowKey: defaultSupplierRowKey.value,
    introImages: introImages.value.map((image) => ({ ...image })),
    nutritionHeaders: { ...nutritionHeaders },
    nutritionRows: nutritionRows.value.map((row) => ({ ...row })),
    extensionInfoRows: extensionInfoRows.value.map((row) => ({ ...row })),
});
const applyDetailPayload = (payload) => {
    Object.assign(form, {
        ...form,
        ...payload,
        stocktakeUnits: payload.stocktakeUnits ?? [],
        stocktakeTypes: payload.stocktakeTypes ?? [],
    });
    unitSettingRows.value = payload.unitSettingRows?.length
        ? payload.unitSettingRows.map((row) => ({
            unit: row.unit ?? '',
            convertFrom: row.convertFrom ?? '1',
            convertTo: row.convertTo ?? '',
            volume: row.volume ?? '',
            volumeUnit: row.volumeUnit ?? 'dm³',
            weight: row.weight ?? '',
            weightUnit: row.weightUnit ?? '斤',
            barcode: row.barcode ?? '',
        }))
        : [{ unit: '斤', convertFrom: '1', convertTo: '', volume: '', volumeUnit: 'dm³', weight: '', weightUnit: '斤', barcode: '' }];
    supplierRelationRows.value = payload.supplierRelationRows?.length
        ? payload.supplierRelationRows.map((row, index) => ({
            key: row.key ?? index + 1,
            supplier: row.supplier ?? '',
            contact: row.contact ?? '',
            phone: row.phone ?? '',
        }))
        : [{ key: 1, supplier: '', contact: '', phone: '' }];
    defaultSupplierRowKey.value = payload.defaultSupplierRowKey ?? supplierRelationRows.value[0]?.key ?? 1;
    introImages.value = payload.introImages?.map((row, index) => ({
        id: row.id ?? index + 1,
        name: row.name ?? `图片${index + 1}`,
        url: row.url ?? '',
    })) ?? [];
    nutritionRows.value = payload.nutritionRows?.length
        ? payload.nutritionRows.map((row, index) => ({
            id: row.id ?? index + 1,
            item: row.item ?? '',
            per100g: row.per100g ?? '',
            nrv: row.nrv ?? '',
        }))
        : [{ id: 1, item: '能量', per100g: '', nrv: '' }];
    Object.assign(nutritionHeaders, {
        item: payload.nutritionHeaders?.item ?? '项目',
        per100g: payload.nutritionHeaders?.per100g ?? '每100g',
        nrv: payload.nutritionHeaders?.nrv ?? 'NRV%',
    });
    const valueMap = new Map((payload.extensionInfoRows ?? [])
        .filter((row) => Boolean(row))
        .map((row) => [row.name ?? '', row.value ?? '']));
    extensionInfoRows.value = extensionInfoFieldNames.map((name, index) => ({
        id: index + 1,
        name,
        value: valueMap.get(name) ?? '',
    }));
};
const handleSaveDraft = async () => {
    if (saving.value) {
        return;
    }
    saving.value = true;
    try {
        const res = await saveItemDraftApi(buildCreatePayload(), resolveItemOrgId());
        ElMessage.success(`草稿已保存（${res.id}）`);
    }
    finally {
        saving.value = false;
    }
};
const handleSave = async () => {
    if (saving.value) {
        return;
    }
    if (!form.name.trim()) {
        ElMessage.warning('请先填写物品名称');
        return;
    }
    if (!form.category.trim()) {
        ElMessage.warning('请先选择物品类别');
        return;
    }
    saving.value = true;
    try {
        const payload = buildCreatePayload();
        if (isEditMode.value && editItemId.value) {
            await updateItemApi(editItemId.value, payload, resolveItemOrgId());
            ElMessage.success('编辑物品成功');
        }
        else {
            await createItemApi(payload, resolveItemOrgId());
            ElMessage.success('新增物品成功');
        }
        router.push('/archive/1/1');
    }
    finally {
        saving.value = false;
    }
};
const loadDetailIfEditMode = async () => {
    if (!isEditMode.value || !editItemId.value) {
        return;
    }
    detailLoading.value = true;
    try {
        const detail = await fetchItemDetailApi(editItemId.value, resolveItemOrgId());
        applyDetailPayload(detail);
    }
    finally {
        detailLoading.value = false;
    }
};
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
    onBack: (__VLS_ctx.goBack)
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
const __VLS_10 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
    model: (__VLS_ctx.form),
    labelWidth: "86px",
    ...{ class: "item-create-form" },
}));
const __VLS_12 = __VLS_11({
    model: (__VLS_ctx.form),
    labelWidth: "86px",
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
/** @type {[typeof CommonMnemonicField, ]} */ ;
// @ts-ignore
const __VLS_14 = __VLS_asFunctionalComponent(CommonMnemonicField, new CommonMnemonicField({
    sourceValue: (__VLS_ctx.form.name),
    modelValue: (__VLS_ctx.form.mnemonicCode),
    sourceLabel: "物品名称",
    mnemonicLabel: "助记码",
    sourceField: "name",
    mnemonicField: "mnemonicCode",
    sourcePlaceholder: "请输入物品名称",
    mnemonicPlaceholder: "根据物品名称自动生成",
}));
const __VLS_15 = __VLS_14({
    sourceValue: (__VLS_ctx.form.name),
    modelValue: (__VLS_ctx.form.mnemonicCode),
    sourceLabel: "物品名称",
    mnemonicLabel: "助记码",
    sourceField: "name",
    mnemonicField: "mnemonicCode",
    sourcePlaceholder: "请输入物品名称",
    mnemonicPlaceholder: "根据物品名称自动生成",
}, ...__VLS_functionalComponentArgsRest(__VLS_14));
const __VLS_17 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
    label: "物品类别",
}));
const __VLS_19 = __VLS_18({
    label: "物品类别",
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
__VLS_20.slots.default;
const __VLS_21 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_22 = __VLS_asFunctionalComponent(__VLS_21, new __VLS_21({
    modelValue: (__VLS_ctx.form.category),
    placeholder: "请选择物品类别",
}));
const __VLS_23 = __VLS_22({
    modelValue: (__VLS_ctx.form.category),
    placeholder: "请选择物品类别",
}, ...__VLS_functionalComponentArgsRest(__VLS_22));
__VLS_24.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.categoryOptions))) {
    const __VLS_25 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_26 = __VLS_asFunctionalComponent(__VLS_25, new __VLS_25({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_27 = __VLS_26({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_26));
}
var __VLS_24;
var __VLS_20;
const __VLS_29 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_30 = __VLS_asFunctionalComponent(__VLS_29, new __VLS_29({
    label: "规格型号",
}));
const __VLS_31 = __VLS_30({
    label: "规格型号",
}, ...__VLS_functionalComponentArgsRest(__VLS_30));
__VLS_32.slots.default;
const __VLS_33 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_34 = __VLS_asFunctionalComponent(__VLS_33, new __VLS_33({
    modelValue: (__VLS_ctx.form.spec),
    placeholder: "请输入规格型号",
}));
const __VLS_35 = __VLS_34({
    modelValue: (__VLS_ctx.form.spec),
    placeholder: "请输入规格型号",
}, ...__VLS_functionalComponentArgsRest(__VLS_34));
var __VLS_32;
const __VLS_37 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_38 = __VLS_asFunctionalComponent(__VLS_37, new __VLS_37({
    label: "物品状态",
}));
const __VLS_39 = __VLS_38({
    label: "物品状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_38));
__VLS_40.slots.default;
const __VLS_41 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_42 = __VLS_asFunctionalComponent(__VLS_41, new __VLS_41({
    modelValue: (__VLS_ctx.form.status),
}));
const __VLS_43 = __VLS_42({
    modelValue: (__VLS_ctx.form.status),
}, ...__VLS_functionalComponentArgsRest(__VLS_42));
__VLS_44.slots.default;
const __VLS_45 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_46 = __VLS_asFunctionalComponent(__VLS_45, new __VLS_45({
    value: "启用",
}));
const __VLS_47 = __VLS_46({
    value: "启用",
}, ...__VLS_functionalComponentArgsRest(__VLS_46));
__VLS_48.slots.default;
var __VLS_48;
const __VLS_49 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_50 = __VLS_asFunctionalComponent(__VLS_49, new __VLS_49({
    value: "停用",
}));
const __VLS_51 = __VLS_50({
    value: "停用",
}, ...__VLS_functionalComponentArgsRest(__VLS_50));
__VLS_52.slots.default;
var __VLS_52;
var __VLS_44;
var __VLS_40;
const __VLS_53 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_54 = __VLS_asFunctionalComponent(__VLS_53, new __VLS_53({
    label: "物品品牌",
}));
const __VLS_55 = __VLS_54({
    label: "物品品牌",
}, ...__VLS_functionalComponentArgsRest(__VLS_54));
__VLS_56.slots.default;
const __VLS_57 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_58 = __VLS_asFunctionalComponent(__VLS_57, new __VLS_57({
    modelValue: (__VLS_ctx.form.brand),
    placeholder: "请输入物品品牌",
}));
const __VLS_59 = __VLS_58({
    modelValue: (__VLS_ctx.form.brand),
    placeholder: "请输入物品品牌",
}, ...__VLS_functionalComponentArgsRest(__VLS_58));
var __VLS_56;
const __VLS_61 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_62 = __VLS_asFunctionalComponent(__VLS_61, new __VLS_61({
    label: "物品条形码",
}));
const __VLS_63 = __VLS_62({
    label: "物品条形码",
}, ...__VLS_functionalComponentArgsRest(__VLS_62));
__VLS_64.slots.default;
const __VLS_65 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_66 = __VLS_asFunctionalComponent(__VLS_65, new __VLS_65({
    modelValue: (__VLS_ctx.form.barcode),
    placeholder: "请输入物品条形码",
}));
const __VLS_67 = __VLS_66({
    modelValue: (__VLS_ctx.form.barcode),
    placeholder: "请输入物品条形码",
}, ...__VLS_functionalComponentArgsRest(__VLS_66));
var __VLS_64;
const __VLS_69 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_70 = __VLS_asFunctionalComponent(__VLS_69, new __VLS_69({
    label: "第三方编码",
}));
const __VLS_71 = __VLS_70({
    label: "第三方编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_70));
__VLS_72.slots.default;
const __VLS_73 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_74 = __VLS_asFunctionalComponent(__VLS_73, new __VLS_73({
    modelValue: (__VLS_ctx.form.thirdPartyCode),
    placeholder: "请输入第三方编码",
}));
const __VLS_75 = __VLS_74({
    modelValue: (__VLS_ctx.form.thirdPartyCode),
    placeholder: "请输入第三方编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_74));
var __VLS_72;
const __VLS_77 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_78 = __VLS_asFunctionalComponent(__VLS_77, new __VLS_77({
    label: "别名",
}));
const __VLS_79 = __VLS_78({
    label: "别名",
}, ...__VLS_functionalComponentArgsRest(__VLS_78));
__VLS_80.slots.default;
const __VLS_81 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_82 = __VLS_asFunctionalComponent(__VLS_81, new __VLS_81({
    modelValue: (__VLS_ctx.form.alias),
    placeholder: "请输入别名",
}));
const __VLS_83 = __VLS_82({
    modelValue: (__VLS_ctx.form.alias),
    placeholder: "请输入别名",
}, ...__VLS_functionalComponentArgsRest(__VLS_82));
var __VLS_80;
const __VLS_85 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_86 = __VLS_asFunctionalComponent(__VLS_85, new __VLS_85({
    label: "ABC 分类",
}));
const __VLS_87 = __VLS_86({
    label: "ABC 分类",
}, ...__VLS_functionalComponentArgsRest(__VLS_86));
__VLS_88.slots.default;
const __VLS_89 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_90 = __VLS_asFunctionalComponent(__VLS_89, new __VLS_89({
    modelValue: (__VLS_ctx.form.abcClass),
    placeholder: "请选择 ABC 分类",
}));
const __VLS_91 = __VLS_90({
    modelValue: (__VLS_ctx.form.abcClass),
    placeholder: "请选择 ABC 分类",
}, ...__VLS_functionalComponentArgsRest(__VLS_90));
__VLS_92.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.abcOptions))) {
    const __VLS_93 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_94 = __VLS_asFunctionalComponent(__VLS_93, new __VLS_93({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_95 = __VLS_94({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_94));
}
var __VLS_92;
var __VLS_88;
const __VLS_97 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_98 = __VLS_asFunctionalComponent(__VLS_97, new __VLS_97({
    label: "批次管理",
}));
const __VLS_99 = __VLS_98({
    label: "批次管理",
}, ...__VLS_functionalComponentArgsRest(__VLS_98));
__VLS_100.slots.default;
const __VLS_101 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_102 = __VLS_asFunctionalComponent(__VLS_101, new __VLS_101({
    modelValue: (__VLS_ctx.form.batchManagement),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}));
const __VLS_103 = __VLS_102({
    modelValue: (__VLS_ctx.form.batchManagement),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}, ...__VLS_functionalComponentArgsRest(__VLS_102));
var __VLS_100;
const __VLS_105 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_106 = __VLS_asFunctionalComponent(__VLS_105, new __VLS_105({
    label: "保质期",
}));
const __VLS_107 = __VLS_106({
    label: "保质期",
}, ...__VLS_functionalComponentArgsRest(__VLS_106));
__VLS_108.slots.default;
const __VLS_109 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_110 = __VLS_asFunctionalComponent(__VLS_109, new __VLS_109({
    modelValue: (__VLS_ctx.form.shelfLifeEnabled),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}));
const __VLS_111 = __VLS_110({
    modelValue: (__VLS_ctx.form.shelfLifeEnabled),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}, ...__VLS_functionalComponentArgsRest(__VLS_110));
var __VLS_108;
const __VLS_113 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_114 = __VLS_asFunctionalComponent(__VLS_113, new __VLS_113({
    label: "保质期天数",
}));
const __VLS_115 = __VLS_114({
    label: "保质期天数",
}, ...__VLS_functionalComponentArgsRest(__VLS_114));
__VLS_116.slots.default;
const __VLS_117 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_118 = __VLS_asFunctionalComponent(__VLS_117, new __VLS_117({
    modelValue: (__VLS_ctx.form.shelfLifeDays),
    min: (0),
    disabled: (!__VLS_ctx.form.shelfLifeEnabled),
    controlsPosition: "right",
    placeholder: "请输入保质期天数",
}));
const __VLS_119 = __VLS_118({
    modelValue: (__VLS_ctx.form.shelfLifeDays),
    min: (0),
    disabled: (!__VLS_ctx.form.shelfLifeEnabled),
    controlsPosition: "right",
    placeholder: "请输入保质期天数",
}, ...__VLS_functionalComponentArgsRest(__VLS_118));
var __VLS_116;
const __VLS_121 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_122 = __VLS_asFunctionalComponent(__VLS_121, new __VLS_121({
    label: "提前预警天数",
}));
const __VLS_123 = __VLS_122({
    label: "提前预警天数",
}, ...__VLS_functionalComponentArgsRest(__VLS_122));
__VLS_124.slots.default;
const __VLS_125 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_126 = __VLS_asFunctionalComponent(__VLS_125, new __VLS_125({
    modelValue: (__VLS_ctx.form.warningDays),
    min: (0),
    disabled: (!__VLS_ctx.form.shelfLifeEnabled),
    controlsPosition: "right",
    placeholder: "请输入提前预警天数",
}));
const __VLS_127 = __VLS_126({
    modelValue: (__VLS_ctx.form.warningDays),
    min: (0),
    disabled: (!__VLS_ctx.form.shelfLifeEnabled),
    controlsPosition: "right",
    placeholder: "请输入提前预警天数",
}, ...__VLS_functionalComponentArgsRest(__VLS_126));
var __VLS_124;
const __VLS_129 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_130 = __VLS_asFunctionalComponent(__VLS_129, new __VLS_129({
    label: "呆滞天数",
}));
const __VLS_131 = __VLS_130({
    label: "呆滞天数",
}, ...__VLS_functionalComponentArgsRest(__VLS_130));
__VLS_132.slots.default;
const __VLS_133 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_134 = __VLS_asFunctionalComponent(__VLS_133, new __VLS_133({
    modelValue: (__VLS_ctx.form.stagnantDays),
    min: (0),
    controlsPosition: "right",
    placeholder: "请输入呆滞天数",
}));
const __VLS_135 = __VLS_134({
    modelValue: (__VLS_ctx.form.stagnantDays),
    min: (0),
    controlsPosition: "right",
    placeholder: "请输入呆滞天数",
}, ...__VLS_functionalComponentArgsRest(__VLS_134));
var __VLS_132;
const __VLS_137 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_138 = __VLS_asFunctionalComponent(__VLS_137, new __VLS_137({
    label: "物品标签",
}));
const __VLS_139 = __VLS_138({
    label: "物品标签",
}, ...__VLS_functionalComponentArgsRest(__VLS_138));
__VLS_140.slots.default;
const __VLS_141 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_142 = __VLS_asFunctionalComponent(__VLS_141, new __VLS_141({
    modelValue: (__VLS_ctx.form.tag),
    placeholder: "请输入物品标签",
}));
const __VLS_143 = __VLS_142({
    modelValue: (__VLS_ctx.form.tag),
    placeholder: "请输入物品标签",
}, ...__VLS_functionalComponentArgsRest(__VLS_142));
var __VLS_140;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('unit')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_145 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_146 = __VLS_asFunctionalComponent(__VLS_145, new __VLS_145({
    label: "单位设置",
    ...{ class: "unit-setting-form-item" },
}));
const __VLS_147 = __VLS_146({
    label: "单位设置",
    ...{ class: "unit-setting-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_146));
__VLS_148.slots.default;
const __VLS_149 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_150 = __VLS_asFunctionalComponent(__VLS_149, new __VLS_149({
    data: (__VLS_ctx.unitSettingRows),
    border: true,
    stripe: true,
    ...{ class: "unit-setting-table" },
}));
const __VLS_151 = __VLS_150({
    data: (__VLS_ctx.unitSettingRows),
    border: true,
    stripe: true,
    ...{ class: "unit-setting-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_150));
__VLS_152.slots.default;
const __VLS_153 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_154 = __VLS_asFunctionalComponent(__VLS_153, new __VLS_153({
    label: "序号",
    width: "46",
}));
const __VLS_155 = __VLS_154({
    label: "序号",
    width: "46",
}, ...__VLS_functionalComponentArgsRest(__VLS_154));
__VLS_156.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_156.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    ($index + 1);
}
var __VLS_156;
const __VLS_157 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_158 = __VLS_asFunctionalComponent(__VLS_157, new __VLS_157({
    label: "操作",
    width: "70",
}));
const __VLS_159 = __VLS_158({
    label: "操作",
    width: "70",
}, ...__VLS_functionalComponentArgsRest(__VLS_158));
__VLS_160.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_160.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    if ($index > 0) {
        const __VLS_161 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_162 = __VLS_asFunctionalComponent(__VLS_161, new __VLS_161({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
            ...{ class: "unit-op-btn" },
        }));
        const __VLS_163 = __VLS_162({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
            ...{ class: "unit-op-btn" },
        }, ...__VLS_functionalComponentArgsRest(__VLS_162));
        let __VLS_165;
        let __VLS_166;
        let __VLS_167;
        const __VLS_168 = {
            onClick: (...[$event]) => {
                if (!($index > 0))
                    return;
                __VLS_ctx.addUnitRow($index);
            }
        };
        __VLS_164.slots.default;
        var __VLS_164;
        const __VLS_169 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_170 = __VLS_asFunctionalComponent(__VLS_169, new __VLS_169({
            ...{ 'onClick': {} },
            text: true,
            ...{ class: "unit-op-btn" },
            disabled: (__VLS_ctx.unitSettingRows.length <= 1),
        }));
        const __VLS_171 = __VLS_170({
            ...{ 'onClick': {} },
            text: true,
            ...{ class: "unit-op-btn" },
            disabled: (__VLS_ctx.unitSettingRows.length <= 1),
        }, ...__VLS_functionalComponentArgsRest(__VLS_170));
        let __VLS_173;
        let __VLS_174;
        let __VLS_175;
        const __VLS_176 = {
            onClick: (...[$event]) => {
                if (!($index > 0))
                    return;
                __VLS_ctx.removeUnitRow($index);
            }
        };
        __VLS_172.slots.default;
        var __VLS_172;
    }
}
var __VLS_160;
const __VLS_177 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_178 = __VLS_asFunctionalComponent(__VLS_177, new __VLS_177({
    label: "单位",
    width: "148",
}));
const __VLS_179 = __VLS_178({
    label: "单位",
    width: "148",
}, ...__VLS_functionalComponentArgsRest(__VLS_178));
__VLS_180.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_180.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    if ($index === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "unit-base-cell" },
        });
        const __VLS_181 = {}.ElInput;
        /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
        // @ts-ignore
        const __VLS_182 = __VLS_asFunctionalComponent(__VLS_181, new __VLS_181({
            value: "基准单位",
            disabled: true,
        }));
        const __VLS_183 = __VLS_182({
            value: "基准单位",
            disabled: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_182));
        const __VLS_185 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_186 = __VLS_asFunctionalComponent(__VLS_185, new __VLS_185({
            modelValue: (row.unit),
            ...{ class: "unit-base-select" },
        }));
        const __VLS_187 = __VLS_186({
            modelValue: (row.unit),
            ...{ class: "unit-base-select" },
        }, ...__VLS_functionalComponentArgsRest(__VLS_186));
        __VLS_188.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.unitOptions))) {
            const __VLS_189 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_190 = __VLS_asFunctionalComponent(__VLS_189, new __VLS_189({
                key: (`setting-${option}`),
                label: (option),
                value: (option),
                disabled: (__VLS_ctx.isUnitOptionDisabled(option, row.unit)),
            }));
            const __VLS_191 = __VLS_190({
                key: (`setting-${option}`),
                label: (option),
                value: (option),
                disabled: (__VLS_ctx.isUnitOptionDisabled(option, row.unit)),
            }, ...__VLS_functionalComponentArgsRest(__VLS_190));
        }
        var __VLS_188;
    }
    else {
        const __VLS_193 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_194 = __VLS_asFunctionalComponent(__VLS_193, new __VLS_193({
            modelValue: (row.unit),
        }));
        const __VLS_195 = __VLS_194({
            modelValue: (row.unit),
        }, ...__VLS_functionalComponentArgsRest(__VLS_194));
        __VLS_196.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.unitOptions))) {
            const __VLS_197 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_198 = __VLS_asFunctionalComponent(__VLS_197, new __VLS_197({
                key: (`setting-${option}`),
                label: (option),
                value: (option),
                disabled: (__VLS_ctx.isUnitOptionDisabled(option, row.unit)),
            }));
            const __VLS_199 = __VLS_198({
                key: (`setting-${option}`),
                label: (option),
                value: (option),
                disabled: (__VLS_ctx.isUnitOptionDisabled(option, row.unit)),
            }, ...__VLS_functionalComponentArgsRest(__VLS_198));
        }
        var __VLS_196;
    }
}
var __VLS_180;
const __VLS_201 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_202 = __VLS_asFunctionalComponent(__VLS_201, new __VLS_201({
    label: "单位换算",
    width: "320",
}));
const __VLS_203 = __VLS_202({
    label: "单位换算",
    width: "320",
}, ...__VLS_functionalComponentArgsRest(__VLS_202));
__VLS_204.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_204.slots;
    const [{ row, $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    if ($index === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "unit-base-tip" },
            title: (__VLS_ctx.unitSettingTip),
        });
        (__VLS_ctx.unitSettingTip);
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "unit-convert-cell" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "unit-convert-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input, __VLS_intrinsicElements.input)({
            value: (row.convertFrom),
            placeholder: "输入数量",
            maxlength: "16",
            type: "text",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (row.unit || '单位');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "unit-convert-eq" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "unit-convert-group" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input, __VLS_intrinsicElements.input)({
            value: (row.convertTo),
            placeholder: "输入数量",
            maxlength: "16",
            type: "text",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.baseUnitLabel);
    }
}
var __VLS_204;
const __VLS_205 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_206 = __VLS_asFunctionalComponent(__VLS_205, new __VLS_205({
    label: "体积",
    width: "156",
}));
const __VLS_207 = __VLS_206({
    label: "体积",
    width: "156",
}, ...__VLS_functionalComponentArgsRest(__VLS_206));
__VLS_208.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_208.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "unit-value-cell" },
    });
    const __VLS_209 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_210 = __VLS_asFunctionalComponent(__VLS_209, new __VLS_209({
        modelValue: (row.volume),
    }));
    const __VLS_211 = __VLS_210({
        modelValue: (row.volume),
    }, ...__VLS_functionalComponentArgsRest(__VLS_210));
    const __VLS_213 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_214 = __VLS_asFunctionalComponent(__VLS_213, new __VLS_213({
        modelValue: (row.volumeUnit),
        ...{ class: "unit-value-select" },
    }));
    const __VLS_215 = __VLS_214({
        modelValue: (row.volumeUnit),
        ...{ class: "unit-value-select" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_214));
    __VLS_216.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.volumeUnitOptions))) {
        const __VLS_217 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_218 = __VLS_asFunctionalComponent(__VLS_217, new __VLS_217({
            key: (`vol-${option}`),
            label: (option),
            value: (option),
        }));
        const __VLS_219 = __VLS_218({
            key: (`vol-${option}`),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_218));
    }
    var __VLS_216;
}
var __VLS_208;
const __VLS_221 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_222 = __VLS_asFunctionalComponent(__VLS_221, new __VLS_221({
    label: "重量",
    width: "168",
}));
const __VLS_223 = __VLS_222({
    label: "重量",
    width: "168",
}, ...__VLS_functionalComponentArgsRest(__VLS_222));
__VLS_224.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_224.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "unit-value-cell" },
    });
    const __VLS_225 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_226 = __VLS_asFunctionalComponent(__VLS_225, new __VLS_225({
        modelValue: (row.weight),
    }));
    const __VLS_227 = __VLS_226({
        modelValue: (row.weight),
    }, ...__VLS_functionalComponentArgsRest(__VLS_226));
    const __VLS_229 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_230 = __VLS_asFunctionalComponent(__VLS_229, new __VLS_229({
        modelValue: (row.weightUnit),
        ...{ class: "unit-value-select" },
    }));
    const __VLS_231 = __VLS_230({
        modelValue: (row.weightUnit),
        ...{ class: "unit-value-select" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_230));
    __VLS_232.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.weightUnitOptions))) {
        const __VLS_233 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_234 = __VLS_asFunctionalComponent(__VLS_233, new __VLS_233({
            key: (`weight-${option}`),
            label: (option),
            value: (option),
        }));
        const __VLS_235 = __VLS_234({
            key: (`weight-${option}`),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_234));
    }
    var __VLS_232;
}
var __VLS_224;
const __VLS_237 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_238 = __VLS_asFunctionalComponent(__VLS_237, new __VLS_237({
    label: "条码",
    minWidth: "120",
}));
const __VLS_239 = __VLS_238({
    label: "条码",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_238));
__VLS_240.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_240.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_241 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_242 = __VLS_asFunctionalComponent(__VLS_241, new __VLS_241({
        modelValue: (row.barcode),
    }));
    const __VLS_243 = __VLS_242({
        modelValue: (row.barcode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_242));
}
var __VLS_240;
var __VLS_152;
var __VLS_148;
for (const [field] of __VLS_getVForSourceType((__VLS_ctx.defaultUnitFields))) {
    const __VLS_245 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_246 = __VLS_asFunctionalComponent(__VLS_245, new __VLS_245({
        key: (field.key),
        label: (field.label),
    }));
    const __VLS_247 = __VLS_246({
        key: (field.key),
        label: (field.label),
    }, ...__VLS_functionalComponentArgsRest(__VLS_246));
    __VLS_248.slots.default;
    const __VLS_249 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_250 = __VLS_asFunctionalComponent(__VLS_249, new __VLS_249({
        modelValue: (__VLS_ctx.form[field.key]),
    }));
    const __VLS_251 = __VLS_250({
        modelValue: (__VLS_ctx.form[field.key]),
    }, ...__VLS_functionalComponentArgsRest(__VLS_250));
    __VLS_252.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.defaultUnitOptions))) {
        const __VLS_253 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_254 = __VLS_asFunctionalComponent(__VLS_253, new __VLS_253({
            key: (`${field.optionKey}-${option}`),
            label: (option),
            value: (option),
        }));
        const __VLS_255 = __VLS_254({
            key: (`${field.optionKey}-${option}`),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_254));
    }
    var __VLS_252;
    var __VLS_248;
}
const __VLS_257 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_258 = __VLS_asFunctionalComponent(__VLS_257, new __VLS_257({
    label: "盘点单位",
    ...{ class: "stocktake-form-item" },
}));
const __VLS_259 = __VLS_258({
    label: "盘点单位",
    ...{ class: "stocktake-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_258));
__VLS_260.slots.default;
const __VLS_261 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_262 = __VLS_asFunctionalComponent(__VLS_261, new __VLS_261({
    modelValue: (__VLS_ctx.form.stocktakeUnits),
    multiple: true,
    ...{ class: "stocktake-select" },
}));
const __VLS_263 = __VLS_262({
    modelValue: (__VLS_ctx.form.stocktakeUnits),
    multiple: true,
    ...{ class: "stocktake-select" },
}, ...__VLS_functionalComponentArgsRest(__VLS_262));
__VLS_264.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.stocktakeUnitOptions))) {
    const __VLS_265 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_266 = __VLS_asFunctionalComponent(__VLS_265, new __VLS_265({
        key: (`stocktake-${option}`),
        label: (option),
        value: (option),
    }));
    const __VLS_267 = __VLS_266({
        key: (`stocktake-${option}`),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_266));
}
var __VLS_264;
var __VLS_260;
const __VLS_269 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_270 = __VLS_asFunctionalComponent(__VLS_269, new __VLS_269({
    label: "辅助单位",
}));
const __VLS_271 = __VLS_270({
    label: "辅助单位",
}, ...__VLS_functionalComponentArgsRest(__VLS_270));
__VLS_272.slots.default;
const __VLS_273 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_274 = __VLS_asFunctionalComponent(__VLS_273, new __VLS_273({
    modelValue: (__VLS_ctx.form.assistUnitEnabled),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}));
const __VLS_275 = __VLS_274({
    modelValue: (__VLS_ctx.form.assistUnitEnabled),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}, ...__VLS_functionalComponentArgsRest(__VLS_274));
var __VLS_272;
const __VLS_277 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_278 = __VLS_asFunctionalComponent(__VLS_277, new __VLS_277({
    label: "生产参考成本",
}));
const __VLS_279 = __VLS_278({
    label: "生产参考成本",
}, ...__VLS_functionalComponentArgsRest(__VLS_278));
__VLS_280.slots.default;
const __VLS_281 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_282 = __VLS_asFunctionalComponent(__VLS_281, new __VLS_281({
    modelValue: (__VLS_ctx.form.productionRefCost),
    placeholder: "可填写",
}));
const __VLS_283 = __VLS_282({
    modelValue: (__VLS_ctx.form.productionRefCost),
    placeholder: "可填写",
}, ...__VLS_functionalComponentArgsRest(__VLS_282));
__VLS_284.slots.default;
{
    const { append: __VLS_thisSlot } = __VLS_284.slots;
}
var __VLS_284;
var __VLS_280;
const __VLS_285 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_286 = __VLS_asFunctionalComponent(__VLS_285, new __VLS_285({
    label: "月结后更新",
}));
const __VLS_287 = __VLS_286({
    label: "月结后更新",
}, ...__VLS_functionalComponentArgsRest(__VLS_286));
__VLS_288.slots.default;
const __VLS_289 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_290 = __VLS_asFunctionalComponent(__VLS_289, new __VLS_289({
    modelValue: (__VLS_ctx.form.monthEndUpdate),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}));
const __VLS_291 = __VLS_290({
    modelValue: (__VLS_ctx.form.monthEndUpdate),
    inlinePrompt: true,
    activeText: "开启",
    inactiveText: "关闭",
}, ...__VLS_functionalComponentArgsRest(__VLS_290));
var __VLS_288;
const __VLS_293 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_294 = __VLS_asFunctionalComponent(__VLS_293, new __VLS_293({
    label: "建议采购价格",
}));
const __VLS_295 = __VLS_294({
    label: "建议采购价格",
}, ...__VLS_functionalComponentArgsRest(__VLS_294));
__VLS_296.slots.default;
const __VLS_297 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_298 = __VLS_asFunctionalComponent(__VLS_297, new __VLS_297({
    modelValue: (__VLS_ctx.form.suggestPurchasePrice),
    placeholder: "可填写",
}));
const __VLS_299 = __VLS_298({
    modelValue: (__VLS_ctx.form.suggestPurchasePrice),
    placeholder: "可填写",
}, ...__VLS_functionalComponentArgsRest(__VLS_298));
var __VLS_296;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('storage')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_301 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_302 = __VLS_asFunctionalComponent(__VLS_301, new __VLS_301({
    label: "储存方式",
}));
const __VLS_303 = __VLS_302({
    label: "储存方式",
}, ...__VLS_functionalComponentArgsRest(__VLS_302));
__VLS_304.slots.default;
const __VLS_305 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_306 = __VLS_asFunctionalComponent(__VLS_305, new __VLS_305({
    modelValue: (__VLS_ctx.form.storageMode),
}));
const __VLS_307 = __VLS_306({
    modelValue: (__VLS_ctx.form.storageMode),
}, ...__VLS_functionalComponentArgsRest(__VLS_306));
__VLS_308.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.storageModeOptions))) {
    const __VLS_309 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_310 = __VLS_asFunctionalComponent(__VLS_309, new __VLS_309({
        key: (`storage-${option}`),
        label: (option),
        value: (option),
    }));
    const __VLS_311 = __VLS_310({
        key: (`storage-${option}`),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_310));
}
var __VLS_308;
var __VLS_304;
const __VLS_313 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_314 = __VLS_asFunctionalComponent(__VLS_313, new __VLS_313({
    label: "统计类型",
}));
const __VLS_315 = __VLS_314({
    label: "统计类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_314));
__VLS_316.slots.default;
const __VLS_317 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_318 = __VLS_asFunctionalComponent(__VLS_317, new __VLS_317({
    modelValue: (__VLS_ctx.form.statType),
    placeholder: "请选择统计类型",
}));
const __VLS_319 = __VLS_318({
    modelValue: (__VLS_ctx.form.statType),
    placeholder: "请选择统计类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_318));
__VLS_320.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.statTypeOptions))) {
    const __VLS_321 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_322 = __VLS_asFunctionalComponent(__VLS_321, new __VLS_321({
        key: (`stat-${option}`),
        label: (option),
        value: (option),
    }));
    const __VLS_323 = __VLS_322({
        key: (`stat-${option}`),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_322));
}
var __VLS_320;
var __VLS_316;
const __VLS_325 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_326 = __VLS_asFunctionalComponent(__VLS_325, new __VLS_325({
    label: "库存下限",
}));
const __VLS_327 = __VLS_326({
    label: "库存下限",
}, ...__VLS_functionalComponentArgsRest(__VLS_326));
__VLS_328.slots.default;
const __VLS_329 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_330 = __VLS_asFunctionalComponent(__VLS_329, new __VLS_329({
    modelValue: (__VLS_ctx.form.stockMin),
    placeholder: "请输入库存下限",
}));
const __VLS_331 = __VLS_330({
    modelValue: (__VLS_ctx.form.stockMin),
    placeholder: "请输入库存下限",
}, ...__VLS_functionalComponentArgsRest(__VLS_330));
__VLS_332.slots.default;
{
    const { append: __VLS_thisSlot } = __VLS_332.slots;
    (__VLS_ctx.defaultStockUnitLabel);
}
var __VLS_332;
var __VLS_328;
const __VLS_333 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_334 = __VLS_asFunctionalComponent(__VLS_333, new __VLS_333({
    label: "库存上限",
}));
const __VLS_335 = __VLS_334({
    label: "库存上限",
}, ...__VLS_functionalComponentArgsRest(__VLS_334));
__VLS_336.slots.default;
const __VLS_337 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_338 = __VLS_asFunctionalComponent(__VLS_337, new __VLS_337({
    modelValue: (__VLS_ctx.form.stockMax),
    placeholder: "请输入库存上限",
}));
const __VLS_339 = __VLS_338({
    modelValue: (__VLS_ctx.form.stockMax),
    placeholder: "请输入库存上限",
}, ...__VLS_functionalComponentArgsRest(__VLS_338));
__VLS_340.slots.default;
{
    const { append: __VLS_thisSlot } = __VLS_340.slots;
    (__VLS_ctx.defaultStockUnitLabel);
}
var __VLS_340;
var __VLS_336;
const __VLS_341 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_342 = __VLS_asFunctionalComponent(__VLS_341, new __VLS_341({
    label: "安全库存",
}));
const __VLS_343 = __VLS_342({
    label: "安全库存",
}, ...__VLS_functionalComponentArgsRest(__VLS_342));
__VLS_344.slots.default;
const __VLS_345 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_346 = __VLS_asFunctionalComponent(__VLS_345, new __VLS_345({
    modelValue: (__VLS_ctx.form.safeStock),
    placeholder: "请输入安全库存",
}));
const __VLS_347 = __VLS_346({
    modelValue: (__VLS_ctx.form.safeStock),
    placeholder: "请输入安全库存",
}, ...__VLS_functionalComponentArgsRest(__VLS_346));
__VLS_348.slots.default;
{
    const { append: __VLS_thisSlot } = __VLS_348.slots;
    (__VLS_ctx.defaultStockUnitLabel);
}
var __VLS_348;
var __VLS_344;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('supplier')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_349 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_350 = __VLS_asFunctionalComponent(__VLS_349, new __VLS_349({
    label: "供货关系",
    ...{ class: "supplier-relation-form-item" },
}));
const __VLS_351 = __VLS_350({
    label: "供货关系",
    ...{ class: "supplier-relation-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_350));
__VLS_352.slots.default;
const __VLS_353 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_354 = __VLS_asFunctionalComponent(__VLS_353, new __VLS_353({
    data: (__VLS_ctx.supplierRelationRows),
    border: true,
    stripe: true,
    ...{ class: "supplier-relation-table" },
}));
const __VLS_355 = __VLS_354({
    data: (__VLS_ctx.supplierRelationRows),
    border: true,
    stripe: true,
    ...{ class: "supplier-relation-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_354));
__VLS_356.slots.default;
const __VLS_357 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_358 = __VLS_asFunctionalComponent(__VLS_357, new __VLS_357({
    label: "序号",
    width: "46",
}));
const __VLS_359 = __VLS_358({
    label: "序号",
    width: "46",
}, ...__VLS_functionalComponentArgsRest(__VLS_358));
__VLS_360.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_360.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    ($index + 1);
}
var __VLS_360;
const __VLS_361 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_362 = __VLS_asFunctionalComponent(__VLS_361, new __VLS_361({
    label: "操作",
    width: "70",
}));
const __VLS_363 = __VLS_362({
    label: "操作",
    width: "70",
}, ...__VLS_functionalComponentArgsRest(__VLS_362));
__VLS_364.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_364.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_365 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_366 = __VLS_asFunctionalComponent(__VLS_365, new __VLS_365({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_367 = __VLS_366({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_366));
    let __VLS_369;
    let __VLS_370;
    let __VLS_371;
    const __VLS_372 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addSupplierRelationRow($index);
        }
    };
    __VLS_368.slots.default;
    var __VLS_368;
    const __VLS_373 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_374 = __VLS_asFunctionalComponent(__VLS_373, new __VLS_373({
        ...{ 'onClick': {} },
        text: true,
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.supplierRelationRows.length <= 1),
    }));
    const __VLS_375 = __VLS_374({
        ...{ 'onClick': {} },
        text: true,
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.supplierRelationRows.length <= 1),
    }, ...__VLS_functionalComponentArgsRest(__VLS_374));
    let __VLS_377;
    let __VLS_378;
    let __VLS_379;
    const __VLS_380 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeSupplierRelationRow($index);
        }
    };
    __VLS_376.slots.default;
    var __VLS_376;
}
var __VLS_364;
const __VLS_381 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_382 = __VLS_asFunctionalComponent(__VLS_381, new __VLS_381({
    label: "供应商",
    minWidth: "160",
}));
const __VLS_383 = __VLS_382({
    label: "供应商",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_382));
__VLS_384.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_384.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_385 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_386 = __VLS_asFunctionalComponent(__VLS_385, new __VLS_385({
        modelValue: (row.supplier),
        placeholder: "请输入供应商",
    }));
    const __VLS_387 = __VLS_386({
        modelValue: (row.supplier),
        placeholder: "请输入供应商",
    }, ...__VLS_functionalComponentArgsRest(__VLS_386));
}
var __VLS_384;
const __VLS_389 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_390 = __VLS_asFunctionalComponent(__VLS_389, new __VLS_389({
    label: "联系人",
    minWidth: "120",
}));
const __VLS_391 = __VLS_390({
    label: "联系人",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_390));
__VLS_392.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_392.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_393 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_394 = __VLS_asFunctionalComponent(__VLS_393, new __VLS_393({
        modelValue: (row.contact),
        placeholder: "请输入联系人",
    }));
    const __VLS_395 = __VLS_394({
        modelValue: (row.contact),
        placeholder: "请输入联系人",
    }, ...__VLS_functionalComponentArgsRest(__VLS_394));
}
var __VLS_392;
const __VLS_397 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_398 = __VLS_asFunctionalComponent(__VLS_397, new __VLS_397({
    label: "联系电话",
    minWidth: "140",
}));
const __VLS_399 = __VLS_398({
    label: "联系电话",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_398));
__VLS_400.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_400.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_401 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_402 = __VLS_asFunctionalComponent(__VLS_401, new __VLS_401({
        modelValue: (row.phone),
        placeholder: "请输入联系电话",
    }));
    const __VLS_403 = __VLS_402({
        modelValue: (row.phone),
        placeholder: "请输入联系电话",
    }, ...__VLS_functionalComponentArgsRest(__VLS_402));
}
var __VLS_400;
const __VLS_405 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_406 = __VLS_asFunctionalComponent(__VLS_405, new __VLS_405({
    label: "默认供应商",
    width: "88",
    align: "center",
}));
const __VLS_407 = __VLS_406({
    label: "默认供应商",
    width: "88",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_406));
__VLS_408.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_408.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_409 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_410 = __VLS_asFunctionalComponent(__VLS_409, new __VLS_409({
        modelValue: (__VLS_ctx.defaultSupplierRowKey),
        value: (row.key),
        ...{ class: "supplier-default-radio" },
    }));
    const __VLS_411 = __VLS_410({
        modelValue: (__VLS_ctx.defaultSupplierRowKey),
        value: (row.key),
        ...{ class: "supplier-default-radio" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_410));
}
var __VLS_408;
var __VLS_356;
var __VLS_352;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('business')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_413 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_414 = __VLS_asFunctionalComponent(__VLS_413, new __VLS_413({
    label: "税目编码",
}));
const __VLS_415 = __VLS_414({
    label: "税目编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_414));
__VLS_416.slots.default;
const __VLS_417 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_418 = __VLS_asFunctionalComponent(__VLS_417, new __VLS_417({
    modelValue: (__VLS_ctx.form.taxCode),
    placeholder: "请输入税目编码",
}));
const __VLS_419 = __VLS_418({
    modelValue: (__VLS_ctx.form.taxCode),
    placeholder: "请输入税目编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_418));
var __VLS_416;
const __VLS_421 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_422 = __VLS_asFunctionalComponent(__VLS_421, new __VLS_421({
    label: "税目名称",
}));
const __VLS_423 = __VLS_422({
    label: "税目名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_422));
__VLS_424.slots.default;
const __VLS_425 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_426 = __VLS_asFunctionalComponent(__VLS_425, new __VLS_425({
    modelValue: (__VLS_ctx.form.taxName),
    placeholder: "请输入税目名称",
}));
const __VLS_427 = __VLS_426({
    modelValue: (__VLS_ctx.form.taxName),
    placeholder: "请输入税目名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_426));
var __VLS_424;
const __VLS_429 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_430 = __VLS_asFunctionalComponent(__VLS_429, new __VLS_429({
    label: "税率",
}));
const __VLS_431 = __VLS_430({
    label: "税率",
}, ...__VLS_functionalComponentArgsRest(__VLS_430));
__VLS_432.slots.default;
const __VLS_433 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_434 = __VLS_asFunctionalComponent(__VLS_433, new __VLS_433({
    modelValue: (__VLS_ctx.form.taxRate),
    placeholder: "请输入税率",
}));
const __VLS_435 = __VLS_434({
    modelValue: (__VLS_ctx.form.taxRate),
    placeholder: "请输入税率",
}, ...__VLS_functionalComponentArgsRest(__VLS_434));
var __VLS_432;
const __VLS_437 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_438 = __VLS_asFunctionalComponent(__VLS_437, new __VLS_437({
    label: "税收优惠",
}));
const __VLS_439 = __VLS_438({
    label: "税收优惠",
}, ...__VLS_functionalComponentArgsRest(__VLS_438));
__VLS_440.slots.default;
const __VLS_441 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_442 = __VLS_asFunctionalComponent(__VLS_441, new __VLS_441({
    modelValue: (__VLS_ctx.form.taxBenefit),
    placeholder: "请选择税收优惠",
}));
const __VLS_443 = __VLS_442({
    modelValue: (__VLS_ctx.form.taxBenefit),
    placeholder: "请选择税收优惠",
}, ...__VLS_functionalComponentArgsRest(__VLS_442));
__VLS_444.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.taxBenefitOptions))) {
    const __VLS_445 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_446 = __VLS_asFunctionalComponent(__VLS_445, new __VLS_445({
        key: (`tax-benefit-${option}`),
        label: (option),
        value: (option),
    }));
    const __VLS_447 = __VLS_446({
        key: (`tax-benefit-${option}`),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_446));
}
var __VLS_444;
var __VLS_440;
for (const [field] of __VLS_getVForSourceType((__VLS_ctx.yesNoRadioFields))) {
    const __VLS_449 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_450 = __VLS_asFunctionalComponent(__VLS_449, new __VLS_449({
        key: (field.key),
        label: (field.label),
    }));
    const __VLS_451 = __VLS_450({
        key: (field.key),
        label: (field.label),
    }, ...__VLS_functionalComponentArgsRest(__VLS_450));
    __VLS_452.slots.default;
    const __VLS_453 = {}.ElRadioGroup;
    /** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
    // @ts-ignore
    const __VLS_454 = __VLS_asFunctionalComponent(__VLS_453, new __VLS_453({
        modelValue: (__VLS_ctx.form[field.key]),
    }));
    const __VLS_455 = __VLS_454({
        modelValue: (__VLS_ctx.form[field.key]),
    }, ...__VLS_functionalComponentArgsRest(__VLS_454));
    __VLS_456.slots.default;
    const __VLS_457 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_458 = __VLS_asFunctionalComponent(__VLS_457, new __VLS_457({
        value: "是",
    }));
    const __VLS_459 = __VLS_458({
        value: "是",
    }, ...__VLS_functionalComponentArgsRest(__VLS_458));
    __VLS_460.slots.default;
    var __VLS_460;
    const __VLS_461 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_462 = __VLS_asFunctionalComponent(__VLS_461, new __VLS_461({
        value: "否",
    }));
    const __VLS_463 = __VLS_462({
        value: "否",
    }, ...__VLS_functionalComponentArgsRest(__VLS_462));
    __VLS_464.slots.default;
    var __VLS_464;
    var __VLS_456;
    var __VLS_452;
}
const __VLS_465 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_466 = __VLS_asFunctionalComponent(__VLS_465, new __VLS_465({
    label: "盘点类型",
}));
const __VLS_467 = __VLS_466({
    label: "盘点类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_466));
__VLS_468.slots.default;
const __VLS_469 = {}.ElCheckboxGroup;
/** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
// @ts-ignore
const __VLS_470 = __VLS_asFunctionalComponent(__VLS_469, new __VLS_469({
    modelValue: (__VLS_ctx.form.stocktakeTypes),
}));
const __VLS_471 = __VLS_470({
    modelValue: (__VLS_ctx.form.stocktakeTypes),
}, ...__VLS_functionalComponentArgsRest(__VLS_470));
__VLS_472.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.stocktakeTypeOptions))) {
    const __VLS_473 = {}.ElCheckbox;
    /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
    // @ts-ignore
    const __VLS_474 = __VLS_asFunctionalComponent(__VLS_473, new __VLS_473({
        key: (`check-${option}`),
        value: (option),
    }));
    const __VLS_475 = __VLS_474({
        key: (`check-${option}`),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_474));
    __VLS_476.slots.default;
    (option);
    var __VLS_476;
}
var __VLS_472;
var __VLS_468;
const __VLS_477 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_478 = __VLS_asFunctionalComponent(__VLS_477, new __VLS_477({
    label: "采购收货数量规则",
    ...{ class: "purchase-rule-form-item" },
}));
const __VLS_479 = __VLS_478({
    label: "采购收货数量规则",
    ...{ class: "purchase-rule-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_478));
__VLS_480.slots.default;
const __VLS_481 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_482 = __VLS_asFunctionalComponent(__VLS_481, new __VLS_481({
    modelValue: (__VLS_ctx.form.purchaseReceiptRule),
    ...{ class: "purchase-rule-radio-group" },
}));
const __VLS_483 = __VLS_482({
    modelValue: (__VLS_ctx.form.purchaseReceiptRule),
    ...{ class: "purchase-rule-radio-group" },
}, ...__VLS_functionalComponentArgsRest(__VLS_482));
__VLS_484.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.purchaseReceiptRuleOptions))) {
    const __VLS_485 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_486 = __VLS_asFunctionalComponent(__VLS_485, new __VLS_485({
        key: (`receipt-rule-${option}`),
        value: (option),
    }));
    const __VLS_487 = __VLS_486({
        key: (`receipt-rule-${option}`),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_486));
    __VLS_488.slots.default;
    (option);
    var __VLS_488;
}
var __VLS_484;
if (__VLS_ctx.form.purchaseReceiptRule === '不大于采购数量的一定比例') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "purchase-rule-extra" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    const __VLS_489 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_490 = __VLS_asFunctionalComponent(__VLS_489, new __VLS_489({
        modelValue: (__VLS_ctx.form.purchaseRuleMaxRatio),
        ...{ class: "purchase-rule-input" },
    }));
    const __VLS_491 = __VLS_490({
        modelValue: (__VLS_ctx.form.purchaseRuleMaxRatio),
        ...{ class: "purchase-rule-input" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_490));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
if (__VLS_ctx.form.purchaseReceiptRule === '不小于采购数量的一定比例') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "purchase-rule-extra" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    const __VLS_493 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_494 = __VLS_asFunctionalComponent(__VLS_493, new __VLS_493({
        modelValue: (__VLS_ctx.form.purchaseRuleMinRatio),
        ...{ class: "purchase-rule-input" },
    }));
    const __VLS_495 = __VLS_494({
        modelValue: (__VLS_ctx.form.purchaseRuleMinRatio),
        ...{ class: "purchase-rule-input" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_494));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
if (__VLS_ctx.form.purchaseReceiptRule === '介于采购数量的一定比例') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "purchase-rule-extra" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    const __VLS_497 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_498 = __VLS_asFunctionalComponent(__VLS_497, new __VLS_497({
        modelValue: (__VLS_ctx.form.purchaseRuleBetweenStart),
        ...{ class: "purchase-rule-input" },
    }));
    const __VLS_499 = __VLS_498({
        modelValue: (__VLS_ctx.form.purchaseRuleBetweenStart),
        ...{ class: "purchase-rule-input" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_498));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    const __VLS_501 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_502 = __VLS_asFunctionalComponent(__VLS_501, new __VLS_501({
        modelValue: (__VLS_ctx.form.purchaseRuleBetweenEnd),
        ...{ class: "purchase-rule-input" },
    }));
    const __VLS_503 = __VLS_502({
        modelValue: (__VLS_ctx.form.purchaseRuleBetweenEnd),
        ...{ class: "purchase-rule-input" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_502));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
var __VLS_480;
for (const [field] of __VLS_getVForSourceType((__VLS_ctx.businessSwitchFields))) {
    const __VLS_505 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_506 = __VLS_asFunctionalComponent(__VLS_505, new __VLS_505({
        key: (field.key),
        label: (field.label),
    }));
    const __VLS_507 = __VLS_506({
        key: (field.key),
        label: (field.label),
    }, ...__VLS_functionalComponentArgsRest(__VLS_506));
    __VLS_508.slots.default;
    const __VLS_509 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_510 = __VLS_asFunctionalComponent(__VLS_509, new __VLS_509({
        modelValue: (__VLS_ctx.form[field.key]),
        inlinePrompt: true,
        activeText: "是",
        inactiveText: "否",
    }));
    const __VLS_511 = __VLS_510({
        modelValue: (__VLS_ctx.form[field.key]),
        inlinePrompt: true,
        activeText: "是",
        inactiveText: "否",
    }, ...__VLS_functionalComponentArgsRest(__VLS_510));
    var __VLS_508;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('intro')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_513 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_514 = __VLS_asFunctionalComponent(__VLS_513, new __VLS_513({
    label: "物品图片",
    ...{ class: "item-intro-image-form-item" },
}));
const __VLS_515 = __VLS_514({
    label: "物品图片",
    ...{ class: "item-intro-image-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_514));
__VLS_516.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-intro-image-panel" },
});
const __VLS_517 = {}.ElUpload;
/** @type {[typeof __VLS_components.ElUpload, typeof __VLS_components.elUpload, typeof __VLS_components.ElUpload, typeof __VLS_components.elUpload, ]} */ ;
// @ts-ignore
const __VLS_518 = __VLS_asFunctionalComponent(__VLS_517, new __VLS_517({
    ...{ 'onChange': {} },
    action: "#",
    autoUpload: (false),
    showFileList: (false),
    accept: "image/*",
}));
const __VLS_519 = __VLS_518({
    ...{ 'onChange': {} },
    action: "#",
    autoUpload: (false),
    showFileList: (false),
    accept: "image/*",
}, ...__VLS_functionalComponentArgsRest(__VLS_518));
let __VLS_521;
let __VLS_522;
let __VLS_523;
const __VLS_524 = {
    onChange: (__VLS_ctx.onIntroImageChange)
};
__VLS_520.slots.default;
const __VLS_525 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_526 = __VLS_asFunctionalComponent(__VLS_525, new __VLS_525({
    type: "primary",
    plain: true,
}));
const __VLS_527 = __VLS_526({
    type: "primary",
    plain: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_526));
__VLS_528.slots.default;
var __VLS_528;
var __VLS_520;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "item-intro-image-tip" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-intro-image-list" },
});
for (const [image] of __VLS_getVForSourceType((__VLS_ctx.introImages))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onDragstart: (...[$event]) => {
                __VLS_ctx.onIntroImageDragStart(image.id);
            } },
        ...{ onDragover: () => { } },
        ...{ onDrop: (...[$event]) => {
                __VLS_ctx.onIntroImageDrop(image.id);
            } },
        ...{ onDragend: (__VLS_ctx.onIntroImageDragEnd) },
        key: (image.id),
        ...{ class: "item-intro-image-card" },
        draggable: "true",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.img, __VLS_intrinsicElements.img)({
        src: (image.url),
        alt: (image.name),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-intro-image-card-foot" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (image.name);
    const __VLS_529 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_530 = __VLS_asFunctionalComponent(__VLS_529, new __VLS_529({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "item-intro-image-remove" },
    }));
    const __VLS_531 = __VLS_530({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
        ...{ class: "item-intro-image-remove" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_530));
    let __VLS_533;
    let __VLS_534;
    let __VLS_535;
    const __VLS_536 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeIntroImage(image.id);
        }
    };
    __VLS_532.slots.default;
    var __VLS_532;
}
var __VLS_516;
const __VLS_537 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_538 = __VLS_asFunctionalComponent(__VLS_537, new __VLS_537({
    label: "产品类别",
}));
const __VLS_539 = __VLS_538({
    label: "产品类别",
}, ...__VLS_functionalComponentArgsRest(__VLS_538));
__VLS_540.slots.default;
const __VLS_541 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_542 = __VLS_asFunctionalComponent(__VLS_541, new __VLS_541({
    modelValue: (__VLS_ctx.form.productCategory),
    placeholder: "请输入产品类别",
}));
const __VLS_543 = __VLS_542({
    modelValue: (__VLS_ctx.form.productCategory),
    placeholder: "请输入产品类别",
}, ...__VLS_functionalComponentArgsRest(__VLS_542));
var __VLS_540;
const __VLS_545 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_546 = __VLS_asFunctionalComponent(__VLS_545, new __VLS_545({
    label: "净含量",
}));
const __VLS_547 = __VLS_546({
    label: "净含量",
}, ...__VLS_functionalComponentArgsRest(__VLS_546));
__VLS_548.slots.default;
const __VLS_549 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_550 = __VLS_asFunctionalComponent(__VLS_549, new __VLS_549({
    modelValue: (__VLS_ctx.form.netContent),
    placeholder: "请输入净含量",
}));
const __VLS_551 = __VLS_550({
    modelValue: (__VLS_ctx.form.netContent),
    placeholder: "请输入净含量",
}, ...__VLS_functionalComponentArgsRest(__VLS_550));
var __VLS_548;
const __VLS_553 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_554 = __VLS_asFunctionalComponent(__VLS_553, new __VLS_553({
    label: "配料",
    ...{ class: "item-intro-wide-form-item" },
}));
const __VLS_555 = __VLS_554({
    label: "配料",
    ...{ class: "item-intro-wide-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_554));
__VLS_556.slots.default;
const __VLS_557 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_558 = __VLS_asFunctionalComponent(__VLS_557, new __VLS_557({
    modelValue: (__VLS_ctx.form.ingredients),
    placeholder: "请输入配料",
}));
const __VLS_559 = __VLS_558({
    modelValue: (__VLS_ctx.form.ingredients),
    placeholder: "请输入配料",
}, ...__VLS_functionalComponentArgsRest(__VLS_558));
var __VLS_556;
const __VLS_561 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_562 = __VLS_asFunctionalComponent(__VLS_561, new __VLS_561({
    label: "物品描述",
    ...{ class: "item-intro-wide-form-item" },
}));
const __VLS_563 = __VLS_562({
    label: "物品描述",
    ...{ class: "item-intro-wide-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_562));
__VLS_564.slots.default;
const __VLS_565 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_566 = __VLS_asFunctionalComponent(__VLS_565, new __VLS_565({
    modelValue: (__VLS_ctx.form.itemDescription),
    type: "textarea",
    rows: (3),
    placeholder: "请输入物品描述",
}));
const __VLS_567 = __VLS_566({
    modelValue: (__VLS_ctx.form.itemDescription),
    type: "textarea",
    rows: (3),
    placeholder: "请输入物品描述",
}, ...__VLS_functionalComponentArgsRest(__VLS_566));
var __VLS_564;
const __VLS_569 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_570 = __VLS_asFunctionalComponent(__VLS_569, new __VLS_569({
    label: "备注",
    ...{ class: "item-intro-wide-form-item" },
}));
const __VLS_571 = __VLS_570({
    label: "备注",
    ...{ class: "item-intro-wide-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_570));
__VLS_572.slots.default;
const __VLS_573 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_574 = __VLS_asFunctionalComponent(__VLS_573, new __VLS_573({
    modelValue: (__VLS_ctx.form.remark),
    type: "textarea",
    rows: (3),
    placeholder: "请输入备注",
}));
const __VLS_575 = __VLS_574({
    modelValue: (__VLS_ctx.form.remark),
    type: "textarea",
    rows: (3),
    placeholder: "请输入备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_574));
var __VLS_572;
const __VLS_577 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_578 = __VLS_asFunctionalComponent(__VLS_577, new __VLS_577({
    label: "营养成分表",
    ...{ class: "nutrition-form-item" },
}));
const __VLS_579 = __VLS_578({
    label: "营养成分表",
    ...{ class: "nutrition-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_578));
__VLS_580.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-editor" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-editor-table" },
});
const __VLS_581 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_582 = __VLS_asFunctionalComponent(__VLS_581, new __VLS_581({
    data: (__VLS_ctx.nutritionRows),
    border: true,
    stripe: true,
    ...{ class: "nutrition-edit-table" },
}));
const __VLS_583 = __VLS_582({
    data: (__VLS_ctx.nutritionRows),
    border: true,
    stripe: true,
    ...{ class: "nutrition-edit-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_582));
__VLS_584.slots.default;
const __VLS_585 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_586 = __VLS_asFunctionalComponent(__VLS_585, new __VLS_585({
    label: "序号",
    width: "46",
    align: "center",
}));
const __VLS_587 = __VLS_586({
    label: "序号",
    width: "46",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_586));
__VLS_588.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_588.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    ($index + 1);
}
var __VLS_588;
const __VLS_589 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_590 = __VLS_asFunctionalComponent(__VLS_589, new __VLS_589({
    label: "操作",
    width: "70",
}));
const __VLS_591 = __VLS_590({
    label: "操作",
    width: "70",
}, ...__VLS_functionalComponentArgsRest(__VLS_590));
__VLS_592.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_592.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_593 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_594 = __VLS_asFunctionalComponent(__VLS_593, new __VLS_593({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }));
    const __VLS_595 = __VLS_594({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
        ...{ class: "unit-op-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_594));
    let __VLS_597;
    let __VLS_598;
    let __VLS_599;
    const __VLS_600 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addNutritionRow($index);
        }
    };
    __VLS_596.slots.default;
    var __VLS_596;
    const __VLS_601 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_602 = __VLS_asFunctionalComponent(__VLS_601, new __VLS_601({
        ...{ 'onClick': {} },
        text: true,
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.nutritionRows.length <= 1),
    }));
    const __VLS_603 = __VLS_602({
        ...{ 'onClick': {} },
        text: true,
        ...{ class: "unit-op-btn" },
        disabled: (__VLS_ctx.nutritionRows.length <= 1),
    }, ...__VLS_functionalComponentArgsRest(__VLS_602));
    let __VLS_605;
    let __VLS_606;
    let __VLS_607;
    const __VLS_608 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeNutritionRow($index);
        }
    };
    __VLS_604.slots.default;
    var __VLS_604;
}
var __VLS_592;
const __VLS_609 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_610 = __VLS_asFunctionalComponent(__VLS_609, new __VLS_609({
    minWidth: "140",
}));
const __VLS_611 = __VLS_610({
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_610));
__VLS_612.slots.default;
{
    const { header: __VLS_thisSlot } = __VLS_612.slots;
    const __VLS_613 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_614 = __VLS_asFunctionalComponent(__VLS_613, new __VLS_613({
        modelValue: (__VLS_ctx.nutritionHeaders.item),
        placeholder: "项目",
    }));
    const __VLS_615 = __VLS_614({
        modelValue: (__VLS_ctx.nutritionHeaders.item),
        placeholder: "项目",
    }, ...__VLS_functionalComponentArgsRest(__VLS_614));
}
{
    const { default: __VLS_thisSlot } = __VLS_612.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_617 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_618 = __VLS_asFunctionalComponent(__VLS_617, new __VLS_617({
        modelValue: (row.item),
        placeholder: "请输入项目",
    }));
    const __VLS_619 = __VLS_618({
        modelValue: (row.item),
        placeholder: "请输入项目",
    }, ...__VLS_functionalComponentArgsRest(__VLS_618));
}
var __VLS_612;
const __VLS_621 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_622 = __VLS_asFunctionalComponent(__VLS_621, new __VLS_621({
    minWidth: "140",
}));
const __VLS_623 = __VLS_622({
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_622));
__VLS_624.slots.default;
{
    const { header: __VLS_thisSlot } = __VLS_624.slots;
    const __VLS_625 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_626 = __VLS_asFunctionalComponent(__VLS_625, new __VLS_625({
        modelValue: (__VLS_ctx.nutritionHeaders.per100g),
        placeholder: "每100g",
    }));
    const __VLS_627 = __VLS_626({
        modelValue: (__VLS_ctx.nutritionHeaders.per100g),
        placeholder: "每100g",
    }, ...__VLS_functionalComponentArgsRest(__VLS_626));
}
{
    const { default: __VLS_thisSlot } = __VLS_624.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_629 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_630 = __VLS_asFunctionalComponent(__VLS_629, new __VLS_629({
        modelValue: (row.per100g),
        placeholder: "请输入每100g",
    }));
    const __VLS_631 = __VLS_630({
        modelValue: (row.per100g),
        placeholder: "请输入每100g",
    }, ...__VLS_functionalComponentArgsRest(__VLS_630));
}
var __VLS_624;
const __VLS_633 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_634 = __VLS_asFunctionalComponent(__VLS_633, new __VLS_633({
    minWidth: "120",
}));
const __VLS_635 = __VLS_634({
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_634));
__VLS_636.slots.default;
{
    const { header: __VLS_thisSlot } = __VLS_636.slots;
    const __VLS_637 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_638 = __VLS_asFunctionalComponent(__VLS_637, new __VLS_637({
        modelValue: (__VLS_ctx.nutritionHeaders.nrv),
        placeholder: "NRV%",
    }));
    const __VLS_639 = __VLS_638({
        modelValue: (__VLS_ctx.nutritionHeaders.nrv),
        placeholder: "NRV%",
    }, ...__VLS_functionalComponentArgsRest(__VLS_638));
}
{
    const { default: __VLS_thisSlot } = __VLS_636.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_641 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_642 = __VLS_asFunctionalComponent(__VLS_641, new __VLS_641({
        modelValue: (row.nrv),
        placeholder: "请输入NRV%",
    }));
    const __VLS_643 = __VLS_642({
        modelValue: (row.nrv),
        placeholder: "请输入NRV%",
    }, ...__VLS_functionalComponentArgsRest(__VLS_642));
}
var __VLS_636;
var __VLS_584;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-preview-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-preview-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-preview-body" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-preview-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-preview-table" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nutrition-preview-row nutrition-preview-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.nutritionHeaders.item || '-');
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.nutritionHeaders.per100g || '-');
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.nutritionHeaders.nrv || '-');
for (const [row] of __VLS_getVForSourceType((__VLS_ctx.nutritionRows))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (`preview-${row.id}`),
        ...{ class: "nutrition-preview-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.item || '-');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.per100g || '-');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.nrv || '-');
}
var __VLS_580;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-block" },
    ref: (__VLS_ctx.registerSectionRef('extension')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid" },
});
const __VLS_645 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_646 = __VLS_asFunctionalComponent(__VLS_645, new __VLS_645({
    label: "自定义项",
    ...{ class: "extension-info-form-item" },
}));
const __VLS_647 = __VLS_646({
    label: "自定义项",
    ...{ class: "extension-info-form-item" },
}, ...__VLS_functionalComponentArgsRest(__VLS_646));
__VLS_648.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "extension-info-wrap" },
});
const __VLS_649 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_650 = __VLS_asFunctionalComponent(__VLS_649, new __VLS_649({
    data: (__VLS_ctx.extensionInfoRows),
    border: true,
    stripe: true,
    ...{ class: "extension-info-table" },
}));
const __VLS_651 = __VLS_650({
    data: (__VLS_ctx.extensionInfoRows),
    border: true,
    stripe: true,
    ...{ class: "extension-info-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_650));
__VLS_652.slots.default;
const __VLS_653 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_654 = __VLS_asFunctionalComponent(__VLS_653, new __VLS_653({
    label: "序号",
    width: "46",
    align: "center",
}));
const __VLS_655 = __VLS_654({
    label: "序号",
    width: "46",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_654));
__VLS_656.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_656.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    ($index + 1);
}
var __VLS_656;
const __VLS_657 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_658 = __VLS_asFunctionalComponent(__VLS_657, new __VLS_657({
    label: "名称",
    minWidth: "180",
}));
const __VLS_659 = __VLS_658({
    label: "名称",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_658));
__VLS_660.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_660.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.name);
}
var __VLS_660;
const __VLS_661 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_662 = __VLS_asFunctionalComponent(__VLS_661, new __VLS_661({
    label: "值",
    minWidth: "280",
}));
const __VLS_663 = __VLS_662({
    label: "值",
    minWidth: "280",
}, ...__VLS_functionalComponentArgsRest(__VLS_662));
__VLS_664.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_664.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_665 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_666 = __VLS_asFunctionalComponent(__VLS_665, new __VLS_665({
        modelValue: (row.value),
        placeholder: "请输入",
    }));
    const __VLS_667 = __VLS_666({
        modelValue: (row.value),
        placeholder: "请输入",
    }, ...__VLS_functionalComponentArgsRest(__VLS_666));
}
var __VLS_664;
var __VLS_652;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "extension-info-tip" },
});
var __VLS_648;
var __VLS_13;
const __VLS_669 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_670 = __VLS_asFunctionalComponent(__VLS_669, new __VLS_669({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.cropDialogVisible),
    ...{ class: "item-image-cropper-dialog" },
    title: "裁剪图片",
    width: "560px",
    top: "1vh",
    appendToBody: true,
}));
const __VLS_671 = __VLS_670({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.cropDialogVisible),
    ...{ class: "item-image-cropper-dialog" },
    title: "裁剪图片",
    width: "560px",
    top: "1vh",
    appendToBody: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_670));
let __VLS_673;
let __VLS_674;
let __VLS_675;
const __VLS_676 = {
    onClosed: (__VLS_ctx.closeCropDialog)
};
__VLS_672.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-image-cropper-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-image-cropper-zoom" },
});
const __VLS_677 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_678 = __VLS_asFunctionalComponent(__VLS_677, new __VLS_677({
    ...{ 'onClick': {} },
    text: true,
}));
const __VLS_679 = __VLS_678({
    ...{ 'onClick': {} },
    text: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_678));
let __VLS_681;
let __VLS_682;
let __VLS_683;
const __VLS_684 = {
    onClick: (...[$event]) => {
        __VLS_ctx.zoomCropImage(-10);
    }
};
__VLS_680.slots.default;
var __VLS_680;
const __VLS_685 = {}.ElSlider;
/** @type {[typeof __VLS_components.ElSlider, typeof __VLS_components.elSlider, ]} */ ;
// @ts-ignore
const __VLS_686 = __VLS_asFunctionalComponent(__VLS_685, new __VLS_685({
    ...{ 'onUpdate:modelValue': {} },
    modelValue: (__VLS_ctx.cropZoomLevel),
    min: (__VLS_ctx.CROP_ZOOM_MIN),
    max: (__VLS_ctx.CROP_ZOOM_MAX),
    step: (1),
}));
const __VLS_687 = __VLS_686({
    ...{ 'onUpdate:modelValue': {} },
    modelValue: (__VLS_ctx.cropZoomLevel),
    min: (__VLS_ctx.CROP_ZOOM_MIN),
    max: (__VLS_ctx.CROP_ZOOM_MAX),
    step: (1),
}, ...__VLS_functionalComponentArgsRest(__VLS_686));
let __VLS_689;
let __VLS_690;
let __VLS_691;
const __VLS_692 = {
    'onUpdate:modelValue': (__VLS_ctx.onCropZoomLevelChange)
};
var __VLS_688;
const __VLS_693 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_694 = __VLS_asFunctionalComponent(__VLS_693, new __VLS_693({
    ...{ 'onClick': {} },
    text: true,
}));
const __VLS_695 = __VLS_694({
    ...{ 'onClick': {} },
    text: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_694));
let __VLS_697;
let __VLS_698;
let __VLS_699;
const __VLS_700 = {
    onClick: (...[$event]) => {
        __VLS_ctx.zoomCropImage(10);
    }
};
__VLS_696.slots.default;
var __VLS_696;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(Math.round((__VLS_ctx.cropImageScale / __VLS_ctx.cropBaseScale) * 100));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-image-cropper-frame" },
});
if (__VLS_ctx.cropImageSrc) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.img, __VLS_intrinsicElements.img)({
        ...{ onMousedown: (__VLS_ctx.onCropPointerDown) },
        src: (__VLS_ctx.cropImageSrc),
        ...{ class: "item-image-cropper-image" },
        ...{ style: ({
                width: `${__VLS_ctx.cropDisplayWidth}px`,
                height: `${__VLS_ctx.cropDisplayHeight}px`,
                transform: `translate(${__VLS_ctx.cropOffsetX}px, ${__VLS_ctx.cropOffsetY}px)`,
            }) },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-image-cropper-help" },
});
{
    const { footer: __VLS_thisSlot } = __VLS_672.slots;
    const __VLS_701 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_702 = __VLS_asFunctionalComponent(__VLS_701, new __VLS_701({
        ...{ 'onClick': {} },
    }));
    const __VLS_703 = __VLS_702({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_702));
    let __VLS_705;
    let __VLS_706;
    let __VLS_707;
    const __VLS_708 = {
        onClick: (__VLS_ctx.closeCropDialog)
    };
    __VLS_704.slots.default;
    var __VLS_704;
    const __VLS_709 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_710 = __VLS_asFunctionalComponent(__VLS_709, new __VLS_709({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_711 = __VLS_710({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_710));
    let __VLS_713;
    let __VLS_714;
    let __VLS_715;
    const __VLS_716 = {
        onClick: (__VLS_ctx.confirmCropImage)
    };
    __VLS_712.slots.default;
    var __VLS_712;
}
var __VLS_672;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-setting-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-setting-table']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-base-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-base-select']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-base-tip']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-convert-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-convert-group']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-convert-eq']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-convert-group']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-value-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-value-select']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-value-cell']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-value-select']} */ ;
/** @type {__VLS_StyleScopedClasses['stocktake-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['stocktake-select']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-relation-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-relation-table']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['supplier-default-radio']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-radio-group']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-extra']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-input']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-extra']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-input']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-extra']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-input']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-rule-input']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-tip']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-list']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-card-foot']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-image-remove']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-wide-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-wide-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['item-intro-wide-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-editor']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-editor-table']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-edit-table']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['unit-op-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-card']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-head']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-body']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-title']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-table']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-row']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-header']} */ ;
/** @type {__VLS_StyleScopedClasses['nutrition-preview-row']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['extension-info-form-item']} */ ;
/** @type {__VLS_StyleScopedClasses['extension-info-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['extension-info-table']} */ ;
/** @type {__VLS_StyleScopedClasses['extension-info-tip']} */ ;
/** @type {__VLS_StyleScopedClasses['item-image-cropper-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['item-image-cropper-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['item-image-cropper-zoom']} */ ;
/** @type {__VLS_StyleScopedClasses['item-image-cropper-frame']} */ ;
/** @type {__VLS_StyleScopedClasses['item-image-cropper-image']} */ ;
/** @type {__VLS_StyleScopedClasses['item-image-cropper-help']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            CommonMnemonicField: CommonMnemonicField,
            form: form,
            categoryOptions: categoryOptions,
            abcOptions: abcOptions,
            storageModeOptions: storageModeOptions,
            statTypeOptions: statTypeOptions,
            taxBenefitOptions: taxBenefitOptions,
            stocktakeTypeOptions: stocktakeTypeOptions,
            purchaseReceiptRuleOptions: purchaseReceiptRuleOptions,
            unitOptions: unitOptions,
            volumeUnitOptions: volumeUnitOptions,
            weightUnitOptions: weightUnitOptions,
            unitSettingRows: unitSettingRows,
            unitSettingTip: unitSettingTip,
            baseUnitLabel: baseUnitLabel,
            defaultStockUnitLabel: defaultStockUnitLabel,
            stocktakeUnitOptions: stocktakeUnitOptions,
            defaultUnitOptions: defaultUnitOptions,
            defaultSupplierRowKey: defaultSupplierRowKey,
            supplierRelationRows: supplierRelationRows,
            introImages: introImages,
            nutritionRows: nutritionRows,
            nutritionHeaders: nutritionHeaders,
            extensionInfoRows: extensionInfoRows,
            sectionNavs: sectionNavs,
            defaultUnitFields: defaultUnitFields,
            yesNoRadioFields: yesNoRadioFields,
            businessSwitchFields: businessSwitchFields,
            activeSectionKey: activeSectionKey,
            cropDialogVisible: cropDialogVisible,
            cropImageSrc: cropImageSrc,
            cropImageScale: cropImageScale,
            cropBaseScale: cropBaseScale,
            cropZoomLevel: cropZoomLevel,
            cropOffsetX: cropOffsetX,
            cropOffsetY: cropOffsetY,
            CROP_ZOOM_MIN: CROP_ZOOM_MIN,
            CROP_ZOOM_MAX: CROP_ZOOM_MAX,
            cropDisplayWidth: cropDisplayWidth,
            cropDisplayHeight: cropDisplayHeight,
            addUnitRow: addUnitRow,
            removeUnitRow: removeUnitRow,
            addSupplierRelationRow: addSupplierRelationRow,
            removeSupplierRelationRow: removeSupplierRelationRow,
            onCropZoomLevelChange: onCropZoomLevelChange,
            zoomCropImage: zoomCropImage,
            onIntroImageChange: onIntroImageChange,
            onCropPointerDown: onCropPointerDown,
            closeCropDialog: closeCropDialog,
            confirmCropImage: confirmCropImage,
            removeIntroImage: removeIntroImage,
            addNutritionRow: addNutritionRow,
            removeNutritionRow: removeNutritionRow,
            onIntroImageDragStart: onIntroImageDragStart,
            onIntroImageDrop: onIntroImageDrop,
            onIntroImageDragEnd: onIntroImageDragEnd,
            registerSectionRef: registerSectionRef,
            scrollToSection: scrollToSection,
            isUnitOptionDisabled: isUnitOptionDisabled,
            goBack: goBack,
            handleSaveDraft: handleSaveDraft,
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
