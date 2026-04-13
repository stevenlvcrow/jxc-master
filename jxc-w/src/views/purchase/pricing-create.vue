<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';

type PricingItemRow = {
  id: number;
  itemKeyword: string;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  purchaseUnit: string;
  spotPrice: string;
  previousPrice: string;
  quantityRange: string;
  taxIncludedPrice: string;
  taxRate: string;
  taxExcludedPrice: string;
  priceDiffRatio: string;
  priceLimit: string;
  remark: string;
};

const router = useRouter();
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);
const pricingCode = `PJ-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-001`;
const itemLookupRows = [
  {
    itemCode: 'ITEM-001',
    itemName: '鸡胸肉',
    spec: '2kg/袋',
    category: '肉类',
    purchaseUnit: '袋',
    spotPrice: '46.00',
    previousPrice: '44.50',
    taxRate: '9',
    priceLimit: '45.00-49.00',
  },
  {
    itemCode: 'ITEM-002',
    itemName: '牛腩',
    spec: '5kg/箱',
    category: '冻品',
    purchaseUnit: '箱',
    spotPrice: '168.00',
    previousPrice: '172.00',
    taxRate: '9',
    priceLimit: '160.00-175.00',
  },
  {
    itemCode: 'ITEM-003',
    itemName: '酸梅汤',
    spec: '500ml*12瓶',
    category: '饮品',
    purchaseUnit: '箱',
    spotPrice: '72.00',
    previousPrice: '70.00',
    taxRate: '13',
    priceLimit: '68.00-75.00',
  },
];

const sectionNavs = [
  { key: 'basic', label: '基础内容' },
  { key: 'items', label: '定价物品' },
];

const pricingTypeOptions = ['通用', '非通用'];
const supplierOptions = ['鲜达食品', '优选农场', '沪上冷链', '盒马包材'];
const attachmentTree = [
  {
    value: 'contract-root',
    label: '合同附件',
    children: [
      { value: 'contract-1', label: '鲜达食品年度合同.pdf' },
      { value: 'contract-2', label: '优选农场定价补充协议.pdf' },
    ],
  },
];

const form = reactive({
  pricingName: '',
  pricingType: '通用',
  supplier: '',
  effectiveDate: '',
  expireDate: '',
  contractAttachment: '',
  remark: '',
});

const itemRows = ref<PricingItemRow[]>([
  {
    id: 1,
    itemKeyword: '',
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    purchaseUnit: '',
    spotPrice: '',
    previousPrice: '',
    quantityRange: '',
    taxIncludedPrice: '',
    taxRate: '',
    taxExcludedPrice: '',
    priceDiffRatio: '',
    priceLimit: '',
    remark: '',
  },
]);

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleBack = () => {
  router.push('/purchase/1/1');
};

const handleSaveDraft = () => {
  ElMessage.success('采购定价单草稿已保存');
};

const handleSave = () => {
  ElMessage.success('采购定价单保存成功');
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const fillDerivedPricingFields = (row: PricingItemRow) => {
  const keyword = row.itemKeyword.trim().toLowerCase();
  if (!keyword) {
    row.itemCode = '';
    row.itemName = '';
    row.spec = '';
    row.category = '';
    row.purchaseUnit = '';
    row.spotPrice = '';
    row.previousPrice = '';
    row.taxRate = '';
    row.priceLimit = '';
    return;
  }

  const matched = itemLookupRows.find((item) => (
    item.itemCode.toLowerCase() === keyword || item.itemName.toLowerCase() === keyword
  ));

  if (!matched) {
    ElMessage.warning('未查询到对应物品，请检查物品编码或名称');
    row.itemCode = '';
    row.itemName = '';
    row.spec = '';
    row.category = '';
    row.purchaseUnit = '';
    row.spotPrice = '';
    row.previousPrice = '';
    row.taxRate = '';
    row.priceLimit = '';
    return;
  }

  row.itemCode = matched.itemCode;
  row.itemName = matched.itemName;
  row.spec = matched.spec;
  row.category = matched.category;
  row.purchaseUnit = matched.purchaseUnit;
  row.spotPrice = matched.spotPrice;
  row.previousPrice = matched.previousPrice;
  row.taxRate = matched.taxRate;
  row.priceLimit = matched.priceLimit;
  if (!row.taxIncludedPrice) {
    row.taxIncludedPrice = matched.spotPrice;
  }
  if (!row.taxExcludedPrice) {
    row.taxExcludedPrice = matched.spotPrice;
  }
};

const addItemRow = (index: number) => {
  itemRows.value.splice(index + 1, 0, {
    id: Date.now() + index,
    itemKeyword: '',
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    purchaseUnit: '',
    spotPrice: '',
    previousPrice: '',
    quantityRange: '',
    taxIncludedPrice: '',
    taxRate: '',
    taxExcludedPrice: '',
    priceDiffRatio: '',
    priceLimit: '',
    remark: '',
  });
};

const removeItemRow = (index: number) => {
  if (itemRows.value.length === 1) {
    ElMessage.warning('至少保留一条定价物品');
    return;
  }
  itemRows.value.splice(index, 1);
};
</script>

<template>
  <div class="item-create-page">
    <FixedActionBreadcrumb
      :navs="sectionNavs"
      :active-key="activeNav"
      @back="handleBack"
      @save-draft="handleSaveDraft"
      @save="handleSave"
      @navigate="scrollToSection"
    />

    <section class="panel form-panel">
      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础内容</h3>
        <el-form :model="form" label-width="96px" class="item-create-form purchase-pricing-form">
          <div class="item-form-grid purchase-pricing-grid">
            <el-form-item label="定价单编号">
              <div class="readonly-field">{{ pricingCode }}</div>
            </el-form-item>
            <el-form-item label="定价单名称">
              <el-input v-model="form.pricingName" placeholder="请输入定价单名称" />
            </el-form-item>
            <el-form-item label="定价单类型">
              <el-radio-group v-model="form.pricingType">
                <el-radio
                  v-for="option in pricingTypeOptions"
                  :key="option"
                  :label="option"
                >
                  {{ option }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="供应商">
              <el-select v-model="form.supplier" placeholder="请选择供应商" style="width: 100%">
                <el-option
                  v-for="option in supplierOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="价格生效日期">
              <el-date-picker
                v-model="form.effectiveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择价格生效日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="价格失效日期">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择价格失效日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="定价启用状态">
              <div class="readonly-field">启用</div>
            </el-form-item>
            <el-form-item label="定价单状态">
              <div class="readonly-field">草稿</div>
            </el-form-item>
            <el-form-item label="定价写入结果">
              <div class="readonly-field">未写入</div>
            </el-form-item>
            <el-form-item label="创建方式">
              <div class="readonly-field">手工创建</div>
            </el-form-item>
            <el-form-item label="合同附件">
              <el-tree-select
                v-model="form.contractAttachment"
                :data="attachmentTree"
                :props="{ label: 'label', value: 'value', children: 'children' }"
                check-strictly
                default-expand-all
                placeholder="请选择合同附件"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
            <el-form-item label="创建人">
              <div class="readonly-field">当前登录人</div>
            </el-form-item>
            <el-form-item label="最后修改人">
              <div class="readonly-field">当前登录人</div>
            </el-form-item>
            <el-form-item label="最后修改时间">
              <div class="readonly-field">保存后生成</div>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">定价物品</h3>

        <div class="table-toolbar">
          <el-button @click="handleToolbarAction('批量修改时价')">批量修改时价</el-button>
          <el-button @click="handleToolbarAction('批量修改定价')">批量修改定价</el-button>
          <el-button @click="handleToolbarAction('批量修改税率')">批量修改税率</el-button>
          <el-button @click="handleToolbarAction('批量修改不含税定价')">批量修改不含税定价</el-button>
          <el-button @click="handleToolbarAction('批量修改单价限制')">批量修改单价限制</el-button>
          <el-button @click="handleToolbarAction('导入')">导入</el-button>
        </div>

        <el-table :data="itemRows" border stripe class="erp-table purchase-pricing-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="86" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" @click="addItemRow($index)">+</el-button>
              <el-button text @click="removeItemRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="120">
            <template #default="{ row }">
              <el-input
                v-model="row.itemKeyword"
                placeholder="输入物品编码/名称"
                @change="fillDerivedPricingFields(row)"
                @blur="fillDerivedPricingFields(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="140">
            <template #default="{ row }">
              <span>{{ row.itemName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="120">
            <template #default="{ row }">
              <span>{{ row.spec || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="120">
            <template #default="{ row }">
              <span>{{ row.category || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="采购单位" min-width="110">
            <template #default="{ row }">
              <span>{{ row.purchaseUnit || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时价" min-width="100">
            <template #default="{ row }">
              <span>{{ row.spotPrice || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="上期价格" min-width="100">
            <template #default="{ row }">
              <span>{{ row.previousPrice || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量范围" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.quantityRange" placeholder="请输入数量范围" />
            </template>
          </el-table-column>
          <el-table-column label="定价 (含税)" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.taxIncludedPrice" placeholder="请输入含税定价" />
            </template>
          </el-table-column>
          <el-table-column label="税率" min-width="90">
            <template #default="{ row }">
              <span>{{ row.taxRate || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="定价 (不含税)" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.taxExcludedPrice" placeholder="请输入不含税定价" />
            </template>
          </el-table-column>
          <el-table-column label="定价环比" min-width="100">
            <template #default="{ row }">
              <el-input v-model="row.priceDiffRatio" placeholder="请输入定价环比" />
            </template>
          </el-table-column>
          <el-table-column label="单价限制" min-width="100">
            <template #default="{ row }">
              <span>{{ row.priceLimit || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.purchase-pricing-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.purchase-pricing-form :deep(.el-input__wrapper),
.purchase-pricing-form :deep(.el-select__wrapper),
.purchase-pricing-form :deep(.el-textarea__inner) {
  min-height: 22px;
}

.readonly-field {
  display: flex;
  align-items: center;
  min-height: 22px;
  color: #334155;
  font-size: 11px;
  line-height: 1;
}

.purchase-pricing-form :deep(.el-form-item__label) {
  font-size: 11px;
}

.purchase-pricing-form :deep(.el-input__inner),
.purchase-pricing-form :deep(.el-select__selected-item),
.purchase-pricing-form :deep(.el-radio__label),
.purchase-pricing-form :deep(.el-date-editor .el-input__inner) {
  font-size: 11px;
}

.purchase-pricing-form :deep(.el-textarea__inner) {
  font-size: 11px;
  padding-top: 4px;
  padding-bottom: 4px;
}

.purchase-pricing-table :deep(.el-table__cell) {
  padding-top: 4px;
  padding-bottom: 4px;
}

.purchase-pricing-table :deep(.el-input__wrapper) {
  min-height: 22px;
  padding-top: 0;
  padding-bottom: 0;
}

.purchase-pricing-table :deep(.el-input__inner) {
  font-size: 10px;
}

.purchase-pricing-table :deep(.el-button.is-text) {
  padding-top: 0;
  padding-bottom: 0;
  min-height: 18px;
}

@media (max-width: 1200px) {
  .purchase-pricing-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .purchase-pricing-grid {
    grid-template-columns: 1fr;
  }
}
</style>
