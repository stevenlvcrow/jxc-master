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

type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type OtherInoutSummaryRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  baseUnit: string;
  warehouse: string;
  inoutType: string;
  reasonType: string;
  quantity: number;
  amountExTax: number;
};

type ReportColumn = {
  key: keyof OtherInoutSummaryRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const inoutTypeTree: TreeNode[] = [
  {
    value: '入库',
    label: '入库',
    children: [
      { value: '其他入库', label: '其他入库' },
      { value: '盘盈入库', label: '盘盈入库' },
      { value: '调整入库', label: '调整入库' },
    ],
  },
  {
    value: '出库',
    label: '出库',
    children: [
      { value: '其他出库', label: '其他出库' },
      { value: '盘亏出库', label: '盘亏出库' },
      { value: '调整出库', label: '调整出库' },
    ],
  },
];
const reasonTypeTree: TreeNode[] = [
  { value: '盘盈', label: '盘盈' },
  { value: '盘亏', label: '盘亏' },
  { value: '报损', label: '报损' },
  { value: '调整', label: '调整' },
  { value: '赠品', label: '赠品' },
  { value: '其他', label: '其他' },
];
const itemStatusTree: TreeNode[] = [
  { value: '全部', label: '全部' },
  { value: '启用', label: '启用' },
  { value: '停用', label: '停用' },
];

const query = reactive({
  dateRange: [] as string[],
  warehouse: '',
  itemCategory: '',
  itemCode: '',
  inoutType: '',
  reasonType: '',
  itemStatus: '全部',
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<OtherInoutSummaryRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const columns: ReportColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'baseUnit', label: '基准单位', minWidth: 100 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'inoutType', label: '出入库类型', minWidth: 130 },
  { key: 'reasonType', label: '原因类型', minWidth: 110 },
  { key: 'quantity', label: '数量', minWidth: 110, align: 'right' },
  { key: 'amountExTax', label: '金额（不含税）', minWidth: 140, align: 'right' },
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

const formatCell = (row: OtherInoutSummaryRow, key: keyof OtherInoutSummaryRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return key === 'amountExTax' ? value.toFixed(2) : value.toFixed(4);
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  if (!orgId) {
    itemCategoryTree.value = [];
    itemTree.value = [];
    await loadWarehouseTree();
    return;
  }
  optionLoading.value = true;
  try {
    const [categoryRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId)),
      loadWarehouseTree(),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    itemTree.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    itemTree.value = [];
    ElMessage.error('其他出入库汇总表筛选项加载失败');
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
  query.dateRange = [];
  query.warehouse = '';
  query.itemCategory = '';
  query.itemCode = '';
  query.inoutType = '';
  query.reasonType = '';
  query.itemStatus = '全部';
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
  const quantityTotal = tableRows.value.reduce((sum, row) => sum + row.quantity, 0);
  const amountTotal = tableRows.value.reduce((sum, row) => sum + row.amountExTax, 0);
  return summaryColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    if (column.property === 'quantity') {
      return quantityTotal.toFixed(4);
    }
    if (column.property === 'amountExTax') {
      return amountTotal.toFixed(2);
    }
    return '';
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

      <el-form-item label="物品">
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
          style="width: 200px"
        />
      </el-form-item>

      <el-form-item label="出入库类型">
        <el-tree-select
          v-model="query.inoutType"
          :data="inoutTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="原因类型">
        <el-tree-select
          v-model="query.reasonType"
          :data="reasonTypeTree"
          :props="{ label: 'label', value: 'value' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 140px"
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
      empty-text="暂无其他出入库汇总数据"
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
