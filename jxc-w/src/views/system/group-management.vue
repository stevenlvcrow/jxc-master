<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection, { type ToolbarButton } from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import {
  bindGroupAdminApi,
  createAdminGroupApi,
  deleteAdminGroupApi,
  fetchGroupAdminCandidatesApi,
  fetchAdminGroupsApi,
  updateAdminGroupApi,
  updateAdminGroupStatusApi,
  type GroupAdminCandidateItem,
  type GroupAdminItem,
} from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';

const loading = ref(false);
const submitting = ref(false);
const bindSubmitting = ref(false);
const bindCandidatesLoading = ref(false);
const createDialogVisible = ref(false);
const bindDialogVisible = ref(false);
const editingGroupId = ref<number | null>(null);
const groups = ref<GroupAdminItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const bindingGroup = ref<GroupAdminItem | null>(null);
const bindCandidates = ref<GroupAdminCandidateItem[]>([]);
const queryForm = reactive({
  keyword: '',
  status: '',
});
const sessionStore = useSessionStore();
const canManageAllGroups = computed(() => sessionStore.platformAdminMode);
const toolbarButtons = computed<ToolbarButton[]>(() => (
  canManageAllGroups.value ? [{ key: 'create', label: '新增集团', type: 'primary' }] : []
));

const form = reactive({
  groupCode: '',
  groupName: '',
  status: 'ENABLED' as 'ENABLED' | 'DISABLED',
  remark: '',
});

const bindForm = reactive({
  userId: undefined as number | undefined,
  phone: '',
  realName: '',
});

const resetForm = () => {
  form.groupCode = '';
  form.groupName = '';
  form.status = 'ENABLED';
  form.remark = '';
  editingGroupId.value = null;
};

const resetBindForm = () => {
  bindForm.userId = undefined;
  bindForm.phone = '';
  bindForm.realName = '';
  bindCandidates.value = [];
  bindingGroup.value = null;
};

const filteredGroups = computed(() => {
  const keyword = queryForm.keyword.trim().toLowerCase();
  const status = queryForm.status;
  return groups.value.filter((item) => {
    const keywordMatched = keyword
      ? `${item.groupCode}${item.groupName}${item.remark ?? ''}`.toLowerCase().includes(keyword)
      : true;
    const statusMatched = status ? item.status === status : true;
    return keywordMatched && statusMatched;
  });
});
const pagedGroups = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredGroups.value.slice(start, start + pageSize.value);
});

const resetQuery = () => {
  queryForm.keyword = '';
  queryForm.status = '';
};
const handlePageChange = (page: number) => {
  currentPage.value = page;
};
const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const loadGroups = async () => {
  loading.value = true;
  try {
    groups.value = await fetchAdminGroupsApi();
  } finally {
    loading.value = false;
  }
};

const openCreateDialog = () => {
  if (!canManageAllGroups.value) {
    return;
  }
  resetForm();
  createDialogVisible.value = true;
};

const openEditDialog = (group: GroupAdminItem) => {
  editingGroupId.value = group.id;
  form.groupCode = group.groupCode;
  form.groupName = group.groupName;
  form.status = group.status;
  form.remark = group.remark ?? '';
  createDialogVisible.value = true;
};

const handleSave = async () => {
  if (!form.groupName.trim()) {
    ElMessage.warning('请填写集团名称');
    return;
  }
  if (editingGroupId.value && !form.groupCode.trim()) {
    ElMessage.warning('请填写集团编码');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      groupCode: editingGroupId.value ? form.groupCode.trim() : undefined,
      groupName: form.groupName.trim(),
      status: form.status,
      remark: form.remark.trim() || undefined,
    };
    if (editingGroupId.value) {
      await updateAdminGroupApi(editingGroupId.value, payload);
      ElMessage.success('集团更新成功');
    } else {
      await createAdminGroupApi(payload);
      ElMessage.success('集团创建成功');
    }
    createDialogVisible.value = false;
    resetForm();
    await loadGroups();
  } finally {
    submitting.value = false;
  }
};

const handleStatusChange = async (row: GroupAdminItem, value: boolean | string | number) => {
  const status = value ? 'ENABLED' : 'DISABLED';
  await updateAdminGroupStatusApi(row.id, status);
  row.status = status;
  ElMessage.success('状态更新成功');
};

const openBindDialog = async (group: GroupAdminItem) => {
  if (!canManageAllGroups.value) {
    return;
  }
  bindingGroup.value = group;
  bindCandidatesLoading.value = true;
  try {
    bindCandidates.value = await fetchGroupAdminCandidatesApi(group.id);
  } finally {
    bindCandidatesLoading.value = false;
  }
  bindDialogVisible.value = true;
};

const handleBindCandidateChange = (userId?: number) => {
  if (!userId) {
    return;
  }
  const target = bindCandidates.value.find((item) => item.userId === userId);
  if (!target) {
    return;
  }
  bindForm.phone = target.phone;
  if (!bindForm.realName.trim()) {
    bindForm.realName = target.realName;
  }
};

const handleToolbarAction = (key: string) => {
  if (key === 'create') {
    openCreateDialog();
  }
};

const handleDeleteGroup = async (group: GroupAdminItem) => {
  if (!canManageAllGroups.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认删除集团“${group.groupName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteAdminGroupApi(group.id);
  ElMessage.success('集团删除成功');
  await loadGroups();
};

const handleBindAdmin = async () => {
  if (!bindingGroup.value) {
    return;
  }
  if (!bindForm.phone.trim()) {
    ElMessage.warning('请选择门店账号或填写手机号');
    return;
  }
  bindSubmitting.value = true;
  try {
    await bindGroupAdminApi(bindingGroup.value.id, {
      phone: bindForm.phone.trim(),
      realName: bindForm.realName.trim() || undefined,
    });
    ElMessage.success('集团管理员绑定成功');
    bindDialogVisible.value = false;
    resetBindForm();
    await loadGroups();
  } finally {
    bindSubmitting.value = false;
  }
};

onMounted(() => {
  loadGroups();
});

watch(
  () => [queryForm.keyword, queryForm.status],
  () => {
    currentPage.value = 1;
  },
);
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <CommonQuerySection :model="queryForm">
        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            placeholder="集团编码/名称/备注"
            clearable
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable style="width: 140px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </CommonQuerySection>

      <CommonToolbarSection v-if="toolbarButtons.length" :buttons="toolbarButtons" @action="handleToolbarAction" />

      <el-table :data="pagedGroups" border stripe class="erp-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="groupCode" label="集团编码" min-width="180" />
        <el-table-column prop="groupName" label="集团名称" min-width="200" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ENABLED'"
              @change="handleStatusChange(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">修改</el-button>
            <el-button v-if="canManageAllGroups" type="danger" link @click="handleDeleteGroup(row)">删除</el-button>
            <el-button v-if="canManageAllGroups" type="primary" link @click="openBindDialog(row)">绑定管理员</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="0"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredGroups.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>

    <el-dialog
      v-model="createDialogVisible"
      :title="editingGroupId ? '修改集团' : '新增集团'"
      width="520px"
      class="standard-form-dialog"
      @closed="resetForm"
    >
      <el-form label-width="100px" class="standard-dialog-form">
        <el-form-item v-if="editingGroupId" label="集团编码" required>
          <el-input v-model="form.groupCode" maxlength="64" />
        </el-form-item>
        <el-form-item label="集团名称" required>
          <el-input v-model="form.groupName" maxlength="128" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindDialogVisible"
      title="绑定集团管理员"
      width="460px"
      class="standard-form-dialog"
      @closed="resetBindForm"
    >
      <el-form label-width="100px" class="standard-dialog-form">
        <el-form-item label="集团">
          <span>{{ bindingGroup?.groupName }}（{{ bindingGroup?.groupCode }}）</span>
        </el-form-item>
        <el-form-item label="门店账号">
          <el-select
            v-model="bindForm.userId"
            filterable
            clearable
            placeholder="从门店绑定账号中选择"
            style="width: 100%"
            :loading="bindCandidatesLoading"
            @change="handleBindCandidateChange($event as number | undefined)"
          >
            <el-option
              v-for="item in bindCandidates"
              :key="item.userId"
              :label="`${item.realName}（${item.phone}）- ${item.storeName}`"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="bindForm.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="bindForm.realName" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="handleBindAdmin">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

