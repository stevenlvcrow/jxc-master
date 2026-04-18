<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import {
  batchDeleteAdminUsersApi,
  assignAdminUserRolesApi,
  createAdminUserApi,
  fetchAdminGroupsApi,
  fetchAdminRolesApi,
  fetchAdminUsersApi,
  fetchGroupStoresApi,
  deleteAdminUserApi,
  updateAdminUserApi,
  updateAdminUserStatusApi,
  type RoleAssignment,
  type GroupAdminItem,
  type GroupStoreItem,
  type RoleAdminItem,
  type UserAdminItem,
} from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';

type EditableAssignment = {
  uid: string;
  roleId?: number;
  scopeType: string;
  scopeId: number | null;
};

const roleAssignmentKey = (assignment: { roleId?: number; scopeType: string; scopeId: number | null }) =>
  assignment.scopeType === 'STORE'
    ? `STORE-${assignment.scopeId ?? 'null'}`
    : `${assignment.roleId ?? 'null'}-${assignment.scopeType}-${assignment.scopeId ?? 'null'}`;

const dedupeAssignments = <T extends { roleId?: number; scopeType: string; scopeId: number | null }>(rows: T[]) => {
  const unique = new Map<string, T>();
  rows.forEach((row) => {
    unique.set(roleAssignmentKey(row), row);
  });
  return [...unique.values()];
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
const selectedUserIds = ref<number[]>([]);
const userTableRef = ref<any>(null);
const sessionStore = useSessionStore();

const createDialogVisible = ref(false);
const assignDialogVisible = ref(false);
const copyDialogVisible = ref(false);
const bulkCopyDialogVisible = ref(false);
const submitting = ref(false);
const assigning = ref(false);
const copying = ref(false);
const bulkCopying = ref(false);
const selectedUser = ref<UserAdminItem | null>(null);
const editingUser = ref<UserAdminItem | null>(null);
const copiedUser = ref<UserAdminItem | null>(null);
const editableAssignments = ref<EditableAssignment[]>([]);
const copySourceStoreId = ref<number | null>(null);
const copyTargetStoreIds = ref<number[]>([]);
const bulkCopySourceStoreId = ref<number | null>(null);
const bulkCopyTargetStoreIds = ref<number[]>([]);

const createForm = reactive({
  realName: '',
  phone: '',
  status: 'ENABLED',
});

const queryForm = reactive({
  realName: '',
  phone: '',
  roleId: undefined as number | undefined,
  storeId: undefined as number | undefined,
});

const roleTypeScopeMap: Record<string, string> = {
  PLATFORM: 'PLATFORM',
  GROUP: 'GROUP',
  STORE: 'STORE',
};
const currentOrgId = computed(() => sessionStore.currentOrgId || undefined);

const roleOptions = computed(() => roles.value.map((item) => ({
  label: `${item.roleName}（${item.roleCode}）`,
  value: item.id,
})));
const userDialogTitle = computed(() => (editingUser.value ? '编辑用户' : '新增用户'));
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
const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '-';
  }
  const match = value.trim().match(
    /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$/,
  );
  if (!match) {
    return value;
  }
  const [, year, month, day, hour, minute, second] = match;
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
};
const buildCopySourceStoreOptions = (usersToScan: UserAdminItem[]) => {
  const seen = new Map<number, RoleAssignment>();
  usersToScan.forEach((user) => {
    user.roles
      .filter((role) => role.scopeType === 'STORE' && role.scopeId != null && role.builtin)
      .forEach((role) => {
        if (!seen.has(role.scopeId as number)) {
          seen.set(role.scopeId as number, role);
        }
      });
  });
  return [...seen.values()].map((role) => ({
    label: `${role.scopeName ?? '门店'} / ${role.roleName}`,
    value: role.scopeId as number,
  }));
};
const copySourceStoreOptions = computed(() => buildCopySourceStoreOptions(copiedUser.value ? [copiedUser.value] : []));
const bulkCopySourceUsers = computed(() => users.value.filter((item) => selectedUserIds.value.includes(item.id)));
const bulkCopySourceStoreOptions = computed(() => buildCopySourceStoreOptions(bulkCopySourceUsers.value));
const copyTargetStoreOptions = computed(() =>
  storeScopeOptions.value.filter((item) => item.value !== copySourceStoreId.value),
);
const bulkCopyTargetStoreOptions = computed(() =>
  storeScopeOptions.value.filter((item) => item.value !== bulkCopySourceStoreId.value),
);

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
  const storeId = queryForm.storeId;

  filteredUsers.value = users.value.filter((item) => {
    const matchRealName = !realName || item.realName.toLowerCase().includes(realName);
    const matchPhone = !phone || item.phone.includes(phone);
    const matchRole = !roleId || item.roles.some((role) => role.roleId === roleId);
    const matchStore = !storeId || item.roles.some((role) => role.scopeType === 'STORE' && role.scopeId === storeId);
    return matchRealName && matchPhone && matchRole && matchStore;
  });
  currentPage.value = 1;
};

const resetQuery = () => {
  queryForm.realName = '';
  queryForm.phone = '';
  queryForm.roleId = undefined;
  queryForm.storeId = undefined;
  applyQuery();
};

const resetCreateForm = () => {
  createForm.realName = '';
  createForm.phone = '';
  createForm.status = 'ENABLED';
  editingUser.value = null;
};

const openCreateDialog = () => {
  editingUser.value = null;
  resetCreateForm();
  createDialogVisible.value = true;
};

const openEditDialog = (row: UserAdminItem) => {
  editingUser.value = row;
  createForm.realName = row.realName;
  createForm.phone = row.phone;
  createForm.status = row.status;
  createDialogVisible.value = true;
};

const loadData = async () => {
  loading.value = true;
  try {
    const [userList, roleList] = await Promise.all([fetchAdminUsersApi(), fetchAdminRolesApi(currentOrgId.value)]);
    users.value = userList.map((item) => ({
      ...item,
      roles: dedupeAssignments(item.roles),
    }));
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

const ensureManagedScopesLoaded = async () => {
  if (groups.value.length || stores.value.length) {
    return;
  }
  await loadManagedScopes();
};

const submitUserForm = async () => {
  if (!createForm.realName.trim() || !createForm.phone.trim()) {
    ElMessage.warning('请填写姓名和手机号');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      realName: createForm.realName.trim(),
      phone: createForm.phone.trim(),
      status: createForm.status,
    };
    if (editingUser.value) {
      await updateAdminUserApi(editingUser.value.id, payload);
      ElMessage.success('用户更新成功');
    } else {
      await createAdminUserApi(payload);
      ElMessage.success('新增用户成功');
    }
    createDialogVisible.value = false;
    resetCreateForm();
    await loadData();
  } finally {
    submitting.value = false;
  }
};

const handleDeleteUser = async (row: UserAdminItem) => {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.realName}”吗？`, '删除确认', { type: 'warning' });
    await deleteAdminUserApi(row.id);
    ElMessage.success('删除成功');
    selectedUserIds.value = selectedUserIds.value.filter((id) => id !== row.id);
    userTableRef.value?.clearSelection?.();
    await loadData();
  } catch {
    // Cancelled or failed; global handler already shows API errors.
  }
};

const handleBatchDeleteUsers = async () => {
  if (!selectedUserIds.value.length) {
    ElMessage.warning('请先选择要删除的用户');
    return;
  }
  try {
    await ElMessageBox.confirm(`确认删除已选 ${selectedUserIds.value.length} 位用户吗？`, '批量删除', { type: 'warning' });
    await batchDeleteAdminUsersApi(selectedUserIds.value);
    ElMessage.success('批量删除成功');
    selectedUserIds.value = [];
    userTableRef.value?.clearSelection?.();
    await loadData();
  } catch {
    // Cancelled or failed; global handler already shows API errors.
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

const handleSelectionChange = (rows: UserAdminItem[]) => {
  selectedUserIds.value = rows.map((item) => item.id);
};

const updateAssignmentScopeByRole = (assignment: EditableAssignment) => {
  const role = getRoleById(assignment.roleId);
  const scopeType = roleTypeScopeMap[role?.roleType ?? 'PLATFORM'] ?? 'PLATFORM';
  assignment.scopeType = scopeType;
  assignment.scopeId = null;
};

const openAssignDialog = async (row: UserAdminItem) => {
  selectedUser.value = row;
  await ensureManagedScopesLoaded();
  const rows = dedupeAssignments(row.roles).map<EditableAssignment>((role) => ({
    uid: nextAssignmentUid(),
    roleId: role.roleId,
    scopeType: role.scopeType,
    scopeId: role.scopeId,
  }));
  editableAssignments.value = rows.length ? rows : [buildEmptyAssignment()];
  assignDialogVisible.value = true;
};

const openCopyDialog = async (row: UserAdminItem) => {
  const storeRoles = dedupeAssignments(row.roles).filter(
    (role) => role.scopeType === 'STORE' && role.scopeId != null && role.builtin,
  );
  if (!storeRoles.length) {
    ElMessage.warning('该用户没有可复制的系统内置门店角色');
    return;
  }
  copiedUser.value = row;
  await ensureManagedScopesLoaded();
  copySourceStoreId.value = storeRoles[0].scopeId;
  copyTargetStoreIds.value = [];
  copyDialogVisible.value = true;
};

const openBulkCopyDialog = async () => {
  if (!selectedUserIds.value.length) {
    ElMessage.warning('请先选择要复制的用户');
    return;
  }
  await ensureManagedScopesLoaded();
  if (!bulkCopySourceUsers.value.length) {
    ElMessage.warning('所选用户不存在');
    return;
  }
  if (!bulkCopySourceStoreOptions.value.length) {
    ElMessage.warning('所选用户没有可批量复制的门店角色');
    return;
  }
  bulkCopySourceStoreId.value = bulkCopySourceStoreOptions.value[0].value;
  bulkCopyTargetStoreIds.value = [];
  bulkCopyDialogVisible.value = true;
};

const getStoreRoleByScopeId = (user: UserAdminItem | null, scopeId: number | null) => {
  if (!user || scopeId == null) {
    return undefined;
  }
  return dedupeAssignments(user.roles).find(
    (role) => role.scopeType === 'STORE' && role.scopeId === scopeId && role.builtin,
  );
};

const handleCopySourceChange = (value: number | string) => {
  const sourceStoreId = Number(value);
  copySourceStoreId.value = sourceStoreId;
  copyTargetStoreIds.value = copyTargetStoreIds.value.filter((storeId) => storeId !== sourceStoreId);
};

const handleCopyTargetChange = (value: Array<number | string>) => {
  copyTargetStoreIds.value = value.map((item) => Number(item)).filter((item) => item !== copySourceStoreId.value);
};

const handleBulkCopySourceChange = (value: number | string) => {
  const sourceStoreId = Number(value);
  bulkCopySourceStoreId.value = sourceStoreId;
  bulkCopyTargetStoreIds.value = bulkCopyTargetStoreIds.value.filter((storeId) => storeId !== sourceStoreId);
};

const handleBulkCopyTargetChange = (value: Array<number | string>) => {
  bulkCopyTargetStoreIds.value = value.map((item) => Number(item)).filter((item) => item !== bulkCopySourceStoreId.value);
};

const handleCopyToStores = async () => {
  if (!copiedUser.value || copySourceStoreId.value == null) {
    return;
  }
  copying.value = true;
  try {
    const success = await copyStoreRolesToTargets([copiedUser.value], copySourceStoreId.value, copyTargetStoreIds.value);
    if (!success) {
      return;
    }
    ElMessage.success('复制成功');
    copyDialogVisible.value = false;
    await loadData();
  } finally {
    copying.value = false;
  }
};

const copyStoreRolesToTargets = async (targetUsers: UserAdminItem[],
                                       sourceStoreId: number,
                                       targetStoreIds: Array<number | string>) => {
  const normalizedTargetStoreIds = Array.from(new Set(targetStoreIds.map((item) => Number(item))))
    .filter((storeId) => storeId !== sourceStoreId);
  if (!normalizedTargetStoreIds.length) {
    ElMessage.warning('请选择至少一个目标门店');
    return false;
  }

  const missingUsers = targetUsers
    .filter((user) => !getStoreRoleByScopeId(user, sourceStoreId))
    .map((user) => user.realName || user.phone || String(user.id));
  if (missingUsers.length) {
    ElMessage.warning(`以下用户没有所选源门店的系统内置角色：${missingUsers.join('、')}`);
    return false;
  }

  for (const user of targetUsers) {
    const sourceRole = getStoreRoleByScopeId(user, sourceStoreId);
    if (!sourceRole) {
      continue;
    }
    const baseAssignments = dedupeAssignments(user.roles)
      .filter((item) => !(item.scopeType === 'STORE' && normalizedTargetStoreIds.includes(item.scopeId ?? -1)))
      .map((item) => ({
        roleId: item.roleId as number,
        scopeType: item.scopeType,
        scopeId: item.scopeType === 'PLATFORM' ? null : item.scopeId,
      }));
    const copiedAssignments = normalizedTargetStoreIds.map((storeId) => ({
      roleId: sourceRole.roleId as number,
      scopeType: 'STORE',
      scopeId: storeId,
    }));
    const finalAssignments = dedupeAssignments([...baseAssignments, ...copiedAssignments]);
    await assignAdminUserRolesApi(user.id, finalAssignments);
  }

  return true;
};

const handleBulkCopyToStores = async () => {
  if (!bulkCopySourceUsers.value.length || bulkCopySourceStoreId.value == null) {
    return;
  }

  bulkCopying.value = true;
  try {
    const success = await copyStoreRolesToTargets(
      bulkCopySourceUsers.value,
      bulkCopySourceStoreId.value,
      bulkCopyTargetStoreIds.value,
    );
    if (!success) {
      return;
    }
    ElMessage.success('批量复制成功');
    bulkCopyDialogVisible.value = false;
    selectedUserIds.value = [];
    await loadData();
  } finally {
    bulkCopying.value = false;
  }
};

const resetCopyDialog = () => {
  copiedUser.value = null;
  copySourceStoreId.value = null;
  copyTargetStoreIds.value = [];
};

const resetBulkCopyDialog = () => {
  bulkCopySourceStoreId.value = null;
  bulkCopyTargetStoreIds.value = [];
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
    new Map(assignments.map((item) => [roleAssignmentKey(item), item])).values(),
  );

  if (deduped.length !== assignments.length) {
    ElMessage.warning('同一用户同一门店只能分配一个角色');
    return;
  }

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
  loadManagedScopes();
  loadData();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    loadManagedScopes();
    loadData();
  },
);
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
        <el-form-item label="门店查询">
          <el-select
            v-model="queryForm.storeId"
            placeholder="请选择门店"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option
              v-for="item in storeScopeOptions"
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
        <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
        <el-button
          type="danger"
          :disabled="!selectedUserIds.length"
          @click="handleBatchDeleteUsers"
        >
          批量删除
        </el-button>
        <el-button
          type="primary"
          :disabled="!selectedUserIds.length"
          @click="openBulkCopyDialog"
        >
          批量复制到门店
        </el-button>
      </div>

      <el-table
        ref="userTableRef"
        :data="pagedUsers"
        border
        stripe
        class="erp-table"
        v-loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" reserve-selection />
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
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeleteUser(row)">删除</el-button>
            <el-button type="primary" link @click="openAssignDialog(row)">分配角色</el-button>
            <el-button type="primary" link @click="openCopyDialog(row)">复制到其它门店</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="selectedUserIds.length"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredUsers.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>

    <el-dialog
      v-model="createDialogVisible"
      :title="userDialogTitle"
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
        <el-button type="primary" :loading="submitting" @click="submitUserForm">
          {{ editingUser ? '更新' : '保存' }}
        </el-button>
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

    <el-dialog
      v-model="copyDialogVisible"
      title="复制到其它门店"
      width="760px"
      class="standard-form-dialog"
      @closed="resetCopyDialog"
    >
      <el-form label-width="90px" class="standard-dialog-form">
        <el-form-item label="用户">
          <span>{{ copiedUser?.realName }}（{{ copiedUser?.phone }}）</span>
        </el-form-item>
        <el-form-item label="源门店">
          <el-select
            :model-value="copySourceStoreId"
            placeholder="请选择源门店"
            filterable
            style="width: 100%"
            @change="handleCopySourceChange"
          >
            <el-option
              v-for="item in copySourceStoreOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标门店">
          <div style="width: 100%">
            <el-checkbox-group :model-value="copyTargetStoreIds" @change="handleCopyTargetChange">
              <el-space wrap>
                <el-checkbox
                  v-for="item in copyTargetStoreOptions"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </el-checkbox>
              </el-space>
            </el-checkbox-group>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="copying" @click="handleCopyToStores">复制</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bulkCopyDialogVisible"
      title="批量复制到门店"
      width="760px"
      class="standard-form-dialog"
      @closed="resetBulkCopyDialog"
    >
      <el-form label-width="90px" class="standard-dialog-form">
        <el-form-item label="已选用户">
          <span>{{ selectedUserIds.length }} 位</span>
        </el-form-item>
        <el-form-item label="源门店">
          <el-select
            :model-value="bulkCopySourceStoreId"
            placeholder="请选择源门店"
            filterable
            style="width: 100%"
            @change="handleBulkCopySourceChange"
          >
            <el-option
              v-for="item in bulkCopySourceStoreOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标门店">
          <div style="width: 100%">
            <el-checkbox-group
              :model-value="bulkCopyTargetStoreIds"
              @change="handleBulkCopyTargetChange"
            >
              <el-space wrap>
                <el-checkbox
                  v-for="item in bulkCopyTargetStoreOptions"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </el-checkbox>
              </el-space>
            </el-checkbox-group>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bulkCopyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bulkCopying" @click="handleBulkCopyToStores">复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>
