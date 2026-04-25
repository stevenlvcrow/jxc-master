<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchInventoryInoutSummaryReportApi } from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  fetchItemStatisticsTypesApi,
  type ItemCategoryTreeNode,
  type ItemStatisticsTypeRow,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticDimension = '物品' | '物品类别';
type ItemStatus = '全部' | '启用' | '停用';
type UnitType = '库存单位';
type QueryScheme = '系统默认方案';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type InoutSummaryRow = {
  id: string;
  itemCode?: string;
  itemName?: string;
  specModel?: string;
  itemCategory: string;
  statisticType?: string;
  unit?: string;
  orgName: string;
  orgCode: string;
  warehouse: string;
  warehouseType: string;
  openingQty: number;
  openingCostAmountExTax: number;
  openingAvgCostExTax: number;
  inboundQty: number;
  inboundCostAmountExTax: number;
  inboundAvgCostExTax: number;
  outboundQty: number;
  outboundCostAmountExTax: number;
  outboundAvgCostExTax: number;
  closingQty: number;
  closingCostAmountExTax: number;
  closingAvgCostExTax: number;
  inventoryProfitLossQty: number;
  inventoryProfitLossCostAmountTaxIncluded: number;
  inventoryProfitLossCostAmountExTax: number;
  inventoryCheckQty: number;
  inventoryCheckCostAmountTaxIncluded: number;
  closingCheckDiffQty: number;
  closingCheckDiffAmountExTax: number;
  returnDifferenceQty: number;
  returnDifferenceCostAmountExTax: number;
};

type ReportColumn = {
  key: keyof InoutSummaryRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const archiveOrgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));

const statisticDimensionOptions: StatisticDimension[] = ['物品', '物品类别'];
const itemStatusOptions: ItemStatus[] = ['全部', '启用', '停用'];
const unitTypeOptions: UnitType[] = ['库存单位'];
const querySchemeOptions: QueryScheme[] = ['系统默认方案'];
const warehouseTypeTree: TreeNode[] = [
  { value: '出品及生产部门', label: '出品及生产部门' },
  { value: '行政部门', label: '行政部门' },
  { value: '普通仓库', label: '普通仓库' },
];
const inoutTypeTree: TreeNode[] = [
  {
    value: '入库',
    label: '入库',
    children: [
      { value: '采购入库', label: '采购入库' },
      { value: '其他入库', label: '其他入库' },
      { value: '生产入库', label: '生产入库' },
      { value: '客户退货入库', label: '客户退货入库' },
    ],
  },
  {
    value: '出库',
    label: '出库',
    children: [
      { value: '采购退货出库', label: '采购退货出库' },
      { value: '报损出库', label: '报损出库' },
      { value: '其他出库', label: '其他出库' },
      { value: '客户销售出库', label: '客户销售出库' },
      { value: '移库出库', label: '移库出库' },
      { value: '菜品消耗出库', label: '菜品消耗出库' },
    ],
  },
];

const query = reactive({
  statisticDimension: '物品' as StatisticDimension,
  warehouse: '',
  warehouseType: '',
  dateRange: [] as string[],
  itemCode: '',
  itemCategory: '',
  statisticType: '',
  itemStatus: '全部' as ItemStatus,
  inoutType: '',
  unitType: '库存单位' as UnitType,
  hideNoInout: false,
  queryScheme: '系统默认方案' as QueryScheme,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<InoutSummaryRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const statisticTypeTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const isItemDimension = computed(() => query.statisticDimension === '物品');

const quantityKeys: Array<keyof InoutSummaryRow> = [
  'openingQty',
  'inboundQty',
  'outboundQty',
  'closingQty',
  'inventoryProfitLossQty',
  'inventoryCheckQty',
  'closingCheckDiffQty',
  'returnDifferenceQty',
];
const moneyKeys: Array<keyof InoutSummaryRow> = [
  'openingCostAmountExTax',
  'openingAvgCostExTax',
  'inboundCostAmountExTax',
  'inboundAvgCostExTax',
  'outboundCostAmountExTax',
  'outboundAvgCostExTax',
  'closingCostAmountExTax',
  'closingAvgCostExTax',
  'inventoryProfitLossCostAmountTaxIncluded',
  'inventoryProfitLossCostAmountExTax',
  'inventoryCheckCostAmountTaxIncluded',
  'closingCheckDiffAmountExTax',
  'returnDifferenceCostAmountExTax',
];
const numericKeys = [...quantityKeys, ...moneyKeys];

const baseMetricColumns: ReportColumn[] = [
  { key: 'orgName', label: '机构', minWidth: 130 },
  { key: 'orgCode', label: '机构编码', minWidth: 120 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'warehouseType', label: '仓库类型', minWidth: 120 },
  { key: 'openingQty', label: '期初数量', minWidth: 110, align: 'right' },
  { key: 'openingCostAmountExTax', label: '期初成本金额 (不含税)', minWidth: 170, align: 'right' },
  { key: 'openingAvgCostExTax', label: '期初成本均价 (不含税)', minWidth: 170, align: 'right' },
  { key: 'inboundQty', label: '入库合计数量', minWidth: 120, align: 'right' },
  { key: 'inboundCostAmountExTax', label: '入库合计成本金额 (不含税)', minWidth: 190, align: 'right' },
  { key: 'inboundAvgCostExTax', label: '入库合计成本均价 (不含税)', minWidth: 190, align: 'right' },
  { key: 'outboundQty', label: '出库合计数量', minWidth: 120, align: 'right' },
  { key: 'outboundCostAmountExTax', label: '出库合计成本金额 (不含税)', minWidth: 190, align: 'right' },
  { key: 'outboundAvgCostExTax', label: '出库合计成本均价 (不含税)', minWidth: 190, align: 'right' },
  { key: 'closingQty', label: '期末数量', minWidth: 110, align: 'right' },
  { key: 'closingCostAmountExTax', label: '期末成本金额 (不含税)', minWidth: 170, align: 'right' },
  { key: 'closingAvgCostExTax', label: '期末成本均价 (不含税)', minWidth: 170, align: 'right' },
  { key: 'inventoryProfitLossQty', label: '盘盈亏数量', minWidth: 120, align: 'right' },
  { key: 'inventoryProfitLossCostAmountTaxIncluded', label: '盘盈亏成本金额 (含税)', minWidth: 180, align: 'right' },
  { key: 'inventoryProfitLossCostAmountExTax', label: '盘盈亏成本金额 (不含税)', minWidth: 190, align: 'right' },
  { key: 'inventoryCheckQty', label: '盘点数量', minWidth: 110, align: 'right' },
  { key: 'inventoryCheckCostAmountTaxIncluded', label: '盘点成本金额 (含税)', minWidth: 170, align: 'right' },
  { key: 'closingCheckDiffQty', label: '期末盘点差异数量', minWidth: 150, align: 'right' },
  { key: 'closingCheckDiffAmountExTax', label: '期末盘点差异金额 (不含税)', minWidth: 200, align: 'right' },
  { key: 'returnDifferenceQty', label: '退返货差异金额数量', minWidth: 170, align: 'right' },
  { key: 'returnDifferenceCostAmountExTax', label: '退返货差异金额成本金额 (不含税)', minWidth: 240, align: 'right' },
];

const itemColumns = computed<ReportColumn[]>(() => [
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'statisticType', label: '统计类型', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 90 },
  ...baseMetricColumns,
]);

const categoryColumns = computed<ReportColumn[]>(() => [
  { key: 'itemCategory', label: '物品类别', minWidth: 140, fixed: 'left' },
  ...baseMetricColumns,
]);

const visibleColumns = computed(() => (isItemDimension.value ? itemColumns.value : categoryColumns.value));

const formatNumber = (value: number, digits = 4) => Number.isFinite(value) ? value.toFixed(digits) : '0.0000';
const formatMoney = (value: number) => Number.isFinite(value) ? value.toFixed(2) : '0.00';
const formatCell = (row: InoutSummaryRow, key: keyof InoutSummaryRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return moneyKeys.includes(key) ? formatMoney(value) : formatNumber(value);
};

const normalizeCategoryTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeCategoryTree(node.children) : undefined,
}));

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
  if (!archiveOrgId.value) {
    itemCategoryTree.value = [];
    statisticTypeTree.value = [];
    itemOptions.value = [];
    await loadWarehouseTree();
    return;
  }
  optionLoading.value = true;
  try {
    const orgId = archiveOrgId.value;
    const [categoryRows, statisticRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId),
      fetchAllPages<ItemStatisticsTypeRow>((pageNo, pageSizeValue) =>
        fetchItemStatisticsTypesApi({ pageNo, pageSize: pageSizeValue }, orgId)),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId)),
      loadWarehouseTree(),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    statisticTypeTree.value = statisticRows.map((row) => ({
      value: row.name,
      label: `${row.code} / ${row.name}`,
    }));
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    statisticTypeTree.value = [];
    itemOptions.value = [];
    ElMessage.error('出入库汇总表筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const fetchReport = async () => {
  loading.value = true;
  try {
    const orgId = archiveOrgId.value;
    if (!orgId) {
      tableRows.value = [];
      total.value = 0;
      return;
    }
    const page = await fetchInventoryInoutSummaryReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      statisticDimension: query.statisticDimension,
      warehouse: query.warehouse || undefined,
      warehouseType: query.warehouseType || undefined,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      itemCode: query.itemCode || undefined,
      itemCategory: query.itemCategory || undefined,
      statisticType: query.statisticType || undefined,
      itemStatus: query.itemStatus === '全部' ? undefined : query.itemStatus,
      inoutType: query.inoutType || undefined,
      unitType: query.unitType,
      hideNoInout: query.hideNoInout,
      queryScheme: query.queryScheme,
    }, orgId);
    tableRows.value = page.list ?? [];
    total.value = Number(page.total ?? 0);
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.statisticDimension = '物品';
  query.warehouse = '';
  query.warehouseType = '';
  query.dateRange = [];
  query.itemCode = '';
  query.itemCategory = '';
  query.statisticType = '';
  query.itemStatus = '全部';
  query.inoutType = '';
  query.unitType = '库存单位';
  query.hideNoInout = false;
  query.queryScheme = '系统默认方案';
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

const summaryTotals = computed(() => {
  return numericKeys.reduce<Record<string, number>>((totals, key) => {
    totals[key] = tableRows.value.reduce((sum, row) => sum + Number(row[key] ?? 0), 0);
    return totals;
  }, {});
});

const getSummaries = ({ columns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return columns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof InoutSummaryRow | undefined;
    if (!property || !numericKeys.includes(property)) {
      return '';
    }
    const value = summaryTotals.value[property] ?? 0;
    return moneyKeys.includes(property) ? formatMoney(value) : formatNumber(value);
  });
};

watch(
  () => query.statisticDimension,
  () => {
    if (!isItemDimension.value) {
      query.itemCode = '';
      query.statisticType = '';
      query.itemStatus = '全部';
    }
    currentPage.value = 1;
  },
);

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
      <el-form-item label="统计维度">
        <el-select v-model="query.statisticDimension" style="width: 140px">
          <el-option v-for="option in statisticDimensionOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="仓库类型">
        <el-tree-select
          v-model="query.warehouseType"
          :data="warehouseTypeTree"
          :props="{ label: 'label', value: 'value' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 160px"
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

      <el-form-item v-if="isItemDimension" label="物品">
        <el-select
          v-model="query.itemCode"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
          style="width: 200px"
        >
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="物品类别">
        <el-tree-select
          v-model="query.itemCategory"
          :data="itemCategoryTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          :loading="optionLoading"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item v-if="isItemDimension" label="统计类型">
        <el-tree-select
          v-model="query.statisticType"
          :data="statisticTypeTree"
          :props="{ label: 'label', value: 'value' }"
          :loading="optionLoading"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item v-if="isItemDimension" label="物品状态">
        <el-select v-model="query.itemStatus" style="width: 120px">
          <el-option v-for="option in itemStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="出入库类型">
        <el-tree-select
          v-model="query.inoutType"
          :data="inoutTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="单位类型">
        <el-select v-model="query.unitType" style="width: 140px">
          <el-option v-for="option in unitTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="不显示统计期间内未发生出入库的数据">
        <el-checkbox v-model="query.hideNoInout" />
      </el-form-item>

      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
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
      empty-text="暂无出入库汇总数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in visibleColumns"
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
