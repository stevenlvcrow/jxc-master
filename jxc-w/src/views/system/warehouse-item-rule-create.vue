<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue';
import { reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import CommonSelectorDialog, {
  type SelectorColumn,
  type SelectorTreeNode,
} from '@/components/CommonSelectorDialog.vue';
import { useSessionStore } from '@/stores/session';
import { fetchWarehousesApi, type WarehouseRow as ApiWarehouseRow } from '@/api/modules/warehouse';
import { createItemRuleApi, type RuleCreatePayload } from '@/api/modules/warehouse-item-rule';
import {
  fetchItemCategoryTreeApi,
  fetchItemCategoriesApi,
  type ItemCategoryVO,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';

type SectionKey = 'basic' | 'items' | 'warehouse';
type ItemTabKey = 'item' | 'category';

type ItemRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
};

type CategoryRow = {
  id: string;
  categoryCode: string;
  categoryName: string;
  parentCategory: string;
  childCategory: string;
};

type WarehouseRow = {
  id: string;
  warehouseId: number | null;
};

const router = useRouter();
const sessionStore = useSessionStore();
const formRef = ref<FormInstance>();
const activeSection = ref<SectionKey>('basic');
const itemTab = ref<ItemTabKey>('item');
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
  businessScopes: [] as string[],
  creator: sessionStore.userName,
  createdAt: '-',
  updatedAt: '-',
});

const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
};

const sectionRefs = ref<Record<SectionKey, HTMLElement | null>>({
  basic: null,
  items: null,
  warehouse: null,
});

const itemRows = ref<ItemRow[]>([]);
const categoryRows = ref<CategoryRow[]>([]);
const warehouseRows = ref<WarehouseRow[]>([]);
const warehouseOptions = ref<ApiWarehouseRow[]>([]);
const itemSelectorVisible = ref(false);
const itemSelectorKeyword = ref('');
const itemSelectorStatus = ref('');
const activeItemTreeId = ref<string>('all');
const itemSelectorCurrentPage = ref(1);
const itemSelectorPageSize = ref(10);
const itemSelectorLoading = ref(false);
const itemSelectorTotal = ref(0);
const selectingItemRowIndex = ref<number | null>(null);
const selectedItemCandidates = ref<Array<Record<string, unknown>>>([]);
const itemTreeData = ref<SelectorTreeNode[]>([]);
const itemCandidateSource = ref<ItemCodeSuggestion[]>([]);
const categorySelectorVisible = ref(false);
const categorySelectorKeyword = ref('');
const categorySelectorStatus = ref('');
const activeCategoryTreeId = ref<string>('all');
const categorySelectorCurrentPage = ref(1);
const categorySelectorPageSize = ref(10);
const categorySelectorLoading = ref(false);
const categorySelectorTotal = ref(0);
const selectingCategoryRowIndex = ref<number | null>(null);
const selectedCategoryCandidates = ref<Array<Record<string, unknown>>>([]);
const categoryTreeData = ref<SelectorTreeNode[]>([]);
const categoryCandidateSource = ref<Array<{
  id: string;
  categoryCode: string;
  categoryName: string;
  parentCategory: string;
  status: string;
}>>([]);

const itemTableColumns: SelectorColumn[] = [
  { prop: 'code', label: '物品编码', minWidth: 130 },
  { prop: 'name', label: '物品名称', minWidth: 130 },
  { prop: 'spec', label: '规格型号', minWidth: 130 },
  { prop: 'category', label: '物品类别', minWidth: 130 },
  { prop: 'type', label: '物品类型', minWidth: 120 },
];

const categoryTableColumns: SelectorColumn[] = [
  { prop: 'categoryCode', label: '分类编码', minWidth: 130 },
  { prop: 'categoryName', label: '分类名称', minWidth: 130 },
  { prop: 'parentCategory', label: '上级分类', minWidth: 130 },
  { prop: 'status', label: '状态', minWidth: 90 },
];

let seed = 0;
const nextId = () => `row-${++seed}`;

const resolveGroupId = (): number | null => {
  const orgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!orgId) return null;
  const raw = orgId.includes('-') ? orgId.slice(orgId.lastIndexOf('-') + 1) : orgId;
  const id = Number(raw);
  return Number.isNaN(id) ? null : id;
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

const registerSectionRef = (key: SectionKey) => (el: Element | ComponentPublicInstance | null) => {
  if (!el) {
    sectionRefs.value[key] = null;
    return;
  }
  if ('$el' in el) {
    sectionRefs.value[key] = (el.$el as HTMLElement | null) ?? null;
    return;
  }
  sectionRefs.value[key] = el as HTMLElement;
};

const scrollToSection = (key: string) => {
  const sectionKey = key as SectionKey;
  activeSection.value = sectionKey;
  sectionRefs.value[sectionKey]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const addItemRow = (index?: number) => {
  const target = typeof index === 'number' ? index + 1 : itemRows.value.length;
  itemRows.value.splice(target, 0, {
    id: nextId(),
    itemCode: '',
    itemName: '',
    specModel: '',
    itemCategory: '',
  });
};

const removeItemRow = (index: number) => {
  if (itemRows.value.length <= 1) {
    ElMessage.warning('至少保留一条物品数据');
    return;
  }
  itemRows.value.splice(index, 1);
};

const addCategoryRow = (index?: number) => {
  const target = typeof index === 'number' ? index + 1 : categoryRows.value.length;
  categoryRows.value.splice(target, 0, {
    id: nextId(),
    categoryCode: '',
    categoryName: '',
    parentCategory: '',
    childCategory: '',
  });
};

const removeCategoryRow = (index: number) => {
  if (categoryRows.value.length <= 1) {
    ElMessage.warning('至少保留一条分类数据');
    return;
  }
  categoryRows.value.splice(index, 1);
};

const addWarehouseRow = (index?: number) => {
  const target = typeof index === 'number' ? index + 1 : warehouseRows.value.length;
  warehouseRows.value.splice(target, 0, {
    id: nextId(),
    warehouseId: null,
  });
};

const removeWarehouseRow = (index: number) => {
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
  const groupId = resolveGroupId();
  if (!groupId) {
    warehouseOptions.value = [];
    return;
  }
  warehouseOptions.value = await fetchWarehousesApi(groupId);
};

type ItemCodeSuggestion = {
  id: string;
  code: string;
  name: string;
  spec: string;
  category: string;
  type: string;
};

const normalizeItemTreeNodes = (nodes: ItemCategoryTreeNode[]): SelectorTreeNode[] => nodes.map((node) => ({
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
    const page = await fetchItemsApi(
      {
        pageNo: itemSelectorCurrentPage.value,
        pageSize: itemSelectorPageSize.value,
        keyword: itemSelectorKeyword.value.trim() || undefined,
        category: activeItemTreeId.value === 'all' ? undefined : activeItemTreeId.value,
        status: itemSelectorStatus.value || undefined,
      },
      orgId,
    );
    itemCandidateSource.value = (page.list ?? []).map(mapItemSuggestion);
    itemSelectorTotal.value = Number(page.total ?? 0);
  } finally {
    itemSelectorLoading.value = false;
  }
};

const mapItemSuggestion = (item: ItemVO): ItemCodeSuggestion => ({
  id: item.id,
  code: item.code,
  name: item.name,
  spec: item.spec,
  category: item.category,
  type: item.type,
});

const mapCategoryCandidate = (item: ItemCategoryVO): {
  id: string;
  categoryCode: string;
  categoryName: string;
  parentCategory: string;
  status: string;
} => ({
  id: String(item.id),
  categoryCode: item.categoryCode,
  categoryName: item.categoryName,
  parentCategory: item.parentCategory,
  status: item.status,
});

const applySelectedItem = (row: ItemRow, item: ItemVO) => {
  row.itemCode = item.code || '';
  row.itemName = item.name || '';
  row.specModel = item.spec || '';
  row.itemCategory = item.category || item.type || '';
};

const openItemSelector = async (index: number) => {
  selectingItemRowIndex.value = index;
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

const handleItemSelectorConfirm = (rows: Array<Record<string, unknown>>) => {
  const picked = rows as ItemCodeSuggestion[];
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
    const page = await fetchItemCategoriesApi(
      {
        pageNo: categorySelectorCurrentPage.value,
        pageSize: categorySelectorPageSize.value,
        categoryInfo: categorySelectorKeyword.value.trim() || undefined,
        status: (categorySelectorStatus.value || undefined) as '启用' | '停用' | undefined,
        treeNode: activeCategoryTreeId.value === 'all' ? undefined : activeCategoryTreeId.value,
      },
      orgId,
    );
    categoryCandidateSource.value = (page.list ?? []).map(mapCategoryCandidate);
    categorySelectorTotal.value = Number(page.total ?? 0);
  } finally {
    categorySelectorLoading.value = false;
  }
};

const openCategorySelector = async (index: number) => {
  selectingCategoryRowIndex.value = index;
  selectedCategoryCandidates.value = [];
  if (!categoryTreeData.value.length) {
    await loadCategoryTree();
  }
  await loadCategoryCandidates();
  categorySelectorVisible.value = true;
};

const handleCategorySelectorSearch = (payload: { keyword: string; status: string }) => {
  categorySelectorKeyword.value = payload.keyword;
  categorySelectorStatus.value = payload.status;
  categorySelectorCurrentPage.value = 1;
  loadCategoryCandidates();
};

const handleCategoryNodeChange = (node: SelectorTreeNode | null) => {
  activeCategoryTreeId.value = String(node?.id ?? 'all');
  categorySelectorCurrentPage.value = 1;
  loadCategoryCandidates();
};

const handleCategorySelectionChange = (rows: Array<Record<string, unknown>>) => {
  selectedCategoryCandidates.value = rows;
};

const handleCategoryClear = () => {
  selectedCategoryCandidates.value = [];
};

const handleCategorySelectorConfirm = (rows: Array<Record<string, unknown>>) => {
  const picked = rows as Array<{
    id: string;
    categoryCode: string;
    categoryName: string;
    parentCategory: string;
    status: string;
  }>;
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

const buildCreatePayload = (): RuleCreatePayload => {
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
      .map((r) => ({ warehouseId: r.warehouseId! })),
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
  } finally {
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
</script>

<template>
  <div class="item-create-page warehouse-item-rule-create-page">
    <FixedActionBreadcrumb
      :navs="navs"
      :active-key="activeSection"
      @back="handleBack"
      @save-draft="handleSaveDraft"
      @save="handleSave"
      @navigate="scrollToSection"
    />

    <section class="panel form-panel" v-loading="saving">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px" class="item-create-form">
        <div class="form-section-block" :ref="registerSectionRef('basic')">
          <div class="form-section-title">基础信息</div>
          <div class="item-form-grid basic-grid">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
            </el-form-item>
            <el-form-item label="是否启用业务管控">
              <el-switch v-model="form.businessControl" inline-prompt active-text="开" inactive-text="关" />
            </el-form-item>

            <el-form-item v-if="form.businessControl" label="业务管控范围" class="full-span">
              <el-checkbox-group v-model="form.businessScopes" class="scope-group">
                <el-checkbox v-for="scope in businessScopeOptions" :key="scope" :value="scope">
                  {{ scope }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="创建人">
              <el-input :model-value="form.creator" disabled />
            </el-form-item>
            <el-form-item label="创建时间">
              <el-input :model-value="form.createdAt" disabled />
            </el-form-item>
            <el-form-item label="最后修改时间">
              <el-input :model-value="form.updatedAt" disabled />
            </el-form-item>
          </div>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('items')">
          <div class="form-section-title">适用物品</div>
          <el-tabs v-model="itemTab" class="rule-item-tabs">
            <el-tab-pane label="物品" name="item">
              <el-table :data="itemRows" border stripe class="erp-table" :fit="false">
                <el-table-column type="index" label="序号" width="56" fixed="left" />
                <el-table-column label="操作" width="80" fixed="left">
                  <template #default="{ $index }">
                    <el-button text type="primary" @click="addItemRow($index)">+</el-button>
                    <el-button text @click="removeItemRow($index)">-</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="物品编码" min-width="130">
                  <template #default="{ row, $index }">
                    <div @click="openItemSelector($index)">
                      {{ row.itemCode || '点击选择物品' }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="物品名称" min-width="130">
                  <template #default="{ row }">{{ row.itemName || '-' }}</template>
                </el-table-column>
                <el-table-column label="规格型号" min-width="130">
                  <template #default="{ row }">{{ row.specModel || '-' }}</template>
                </el-table-column>
                <el-table-column label="物品类别" min-width="130">
                  <template #default="{ row }">{{ row.itemCategory || '-' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="分类" name="category">
              <el-table :data="categoryRows" border stripe class="erp-table" :fit="false">
                <el-table-column type="index" label="序号" width="56" fixed="left" />
                <el-table-column label="操作" width="80" fixed="left">
                  <template #default="{ $index }">
                    <el-button text type="primary" @click="addCategoryRow($index)">+</el-button>
                    <el-button text @click="removeCategoryRow($index)">-</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="分类编码" min-width="130">
                  <template #default="{ row, $index }">
                    <div @click="openCategorySelector($index)">
                      {{ row.categoryCode || '点击选择分类' }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="分类名称" min-width="130">
                  <template #default="{ row }"><el-input v-model="row.categoryName" /></template>
                </el-table-column>
                <el-table-column label="上级分类" min-width="130">
                  <template #default="{ row }"><el-input v-model="row.parentCategory" /></template>
                </el-table-column>
                <el-table-column label="下级分类" min-width="130">
                  <template #default="{ row }"><el-input v-model="row.childCategory" /></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="form-section-block" :ref="registerSectionRef('warehouse')">
          <div class="form-section-title">适用仓库</div>
          <el-table :data="warehouseRows" border stripe class="erp-table" :fit="false">
            <el-table-column type="index" label="序号" width="56" fixed="left" />
            <el-table-column label="操作" width="80" fixed="left">
              <template #default="{ $index }">
                <el-button text type="primary" @click="addWarehouseRow($index)">+</el-button>
                <el-button text @click="removeWarehouseRow($index)">-</el-button>
              </template>
            </el-table-column>
            <el-table-column label="仓库/档口" min-width="220">
              <template #default="{ row }">
                <el-select v-model="row.warehouseId" clearable filterable placeholder="请选择仓库/档口" style="width:100%">
                  <el-option
                    v-for="option in warehouseOptions"
                    :key="option.id"
                    :label="`${option.warehouseName} (${option.warehouseCode})`"
                    :value="option.id"
                  />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>
    </section>

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

    <CommonSelectorDialog
      v-model="categorySelectorVisible"
      title="选择分类"
      :tree-data="categoryTreeData"
      :table-data="categoryCandidateSource"
      :loading="categorySelectorLoading"
      :columns="categoryTableColumns"
      row-key="id"
      selected-label-key="categoryName"
      :selected-rows="selectedCategoryCandidates"
      :keyword-value="categorySelectorKeyword"
      :status-value="categorySelectorStatus"
      keyword-label="分类"
      keyword-placeholder="支持按分类编码和名称查询..."
      status-label="启用状态"
      :status-options="[
        { label: '全部', value: '' },
        { label: '启用', value: '启用' },
        { label: '停用', value: '停用' },
      ]"
      :total="categorySelectorTotal"
      :current-page="categorySelectorCurrentPage"
      :page-size="categorySelectorPageSize"
      @search="handleCategorySelectorSearch"
      @node-change="handleCategoryNodeChange"
      @selection-change="handleCategorySelectionChange"
      @clear-selection="handleCategoryClear"
      @page-change="(p) => { categorySelectorCurrentPage = p; loadCategoryCandidates(); }"
      @page-size-change="(s) => { categorySelectorPageSize = s; categorySelectorCurrentPage = 1; loadCategoryCandidates(); }"
      @confirm="handleCategorySelectorConfirm"
    />
  </div>
</template>

<style scoped lang="scss">
.warehouse-item-rule-create-page {
  .basic-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .full-span {
    grid-column: 1 / -1;
  }

  .scope-group {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 12px;
  }

  .rule-item-tabs {
    margin-top: 2px;
  }
}

@media (max-width: 1200px) {
  .warehouse-item-rule-create-page .basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .warehouse-item-rule-create-page .basic-grid {
    grid-template-columns: 1fr;
  }
}
</style>
