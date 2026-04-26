<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { fetchStockWarningReportApi, type StockWarningReportRow } from '@/api/modules/inventory';
import { fetchItemCategoryTreeApi, fetchItemsApi, type ItemCategoryTreeNode, type ItemVO } from '@/api/modules/item';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useRequiredOrgScope } from '@/composables/useRequiredOrgScope';

type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

const { orgId } = useRequiredOrgScope();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const loading = ref(false);
const optionLoading = ref(false);
const rows = ref<StockWarningReportRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const warningStatusOptions: TreeNode[] = [
  { value: '正常', label: '正常' },
  { value: '库存不足', label: '库存不足' },
  { value: '库存超储', label: '库存超储' },
];

const query = reactive({
  warehouse: '',
  itemCategory: '',
  itemCode: '',
  warningStatus: '',
});

const fetchAllPages = async <T,>(
  loader: (pageNo: number, pageSizeValue: number) => Promise<{ list?: T[]; total?: number; pageSize?: number }>,
  pageSizeValue = 200,
) => {
  const collected: T[] = [];
  let pageNo = 1;
  let totalValue: number;
  do {
    const page = await loader(pageNo, pageSizeValue);
    const list = Array.isArray(page.list) ? page.list : [];
    collected.push(...list);
    totalValue = Number(page.total ?? collected.length);
    if (!list.length || Number(page.pageSize ?? 0) <= 0) {
      break;
    }
    pageNo += 1;
  } while (collected.length < totalValue);
  return collected;
};

const normalizeCategoryTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeCategoryTree(node.children) : undefined,
}));

const loadOptions = async () => {
  optionLoading.value = true;
  try {
    await loadWarehouseTree();
    if (!orgId.value) {
      itemCategoryTree.value = [];
      itemTree.value = [];
      return;
    }
    const [categoryRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId.value),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId.value)),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    itemTree.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    itemTree.value = [];
    ElMessage.error('库存上下限筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const loadRows = async () => {
  if (!orgId.value) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const page = await fetchStockWarningReportApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      statisticDimension: '仓库',
      warehouse: query.warehouse || undefined,
      itemCategory: query.itemCategory || undefined,
      itemCode: query.itemCode || undefined,
      warningStatus: query.warningStatus || undefined,
    }, orgId.value);
    rows.value = page.list ?? [];
    total.value = Number(page.total ?? 0);
  } catch {
    rows.value = [];
    total.value = 0;
    ElMessage.error('库存上下限列表加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await loadRows();
};

const handleReset = async () => {
  query.warehouse = '';
  query.itemCategory = '';
  query.itemCode = '';
  query.warningStatus = '';
  currentPage.value = 1;
  await loadRows();
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await loadRows();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await loadRows();
};

const selectedIds = ref<string[]>([]);
const handleSelectionChange = (selectedRows: StockWarningReportRow[]) => {
  selectedIds.value = selectedRows.map((row) => row.id);
};

const emptyText = computed(() => (orgId.value ? '暂无库存上下限数据' : '请先选择机构'));

watch(
  orgId,
  async () => {
    rows.value = [];
    total.value = 0;
    selectedIds.value = [];
    currentPage.value = 1;
    await loadOptions();
    await loadRows();
  },
);

onMounted(async () => {
  await loadOptions();
  await loadRows();
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
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item label="库存状态">
        <el-tree-select
          v-model="query.warningStatus"
          :data="warningStatusOptions"
          :props="{ label: 'label', value: 'value' }"
          clearable
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

    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="400"
      :empty-text="emptyText"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="warehouse" label="仓库" min-width="140" show-overflow-tooltip />
      <el-table-column prop="itemCode" label="物品编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemName" label="物品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="itemCategory" label="物品类别" min-width="120" show-overflow-tooltip />
      <el-table-column prop="unit" label="单位" min-width="80" show-overflow-tooltip />
      <el-table-column prop="stockLowerLimit" label="库存下限数量" min-width="140" align="right" show-overflow-tooltip />
      <el-table-column prop="stockUpperLimit" label="库存上限数量" min-width="140" align="right" show-overflow-tooltip />
      <el-table-column prop="currentStock" label="当前库存" min-width="100" align="right" show-overflow-tooltip />
      <el-table-column prop="warningStatus" label="库存状态" min-width="110" show-overflow-tooltip />
      <el-table-column prop="itemStatus" label="物品状态" min-width="100" show-overflow-tooltip />
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">已选 {{ selectedIds.length }} 条</div>
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
