<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchStockInoutSummaryReportApi } from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  fetchItemStatisticsTypesApi,
  type ItemCategoryTreeNode,
  type ItemStatisticsTypeRow,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticPeriod = '日' | '周' | '月' | '自定义周期';
type ItemStatus = '全部' | '启用' | '停用';
type UnitType = '库存单位' | '基准单位' | '采购单位';
type QueryScheme = '系统默认方案';
type TreeNode = { value: string; label: string; children?: TreeNode[] };
const INVENTORY_PERIOD_TYPE_DICT = 'inventory.period_type';

type StockInoutSummaryRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  statisticType: string;
  unit: string;
  inoutType: string;
  warehouse: string;
  warehouseType: string;
  oppositeOrg: string;
  oppositeWarehouse: string;
  inboundQty: number;
  inboundCostAmountTaxIncluded: number;
  inboundAvgCostTaxIncluded: number;
  inboundSettlementAmountTaxIncluded: number;
  inboundAvgSettlementTaxIncluded: number;
  outboundQty: number;
  outboundCostAmountTaxIncluded: number;
  outboundAvgCostTaxIncluded: number;
  outboundSettlementAmountTaxIncluded: number;
  outboundAvgSettlementTaxIncluded: number;
  statisticMode: string;
};

type ReportColumn = {
  key: keyof StockInoutSummaryRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const { optionsOf } = useDictionaryOptions([INVENTORY_PERIOD_TYPE_DICT]);
const periodTypeOptions = optionsOf(INVENTORY_PERIOD_TYPE_DICT);
const archiveOrgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));

const statisticPeriodOptions: StatisticPeriod[] = ['日', '周', '月', '自定义周期'];
const itemStatusTree: TreeNode[] = [
  { value: '全部', label: '全部' },
  { value: '启用', label: '启用' },
  { value: '停用', label: '停用' },
];
const unitTypeTree: TreeNode[] = [
  { value: '库存单位', label: '库存单位' },
  { value: '基准单位', label: '基准单位' },
  { value: '采购单位', label: '采购单位' },
];
const querySchemeOptions: QueryScheme[] = ['系统默认方案'];
const warehouseTypeTree: TreeNode[] = [
  { value: '普通仓库', label: '普通仓库' },
  { value: '出品及生产部门', label: '出品及生产部门' },
  { value: '行政部门', label: '行政部门' },
];
const inoutTypeTree: TreeNode[] = [
  {
    value: '入库',
    label: '入库',
    children: [
      { value: '采购入库', label: '采购入库' },
      { value: '其他入库', label: '其他入库' },
      { value: '生产入库', label: '生产入库' },
    ],
  },
  {
    value: '出库',
    label: '出库',
    children: [
      { value: '采购退货出库', label: '采购退货出库' },
      { value: '其他出库', label: '其他出库' },
      { value: '客户销售出库', label: '客户销售出库' },
    ],
  },
];
const inoutDirectionTree: TreeNode[] = [
  { value: '入库', label: '入库' },
  { value: '出库', label: '出库' },
];
const oppositeOrgTree: TreeNode[] = [
  { value: '总部配送中心', label: '总部配送中心' },
  { value: '华北门店', label: '华北门店' },
  { value: '鲜达食品', label: '鲜达食品' },
];

const query = reactive({
  statisticPeriod: '日' as StatisticPeriod,
  dateRange: [] as string[],
  periodType: 'MONTH',
  periodStartDate: '',
  warehouse: '',
  warehouseType: '',
  itemCode: '',
  itemCategory: '',
  statisticType: '',
  itemStatus: '全部' as ItemStatus,
  inoutType: '',
  inoutDirection: '',
  oppositeOrg: '',
  unitType: '库存单位' as UnitType,
  queryScheme: '系统默认方案' as QueryScheme,
});

const loading = ref(false);
const optionLoading = ref(false);
const itemCategoryTree = ref<TreeNode[]>([]);
const statisticTypeTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const tableRows = ref<StockInoutSummaryRow[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const quantityKeys: Array<keyof StockInoutSummaryRow> = ['inboundQty', 'outboundQty'];
const moneyKeys: Array<keyof StockInoutSummaryRow> = [
  'inboundCostAmountTaxIncluded',
  'inboundAvgCostTaxIncluded',
  'inboundSettlementAmountTaxIncluded',
  'inboundAvgSettlementTaxIncluded',
  'outboundCostAmountTaxIncluded',
  'outboundAvgCostTaxIncluded',
  'outboundSettlementAmountTaxIncluded',
  'outboundAvgSettlementTaxIncluded',
];
const numericKeys = [...quantityKeys, ...moneyKeys];

const columns: ReportColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'statisticType', label: '统计类型', minWidth: 120 },
  { key: 'statisticMode', label: '统计方式', minWidth: 110 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'inoutType', label: '出入库类型', minWidth: 130 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'warehouseType', label: '仓库类型', minWidth: 120 },
  { key: 'oppositeOrg', label: '对方机构', minWidth: 140 },
  { key: 'oppositeWarehouse', label: '对方仓库', minWidth: 130 },
  { key: 'inboundQty', label: '入库数量', minWidth: 110, align: 'right' },
  { key: 'inboundCostAmountTaxIncluded', label: '入库成本金额（含税）', minWidth: 180, align: 'right' },
  { key: 'inboundAvgCostTaxIncluded', label: '入库成本均价（含税）', minWidth: 180, align: 'right' },
  { key: 'inboundSettlementAmountTaxIncluded', label: '入库结算金额（含税）', minWidth: 180, align: 'right' },
  { key: 'inboundAvgSettlementTaxIncluded', label: '入库结算均价（含税）', minWidth: 180, align: 'right' },
  { key: 'outboundQty', label: '出库数量', minWidth: 110, align: 'right' },
  { key: 'outboundCostAmountTaxIncluded', label: '出库成本金额（含税）', minWidth: 180, align: 'right' },
  { key: 'outboundAvgCostTaxIncluded', label: '出库成本均价（含税）', minWidth: 180, align: 'right' },
  { key: 'outboundSettlementAmountTaxIncluded', label: '出库结算金额（含税）', minWidth: 180, align: 'right' },
  { key: 'outboundAvgSettlementTaxIncluded', label: '出库结算均价（含税）', minWidth: 180, align: 'right' },
];

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
    statisticTypeTree.value = statisticRows.map((row) => ({ value: row.name, label: `${row.code} / ${row.name}` }));
    itemOptions.value = itemRows.map((row) => ({ value: row.code, label: `${row.code} / ${row.name}` }));
  } catch {
    itemCategoryTree.value = [];
    statisticTypeTree.value = [];
    itemOptions.value = [];
    ElMessage.error('库存进出汇总表筛选项加载失败');
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
    const page = await fetchStockInoutSummaryReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      dateDimension: query.statisticPeriod,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      periodType: query.periodType,
      periodStartDate: query.periodStartDate,
      warehouse: query.warehouse || undefined,
      warehouseType: query.warehouseType || undefined,
      itemCategory: query.itemCategory || undefined,
      itemKeyword: query.itemCode || undefined,
      statisticType: query.statisticType || undefined,
      itemStatus: query.itemStatus === '全部' ? undefined : query.itemStatus,
      inoutType: query.inoutType || undefined,
      inoutDirection: query.inoutDirection || undefined,
      oppositeOrg: query.oppositeOrg || undefined,
      unitType: query.unitType,
      queryScheme: query.queryScheme,
    }, orgId);
    tableRows.value = page.list ?? [];
    total.value = Number(page.total ?? 0);
  } finally {
    loading.value = false;
  }
};

const formatNumber = (value: number, digits = 4) => Number.isFinite(value) ? value.toFixed(digits) : '0.0000';
const formatMoney = (value: number) => Number.isFinite(value) ? value.toFixed(2) : '0.00';
const formatCell = (row: StockInoutSummaryRow, key: keyof StockInoutSummaryRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return moneyKeys.includes(key) ? formatMoney(value) : formatNumber(value);
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.statisticPeriod = '日';
  query.dateRange = [];
  query.periodType = 'MONTH';
  query.periodStartDate = '';
  query.warehouse = '';
  query.warehouseType = '';
  query.itemCode = '';
  query.itemCategory = '';
  query.statisticType = '';
  query.itemStatus = '全部';
  query.inoutType = '';
  query.inoutDirection = '';
  query.oppositeOrg = '';
  query.unitType = '库存单位';
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

const summaryTotals = computed(() => numericKeys.reduce<Record<string, number>>((totals, key) => {
  totals[key] = tableRows.value.reduce((sum, row) => sum + Number(row[key] ?? 0), 0);
  return totals;
}, {}));

const getSummaries = ({ columns: tableColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return tableColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof StockInoutSummaryRow | undefined;
    if (!property || !numericKeys.includes(property)) {
      return '';
    }
    const value = summaryTotals.value[property] ?? 0;
    return moneyKeys.includes(property) ? formatMoney(value) : formatNumber(value);
  });
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  async () => {
    query.dateRange = [];
    query.periodType = 'MONTH';
    query.periodStartDate = '';
    query.warehouse = '';
    query.warehouseType = '';
    query.itemCode = '';
    query.itemCategory = '';
    query.statisticType = '';
    query.itemStatus = '全部';
    query.inoutType = '';
    query.inoutDirection = '';
    query.oppositeOrg = '';
    currentPage.value = 1;
    tableRows.value = [];
    total.value = 0;
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
      <el-form-item label="统计周期">
        <el-radio-group v-model="query.statisticPeriod">
          <el-radio-button v-for="option in statisticPeriodOptions" :key="option" :label="option" :value="option" />
        </el-radio-group>
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
      <el-form-item label="期初周期">
        <el-select v-model="query.periodType" style="width: 130px">
          <el-option
            v-for="option in periodTypeOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="周期开始">
        <el-date-picker
          v-model="query.periodStartDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="仓库">
        <el-tree-select v-model="query.warehouse" :data="warehouseTree" clearable check-strictly default-expand-all placeholder="请选择" style="width: 180px" />
      </el-form-item>
      <el-form-item label="仓库类型">
        <el-tree-select v-model="query.warehouseType" :data="warehouseTypeTree" clearable check-strictly default-expand-all placeholder="请选择" style="width: 160px" />
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemCode" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 200px">
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品类别">
        <el-tree-select v-model="query.itemCategory" :data="itemCategoryTree" :loading="optionLoading" clearable check-strictly default-expand-all placeholder="请选择" style="width: 180px" />
      </el-form-item>
      <el-form-item label="统计类型">
        <el-tree-select v-model="query.statisticType" :data="statisticTypeTree" :loading="optionLoading" clearable check-strictly default-expand-all placeholder="请选择" style="width: 180px" />
      </el-form-item>
      <el-form-item label="物品状态">
        <el-tree-select v-model="query.itemStatus" :data="itemStatusTree" clearable check-strictly placeholder="请选择" style="width: 130px" />
      </el-form-item>
      <el-form-item label="出入库类型">
        <el-tree-select v-model="query.inoutType" :data="inoutTypeTree" clearable check-strictly default-expand-all placeholder="请选择" style="width: 180px" />
      </el-form-item>
      <el-form-item label="出入库方向">
        <el-tree-select v-model="query.inoutDirection" :data="inoutDirectionTree" clearable check-strictly placeholder="请选择" style="width: 140px" />
      </el-form-item>
      <el-form-item label="对方机构">
        <el-tree-select v-model="query.oppositeOrg" :data="oppositeOrgTree" clearable check-strictly placeholder="请选择" style="width: 180px" />
      </el-form-item>
      <el-form-item label="单位类型">
        <el-tree-select v-model="query.unitType" :data="unitTypeTree" check-strictly placeholder="请选择" style="width: 130px" />
      </el-form-item>
      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="handleReset"><el-icon><RefreshRight /></el-icon>重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonTableSection
      :data="tableRows"
      :loading="loading"
      show-summary
      :summary-method="getSummaries"
      height="520"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :align="column.align ?? 'left'"
        :fixed="column.fixed"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ formatCell(row, column.key) }}</template>
      </el-table-column>
      <template #footer>
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          small
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </template>
    </CommonTableSection>
  </section>
</template>
