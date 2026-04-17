<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import {
  createAdminRoleApi,
  fetchAdminRolesApi,
  updateAdminRoleApi,
  type RoleAdminItem,
  type RoleUpsertPayload,
} from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingRoleId = ref<number | null>(null);
const roles = ref<RoleAdminItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();

const queryForm = reactive({
  keyword: '',
  roleType: '',
});

const toolbarButtons = [{ key: 'create', label: '新增角色', type: 'primary' }] as const;

const form = reactive<RoleUpsertPayload>({
  roleCode: '',
  roleName: '',
  roleType: 'PLATFORM',
  dataScopeType: 'ALL',
  description: '',
  status: 'ENABLED',
  menuIds: [],
});

const roleTypeOptions = ['PLATFORM', 'GROUP', 'STORE'];
const dataScopeOptions = ['ALL', 'GROUP', 'STORE', 'CUSTOM'];
const isRoleEditable = (role: RoleAdminItem) => role.editable !== false;
const isBuiltinRole = (role: RoleAdminItem) => role.builtin === true;
const roleTypeSelectableOptions = computed(() => (
  sessionStore.platformAdminMode ? roleTypeOptions : roleTypeOptions.filter((item) => item !== 'PLATFORM')
));

const filteredRoles = computed(() => {
  const keyword = queryForm.keyword.trim().toLowerCase();
  const roleType = queryForm.roleType;

  return roles.value.filter((item) => {
    const matchKeyword =
      !keyword ||
      item.roleCode.toLowerCase().includes(keyword) ||
      item.roleName.toLowerCase().includes(keyword);
    const matchRoleType = !roleType || item.roleType === roleType;
    return matchKeyword && matchRoleType;
  });
});

const pagedRoles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredRoles.value.slice(start, end);
});

const resetForm = () => {
  form.roleCode = '';
  form.roleName = '';
  form.roleType = 'PLATFORM';
  form.dataScopeType = 'ALL';
  form.description = '';
  form.status = 'ENABLED';
  form.menuIds = [];
  editingRoleId.value = null;
};

const resetQuery = () => {
  queryForm.keyword = '';
  queryForm.roleType = '';
};

const handleToolbarAction = (key: string) => {
  if (key === 'create') {
    openCreate();
  }
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const loadRoles = async () => {
  loading.value = true;
  try {
    roles.value = await fetchAdminRolesApi();
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  resetForm();
  if (!sessionStore.platformAdminMode) {
    form.roleType = 'GROUP';
    form.dataScopeType = 'GROUP';
  }
  dialogVisible.value = true;
};

const openEdit = (row: RoleAdminItem) => {
  if (!isRoleEditable(row)) {
    ElMessage.warning('内置角色不可编辑');
    return;
  }
  editingRoleId.value = row.id;
  form.roleCode = row.roleCode;
  form.roleName = row.roleName;
  form.roleType = row.roleType;
  form.dataScopeType = row.dataScopeType;
  form.description = row.description ?? '';
  form.status = row.status;
  form.menuIds = row.menuIds ?? [];
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!form.roleName?.trim()) {
    ElMessage.warning('请填写角色名称');
    return;
  }
  if (editingRoleId.value && !form.roleCode?.trim()) {
    ElMessage.warning('请填写角色编码');
    return;
  }
  submitting.value = true;
  try {
    const roleCode = editingRoleId.value ? form.roleCode?.trim() : undefined;
    const payload: RoleUpsertPayload = {
      roleCode,
      roleName: form.roleName.trim(),
      roleType: form.roleType ?? 'PLATFORM',
      dataScopeType: form.dataScopeType ?? 'ALL',
      description: form.description?.trim(),
      status: form.status ?? 'ENABLED',
      menuIds: form.menuIds ?? [],
    };
    if (editingRoleId.value) {
      await updateAdminRoleApi(editingRoleId.value, payload);
      ElMessage.success('角色更新成功');
    } else {
      await createAdminRoleApi(payload);
      ElMessage.success('角色创建成功');
    }
    dialogVisible.value = false;
    resetForm();
    await loadRoles();
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadRoles();
});

watch(
  () => [queryForm.keyword, queryForm.roleType],
  () => {
    currentPage.value = 1;
  },
);
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <CommonQuerySection :model="queryForm">
        <el-form-item label="角色编码">
          <el-input
            v-model="queryForm.keyword"
            placeholder="请输入角色编码或名称"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="角色类型">
          <el-select
            v-model="queryForm.roleType"
            placeholder="请选择角色类型"
            clearable
            style="width: 160px"
          >
            <el-option v-for="item in roleTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </CommonQuerySection>

      <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

      <el-table :data="pagedRoles" border stripe class="erp-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" min-width="200" />
        <el-table-column prop="roleName" label="角色名称" min-width="180" />
        <el-table-column prop="roleType" label="角色类型" width="140" />
        <el-table-column label="属性" width="110">
          <template #default="{ row }">
            <el-tag v-if="isBuiltinRole(row)" type="warning" size="small">内置</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :content="isRoleEditable(row) ? '' : '内置角色不可编辑'"
              :disabled="isRoleEditable(row)"
              placement="top"
            >
              <el-button type="primary" link :disabled="!isRoleEditable(row)" @click="openEdit(row)">编辑</el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="0"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredRoles.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingRoleId ? '编辑角色' : '新增角色'"
      width="520px"
      class="standard-form-dialog"
      @closed="resetForm"
    >
      <el-form label-width="100px" class="standard-dialog-form">
        <el-form-item v-if="editingRoleId" label="角色编码" required>
          <el-input v-model="form.roleCode" :disabled="Boolean(editingRoleId)" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色类型">
          <el-select v-model="form.roleType" style="width: 100%">
            <el-option v-for="item in roleTypeSelectableOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScopeType" style="width: 100%">
            <el-option v-for="item in dataScopeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

