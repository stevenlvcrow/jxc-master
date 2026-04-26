<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, ArrowUp, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import {
  fetchDishConsumptionOutboundReportApi,
  type DishConsumptionOutboundReportDimension,
  type DishConsumptionOutboundReportRow,
  type DishConsumptionOutboundReportSummary,
} from '@/api/modules/inventory';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';

type UnitType = '基准单位' | '库存单位' | '成本单位';
type QueryScheme = '系统默认方案';
type DeductionType = '全部' | '销售扣减' | '报损扣减';

type ReportColumn = {
  key: keyof DishConsumptionOutboundReportRow;
  label: string;
  minWidth: number;
  fixed?: 'left';
  align?: 'left' | 'right';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const dimensionOptions: DishConsumptionOutboundReportDimension[] = ['菜品消耗单明细', '菜品消耗汇总'];
const deductionTypeOptions: DeductionType[] = ['全部', '销售扣减', '报损扣减'];
const unitTypeOptions: UnitType[] = ['基准单位', '库存单位', '成本单位'];
const querySchemeOptions: QueryScheme[] = ['系统默认方案'];

const query = reactive({
  dimension: '菜品消耗单明细' as DishConsumptionOutboundReportDimension,
  dateRange: [] as string[],
  warehouse: '',
  dishName: '',
  itemCode: '',
  deductionType: '全部' as DeductionType,
  unitType: '基准单位' as UnitType,
  queryScheme: '系统默认方案' as QueryScheme,
});

const collapsed = ref(false);
const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<DishConsumptionOutboundReportRow[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const serverSummary = ref<DishConsumptionOutboundReportSummary | null>(null);

const currentOrgId = computed(() => String(sessionStore.currentOrgId ?? '').trim().toLowerCase());

const columns: ReportColumn[] = [
  { key: 'consumptionNo', label: '消耗单号', minWidth: 150, fixed: 'left' },
  { key: 'businessDate', label: '日期', minWidth: 110 },
  { key: 'dishSpuCode', label: '菜品 SPU 编码', minWidth: 140 },
  { key: 'dishSkuCode', label: '菜品 SKU 编码', minWidth: 140 },
  { key: 'dishName', label: '菜品名称', minWidth: 130 },
  { key: 'dishSpec', label: '菜品规格', minWidth: 120 },
  { key: 'costCard', label: '成本卡', minWidth: 130 },
  { key: 'orderSource', label: '订单来源', minWidth: 110 },
  { key: 'dishCategory', label: '菜品分类', minWidth: 120 },
  { key: 'deductionType', label: '扣减类型', minWidth: 110 },
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'itemUnit', label: '物品单位', minWidth: 100 },
  { key: 'dishQty', label: '菜品销售 / 报损数量', minWidth: 170, align: 'right' },
  { key: 'theoreticalQty', label: '物品理论用量', minWidth: 130, align: 'right' },
  { key: 'outboundQty', label: '物品已出库数量', minWidth: 140, align: 'right' },
  { key: 'pendingOutboundQty', label: '物品未出库数量', minWidth: 140, align: 'right' },
];

const numericKeys: Array<keyof DishConsumptionOutboundReportSummary> = [
  'dishQty',
  'theoreticalQty',
  'outboundQty',
  'pendingOutboundQty',
];

const normalizeText = (value: unknown) => (typeof value === 'string' ? value.trim() : '');

const parseNumber = (value: unknown) => {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0;
  }
  const parsed = Number(normalizeText(value));
  return Number.isFinite(parsed) ? parsed : 0;
};

const formatNumber = (value: unknown) => {
  const numberValue = parseNumber(value);
  return Number.isInteger(numberValue) ? String(numberValue) : numberValue.toFixed(4);
};

const formatCell = (row: DishConsumptionOutboundReportRow, key: keyof DishConsumptionOutboundReportRow) => {
  if (key === 'dishQty' || key === 'theoreticalQty' || key === 'outboundQty' || key === 'pendingOutboundQty') {
    return formatNumber(row[key]);
  }
  return String(row[key] ?? '-');
};

const buildParams = () => {
  const [startDate, endDate] = query.dateRange;
  return {
    pageNo: currentPage.value,
    pageSize: pageSize.value,
    dimension: query.dimension,
    startDate,
    endDate,
    warehouse: query.warehouse || undefined,
    dishName: normalizeText(query.dishName) || undefined,
    itemCode: query.itemCode || undefined,
    deductionType: query.deductionType === '全部' ? undefined : query.deductionType,
    unitType: query.unitType,
    queryScheme: query.queryScheme,
  };
};

const clientSummary = computed<DishConsumptionOutboundReportSummary>(() => {
  return numericKeys.reduce<DishConsumptionOutboundReportSummary>((summary, key) => {
    summary[key] = tableRows.value.reduce((sum, row) => sum + parseNumber(row[key]), 0);
    return summary;
  }, {});
});

const activeSummary = computed(() => serverSummary.value ?? clientSummary.value);

const getSummaries = ({ columns: summaryColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return summaryColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof DishConsumptionOutboundReportSummary | undefined;
    if (!property || !numericKeys.includes(property)) {
      return '';
    }
    return formatNumber(activeSummary.value[property]);
  });
};

const loadItemOptions = async () => {
  if (!currentOrgId.value) {
    itemOptions.value = [];
    return;
  }
  optionLoading.value = true;
  try {
    const page = await fetchItemsApi({
      pageNo: 1,
      pageSize: 200,
      status: '全部',
      itemType: '全部',
    }, currentOrgId.value);
    itemOptions.value = (page.list ?? []).map((item: ItemVO) => ({
      value: item.code,
      label: `${item.code} / ${item.name}`,
    }));
  } catch {
    itemOptions.value = [];
    ElMessage.error('物品下拉加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const loadOptions = async () => {
  await Promise.all([
    loadWarehouseTree(),
    loadItemOptions(),
  ]);
};

const fetchReport = async () => {
  if (!currentOrgId.value) {
    tableRows.value = [];
    total.value = 0;
    serverSummary.value = null;
    return;
  }
  loading.value = true;
  try {
    const page = await fetchDishConsumptionOutboundReportApi(buildParams(), currentOrgId.value);
    tableRows.value = page.list ?? [];
    total.value = Number(page.total ?? 0);
    serverSummary.value = page.summary ?? null;
  } catch {
    tableRows.value = [];
    total.value = 0;
    serverSummary.value = null;
    ElMessage.error('菜品消耗出库查询表加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.dimension = '菜品消耗单明细';
  query.dateRange = [];
  query.warehouse = '';
  query.dishName = '';
  query.itemCode = '';
  query.deductionType = '全部';
  query.unitType = '基准单位';
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

watch(
  () => sessionStore.currentOrgId,
  async () => {
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
      <el-form-item label="维度">
        <el-select v-model="query.dimension" style="width: 180px">
          <el-option v-for="option in dimensionOptions" :key="option" :label="option" :value="option" />
        </el-select>
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

      <template v-if="!collapsed">
        <el-form-item label="仓库">
          <el-tree-select
            v-model="query.warehouse"
            :data="warehouseTree"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            clearable
            check-strictly
            default-expand-all
            placeholder="全部"
            style="width: 180px"
          />
        </el-form-item>

        <el-form-item label="菜品名称">
          <el-input v-model="query.dishName" clearable placeholder="请输入" style="width: 160px" />
        </el-form-item>

        <el-form-item label="物品">
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

        <el-form-item label="扣减类型">
          <el-tree-select
            v-model="query.deductionType"
            :data="deductionTypeOptions.map((option) => ({ value: option, label: option }))"
            :props="{ label: 'label', value: 'value' }"
            check-strictly
            default-expand-all
            style="width: 140px"
          />
        </el-form-item>

        <el-form-item label="单位类型">
          <el-tree-select
            v-model="query.unitType"
            :data="unitTypeOptions.map((option) => ({ value: option, label: option }))"
            :props="{ label: 'label', value: 'value' }"
            check-strictly
            default-expand-all
            style="width: 140px"
          />
        </el-form-item>

        <el-form-item label="查询方案">
          <el-select v-model="query.queryScheme" style="width: 150px">
            <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
      </template>

      <el-form-item>
        <el-button @click="collapsed = !collapsed">
          <el-icon>
            <ArrowDown v-if="collapsed" />
            <ArrowUp v-else />
          </el-icon>
          {{ collapsed ? '展开筛选' : '收起筛选' }}
        </el-button>
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
      :height="420"
      :show-summary="true"
      :summary-method="getSummaries"
      empty-text="暂无菜品消耗出库数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :fixed="column.fixed"
        :align="column.align"
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
