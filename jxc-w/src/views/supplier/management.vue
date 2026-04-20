<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useRouter } from 'vue-router';
import {
  createSupplierCategoryApi,
  fetchSupplierCategoryTreeApi,
  fetchSuppliersApi,
  type SupplierCategoryTreeNode,
} from '@/api/modules/supplier';
import { useSessionStore } from '@/stores/session';
import SupplierCategoryTree from './components/SupplierCategoryTree.vue';
import SupplierPaginationSection from './components/SupplierPaginationSection.vue';
import SupplierQuerySection from './components/SupplierQuerySection.vue';
import SupplierTableSection from './components/SupplierTableSection.vue';
import SupplierToolbarSection from './components/SupplierToolbarSection.vue';
import {
  bindStatusOptions,
  defaultSupplierQuery,
  sourceOptions,
  statusOptions,
  supplierColumns,
  supplierToolbarButtons,
  supplyRelationOptions,
} from './management-data';
import type { SupplierRow } from './types';

const router = useRouter();
const sessionStore = useSessionStore();
const query = reactive({ ...defaultSupplierQuery });
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableHeight = 360;
const emptyText = '当前机构暂无数据';
const selectedRows = ref<SupplierRow[]>([]);
const selectedCount = ref(0);
const selectedCategoryId = ref('all');
const selectedCategoryLabel = ref('供应商类别');
const loading = ref(false);
const tableData = ref<SupplierRow[]>([]);
const supplierCategoryTree = ref<SupplierCategoryTreeNode[]>([]);
const categoryDialogVisible = ref(false);
const categoryFormRef = ref<FormInstance>();
const categoryForm = reactive({
  categoryName: '',
  parentCategory: '供应商类别',
});

const categoryFormRules: FormRules = {
  categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
  parentCategory: [{ required: true, message: '请选择上级类别', trigger: 'change' }],
};

const resolveSupplierOrgId = () => {
  return String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
};

const categorySelectTree = computed(() => supplierCategoryTree.value);

const fetchCategoryTree = async () => {
  try {
    supplierCategoryTree.value = await fetchSupplierCategoryTreeApi(resolveSupplierOrgId());
  } catch {
    supplierCategoryTree.value = [];
    ElMessage.error('供应商类别加载失败');
  }
  if (!findCategoryById(supplierCategoryTree.value, selectedCategoryId.value)) {
    selectedCategoryId.value = 'all';
    selectedCategoryLabel.value = '供应商类别';
  }
};

const fetchTableData = async () => {
  loading.value = true;
  try {
    const result = await fetchSuppliersApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      supplierInfo: query.supplierInfo || undefined,
      status: query.status === '全部' ? undefined : (query.status as '启用' | '停用'),
      bindStatus: query.bindStatus === '全部' ? undefined : query.bindStatus as '已绑定' | '未绑定',
      source: query.source === '全部' ? undefined : query.source as '集团' | '门店',
      supplyRelation: query.supplyRelation === '全部' ? undefined : query.supplyRelation as '有' | '无',
      treeNode: selectedCategoryId.value === 'all' ? undefined : selectedCategoryId.value,
    }, resolveSupplierOrgId());

    tableData.value = (result.list ?? []) as SupplierRow[];
    total.value = result.total ?? 0;
    selectedRows.value = [];
    selectedCount.value = 0;
  } catch {
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchTableData();
};

const handleReset = async () => {
  Object.assign(query, defaultSupplierQuery);
  selectedCategoryId.value = 'all';
  selectedCategoryLabel.value = '供应商类别';
  currentPage.value = 1;
  await fetchTableData();
};

const ensureSelection = () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择供应商');
    return false;
  }
  return true;
};

const handleToolbarAction = async (action: string) => {
  if (action === '新增') {
    router.push('/archive/3/1/create');
    return;
  }

  if (['批量启用', '批量停用', '批量删除', '批量注册绑定', '批量修改'].includes(action) && !ensureSelection()) {
    return;
  }

  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (selection: SupplierRow[]) => {
  selectedRows.value = selection;
  selectedCount.value = selection.length;
};

const handleEdit = (row: SupplierRow) => {
  router.push(`/archive/3/1/edit/${row.id}`);
};

const handleBind = (row: SupplierRow) => {
  ElMessage.info(`绑定：${row.supplierName}`);
};

const handleDelete = (row: SupplierRow) => {
  ElMessage.info(`删除：${row.supplierName}`);
};

const findCategoryById = (nodes: SupplierCategoryTreeNode[], id: string): SupplierCategoryTreeNode | null => {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    if (node.children?.length) {
      const target = findCategoryById(node.children, id);
      if (target) {
        return target;
      }
    }
  }
  return null;
};

const handleCategorySelect = async (id: string) => {
  selectedCategoryId.value = id;
  const node = findCategoryById(supplierCategoryTree.value, id);
  selectedCategoryLabel.value = node?.label ?? '供应商类别';
  currentPage.value = 1;
  await fetchTableData();
};

const openAddCategoryDialog = () => {
  categoryForm.categoryName = '';
  categoryForm.parentCategory = selectedCategoryId.value === 'all' ? '供应商类别' : selectedCategoryLabel.value;
  categoryDialogVisible.value = true;
};

const closeAddCategoryDialog = () => {
  categoryDialogVisible.value = false;
  categoryFormRef.value?.clearValidate();
};

const handleAddCategorySubmit = async () => {
  if (!categoryFormRef.value) {
    return;
  }
  const valid = await categoryFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }
  await createSupplierCategoryApi({
    categoryCode: undefined,
    categoryName: categoryForm.categoryName.trim(),
    parentCategory: categoryForm.parentCategory,
  }, resolveSupplierOrgId());
  await fetchCategoryTree();
  ElMessage.success('新增子类成功');
  closeAddCategoryDialog();
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

onMounted(async () => {
  await fetchCategoryTree();
  await fetchTableData();
});
</script>

<template>
  <div class="item-management-layout">
    <SupplierCategoryTree
      :tree-data="supplierCategoryTree"
      :selected-id="selectedCategoryId"
      @select="handleCategorySelect"
      @add-subcategory="openAddCategoryDialog"
    />

    <section class="panel item-main-panel">
      <SupplierQuerySection
        :model-value="query"
        :supplier-info-tree="supplierCategoryTree"
        :status-options="statusOptions"
        :bind-status-options="bindStatusOptions"
        :source-options="sourceOptions"
        :supply-relation-options="supplyRelationOptions"
        @search="handleSearch"
        @reset="handleReset"
      />

      <SupplierToolbarSection
        :buttons="supplierToolbarButtons"
        @action="handleToolbarAction"
      />

      <SupplierTableSection
        :data="tableData"
        :columns="supplierColumns"
        :height="tableHeight"
        :loading="loading"
        :empty-text="emptyText"
        @selection-change="handleSelectionChange"
        @edit="handleEdit"
        @bind="handleBind"
        @delete="handleDelete"
      />

      <SupplierPaginationSection
        :selected-count="selectedCount"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>
  </div>

  <el-dialog
    v-model="categoryDialogVisible"
    title="新增子类"
    width="560px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeAddCategoryDialog"
  >
      <el-form
      ref="categoryFormRef"
      class="standard-dialog-form"
      :model="categoryForm"
      :rules="categoryFormRules"
      label-width="90px"
    >
      <el-form-item label="类别名称" prop="categoryName">
        <el-input v-model="categoryForm.categoryName" placeholder="请输入类别名称" />
      </el-form-item>
      <el-form-item label="上级类别" prop="parentCategory">
        <el-tree-select
          v-model="categoryForm.parentCategory"
          :data="categorySelectTree"
          :props="{ label: 'label', value: 'label', children: 'children' }"
          check-strictly
          default-expand-all
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeAddCategoryDialog">取消</el-button>
      <el-button type="primary" @click="handleAddCategorySubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
