<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchStockTurnoverRateReportApi } from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticDimension = '仓库' | '物品';
type StatisticMethod = '按金额计算' | '按数量计算';
type UnitType = '基准单位';
const INVENTORY_PERIOD_TYPE_DICT = 'inventory.period_type';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type StockTurnoverRow = {
  id: string;
  orgName?: string;
  warehouse: string;
  itemName?: string;
  itemCode?: string;
  unit?: string;
  itemCategory?: string;
  itemStatus?: string;
  openingAmount: number;
  closingAmount: number;
  avgStockAmount: number;
  outboundAmount: number;
  turnoverRate: number;
  turnoverDays: number;
};

type ReportColumn = {
  key: keyof StockTurnoverRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const { optionsOf } = useDictionaryOptions([INVENTORY_PERIOD_TYPE_DICT]);
const periodTypeOptions = optionsOf(INVENTORY_PERIOD_TYPE_DICT);

const statisticDimensionOptions: StatisticDimension[] = ['仓库', '物品'];
const statisticMethodTree: TreeNode[] = [
  { value: '按金额计算', label: '按金额计算' },
  { value: '按数量计算', label: '按数量计算' },
];
const itemStatusTree: TreeNode[] = [
  { value: '全部', label: '全部' },
  { value: '启用', label: '启用' },
  { value: '停用', label: '停用' },
];
const unitTypeTree: TreeNode[] = [{ value: '基准单位', label: '基准单位' }];

const query = reactive({
  statisticDimension: '仓库' as StatisticDimension,
  statisticMethod: '按金额计算' as StatisticMethod,
  dateRange: [] as string[],
  periodType: 'MONTH',
  periodStartDate: '',
  warehouse: '',
  itemCategory: '',
  itemCode: '',
  itemStatus: '全部',
  unitType: '基准单位' as UnitType,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<StockTurnoverRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const isWarehouseDimension = computed(() => query.statisticDimension === '仓库');

const warehouseColumns: ReportColumn[] = [
  { key: 'warehouse', label: '仓库', minWidth: 140, fixed: 'left' },
  { key: 'openingAmount', label: '期初金额（不含税）', minWidth: 160, align: 'right' },
  { key: 'closingAmount', label: '期末金额（不含税）', minWidth: 160, align: 'right' },
  { key: 'avgStockAmount', label: '平均库存金额（不含税）', minWidth: 180, align: 'right' },
  { key: 'outboundAmount', label: '出库金额（不含税）', minWidth: 160, align: 'right' },
  { key: 'turnoverRate', label: '库存周转率', minWidth: 120, align: 'right' },
  { key: 'turnoverDays', label: '库存周转天数', minWidth: 130, align: 'right' },
];

const itemColumns: ReportColumn[] = [
  { key: 'orgName', label: '机构', minWidth: 130 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'itemStatus', label: '物品状态', minWidth: 100 },
  { key: 'openingAmount', label: '期初金额', minWidth: 120, align: 'right' },
  { key: 'closingAmount', label: '期末金额', minWidth: 120, align: 'right' },
  { key: 'avgStockAmount', label: '平均库存金额', minWidth: 130, align: 'right' },
  { key: 'outboundAmount', label: '出库金额', minWidth: 120, align: 'right' },
  { key: 'turnoverRate', label: '库存周转率', minWidth: 120, align: 'right' },
  { key: 'turnoverDays', label: '库存周转天数', minWidth: 130, align: 'right' },
];

const visibleColumns = computed(() => (isWarehouseDimension.value ? warehouseColumns : itemColumns));
const numericKeys: Array<keyof StockTurnoverRow> = [
  'openingAmount',
  'closingAmount',
  'avgStockAmount',
  'outboundAmount',
  'turnoverRate',
  'turnoverDays',
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

const formatCell = (row: StockTurnoverRow, key: keyof StockTurnoverRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  if (key === 'turnoverRate' || key === 'turnoverDays') {
    return value.toFixed(4);
  }
  return value.toFixed(2);
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await loadWarehouseTree();
    if (!orgId) {
      itemCategoryTree.value = [];
      itemTree.value = [];
      return;
    }
    const [categoryRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId)),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    itemTree.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    itemTree.value = [];
    ElMessage.error('库存周转率统计表筛选项加载失败');
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
    const page = await fetchStockTurnoverRateReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      statisticDimension: query.statisticDimension,
      statisticMethod: query.statisticMethod,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      periodType: query.periodType,
      periodStartDate: query.periodStartDate,
      warehouse: query.warehouse || undefined,
      itemCategory: query.itemCategory || undefined,
      itemCode: query.itemCode || undefined,
      itemStatus: query.itemStatus === '全部' ? undefined : query.itemStatus,
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
  query.statisticDimension = '仓库';
  query.statisticMethod = '按金额计算';
  query.dateRange = [];
  query.periodType = 'MONTH';
  query.periodStartDate = '';
  query.warehouse = '';
  query.itemCategory = '';
  query.itemCode = '';
  query.itemStatus = '全部';
  query.unitType = '基准单位';
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

const getSummaries = ({ columns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return columns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof StockTurnoverRow | undefined;
    if (!property || !numericKeys.includes(property)) {
      return '';
    }
    const totalValue = tableRows.value.reduce((sum, row) => sum + Number(row[property] ?? 0), 0);
    return property === 'turnoverRate' || property === 'turnoverDays' ? totalValue.toFixed(4) : totalValue.toFixed(2);
  });
};

watch(
  () => query.statisticDimension,
  () => {
    if (isWarehouseDimension.value) {
      query.itemCategory = '';
      query.itemCode = '';
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
        <el-radio-group v-model="query.statisticDimension">
          <el-radio v-for="option in statisticDimensionOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="统计方式">
        <el-tree-select
          v-model="query.statisticMethod"
          :data="statisticMethodTree"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 140px"
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

      <el-form-item v-if="!isWarehouseDimension" label="物品类别">
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

      <el-form-item v-if="!isWarehouseDimension" label="物品信息">
        <el-tree-select
          v-model="query.itemCode"
          :data="itemTree"
          :props="{ label: 'label', value: 'value' }"
          :loading="optionLoading"
          clearable
          filterable
          check-strictly
          default-expand-all
          placeholder="物品名称 / 编码 / 助记码 / 拼音码"
          style="width: 240px"
        />
      </el-form-item>

      <el-form-item v-if="!isWarehouseDimension" label="物品状态">
        <el-tree-select
          v-model="query.itemStatus"
          :data="itemStatusTree"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 120px"
        />
      </el-form-item>

      <el-form-item label="单位类型">
        <el-tree-select
          v-model="query.unitType"
          :data="unitTypeTree"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 140px"
        />
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
      empty-text="暂无库存周转率统计数据"
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
