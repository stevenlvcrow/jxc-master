<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchStockWarningReportApi } from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticDimension = '机构' | '仓库';
type ItemStatus = '启用' | '停用';
type WarningStatus = '全部' | '库存不足' | '库存超储';
type UnitType = '库存单位';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type StockWarningRow = {
  id: string;
  itemCode: string;
  itemName: string;
  unit: string;
  itemCategory: string;
  warehouse?: string;
  currentStock: number;
  stockUpperLimit: number;
  stockLowerLimit: number;
  warningStatus: string;
};

type ReportColumn = {
  key: keyof StockWarningRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right' | 'center';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const statisticDimensionOptions: StatisticDimension[] = ['机构', '仓库'];
const itemStatusTree: TreeNode[] = [
  { value: '启用', label: '启用' },
  { value: '停用', label: '停用' },
];
const warningStatusTree: TreeNode[] = [
  { value: '全部', label: '全部' },
  { value: '库存不足', label: '库存不足' },
  { value: '库存超储', label: '库存超储' },
];
const unitTypeTree: TreeNode[] = [{ value: '库存单位', label: '库存单位' }];

const query = reactive({
  statisticDimension: '机构' as StatisticDimension,
  warehouse: '',
  itemCategory: '',
  itemCode: '',
  itemStatus: '启用' as ItemStatus,
  warningStatus: '全部' as WarningStatus,
  unitType: '库存单位' as UnitType,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<StockWarningRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const isWarehouseDimension = computed(() => query.statisticDimension === '仓库');

const baseColumns: ReportColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 150, fixed: 'left' },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'itemCategory', label: '物品类别', minWidth: 130 },
  { key: 'currentStock', label: '当前库存数', minWidth: 120, align: 'right' },
  { key: 'stockUpperLimit', label: '库存上限', minWidth: 110, align: 'right' },
  { key: 'stockLowerLimit', label: '库存下限', minWidth: 110, align: 'right' },
  { key: 'warningStatus', label: '预警状态', minWidth: 110 },
];

const warehouseColumns: ReportColumn[] = [
  ...baseColumns.slice(0, 4),
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  ...baseColumns.slice(4),
];

const visibleColumns = computed(() => (isWarehouseDimension.value ? warehouseColumns : baseColumns));
const numericKeys: Array<keyof StockWarningRow> = ['currentStock', 'stockUpperLimit', 'stockLowerLimit'];

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

const formatCell = (row: StockWarningRow, key: keyof StockWarningRow) => {
  const value = row[key];
  if (typeof value === 'number') {
    return value.toFixed(2);
  }
  return String(value || '-');
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
    ElMessage.error('库存预警表筛选项加载失败');
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
    const page = await fetchStockWarningReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      statisticDimension: query.statisticDimension,
      warehouse: query.warehouse || undefined,
      itemCategory: query.itemCategory || undefined,
      itemCode: query.itemCode || undefined,
      itemStatus: query.itemStatus,
      warningStatus: query.warningStatus === '全部' ? undefined : query.warningStatus,
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
  query.statisticDimension = '机构';
  query.warehouse = '';
  query.itemCategory = '';
  query.itemCode = '';
  query.itemStatus = '启用';
  query.warningStatus = '全部';
  query.unitType = '库存单位';
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
    const property = column.property as keyof StockWarningRow | undefined;
    if (!property || !numericKeys.includes(property)) {
      return '';
    }
    return tableRows.value.reduce((sum, row) => sum + Number(row[property] ?? 0), 0).toFixed(2);
  });
};

watch(
  () => query.statisticDimension,
  () => {
    if (!isWarehouseDimension.value) {
      query.warehouse = '';
      query.itemStatus = '启用';
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
        <el-select v-model="query.statisticDimension" style="width: 120px">
          <el-option v-for="option in statisticDimensionOptions" :key="option" :label="option" :value="option" />
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
          placeholder="请选择"
          style="width: 180px"
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

      <el-form-item label="物品信息">
        <el-tree-select
          v-model="query.itemCode"
          :data="itemTree"
          :props="{ label: 'label', value: 'value' }"
          :loading="optionLoading"
          clearable
          filterable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 220px"
        />
      </el-form-item>

      <el-form-item label="物品状态">
        <el-tree-select
          v-model="query.itemStatus"
          :data="itemStatusTree"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 120px"
        />
      </el-form-item>

      <el-form-item label="预警状态">
        <el-tree-select
          v-model="query.warningStatus"
          :data="warningStatusTree"
          :props="{ label: 'label', value: 'value' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item label="单位类型">
        <el-tree-select
          v-model="query.unitType"
          :data="unitTypeTree"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 120px"
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
      empty-text="暂无库存预警数据"
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
          <el-tag v-if="column.key === 'warningStatus' && row.warningStatus" type="warning" size="small">
            {{ row.warningStatus }}
          </el-tag>
          <span v-else>{{ formatCell(row, column.key) }}</span>
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
