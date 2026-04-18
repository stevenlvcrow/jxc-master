<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection, { type ToolbarButton } from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import {
  createGroupStoreApi,
  deleteGroupStoreApi,
  fetchAdminGroupsApi,
  fetchGroupStoresApi,
  updateGroupStoreApi,
  type GroupAdminItem,
  type GroupStoreItem,
} from '@/api/modules/system-admin';
import { useSessionStore, type OrgNode } from '@/stores/session';

const sessionStore = useSessionStore();
const loading = ref(false);
const creating = ref(false);
const createDialogVisible = ref(false);
const dialogTitle = ref('新增门店');
const isEdit = ref(false);
const editingStoreId = ref<number | null>(null);
const groups = ref<GroupAdminItem[]>([]);
const stores = ref<GroupStoreItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedGroupId = ref<number>();
const query = reactive({
  keyword: '',
  status: '',
});

const createForm = reactive({
  storeName: '',
  status: 'ENABLED' as 'ENABLED' | 'DISABLED',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
});

const toolbarButtons: ToolbarButton[] = [
  { key: 'create', label: '新增门店', type: 'primary' },
];

const resolveParentGroup = (org: OrgNode | null) => {
  if (!org) {
    return null;
  }
  if (org.type === 'group') {
    return org;
  }
  return sessionStore.rootGroups.find((group) => group.children?.some((child) => child.id === org.id)) ?? null;
};

const filteredStores = computed(() => stores.value.filter((item) => {
  const keyword = query.keyword.trim().toLowerCase();
  const keywordMatched = keyword
    ? `${item.storeCode}${item.storeName}${item.contactName ?? ''}${item.contactPhone ?? ''}`.toLowerCase().includes(keyword)
    : true;
  const statusMatched = query.status ? item.status === query.status : true;
  return keywordMatched && statusMatched;
}));
const pagedStores = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredStores.value.slice(start, start + pageSize.value);
});

const resetCreateForm = () => {
  createForm.storeName = '';
  createForm.status = 'ENABLED';
  createForm.contactName = '';
  createForm.contactPhone = '';
  createForm.address = '';
  createForm.remark = '';
  isEdit.value = false;
  editingStoreId.value = null;
  dialogTitle.value = '新增门店';
};

const loadGroups = async () => {
  groups.value = await fetchAdminGroupsApi();
  if (!groups.value.length) {
    selectedGroupId.value = undefined;
    stores.value = [];
    return;
  }
  const currentGroup = resolveParentGroup(sessionStore.currentOrg);
  if (!selectedGroupId.value) {
    if (currentGroup?.id.startsWith('group-')) {
      const currentId = Number(currentGroup.id.slice('group-'.length));
      if (!Number.isNaN(currentId) && groups.value.some((item) => item.id === currentId)) {
        selectedGroupId.value = currentId;
      }
    }
    if (!selectedGroupId.value) {
      selectedGroupId.value = groups.value[0].id;
    }
  }
};

const loadStores = async () => {
  if (!selectedGroupId.value) {
    stores.value = [];
    return;
  }
  loading.value = true;
  try {
    stores.value = await fetchGroupStoresApi(selectedGroupId.value);
  } finally {
    loading.value = false;
  }
};

const refresh = async () => {
  loading.value = true;
  try {
    await loadGroups();
    await loadStores();
  } finally {
    loading.value = false;
  }
};

const handleToolbarAction = (key: string) => {
  if (key === 'create') {
    if (!selectedGroupId.value) {
      ElMessage.warning('请先选择集团');
      return;
    }
    resetCreateForm();
    createDialogVisible.value = true;
    return;
  }
};

const openEditDialog = (row: GroupStoreItem) => {
  isEdit.value = true;
  editingStoreId.value = row.id;
  dialogTitle.value = '编辑门店';
  createForm.storeName = row.storeName;
  createForm.status = row.status;
  createForm.contactName = row.contactName ?? '';
  createForm.contactPhone = row.contactPhone ?? '';
  createForm.address = row.address ?? '';
  createForm.remark = row.remark ?? '';
  createDialogVisible.value = true;
};

const handleDeleteStore = async (row: GroupStoreItem) => {
  if (!selectedGroupId.value) {
    ElMessage.warning('请先选择集团');
    return;
  }
  await ElMessageBox.confirm(`确认删除门店“${row.storeName}”吗？`, '删除确认', { type: 'warning' });
  await deleteGroupStoreApi(selectedGroupId.value, row.id);
  ElMessage.success('门店删除成功');
  await loadStores();
};
const handlePageChange = (page: number) => {
  currentPage.value = page;
};
const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const handleCreateStore = async () => {
  if (!selectedGroupId.value) {
    ElMessage.warning('请先选择集团');
    return;
  }
  if (!createForm.storeName.trim()) {
    ElMessage.warning('请填写门店名称');
    return;
  }
  creating.value = true;
  try {
    const payload = {
      storeCode: undefined,
      storeName: createForm.storeName.trim(),
      status: createForm.status,
      contactName: createForm.contactName.trim() || undefined,
      contactPhone: createForm.contactPhone.trim() || undefined,
      address: createForm.address.trim() || undefined,
      remark: createForm.remark.trim() || undefined,
    };
    if (isEdit.value && editingStoreId.value != null) {
      await updateGroupStoreApi(selectedGroupId.value, editingStoreId.value, payload);
      ElMessage.success('门店更新成功');
    } else {
      await createGroupStoreApi(selectedGroupId.value, {
        ...payload,
        storeCode: undefined,
      });
      ElMessage.success('门店创建成功');
    }
    createDialogVisible.value = false;
    resetCreateForm();
    await loadStores();
  } finally {
    creating.value = false;
  }
};

watch(selectedGroupId, () => {
  currentPage.value = 1;
  loadStores();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    selectedGroupId.value = undefined;
    refresh();
  },
);

watch(
  () => [query.keyword, query.status],
  () => {
    currentPage.value = 1;
  },
);

onMounted(() => {
  refresh();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <CommonQuerySection :model="query">
        <el-form-item label="集团">
          <el-select v-model="selectedGroupId" style="width: 260px" filterable>
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="`${group.groupName}（${group.groupCode}）`"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="门店编码/名称/联系方式" clearable style="width: 260px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 140px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </CommonQuerySection>

      <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

      <el-table :data="pagedStores" border stripe class="erp-table" v-loading="loading">
        <el-table-column prop="storeCode" label="门店编码" min-width="140" />
        <el-table-column prop="storeName" label="门店名称" min-width="180" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeleteStore(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="0"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredStores.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>

    <el-dialog
      v-model="createDialogVisible"
      :title="dialogTitle"
      width="560px"
      class="standard-form-dialog"
      @closed="resetCreateForm"
    >
      <el-form label-width="100px" class="standard-dialog-form">
        <el-form-item label="门店名称" required>
          <el-input v-model="createForm.storeName" maxlength="128" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="createForm.contactName" maxlength="64" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="createForm.contactPhone" maxlength="32" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="createForm.address" maxlength="255" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateStore">
          {{ isEdit ? '更新' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

