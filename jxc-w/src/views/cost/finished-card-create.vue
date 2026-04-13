<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { ComponentPublicInstance } from 'vue';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';

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

type SectionNav = {
  key: string;
  label: string;
};

const router = useRouter();

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
              <template #default="{ row }">
                <el-input v-model="row.spuCode" />
              </template>
            </el-table-column>
            <el-table-column label="菜品名称" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.dishName" />
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.spec" />
              </template>
            </el-table-column>
            <el-table-column label="菜品分类" min-width="110">
              <template #default="{ row }">
                <el-input v-model="row.category" />
              </template>
            </el-table-column>
            <el-table-column label="菜品类型" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.dishType" />
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
              <template #default="{ row }">
                <el-input v-model="row.itemCode" />
              </template>
            </el-table-column>
            <el-table-column label="物品名称" width="120">
              <template #default="{ row }">
                <el-input v-model="row.itemName" />
              </template>
            </el-table-column>
            <el-table-column label="规格型号" width="120">
              <template #default="{ row }">
                <el-input v-model="row.specModel" />
              </template>
            </el-table-column>
            <el-table-column label="成本单位" width="96">
              <template #default="{ row }">
                <el-input v-model="row.costUnit" />
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
                <el-input v-model="row.baseUnit" />
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
  </div>
</template>
