<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  batchImportItemTagsApi,
  createItemTagApi,
  deleteItemTagApi,
  fetchItemTagsApi,
  updateItemTagApi,
  type ItemTagRow,
} from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';

type TagDialogForm = {
  tagCode: string;
  tagName: string;
  itemName: string;
};

type BatchImportRow = {
  tagName: string;
  itemName: string;
};

const sessionStore = useSessionStore();
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableHeight = 360;
const tableLoading = ref(false);
const emptyText = '当前机构暂无数据';
const tableData = ref<ItemTagRow[]>([]);
const toolbarButtons = ['新增标签', '批量导入'];
const query = reactive({
  tagCode: '',
  tagName: '',
  itemName: '',
});

const createDialogVisible = ref(false);
const batchImportDialogVisible = ref(false);
const editingTagId = ref<number | null>(null);
const dialogTitle = ref('新增标签');
const formRef = ref<FormInstance>();
const form = reactive<TagDialogForm>({
  tagCode: '',
  tagName: '',
  itemName: '',
});

const batchRows = ref<BatchImportRow[]>([
  { tagName: '', itemName: '' },
]);

const formRules: FormRules<TagDialogForm> = {
  tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
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

const fetchList = async () => {
  tableLoading.value = true;
  try {
    const data = await fetchItemTagsApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      tagCode: query.tagCode.trim() || undefined,
      tagName: query.tagName.trim() || undefined,
      itemName: query.itemName.trim() || undefined,
    }, resolveItemOrgId());
    tableData.value = data.list ?? [];
    total.value = data.total ?? 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchList();
};

const handleReset = async () => {
  query.tagCode = '';
  query.tagName = '';
  query.itemName = '';
  currentPage.value = 1;
  await fetchList();
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchList();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchList();
};

const resetForm = () => {
  editingTagId.value = null;
  form.tagCode = '';
  form.tagName = '';
  form.itemName = '';
};

const openCreateDialog = () => {
  resetForm();
  dialogTitle.value = '新增标签';
  createDialogVisible.value = true;
};

const openEditDialog = (row: ItemTagRow) => {
  editingTagId.value = row.id;
  form.tagCode = row.tagCode;
  form.tagName = row.tagName;
  form.itemName = row.itemName ?? '';
  dialogTitle.value = '编辑标签';
  createDialogVisible.value = true;
};

const closeCreateDialog = () => {
  createDialogVisible.value = false;
  formRef.value?.clearValidate();
};

const handleSubmit = async () => {
  if (!formRef.value) {
    return;
  }
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  const payload = {
    tagCode: editingTagId.value !== null ? form.tagCode.trim() : undefined,
    tagName: form.tagName.trim(),
    itemName: form.itemName.trim() || undefined,
  };

  if (editingTagId.value) {
    await updateItemTagApi(editingTagId.value, payload, resolveItemOrgId());
    ElMessage.success('编辑标签成功');
  } else {
    await createItemTagApi(payload, resolveItemOrgId());
    ElMessage.success('新增标签成功');
  }
  await fetchList();
  closeCreateDialog();
};

const handleDelete = async (row: ItemTagRow) => {
  try {
    await ElMessageBox.confirm(`确认删除标签“${row.tagName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await deleteItemTagApi(row.id, resolveItemOrgId());
    ElMessage.success('删除标签成功');
    await fetchList();
  } catch {
    // 用户取消删除
  }
};

const resetBatchRows = () => {
  batchRows.value = [{ tagName: '', itemName: '' }];
};

const openBatchDialog = () => {
  resetBatchRows();
  batchImportDialogVisible.value = true;
};

const closeBatchDialog = () => {
  batchImportDialogVisible.value = false;
};

const addBatchRow = (index: number) => {
  batchRows.value.splice(index + 1, 0, { tagName: '', itemName: '' });
};

const removeBatchRow = (index: number) => {
  if (batchRows.value.length === 1) {
    ElMessage.warning('至少保留一行');
    return;
  }
  batchRows.value.splice(index, 1);
};

const handleBatchImportSubmit = async () => {
  const rows = batchRows.value.map((row) => ({
    tagName: row.tagName.trim(),
    itemName: row.itemName.trim(),
  }));

  const emptyName = rows.findIndex((row) => !row.tagName);
  if (emptyName !== -1) {
    ElMessage.warning(`第 ${emptyName + 1} 行标签名称不能为空`);
    return;
  }

  await batchImportItemTagsApi({
    items: rows.map((row) => ({
      tagName: row.tagName,
      itemName: row.itemName || undefined,
    })),
  }, resolveItemOrgId());

  ElMessage.success('批量导入完成');
  closeBatchDialog();
  await fetchList();
};

const handleToolbarAction = (action: string) => {
  if (action === '新增标签') {
    openCreateDialog();
    return;
  }
  if (action === '批量导入') {
    openBatchDialog();
    return;
  }
  ElMessage.info(`${action}功能待接入`);
};

onMounted(async () => {
  await fetchList();
});
</script>

<template>
  <section class="panel item-main-panel">
    <el-form :model="query" inline class="compact-filter-bar">
      <el-form-item label="标签编码">
        <el-input
          v-model="query.tagCode"
          placeholder="请输入标签编码"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="标签名称">
        <el-input
          v-model="query.tagName"
          placeholder="请输入标签名称"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="物品">
        <el-input
          v-model="query.itemName"
          placeholder="请输入物品"
          clearable
          style="width: 140px"
        />
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
      :data="tableData"
      :fit="false"
      border
      stripe
      scrollbar-always-on
      class="erp-table"
      :height="tableHeight"
      :empty-text="emptyText"
    >
      <el-table-column prop="index" label="序号" width="60" align="center" />
      <el-table-column prop="tagCode" label="标签编码" min-width="160" show-overflow-tooltip />
      <el-table-column prop="tagName" label="标签名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="最后修改时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button text @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ total }} 条</div>
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

  <el-dialog
    v-model="createDialogVisible"
    :title="dialogTitle"
    width="520px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeCreateDialog"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px" class="standard-dialog-form">
      <el-form-item v-if="editingTagId !== null" label="标签编码" prop="tagCode">
        <el-input v-model="form.tagCode" placeholder="请输入标签编码" />
      </el-form-item>
      <el-form-item label="标签名称" prop="tagName">
        <el-input v-model="form.tagName" placeholder="请输入标签名称" />
      </el-form-item>
      <el-form-item label="物品">
        <el-input v-model="form.itemName" placeholder="请输入物品名称" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="closeCreateDialog">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="batchImportDialogVisible"
    title="批量导入标签"
    width="760px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeBatchDialog"
  >
    <el-table :data="batchRows" border class="erp-table">
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
      <el-table-column label="标签名称" width="180">
        <template #default="{ row }">
          <el-input v-model="row.tagName" placeholder="请输入标签名称" />
        </template>
      </el-table-column>
      <el-table-column label="物品">
        <template #default="{ row }">
          <el-input v-model="row.itemName" placeholder="请输入物品名称" />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="closeBatchDialog">取消</el-button>
      <el-button type="primary" @click="handleBatchImportSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
