<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  bindWorkflowProcessStoresApi,
  createWorkflowProcessApi,
  deleteWorkflowProcessApi,
  deleteWorkflowConfigApi,
  fetchWorkflowProcessStoresApi,
  fetchWorkflowProcessesApi,
  fetchWorkflowPublishHistoryManageApi,
  publishWorkflowConfigApi,
  updateWorkflowProcessApi,
  type WorkflowProcessItem,
  type WorkflowProcessStoreOption,
  type WorkflowPublishHistoryManageItem,
} from '@/api/modules/workflow';
import { useSessionStore } from '@/stores/session';

const sessionStore = useSessionStore();
const router = useRouter();
const loading = ref(false);
const rows = ref<WorkflowProcessItem[]>([]);
const histories = ref<WorkflowPublishHistoryManageItem[]>([]);
const query = reactive({
  keyword: '',
});
const processDialogVisible = ref(false);
const processSubmitting = ref(false);
const editingBusinessId = ref<number | null>(null);
const workflowBusinessOptions = [
  { processCode: 'PURCHASE_INBOUND', businessName: '采购入库流程' },
];
const processForm = reactive({
  processCode: '',
  businessName: '',
});
const adoptingBusinessCode = ref('');
const publishingConfigId = ref<number | null>(null);
const deletingConfigId = ref<number | null>(null);
const adoptedTemplateMap = reactive<Record<string, string>>({});
const expandedRowKeys = ref<number[]>([]);
const bindStoreDialogVisible = ref(false);
const bindStoreSubmitting = ref(false);
const storeLoadWarned = ref(false);
const bindingStoreBusinessId = ref<number | null>(null);
const storeOptions = ref<WorkflowProcessStoreOption[]>([]);
const bindStoreForm = reactive({
  storeIds: [] as number[],
});
const bindStoreSearch = ref('');
const bindStoreSelectedRows = ref<WorkflowProcessStoreOption[]>([]);
const bindStoreTableRef = ref<InstanceType<typeof import('element-plus').ElTable> | null>(null);

const filteredStoreOptions = computed(() => {
  const kw = bindStoreSearch.value.trim().toLowerCase();
  if (!kw) return storeOptions.value;
  return storeOptions.value.filter((item) =>
    `${item.storeName}（${item.storeCode}）`.toLowerCase().includes(kw)
  );
});

const historyMap = computed(() => {
  const grouped = new Map<string, WorkflowPublishHistoryManageItem[]>();
  histories.value.forEach((item) => {
    const list = grouped.get(item.businessCode) ?? [];
    list.push(item);
    grouped.set(item.businessCode, list);
  });
  grouped.forEach((list) => {
    list.sort((a, b) => {
      const byVersion = (b.versionNo ?? 0) - (a.versionNo ?? 0);
      if (byVersion !== 0) {
        return byVersion;
      }
      return String(b.savedAt).localeCompare(String(a.savedAt));
    });
  });
  return grouped;
});

const filteredRows = computed(() => {
  const keyword = query.keyword.trim().toLowerCase();
  if (!keyword) {
    return rows.value;
  }
  return rows.value.filter((item) => {
    const baseMatched = `${item.processId}${item.businessName}${item.createdAt}`.toLowerCase().includes(keyword);
    if (baseMatched) {
      return true;
    }
    const childRows = historyMap.value.get(item.processId) ?? [];
    return childRows.some((child) =>
      `${child.workflowCode}${child.workflowName}${child.savedAt}${child.versionNo}`.toLowerCase().includes(keyword));
  });
});

const getHistoryRows = (businessCode: string) => historyMap.value.get(businessCode) ?? [];

const resetProcessForm = () => {
  processForm.processCode = '';
  processForm.businessName = '';
  editingBusinessId.value = null;
};

const syncBusinessNameFromCode = () => {
  const selected = workflowBusinessOptions.find((item) => item.processCode === processForm.processCode);
  processForm.businessName = selected?.businessName ?? '';
};

const openCreateBusiness = () => {
  resetProcessForm();
  processForm.processCode = workflowBusinessOptions[0]?.processCode ?? '';
  syncBusinessNameFromCode();
  processDialogVisible.value = true;
};

const openEditBusiness = (business: WorkflowProcessItem) => {
  editingBusinessId.value = business.id;
  processForm.processCode = business.processId;
  processForm.businessName = business.businessName;
  processDialogVisible.value = true;
};

const submitBusinessForm = async () => {
  if (!processForm.processCode.trim()) {
    ElMessage.warning('请选择业务ID');
    return;
  }
  const selectedBusiness = workflowBusinessOptions.find((item) => item.processCode === processForm.processCode);
  if (!selectedBusiness) {
    ElMessage.warning('请选择有效的业务名称');
    return;
  }
  if (!selectedBusiness.businessName.trim()) {
    ElMessage.warning('请填写业务名称');
    return;
  }
  processSubmitting.value = true;
  try {
    if (editingBusinessId.value) {
      const target = rows.value.find((item) => item.id === editingBusinessId.value);
      if (!target) {
        ElMessage.warning('业务不存在');
        return;
      }
      await updateWorkflowProcessApi(target.id, {
        orgId: sessionStore.currentOrgId,
        processCode: target.processId,
        businessName: selectedBusiness.businessName,
        templateId: target.templateId,
      });
      ElMessage.success('业务更新成功');
    } else {
      await createWorkflowProcessApi({
        orgId: sessionStore.currentOrgId,
        processCode: selectedBusiness.processCode,
        businessName: selectedBusiness.businessName,
      });
      ElMessage.success('业务新增成功');
    }
    processDialogVisible.value = false;
    resetProcessForm();
    await loadRows();
  } finally {
    processSubmitting.value = false;
  }
};

const removeBusiness = async (business: WorkflowProcessItem) => {
  try {
    await ElMessageBox.confirm(`确认删除业务“${business.businessName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteWorkflowProcessApi(business.id, sessionStore.currentOrgId);
  delete adoptedTemplateMap[business.processId];
  ElMessage.success('业务删除成功');
  await loadRows();
};

const loadRows = async () => {
  loading.value = true;
  try {
    const [processRows, historyRows] = await Promise.all([
      fetchWorkflowProcessesApi(sessionStore.currentOrgId),
      fetchWorkflowPublishHistoryManageApi(sessionStore.currentOrgId, 500),
    ]);
    rows.value = processRows;
    histories.value = historyRows;
    const publishedWorkflowMap = new Map<string, Set<string>>();
    historyRows.forEach((item) => {
      if (item.status !== 'PUBLISHED') {
        return;
      }
      const workflowSet = publishedWorkflowMap.get(item.businessCode) ?? new Set<string>();
      workflowSet.add(item.workflowCode);
      publishedWorkflowMap.set(item.businessCode, workflowSet);
    });
    processRows.forEach((item) => {
      const templateId = item.templateId ?? '';
      const publishedWorkflowSet = publishedWorkflowMap.get(item.processId) ?? new Set<string>();
      if (templateId && !publishedWorkflowSet.has(templateId)) {
        adoptedTemplateMap[item.processId] = '';
        item.templateId = undefined;
        return;
      }
      adoptedTemplateMap[item.processId] = templateId;
    });
    expandedRowKeys.value = processRows.length ? [processRows[0].id] : [];
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

const openWorkflowConfig = (payload?: { businessCode?: string; copyFromWorkflowCode?: string; viewWorkflowCode?: string }) => {
  router.push({
    path: '/group/workflow-config',
    query: payload?.businessCode
      ? {
          businessCode: payload.businessCode,
          copyFromWorkflowCode: payload.copyFromWorkflowCode,
          viewWorkflowCode: payload.viewWorkflowCode,
        }
      : undefined,
  });
};

const publishConfig = async (business: WorkflowProcessItem, row: WorkflowPublishHistoryManageItem) => {
  if (row.status === 'PUBLISHED') {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认发布流程版本“${row.workflowCode}”吗？`, '发布确认', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  publishingConfigId.value = row.id;
  try {
    await publishWorkflowConfigApi({
      orgId: sessionStore.currentOrgId,
      businessCode: business.processId,
      workflowCode: row.workflowCode,
    });
    ElMessage.success('流程发布成功');
    await loadRows();
  } finally {
    publishingConfigId.value = null;
  }
};

const openBindStores = async (business: WorkflowProcessItem) => {
  bindingStoreBusinessId.value = business.id;
  bindStoreSearch.value = '';
  if (!storeOptions.value.length) {
    await loadStoreOptions();
  }
  const existingIds = Array.isArray(business.storeIds) ? [...business.storeIds] : [];
  const selected = storeOptions.value.filter((s) => existingIds.includes(s.storeId));
  bindStoreSelectedRows.value = [...selected];
  bindStoreDialogVisible.value = true;
  await nextTick();
  if (bindStoreTableRef.value) {
    storeOptions.value.forEach((row) => {
      (bindStoreTableRef.value as any).toggleRowSelection(row, selected.some((s) => s.storeId === row.storeId));
    });
  }
};

const onBindStoreSelectionChange = (rows: WorkflowProcessStoreOption[]) => {
  bindStoreSelectedRows.value = rows;
};

const submitBindStores = async () => {
  if (!bindingStoreBusinessId.value) {
    return;
  }
  const storeIds = bindStoreSelectedRows.value.map((s) => s.storeId);
  bindStoreSubmitting.value = true;
  try {
    await bindWorkflowProcessStoresApi(bindingStoreBusinessId.value, storeIds, sessionStore.currentOrgId);
    ElMessage.success('门店绑定成功');
    bindStoreDialogVisible.value = false;
    await loadRows();
  } finally {
    bindStoreSubmitting.value = false;
  }
};

const adoptTemplate = async (business: WorkflowProcessItem, workflowCode: string) => {
  if (adoptedTemplateMap[business.processId] === workflowCode) {
    return;
  }
  const target = getHistoryRows(business.processId).find((item) => item.workflowCode === workflowCode);
  if (!target || target.status !== 'PUBLISHED') {
    ElMessage.warning('未发布版本不能使用，请先发布流程版本');
    return;
  }
  adoptingBusinessCode.value = business.processId;
  try {
    await updateWorkflowProcessApi(business.id, {
      orgId: sessionStore.currentOrgId,
      processCode: business.processId,
      businessName: business.businessName,
      templateId: workflowCode,
    });
    adoptedTemplateMap[business.processId] = workflowCode;
    business.templateId = workflowCode;
    ElMessage.success('使用状态已更新');
  } finally {
    adoptingBusinessCode.value = '';
  }
};

const removeConfig = async (business: WorkflowProcessItem, row: WorkflowPublishHistoryManageItem) => {
  try {
    await ElMessageBox.confirm(`确认删除流程模板“${row.workflowCode}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  deletingConfigId.value = row.id;
  try {
    await deleteWorkflowConfigApi(row.id, sessionStore.currentOrgId);
    if ((adoptedTemplateMap[business.processId] ?? '') === row.workflowCode) {
      await updateWorkflowProcessApi(business.id, {
        orgId: sessionStore.currentOrgId,
        processCode: business.processId,
        businessName: business.businessName,
        templateId: undefined,
      });
      adoptedTemplateMap[business.processId] = '';
      business.templateId = undefined;
    }
    ElMessage.success('流程模板删除成功');
    await loadRows();
  } finally {
    deletingConfigId.value = null;
  }
};

onMounted(() => {
  void loadRows();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索业务ID/业务名称/流程ID/流程版本" clearable style="width: 360px" />
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateBusiness">新增</el-button>
          <el-button @click="loadRows">刷新</el-button>
        </div>
      </div>

      <el-table
        :data="filteredRows"
        :expand-row-keys="expandedRowKeys"
        border
        stripe
        v-loading="loading"
        row-key="id"
        class="erp-table"
      >
        <el-table-column type="expand" width="48">
          <template #default="{ row: business }">
            <el-table :data="getHistoryRows(business.processId)" border size="small" class="expand-table">
              <el-table-column prop="workflowCode" label="流程版本" width="150" show-overflow-tooltip />
              <el-table-column prop="savedAt" label="流程发布时间" width="170" />
              <el-table-column label="是否发布" width="92" align="center">
                <template #default="{ row }">
                  <span class="publish-status" :class="row.status === 'PUBLISHED' ? 'is-published' : 'is-draft'">
                    {{ row.status === 'PUBLISHED' ? '已发布' : '未发布' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="是否使用" width="92" align="center">
                <template #default="{ row }">
                  <div class="use-radio-cell">
                    <el-radio
                      :value="row.workflowCode"
                      :model-value="adoptedTemplateMap[business.processId]"
                      :disabled="adoptingBusinessCode === business.processId || row.status !== 'PUBLISHED'"
                      @change="adoptTemplate(business, row.workflowCode)"
                    />
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openWorkflowConfig({ businessCode: business.processId, viewWorkflowCode: row.workflowCode })">
                    查看
                  </el-button>
                  <el-button text type="primary" @click="openWorkflowConfig({ businessCode: business.processId, copyFromWorkflowCode: row.workflowCode })">
                    复制
                  </el-button>
                  <el-button
                    v-if="row.status !== 'PUBLISHED'"
                    text
                    type="primary"
                    :loading="publishingConfigId === row.id"
                    @click="publishConfig(business, row)"
                  >
                    发布
                  </el-button>
                  <el-button text type="danger" :loading="deletingConfigId === row.id" @click="removeConfig(business, row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="expand-empty">暂无流程版本数据</div>
              </template>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="processId" label="业务ID" width="180" show-overflow-tooltip />
        <el-table-column prop="businessName" label="业务名称" width="180" show-overflow-tooltip />
        <el-table-column prop="storeNames" label="关联门店" width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.storeNames || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="业务添加时间" width="160" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openWorkflowConfig({ businessCode: row.processId })">新增流程</el-button>
            <el-button text @click="openEditBusiness(row)">编辑</el-button>
            <el-button text @click="openBindStores(row)">绑定门店</el-button>
            <el-button text type="danger" @click="removeBusiness(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        v-model="processDialogVisible"
        :title="editingBusinessId ? '编辑业务' : '新增业务'"
        width="460px"
        append-to-body
        destroy-on-close
      >
        <el-form label-width="86px">
          <el-form-item label="业务ID">
            <el-select
              v-model="processForm.processCode"
              placeholder="请选择业务ID"
              style="width: 100%"
              :disabled="Boolean(editingBusinessId)"
              @change="syncBusinessNameFromCode"
            >
              <el-option
                v-for="option in workflowBusinessOptions"
                :key="option.processCode"
                :label="`${option.businessName}（${option.processCode}）`"
                :value="option.processCode"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="processDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="processSubmitting" @click="submitBusinessForm">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="bindStoreDialogVisible"
        title="绑定门店"
        width="680px"
        append-to-body
        destroy-on-close
      >
        <div style="margin-bottom: 10px">
          <el-input v-model="bindStoreSearch" placeholder="搜索门店名称/编码" clearable style="width: 260px" />
        </div>
        <el-table
          ref="bindStoreTableRef"
          :data="filteredStoreOptions"
          border
          stripe
          size="small"
          max-height="400"
          @selection-change="onBindStoreSelectionChange"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column prop="storeCode" label="门店编码" width="140" show-overflow-tooltip />
          <el-table-column prop="storeName" label="门店名称" min-width="200" show-overflow-tooltip />
        </el-table>
        <template #footer>
          <el-button @click="bindStoreDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="bindStoreSubmitting" @click="submitBindStores">保存</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.expand-table {
  margin: 4px 8px 8px;
}

.expand-table :deep(.el-table__cell) {
  padding-left: 8px;
  padding-right: 8px;
}

.expand-table :deep(.el-table__cell.is-center) {
  text-align: center;
}

.use-radio-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.publish-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  font-size: 12px;
  line-height: 1;
}

.publish-status.is-published {
  color: #22c55e;
}

.publish-status.is-draft {
  color: #f59e0b;
}

.expand-empty {
  color: #909399;
  padding: 10px 0;
}
</style>
