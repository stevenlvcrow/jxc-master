<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createUnitApi, deleteUnitApi, fetchUnitsApi, updateUnitApi, type UnitItem } from '@/api/modules/unit';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import { useSessionStore } from '@/stores/session';

type UnitType = 'STANDARD' | 'AUXILIARY';
type UnitStatus = 'ENABLED' | 'DISABLED';

type UnitRecord = {
  id: number;
  code: string;
  name: string;
  type: UnitType;
  status: UnitStatus;
  createdAt: string;
};

const query = reactive<{
  keyword: string;
  status: UnitStatus | 'ALL';
  unitType: UnitType | 'ALL';
}>({
  keyword: '',
  status: 'ALL',
  unitType: 'ALL',
});
const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();

const tableData = ref<UnitRecord[]>([]);
const loading = ref(false);

const dialogVisible = ref(false);
const saving = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const editingId = ref<number | null>(null);

const createForm = reactive({
  code: '',
  name: '',
  type: 'STANDARD' as UnitType,
  status: true,
});

const createRules = computed(() => {
  const rules: Record<string, Array<{ required: boolean; message: string; trigger: string }>> = {
    name: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
    type: [{ required: true, message: '请选择单位类型', trigger: 'change' }],
  };
  if (dialogMode.value === 'edit') {
    rules.code = [{ required: true, message: '请输入单位编码', trigger: 'blur' }];
  }
  return rules;
});

const createFormRef = ref();

const filteredData = computed(() => {
  return tableData.value;
});
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredData.value.slice(start, start + pageSize.value);
});

const statusLabel = (status: UnitStatus) => (status === 'ENABLED' ? '启用' : '停用');
const typeLabel = (type: UnitType) => (type === 'STANDARD' ? '标准单位' : '辅助单位');

const openCreateDialog = () => {
  dialogMode.value = 'create';
  editingId.value = null;
  createForm.code = '';
  createForm.name = '';
  createForm.type = 'STANDARD';
  createForm.status = true;
  dialogVisible.value = true;
};
const openEditDialog = (row: UnitRecord) => {
  dialogMode.value = 'edit';
  editingId.value = row.id;
  createForm.code = row.code;
  createForm.name = row.name;
  createForm.type = row.type;
  createForm.status = row.status === 'ENABLED';
  dialogVisible.value = true;
};
const loadUnits = async () => {
  loading.value = true;
  try {
    const data = await fetchUnitsApi({
      keyword: query.keyword.trim() || undefined,
      status: query.status,
      unitType: query.unitType,
    }, sessionStore.currentOrgId || undefined);
    tableData.value = data.map((item: UnitItem) => ({
      id: item.id,
      code: item.code,
      name: item.name,
      type: item.type,
      status: item.status,
      createdAt: item.createdAt,
    }));
  } finally {
    loading.value = false;
  }
};
const handleSearch = () => {
  currentPage.value = 1;
  void loadUnits();
};
const handleReset = () => {
  query.keyword = '';
  query.status = 'ALL';
  query.unitType = 'ALL';
  currentPage.value = 1;
  void loadUnits();
};
const handlePageChange = (page: number) => {
  currentPage.value = page;
};
const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const handleDelete = async (row: UnitRecord) => {
  await ElMessageBox.confirm(`确定删除单位“${row.name}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  });
  await deleteUnitApi(row.id, sessionStore.currentOrgId || undefined);
  ElMessage.success('删除成功');
  await loadUnits();
};

const submitCreate = async () => {
  await createFormRef.value?.validate();
  saving.value = true;
  const payload = {
    code: dialogMode.value === 'edit' ? createForm.code.trim() : undefined,
    name: createForm.name.trim(),
    type: createForm.type,
    status: createForm.status ? 'ENABLED' : 'DISABLED',
  } as const;
  try {
    if (dialogMode.value === 'create') {
      await createUnitApi(payload, sessionStore.currentOrgId || undefined);
      ElMessage.success('新增成功');
    } else if (editingId.value != null) {
      await updateUnitApi(editingId.value, payload, sessionStore.currentOrgId || undefined);
      ElMessage.success('编辑成功');
    }
    dialogVisible.value = false;
    await loadUnits();
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  void loadUnits();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <el-form :model="query" inline class="filter-bar compact-filter-bar">
        <el-form-item label="单位信息">
          <el-input
            v-model="query.keyword"
            placeholder="根据编码或名称检索"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 120px">
            <el-option label="全部" value="ALL" />
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位类型">
          <el-select v-model="query.unitType" style="width: 130px">
            <el-option label="标准单位" value="STANDARD" />
            <el-option label="辅助单位" value="AUXILIARY" />
            <el-option label="全部" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" @click="openCreateDialog">新增</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedData"
        border
        stripe
        class="erp-table"
      >
        <el-table-column label="序号" width="60" align="center">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="code" label="单位编码" min-width="110" />
        <el-table-column prop="name" label="单位名称" min-width="140" />
        <el-table-column label="单位类型" min-width="100">
          <template #default="{ row }">
            {{ typeLabel(row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="0"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredData.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>
  </div>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogMode === 'create' ? '新增单位' : '编辑单位'"
    width="520px"
    class="standard-form-dialog"
    destroy-on-close
  >
    <el-form
      ref="createFormRef"
      :model="createForm"
      :rules="createRules"
      label-width="90px"
      class="standard-dialog-form"
    >
      <el-form-item v-if="dialogMode === 'edit'" label="单位编码" prop="code">
        <el-input v-model="createForm.code" clearable />
      </el-form-item>
      <el-form-item label="单位名称" prop="name">
        <el-input v-model="createForm.name" clearable />
      </el-form-item>
      <el-form-item label="单位类型" prop="type">
        <el-radio-group v-model="createForm.type">
          <el-radio value="STANDARD">标准单位</el-radio>
          <el-radio value="AUXILIARY">辅助单位</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch
          v-model="createForm.status"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitCreate">确定</el-button>
    </template>
  </el-dialog>
</template>
