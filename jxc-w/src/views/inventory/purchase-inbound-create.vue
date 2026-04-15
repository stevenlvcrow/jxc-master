<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import { createPurchaseInboundApi } from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';

type SupplierOption = {
  id: number;
  name: string;
  code: string;
  contact: string;
};

type ItemCatalog = {
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  purchaseUnit: string;
};

type ItemRow = {
  id: number;
  itemQuery: string;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  warehouse: string;
  purchaseUnit: string;
  quantity: number | null;
  inboundPrice: number | null;
  amount: number | null;
  gift: boolean;
  remark: string;
};

const router = useRouter();
const sessionStore = useSessionStore();
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const itemSectionRef = ref<HTMLElement | null>(null);

const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '物品信息' },
];

const supplierOptions: SupplierOption[] = [
  { id: 1, name: '鲜达食品', code: 'SUP-001', contact: '张敏' },
  { id: 2, name: '优选农场', code: 'SUP-002', contact: '李娜' },
  { id: 3, name: '盒马包材', code: 'SUP-003', contact: '王磊' },
];
const salesOptions = ['张敏', '李娜', '王磊', '赵强'];
const warehouseOptions = ['中央成品仓', '北区原料仓', '南区包材仓'];
const purchaseUnitOptions = ['斤', '箱', '袋', '个', '瓶'];

const itemCatalog: ItemCatalog[] = [
  { itemCode: 'ITEM-001', itemName: '鸡胸肉', spec: '2kg/袋', category: '肉类', purchaseUnit: '袋' },
  { itemCode: 'ITEM-002', itemName: '牛腩', spec: '5kg/箱', category: '冻品', purchaseUnit: '箱' },
  { itemCode: 'ITEM-003', itemName: '包装盒', spec: '100个/箱', category: '包材', purchaseUnit: '箱' },
  { itemCode: 'ITEM-004', itemName: '酸梅汤', spec: '500ml*12瓶', category: '饮品', purchaseUnit: '箱' },
];

const form = reactive({
  supplierId: 0,
  supplierName: '',
  inboundDate: '',
  salesman: '',
  remark: '',
});

const rowSeed = ref(2);
const rows = ref<ItemRow[]>([
  {
    id: 1,
    itemQuery: '',
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    warehouse: '',
    purchaseUnit: '',
    quantity: null,
    inboundPrice: null,
    amount: null,
    gift: false,
    remark: '',
  },
]);

const batchWarehouseDialogVisible = ref(false);
const batchWarehouse = ref('');

const resolveOrgId = () => {
  const orgId = (sessionStore.currentOrgId ?? '').trim();
  if (!orgId) {
    return undefined;
  }
  if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
    return orgId;
  }
  return undefined;
};

const handleBack = () => {
  router.push('/inventory/1/2');
};

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleSupplierChange = (supplierId: number) => {
  const supplier = supplierOptions.find((item) => item.id === supplierId);
  if (!supplier) {
    form.supplierName = '';
    return;
  }
  form.supplierId = supplier.id;
  form.supplierName = supplier.name;
};

const addRow = (index?: number) => {
  const targetIndex = typeof index === 'number' ? index + 1 : rows.value.length;
  rows.value.splice(targetIndex, 0, {
    id: rowSeed.value,
    itemQuery: '',
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    warehouse: '',
    purchaseUnit: '',
    quantity: null,
    inboundPrice: null,
    amount: null,
    gift: false,
    remark: '',
  });
  rowSeed.value += 1;
};

const removeRow = (index: number) => {
  if (rows.value.length <= 1) {
    ElMessage.warning('至少保留一条物品');
    return;
  }
  rows.value.splice(index, 1);
};

const applyCatalogToRow = (row: ItemRow, matched?: ItemCatalog) => {
  if (!matched) {
    row.itemCode = '';
    row.itemName = '';
    row.spec = '';
    row.category = '';
    row.purchaseUnit = '';
    return;
  }
  row.itemCode = matched.itemCode;
  row.itemName = matched.itemName;
  row.spec = matched.spec;
  row.category = matched.category;
  if (!row.purchaseUnit) {
    row.purchaseUnit = matched.purchaseUnit;
  }
};

const querySearchItem = (queryString: string, cb: (items: Array<{ value: string; raw: ItemCatalog }>) => void) => {
  const keyword = queryString.trim().toLowerCase();
  const result = itemCatalog
    .filter((item) => !keyword
      || item.itemCode.toLowerCase().includes(keyword)
      || item.itemName.toLowerCase().includes(keyword))
    .map((item) => ({
      value: `${item.itemCode} / ${item.itemName}`,
      raw: item,
    }));
  cb(result);
};

const handleSelectItem = (row: ItemRow, selected: { value: string; raw: ItemCatalog }) => {
  row.itemQuery = selected.value;
  applyCatalogToRow(row, selected.raw);
};

const handleBlurItemQuery = (row: ItemRow) => {
  const keyword = row.itemQuery.trim().toLowerCase();
  if (!keyword) {
    applyCatalogToRow(row, undefined);
    return;
  }
  const matched = itemCatalog.find((item) => item.itemCode.toLowerCase() === keyword || item.itemName.toLowerCase() === keyword);
  applyCatalogToRow(row, matched);
};

const totalQuantity = computed(() => rows.value.reduce((sum, row) => sum + (row.quantity ?? 0), 0));
const totalAmount = computed(() => rows.value.reduce((sum, row) => sum + (row.amount ?? 0), 0));

const handleToolbarAction = (action: string) => {
  if (action === '添加物品') {
    addRow();
    return;
  }
  if (action === '通过模板新建') {
    rows.value = [
      {
        id: rowSeed.value++,
        itemQuery: 'ITEM-001 / 鸡胸肉',
        itemCode: 'ITEM-001',
        itemName: '鸡胸肉',
        spec: '2kg/袋',
        category: '肉类',
        warehouse: '中央成品仓',
        purchaseUnit: '袋',
        quantity: 10,
        inboundPrice: 46.5,
        amount: 465,
        gift: false,
        remark: '',
      },
      {
        id: rowSeed.value++,
        itemQuery: 'ITEM-003 / 包装盒',
        itemCode: 'ITEM-003',
        itemName: '包装盒',
        spec: '100个/箱',
        category: '包材',
        warehouse: '南区包材仓',
        purchaseUnit: '箱',
        quantity: 6,
        inboundPrice: 68,
        amount: 408,
        gift: false,
        remark: '',
      },
    ];
    ElMessage.success('已通过模板填充示例物品');
    return;
  }
  if (action === '批量选择仓库') {
    batchWarehouse.value = '';
    batchWarehouseDialogVisible.value = true;
    return;
  }
  if (action === '批量导入物品') {
    const imported = itemCatalog.slice(0, 2).map((item) => ({
      id: rowSeed.value++,
      itemQuery: `${item.itemCode} / ${item.itemName}`,
      itemCode: item.itemCode,
      itemName: item.itemName,
      spec: item.spec,
      category: item.category,
      warehouse: '',
      purchaseUnit: item.purchaseUnit,
      quantity: null,
      inboundPrice: null,
      amount: null,
      gift: false,
      remark: '',
    }));
    rows.value.push(...imported);
    ElMessage.success(`已导入 ${imported.length} 条物品`);
  }
};

const confirmBatchWarehouse = () => {
  if (!batchWarehouse.value) {
    ElMessage.warning('请选择仓库');
    return;
  }
  rows.value.forEach((row) => {
    row.warehouse = batchWarehouse.value;
  });
  batchWarehouseDialogVisible.value = false;
};

const resolveSingleWarehouse = () => {
  const warehouses = Array.from(new Set(rows.value.map((row) => row.warehouse).filter((value) => !!value)));
  if (!warehouses.length) {
    return '';
  }
  if (warehouses.length > 1) {
    return null;
  }
  return warehouses[0];
};

const validateForm = () => {
  if (!form.supplierName) {
    ElMessage.warning('请选择供应商');
    return false;
  }
  if (!form.inboundDate) {
    ElMessage.warning('请选择入库日期');
    return false;
  }
  if (!form.salesman) {
    ElMessage.warning('请选择业务员');
    return false;
  }
  if (!rows.value.length) {
    ElMessage.warning('请添加物品');
    return false;
  }
  const invalidRow = rows.value.find((row) => !row.itemCode || !row.itemName || !row.warehouse || !row.purchaseUnit || !row.quantity);
  if (invalidRow) {
    ElMessage.warning('请完善物品信息（编码、名称、仓库、单位、数量）');
    return false;
  }
  if (resolveSingleWarehouse() === null) {
    ElMessage.warning('当前接口仅支持单仓入库，请将所有物品统一为同一仓库');
    return false;
  }
  return true;
};

const handleSaveDraft = () => {
  ElMessage.info('当前版本仅支持直接保存入库单，草稿功能待接入');
};

const handleSave = async () => {
  if (!validateForm()) {
    return;
  }
  const singleWarehouse = resolveSingleWarehouse();
  if (!singleWarehouse) {
    ElMessage.warning('请选择仓库');
    return;
  }
  const payload = {
    inboundDate: form.inboundDate,
    warehouse: singleWarehouse,
    supplier: form.supplierName,
    remark: form.remark || undefined,
    items: rows.value.map((row) => ({
      itemCode: row.itemCode,
      itemName: row.itemName,
      quantity: Number(row.quantity ?? 0),
      unitPrice: Number(row.inboundPrice ?? 0),
      taxRate: 13,
    })),
  };
  const created = await createPurchaseInboundApi(payload, resolveOrgId());
  ElMessage.success(`保存成功：${created.documentCode}`);
  router.push('/inventory/1/2');
};
</script>

<template>
  <div class="item-create-page">
    <FixedActionBreadcrumb
      :navs="navs"
      :active-key="activeNav"
      @back="handleBack"
      @save-draft="handleSaveDraft"
      @save="handleSave"
      @navigate="scrollToSection"
    />

    <section class="panel form-panel">
      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础信息</h3>
        <el-form label-width="96px" class="item-create-form purchase-inbound-create-form">
          <div class="item-form-grid purchase-inbound-basic-grid">
            <el-form-item label="单据编号">
              <div class="readonly-field">保存后生成</div>
            </el-form-item>
            <el-form-item label="供应商">
              <el-select
                v-model="form.supplierId"
                placeholder="请选择供应商"
                style="width: 100%"
                @change="handleSupplierChange"
              >
                <el-option
                  v-for="option in supplierOptions"
                  :key="option.id"
                  :label="option.name"
                  :value="option.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="入库日期">
              <el-date-picker
                v-model="form.inboundDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择入库日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="业务员">
              <el-select v-model="form.salesman" placeholder="请选择业务员" style="width: 100%">
                <el-option v-for="option in salesOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注" class="purchase-inbound-remark-item">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="itemSectionRef" class="form-section-block">
        <h3 class="form-section-title">物品信息</h3>
        <div class="table-toolbar">
          <el-button type="primary" plain @click="handleToolbarAction('添加物品')">添加物品</el-button>
          <el-button @click="handleToolbarAction('通过模板新建')">通过模板新建</el-button>
          <el-button @click="handleToolbarAction('批量选择仓库')">批量选择仓库</el-button>
          <el-button @click="handleToolbarAction('批量导入物品')">批量导入物品</el-button>
        </div>

        <el-table :data="rows" border stripe class="erp-table purchase-inbound-item-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="96" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" @click="addRow($index)">+</el-button>
              <el-button text @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="180">
            <template #default="{ row }">
              <el-autocomplete
                v-model="row.itemQuery"
                :fetch-suggestions="querySearchItem"
                placeholder="编码/名称检索"
                clearable
                @select="handleSelectItem(row, $event)"
                @blur="handleBlurItemQuery(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="130">
            <template #default="{ row }">{{ row.itemName || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="120">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="110">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column label="仓库" min-width="140">
            <template #default="{ row }">
              <el-select v-model="row.warehouse" placeholder="请选择仓库">
                <el-option v-for="option in warehouseOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="采购单位" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.purchaseUnit" placeholder="请选择单位">
                <el-option v-for="option in purchaseUnitOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" min-width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="4" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="入库单价" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.inboundPrice" :min="0" :precision="4" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="金额" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="0" :precision="2" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="是否赠品" min-width="96">
            <template #default="{ row }">
              <el-switch v-model="row.gift" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="请输入备注" />
            </template>
          </el-table-column>
          <template #append>
            <div class="purchase-inbound-summary-row">
              <span class="summary-title">合计</span>
              <span class="summary-cell">数量：{{ totalQuantity.toFixed(4) }}</span>
              <span class="summary-cell">金额：{{ totalAmount.toFixed(2) }}</span>
            </div>
          </template>
        </el-table>
      </div>
    </section>

    <el-dialog
      v-model="batchWarehouseDialogVisible"
      title="批量选择仓库"
      width="420px"
      class="standard-form-dialog"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="仓库">
          <el-select v-model="batchWarehouse" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="option in warehouseOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchWarehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchWarehouse">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.purchase-inbound-basic-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.purchase-inbound-remark-item {
  grid-column: span 2;
}

.readonly-field {
  display: flex;
  align-items: center;
  min-height: 22px;
  color: #334155;
  font-size: 11px;
  line-height: 1;
}

.purchase-inbound-create-form :deep(.el-input__wrapper),
.purchase-inbound-create-form :deep(.el-select__wrapper),
.purchase-inbound-create-form :deep(.el-textarea__inner) {
  min-height: 22px;
}

.purchase-inbound-create-form :deep(.el-form-item__label) {
  font-size: 11px;
}

.purchase-inbound-create-form :deep(.el-input__inner),
.purchase-inbound-create-form :deep(.el-select__selected-item),
.purchase-inbound-create-form :deep(.el-date-editor .el-input__inner) {
  font-size: 11px;
}

.purchase-inbound-item-table :deep(.el-input-number) {
  width: 100%;
}

.purchase-inbound-item-table :deep(.el-input__wrapper),
.purchase-inbound-item-table :deep(.el-select__wrapper) {
  min-height: 24px;
}

.purchase-inbound-summary-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 32px;
  padding: 10px 16px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-size: 12px;
  color: #334155;
}

.summary-title {
  margin-right: auto;
  color: #0f172a;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .purchase-inbound-basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .purchase-inbound-basic-grid {
    grid-template-columns: 1fr;
  }

  .purchase-inbound-remark-item {
    grid-column: auto;
  }
}
</style>
