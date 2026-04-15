<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  bindWorkflowProcessStoresApi,
  bindWorkflowTemplateApi,
  deleteWorkflowProcessApi,
  fetchWorkflowProcessStoresApi,
  fetchWorkflowProcessesApi,
  updateWorkflowProcessApi,
  type WorkflowProcessItem,
  type WorkflowProcessStoreOption,
} from '@/api/modules/workflow';
import { useSessionStore } from '@/stores/session';

const sessionStore = useSessionStore();
const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const bindSubmitting = ref(false);
const bindStoreSubmitting = ref(false);
const storeLoadWarned = ref(false);
const dialogVisible = ref(false);
const bindDialogVisible = ref(false);
const bindStoreDialogVisible = ref(false);
const editingId = ref<number | null>(null);
const bindingId = ref<number | null>(null);
const bindingStoreId = ref<number | null>(null);
const rows = ref<WorkflowProcessItem[]>([]);
const storeOptions = ref<WorkflowProcessStoreOption[]>([]);

const query = reactive({
  keyword: '',
});

const form = reactive({
  processCode: '',
  businessName: '',
  templateId: '',
});

const bindForm = reactive({
  templateId: '',
});

const bindStoreForm = reactive({
  storeIds: [] as number[],
});

const filteredRows = computed(() => {
  const keyword = query.keyword.trim().toLowerCase();
  if (!keyword) {
    return rows.value;
  }
  return rows.value.filter((item) =>
    `${item.processId}${item.businessName}${item.templateId ?? ''}${item.storeNames ?? ''}`.toLowerCase().includes(keyword));
});

const loadRows = async () => {
  loading.value = true;
  try {
    rows.value = await fetchWorkflowProcessesApi(sessionStore.currentOrgId);
  } finally {
    loading.value = false;
  }
};

const loadStoreOptions = async () => {
  try {
    storeOptions.value = await fetchWorkflowProcessStoresApi(sessionStore.currentOrgId);
  } catch {
    storeOptions.value = [];
    if (!storeLoadWarned.value) {
      ElMessage.warning('门店列表加载失败，请确认登录状态或联系管理员初始化数据');
      storeLoadWarned.value = true;
    }
  }
};

const resetForm = () => {
  form.processCode = '';
  form.businessName = '';
  form.templateId = '';
  editingId.value = null;
};

const openWorkflowTemplateConfig = (row?: WorkflowProcessItem) => {
  const query = row?.processId ? { businessCode: row.processId } : undefined;
  router.push({ path: '/group/workflow-config', query });
};

const openEdit = (row: WorkflowProcessItem) => {
  editingId.value = row.id;
  form.processCode = row.processId;
  form.businessName = row.businessName;
  form.templateId = row.templateId ?? '';
  dialogVisible.value = true;
};

const openBind = (row: WorkflowProcessItem) => {
  bindingId.value = row.id;
  bindForm.templateId = row.templateId ?? '';
  bindDialogVisible.value = true;
};

const openBindStores = async (row: WorkflowProcessItem) => {
  bindingStoreId.value = row.id;
  bindStoreForm.storeIds = Array.isArray(row.storeIds) ? [...row.storeIds] : [];
  if (!storeOptions.value.length) {
    await loadStoreOptions();
  }
  bindStoreDialogVisible.value = true;
};

const submitForm = async () => {
  if (!form.businessName.trim()) {
    ElMessage.warning('请填写业务名称');
    return;
  }
  if (editingId.value && !form.processCode.trim()) {
    ElMessage.warning('流程ID不能为空');
    return;
  }
  if (!editingId.value) {
    ElMessage.warning('当前仅支持修改已有流程');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      orgId: sessionStore.currentOrgId,
      processCode: form.processCode.trim(),
      businessName: form.businessName.trim(),
      templateId: form.templateId.trim() || undefined,
    };
    await updateWorkflowProcessApi(editingId.value, payload);
    ElMessage.success('流程更新成功');
    dialogVisible.value = false;
    await loadRows();
  } finally {
    submitting.value = false;
  }
};

const submitBind = async () => {
  if (!bindingId.value) {
    return;
  }
  if (!bindForm.templateId.trim()) {
    ElMessage.warning('请填写模板ID');
    return;
  }
  bindSubmitting.value = true;
  try {
    await bindWorkflowTemplateApi(bindingId.value, bindForm.templateId.trim(), sessionStore.currentOrgId);
    ElMessage.success('模板绑定成功');
    bindDialogVisible.value = false;
    await loadRows();
  } finally {
    bindSubmitting.value = false;
  }
};

const submitBindStores = async () => {
  if (!bindingStoreId.value) {
    return;
  }
  bindStoreSubmitting.value = true;
  try {
    await bindWorkflowProcessStoresApi(bindingStoreId.value, bindStoreForm.storeIds, sessionStore.currentOrgId);
    ElMessage.success('门店绑定成功');
    bindStoreDialogVisible.value = false;
    await loadRows();
  } finally {
    bindStoreSubmitting.value = false;
  }
};

const removeRow = async (row: WorkflowProcessItem) => {
  try {
    await ElMessageBox.confirm(`确认删除流程“${row.processId}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteWorkflowProcessApi(row.id, sessionStore.currentOrgId);
  ElMessage.success('流程删除成功');
  await loadRows();
};

onMounted(() => {
  void loadRows();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索流程ID/业务名称/模板ID/关联门店" clearable style="width: 320px" />
        <el-button type="primary" @click="openWorkflowTemplateConfig">新增</el-button>
      </div>

      <el-table :data="filteredRows" border stripe v-loading="loading" class="erp-table">
        <el-table-column prop="processId" label="流程ID" min-width="180" />
        <el-table-column prop="businessName" label="业务名称" min-width="220" />
        <el-table-column prop="createdAt" label="添加时间" min-width="180" />
        <el-table-column prop="templateId" label="模板ID" min-width="180" />
        <el-table-column prop="storeNames" label="关联门店" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.storeNames || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openWorkflowTemplateConfig(row)">保存</el-button>
            <el-button text @click="openEdit(row)">修改</el-button>
            <el-button text @click="openBind(row)">绑定模板ID</el-button>
            <el-button text @click="openBindStores(row)">绑定门店</el-button>
            <el-button text type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '修改流程' : '新增流程'"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="96px">
        <el-form-item v-if="editingId" label="流程ID">
          <el-input v-model="form.processCode" placeholder="例如：PURCHASE_INBOUND" />
        </el-form-item>
        <el-form-item label="业务名称">
          <el-input v-model="form.businessName" placeholder="例如：采购入库审批" />
        </el-form-item>
        <el-form-item v-if="editingId" label="模板ID">
          <el-input v-model="form.templateId" placeholder="可选，后续可绑定" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindDialogVisible"
      title="绑定模板ID"
      width="460px"
      destroy-on-close
    >
      <el-form label-width="86px">
        <el-form-item label="模板ID">
          <el-input v-model="bindForm.templateId" placeholder="请输入模板ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitBind">绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindStoreDialogVisible"
      title="绑定门店"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="86px">
        <el-form-item label="门店">
          <el-select
            v-model="bindStoreForm.storeIds"
            multiple
            filterable
            clearable
            placeholder="请选择门店"
            style="width: 100%"
          >
            <el-option
              v-for="item in storeOptions"
              :key="item.storeId"
              :label="`${item.storeName}（${item.storeCode}）`"
              :value="item.storeId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindStoreDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindStoreSubmitting" @click="submitBindStores">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
