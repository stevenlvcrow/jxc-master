<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue';
import { reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import { useSessionStore } from '@/stores/session';
import { fetchWarehousesApi, type WarehouseRow as ApiWarehouseRow } from '@/api/modules/warehouse';
import { createItemRuleApi, type RuleCreatePayload } from '@/api/modules/warehouse-item-rule';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';

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
  ruleCode: '',
  ruleName: '',
  businessControl: false,
  businessScopes: [] as string[],
  creator: sessionStore.userName,
  createdAt: '-',
  updatedAt: '-',
});

const formRules: FormRules = {
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
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

let seed = 0;
const nextId = () => `row-${++seed}`;

const resolveGroupId = (): number | null => {
  const orgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!orgId) return null;

  if (orgId.startsWith('group-')) {
    const id = Number(orgId.slice('group-'.length));
    return Number.isNaN(id) ? null : id;
  }

  // 当前机构为门店时，回溯其所属集团作为仓库数据作用域
  for (const group of sessionStore.rootGroups) {
    if (!group.id.startsWith('group-')) continue;
    if (group.children?.some((child) => child.id === orgId)) {
      const id = Number(group.id.slice('group-'.length));
      return Number.isNaN(id) ? null : id;
    }
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

const handleImport = () => {
  ElMessage.info('导入功能待接入');
};

const handleCategoryImport = () => {
  ElMessage.info('分类导入功能待接入');
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
  value: string;
  raw: ItemVO;
};

const querySearchItemCode = async (
  queryString: string,
  cb: (data: ItemCodeSuggestion[]) => void,
) => {
  const keyword = queryString.trim();
  if (!keyword) {
    cb([]);
    return;
  }
  try {
    const orgId = sessionStore.currentOrgId || undefined;
    const page = await fetchItemsApi(
      {
        pageNo: 1,
        pageSize: 20,
        keyword,
      },
      orgId,
    );
    const suggestions = (page.list ?? []).map((item) => ({
      value: item.code,
      raw: item,
    }));
    cb(suggestions);
  } catch {
    cb([]);
  }
};

const applySelectedItem = (row: ItemRow, item: ItemVO) => {
  row.itemCode = item.code || '';
  row.itemName = item.name || '';
  row.specModel = item.spec || '';
  row.itemCategory = item.category || item.type || '';
};

const handleSelectItemCode = (row: ItemRow, suggestion: ItemCodeSuggestion) => {
  applySelectedItem(row, suggestion.raw);
};

const buildCreatePayload = (): RuleCreatePayload => {
  const controlOrder = form.businessScopes.includes('管控适用机构订货');
  const controlPurchaseInbound = form.businessScopes.includes('管控适用机构采购入库');
  const controlTransferInbound = form.businessScopes.includes('管控适用机构调拨入库');

  return {
    ruleCode: form.ruleCode.trim(),
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
    router.back();
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
            <el-form-item label="规则编码" prop="ruleCode">
              <el-input v-model="form.ruleCode" placeholder="请输入规则编码" />
            </el-form-item>
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
              <div class="table-toolbar">
                <el-button @click="addItemRow()">添加物品</el-button>
                <el-button @click="handleImport">导入</el-button>
              </div>
              <el-table :data="itemRows" border stripe class="erp-table" :fit="false">
                <el-table-column type="index" label="序号" width="56" fixed="left" />
                <el-table-column label="操作" width="80" fixed="left">
                  <template #default="{ $index }">
                    <el-button text type="primary" @click="addItemRow($index)">+</el-button>
                    <el-button text @click="removeItemRow($index)">-</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="物品编码" min-width="130">
                  <template #default="{ row }">
                    <el-autocomplete
                      v-model="row.itemCode"
                      :fetch-suggestions="querySearchItemCode"
                      value-key="value"
                      placeholder="请输入编码搜索物品"
                      @select="handleSelectItemCode(row, $event)"
                    />
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
              <div class="table-toolbar">
                <el-button @click="addCategoryRow()">添加分类</el-button>
                <el-button @click="handleCategoryImport">导入</el-button>
              </div>
              <el-table :data="categoryRows" border stripe class="erp-table" :fit="false">
                <el-table-column type="index" label="序号" width="56" fixed="left" />
                <el-table-column label="操作" width="80" fixed="left">
                  <template #default="{ $index }">
                    <el-button text type="primary" @click="addCategoryRow($index)">+</el-button>
                    <el-button text @click="removeCategoryRow($index)">-</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="分类编码" min-width="130">
                  <template #default="{ row }"><el-input v-model="row.categoryCode" /></template>
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
