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

type DateType = '订货日期' | '期望到货日期' | '收货日期';
type DocumentStatus = '草稿' | '已提交' | '已审核' | '已关闭';
type ReceiveStatus = '未收货' | '部分收货' | '已收货';
type GiftStatus = '全部' | '是' | '否';
type CrossMonthStatus = '全部' | '是' | '否';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseOrderTrackingRow = {
  id: string;
  purchaseOrderCode: string;
  documentStatus: DocumentStatus;
  receiveStatus: ReceiveStatus;
  orderDate: string;
  expectedArrivalDate: string;
  supplierName: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  isGift: Exclude<GiftStatus, '全部'>;
  purchaseUnit: string;
  purchasePrice: number;
  purchaseQty: number;
  auditQty: number;
  purchaseAmount: number;
  auditAmount: number;
  purchaseWarehouse: string;
  receiptDate: string;
  receiptWarehouse: string;
  receivedQty: number;
  receiptAmount: number;
  unreceivedQty: number;
  returnedQty: number;
  returnAmount: number;
};
type ReportColumn = {
  key: keyof PurchaseOrderTrackingRow;
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

const dateTypeOptions: DateType[] = ['订货日期', '期望到货日期', '收货日期'];
const documentStatusTree: TreeNode[] = [
  { value: '草稿', label: '草稿' },
  { value: '已提交', label: '已提交' },
  { value: '已审核', label: '已审核' },
  { value: '已关闭', label: '已关闭' },
];
const receiveStatusTree: TreeNode[] = [
  { value: '未收货', label: '未收货' },
  { value: '部分收货', label: '部分收货' },
  { value: '已收货', label: '已收货' },
];
const giftStatusOptions: GiftStatus[] = ['全部', '是', '否'];
const crossMonthOptions: CrossMonthStatus[] = ['全部', '是', '否'];

const query = reactive({
  dateType: '订货日期' as DateType,
  dateRange: [] as string[],
  receiptWarehouse: '',
  purchaseWarehouse: '',
  itemCode: '',
  supplier: '',
  purchaseOrderCode: '',
  documentStatus: '',
  receiveStatus: '',
  isGift: '全部' as GiftStatus,
  crossMonth: '全部' as CrossMonthStatus,
});

const loading = ref(false);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const tableRows = ref<PurchaseOrderTrackingRow[]>([
  {
    id: '1',
    purchaseOrderCode: 'PO-202604-001',
    documentStatus: '已审核',
    receiveStatus: '部分收货',
    orderDate: '2026-04-20',
    expectedArrivalDate: '2026-04-24',
    supplierName: '鲜达食品',
    itemName: '鸡胸肉',
    spec: '10kg/箱',
    itemCategory: '生鲜原料',
    isGift: '否',
    purchaseUnit: '箱',
    purchasePrice: 185,
    purchaseQty: 20,
    auditQty: 20,
    purchaseAmount: 3700,
    auditAmount: 3700,
    purchaseWarehouse: '中央成品仓',
    receiptDate: '2026-04-22',
    receiptWarehouse: '中央成品仓',
    receivedQty: 12,
    receiptAmount: 2220,
    unreceivedQty: 8,
    returnedQty: 1,
    returnAmount: 185,
  },
  {
    id: '2',
    purchaseOrderCode: 'PO-202604-002',
    documentStatus: '已提交',
    receiveStatus: '未收货',
    orderDate: '2026-04-21',
    expectedArrivalDate: '2026-04-25',
    supplierName: '优选农场',
    itemName: '牛腩',
    spec: '5kg/包',
    itemCategory: '生鲜原料',
    isGift: '否',
    purchaseUnit: '包',
    purchasePrice: 260,
    purchaseQty: 14,
    auditQty: 14,
    purchaseAmount: 3640,
    auditAmount: 3640,
    purchaseWarehouse: '北区原料仓',
    receiptDate: '-',
    receiptWarehouse: '北区原料仓',
    receivedQty: 0,
    receiptAmount: 0,
    unreceivedQty: 14,
    returnedQty: 0,
    returnAmount: 0,
  },
  {
    id: '3',
    purchaseOrderCode: 'PO-202604-003',
    documentStatus: '已审核',
    receiveStatus: '已收货',
    orderDate: '2026-04-21',
    expectedArrivalDate: '2026-04-26',
    supplierName: '盒马包材',
    itemName: '包装盒',
    spec: '500个/箱',
    itemCategory: '包材',
    isGift: '否',
    purchaseUnit: '箱',
    purchasePrice: 96,
    purchaseQty: 8,
    auditQty: 8,
    purchaseAmount: 768,
    auditAmount: 768,
    purchaseWarehouse: '南区包材仓',
    receiptDate: '2026-04-23',
    receiptWarehouse: '南区包材仓',
    receivedQty: 8,
    receiptAmount: 768,
    unreceivedQty: 0,
    returnedQty: 0,
    returnAmount: 0,
  },
]);

const supplierTree = computed<TreeNode[]>(() => supplierOptions.value.map((item) => ({
  value: item.value,
  label: item.label,
})));

const columns: ReportColumn[] = [
  { key: 'purchaseOrderCode', label: '采购单号', minWidth: 150, fixed: 'left' },
  { key: 'documentStatus', label: '单据状态', minWidth: 100 },
  { key: 'receiveStatus', label: '收货状态', minWidth: 100 },
  { key: 'orderDate', label: '订单日期', minWidth: 120 },
  { key: 'expectedArrivalDate', label: '期望到货日期', minWidth: 140 },
  { key: 'supplierName', label: '供应商名称', minWidth: 140 },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'spec', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'isGift', label: '是否赠品', minWidth: 100 },
  { key: 'purchaseUnit', label: '采购单位', minWidth: 100 },
  { key: 'purchasePrice', label: '采购单价', minWidth: 110, align: 'right' },
  { key: 'purchaseQty', label: '采购数量', minWidth: 110, align: 'right' },
  { key: 'auditQty', label: '审核数量', minWidth: 110, align: 'right' },
  { key: 'purchaseAmount', label: '采购金额', minWidth: 110, align: 'right' },
  { key: 'auditAmount', label: '审核金额', minWidth: 110, align: 'right' },
  { key: 'purchaseWarehouse', label: '采购仓库', minWidth: 130 },
  { key: 'receiptDate', label: '收货日期', minWidth: 120 },
  { key: 'receiptWarehouse', label: '收货仓库', minWidth: 130 },
  { key: 'receivedQty', label: '已收货数量', minWidth: 120, align: 'right' },
  { key: 'receiptAmount', label: '收货金额', minWidth: 110, align: 'right' },
  { key: 'unreceivedQty', label: '采购数量 - 已收货数量', minWidth: 170, align: 'right' },
  { key: 'returnedQty', label: '已退货数量', minWidth: 120, align: 'right' },
  { key: 'returnAmount', label: '退货金额', minWidth: 110, align: 'right' },
];
const numericKeys: Array<keyof PurchaseOrderTrackingRow> = [
  'purchasePrice',
  'purchaseQty',
  'auditQty',
  'purchaseAmount',
  'auditAmount',
  'receivedQty',
  'receiptAmount',
  'unreceivedQty',
  'returnedQty',
  'returnAmount',
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
    ElMessage.error('采购订单状态跟踪表筛选项加载失败');
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

const formatCell = (row: PurchaseOrderTrackingRow, key: keyof PurchaseOrderTrackingRow) => {
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
  query.dateType = '订货日期';
  query.dateRange = [];
  query.receiptWarehouse = '';
  query.purchaseWarehouse = '';
  query.itemCode = '';
  query.supplier = '';
  query.purchaseOrderCode = '';
  query.documentStatus = '';
  query.receiveStatus = '';
  query.isGift = '全部';
  query.crossMonth = '全部';
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
    const property = column.property as keyof PurchaseOrderTrackingRow | undefined;
    if (!property || !numericKeys.includes(property) || property === 'purchasePrice') {
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
      <el-form-item label="日期">
        <el-select v-model="query.dateType" style="width: 140px">
          <el-option v-for="option in dateTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="范围时间">
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

      <el-form-item label="收货仓库">
        <el-tree-select
          v-model="query.receiptWarehouse"
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

      <el-form-item label="采购仓库">
        <el-tree-select
          v-model="query.purchaseWarehouse"
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

      <el-form-item label="采购单据号">
        <el-input v-model="query.purchaseOrderCode" clearable placeholder="请输入采购单据号" style="width: 180px" />
      </el-form-item>

      <el-form-item label="单据状态">
        <el-tree-select
          v-model="query.documentStatus"
          :data="documentStatusTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          placeholder="请选择"
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item label="收货状态">
        <el-tree-select
          v-model="query.receiveStatus"
          :data="receiveStatusTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          placeholder="请选择"
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item label="是否赠品">
        <el-select v-model="query.isGift" style="width: 120px">
          <el-option v-for="option in giftStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="查询跨月单据">
        <el-select v-model="query.crossMonth" style="width: 120px">
          <el-option v-for="option in crossMonthOptions" :key="option" :label="option" :value="option" />
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
      empty-text="暂无采购订单状态跟踪数据"
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
