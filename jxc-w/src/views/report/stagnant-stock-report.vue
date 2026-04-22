<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type UnitType = '库存单位';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type StagnantStockRow = {
  id: string;
  warehouse: string;
  itemName: string;
  itemCode: string;
  specModel: string;
  unit: string;
  firstInboundTime: string;
  latestInboundTime: string;
  latestOutboundTime: string;
  latestInboundQty: number;
  latestOutboundQty: number;
  stockQty: number;
  retainedDays: number;
  itemStagnantDays: number;
  stagnant: string;
};

type ReportColumn = {
  key: keyof StagnantStockRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const itemStatusTree: TreeNode[] = [
  { value: '全部', label: '全部' },
  { value: '启用', label: '启用' },
  { value: '停用', label: '停用' },
];
const stagnantTree: TreeNode[] = [
  { value: '全部', label: '全部' },
  { value: '是', label: '是' },
  { value: '否', label: '否' },
];
const unitTypeTree: TreeNode[] = [{ value: '库存单位', label: '库存单位' }];

const query = reactive({
  warehouse: '',
  itemCode: '',
  itemCategory: '',
  itemStatus: '全部',
  stagnant: '全部',
  stagnantDaysGreaterThan: '',
  unitType: '库存单位' as UnitType,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<StagnantStockRow[]>([]);
const itemTree = ref<TreeNode[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const columns: ReportColumn[] = [
  { key: 'warehouse', label: '仓库', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'firstInboundTime', label: '最早一次入库时间', minWidth: 160 },
  { key: 'latestInboundTime', label: '最近一次入库时间', minWidth: 160 },
  { key: 'latestOutboundTime', label: '最近一次出库时间', minWidth: 160 },
  { key: 'latestInboundQty', label: '最近一次入库数量', minWidth: 150, align: 'right' },
  { key: 'latestOutboundQty', label: '最近一次出库数量', minWidth: 150, align: 'right' },
  { key: 'stockQty', label: '库存数量', minWidth: 110, align: 'right' },
  { key: 'retainedDays', label: '已滞留天数', minWidth: 110, align: 'right' },
  { key: 'itemStagnantDays', label: '物品呆滞天数', minWidth: 130, align: 'right' },
  { key: 'stagnant', label: '是否呆滞', minWidth: 100 },
];

const quantityKeys: Array<keyof StagnantStockRow> = ['latestInboundQty', 'latestOutboundQty', 'stockQty'];
const integerKeys: Array<keyof StagnantStockRow> = ['retainedDays', 'itemStagnantDays'];

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

const formatCell = (row: StagnantStockRow, key: keyof StagnantStockRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  if (integerKeys.includes(key)) {
    return String(value);
  }
  return value.toFixed(4);
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await loadWarehouseTree();
    if (!orgId) {
      itemTree.value = [];
      itemCategoryTree.value = [];
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
    itemTree.value = [];
    itemCategoryTree.value = [];
    ElMessage.error('库存呆滞品查询表筛选项加载失败');
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
  query.warehouse = '';
  query.itemCode = '';
  query.itemCategory = '';
  query.itemStatus = '全部';
  query.stagnant = '全部';
  query.stagnantDaysGreaterThan = '';
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

const getSummaries = ({ columns: summaryColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return summaryColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof StagnantStockRow | undefined;
    if (!property || !quantityKeys.includes(property)) {
      return '';
    }
    return tableRows.value.reduce((sum, row) => sum + Number(row[property] ?? 0), 0).toFixed(4);
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

      <el-form-item label="物品名称">
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

      <el-form-item label="是否呆滞">
        <el-tree-select
          v-model="query.stagnant"
          :data="stagnantTree"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 120px"
        />
      </el-form-item>

      <el-form-item label="滞销天数大于">
        <el-input v-model="query.stagnantDaysGreaterThan" clearable placeholder="请输入" style="width: 140px" />
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
      empty-text="暂无库存呆滞品数据"
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
