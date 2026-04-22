<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import { useSessionStore } from '@/stores/session';

type OrderType = '普通订单' | '预售订单' | '代店订单';
type LogisticsMode = '--' | '自提' | '配送' | '三方物流';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type OrderItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  brand: string;
  spec: string;
  category: string;
  orderUnit: string;
  totalLimitQty: number;
  storeLimitQty: number;
  storeLimitTimes: number;
  orderQty: number | null;
  orderPrice: number;
  orderAmount: number;
  remark: string;
};

const router = useRouter();
const sessionStore = useSessionStore();
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);

const sectionNavs = [
  { key: 'basic', label: '基础内容' },
  { key: 'items', label: '订货物品' },
];
const logisticsModeOptions: LogisticsMode[] = ['--', '自提', '配送', '三方物流'];
const orderTypeOptions: OrderType[] = ['普通订单', '预售订单', '代店订单'];
const receiverOptions = ['王总', '李总', '赵总'];

const supplierOrgTree = computed<TreeNode[]>(() => sessionStore.rootGroups.map((node) => ({
  value: node.id,
  label: node.name,
  children: node.children?.map((child) => ({
    value: child.id,
    label: child.name,
  })),
})));

const form = reactive({
  documentCode: '保存后生成',
  supplierOrg: '',
  sourceDocument: '-',
  logisticsMode: '--' as LogisticsMode,
  orderType: '普通订单' as OrderType,
  orderDate: '',
  expectedArrivalDate: '',
  estimatedArrivalDate: '-',
  receiver: '王总',
  receiverPhone: '15644432355',
  receiverAddress: '北京市北京市朝阳区朝阳一号',
  backupContact: '李总',
  backupPhone: '16578890007',
  orderTemplate: '-',
  createMode: '手工创建',
  createdAt: '保存后生成',
  creator: '当前登录人',
  submitter: '-',
  receiverClerk: '-',
  remark: '',
});

const itemRows = ref<OrderItemRow[]>([
  {
    id: 1,
    itemCode: 'ITEM-001',
    itemName: '鸡胸肉',
    brand: '鲜达',
    spec: '10kg/箱',
    category: '生鲜原料',
    orderUnit: '箱',
    totalLimitQty: 500,
    storeLimitQty: 30,
    storeLimitTimes: 2,
    orderQty: 10,
    orderPrice: 185,
    orderAmount: 1850,
    remark: '',
  },
]);

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleBack = () => {
  router.push('/order/order-documents');
};

const handleSaveDraft = () => {
  ElMessage.success('订货单草稿已保存');
};

const handleSave = () => {
  ElMessage.success('订货单保存成功');
};

const recalcAmount = (row: OrderItemRow) => {
  row.orderAmount = Number(row.orderQty || 0) * Number(row.orderPrice || 0);
};

const addItemRow = () => {
  itemRows.value.push({
    id: Date.now(),
    itemCode: '',
    itemName: '',
    brand: '',
    spec: '',
    category: '',
    orderUnit: '',
    totalLimitQty: 0,
    storeLimitQty: 0,
    storeLimitTimes: 0,
    orderQty: null,
    orderPrice: 0,
    orderAmount: 0,
    remark: '',
  });
};

const removeItemRow = (index: number) => {
  if (itemRows.value.length === 1) {
    ElMessage.warning('至少保留一条订货物品');
    return;
  }
  itemRows.value.splice(index, 1);
};

const removeSelectedEmptyRows = () => {
  itemRows.value = itemRows.value.filter((row) => Number(row.orderQty || 0) > 0);
  if (!itemRows.value.length) {
    addItemRow();
  }
};

const handleToolbarAction = (action: string) => {
  if (action === '添加物品') {
    addItemRow();
    return;
  }
  if (action === '移除为0或为空物品') {
    removeSelectedEmptyRows();
    return;
  }
  ElMessage.info(`${action}功能待接入`);
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
        <el-form :model="form" label-width="104px" class="item-create-form order-create-form">
          <div class="item-form-grid order-create-grid">
            <el-form-item label="单据号">
              <div class="readonly-field">{{ form.documentCode }}</div>
            </el-form-item>
            <el-form-item label="供货机构">
              <el-tree-select
                v-model="form.supplierOrg"
                :data="supplierOrgTree"
                :props="{ label: 'label', value: 'value', children: 'children' }"
                clearable
                filterable
                check-strictly
                default-expand-all
                placeholder="请选择供货机构"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="来源单据">
              <div class="readonly-field">{{ form.sourceDocument }}</div>
            </el-form-item>
            <el-form-item label="物流方式">
              <el-select v-model="form.logisticsMode" style="width: 100%">
                <el-option
                  v-for="option in logisticsModeOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                  :disabled="option === '三方物流'"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="订单类型">
              <el-select v-model="form.orderType" style="width: 100%">
                <el-option v-for="option in orderTypeOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="订单日期">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="期望到货">
              <el-date-picker v-model="form.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="预计到货日期">
              <div class="readonly-field">{{ form.estimatedArrivalDate }}</div>
            </el-form-item>
            <el-form-item label="收货人">
              <el-select v-model="form.receiver" style="width: 100%">
                <el-option v-for="option in receiverOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="收货电话">
              <el-input v-model="form.receiverPhone" disabled />
            </el-form-item>
            <el-form-item label="收货地址">
              <el-input v-model="form.receiverAddress" disabled />
            </el-form-item>
            <el-form-item label="备用联系人">
              <el-input v-model="form.backupContact" disabled />
            </el-form-item>
            <el-form-item label="备用联系电话">
              <el-input v-model="form.backupPhone" disabled />
            </el-form-item>
            <el-form-item label="配送体积">
              <div class="readonly-field">0 m³</div>
            </el-form-item>
            <el-form-item label="配送重量">
              <div class="readonly-field">0 kg</div>
            </el-form-item>
            <el-form-item label="是否代店下单">
              <div class="readonly-field">否</div>
            </el-form-item>
            <el-form-item label="订货模板">
              <div class="readonly-field">{{ form.orderTemplate }}</div>
            </el-form-item>
            <el-form-item label="创建方式">
              <div class="readonly-field">{{ form.createMode }}</div>
            </el-form-item>
            <el-form-item label="创建日期">
              <div class="readonly-field">{{ form.createdAt }}</div>
            </el-form-item>
            <el-form-item label="创建人">
              <div class="readonly-field">{{ form.creator }}</div>
            </el-form-item>
            <el-form-item label="提交人">
              <div class="readonly-field">{{ form.submitter }}</div>
            </el-form-item>
            <el-form-item label="接单人">
              <div class="readonly-field">{{ form.receiverClerk }}</div>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
            <el-form-item label="附件">
              <el-button @click="handleToolbarAction('附件')">附件</el-button>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">订货物品</h3>

        <div class="table-toolbar">
          <el-button type="primary" @click="handleToolbarAction('添加物品')">添加物品</el-button>
          <el-button @click="handleToolbarAction('批量移除物品')">批量移除物品</el-button>
          <el-button @click="handleToolbarAction('移除为0或为空物品')">移除为0或为空物品</el-button>
          <el-button @click="handleToolbarAction('移除售罄物品')">移除售罄物品</el-button>
          <el-button @click="handleToolbarAction('通过模板新增')">通过模板新增</el-button>
          <el-button @click="handleToolbarAction('通过菜品销售计划新增')">通过菜品销售计划新增</el-button>
          <el-button @click="handleToolbarAction('导入')">导入</el-button>
        </div>

        <el-table :data="itemRows" border stripe class="erp-table order-item-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="86" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" @click="addItemRow">+</el-button>
              <el-button text @click="removeItemRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.itemCode" placeholder="请输入物品编码" />
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.itemName" placeholder="请输入物品名称" />
            </template>
          </el-table-column>
          <el-table-column prop="brand" label="物品品牌" min-width="110" show-overflow-tooltip />
          <el-table-column prop="spec" label="规格型号" min-width="120" show-overflow-tooltip />
          <el-table-column prop="category" label="物品类别" min-width="120" show-overflow-tooltip />
          <el-table-column prop="orderUnit" label="订货单位" min-width="100" show-overflow-tooltip />
          <el-table-column prop="totalLimitQty" label="总限购数量" min-width="120" align="right" />
          <el-table-column prop="storeLimitQty" label="门店限购数量" min-width="130" align="right" />
          <el-table-column prop="storeLimitTimes" label="门店限购次数" min-width="130" align="right" />
          <el-table-column label="订货数量" min-width="120" align="right">
            <template #default="{ row }">
              <el-input-number
                v-model="row.orderQty"
                :min="0"
                controls-position="right"
                size="small"
                style="width: 110px"
                @change="recalcAmount(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="订货单价" min-width="110" align="right">
            <template #default="{ row }">{{ row.orderPrice.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="订货金额" min-width="110" align="right">
            <template #default="{ row }">{{ row.orderAmount.toFixed(2) }}</template>
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
.order-create-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.readonly-field {
  display: flex;
  align-items: center;
  min-height: 22px;
  color: #334155;
  font-size: 11px;
  line-height: 1;
}

.order-create-form :deep(.el-form-item__label),
.order-create-form :deep(.el-input__inner),
.order-create-form :deep(.el-select__selected-item),
.order-create-form :deep(.el-date-editor .el-input__inner) {
  font-size: 11px;
}

.order-create-form :deep(.el-input__wrapper),
.order-create-form :deep(.el-select__wrapper) {
  min-height: 22px;
}

.order-item-table :deep(.el-table__cell) {
  padding-top: 4px;
  padding-bottom: 4px;
}

.order-item-table :deep(.el-input__wrapper) {
  min-height: 22px;
}

.order-item-table :deep(.el-input__inner) {
  font-size: 11px;
}

@media (max-width: 1200px) {
  .order-create-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
