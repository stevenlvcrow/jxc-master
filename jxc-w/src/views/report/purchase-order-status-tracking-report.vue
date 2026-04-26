<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import {
  fetchPurchaseOrderStatusTrackingReportApi,
  type PurchaseOrderStatusTrackingReportRow,
} from '@/api/modules/purchase';
import { resolveArchiveOrgId } from '@/views/items/org';

type DateType = '订货日期' | '期望到货日期' | '收货日期';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseOrderTrackingRow = PurchaseOrderStatusTrackingReportRow;
type ReportColumn = {
  key: keyof PurchaseOrderTrackingRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right' | 'center';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const COMMON_YES_NO_DICT = 'common.yes_no';
const { optionsOf } = useDictionaryOptions([INVENTORY_DOCUMENT_STATUS_DICT, COMMON_YES_NO_DICT]);
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const dateTypeOptions: DateType[] = ['订货日期', '期望到货日期', '收货日期'];
const receiveStatusTree: TreeNode[] = [
  { value: '未收货', label: '未收货' },
  { value: '部分收货', label: '部分收货' },
  { value: '已收货', label: '已收货' },
];
const documentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);
const yesNoOptions = optionsOf(COMMON_YES_NO_DICT, { enabled: true, label: '全部', value: '全部' });
const documentStatusTree = computed<TreeNode[]>(() => documentStatusOptions.value.map((item) => ({
  value: item.itemCode,
  label: item.itemLabel,
})));

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
  isGift: '全部',
  crossMonth: '全部',
});

const loading = ref(false);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const tableRows = ref<PurchaseOrderTrackingRow[]>([]);

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
    const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
    if (!orgId) {
      tableRows.value = [];
      total.value = 0;
      return;
    }
    const page = await fetchPurchaseOrderStatusTrackingReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      dateType: query.dateType,
      receiptWarehouse: query.receiptWarehouse || undefined,
      purchaseWarehouse: query.purchaseWarehouse || undefined,
      itemCode: query.itemCode || undefined,
      supplier: query.supplier || undefined,
      purchaseOrderCode: query.purchaseOrderCode || undefined,
      documentStatus: query.documentStatus || undefined,
      receiveStatus: query.receiveStatus || undefined,
      isGift: query.isGift === '全部' ? undefined : query.isGift,
      crossMonth: query.crossMonth === '全部' ? undefined : query.crossMonth,
    }, orgId);
    tableRows.value = page.list ?? [];
    total.value = Number(page.total ?? 0);
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
    tableRows.value = [];
    total.value = 0;
    currentPage.value = 1;
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
          <el-option
            v-for="option in yesNoOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="查询跨月单据">
        <el-select v-model="query.crossMonth" style="width: 120px">
          <el-option
            v-for="option in yesNoOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
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
