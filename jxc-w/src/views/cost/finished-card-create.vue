<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { ComponentPublicInstance } from 'vue';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonSelectorDialog, {
  type SelectorColumn,
  type SelectorTreeNode,
} from '@/components/CommonSelectorDialog.vue';
import {
  fetchDishCategoryTreeApi,
  fetchDishesApi,
  type DishListRow,
  type DishTreeNode,
} from '@/api/modules/dish';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';

type BaseForm = {
  name: string;
  processPortions: number;
  usageDishType: '全部' | '堂食' | '外卖';
  usageDishSpec: string;
  itemCostTax: number;
  otherCost: number;
  remark: string;
};

type DishRow = {
  spuCode: string;
  dishName: string;
  spec: string;
  category: string;
  dishType: string;
};

type DishCandidate = {
  id: string;
  spuCode: string;
  dishName: string;
  spec: string;
  category: string;
  dishType: string;
  deleted: 'Y' | 'N';
  linkedCostCard: '是' | '否';
};

type MaterialRow = {
  itemCode: string;
  itemName: string;
  specModel: string;
  costUnit: string;
  netAmount: number;
  netRate: number;
  grossAmount: number;
  taxPrice: number;
  taxAmount: number;
  isMainMaterial: boolean;
  assistDeductMode: '是' | '否';
  baseUnit: string;
  baseGrossAmount: number;
  baseTaxPrice: number;
  substituteMaterial: string;
  remark: string;
};

type ItemCandidate = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  type: string;
  costUnit: string;
  baseUnit: string;
  status: string;
};

type SectionNav = {
  key: string;
  label: string;
};

const router = useRouter();
const sessionStore = useSessionStore();

const baseForm = reactive<BaseForm>({
  name: '',
  processPortions: 1,
  usageDishType: '全部',
  usageDishSpec: '',
  itemCostTax: 0,
  otherCost: 0,
  remark: '',
});

const dishRows = ref<DishRow[]>([
  {
    spuCode: '',
    dishName: '',
    spec: '',
    category: '',
    dishType: '',
  },
]);

const materialRows = ref<MaterialRow[]>([
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
const sectionNavs: SectionNav[] = [
  { key: 'basic', label: '基础信息' },
  { key: 'dish', label: '适用菜品' },
  { key: 'material', label: '物料明细' },
];
const activeSectionKey = ref('basic');
const sectionRefs = ref<Record<string, HTMLElement | null>>({});

const dishSelectorVisible = ref(false);
const dishSelectorKeyword = ref('');
const dishSelectorDeleted = ref('');
const activeDishTreeId = ref<string>('all');
const dishSelectorCurrentPage = ref(1);
const dishSelectorPageSize = ref(10);
const dishSelectorLoading = ref(false);
const dishSelectorTotal = ref(0);
const selectingDishRowIndex = ref<number | null>(null);
const selectedDishCandidates = ref<Array<Record<string, unknown>>>([]);
const dishTreeData = ref<SelectorTreeNode[]>([]);

const dishTableColumns: SelectorColumn[] = [
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
const activeItemTreeId = ref<string>('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingMaterialRowIndex = ref<number | null>(null);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);

const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'category', label: '物品类别', minWidth: 120 },
  { prop: 'type', label: '物品类型', minWidth: 100 },
  { prop: 'costUnit', label: '成本单位', minWidth: 100 },
];

const dishCandidateSource = ref<DishCandidate[]>([]);
const itemCandidateSource = ref<ItemCandidate[]>([]);

const resolveOrgId = () => {
  const orgId = (sessionStore.currentOrgId ?? '').trim();
  return orgId || undefined;
};

const normalizeTreeNodes = (nodes: DishTreeNode[]): SelectorTreeNode[] => nodes.map((node) => ({
  id: node.id,
  label: String(node.label ?? ''),
  children: Array.isArray(node.children) ? normalizeTreeNodes(node.children) : undefined,
}));

const normalizeItemTreeNodes = (nodes: ItemCategoryTreeNode[]): SelectorTreeNode[] => nodes.map((node) => ({
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

const mapDishCandidate = (row: DishListRow): DishCandidate => ({
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
      deleted: (dishSelectorDeleted.value || undefined) as 'Y' | 'N' | undefined,
      categoryId: activeDishTreeId.value === 'all' ? undefined : activeDishTreeId.value,
    }, resolveOrgId());
    dishCandidateSource.value = page.list.map(mapDishCandidate);
    dishSelectorTotal.value = Number(page.total ?? 0);
  } finally {
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

const mapItemCandidate = (row: ItemVO): ItemCandidate => ({
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
  } finally {
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

const removeDishRow = (index: number) => {
  if (dishRows.value.length === 1) {
    return;
  }
  dishRows.value.splice(index, 1);
};

const openDishSelector = async (index: number) => {
  selectingDishRowIndex.value = index;
  selectedDishCandidates.value = [];
  if (!dishTreeData.value.length) {
    await loadDishTree();
  }
  await loadDishCandidates();
  dishSelectorVisible.value = true;
};

const handleDishSelectorSearch = (payload: { keyword: string; status: string }) => {
  dishSelectorKeyword.value = payload.keyword;
  dishSelectorDeleted.value = payload.status;
  dishSelectorCurrentPage.value = 1;
  loadDishCandidates();
};

const handleDishNodeChange = (node: SelectorTreeNode | null) => {
  activeDishTreeId.value = String(node?.id ?? 'all');
  dishSelectorCurrentPage.value = 1;
  loadDishCandidates();
};

const handleDishSelectionChange = (rows: Array<Record<string, unknown>>) => {
  selectedDishCandidates.value = rows;
};

const handleDishClear = () => {
  selectedDishCandidates.value = [];
};

const applyDishToRow = (row: DishRow, dish: DishCandidate) => {
  row.spuCode = dish.spuCode;
  row.dishName = dish.dishName;
  row.spec = dish.spec;
  row.category = dish.category;
  row.dishType = dish.dishType;
};

const handleDishSelectorConfirm = (rows: Array<Record<string, unknown>>) => {
  const picked = rows as DishCandidate[];
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

const openItemSelector = async (index: number) => {
  selectingMaterialRowIndex.value = index;
  selectedItemCandidates.value = [];
  if (!itemTreeData.value.length) {
    await loadItemTree();
  }
  await loadItemCandidates();
  itemSelectorVisible.value = true;
};

const handleItemSelectorSearch = (payload: { keyword: string; status: string }) => {
  itemSelectorKeyword.value = payload.keyword;
  itemSelectorStatus.value = payload.status;
  itemSelectorCurrentPage.value = 1;
  loadItemCandidates();
};

const handleItemNodeChange = (node: SelectorTreeNode | null) => {
  activeItemTreeId.value = String(node?.id ?? 'all');
  itemSelectorCurrentPage.value = 1;
  loadItemCandidates();
};

const handleItemSelectionChange = (rows: Array<Record<string, unknown>>) => {
  selectedItemCandidates.value = rows;
};

const handleItemClear = () => {
  selectedItemCandidates.value = [];
};

const applyItemToRow = (row: MaterialRow, item: ItemCandidate) => {
  row.itemCode = item.code;
  row.itemName = item.name;
  row.specModel = item.spec;
  row.costUnit = item.costUnit;
  row.baseUnit = item.baseUnit;
};

const handleItemSelectorConfirm = (rows: Array<Record<string, unknown>>) => {
  const picked = rows as ItemCandidate[];
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

const removeMaterialRow = (index: number) => {
  if (materialRows.value.length === 1) {
    return;
  }
  materialRows.value.splice(index, 1);
};

const backToArchive = () => {
  router.push('/archive/2/1');
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

const scrollToSection = (key: string) => {
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
</script>

<template>
  <div class="item-create-page">
    <section class="panel form-panel">
      <FixedActionBreadcrumb
        :navs="sectionNavs"
        :active-key="activeSectionKey"
        @back="backToArchive"
        @save-draft="saveDraft"
        @save="saveCard"
        @navigate="scrollToSection"
      />

      <el-form label-width="120px" class="item-create-form">
        <section class="form-section-block" :ref="registerSectionRef('basic')">
          <div class="form-section-title">基础信息</div>
          <div class="item-form-grid">
            <el-form-item label="成本卡名称">
              <el-input v-model="baseForm.name" placeholder="请输入成本卡名称" />
            </el-form-item>
            <el-form-item label="加工份数">
              <el-input-number v-model="baseForm.processPortions" :min="1" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="使用菜品类型">
              <el-radio-group v-model="baseForm.usageDishType">
                <el-radio value="全部">全部</el-radio>
                <el-radio value="堂食">堂食</el-radio>
                <el-radio value="外卖">外卖</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="使用菜品规格">
              <el-input v-model="baseForm.usageDishSpec" placeholder="请输入使用菜品规格" />
            </el-form-item>
            <el-form-item label="物品成本（含税）">
              <el-input-number v-model="baseForm.itemCostTax" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="其他成本">
              <el-input-number v-model="baseForm.otherCost" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="合计成本（含税）">
              <el-input :model-value="totalCostTax.toFixed(2)" readonly />
            </el-form-item>
            <el-form-item label="备注" class="item-intro-wide-form-item">
              <el-input v-model="baseForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </div>
        </section>

        <section class="form-section-block" :ref="registerSectionRef('dish')">
          <div class="form-section-title">适用菜品</div>
          <el-table :data="dishRows" border stripe class="erp-table">
            <el-table-column type="index" label="序号" width="56" />
            <el-table-column label="操作" width="72">
              <template #default="{ $index }">
                <el-button class="unit-op-btn" @click="addDishRow">+</el-button>
                <el-button class="unit-op-btn" :disabled="dishRows.length === 1" @click="removeDishRow($index)">-</el-button>
              </template>
            </el-table-column>
            <el-table-column label="菜品SPU编码" min-width="130">
              <template #default="{ row, $index }">
                <div @click="openDishSelector($index)">
                  {{ row.spuCode || '点击选择菜品' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="菜品名称" min-width="120">
              <template #default="{ row }">
                <span>{{ row.dishName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="100">
              <template #default="{ row }">
                <span>{{ row.spec || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="菜品分类" min-width="110">
              <template #default="{ row }">
                <span>{{ row.category || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="菜品类型" min-width="100">
              <template #default="{ row }">
                <span>{{ row.dishType || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="form-section-block" :ref="registerSectionRef('material')">
          <div class="form-section-title">物料明细</div>
          <el-table :data="materialRows" border stripe class="erp-table" :fit="false">
            <el-table-column type="index" label="序号" width="56" fixed="left" />
            <el-table-column label="操作" width="72" fixed="left">
              <template #default="{ $index }">
                <el-button class="unit-op-btn" @click="addMaterialRow">+</el-button>
                <el-button class="unit-op-btn" :disabled="materialRows.length === 1" @click="removeMaterialRow($index)">-</el-button>
              </template>
            </el-table-column>
            <el-table-column label="物品编码" width="120">
              <template #default="{ row, $index }">
                <div @click="openItemSelector($index)">
                  {{ row.itemCode || '点击选择物品' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="物品名称" width="120">
              <template #default="{ row }">
                <span>{{ row.itemName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="规格型号" width="120">
              <template #default="{ row }">
                <span>{{ row.specModel || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成本单位" width="96">
              <template #default="{ row }">
                <span>{{ row.costUnit || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="净料量" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.netAmount" :min="0" :precision="4" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="净料率" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.netRate" :min="0" :max="100" :precision="2" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="毛料量" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.grossAmount" :min="0" :precision="4" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="消耗单价（含税）" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.taxPrice" :min="0" :precision="4" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="消耗金额(含税)" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.taxAmount" :min="0" :precision="2" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="是否主料" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.isMainMaterial" inline-prompt active-text="是" inactive-text="否" />
              </template>
            </el-table-column>
            <el-table-column label="是否辅助单位扣减料" width="136">
              <template #default="{ row }">
                <el-radio-group v-model="row.assistDeductMode">
                  <el-radio value="是">是</el-radio>
                  <el-radio value="否">否</el-radio>
                </el-radio-group>
              </template>
            </el-table-column>
            <el-table-column label="基准单位" width="96">
              <template #default="{ row }">
                <span>{{ row.baseUnit || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="基准单位毛料量" width="126">
              <template #default="{ row }">
                <el-input-number v-model="row.baseGrossAmount" :min="0" :precision="4" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="基准单位消耗单价（含税）" width="160">
              <template #default="{ row }">
                <el-input-number v-model="row.baseTaxPrice" :min="0" :precision="4" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="替代料" width="100">
              <template #default="{ row }">
                <el-input v-model="row.substituteMaterial" />
              </template>
            </el-table-column>
            <el-table-column label="备注" width="140">
              <template #default="{ row }">
                <el-input v-model="row.remark" />
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-form>
    </section>

    <CommonSelectorDialog
      v-model="dishSelectorVisible"
      title="选择菜品"
      :tree-data="dishTreeData"
      :table-data="dishCandidateSource"
      :loading="dishSelectorLoading"
      :columns="dishTableColumns"
      row-key="id"
      selected-label-key="dishName"
      :selected-rows="selectedDishCandidates"
      :keyword-value="dishSelectorKeyword"
      :status-value="dishSelectorDeleted"
      keyword-label="菜品"
      keyword-placeholder="支持按名称和拼音助记码..."
      status-label="菜品是否删除"
      :status-options="[
        { label: '全部', value: '' },
        { label: '否', value: 'N' },
        { label: '是', value: 'Y' },
      ]"
      :total="dishSelectorTotal"
      :current-page="dishSelectorCurrentPage"
      :page-size="dishSelectorPageSize"
      @search="handleDishSelectorSearch"
      @node-change="handleDishNodeChange"
      @selection-change="handleDishSelectionChange"
      @clear-selection="handleDishClear"
      @page-change="(p) => { dishSelectorCurrentPage = p; loadDishCandidates(); }"
      @page-size-change="(s) => { dishSelectorPageSize = s; dishSelectorCurrentPage = 1; loadDishCandidates(); }"
      @confirm="handleDishSelectorConfirm"
    />

    <CommonSelectorDialog
      v-model="itemSelectorVisible"
      title="选择物品"
      :tree-data="itemTreeData"
      :table-data="itemCandidateSource"
      :loading="itemSelectorLoading"
      :columns="itemTableColumns"
      row-key="id"
      selected-label-key="name"
      :selected-rows="selectedItemCandidates"
      :keyword-value="itemSelectorKeyword"
      :status-value="itemSelectorStatus"
      keyword-label="物品"
      keyword-placeholder="支持按物品编码和名称查询..."
      status-label="启用状态"
      :status-options="[
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
      ]"
      :total="itemSelectorTotal"
      :current-page="itemSelectorCurrentPage"
      :page-size="itemSelectorPageSize"
      @search="handleItemSelectorSearch"
      @node-change="handleItemNodeChange"
      @selection-change="handleItemSelectionChange"
      @clear-selection="handleItemClear"
      @page-change="(p) => { itemSelectorCurrentPage = p; loadItemCandidates(); }"
      @page-size-change="(s) => { itemSelectorPageSize = s; itemSelectorCurrentPage = 1; loadItemCandidates(); }"
      @confirm="handleItemSelectorConfirm"
    />
  </div>
</template>
