<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import {
  fetchInventoryBalancesApi,
  type InventoryBalanceRow,
} from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemDetailApi,
  fetchItemStatisticsTypesApi,
  fetchItemsApi,
  type ItemCreatePayload,
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
  unitRateText: string;
  volume: number;
  weight: number;
  stockQty: number;
  stockAmount: number;
  stockAmountExTax: number;
  taxAmount: number;
  avgPriceExTax: number;
  expectedInboundQty: number;
  expectedOutboundQty: number;
  theoreticalQty: number;
  theoreticalAmount: number;
  d1TheoreticalQty: number;
  d2TheoreticalQty: number;
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
  hideZeroStock: '',
  hideZeroTheoretical: '',
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

const warehouseColumns: DisplayColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'spec', label: '规格型号', minWidth: 120 },
  { key: 'category', label: '物品类别', minWidth: 120 },
  { key: 'statType', label: '统计类型', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 100 },
  { key: 'unitRateText', label: '与基准单位的换算率', minWidth: 180 },
  { key: 'volume', label: '物品体积', minWidth: 100 },
  { key: 'weight', label: '物品重量', minWidth: 100 },
  { key: 'warehouseName', label: '仓库', minWidth: 140 },
  { key: 'stockQty', label: '库存量', minWidth: 100 },
  { key: 'stockAmount', label: '库存金额', minWidth: 110 },
  { key: 'stockAmountExTax', label: '库存金额（不含税）', minWidth: 140 },
  { key: 'taxAmount', label: '库存税额', minWidth: 110 },
  { key: 'avgPriceExTax', label: '库存均价（不含税）', minWidth: 140 },
  { key: 'expectedInboundQty', label: '预计入库量', minWidth: 110 },
  { key: 'expectedOutboundQty', label: '预计出库量', minWidth: 110 },
  { key: 'theoreticalQty', label: '理论库存量', minWidth: 110 },
  { key: 'theoreticalAmount', label: '理论库存金额', minWidth: 140 },
  { key: 'd1TheoreticalQty', label: 'D+1 理论库存量', minWidth: 130 },
  { key: 'd2TheoreticalQty', label: 'D+2 理论库存量', minWidth: 130 },
];

const itemColumns: DisplayColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'spec', label: '规格型号', minWidth: 120 },
  { key: 'category', label: '物品类别', minWidth: 120 },
  { key: 'statType', label: '统计类型', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 100 },
  { key: 'unitRateText', label: '与基准单位的换算率', minWidth: 180 },
  { key: 'volume', label: '物品体积', minWidth: 100 },
  { key: 'weight', label: '物品重量', minWidth: 100 },
  { key: 'stockQty', label: '库存量', minWidth: 100 },
  { key: 'stockAmount', label: '库存金额', minWidth: 110 },
  { key: 'stockAmountExTax', label: '库存金额（不含税）', minWidth: 140 },
  { key: 'taxAmount', label: '库存税额', minWidth: 110 },
  { key: 'avgPriceExTax', label: '库存均价（不含税）', minWidth: 140 },
  { key: 'expectedInboundQty', label: '预计入库量', minWidth: 110 },
  { key: 'expectedOutboundQty', label: '预计出库量', minWidth: 110 },
  { key: 'theoreticalQty', label: '理论库存量', minWidth: 110 },
  { key: 'theoreticalAmount', label: '理论库存金额', minWidth: 140 },
];

const visibleColumns = computed(() => (isWarehouseDimension.value ? warehouseColumns : itemColumns));
const tableData = computed(() => {
  const pageRows = filteredRows.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return pageRows.slice(start, start + pageSize.value);
});

const normalizeText = (value: unknown) => (typeof value === 'string' ? value.trim() : '');
const parseNumber = (value: unknown) => {
  const text = normalizeText(value);
  if (!text) {
    return 0;
  }
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : 0;
};
const formatNumber = (value: number, digits = 4) => Number.isFinite(value) ? value.toFixed(digits) : '0.0000';
const formatMoney = (value: number) => formatNumber(value, 2);
const formatCell = (value: unknown) => {
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : formatNumber(value);
  }
  return String(value ?? '-');
};
const toCsvCell = (value: unknown) => `"${String(value ?? '').replace(/"/g, '""')}"`;
const isTruthyFilter = (value: string) => ['1', 'true', '是', 'yes', 'y'].includes(value.trim().toLowerCase());

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

const resolveUnitRateText = (detail: ItemCreatePayload | undefined, item: ItemVO) => {
  const baseUnit = normalizeText(detail?.unitSettingRows?.[0]?.unit) || normalizeText(item.baseUnit) || '基准单位';
  const stockUnit = normalizeText(detail?.defaultStockUnit) || normalizeText(item.stockUnit) || baseUnit;
  const stockUnitRow = detail?.unitSettingRows?.find((row) => normalizeText(row.unit) === stockUnit)
    ?? detail?.unitSettingRows?.[0];
  if (!stockUnitRow) {
    return `1${stockUnit}=1${baseUnit}`;
  }
  const from = parseNumber(stockUnitRow.convertFrom || '1');
  const to = parseNumber(stockUnitRow.convertTo || stockUnitRow.convertFrom || '1');
  const rate = from > 0 ? to / from : 1;
  return `1${stockUnit}=${formatNumber(rate, 4)}${baseUnit}`;
};

const loadBalanceRows = async (orgId: string) => {
  return fetchAllPages<InventoryBalanceRow>(
    (pageNum, pageSizeValue) => fetchInventoryBalancesApi({
      pageNum,
      pageSize: pageSizeValue,
      warehouse: isWarehouseDimension.value ? query.warehouse || undefined : undefined,
      itemName: query.item || undefined,
    }, orgId),
  );
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
    const [items, balances] = await Promise.all([
      loadItemOptions(orgId),
      loadBalanceRows(orgId),
    ]);
    await Promise.all([
      loadCategoryTree(orgId),
      loadStatTypeTree(orgId),
      loadWarehouseTree(),
    ]);

    const balanceItemCodes = Array.from(new Set(balances.map((row) => row.itemCode).filter((code) => Boolean(code))));
    const detailPairs = await Promise.all(
      balanceItemCodes.map(async (code) => {
        const item = items.find((row) => row.code === code);
        if (!item) {
          return [code, null] as const;
        }
        const detail = await fetchItemDetailApi(item.id, orgId).catch(() => null);
        return [code, detail] as const;
      }),
    );
    const detailMap = new Map<string, ItemCreatePayload>();
    detailPairs.forEach(([code, detail]) => {
      if (detail) {
        detailMap.set(code, detail);
      }
    });
    const itemMap = new Map(items.map((item) => [item.code, item]));

    const matchedItemCodes = new Set<string>();
    const grouped = new Map<string, ReportRow & { totalQty: number }>();

    balances.forEach((balance) => {
      const item = itemMap.get(balance.itemCode);
      if (!item) {
        return;
      }
      if (query.status && query.status !== '启用' && query.status !== '停用') {
        return;
      }
      if (query.category && normalizeText(item.category) !== normalizeText(query.category)) {
        return;
      }
      if (query.statType && normalizeText(item.statType) !== normalizeText(query.statType)) {
        return;
      }
      if (query.item && normalizeText(item.code) !== normalizeText(query.item)) {
        return;
      }
      if (query.status && normalizeText(item.status) !== normalizeText(query.status)) {
        return;
      }

      const qty = parseNumber(balance.quantity);
      const detail = detailMap.get(item.code);
      const unitPrice = parseNumber(detail?.productionRefCost ?? item.productionCost ?? detail?.suggestPurchasePrice ?? item.suggestPrice);
      const taxRateValue = parseNumber(detail?.taxRate);
      const taxRate = taxRateValue > 1 ? taxRateValue / 100 : taxRateValue;
      const stockAmount = qty * unitPrice;
      const stockAmountExTax = taxRate > 0 ? stockAmount / (1 + taxRate) : stockAmount;
      const taxAmount = stockAmount - stockAmountExTax;
      const avgPriceExTax = qty > 0 ? stockAmountExTax / qty : 0;
      const theoreticalQty = qty;
      const theoreticalAmount = theoreticalQty * avgPriceExTax;
      const rowKey = isWarehouseDimension.value
        ? `${balance.warehouse}|${item.code}`
        : item.code;
      const existing = grouped.get(rowKey);
      if (!existing) {
        grouped.set(rowKey, {
          key: rowKey,
          warehouseName: balance.warehouse,
          itemCode: item.code,
          itemName: item.name,
          spec: item.spec,
          category: item.category,
          statType: item.statType,
          unit: normalizeText(detail?.defaultStockUnit) || normalizeText(item.stockUnit) || '-',
          unitRateText: resolveUnitRateText(detail ?? undefined, item),
          volume: parseNumber(item.volume),
          weight: parseNumber(item.weight),
          stockQty: qty,
          stockAmount,
          stockAmountExTax,
          taxAmount,
          avgPriceExTax,
          expectedInboundQty: 0,
          expectedOutboundQty: 0,
          theoreticalQty,
          theoreticalAmount,
          d1TheoreticalQty: isWarehouseDimension.value ? 0 : 0,
          d2TheoreticalQty: isWarehouseDimension.value ? 0 : 0,
          totalQty: qty,
        });
      } else {
        existing.stockQty += qty;
        existing.stockAmount += stockAmount;
        existing.stockAmountExTax += stockAmountExTax;
        existing.taxAmount += taxAmount;
        existing.expectedInboundQty += 0;
        existing.expectedOutboundQty += 0;
        existing.theoreticalQty += theoreticalQty;
        existing.theoreticalAmount += theoreticalAmount;
        existing.totalQty += qty;
        if (isWarehouseDimension.value) {
          existing.d1TheoreticalQty = 0;
          existing.d2TheoreticalQty = 0;
        }
      }
      matchedItemCodes.add(item.code);
    });

    const filtered = Array.from(grouped.values())
      .filter((row) => !isTruthyFilter(query.hideZeroStock) || row.stockQty !== 0)
      .filter((row) => !isTruthyFilter(query.hideZeroTheoretical) || row.theoreticalQty !== 0);

    rawRows.value = filtered;
    total.value = filtered.length;
    currentPage.value = 1;

    if (!matchedItemCodes.size) {
      itemOptions.value = items.map((row) => ({ value: row.code, label: `${row.code} / ${row.name}` }));
    }
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
  const taxAmount = rows.reduce((sum, row) => sum + row.taxAmount, 0);
  const expectedInboundQty = rows.reduce((sum, row) => sum + row.expectedInboundQty, 0);
  const expectedOutboundQty = rows.reduce((sum, row) => sum + row.expectedOutboundQty, 0);
  const theoreticalQty = rows.reduce((sum, row) => sum + row.theoreticalQty, 0);
  const theoreticalAmount = rows.reduce((sum, row) => sum + row.theoreticalAmount, 0);
  const d1TheoreticalQty = rows.reduce((sum, row) => sum + row.d1TheoreticalQty, 0);
  const d2TheoreticalQty = rows.reduce((sum, row) => sum + row.d2TheoreticalQty, 0);
  return {
    stockQty,
    stockAmount,
    stockAmountExTax,
    taxAmount,
    avgPriceExTax: stockQty > 0 ? stockAmountExTax / stockQty : 0,
    expectedInboundQty,
    expectedOutboundQty,
    theoreticalQty,
    theoreticalAmount,
    d1TheoreticalQty,
    d2TheoreticalQty,
  };
});

const summaryCells = computed(() => {
  const totals = summaryTotals.value;
  const labels = [
    `库存量：${formatNumber(totals.stockQty, 4)}`,
    `库存金额：${formatMoney(totals.stockAmount)}`,
    `库存金额（不含税）：${formatMoney(totals.stockAmountExTax)}`,
    `库存税额：${formatMoney(totals.taxAmount)}`,
    `库存均价（不含税）：${formatMoney(totals.avgPriceExTax)}`,
    `预计入库量：${formatNumber(totals.expectedInboundQty, 4)}`,
    `预计出库量：${formatNumber(totals.expectedOutboundQty, 4)}`,
    `理论库存量：${formatNumber(totals.theoreticalQty, 4)}`,
    `理论库存金额：${formatMoney(totals.theoreticalAmount)}`,
  ];
  if (isWarehouseDimension.value) {
    labels.push(`D+1 理论库存量：${formatNumber(totals.d1TheoreticalQty, 4)}`);
    labels.push(`D+2 理论库存量：${formatNumber(totals.d2TheoreticalQty, 4)}`);
  }
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
    const headers = isWarehouseDimension.value
      ? ['物品编码', '物品名称', '规格型号', '物品类别', '统计类型', '单位', '与基准单位的换算率', '物品体积', '物品重量', '仓库', '库存量', '库存金额', '库存金额（不含税）', '库存税额', '库存均价（不含税）', '预计入库量', '预计出库量', '理论库存量', '理论库存金额', 'D+1 理论库存量', 'D+2 理论库存量']
      : ['物品编码', '物品名称', '规格型号', '物品类别', '统计类型', '单位', '与基准单位的换算率', '物品体积', '物品重量', '库存量', '库存金额', '库存金额（不含税）', '库存税额', '库存均价（不含税）', '预计入库量', '预计出库量', '理论库存量', '理论库存金额'];
    const lines = [headers.map(toCsvCell).join(',')];
    rows.forEach((row) => {
      const cells = isWarehouseDimension.value
        ? [
            row.itemCode,
            row.itemName,
            row.spec,
            row.category,
            row.statType,
            row.unit,
            row.unitRateText,
            formatNumber(row.volume),
            formatNumber(row.weight),
            row.warehouseName,
            formatNumber(row.stockQty, 4),
            formatMoney(row.stockAmount),
            formatMoney(row.stockAmountExTax),
            formatMoney(row.taxAmount),
            formatMoney(row.avgPriceExTax),
            formatNumber(row.expectedInboundQty, 4),
            formatNumber(row.expectedOutboundQty, 4),
            formatNumber(row.theoreticalQty, 4),
            formatMoney(row.theoreticalAmount),
            formatNumber(row.d1TheoreticalQty, 4),
            formatNumber(row.d2TheoreticalQty, 4),
          ]
        : [
            row.itemCode,
            row.itemName,
            row.spec,
            row.category,
            row.statType,
            row.unit,
            row.unitRateText,
            formatNumber(row.volume),
            formatNumber(row.weight),
            formatNumber(row.stockQty, 4),
            formatMoney(row.stockAmount),
            formatMoney(row.stockAmountExTax),
            formatMoney(row.taxAmount),
            formatMoney(row.avgPriceExTax),
            formatNumber(row.expectedInboundQty, 4),
            formatNumber(row.expectedOutboundQty, 4),
            formatNumber(row.theoreticalQty, 4),
            formatMoney(row.theoreticalAmount),
          ];
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
  query.hideZeroStock = '';
  query.hideZeroTheoretical = '';
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
        <el-input v-model="query.hideZeroStock" placeholder="输入1启用" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="不显示实时库存为0的物品">
        <el-input v-model="query.hideZeroTheoretical" placeholder="输入1启用" clearable style="width: 160px" />
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
