<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import {
  fetchPurchaseItemPriceAnalysisReportApi,
  type PurchaseItemPriceAnalysisReportRow,
  type PurchasePriceAnalysisPeriod,
} from '@/api/modules/purchase';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticMethod = '采购总价' | '采购均价';
type DetailGranularity = '不显示' | '到供应商粒度';
type StatisticPeriod = '按日' | '按周' | '按月';
type TimeSort = '按时间倒序' | '按时间正序';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchasePriceAnalysisRow = PurchaseItemPriceAnalysisReportRow;

const sessionStore = useSessionStore();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const statisticMethodOptions: StatisticMethod[] = ['采购总价', '采购均价'];
const detailGranularityOptions: DetailGranularity[] = ['不显示', '到供应商粒度'];
const statisticPeriodOptions: StatisticPeriod[] = ['按日', '按周', '按月'];
const timeSortOptions: TimeSort[] = ['按时间倒序', '按时间正序'];

const query = reactive({
  statisticMethod: '采购总价' as StatisticMethod,
  detailGranularity: '不显示' as DetailGranularity,
  dateRange: [] as string[],
  statisticPeriod: '按周' as StatisticPeriod,
  supplier: '',
  itemCode: '',
  itemCategory: '',
  timeSort: '按时间倒序' as TimeSort,
});

const loading = ref(false);
const optionLoading = ref(false);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const periods = ref<PurchasePriceAnalysisPeriod[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableRows = ref<PurchasePriceAnalysisRow[]>([]);

const isTotalMode = computed(() => query.statisticMethod === '采购总价');
const supplierSelectOptions = computed(() => supplierOptions.value.map((item) => ({
  value: item.value,
  label: item.label,
})));

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
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await loadSupplierOptions();
    if (!orgId) {
      itemCategoryTree.value = [];
      itemOptions.value = [];
      return;
    }
    const [categoryRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId)),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    itemOptions.value = [];
    ElMessage.error('采购物品价格分析表筛选项加载失败');
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
      periods.value = [];
      total.value = 0;
      return;
    }
    const report = await fetchPurchaseItemPriceAnalysisReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      statisticMethod: query.statisticMethod,
      detailGranularity: query.detailGranularity,
      statisticPeriod: query.statisticPeriod,
      supplier: query.supplier || undefined,
      itemCode: query.itemCode || undefined,
      itemCategory: query.itemCategory || undefined,
      timeSort: query.timeSort,
    }, orgId);
    periods.value = report.periods ?? [];
    tableRows.value = report.page?.list ?? [];
    total.value = Number(report.page?.total ?? 0);
  } finally {
    loading.value = false;
  }
};

const valueOf = (row: PurchasePriceAnalysisRow, periodKey: string, key: 'total' | 'avg' | 'fluctuationRate') => (
  row.values.find((item) => item.periodKey === periodKey)?.[key] ?? 0
);
const formatAmount = (value: number) => Number(value || 0).toFixed(2);
const formatRate = (value: number) => `${Number(value || 0).toFixed(2)}%`;

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.statisticMethod = '采购总价';
  query.detailGranularity = '不显示';
  query.dateRange = [];
  query.statisticPeriod = '按周';
  query.supplier = '';
  query.itemCode = '';
  query.itemCategory = '';
  query.timeSort = '按时间倒序';
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

const handleViewTrend = (row: PurchasePriceAnalysisRow) => {
  ElMessage.info(`查看价格趋势：${row.itemName}`);
};

const getSummaries = ({ columns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return columns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property ?? '';
    if (!property.startsWith('total:')) {
      return '';
    }
    const key = property.slice('total:'.length);
    return tableRows.value.reduce((sum, row) => sum + valueOf(row, key, 'total'), 0).toFixed(2);
  });
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  async () => {
    tableRows.value = [];
    periods.value = [];
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
    <CommonQuerySection :model="query" class="purchase-price-query">
      <el-form-item label="统计方式" class="query-method">
        <el-radio-group v-model="query.statisticMethod">
          <el-radio-button v-for="option in statisticMethodOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="明细显示粒度" class="query-detail">
        <el-radio-group v-model="query.detailGranularity">
          <el-radio v-for="option in detailGranularityOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="日期" class="query-date">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item label="统计周期" class="query-period">
        <el-radio-group v-model="query.statisticPeriod">
          <el-radio v-for="option in statisticPeriodOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="供应商" class="query-supplier">
        <el-select
          v-model="query.supplier"
          :loading="supplierLoading"
          clearable
          filterable
          placeholder="请选择"
        >
          <el-option
            v-for="option in supplierSelectOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="物品" class="query-item">
        <el-select
          v-model="query.itemCode"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
        >
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="物品类别" class="query-category">
        <el-tree-select
          v-model="query.itemCategory"
          :data="itemCategoryTree"
          :loading="optionLoading"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          filterable
          check-strictly
          default-expand-all
          placeholder="请选择"
        />
      </el-form-item>

      <el-form-item label="时间排序" class="query-sort">
        <el-radio-group v-model="query.timeSort">
          <el-radio v-for="option in timeSortOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item class="query-actions">
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
      :show-summary="isTotalMode"
      :summary-method="getSummaries"
      empty-text="暂无采购物品价格分析数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="itemName" label="物品名称" min-width="140" fixed="left" show-overflow-tooltip />
      <el-table-column prop="itemCode" label="物品编码" min-width="130" fixed="left" show-overflow-tooltip />
      <el-table-column prop="spec" label="规格型号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCategory" label="物品类别" min-width="120" show-overflow-tooltip />
      <el-table-column v-if="isTotalMode" prop="baseUnit" label="基准单位" min-width="100" show-overflow-tooltip />
      <el-table-column v-else prop="unit" label="单位" min-width="90" show-overflow-tooltip />
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />

      <template v-if="isTotalMode">
        <el-table-column
          v-for="period in periods"
          :key="period.key"
          :prop="`total:${period.key}`"
          :label="`${period.label} 总价`"
          min-width="140"
          align="right"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ formatAmount(valueOf(row, period.key, 'total')) }}
          </template>
        </el-table-column>
      </template>

      <template v-else>
        <template v-for="period in periods" :key="period.key">
          <el-table-column
            :label="`${period.compactLabel} 均价`"
            min-width="130"
            align="right"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ formatAmount(valueOf(row, period.key, 'avg')) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="`${period.compactLabel} 波动率`"
            min-width="140"
            align="right"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ formatRate(valueOf(row, period.key, 'fluctuationRate')) }}
            </template>
          </el-table-column>
        </template>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewTrend(row)">趋势</el-button>
          </template>
        </el-table-column>
      </template>
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

<style scoped>
:deep(.purchase-price-query) {
  display: grid;
  grid-template-columns: 260px 300px minmax(340px, 1.1fr) 300px minmax(260px, 0.9fr);
  gap: 10px 16px;
  align-items: center;
  padding: 10px 12px;
}

:deep(.purchase-price-query .el-form-item) {
  min-width: 0;
}

:deep(.purchase-price-query .el-form-item__label) {
  flex: 0 0 auto;
  height: 28px;
  line-height: 28px;
  font-size: 12px;
}

:deep(.purchase-price-query .el-form-item__content) {
  min-width: 0;
  min-height: 28px;
  line-height: 28px;
}

:deep(.purchase-price-query .el-radio-group) {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

:deep(.purchase-price-query .el-radio) {
  height: 28px;
  margin-right: 0;
}

:deep(.purchase-price-query .el-radio-button__inner),
:deep(.purchase-price-query .el-button) {
  height: 28px;
  padding: 0 14px;
  line-height: 26px;
}

:deep(.purchase-price-query .el-input),
:deep(.purchase-price-query .el-select),
:deep(.purchase-price-query .el-date-editor) {
  width: 100%;
  --el-input-height: 28px;
  font-size: 12px;
}

:deep(.purchase-price-query .query-method) {
  grid-column: 1;
}

:deep(.purchase-price-query .query-detail) {
  grid-column: 2;
}

:deep(.purchase-price-query .query-date) {
  grid-column: 3;
}

:deep(.purchase-price-query .query-period) {
  grid-column: 4;
}

:deep(.purchase-price-query .query-supplier) {
  grid-column: 5;
}

:deep(.purchase-price-query .query-item) {
  grid-column: 1 / span 2;
}

:deep(.purchase-price-query .query-category) {
  grid-column: 3;
}

:deep(.purchase-price-query .query-sort) {
  grid-column: 4;
}

:deep(.purchase-price-query .query-actions) {
  grid-column: 5;
}

:deep(.purchase-price-query .query-actions .el-form-item__content) {
  gap: 10px;
  justify-content: flex-start;
}

@media (max-width: 1280px) {
  :deep(.purchase-price-query) {
    grid-template-columns: repeat(2, minmax(280px, 1fr));
  }

  :deep(.purchase-price-query .el-form-item) {
    grid-column: auto;
  }
}
</style>
