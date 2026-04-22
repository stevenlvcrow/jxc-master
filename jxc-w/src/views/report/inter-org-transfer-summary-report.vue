<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useSessionStore, type OrgNode } from '@/stores/session';
import {
  fetchItemCategoryTreeApi,
  type ItemCategoryTreeNode,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticMode = '从本店调出' | '调入本店';
type DateDimension = '调拨日期' | '调拨入库日期';
type StatisticDimension = '门店' | '门店+仓库';
type UnitType = '库存单位';
type QueryScheme = '系统默认方案';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type TransferSummaryRow = {
  id: string;
  itemCode: string;
  itemName: string;
  sourceStore: string;
  targetStore: string;
  specModel: string;
  itemCategory: string;
  unit: string;
  transferQty: number;
  inboundAmountTaxIncluded: number;
  outboundCostAmountExTax: number;
  outboundSettlementAmountTaxIncluded: number;
  inboundAvgPriceTaxIncluded: number;
  outboundCostAvgPriceExTax: number;
  outboundSettlementAvgPriceTaxIncluded: number;
};

type ReportColumn = {
  key: keyof TransferSummaryRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();

const statisticModeOptions: StatisticMode[] = ['从本店调出', '调入本店'];
const dateDimensionOptions: DateDimension[] = ['调拨日期', '调拨入库日期'];
const statisticDimensionOptions: StatisticDimension[] = ['门店', '门店+仓库'];
const unitTypeOptions: UnitType[] = ['库存单位'];
const querySchemeOptions: QueryScheme[] = ['系统默认方案'];

const query = reactive({
  statisticMode: '从本店调出' as StatisticMode,
  dateDimension: '调拨日期' as DateDimension,
  dateRange: [] as string[],
  statisticDimension: '门店' as StatisticDimension,
  targetStore: '',
  itemKeyword: '',
  itemCategory: '',
  unitType: '库存单位' as UnitType,
  queryScheme: '系统默认方案' as QueryScheme,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<TransferSummaryRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const storeTree = computed<TreeNode[]>(() => {
  const normalize = (node: OrgNode): TreeNode | null => {
    if (node.type === 'store') {
      return {
        value: node.id,
        label: node.code ? `${node.name} / ${node.code}` : node.name,
      };
    }
    const children = (node.children ?? [])
      .map((child) => normalize(child))
      .filter((child): child is TreeNode => Boolean(child));
    if (!children.length) {
      return null;
    }
    return {
      value: node.id,
      label: node.name,
      children,
    };
  };
  return sessionStore.rootGroups
    .map((node) => normalize(node))
    .filter((node): node is TreeNode => Boolean(node));
});

const columns: ReportColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'sourceStore', label: '调出门店', minWidth: 130 },
  { key: 'targetStore', label: '调入门店', minWidth: 130 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'transferQty', label: '调拨数量', minWidth: 110, align: 'right' },
  { key: 'inboundAmountTaxIncluded', label: '调入金额（含税）', minWidth: 150, align: 'right' },
  { key: 'outboundCostAmountExTax', label: '调出成本金额（不含税）', minWidth: 180, align: 'right' },
  { key: 'outboundSettlementAmountTaxIncluded', label: '调出结算金额（含税）', minWidth: 180, align: 'right' },
  { key: 'inboundAvgPriceTaxIncluded', label: '调入均价（含税）', minWidth: 150, align: 'right' },
  { key: 'outboundCostAvgPriceExTax', label: '调出成本均价（不含税）', minWidth: 180, align: 'right' },
  { key: 'outboundSettlementAvgPriceTaxIncluded', label: '调出结算均价（含税）', minWidth: 180, align: 'right' },
];

const quantityKeys: Array<keyof TransferSummaryRow> = ['transferQty'];
const moneyKeys: Array<keyof TransferSummaryRow> = [
  'inboundAmountTaxIncluded',
  'outboundCostAmountExTax',
  'outboundSettlementAmountTaxIncluded',
  'inboundAvgPriceTaxIncluded',
  'outboundCostAvgPriceExTax',
  'outboundSettlementAvgPriceTaxIncluded',
];

const normalizeCategoryTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeCategoryTree(node.children) : undefined,
}));

const formatCell = (row: TransferSummaryRow, key: keyof TransferSummaryRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return moneyKeys.includes(key) ? value.toFixed(2) : value.toFixed(4);
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  if (!orgId) {
    itemCategoryTree.value = [];
    return;
  }
  optionLoading.value = true;
  try {
    const categoryRows = await fetchItemCategoryTreeApi(orgId);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
  } catch {
    itemCategoryTree.value = [];
    ElMessage.error('机构间调拨汇总表筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const fetchReport = async () => {
  loading.value = true;
  try {
    tableRows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.statisticMode = '从本店调出';
  query.dateDimension = '调拨日期';
  query.dateRange = [];
  query.statisticDimension = '门店';
  query.targetStore = '';
  query.itemKeyword = '';
  query.itemCategory = '';
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

const getSummaries = ({ columns: summaryColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return summaryColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof TransferSummaryRow | undefined;
    if (!property || (!quantityKeys.includes(property) && !moneyKeys.includes(property))) {
      return '';
    }
    const totalValue = tableRows.value.reduce((sum, row) => sum + Number(row[property] ?? 0), 0);
    return moneyKeys.includes(property) ? totalValue.toFixed(2) : totalValue.toFixed(4);
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
      <el-form-item label="统计方式">
        <el-radio-group v-model="query.statisticMode">
          <el-radio-button v-for="option in statisticModeOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="日期维度">
        <el-radio-group v-model="query.dateDimension">
          <el-radio-button v-for="option in dateDimensionOptions" :key="option" :label="option" />
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

      <el-form-item label="统计维度">
        <el-radio-group v-model="query.statisticDimension">
          <el-radio-button v-for="option in statisticDimensionOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="调入门店">
        <el-tree-select
          v-model="query.targetStore"
          :data="storeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="物品名称">
        <el-input v-model="query.itemKeyword" clearable placeholder="物品名称 / 编码 / 助记码 / 拼音码" style="width: 240px" />
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

      <el-form-item label="计量单位类型">
        <el-select v-model="query.unitType" style="width: 140px">
          <el-option v-for="option in unitTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
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
      empty-text="暂无机构间调拨汇总数据"
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
