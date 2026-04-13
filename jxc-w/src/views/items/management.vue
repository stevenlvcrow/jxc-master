<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  batchDeleteItemsApi,
  batchUpdateItemStatusApi,
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemStatus,
} from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
import ItemCategoryTree from './components/ItemCategoryTree.vue';
import ItemPaginationSection from './components/ItemPaginationSection.vue';
import ItemQuerySection from './components/ItemQuerySection.vue';
import ItemTableSection from './components/ItemTableSection.vue';
import ItemToolbarSection from './components/ItemToolbarSection.vue';
import {
  defaultItemQuery,
  itemColumns,
  itemToolbarButtons,
  itemTypeOptions,
  statTypeOptions,
  statusOptions,
  storageModeOptions,
} from './management-data';
import type { ItemRow } from './types';

const router = useRouter();
const sessionStore = useSessionStore();

const query = reactive({ ...defaultItemQuery });
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableHeight = 360;
const tableData = ref<ItemRow[]>([]);
const selectedRows = ref<ItemRow[]>([]);
const loading = ref(false);
const emptyText = ref('当前机构暂无数据');

const selectedCount = ref(0);
const rootCategoryName = '物品类别';
const selectedTreeNode = ref(rootCategoryName);
const categoryTree = ref<ItemCategoryTreeNode[]>([{ label: rootCategoryName, children: [] }]);
const selectedTreeCategories = ref<string[]>([]);

const isCancelError = (error: unknown) => {
  if (error === 'cancel' || error === 'close') {
    return true;
  }
  if (error instanceof Error) {
    return error.message.includes('cancel');
  }
  return false;
};

const resolveItemOrgId = () => {
  const orgId = (sessionStore.currentOrgId ?? '').trim();
  if (!orgId) {
    return undefined;
  }
  if (orgId.startsWith('group-') || orgId.startsWith('store-')) {
    return orgId;
  }
  return undefined;
};

const fetchCategoryTree = async () => {
  const tree = await fetchItemCategoryTreeApi(resolveItemOrgId());
  categoryTree.value = tree.length ? tree : [{ label: rootCategoryName, children: [] }];
};

const fetchTableData = async () => {
  loading.value = true;
  try {
    const res = await fetchItemsApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      keyword: query.keyword,
      category: selectedTreeCategories.value.length ? selectedTreeCategories.value.join(',') : undefined,
      status: query.status,
      itemType: query.itemType,
      statType: query.statType,
      storageMode: query.storageMode,
      tag: query.tag,
    }, resolveItemOrgId());

    tableData.value = res.list;
    total.value = res.total;
    selectedRows.value = [];
    selectedCount.value = 0;
    emptyText.value = '当前机构暂无数据';
  } catch {
    tableData.value = [];
    total.value = 0;
    emptyText.value = '当前机构暂无数据';
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchTableData();
};

const handleReset = async () => {
  Object.assign(query, defaultItemQuery);
  selectedTreeNode.value = rootCategoryName;
  selectedTreeCategories.value = [];
  currentPage.value = 1;
  await fetchTableData();
};

const collectCategoryLabels = (nodes: ItemCategoryTreeNode[]): string[] => {
  const labels: string[] = [];
  const walk = (currentNodes: ItemCategoryTreeNode[]) => {
    currentNodes.forEach((node) => {
      labels.push(node.label);
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };
  walk(nodes);
  return labels.filter((label) => label !== rootCategoryName);
};

const findNodeByLabel = (nodes: ItemCategoryTreeNode[], label: string): ItemCategoryTreeNode | null => {
  for (const node of nodes) {
    if (node.label === label) {
      return node;
    }
    if (node.children?.length) {
      const child = findNodeByLabel(node.children, label);
      if (child) {
        return child;
      }
    }
  }
  return null;
};

const resolveTreeCategories = (label: string) => {
  if (label === rootCategoryName) {
    return [];
  }
  const selectedNode = findNodeByLabel(categoryTree.value, label);
  if (!selectedNode) {
    return [label];
  }
  const categories = collectCategoryLabels([selectedNode]);
  return categories.length ? categories : [label];
};

const handleTreeSelect = async (label: string) => {
  selectedTreeNode.value = label;
  selectedTreeCategories.value = resolveTreeCategories(label);
  currentPage.value = 1;
  await fetchTableData();
};

const batchUpdateStatus = async (status: ItemStatus) => {
  const ids = selectedRows.value.map((item) => item.id);
  if (!ids.length) {
    ElMessage.warning('请先选择物品');
    return;
  }

  await batchUpdateItemStatusApi(ids, status, resolveItemOrgId());
  ElMessage.success(`批量${status}成功`);
  await fetchTableData();
};

const batchDelete = async () => {
  const ids = selectedRows.value.map((item) => item.id);
  if (!ids.length) {
    ElMessage.warning('请先选择物品');
    return;
  }

  try {
    await ElMessageBox.confirm(`确认删除已选 ${ids.length} 条物品吗？`, '批量删除', {
      type: 'warning',
    });
  } catch (error) {
    if (isCancelError(error)) {
      return;
    }
    throw error;
  }

  await batchDeleteItemsApi(ids, resolveItemOrgId());
  ElMessage.success('批量删除成功');
  if ((currentPage.value - 1) * pageSize.value >= Math.max(total.value - ids.length, 0) && currentPage.value > 1) {
    currentPage.value -= 1;
  }
  await fetchTableData();
};

const handleToolbarAction = async (action: string) => {
  try {
    if (action === '新增物品') {
      router.push('/archive/1/1/create');
      return;
    }

    if (action === '批量启用') {
      await batchUpdateStatus('启用');
      return;
    }

    if (action === '批量停用') {
      await batchUpdateStatus('停用');
      return;
    }

    if (action === '批量删除') {
      await batchDelete();
      return;
    }

    ElMessage.info(`${action}功能待接入`);
  } catch {
    // Global error message handled in http interceptor.
  }
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchTableData();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchTableData();
};

const handleSelectionChange = (rows: ItemRow[]) => {
  selectedRows.value = rows;
  selectedCount.value = rows.length;
};

const handleToggleStatus = async (row: ItemRow) => {
  try {
    const targetStatus: ItemStatus = row.status === '启用' ? '停用' : '启用';
    await batchUpdateItemStatusApi([row.id], targetStatus, resolveItemOrgId());
    ElMessage.success(`${row.name}已${targetStatus}`);
    await fetchTableData();
  } catch {
    // Global error message handled in http interceptor.
  }
};

const handleEditOne = (row: ItemRow) => {
  router.push({
    path: '/archive/1/1/create',
    query: {
      id: row.id,
      mode: 'edit',
    },
  });
};

const handleDeleteOne = async (row: ItemRow) => {
  try {
    await ElMessageBox.confirm(`确认删除物品“${row.name}”吗？`, '删除确认', {
      type: 'warning',
    });
    await batchDeleteItemsApi([row.id], resolveItemOrgId());
    ElMessage.success('删除成功');
    if ((currentPage.value - 1) * pageSize.value >= Math.max(total.value - 1, 0) && currentPage.value > 1) {
      currentPage.value -= 1;
    }
    await fetchTableData();
  } catch (error) {
    if (isCancelError(error)) {
      return;
    }
  }
};

onMounted(async () => {
  await Promise.all([fetchTableData(), fetchCategoryTree()]);
});
</script>

<template>
  <div class="item-management-layout">
    <ItemCategoryTree
      :tree-data="categoryTree"
      :selected-label="selectedTreeNode"
      @select="handleTreeSelect"
    />

    <section class="panel item-main-panel">
      <ItemQuerySection
        :model-value="query"
        :status-options="statusOptions"
        :item-type-options="itemTypeOptions"
        :stat-type-options="statTypeOptions"
        :storage-mode-options="storageModeOptions"
        @search="handleSearch"
        @reset="handleReset"
      />

      <ItemToolbarSection
        :buttons="itemToolbarButtons"
        @action="handleToolbarAction"
      />

      <ItemTableSection
        :data="tableData"
        :columns="itemColumns"
        :height="tableHeight"
        :loading="loading"
        :empty-text="emptyText"
        @selection-change="handleSelectionChange"
        @edit="handleEditOne"
        @toggle-status="handleToggleStatus"
        @delete="handleDeleteOne"
      />

      <ItemPaginationSection
        :selected-count="selectedCount"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>
  </div>
</template>
