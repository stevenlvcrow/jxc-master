<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type DocumentStatus = '全部' | '草稿' | '已提交' | '已审核' | '已关闭';
type GiftStatus = '全部' | '是' | '否';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseReturnTrackingRow = {
  id: string;
  returnCode: string;
  documentStatus: Exclude<DocumentStatus, '全部'>;
  returnDate: string;
  sourceCode: string;
  supplierCode: string;
  supplierName: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  purchaseUnit: string;
  baseUnit: string;
  isGift: Exclude<GiftStatus, '全部'>;
  returnQty: number;
  returnBaseQty: number;
  returnAmount: number;
  auditQty: number;
  shippedQty: number;
  shippedBaseQty: number;
  shippedAmount: number;
  shippingWarehouse: string;
};
type ReportColumn = {
  key: keyof PurchaseReturnTrackingRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right' | 'center';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const documentStatusOptions: DocumentStatus[] = ['全部', '草稿', '已提交', '已审核', '已关闭'];
const giftStatusOptions: GiftStatus[] = ['全部', '是', '否'];

const query = reactive({
  shippingWarehouse: '',
  dateRange: [] as string[],
  supplier: '',
  returnCode: '',
  itemCode: '',
  documentStatus: '全部' as DocumentStatus,
  isGift: '全部' as GiftStatus,
});

const loading = ref(false);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const tableRows = ref<PurchaseReturnTrackingRow[]>([
  {
    id: '1',
    returnCode: 'PRT-202604-001',
    documentStatus: '已审核',
    returnDate: '2026-04-23',
    sourceCode: 'PR-202604-001',
    supplierCode: 'SUP-001',
    supplierName: '鲜达食品',
    itemName: '鸡胸肉',
    spec: '10kg/箱',
    itemCategory: '生鲜原料',
    purchaseUnit: '箱',
    baseUnit: 'kg',
    isGift: '否',
    returnQty: 1,
    returnBaseQty: 10,
    returnAmount: 185,
    auditQty: 1,
    shippedQty: 1,
    shippedBaseQty: 10,
    shippedAmount: 185,
    shippingWarehouse: '中央成品仓',
  },
  {
    id: '2',
    returnCode: 'PRT-202604-002',
    documentStatus: '已提交',
    returnDate: '2026-04-24',
    sourceCode: 'PO-202604-002',
    supplierCode: 'SUP-002',
    supplierName: '优选农场',
    itemName: '牛腩',
    spec: '5kg/包',
    itemCategory: '生鲜原料',
    purchaseUnit: '包',
    baseUnit: 'kg',
    isGift: '否',
    returnQty: 2,
    returnBaseQty: 10,
    returnAmount: 520,
    auditQty: 2,
    shippedQty: 0,
    shippedBaseQty: 0,
    shippedAmount: 0,
    shippingWarehouse: '北区原料仓',
  },
  {
    id: '3',
    returnCode: 'PRT-202604-003',
    documentStatus: '草稿',
    returnDate: '2026-04-24',
    sourceCode: '-',
    supplierCode: 'SUP-003',
    supplierName: '盒马包材',
    itemName: '包装盒',
    spec: '500个/箱',
    itemCategory: '包材',
    purchaseUnit: '箱',
    baseUnit: '个',
    isGift: '否',
    returnQty: 1,
    returnBaseQty: 500,
    returnAmount: 96,
    auditQty: 0,
    shippedQty: 0,
    shippedBaseQty: 0,
    shippedAmount: 0,
    shippingWarehouse: '南区包材仓',
  },
]);

const supplierTree = computed<TreeNode[]>(() => supplierOptions.value.map((item) => ({
  value: item.value,
  label: item.label,
})));

const columns: ReportColumn[] = [
  { key: 'returnCode', label: '退货单号', minWidth: 150, fixed: 'left' },
  { key: 'documentStatus', label: '单据状态', minWidth: 100 },
  { key: 'returnDate', label: '退货日期', minWidth: 120 },
  { key: 'sourceCode', label: '来源单号', minWidth: 150 },
  { key: 'supplierCode', label: '供应商编码', minWidth: 130 },
  { key: 'supplierName', label: '供应商名称', minWidth: 140 },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'spec', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'purchaseUnit', label: '采购单位', minWidth: 100 },
  { key: 'baseUnit', label: '基准单位', minWidth: 100 },
  { key: 'isGift', label: '是否赠品', minWidth: 100 },
  { key: 'returnQty', label: '退货单数量', minWidth: 120, align: 'right' },
  { key: 'returnBaseQty', label: '退货单数量（基准单位）', minWidth: 180, align: 'right' },
  { key: 'returnAmount', label: '退货单金额', minWidth: 120, align: 'right' },
  { key: 'auditQty', label: '审核数量', minWidth: 110, align: 'right' },
  { key: 'shippedQty', label: '发货数量', minWidth: 110, align: 'right' },
  { key: 'shippedBaseQty', label: '发货数量（基准单位）', minWidth: 170, align: 'right' },
  { key: 'shippedAmount', label: '发货金额', minWidth: 110, align: 'right' },
  { key: 'shippingWarehouse', label: '发货仓库', minWidth: 130 },
];
const numericKeys: Array<keyof PurchaseReturnTrackingRow> = [
  'returnQty',
  'returnBaseQty',
  'returnAmount',
  'auditQty',
  'shippedQty',
  'shippedBaseQty',
  'shippedAmount',
];

const fetchAllPages = async <T,>(
  loader: (pageNo: number, pageSizeValue: number) => Promise<{ list?: T[]; total?: number; pageSize?: number }>,
  pageSizeValue = 200,
) => {
  const rows: T[] = [];
  let pageNo = 1;
  let totalValue: number;
  do {
    const page = await loader(pageNo, pageSizeValue);
    const list = Array.isArray(page.list) ? page.list : [];
    rows.push(...list);
    totalValue = Number(page.total ?? rows.length);
    if (!list.length || Number(page.pageSize ?? 0) <= 0) {
      break;
    }
    pageNo += 1;
  } while (rows.length < totalValue);
  return rows;
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await Promise.all([loadWarehouseTree(), loadSupplierOptions()]);
    if (!orgId) {
      itemOptions.value = [];
      return;
    }
    const itemRows = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId));
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemOptions.value = [];
    ElMessage.error('退货单状态跟踪表筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const fetchReport = async () => {
  loading.value = true;
  try {
    total.value = tableRows.value.length;
  } finally {
    loading.value = false;
  }
};

const formatCell = (row: PurchaseReturnTrackingRow, key: keyof PurchaseReturnTrackingRow) => {
  const value = row[key];
  if (typeof value === 'number') {
    return value.toFixed(2);
  }
  return String(value || '-');
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.shippingWarehouse = '';
  query.dateRange = [];
  query.supplier = '';
  query.returnCode = '';
  query.itemCode = '';
  query.documentStatus = '全部';
  query.isGift = '全部';
  currentPage.value = 1;
  await fetchReport();
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchReport();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchReport();
};

const getSummaries = ({ columns: tableColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return tableColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof PurchaseReturnTrackingRow | undefined;
    if (!property || !numericKeys.includes(property)) {
      return '';
    }
    return tableRows.value.reduce((sum, row) => sum + Number(row[property] ?? 0), 0).toFixed(2);
  });
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  async () => {
    await loadOptions();
    await fetchReport();
  },
);

onMounted(async () => {
  await loadOptions();
  await fetchReport();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="发货仓库">
        <el-tree-select
          v-model="query.shippingWarehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          filterable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="日期">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item label="供应商">
        <el-tree-select
          v-model="query.supplier"
          :data="supplierTree"
          :loading="supplierLoading"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          filterable
          check-strictly
          placeholder="请选择"
          style="width: 190px"
        />
      </el-form-item>

      <el-form-item label="退货单据号">
        <el-input v-model="query.returnCode" clearable placeholder="请输入退货单据号" style="width: 180px" />
      </el-form-item>

      <el-form-item label="物品">
        <el-select
          v-model="query.itemCode"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
          style="width: 220px"
        >
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" style="width: 120px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="是否赠品">
        <el-select v-model="query.isGift" style="width: 120px">
          <el-option v-for="option in giftStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonTableSection
      :data="tableRows"
      row-key="id"
      :loading="loading"
      :height="460"
      :show-summary="true"
      :summary-method="getSummaries"
      empty-text="暂无退货单状态跟踪数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatCell(row, column.key) }}
        </template>
      </el-table-column>
    </CommonTableSection>

    <div class="table-pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>
</template>
