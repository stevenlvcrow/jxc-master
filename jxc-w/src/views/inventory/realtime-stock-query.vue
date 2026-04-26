<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import {
  fetchRealtimeStockReportApi,
  type RealtimeStockReportRow,
} from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemStatisticsTypesApi,
  fetchItemsApi,
  type ItemStatisticsTypeRow,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type DimensionType = '仓库维度' | '物品维度';
type QueryScheme = '系统默认方案';
type ItemStatus = '启用' | '停用';
type UnitType = '库存单位';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type ItemCategoryTreeSource = {
  label: string;
  children?: ItemCategoryTreeSource[];
};

type ReportRow = {
  key: string;
  warehouseName: string;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  statType: string;
  unit: string;
  unitConversionRate: number;
  itemVolume: number;
  itemWeight: number;
  stockQty: number;
  stockAmount: number;
  stockAmountExTax: number;
  stockTaxAmount: number;
  avgCost: number;
  avgCostExTax: number;
};

type DisplayColumn = {
  key: keyof ReportRow;
  label: string;
  minWidth: number;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const archiveOrgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));

const dimensionOptions: DimensionType[] = ['仓库维度', '物品维度'];
const querySchemeOptions: QueryScheme[] = ['系统默认方案'];
const statusOptions: ItemStatus[] = ['启用', '停用'];
const unitTypeOptions: UnitType[] = ['库存单位'];

const query = reactive({
  dimension: '物品维度' as DimensionType,
  warehouse: '',
  category: '',
  statType: '',
  item: '',
  status: '启用' as ItemStatus,
  unitType: '库存单位' as UnitType,
  hideZeroStock: false,
  hideZeroTheoretical: false,
  scheme: '系统默认方案' as QueryScheme,
});

const loading = ref(false);
const exportLoading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const itemCategoryTree = ref<TreeNode[]>([]);
const statTypeTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const rawRows = ref<ReportRow[]>([]);
const isWarehouseDimension = computed(() => query.dimension === '仓库维度');

const reportColumns: DisplayColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'spec', label: '规格型号', minWidth: 120 },
  { key: 'category', label: '物品类别', minWidth: 120 },
  { key: 'statType', label: '统计类型', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 100 },
  { key: 'unitConversionRate', label: '与基准单位的换算率', minWidth: 160 },
  { key: 'itemVolume', label: '物品体积', minWidth: 110 },
  { key: 'itemWeight', label: '物品重量', minWidth: 110 },
  { key: 'warehouseName', label: '仓库', minWidth: 140 },
  { key: 'stockQty', label: '库存量', minWidth: 110 },
  { key: 'stockAmount', label: '库存金额', minWidth: 120 },
  { key: 'stockAmountExTax', label: '库存金额（不含税）', minWidth: 160 },
  { key: 'stockTaxAmount', label: '库存税额', minWidth: 120 },
  { key: 'avgCostExTax', label: '库存均价（不含税）', minWidth: 160 },
];
const visibleColumns = computed(() => reportColumns);
const tableData = computed(() => {
  const pageRows = filteredRows.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return pageRows.slice(start, start + pageSize.value);
});

const normalizeText = (value: unknown) => (typeof value === 'string' ? value.trim() : '');
const parseNumber = (value: unknown) => Number.isFinite(Number(value)) ? Number(value) : 0;
const formatNumber = (value: number, digits = 4) => Number.isFinite(value) ? value.toFixed(digits) : '0.0000';
const formatMoney = (value: number) => formatNumber(value, 2);
const formatCell = (value: unknown) => {
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : formatNumber(value);
  }
  return String(value ?? '-');
};
const toCsvCell = (value: unknown) => `"${String(value ?? '').replace(/"/g, '""')}"`;

const fetchAllPages = async <T,>(
  loader: (pageNum: number, pageSize: number) => Promise<{ list?: T[]; total?: number; pageSize?: number }>,
  pageSizeValue = 200,
) => {
  const rows: T[] = [];
  let pageNum = 1;
  let totalValue: number;
  do {
    const page = await loader(pageNum, pageSizeValue);
    const list = Array.isArray(page.list) ? page.list : [];
    rows.push(...list);
    totalValue = Number(page.total ?? rows.length);
    if (!list.length || Number(page.pageSize ?? 0) <= 0) {
      break;
    }
    pageNum += 1;
  } while (rows.length < totalValue);
  return rows;
};

const loadCategoryTree = async (orgId: string) => {
  const rows = await fetchItemCategoryTreeApi(orgId);
  const normalizeTree = (nodes: ItemCategoryTreeSource[]): TreeNode[] =>
    nodes.map((node) => ({
      value: node.label,
      label: node.label,
      children: node.children?.length ? normalizeTree(node.children) : undefined,
    }));
  itemCategoryTree.value = normalizeTree(rows ?? []);
};

const loadStatTypeTree = async (orgId: string) => {
  const rows = await fetchAllPages<ItemStatisticsTypeRow>(
    (pageNum, pageSizeValue) => fetchItemStatisticsTypesApi({ pageNo: pageNum, pageSize: pageSizeValue }, orgId),
  );
  const grouped = new Map<string, Array<{ value: string; label: string }>>();
  rows.forEach((row) => {
    const group = normalizeText(row.statisticsCategory) || '未分类';
    if (!grouped.has(group)) {
      grouped.set(group, []);
    }
    grouped.get(group)?.push({ value: row.name, label: `${row.code} / ${row.name}` });
  });
  statTypeTree.value = Array.from(grouped.entries()).map(([label, children]) => ({
    value: label,
    label,
    children,
  }));
};

const loadItemOptions = async (orgId: string) => {
  const rows = await fetchAllPages<ItemVO>(
    (pageNum, pageSizeValue) => fetchItemsApi({
      pageNo: pageNum,
      pageSize: pageSizeValue,
      status: '全部',
      itemType: '全部',
    }, orgId),
  );
  itemOptions.value = rows.map((row) => ({ value: row.code, label: `${row.code} / ${row.name}` }));
  return rows;
};

const buildReportRows = async () => {
  if (!archiveOrgId.value) {
    rawRows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const orgId = archiveOrgId.value;
    const [items, page] = await Promise.all([
      loadItemOptions(orgId),
      fetchRealtimeStockReportApi({
        pageNo: 1,
        pageSize: 200,
        warehouse: isWarehouseDimension.value ? query.warehouse || undefined : undefined,
        itemCategory: query.category || undefined,
        itemCode: query.item || undefined,
        itemStatus: query.status || undefined,
      }, orgId),
    ]);
    await Promise.all([
      loadCategoryTree(orgId),
      loadStatTypeTree(orgId),
      loadWarehouseTree(),
    ]);

    const itemMap = new Map(items.map((item) => [item.code, item]));
    const grouped = new Map<string, ReportRow>();

    (page.list ?? []).forEach((balance: RealtimeStockReportRow) => {
      const item = itemMap.get(balance.itemCode);
      const rowKey = `${balance.warehouse}|${balance.itemCode}`;
      const existing = grouped.get(rowKey);
      if (!existing) {
        grouped.set(rowKey, {
          key: rowKey,
          warehouseName: balance.warehouse,
          itemCode: balance.itemCode,
          itemName: balance.itemName,
          spec: balance.spec,
          category: balance.itemCategory,
          statType: normalizeText(balance.statisticType || item?.statType),
          unit: balance.unit,
          unitConversionRate: parseNumber(balance.unitConversionRate),
          itemVolume: parseNumber(balance.itemVolume),
          itemWeight: parseNumber(balance.itemWeight),
          stockQty: parseNumber(balance.currentStock),
          stockAmount: parseNumber(balance.costAmount),
          stockAmountExTax: parseNumber(balance.costAmountExTax),
          stockTaxAmount: parseNumber(balance.taxAmount),
          avgCost: parseNumber(balance.avgCost),
          avgCostExTax: parseNumber(balance.avgCostExTax),
        });
      } else {
        existing.stockQty += parseNumber(balance.currentStock);
        existing.stockAmount += parseNumber(balance.costAmount);
        existing.stockAmountExTax += parseNumber(balance.costAmountExTax);
        existing.stockTaxAmount += parseNumber(balance.taxAmount);
        existing.avgCost = existing.stockQty > 0 ? existing.stockAmount / existing.stockQty : 0;
        existing.avgCostExTax = existing.stockQty > 0 ? existing.stockAmountExTax / existing.stockQty : 0;
      }
    });

    const filtered = Array.from(grouped.values())
      .filter((row) => !query.hideZeroStock || row.stockQty !== 0)
      .filter((row) => !query.hideZeroTheoretical || row.stockQty !== 0);

    rawRows.value = filtered;
    total.value = filtered.length;
    currentPage.value = 1;
  } catch {
    rawRows.value = [];
    total.value = 0;
    ElMessage.error('实时库存查询表加载失败');
  } finally {
    loading.value = false;
  }
};

const filteredRows = computed(() => rawRows.value);

const summaryTotals = computed(() => {
  const rows = filteredRows.value;
  const stockQty = rows.reduce((sum, row) => sum + row.stockQty, 0);
  const stockAmount = rows.reduce((sum, row) => sum + row.stockAmount, 0);
  const stockAmountExTax = rows.reduce((sum, row) => sum + row.stockAmountExTax, 0);
  const stockTaxAmount = rows.reduce((sum, row) => sum + row.stockTaxAmount, 0);
  return {
    stockQty,
    stockAmount,
    stockAmountExTax,
    stockTaxAmount,
    avgCostExTax: stockQty > 0 ? stockAmountExTax / stockQty : 0,
  };
});

const summaryCells = computed(() => {
  const totals = summaryTotals.value;
  const labels = [
    `库存量：${formatNumber(totals.stockQty, 4)}`,
    `库存金额：${formatMoney(totals.stockAmount)}`,
    `库存金额（不含税）：${formatMoney(totals.stockAmountExTax)}`,
    `库存税额：${formatMoney(totals.stockTaxAmount)}`,
    `库存均价（不含税）：${formatNumber(totals.avgCostExTax, 4)}`,
  ];
  return labels;
});

const handleSearch = async () => {
  currentPage.value = 1;
  await buildReportRows();
};

const handleExport = async () => {
  exportLoading.value = true;
  try {
    const rows = filteredRows.value;
    const headers = reportColumns.map((column) => column.label);
    const lines = [headers.map(toCsvCell).join(',')];
    rows.forEach((row) => {
      const cells = reportColumns.map((column) => formatCell(row[column.key]));
      lines.push(cells.map(toCsvCell).join(','));
    });
    const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = '实时库存查询表.csv';
    document.body.appendChild(link);
    link.click();
    URL.revokeObjectURL(link.href);
    document.body.removeChild(link);
    ElMessage.success('导出成功');
  } finally {
    exportLoading.value = false;
  }
};

const handleReset = async () => {
  query.dimension = '物品维度';
  query.warehouse = '';
  query.category = '';
  query.statType = '';
  query.item = '';
  query.status = '启用';
  query.unitType = '库存单位';
  query.hideZeroStock = false;
  query.hideZeroTheoretical = false;
  query.scheme = '系统默认方案';
  currentPage.value = 1;
  await buildReportRows();
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

watch(
  () => query.dimension,
  async () => {
    if (!isWarehouseDimension.value) {
      query.warehouse = '';
    }
    await buildReportRows();
  },
);

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  async () => {
    await buildReportRows();
  },
);

onMounted(async () => {
  await buildReportRows();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="维度">
        <el-select v-model="query.dimension" style="width: 120px">
          <el-option v-for="option in dimensionOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isWarehouseDimension" label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="物品类别">
        <el-tree-select
          v-model="query.category"
          :data="itemCategoryTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="统计类型">
        <el-tree-select
          v-model="query.statType"
          :data="statTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.item" clearable filterable style="width: 180px" placeholder="请选择物品">
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品状态">
        <el-select v-model="query.status" style="width: 120px">
          <el-option v-for="option in statusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="计量单位类型">
        <el-select v-model="query.unitType" style="width: 120px">
          <el-option v-for="option in unitTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="不显示库存量为0的物品">
        <el-checkbox v-model="query.hideZeroStock" />
      </el-form-item>
      <el-form-item label="不显示实时库存为0的物品">
        <el-checkbox v-model="query.hideZeroTheoretical" />
      </el-form-item>
      <el-form-item label="查询方案">
        <el-select v-model="query.scheme" style="width: 140px">
          <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button :loading="exportLoading" @click="handleExport">
          导出
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonTableSection
      :data="tableData"
      :loading="loading"
      :height="420"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in visibleColumns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatCell(row[column.key]) }}
        </template>
      </el-table-column>
      <template #append>
        <div class="realtime-stock-summary-row">
          <span class="summary-title">合计</span>
          <span v-for="cell in summaryCells" :key="cell" class="summary-cell">{{ cell }}</span>
        </div>
      </template>
    </CommonTableSection>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ total }} 条</div>
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

<style scoped>
.realtime-stock-summary-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 32px;
  padding: 6px 12px;
  border-top: 1px solid #cfd6e4;
  background: #f8fafc;
  color: #1f2937;
  font-size: 12px;
  white-space: nowrap;
  overflow-x: auto;
}

.summary-title {
  flex: 0 0 auto;
  font-weight: 600;
}

.summary-cell {
  flex: 0 0 auto;
}
</style>
