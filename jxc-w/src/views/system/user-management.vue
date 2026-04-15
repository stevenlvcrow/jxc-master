<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import {
  assignAdminUserRolesApi,
  createAdminUserApi,
  fetchAdminGroupsApi,
  fetchAdminRolesApi,
  fetchAdminUsersApi,
  fetchGroupStoresApi,
  updateAdminUserStatusApi,
  type GroupAdminItem,
  type GroupStoreItem,
  type RoleAdminItem,
  type UserAdminItem,
} from '@/api/modules/system-admin';

type EditableAssignment = {
  uid: string;
  roleId?: number;
  scopeType: string;
  scopeId: number | null;
};

const loading = ref(false);
const scopeLoading = ref(false);
const users = ref<UserAdminItem[]>([]);
const roles = ref<RoleAdminItem[]>([]);
const groups = ref<GroupAdminItem[]>([]);
const stores = ref<GroupStoreItem[]>([]);
const filteredUsers = ref<UserAdminItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const createDialogVisible = ref(false);
const assignDialogVisible = ref(false);
const submitting = ref(false);
const assigning = ref(false);
const selectedUser = ref<UserAdminItem | null>(null);
const editableAssignments = ref<EditableAssignment[]>([]);

const createForm = reactive({
  realName: '',
  phone: '',
  status: 'ENABLED',
});

const queryForm = reactive({
  realName: '',
  phone: '',
  roleId: undefined as number | undefined,
});

const roleTypeScopeMap: Record<string, string> = {
  PLATFORM: 'PLATFORM',
  GROUP: 'GROUP',
  STORE: 'STORE',
};

const roleOptions = computed(() => roles.value.map((item) => ({
  label: `${item.roleName}（${item.roleCode}）`,
  value: item.id,
})));
const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredUsers.value.slice(start, start + pageSize.value);
});
const groupScopeOptions = computed(() => groups.value.map((group) => ({
  label: `${group.groupName}（${group.groupCode}）`,
  value: group.id,
})));
const groupNameMap = computed(() => new Map(groups.value.map((group) => [group.id, group.groupName])));
const storeScopeOptions = computed(() => stores.value.map((store) => ({
  label: `${groupNameMap.value.get(store.groupId) ?? '集团'} / ${store.storeName}（${store.storeCode}）`,
  value: store.id,
})));

let assignmentSeed = 0;

const nextAssignmentUid = () => {
  assignmentSeed += 1;
  return `assignment-${assignmentSeed}`;
};

const buildEmptyAssignment = (): EditableAssignment => ({
  uid: nextAssignmentUid(),
  roleId: undefined,
  scopeType: 'PLATFORM',
  scopeId: null,
});

const getRoleById = (roleId?: number) => roles.value.find((role) => role.id === roleId);

const applyQuery = () => {
  const realName = queryForm.realName.trim().toLowerCase();
  const phone = queryForm.phone.trim();
  const roleId = queryForm.roleId;

  filteredUsers.value = users.value.filter((item) => {
    const matchRealName = !realName || item.realName.toLowerCase().includes(realName);
    const matchPhone = !phone || item.phone.includes(phone);
    const matchRole = !roleId || item.roles.some((role) => role.roleId === roleId);
    return matchRealName && matchPhone && matchRole;
  });
  currentPage.value = 1;
};

const resetQuery = () => {
  queryForm.realName = '';
  queryForm.phone = '';
  queryForm.roleId = undefined;
  applyQuery();
};

const resetCreateForm = () => {
  createForm.realName = '';
  createForm.phone = '';
  createForm.status = 'ENABLED';
};

const loadData = async () => {
  loading.value = true;
  try {
    const [userList, roleList] = await Promise.all([fetchAdminUsersApi(), fetchAdminRolesApi()]);
    users.value = userList;
    roles.value = roleList;
    applyQuery();
  } finally {
    loading.value = false;
  }
};

const loadManagedScopes = async () => {
  scopeLoading.value = true;
  try {
    const groupList = await fetchAdminGroupsApi();
    groups.value = groupList;
    const storeBuckets = await Promise.all(groupList.map((group) => fetchGroupStoresApi(group.id)));
    stores.value = storeBuckets.flat();
  } finally {
    scopeLoading.value = false;
  }
};

const handleCreate = async () => {
  if (!createForm.realName.trim() || !createForm.phone.trim()) {
    ElMessage.warning('请填写姓名和手机号');
    return;
  }
  submitting.value = true;
  try {
    await createAdminUserApi({
      realName: createForm.realName.trim(),
      phone: createForm.phone.trim(),
      status: createForm.status,
    });
    ElMessage.success('新增用户成功');
    createDialogVisible.value = false;
    resetCreateForm();
    await loadData();
  } finally {
    submitting.value = false;
  }
};

const handleStatusChange = async (row: UserAdminItem, value: boolean | string | number) => {
  const status = value ? 'ENABLED' : 'DISABLED';
  try {
    await updateAdminUserStatusApi(row.id, status);
    row.status = status;
    ElMessage.success('状态更新成功');
  } catch {
    // Global error message handled in http interceptor.
  }
};

const updateAssignmentScopeByRole = (assignment: EditableAssignment) => {
  const role = getRoleById(assignment.roleId);
  const scopeType = roleTypeScopeMap[role?.roleType ?? 'PLATFORM'] ?? 'PLATFORM';
  assignment.scopeType = scopeType;
  assignment.scopeId = null;
};

const openAssignDialog = async (row: UserAdminItem) => {
  selectedUser.value = row;
  await loadManagedScopes();
  const rows = row.roles.map<EditableAssignment>((role) => ({
    uid: nextAssignmentUid(),
    roleId: role.roleId,
    scopeType: role.scopeType,
    scopeId: role.scopeId,
  }));
  editableAssignments.value = rows.length ? rows : [buildEmptyAssignment()];
  assignDialogVisible.value = true;
};

const addAssignmentRow = () => {
  editableAssignments.value.push(buildEmptyAssignment());
};

const removeAssignmentRow = (uid: string) => {
  editableAssignments.value = editableAssignments.value.filter((item) => item.uid !== uid);
  if (!editableAssignments.value.length) {
    editableAssignments.value = [buildEmptyAssignment()];
  }
};

const handleAssignmentRoleChange = (row: EditableAssignment, value: string | number) => {
  row.roleId = Number(value);
  updateAssignmentScopeByRole(row);
};

const handleAssignRoles = async () => {
  if (!selectedUser.value) {
    return;
  }
  const assignments = editableAssignments.value
    .filter((item) => item.roleId != null)
    .map((item) => ({
      roleId: Number(item.roleId),
      scopeType: item.scopeType,
      scopeId: item.scopeType === 'PLATFORM' ? null : item.scopeId,
    }));

  if (!assignments.length) {
    ElMessage.warning('请至少选择一个角色');
    return;
  }

  const missingScope = assignments.find((item) => item.scopeType !== 'PLATFORM' && item.scopeId == null);
  if (missingScope) {
    ElMessage.warning('集团/门店角色必须指定作用域');
    return;
  }

  const deduped = Array.from(
    new Map(
      assignments.map((item) => [`${item.roleId}-${item.scopeType}-${item.scopeId ?? 'null'}`, item]),
    ).values(),
  );

  assigning.value = true;
  try {
    await assignAdminUserRolesApi(selectedUser.value.id, deduped);
    ElMessage.success('角色分配成功');
    assignDialogVisible.value = false;
    await loadData();
  } finally {
    assigning.value = false;
  }
};
const handlePageChange = (page: number) => {
  currentPage.value = page;
};
const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <el-form :model="queryForm" :inline="true" class="filter-bar compact-filter-bar">
        <el-form-item label="姓名查询">
          <el-input
            v-model="queryForm.realName"
            placeholder="请输入姓名"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="手机号查询">
          <el-input
            v-model="queryForm.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="角色查询">
          <el-select
            v-model="queryForm.roleId"
            placeholder="请选择角色"
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" @click="createDialogVisible = true">新增用户</el-button>
      </div>

      <el-table :data="pagedUsers" border stripe class="erp-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户编码" min-width="160" />
        <el-table-column prop="realName" label="姓名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="拥有角色" min-width="280">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                v-for="role in row.roles"
                :key="`${role.roleId}-${role.scopeType}-${role.scopeId ?? 'null'}`"
                size="small"
              >
                {{ role.roleName }}{{ role.scopeName ? ` / ${role.scopeName}` : '' }}
              </el-tag>
              <span v-if="!row.roles.length">-</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ENABLED'"
              @change="handleStatusChange(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openAssignDialog(row)">分配角色</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="0"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredUsers.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>

    <el-dialog
      v-model="createDialogVisible"
      title="新增用户"
      width="420px"
      class="standard-form-dialog"
      @closed="resetCreateForm"
    >
      <el-form label-width="90px" class="standard-dialog-form">
        <el-form-item label="姓名" required>
          <el-input v-model="createForm.realName" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="createForm.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配角色" width="840px" class="standard-form-dialog">
      <el-form label-width="90px" class="standard-dialog-form" v-loading="scopeLoading">
        <el-form-item label="用户">
          <span>{{ selectedUser?.realName }}（{{ selectedUser?.phone }}）</span>
        </el-form-item>
        <el-form-item label="授权明细">
          <div style="width: 100%">
            <el-table :data="editableAssignments" border size="small">
              <el-table-column label="角色" min-width="280">
                <template #default="{ row }">
                  <el-select
                    :model-value="row.roleId"
                    placeholder="请选择角色"
                    filterable
                    style="width: 100%"
                    @change="handleAssignmentRoleChange(row, $event)"
                  >
                    <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="作用域" min-width="260">
                <template #default="{ row }">
                  <el-select
                    v-if="row.scopeType === 'GROUP'"
                    v-model="row.scopeId"
                    placeholder="请选择集团"
                    filterable
                    style="width: 100%"
                  >
                    <el-option
                      v-for="item in groupScopeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                  <el-select
                    v-else-if="row.scopeType === 'STORE'"
                    v-model="row.scopeId"
                    placeholder="请选择门店"
                    filterable
                    style="width: 100%"
                  >
                    <el-option
                      v-for="item in storeScopeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                  <el-input v-else model-value="平台" disabled />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center">
                <template #default="{ row }">
                  <el-button type="danger" link @click="removeAssignmentRow(row.uid)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div style="margin-top: 10px">
              <el-button plain type="primary" @click="addAssignmentRow">新增一行</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssignRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
