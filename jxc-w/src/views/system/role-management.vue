<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import {
  createAdminRoleApi,
  deleteAdminRoleApi,
  fetchAdminRolesApi,
  updateAdminRoleApi,
  type RoleAdminItem,
  type RoleUpsertPayload,
} from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingRoleId = ref<number | null>(null);
const roles = ref<RoleAdminItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();
const COMMON_STATUS_DICT = 'common.enabled_status';
const ROLE_TYPE_DICT = 'identity.role_type';
const DATA_SCOPE_DICT = 'identity.data_scope_type';
const { optionsOf } = useDictionaryOptions([COMMON_STATUS_DICT, ROLE_TYPE_DICT, DATA_SCOPE_DICT]);
const statusOptions = optionsOf(COMMON_STATUS_DICT);
const roleTypeDictionaryOptions = optionsOf(ROLE_TYPE_DICT);
const dataScopeDictionaryOptions = optionsOf(DATA_SCOPE_DICT);
const enabledStatus = computed(() => (
  statusOptions.value.find((item) => item.itemKey === 'ENABLED')?.itemCode ?? 'ENABLED'
));

const queryForm = reactive({
  keyword: '',
  roleType: '',
});

const toolbarButtons = [{ key: 'create', label: '新增角色', type: 'primary' }] as const;

const form = reactive<RoleUpsertPayload>({
  roleCode: '',
  roleName: '',
  builtin: false,
  roleType: 'PLATFORM',
  dataScopeType: 'ALL',
  description: '',
  status: 'ENABLED',
  menuIds: [],
});

const roleTypeOptions = ['PLATFORM', 'GROUP', 'STORE'];
const dataScopeValues = ['ALL', 'GROUP', 'STORE', 'SELF'];
const builtinOptions = [
  { label: '内置', value: true },
  { label: '非内置', value: false },
] as const;
const isRoleEditable = (role: RoleAdminItem) => role.editable !== false;
const isBuiltinRole = (role: RoleAdminItem) => role.builtin === true;
const roleAttributeLabel = (builtin?: boolean) => (builtin ? '内置' : '非内置');
const roleBuiltinSelectableOptions = computed(() => {
  if (sessionStore.platformAdminMode) {
    return builtinOptions;
  }
  return form.builtin ? [builtinOptions[0]] : [builtinOptions[1]];
});
const roleTypeSelectableOptions = computed(() => (
  sessionStore.platformAdminMode
    ? (form.builtin ? ['PLATFORM', 'GROUP', 'STORE'] : ['PLATFORM'])
    : ['GROUP', 'STORE']
));
const roleTypeSelectOptions = computed(() => (
  roleTypeDictionaryOptions.value.filter((item) => roleTypeOptions.includes(item.itemCode))
));
const roleTypeFormOptions = computed(() => (
  roleTypeSelectableOptions.value.map((value) => (
    roleTypeDictionaryOptions.value.find((item) => item.itemCode === value) ?? {
      id: 0,
      parentId: null,
      itemKey: value,
      itemCode: value,
      itemLabel: value,
      sortNo: 0,
      extraJson: null,
    }
  ))
));
const roleTypeLabel = (value: string) => (
  roleTypeDictionaryOptions.value.find((item) => item.itemCode === value)?.itemLabel ?? value
);
const currentOrgId = computed(() => sessionStore.currentOrgId || undefined);

const normalizeDataScopeForRole = (roleType: string, dataScopeType?: string) => {
  if (roleType === 'PLATFORM') {
    return 'ALL';
  }
  if (roleType === 'GROUP') {
    return dataScopeType === 'GROUP' ? 'GROUP' : 'SELF';
  }
  if (roleType === 'STORE') {
    return dataScopeType === 'STORE' ? 'STORE' : 'SELF';
  }
  return 'SELF';
};

const allowedDataScopesByRoleType = computed(() => {
  if (form.roleType === 'PLATFORM') {
    return ['ALL'];
  }
  if (form.roleType === 'GROUP') {
    return ['SELF', 'GROUP'];
  }
  if (form.roleType === 'STORE') {
    return ['SELF', 'STORE'];
  }
  return ['SELF'];
});
const dataScopeFormOptions = computed(() => (
  dataScopeDictionaryOptions.value.filter((item) => dataScopeValues.includes(item.itemCode)
    && allowedDataScopesByRoleType.value.includes(item.itemCode))
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
  form.builtin = false;
  form.roleType = sessionStore.platformAdminMode ? 'PLATFORM' : 'GROUP';
  form.dataScopeType = sessionStore.platformAdminMode ? 'ALL' : 'SELF';
  form.description = '';
  form.status = enabledStatus.value;
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
    roles.value = await fetchAdminRolesApi(currentOrgId.value);
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  resetForm();
  if (!sessionStore.platformAdminMode) {
    form.builtin = false;
    form.roleType = 'GROUP';
    form.dataScopeType = 'SELF';
  }
  dialogVisible.value = true;
};

const openEdit = (row: RoleAdminItem) => {
  editingRoleId.value = row.id;
  form.roleCode = row.roleCode;
  form.roleName = row.roleName;
  form.builtin = Boolean(row.builtin);
  form.roleType = row.roleType;
  form.dataScopeType = normalizeDataScopeForRole(row.roleType, row.dataScopeType);
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
      builtin: Boolean(form.builtin),
      roleType: form.roleType ?? 'PLATFORM',
      dataScopeType: normalizeDataScopeForRole(form.roleType ?? 'PLATFORM', form.dataScopeType),
      description: form.description?.trim(),
      status: form.status ?? enabledStatus.value,
      menuIds: form.menuIds ?? [],
    };
    if (editingRoleId.value) {
      await updateAdminRoleApi(editingRoleId.value, payload, currentOrgId.value);
      ElMessage.success('角色更新成功');
    } else {
      await createAdminRoleApi(payload, currentOrgId.value);
      ElMessage.success('角色创建成功');
    }
    dialogVisible.value = false;
    resetForm();
    await loadRoles();
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row: RoleAdminItem) => {
  if (!isRoleEditable(row)) {
    ElMessage.warning('当前角色不可删除');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除角色“${row.roleName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
    await deleteAdminRoleApi(row.id, currentOrgId.value);
    ElMessage.success('角色删除成功');
    await loadRoles();
  } catch {
    // 取消删除或删除失败时由全局错误处理器提示。
  }
};

onMounted(() => {
  loadRoles();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    loadRoles();
  },
);

watch(
  () => [queryForm.keyword, queryForm.roleType],
  () => {
    currentPage.value = 1;
  },
);

watch(
  () => form.builtin,
  (builtin) => {
    if (!sessionStore.platformAdminMode) {
      return;
    }
    if (builtin) {
      if (!roleTypeOptions.includes(form.roleType)) {
        form.roleType = 'PLATFORM';
      }
      form.dataScopeType = normalizeDataScopeForRole(form.roleType, form.dataScopeType);
      return;
    }
    form.roleType = 'PLATFORM';
    form.dataScopeType = 'ALL';
  },
  { immediate: true },
);

watch(
  () => form.roleType,
  (roleType) => {
    if (!roleType) {
      return;
    }
    form.dataScopeType = normalizeDataScopeForRole(roleType, form.dataScopeType);
  },
  { immediate: true },
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
            <el-option
              v-for="item in roleTypeSelectOptions"
              :key="item.itemCode"
              :label="item.itemLabel"
              :value="item.itemCode"
            />
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
        <el-table-column label="所属集团" min-width="180">
          <template #default="{ row }">
            <span>{{ row.tenantGroupName || (row.tenantGroupId > 0 ? `集团ID ${row.tenantGroupId}` : '平台') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" min-width="180" />
        <el-table-column label="角色类型" width="140">
          <template #default="{ row }">
            {{ roleTypeLabel(row.roleType) }}
          </template>
        </el-table-column>
        <el-table-column label="属性" width="110">
          <template #default="{ row }">
            <el-tag :type="isBuiltinRole(row) ? 'warning' : 'info'" size="small">
              {{ roleAttributeLabel(row.builtin) }}
            </el-tag>
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
            <el-button
              type="danger"
              link
              :disabled="!isRoleEditable(row)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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
        <el-form-item label="角色属性">
          <el-select v-model="form.builtin" style="width: 100%" :disabled="!sessionStore.platformAdminMode">
            <el-option
              v-for="item in roleBuiltinSelectableOptions"
              :key="String(item.value)"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色类型">
          <el-select v-model="form.roleType" style="width: 100%">
            <el-option
              v-for="item in roleTypeFormOptions"
              :key="item.itemCode"
              :label="item.itemLabel"
              :value="item.itemCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScopeType" style="width: 100%">
            <el-option
              v-for="item in dataScopeFormOptions"
              :key="item.itemCode"
              :label="item.itemLabel"
              :value="item.itemCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="item in statusOptions"
              :key="item.itemCode"
              :label="item.itemLabel"
              :value="item.itemCode"
            />
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

