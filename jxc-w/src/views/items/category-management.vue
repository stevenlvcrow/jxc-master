<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import ItemCategoryTree from './components/ItemCategoryTree.vue';
import ItemPaginationSection from './components/ItemPaginationSection.vue';
import {
  batchCreateItemCategoriesApi,
  createItemCategoryApi,
  deleteItemCategoryApi,
  fetchItemCategoriesApi,
  fetchItemCategoryTreeApi,
  updateItemCategoryApi,
  type ItemCategoryTreeNode,
} from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';

type CategoryTableRow = {
  id: number;
  index: number;
  categoryCode: string;
  categoryName: string;
  parentCategory: string;
  status: '启用' | '停用';
  createdAt: string;
  remark: string | null;
};

type CategoryTableColumn = {
  prop: keyof CategoryTableRow;
  label: string;
  width: number;
  fixed?: 'left' | 'right';
};

type CategorySelectNode = {
  label: string;
  value: string;
  children?: CategorySelectNode[];
};
type BatchCreateRow = {
  categoryCode: string;
  categoryName: string;
};

const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();
const total = ref(0);
const tableLoading = ref(false);
const emptyText = '当前机构暂无数据';
const selectedCount = ref(0);
const tableHeight = 400;
const statusOptions = ['全部', '启用', '停用'] as const;
const toolbarButtons = ['新增', '批量新增', '批量导入', '分类排序'];
const query = reactive<{
  categoryInfo: string;
  status: '全部' | '启用' | '停用';
}>({
  categoryInfo: '',
  status: '全部',
});
const createDialogVisible = ref(false);
const batchCreateDialogVisible = ref(false);
const createFormRef = ref<FormInstance>();
const rootCategoryName = '物品类别';
const selectedTreeNode = ref(rootCategoryName);
const sortByParentCategory = ref(false);
const editingCategoryId = ref<number | null>(null);
const isEditMode = computed(() => editingCategoryId.value !== null);
const createDialogTitle = computed(() => (isEditMode.value ? '编辑物品类别' : '新增物品类别'));
const batchCreateForm = reactive({
  parentCategory: rootCategoryName,
});
const batchCreateRows = ref<BatchCreateRow[]>([
  { categoryCode: '', categoryName: '' },
]);

const createForm = reactive({
  categoryCode: '',
  categoryName: '',
  parentCategory: rootCategoryName,
  status: '启用' as '启用' | '停用',
  remark: '',
});

const categoryColumns: CategoryTableColumn[] = [
  { prop: 'index', label: '序号', width: 56, fixed: 'left' },
  { prop: 'categoryCode', label: '类别编码', width: 140, fixed: 'left' },
  { prop: 'categoryName', label: '物品类别', width: 180 },
  { prop: 'parentCategory', label: '上级类别', width: 180 },
  { prop: 'status', label: '状态', width: 96 },
  { prop: 'createdAt', label: '创建时间', width: 180 },
];

const categoryTableData = ref<CategoryTableRow[]>([]);
const categoryTree = ref<ItemCategoryTreeNode[]>([{ label: rootCategoryName, children: [] }]);

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

const categorySelectTree = computed<CategorySelectNode[]>(() => {
  const toSelectNodes = (nodes: ItemCategoryTreeNode[]): CategorySelectNode[] => nodes.map((node) => ({
    label: node.label,
    value: node.label,
    children: node.children?.length ? toSelectNodes(node.children) : undefined,
  }));
  return toSelectNodes(categoryTree.value);
});

const createFormRules: FormRules = {
  categoryCode: [{ required: true, message: '请输入类别编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入物品类别', trigger: 'blur' }],
  parentCategory: [{ required: true, message: '请选择上级类别', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const fetchCategoryList = async () => {
  tableLoading.value = true;
  try {
    const result = await fetchItemCategoriesApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      categoryInfo: query.categoryInfo.trim() || undefined,
      status: query.status === '全部' ? undefined : query.status,
      treeNode: selectedTreeNode.value === rootCategoryName ? undefined : selectedTreeNode.value,
      sortBy: sortByParentCategory.value ? 'parentCategory' : undefined,
    }, resolveItemOrgId());
    categoryTableData.value = result.list ?? [];
    total.value = result.total ?? 0;
    selectedCount.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const fetchCategoryTree = async () => {
  const tree = await fetchItemCategoryTreeApi(resolveItemOrgId());
  categoryTree.value = tree.length ? tree : [{ label: rootCategoryName, children: [] }];
};

const handleSelectionChange = (rows: CategoryTableRow[]) => {
  selectedCount.value = rows.length;
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchCategoryList();
};

const handleReset = async () => {
  query.categoryInfo = '';
  query.status = '全部';
  selectedTreeNode.value = rootCategoryName;
  sortByParentCategory.value = false;
  currentPage.value = 1;
  await fetchCategoryList();
};

const handleTreeSelect = async (label: string) => {
  selectedTreeNode.value = label;
  currentPage.value = 1;
  await fetchCategoryList();
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchCategoryList();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchCategoryList();
};

const resetCreateForm = () => {
  editingCategoryId.value = null;
  createForm.categoryCode = '';
  createForm.categoryName = '';
  createForm.parentCategory = rootCategoryName;
  createForm.status = '启用';
  createForm.remark = '';
};

const openCreateDialog = () => {
  resetCreateForm();
  createDialogVisible.value = true;
};

const openEditDialog = (row: CategoryTableRow) => {
  editingCategoryId.value = row.id;
  createForm.categoryCode = row.categoryCode;
  createForm.categoryName = row.categoryName;
  createForm.parentCategory = row.parentCategory;
  createForm.status = row.status;
  createForm.remark = row.remark ?? '';
  createDialogVisible.value = true;
};

const closeCreateDialog = () => {
  createDialogVisible.value = false;
  createFormRef.value?.clearValidate();
};

const resetBatchCreateForm = () => {
  batchCreateForm.parentCategory = rootCategoryName;
  batchCreateRows.value = [{ categoryCode: '', categoryName: '' }];
};

const openBatchCreateDialog = () => {
  resetBatchCreateForm();
  batchCreateDialogVisible.value = true;
};

const closeBatchCreateDialog = () => {
  batchCreateDialogVisible.value = false;
};

const addBatchRow = (index: number) => {
  batchCreateRows.value.splice(index + 1, 0, { categoryCode: '', categoryName: '' });
};

const removeBatchRow = (index: number) => {
  if (batchCreateRows.value.length === 1) {
    ElMessage.warning('至少保留一行');
    return;
  }
  batchCreateRows.value.splice(index, 1);
};

const handleCreateSubmit = async () => {
  if (!createFormRef.value) {
    return;
  }
  const valid = await createFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  const payload = {
    categoryCode: createForm.categoryCode.trim(),
    categoryName: createForm.categoryName.trim(),
    parentCategory: createForm.parentCategory,
    status: createForm.status,
    remark: createForm.remark.trim() || undefined,
  };

  if (isEditMode.value && editingCategoryId.value) {
    await updateItemCategoryApi(editingCategoryId.value, payload, resolveItemOrgId());
    ElMessage.success('编辑类别成功');
  } else {
    await createItemCategoryApi(payload, resolveItemOrgId());
    ElMessage.success('新增类别成功');
  }

  await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
  closeCreateDialog();
};

const handleBatchCreateSubmit = async () => {
  const rows = batchCreateRows.value.map((row) => ({
    categoryCode: row.categoryCode.trim(),
    categoryName: row.categoryName.trim(),
  }));

  const emptyCodeIndex = rows.findIndex((row) => !row.categoryCode);
  if (emptyCodeIndex !== -1) {
    ElMessage.warning(`第 ${emptyCodeIndex + 1} 行类别编码不能为空`);
    return;
  }

  const emptyNameIndex = rows.findIndex((row) => !row.categoryName);
  if (emptyNameIndex !== -1) {
    ElMessage.warning(`第 ${emptyNameIndex + 1} 行类别名称不能为空`);
    return;
  }

  const duplicateCode = rows.find((row, index) => rows.findIndex((item) => item.categoryCode === row.categoryCode) !== index);
  if (duplicateCode) {
    ElMessage.warning(`批量新增中类别编码重复：${duplicateCode.categoryCode}`);
    return;
  }

  const duplicateName = rows.find((row, index) => rows.findIndex((item) => item.categoryName === row.categoryName) !== index);
  if (duplicateName) {
    ElMessage.warning(`批量新增中类别名称重复：${duplicateName.categoryName}`);
    return;
  }

  await batchCreateItemCategoriesApi({
    parentCategory: batchCreateForm.parentCategory,
    status: '启用',
    items: rows.map((row) => ({
      categoryCode: row.categoryCode,
      categoryName: row.categoryName,
    })),
  }, resolveItemOrgId());

  await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
  closeBatchCreateDialog();
  ElMessage.success(`批量新增成功，共 ${rows.length} 条`);
};

const handleDelete = async (row: CategoryTableRow) => {
  try {
    await ElMessageBox.confirm(`确认删除类别“${row.categoryName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await deleteItemCategoryApi(row.id, resolveItemOrgId());
    await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
    ElMessage.success('删除类别成功');
  } catch {
    // 用户取消删除时忽略
  }
};

const handleToolbarAction = async (action: string) => {
  if (action === '新增') {
    openCreateDialog();
    return;
  }
  if (action === '批量新增') {
    openBatchCreateDialog();
    return;
  }
  if (action === '分类排序') {
    sortByParentCategory.value = true;
    currentPage.value = 1;
    await fetchCategoryList();
    ElMessage.success('已按上级类别排序');
    return;
  }
  ElMessage.info(`${action}功能待接入`);
};

onMounted(async () => {
  await Promise.all([fetchCategoryList(), fetchCategoryTree()]);
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
      <el-form :model="query" inline class="compact-filter-bar">
        <el-form-item label="类别信息">
          <el-input
            v-model="query.categoryInfo"
            placeholder="请输入类别编码或名称"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 110px">
            <el-option
              v-for="option in statusOptions"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button
          v-for="(button, index) in toolbarButtons"
          :key="button"
          :type="index === 0 ? 'primary' : 'default'"
          @click="handleToolbarAction(button)"
        >
          {{ button }}
        </el-button>
      </div>

      <el-table
        v-loading="tableLoading"
        :data="categoryTableData"
        :fit="false"
        border
        stripe
        scrollbar-always-on
        class="erp-table"
        :height="tableHeight"
        :empty-text="emptyText"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column
          v-for="column in categoryColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :fixed="column.fixed"
          show-overflow-tooltip
        >
          <template v-if="column.prop === 'status'" #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

  <el-dialog
    v-model="createDialogVisible"
    :title="createDialogTitle"
    width="560px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeCreateDialog"
  >
    <el-form
      ref="createFormRef"
      class="standard-dialog-form category-create-form"
      :model="createForm"
      :rules="createFormRules"
      label-width="90px"
    >
      <el-form-item label="类别编码" prop="categoryCode">
        <el-input
          v-model="createForm.categoryCode"
          placeholder="请输入类别编码"
        />
      </el-form-item>
      <el-form-item label="物品类别" prop="categoryName">
        <el-input
          v-model="createForm.categoryName"
          placeholder="请输入物品类别名称"
        />
      </el-form-item>
      <el-form-item label="上级类别" prop="parentCategory">
        <el-tree-select
          v-model="createForm.parentCategory"
          class="category-tree-select"
          :data="categorySelectTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          check-strictly
          default-expand-all
          popper-class="category-tree-select-popper"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="createForm.status">
          <el-radio label="启用">启用</el-radio>
          <el-radio label="停用">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="createForm.remark"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="closeCreateDialog">取消</el-button>
      <el-button type="primary" @click="handleCreateSubmit">{{ isEditMode ? '保存' : '确定' }}</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="batchCreateDialogVisible"
    title="批量新增物品类别"
    width="760px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeBatchCreateDialog"
  >
    <el-form :model="batchCreateForm" label-width="90px" class="standard-dialog-form category-create-form">
      <el-form-item label="上级类别">
        <el-tree-select
          v-model="batchCreateForm.parentCategory"
          class="category-tree-select"
          :data="categorySelectTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          check-strictly
          default-expand-all
          popper-class="category-tree-select-popper"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <el-table :data="batchCreateRows" border class="erp-table">
      <el-table-column label="序号" width="70">
        <template #default="{ $index }">
          {{ $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ $index }">
          <el-button text type="primary" @click="addBatchRow($index)">+</el-button>
          <el-button text @click="removeBatchRow($index)">-</el-button>
        </template>
      </el-table-column>
      <el-table-column label="类别编码" width="220">
        <template #default="{ row }">
          <el-input v-model="row.categoryCode" placeholder="请输入类别编码" />
        </template>
      </el-table-column>
      <el-table-column label="类别名称">
        <template #default="{ row }">
          <el-input v-model="row.categoryName" placeholder="请输入类别名称" />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="closeBatchCreateDialog">取消</el-button>
      <el-button type="primary" @click="handleBatchCreateSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<style>
.category-create-form .el-input__wrapper {
  min-height: 22px;
  height: 22px;
}

.category-create-form .el-input__inner {
  font-size: 10px;
}

.category-tree-select .el-select__wrapper {
  min-height: 22px;
  height: 22px;
}

.category-tree-select-popper .el-tree-node__content {
  height: 22px;
  font-size: 10px;
}

.category-tree-select-popper .el-tree-node__label {
  font-size: 10px;
}
</style>
