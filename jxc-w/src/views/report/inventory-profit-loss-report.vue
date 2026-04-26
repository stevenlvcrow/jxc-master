<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchInventoryProfitLossReportApi } from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  fetchItemStatisticsTypesApi,
  type ItemCategoryTreeNode,
  type ItemStatisticsTypeRow,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type ProfitLossResult = '全部' | '盘盈' | '盘亏' | '无差异';
type UnitType = '库存单位';

type ProfitLossReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  statisticsType: string;
  unit: string;
  checkDocumentNo: string;
  checkType: string;
  stockDocumentNo: string;
  orgName: string;
  orgCode: string;
  warehouse: string;
  checkTime: string;
  auditTime: string;
  auditor: string;
  bookQty: number;
  bookAmount: number;
  actualQty: number;
  actualAmount: number;
  profitLossQty: number;
  profitLossAmount: number;
  profitLossQtyAbs: number;
  profitLossAmountAbs: number;
  adjustmentAmount: number;
  profitInboundPrice: number;
  lossOutboundPrice: number;
  checkReason: string;
  remark: string;
};

type ReportColumn = {
  key: keyof ProfitLossReportRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const archiveOrgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));

const profitLossResultOptions: ProfitLossResult[] = ['全部', '盘盈', '盘亏', '无差异'];
const unitTypeOptions: UnitType[] = ['库存单位'];
const checkTypeOptions = ['全部', '指定物品', '全仓盘点'];

const query = reactive({
  warehouse: '',
  dateRange: [] as string[],
  itemCategory: '',
  statisticsType: '',
  itemKeyword: '',
  checkType: '',
  profitLossResult: '全部' as ProfitLossResult,
  unitType: '库存单位' as UnitType,
  showUnitRate: false,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<ProfitLossReportRow[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const itemCategoryTree = ref<TreeNode[]>([]);
const statisticsTypeTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const columns = computed<ReportColumn[]>(() => {
  const baseColumns: ReportColumn[] = [
    { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
    { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
    { key: 'specModel', label: '规格型号', minWidth: 120 },
    { key: 'itemCategory', label: '物品类别', minWidth: 120 },
    { key: 'statisticsType', label: '统计类型', minWidth: 120 },
    { key: 'unit', label: '单位', minWidth: 90 },
    { key: 'checkDocumentNo', label: '盘点单号', minWidth: 150 },
    { key: 'checkType', label: '盘点类型', minWidth: 110 },
    { key: 'stockDocumentNo', label: '盘点出入库单号', minWidth: 160 },
    { key: 'orgName', label: '机构', minWidth: 130 },
    { key: 'orgCode', label: '机构编码', minWidth: 120 },
    { key: 'warehouse', label: '仓库', minWidth: 130 },
    { key: 'checkTime', label: '盘点时间', minWidth: 160 },
    { key: 'auditTime', label: '审核时间', minWidth: 160 },
    { key: 'auditor', label: '审核人', minWidth: 100 },
    { key: 'bookQty', label: '账面数', minWidth: 110, align: 'right' },
    { key: 'bookAmount', label: '账面金额', minWidth: 110, align: 'right' },
    { key: 'actualQty', label: '实盘数', minWidth: 110, align: 'right' },
    { key: 'actualAmount', label: '实盘金额', minWidth: 110, align: 'right' },
    { key: 'profitLossQty', label: '盈亏数量', minWidth: 110, align: 'right' },
    { key: 'profitLossAmount', label: '盈亏金额', minWidth: 110, align: 'right' },
    { key: 'profitLossQtyAbs', label: '盈亏数量绝对值', minWidth: 140, align: 'right' },
    { key: 'profitLossAmountAbs', label: '盈亏金额绝对值', minWidth: 150, align: 'right' },
    { key: 'adjustmentAmount', label: '调整金额', minWidth: 110, align: 'right' },
    { key: 'profitInboundPrice', label: '盘盈入库单价', minWidth: 130, align: 'right' },
    { key: 'lossOutboundPrice', label: '盘亏出库单价', minWidth: 130, align: 'right' },
    { key: 'checkReason', label: '盘点原因', minWidth: 140 },
    { key: 'remark', label: '备注', minWidth: 140 },
  ];
  if (!query.showUnitRate) {
    return baseColumns;
  }
  return [
    ...baseColumns.slice(0, 6),
    { key: 'remark', label: '展示单位换算率', minWidth: 140 },
    ...baseColumns.slice(6),
  ];
});

const numberKeys: Array<keyof ProfitLossReportRow> = [
  'bookQty',
  'bookAmount',
  'actualQty',
  'actualAmount',
  'profitLossQty',
  'profitLossAmount',
  'profitLossQtyAbs',
  'profitLossAmountAbs',
  'adjustmentAmount',
  'profitInboundPrice',
  'lossOutboundPrice',
];

const amountKeys: Array<keyof ProfitLossReportRow> = [
  'bookAmount',
  'actualAmount',
  'profitLossAmount',
  'profitLossAmountAbs',
  'adjustmentAmount',
  'profitInboundPrice',
  'lossOutboundPrice',
];

const normalizeText = (value: unknown) => (typeof value === 'string' ? value.trim() : '');
const formatNumber = (value: number, digits = 4) => Number.isFinite(value) ? value.toFixed(digits) : '0.0000';
const formatMoney = (value: number) => Number.isFinite(value) ? value.toFixed(2) : '0.00';

const formatCell = (row: ProfitLossReportRow, key: keyof ProfitLossReportRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return amountKeys.includes(key) ? formatMoney(value) : formatNumber(value);
};

const normalizeTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeTree(node.children) : undefined,
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

const loadCategoryTree = async (orgId: string) => {
  const rows = await fetchItemCategoryTreeApi(orgId);
  itemCategoryTree.value = normalizeTree(rows ?? []);
};

const loadStatisticsTypeTree = async (orgId: string) => {
  const rows = await fetchAllPages<ItemStatisticsTypeRow>(
    (pageNo, pageSizeValue) => fetchItemStatisticsTypesApi({ pageNo, pageSize: pageSizeValue }, orgId),
  );
  const grouped = new Map<string, Array<{ value: string; label: string }>>();
  rows.forEach((row) => {
    const group = normalizeText(row.statisticsCategory) || '未分类';
    if (!grouped.has(group)) {
      grouped.set(group, []);
    }
    grouped.get(group)?.push({ value: row.name, label: `${row.code} / ${row.name}` });
  });
  statisticsTypeTree.value = Array.from(grouped.entries()).map(([label, children]) => ({
    value: label,
    label,
    children,
  }));
};

const loadItemOptions = async (orgId: string) => {
  const rows = await fetchAllPages<ItemVO>(
    (pageNo, pageSizeValue) => fetchItemsApi({
      pageNo,
      pageSize: pageSizeValue,
      status: '全部',
      itemType: '全部',
    }, orgId),
  );
  itemOptions.value = rows.map((row) => ({ value: row.code, label: `${row.code} / ${row.name}` }));
};

const loadOptions = async () => {
  if (!archiveOrgId.value) {
    itemCategoryTree.value = [];
    statisticsTypeTree.value = [];
    itemOptions.value = [];
    await loadWarehouseTree();
    return;
  }
  optionLoading.value = true;
  try {
    await Promise.all([
      loadWarehouseTree(),
      loadCategoryTree(archiveOrgId.value),
      loadStatisticsTypeTree(archiveOrgId.value),
      loadItemOptions(archiveOrgId.value),
    ]);
  } catch {
    ElMessage.error('盘点盈亏表筛选项加载失败');
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
    const page = await fetchInventoryProfitLossReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      warehouse: query.warehouse || undefined,
      dateRangeStart: query.dateRange[0],
      dateRangeEnd: query.dateRange[1],
      itemCategory: query.itemCategory || undefined,
      statisticsType: query.statisticsType || undefined,
      itemKeyword: query.itemKeyword || undefined,
      checkType: query.checkType || undefined,
      profitLossResult: query.profitLossResult === '全部' ? undefined : query.profitLossResult,
      unitType: query.unitType,
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
  query.warehouse = '';
  query.dateRange = [];
  query.itemCategory = '';
  query.statisticsType = '';
  query.itemKeyword = '';
  query.checkType = '';
  query.profitLossResult = '全部';
  query.unitType = '库存单位';
  query.showUnitRate = false;
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
  return numberKeys.reduce<Record<string, number>>((totals, key) => {
    totals[key] = tableRows.value.reduce((sum, row) => sum + Number(row[key] ?? 0), 0);
    return totals;
  }, {});
});

const getSummaries = ({ columns: summaryColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return summaryColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof ProfitLossReportRow | undefined;
    if (!property || !numberKeys.includes(property)) {
      return '';
    }
    const value = summaryTotals.value[property] ?? 0;
    return amountKeys.includes(property) ? formatMoney(value) : formatNumber(value);
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

      <el-form-item label="统计类型">
        <el-tree-select
          v-model="query.statisticsType"
          :data="statisticsTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          :loading="optionLoading"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="物品">
        <el-select
          v-model="query.itemKeyword"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请输入或选择"
          style="width: 200px"
        >
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="盘点类型">
        <el-tree-select
          v-model="query.checkType"
          :data="checkTypeOptions.map((option) => ({ value: option === '全部' ? '' : option, label: option }))"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item label="盈亏结果">
        <el-select v-model="query.profitLossResult" style="width: 120px">
          <el-option v-for="option in profitLossResultOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="计量单位类型">
        <el-select v-model="query.unitType" style="width: 140px">
          <el-option v-for="option in unitTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="展示单位换算率">
        <el-checkbox v-model="query.showUnitRate" />
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
      empty-text="暂无盘点盈亏数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in columns"
        :key="`${column.key}-${column.label}`"
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
