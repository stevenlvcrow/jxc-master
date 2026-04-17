<script setup lang="ts">
import type { UploadFile } from 'element-plus';
import type { ComponentPublicInstance } from 'vue';
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSessionStore } from '@/stores/session';
import {
  createItemApi,
  fetchItemDetailApi,
  saveItemDraftApi,
  type ItemCreatePayload,
  updateItemApi,
} from '@/api/modules/item';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';

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
  stocktakeTypes: [] as string[],
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
  shelfLifeDays: undefined as number | undefined,
  warningDays: undefined as number | undefined,
  stagnantDays: undefined as number | undefined,
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
type UnitSettingRow = {
  unit: string;
  convertFrom: string;
  convertTo: string;
  volume: string;
  volumeUnit: string;
  weight: string;
  weightUnit: string;
  barcode: string;
};
type SupplierRelationRow = {
  key: number;
  supplier: string;
  contact: string;
  phone: string;
};
type IntroImageItem = {
  id: number;
  name: string;
  url: string;
};
type NutritionRow = {
  id: number;
  item: string;
  per100g: string;
  nrv: string;
};
type ExtensionInfoRow = {
  id: number;
  name: string;
  value: string;
};
const extensionInfoFieldNames = ['致癌物质提示', '使用方法', '生产许可证号', '产品标准号'] as const;
type SectionNav = {
  key: string;
  label: string;
};
type DefaultUnitField = {
  key: 'defaultPurchaseUnit' | 'defaultOrderUnit' | 'defaultStockUnit' | 'defaultCostUnit';
  label: string;
  optionKey: string;
};
type YesNoRadioField = {
  key: 'consumeOnInbound' | 'disableStocktake' | 'defaultNoStocktake';
  label: string;
};
type BusinessSwitchField = {
  key: 'requireAssemblyProcess' | 'requireSplitProcess' | 'requireBatchReport' | 'allowLossReport' | 'allowTransfer' | 'enablePrepare';
  label: string;
};

const unitSettingRows = ref<UnitSettingRow[]>([
  { unit: '斤', convertFrom: '1', convertTo: '', volume: '0.002', volumeUnit: 'dm³', weight: '1.000', weightUnit: '斤', barcode: 'U-0001' },
  { unit: '个', convertFrom: '1', convertTo: '', volume: '0.001', volumeUnit: 'dm³', weight: '0.500', weightUnit: '斤', barcode: 'U-0002' },
  { unit: '袋', convertFrom: '1', convertTo: '', volume: '0.004', volumeUnit: 'dm³', weight: '2.000', weightUnit: '斤', barcode: 'U-0003' },
]);
const unitSettingTip = '1.基准单位一般是最小的【包装】单位；成本单位是最细粒度单位，可能比基准单位小；若生产单位很小，建议基准单位也设小，以防单位换算后生产数据失真。2.单位设置举例：速冻青芒汁（1kg*12瓶/箱），基准单位设置瓶，库存单位设置箱，成本单位设置克，采购单位和订货单位建议与库存单位一致。';
const baseUnitLabel = computed(() => unitSettingRows.value[0]?.unit || '斤');
const defaultStockUnitLabel = computed(() => form.defaultStockUnit || '未设置');
const stocktakeUnitOptions = computed(() => (
  Array.from(
    new Set(
      unitSettingRows.value
        .slice(1)
        .map((row) => row.unit.trim())
        .filter(Boolean),
    ),
  )
));
const defaultUnitOptions = computed(() => stocktakeUnitOptions.value);
const supplierRowSeed = ref(2);
const defaultSupplierRowKey = ref(1);
const supplierRelationRows = ref<SupplierRelationRow[]>([
  { key: 1, supplier: '', contact: '', phone: '' },
]);
const introImages = ref<IntroImageItem[]>([]);
const introImageSeed = ref(1);
const nutritionSeed = ref(3);
const nutritionRows = ref<NutritionRow[]>([
  { id: 1, item: '能量', per100g: '', nrv: '' },
  { id: 2, item: '蛋白质', per100g: '', nrv: '' },
]);
const nutritionHeaders = reactive({
  item: '项目',
  per100g: '每100g',
  nrv: 'NRV%',
});
const buildDefaultExtensionInfoRows = (): ExtensionInfoRow[] => (
  extensionInfoFieldNames.map((name, index) => ({ id: index + 1, name, value: '' }))
);
const extensionInfoRows = ref<ExtensionInfoRow[]>(buildDefaultExtensionInfoRows());
const sectionNavs: SectionNav[] = [
  { key: 'basic', label: '基础信息' },
  { key: 'unit', label: '单位价格' },
  { key: 'storage', label: '存储信息' },
  { key: 'supplier', label: '供货关系' },
  { key: 'business', label: '业务信息' },
  { key: 'intro', label: '物品介绍' },
  { key: 'extension', label: '扩展信息' },
];
const defaultUnitFields: DefaultUnitField[] = [
  { key: 'defaultPurchaseUnit', label: '默认采购单位', optionKey: 'purchase' },
  { key: 'defaultOrderUnit', label: '默认订货单位', optionKey: 'order' },
  { key: 'defaultStockUnit', label: '默认库存单位', optionKey: 'stock' },
  { key: 'defaultCostUnit', label: '默认成本单位', optionKey: 'cost' },
];
const yesNoRadioFields: YesNoRadioField[] = [
  { key: 'consumeOnInbound', label: '入库即耗用' },
  { key: 'disableStocktake', label: '是否禁盘' },
  { key: 'defaultNoStocktake', label: '默认不盘点' },
];
const businessSwitchFields: BusinessSwitchField[] = [
  { key: 'requireAssemblyProcess', label: '是否需组合加工' },
  { key: 'requireSplitProcess', label: '是否需拆分加工' },
  { key: 'requireBatchReport', label: '是否需上传批检报告' },
  { key: 'allowLossReport', label: '是否可报损' },
  { key: 'allowTransfer', label: '是否可调拨' },
  { key: 'enablePrepare', label: '是否制备' },
];
const sectionRefs = ref<Record<string, HTMLElement | null>>({});
const activeSectionKey = ref('basic');
const contentScrollEl = ref<HTMLElement | null>(null);
const draggingIntroImageId = ref<number | null>(null);
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
const cropImageElement = ref<HTMLImageElement | null>(null);
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

const addUnitRow = (index: number) => {
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

const removeUnitRow = (index: number) => {
  if (index === 0 || unitSettingRows.value.length <= 1) {
    return;
  }
  unitSettingRows.value.splice(index, 1);
};

const addSupplierRelationRow = (index: number) => {
  supplierRelationRows.value.splice(index + 1, 0, {
    key: supplierRowSeed.value,
    supplier: '',
    contact: '',
    phone: '',
  });
  supplierRowSeed.value += 1;
};

const removeSupplierRelationRow = (index: number) => {
  if (supplierRelationRows.value.length <= 1) {
    return;
  }
  const removedRow = supplierRelationRows.value[index];
  supplierRelationRows.value.splice(index, 1);
  if (removedRow && removedRow.key === defaultSupplierRowKey.value) {
    defaultSupplierRowKey.value = supplierRelationRows.value[0]?.key ?? 0;
  }
};

const readFileAsDataUrl = (file: File) => (
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('读取图片失败'));
    reader.readAsDataURL(file);
  })
);

const loadImageElement = (src: string) => (
  new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('加载图片失败'));
    image.src = src;
  })
);

const clampCropOffset = (x: number, y: number) => {
  const minX = cropDisplayWidth.value <= CROP_FRAME_SIZE ? 0 : CROP_FRAME_SIZE - cropDisplayWidth.value;
  const maxX = cropDisplayWidth.value <= CROP_FRAME_SIZE ? CROP_FRAME_SIZE - cropDisplayWidth.value : 0;
  const minY = cropDisplayHeight.value <= CROP_FRAME_SIZE ? 0 : CROP_FRAME_SIZE - cropDisplayHeight.value;
  const maxY = cropDisplayHeight.value <= CROP_FRAME_SIZE ? CROP_FRAME_SIZE - cropDisplayHeight.value : 0;
  return {
    x: Math.min(maxX, Math.max(minX, x)),
    y: Math.min(maxY, Math.max(minY, y)),
  };
};

const zoomLevelToScale = (level: number) => cropBaseScale.value * (2 ** (level / 50));

const initCropPosition = () => {
  const baseScale = Math.max(
    CROP_FRAME_SIZE / cropImageNaturalWidth.value,
    CROP_FRAME_SIZE / cropImageNaturalHeight.value,
  );
  cropBaseScale.value = baseScale;
  cropZoomLevel.value = 0;
  cropImageScale.value = baseScale;
  cropOffsetX.value = (CROP_FRAME_SIZE - cropDisplayWidth.value) / 2;
  cropOffsetY.value = (CROP_FRAME_SIZE - cropDisplayHeight.value) / 2;
};

const onCropZoomLevelChange = (nextLevel: number) => {
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

const zoomCropImage = (delta: number) => {
  onCropZoomLevelChange(cropZoomLevel.value + delta);
};

const openCropDialog = async (file: File, fileName: string) => {
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

const onIntroImageChange = async (uploadFile: UploadFile) => {
  const raw = uploadFile.raw;
  if (!raw) {
    return;
  }
  await openCropDialog(raw, uploadFile.name || raw.name || `图片${introImageSeed.value}`);
};

const onCropPointerMove = (event: MouseEvent) => {
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

const onCropPointerDown = (event: MouseEvent) => {
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
  context.drawImage(
    cropImageElement.value,
    sourceX,
    sourceY,
    sourceWidth,
    sourceHeight,
    0,
    0,
    CROP_FRAME_SIZE,
    CROP_FRAME_SIZE,
  );
  introImages.value.push({
    id: introImageSeed.value,
    name: cropImageName.value || `图片${introImageSeed.value}`,
    url: canvas.toDataURL('image/jpeg', 0.92),
  });
  introImageSeed.value += 1;
  closeCropDialog();
};

const removeIntroImage = (id: number) => {
  introImages.value = introImages.value.filter((item) => item.id !== id);
};

const addNutritionRow = (index: number) => {
  nutritionRows.value.splice(index + 1, 0, {
    id: nutritionSeed.value,
    item: '',
    per100g: '',
    nrv: '',
  });
  nutritionSeed.value += 1;
};

const removeNutritionRow = (index: number) => {
  if (nutritionRows.value.length <= 1) {
    return;
  }
  nutritionRows.value.splice(index, 1);
};

const onIntroImageDragStart = (id: number) => {
  draggingIntroImageId.value = id;
};

const onIntroImageDrop = (targetId: number) => {
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

const registerSectionRef = (key: string) => (el: Element | ComponentPublicInstance | null) => {
  if (!el) {
    sectionRefs.value[key] = null;
    return;
  }
  if ('$el' in el) {
    sectionRefs.value[key] = (el.$el as HTMLElement | null);
    return;
  }
  sectionRefs.value[key] = (el as HTMLElement);
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

const scrollToSection = (key: string) => {
  const target = sectionRefs.value[key];
  if (!target) {
    return;
  }
  activeSectionKey.value = key;
  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const isUnitOptionDisabled = (option: string, currentUnit: string) => (
  option !== currentUnit && unitSettingRows.value.some((row) => row.unit === option)
);

watch(stocktakeUnitOptions, (options) => {
  form.stocktakeUnits = form.stocktakeUnits.filter((item) => options.includes(item));
  const normalizeSingle = (value: string) => (options.includes(value) ? value : '');
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

const buildCreatePayload = (): ItemCreatePayload => ({
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

const applyDetailPayload = (payload: ItemCreatePayload) => {
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

  const valueMap = new Map(
    (payload.extensionInfoRows ?? [])
      .filter((row): row is NonNullable<typeof row> => Boolean(row))
      .map((row) => [row.name ?? '', row.value ?? '']),
  );
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
  } finally {
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
    } else {
      await createItemApi(payload, resolveItemOrgId());
      ElMessage.success('新增物品成功');
    }
    router.push('/archive/1/1');
  } finally {
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
  } finally {
    detailLoading.value = false;
  }
};
</script>

<template>
  <div class="item-create-page">
    <section class="panel form-panel">
      <FixedActionBreadcrumb
        :navs="sectionNavs"
        :active-key="activeSectionKey"
        @back="goBack"
        @save-draft="handleSaveDraft"
        @save="handleSave"
        @navigate="scrollToSection"
      />

      <el-form :model="form" label-width="86px" class="item-create-form">
        <div class="form-section-block" :ref="registerSectionRef('basic')">
          <div class="form-section-title">基础信息</div>
          <div class="item-form-grid">
            <el-form-item label="物品名称">
              <el-input v-model="form.name" placeholder="请输入物品名称" />
            </el-form-item>
            <el-form-item label="物品类别">
              <el-select v-model="form.category" placeholder="请选择物品类别">
                <el-option v-for="option in categoryOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="规格型号">
              <el-input v-model="form.spec" placeholder="请输入规格型号" />
            </el-form-item>

            <el-form-item label="物品状态">
              <el-radio-group v-model="form.status">
                <el-radio value="启用">启用</el-radio>
                <el-radio value="停用">停用</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="物品品牌">
              <el-input v-model="form.brand" placeholder="请输入物品品牌" />
            </el-form-item>
            <el-form-item label="助记码">
              <el-input v-model="form.mnemonicCode" placeholder="请输入助记码" />
            </el-form-item>
            <el-form-item label="物品条形码">
              <el-input v-model="form.barcode" placeholder="请输入物品条形码" />
            </el-form-item>
            <el-form-item label="第三方编码">
              <el-input v-model="form.thirdPartyCode" placeholder="请输入第三方编码" />
            </el-form-item>
            <el-form-item label="别名">
              <el-input v-model="form.alias" placeholder="请输入别名" />
            </el-form-item>
            <el-form-item label="ABC 分类">
              <el-select v-model="form.abcClass" placeholder="请选择 ABC 分类">
                <el-option v-for="option in abcOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="批次管理">
              <el-switch
                v-model="form.batchManagement"
                inline-prompt
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item label="保质期">
              <el-switch
                v-model="form.shelfLifeEnabled"
                inline-prompt
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item label="保质期天数">
              <el-input-number
                v-model="form.shelfLifeDays"
                :min="0"
                :disabled="!form.shelfLifeEnabled"
                controls-position="right"
                placeholder="请输入保质期天数"
              />
            </el-form-item>
            <el-form-item label="提前预警天数">
              <el-input-number
                v-model="form.warningDays"
                :min="0"
                :disabled="!form.shelfLifeEnabled"
                controls-position="right"
                placeholder="请输入提前预警天数"
              />
            </el-form-item>
            <el-form-item label="呆滞天数">
              <el-input-number
                v-model="form.stagnantDays"
                :min="0"
                controls-position="right"
                placeholder="请输入呆滞天数"
              />
            </el-form-item>
            <el-form-item label="物品标签">
              <el-input v-model="form.tag" placeholder="请输入物品标签" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('unit')">
          <div class="form-section-title">单位价格</div>
          <div class="item-form-grid">
            <el-form-item label="单位设置" class="unit-setting-form-item">
              <el-table :data="unitSettingRows" border stripe class="unit-setting-table">
                <el-table-column label="序号" width="46">
                  <template #default="{ $index }">
                    {{ $index + 1 }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="70">
                  <template #default="{ $index }">
                    <template v-if="$index > 0">
                      <el-button text type="primary" class="unit-op-btn" @click="addUnitRow($index)">+</el-button>
                      <el-button
                        text
                        class="unit-op-btn"
                        :disabled="unitSettingRows.length <= 1"
                        @click="removeUnitRow($index)"
                      >
                        -
                      </el-button>
                    </template>
                  </template>
                </el-table-column>
                <el-table-column label="单位" width="148">
                  <template #default="{ row, $index }">
                    <div v-if="$index === 0" class="unit-base-cell">
                      <el-input value="基准单位" disabled />
                      <el-select v-model="row.unit" class="unit-base-select">
                        <el-option
                          v-for="option in unitOptions"
                          :key="`setting-${option}`"
                          :label="option"
                          :value="option"
                          :disabled="isUnitOptionDisabled(option, row.unit)"
                        />
                      </el-select>
                    </div>
                    <el-select v-else v-model="row.unit">
                      <el-option
                        v-for="option in unitOptions"
                        :key="`setting-${option}`"
                        :label="option"
                        :value="option"
                        :disabled="isUnitOptionDisabled(option, row.unit)"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="单位换算" width="320">
                  <template #default="{ row, $index }">
                    <div v-if="$index === 0" class="unit-base-tip" :title="unitSettingTip">
                      {{ unitSettingTip }}
                    </div>
                    <div v-else class="unit-convert-cell">
                      <span class="unit-convert-group">
                        <input v-model="row.convertFrom" placeholder="输入数量" maxlength="16" type="text">
                        <span>{{ row.unit || '单位' }}</span>
                      </span>
                      <span class="unit-convert-eq">=</span>
                      <span class="unit-convert-group">
                        <input v-model="row.convertTo" placeholder="输入数量" maxlength="16" type="text">
                        <span>基准单位：{{ baseUnitLabel }}</span>
                      </span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="体积" width="156">
                  <template #default="{ row }">
                    <div class="unit-value-cell">
                      <el-input v-model="row.volume" />
                      <el-select v-model="row.volumeUnit" class="unit-value-select">
                        <el-option
                          v-for="option in volumeUnitOptions"
                          :key="`vol-${option}`"
                          :label="option"
                          :value="option"
                        />
                      </el-select>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="重量" width="168">
                  <template #default="{ row }">
                    <div class="unit-value-cell">
                      <el-input v-model="row.weight" />
                      <el-select v-model="row.weightUnit" class="unit-value-select">
                        <el-option
                          v-for="option in weightUnitOptions"
                          :key="`weight-${option}`"
                          :label="option"
                          :value="option"
                        />
                      </el-select>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="条码" min-width="120">
                  <template #default="{ row }">
                    <el-input v-model="row.barcode" />
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>

            <el-form-item
              v-for="field in defaultUnitFields"
              :key="field.key"
              :label="field.label"
            >
              <el-select v-model="form[field.key]">
                <el-option
                  v-for="option in defaultUnitOptions"
                  :key="`${field.optionKey}-${option}`"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="盘点单位" class="stocktake-form-item">
              <el-select v-model="form.stocktakeUnits" multiple class="stocktake-select">
                <el-option
                  v-for="option in stocktakeUnitOptions"
                  :key="`stocktake-${option}`"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="辅助单位">
              <el-switch
                v-model="form.assistUnitEnabled"
                inline-prompt
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>

            <el-form-item label="生产参考成本">
              <el-input v-model="form.productionRefCost" placeholder="可填写">
                <template #append>元 / 斤</template>
              </el-input>
            </el-form-item>
            <el-form-item label="月结后更新">
              <el-switch
                v-model="form.monthEndUpdate"
                inline-prompt
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item label="建议采购价格">
              <el-input v-model="form.suggestPurchasePrice" placeholder="可填写" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('storage')">
          <div class="form-section-title">储存信息</div>
          <div class="item-form-grid">
            <el-form-item label="储存方式">
              <el-select v-model="form.storageMode">
                <el-option v-for="option in storageModeOptions" :key="`storage-${option}`" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="统计类型">
              <el-select v-model="form.statType" placeholder="请选择统计类型">
                <el-option v-for="option in statTypeOptions" :key="`stat-${option}`" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="库存下限">
              <el-input v-model="form.stockMin" placeholder="请输入库存下限">
                <template #append>{{ defaultStockUnitLabel }}</template>
              </el-input>
            </el-form-item>
            <el-form-item label="库存上限">
              <el-input v-model="form.stockMax" placeholder="请输入库存上限">
                <template #append>{{ defaultStockUnitLabel }}</template>
              </el-input>
            </el-form-item>
            <el-form-item label="安全库存">
              <el-input v-model="form.safeStock" placeholder="请输入安全库存">
                <template #append>{{ defaultStockUnitLabel }}</template>
              </el-input>
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('supplier')">
          <div class="form-section-title">供货关系</div>
          <div class="item-form-grid">
            <el-form-item label="供货关系" class="supplier-relation-form-item">
              <el-table :data="supplierRelationRows" border stripe class="supplier-relation-table">
                <el-table-column label="序号" width="46">
                  <template #default="{ $index }">
                    {{ $index + 1 }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="70">
                  <template #default="{ $index }">
                    <el-button text type="primary" class="unit-op-btn" @click="addSupplierRelationRow($index)">+</el-button>
                    <el-button
                      text
                      class="unit-op-btn"
                      :disabled="supplierRelationRows.length <= 1"
                      @click="removeSupplierRelationRow($index)"
                    >
                      -
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="供应商" min-width="160">
                  <template #default="{ row }">
                    <el-input v-model="row.supplier" placeholder="请输入供应商" />
                  </template>
                </el-table-column>
                <el-table-column label="联系人" min-width="120">
                  <template #default="{ row }">
                    <el-input v-model="row.contact" placeholder="请输入联系人" />
                  </template>
                </el-table-column>
                <el-table-column label="联系电话" min-width="140">
                  <template #default="{ row }">
                    <el-input v-model="row.phone" placeholder="请输入联系电话" />
                  </template>
                </el-table-column>
                <el-table-column label="默认供应商" width="88" align="center">
                  <template #default="{ row }">
                    <el-radio v-model="defaultSupplierRowKey" :value="row.key" class="supplier-default-radio" />
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('business')">
          <div class="form-section-title">业务信息</div>
          <div class="item-form-grid">
            <el-form-item label="税目编码">
              <el-input v-model="form.taxCode" placeholder="请输入税目编码" />
            </el-form-item>
            <el-form-item label="税目名称">
              <el-input v-model="form.taxName" placeholder="请输入税目名称" />
            </el-form-item>
            <el-form-item label="税率">
              <el-input v-model="form.taxRate" placeholder="请输入税率" />
            </el-form-item>
            <el-form-item label="税收优惠">
              <el-select v-model="form.taxBenefit" placeholder="请选择税收优惠">
                <el-option v-for="option in taxBenefitOptions" :key="`tax-benefit-${option}`" :label="option" :value="option" />
              </el-select>
            </el-form-item>

            <el-form-item
              v-for="field in yesNoRadioFields"
              :key="field.key"
              :label="field.label"
            >
              <el-radio-group v-model="form[field.key]">
                <el-radio value="是">是</el-radio>
                <el-radio value="否">否</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="盘点类型">
              <el-checkbox-group v-model="form.stocktakeTypes">
                <el-checkbox v-for="option in stocktakeTypeOptions" :key="`check-${option}`" :value="option">
                  {{ option }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="采购收货数量规则" class="purchase-rule-form-item">
              <el-radio-group v-model="form.purchaseReceiptRule" class="purchase-rule-radio-group">
                <el-radio
                  v-for="option in purchaseReceiptRuleOptions"
                  :key="`receipt-rule-${option}`"
                  :value="option"
                >
                  {{ option }}
                </el-radio>
              </el-radio-group>
              <div
                v-if="form.purchaseReceiptRule === '不大于采购数量的一定比例'"
                class="purchase-rule-extra"
              >
                <span>不大于：</span>
                <el-input v-model="form.purchaseRuleMaxRatio" class="purchase-rule-input" />
                <span>%</span>
              </div>
              <div
                v-if="form.purchaseReceiptRule === '不小于采购数量的一定比例'"
                class="purchase-rule-extra"
              >
                <span>不小于：</span>
                <el-input v-model="form.purchaseRuleMinRatio" class="purchase-rule-input" />
                <span>%</span>
              </div>
              <div
                v-if="form.purchaseReceiptRule === '介于采购数量的一定比例'"
                class="purchase-rule-extra"
              >
                <span>介于</span>
                <el-input v-model="form.purchaseRuleBetweenStart" class="purchase-rule-input" />
                <span>~</span>
                <el-input v-model="form.purchaseRuleBetweenEnd" class="purchase-rule-input" />
                <span>%</span>
              </div>
            </el-form-item>

            <el-form-item
              v-for="field in businessSwitchFields"
              :key="field.key"
              :label="field.label"
            >
              <el-switch
                v-model="form[field.key]"
                inline-prompt
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('intro')">
          <div class="form-section-title">物品介绍</div>
          <div class="item-form-grid">
            <el-form-item label="物品图片" class="item-intro-image-form-item">
              <div class="item-intro-image-panel">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/*"
                  @change="onIntroImageChange"
                >
                  <el-button type="primary" plain>选择图片</el-button>
                </el-upload>
                <span class="item-intro-image-tip">支持多图上传，拖拽图片可调整顺序，裁剪输出 480 × 480</span>
                <div class="item-intro-image-list">
                  <div
                    v-for="image in introImages"
                    :key="image.id"
                    class="item-intro-image-card"
                    draggable="true"
                    @dragstart="onIntroImageDragStart(image.id)"
                    @dragover.prevent
                    @drop="onIntroImageDrop(image.id)"
                    @dragend="onIntroImageDragEnd"
                  >
                    <img :src="image.url" :alt="image.name">
                    <div class="item-intro-image-card-foot">
                      <span>{{ image.name }}</span>
                      <el-button text type="danger" class="item-intro-image-remove" @click="removeIntroImage(image.id)">
                        删除
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="产品类别">
              <el-input v-model="form.productCategory" placeholder="请输入产品类别" />
            </el-form-item>
            <el-form-item label="净含量">
              <el-input v-model="form.netContent" placeholder="请输入净含量" />
            </el-form-item>
            <el-form-item label="配料" class="item-intro-wide-form-item">
              <el-input v-model="form.ingredients" placeholder="请输入配料" />
            </el-form-item>
            <el-form-item label="物品描述" class="item-intro-wide-form-item">
              <el-input v-model="form.itemDescription" type="textarea" :rows="3" placeholder="请输入物品描述" />
            </el-form-item>
            <el-form-item label="备注" class="item-intro-wide-form-item">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
            <el-form-item label="营养成分表" class="nutrition-form-item">
              <div class="nutrition-editor">
                <div class="nutrition-editor-table">
                  <el-table :data="nutritionRows" border stripe class="nutrition-edit-table">
                    <el-table-column label="序号" width="46" align="center">
                      <template #default="{ $index }">
                        {{ $index + 1 }}
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="70">
                      <template #default="{ $index }">
                        <el-button text type="primary" class="unit-op-btn" @click="addNutritionRow($index)">+</el-button>
                        <el-button
                          text
                          class="unit-op-btn"
                          :disabled="nutritionRows.length <= 1"
                          @click="removeNutritionRow($index)"
                        >
                          -
                        </el-button>
                      </template>
                    </el-table-column>
                    <el-table-column min-width="140">
                      <template #header>
                        <el-input v-model="nutritionHeaders.item" placeholder="项目" />
                      </template>
                      <template #default="{ row }">
                        <el-input v-model="row.item" placeholder="请输入项目" />
                      </template>
                    </el-table-column>
                    <el-table-column min-width="140">
                      <template #header>
                        <el-input v-model="nutritionHeaders.per100g" placeholder="每100g" />
                      </template>
                      <template #default="{ row }">
                        <el-input v-model="row.per100g" placeholder="请输入每100g" />
                      </template>
                    </el-table-column>
                    <el-table-column min-width="120">
                      <template #header>
                        <el-input v-model="nutritionHeaders.nrv" placeholder="NRV%" />
                      </template>
                      <template #default="{ row }">
                        <el-input v-model="row.nrv" placeholder="请输入NRV%" />
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
                <div class="nutrition-preview-card">
                  <div class="nutrition-preview-head">预览</div>
                  <div class="nutrition-preview-body">
                    <div class="nutrition-preview-title">营养成分表</div>
                    <div class="nutrition-preview-table">
                      <div class="nutrition-preview-row nutrition-preview-header">
                        <span>{{ nutritionHeaders.item || '-' }}</span>
                        <span>{{ nutritionHeaders.per100g || '-' }}</span>
                        <span>{{ nutritionHeaders.nrv || '-' }}</span>
                      </div>
                      <div
                        v-for="row in nutritionRows"
                        :key="`preview-${row.id}`"
                        class="nutrition-preview-row"
                      >
                        <span>{{ row.item || '-' }}</span>
                        <span>{{ row.per100g || '-' }}</span>
                        <span>{{ row.nrv || '-' }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('extension')">
          <div class="form-section-title">扩展信息</div>
          <div class="item-form-grid">
            <el-form-item label="自定义项" class="extension-info-form-item">
              <div class="extension-info-wrap">
                <el-table :data="extensionInfoRows" border stripe class="extension-info-table">
                  <el-table-column label="序号" width="46" align="center">
                    <template #default="{ $index }">
                      {{ $index + 1 }}
                    </template>
                  </el-table-column>
                  <el-table-column label="名称" min-width="180">
                    <template #default="{ row }">
                      <span>{{ row.name }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="值" min-width="280">
                    <template #default="{ row }">
                      <el-input v-model="row.value" placeholder="请输入" />
                    </template>
                  </el-table-column>
                </el-table>
                <div class="extension-info-tip">生产许可证号、产品标准号用于加工品生产标签打印；</div>
              </div>
            </el-form-item>
          </div>
        </div>

      </el-form>
    </section>

    <el-dialog
      v-model="cropDialogVisible"
      class="item-image-cropper-dialog"
      title="裁剪图片"
      width="560px"
      top="1vh"
      append-to-body
      @closed="closeCropDialog"
    >
      <div class="item-image-cropper-wrap">
        <div class="item-image-cropper-zoom">
          <el-button text @click="zoomCropImage(-10)">-</el-button>
          <el-slider
            :model-value="cropZoomLevel"
            :min="CROP_ZOOM_MIN"
            :max="CROP_ZOOM_MAX"
            :step="1"
            @update:model-value="onCropZoomLevelChange"
          />
          <el-button text @click="zoomCropImage(10)">+</el-button>
          <span>{{ Math.round((cropImageScale / cropBaseScale) * 100) }}%</span>
        </div>
        <div class="item-image-cropper-frame">
          <img
            v-if="cropImageSrc"
            :src="cropImageSrc"
            class="item-image-cropper-image"
            :style="{
              width: `${cropDisplayWidth}px`,
              height: `${cropDisplayHeight}px`,
              transform: `translate(${cropOffsetX}px, ${cropOffsetY}px)`,
            }"
            @mousedown.prevent="onCropPointerDown"
          >
        </div>
        <div class="item-image-cropper-help">拖拽图片调整位置，裁剪后固定为 480 × 480</div>
      </div>
      <template #footer>
        <el-button @click="closeCropDialog">取消</el-button>
        <el-button type="primary" @click="confirmCropImage">确认裁剪</el-button>
      </template>
    </el-dialog>
  </div>
</template>
