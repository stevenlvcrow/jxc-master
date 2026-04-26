<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import {
  approvePeriodOpeningApi,
  deletePeriodOpeningApi,
  fetchPeriodOpeningPageApi,
  generatePeriodOpeningApi,
  rejectPeriodOpeningApi,
  submitPeriodOpeningApi,
  type PeriodOpeningRow,
} from '@/api/modules/inventory';
import { fetchStoreWarehousesApi, fetchWarehousesApi, type WarehouseRow } from '@/api/modules/warehouse';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { useRequiredOrgScope } from '@/composables/useRequiredOrgScope';
import { useSessionStore } from '@/stores/session';
import { parseGroupId, parseStoreId } from '@/utils/org';

type WarehouseOption = {
  name: string;
  label: string;
};

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const { orgId, requireOrgId } = useRequiredOrgScope();
const INVENTORY_PERIOD_TYPE_DICT = 'inventory.period_type';
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const { optionsOf } = useDictionaryOptions([INVENTORY_PERIOD_TYPE_DICT, INVENTORY_DOCUMENT_STATUS_DICT]);
const periodTypeOptions = optionsOf(INVENTORY_PERIOD_TYPE_DICT);
const documentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);

const query = reactive({
  documentCode: '',
  warehouseName: '',
  periodType: '',
  startDate: '',
  endDate: '',
  status: '',
});

const generateForm = reactive({
  warehouseName: '',
  periodType: 'MONTH',
  periodStartDate: '',
  remark: '',
});

const loading = ref(false);
const actionLoading = ref(false);
const generateVisible = ref(false);
const rows = ref<PeriodOpeningRow[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedRows = ref<PeriodOpeningRow[]>([]);
const warehouseOptions = ref<WarehouseOption[]>([]);

const statusLabelMap = computed(() =>
  documentStatusOptions.value.reduce<Record<string, string>>((result, item) => {
    result[item.itemCode] = item.itemLabel;
    return result;
  }, {}),
);
const periodTypeLabelMap = computed(() =>
  periodTypeOptions.value.reduce<Record<string, string>>((result, item) => {
    result[item.itemCode] = item.itemLabel;
    return result;
  }, {}),
);
const selectedIds = computed(() => selectedRows.value.map((item) => item.id));
const draftStatus = computed(() =>
  documentStatusOptions.value.find((item) => item.itemKey === 'DRAFT')?.itemCode ?? '',
);
const submittedStatus = computed(() =>
  documentStatusOptions.value.find((item) => item.itemKey === 'SUBMITTED')?.itemCode ?? '',
);
const approvedStatus = computed(() =>
  documentStatusOptions.value.find((item) => item.itemKey === 'APPROVED')?.itemCode ?? '',
);

const mapWarehouseOption = (row: WarehouseRow): WarehouseOption => ({
  name: row.warehouseName,
  label: row.warehouseCode ? `${row.warehouseName} / ${row.warehouseCode}` : row.warehouseName,
});

const loadWarehouses = async () => {
  if (!orgId.value) {
    warehouseOptions.value = [];
    return;
  }
  const groupId = parseGroupId(orgId.value);
  const storeId = parseStoreId(orgId.value);
  const data = groupId
    ? await fetchWarehousesApi(groupId, { status: 'ENABLED' })
    : storeId
      ? await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' })
      : [];
  warehouseOptions.value = data.map(mapWarehouseOption).sort((left, right) => left.name.localeCompare(right.name, 'zh-Hans-CN'));
};

const loadRows = async () => {
  if (!orgId.value) {
    rows.value = [];
    total.value = 0;
    selectedRows.value = [];
    return;
  }
  loading.value = true;
  try {
    const result = await fetchPeriodOpeningPageApi({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      documentCode: query.documentCode || undefined,
      warehouseName: query.warehouseName || undefined,
      periodType: query.periodType || undefined,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined,
      status: query.status || undefined,
    }, orgId.value);
    rows.value = result.list;
    total.value = Number(result.total ?? 0);
    selectedRows.value = [];
  } finally {
    loading.value = false;
  }
};

const refreshAll = async () => {
  await Promise.all([loadRows(), loadWarehouses()]);
};

const resetQuery = async () => {
  query.documentCode = '';
  query.warehouseName = '';
  query.periodType = '';
  query.startDate = '';
  query.endDate = '';
  query.status = '';
  currentPage.value = 1;
  await loadRows();
};

const openGenerateDialog = () => {
  generateForm.warehouseName = query.warehouseName || '';
  generateForm.periodType = query.periodType || 'MONTH';
  generateForm.periodStartDate = '';
  generateForm.remark = '';
  generateVisible.value = true;
};

const submitGenerate = async () => {
  const currentOrgId = requireOrgId();
  if (!currentOrgId) {
    return;
  }
  if (!generateForm.warehouseName || !generateForm.periodType || !generateForm.periodStartDate) {
    ElMessage.warning('仓库、周期类型和周期开始日期不能为空');
    return;
  }
  actionLoading.value = true;
  try {
    const result = await generatePeriodOpeningApi({
      warehouseName: generateForm.warehouseName,
      periodType: generateForm.periodType,
      periodStartDate: generateForm.periodStartDate,
      remark: generateForm.remark,
    }, currentOrgId);
    ElMessage.success(`已生成期初库存 ${result.documentCode}`);
    generateVisible.value = false;
    await loadRows();
  } finally {
    actionLoading.value = false;
  }
};

const handleSubmit = async (row: PeriodOpeningRow) => {
  await submitPeriodOpeningApi(row.id, orgId.value);
  ElMessage.success('提交成功');
  await loadRows();
};

const handleApprove = async (row: PeriodOpeningRow) => {
  await approvePeriodOpeningApi(row.id, orgId.value);
  ElMessage.success('审核成功');
  await loadRows();
};

const handleReject = async (row: PeriodOpeningRow) => {
  const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回期初库存', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空',
  });
  await rejectPeriodOpeningApi(row.id, reason.value, orgId.value);
  ElMessage.success('已驳回');
  await loadRows();
};

const handleDelete = async (row: PeriodOpeningRow) => {
  if (!canDelete(row)) {
    ElMessage.warning('已确认期初不允许删除');
    return;
  }
  await ElMessageBox.confirm(`确认删除期初库存 ${row.documentCode} 吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  });
  await deletePeriodOpeningApi(row.id, orgId.value);
  ElMessage.success('删除成功');
  await loadRows();
};

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择期初库存');
    return;
  }
  const deletableRows = selectedRows.value.filter(canDelete);
  if (!deletableRows.length) {
    ElMessage.warning('所选期初库存均不可删除');
    return;
  }
  if (deletableRows.length !== selectedRows.value.length) {
    ElMessage.warning('已确认期初不允许删除，请重新选择');
    return;
  }
  await ElMessageBox.confirm(`确认删除选中的 ${deletableRows.length} 张期初库存吗？`, '批量删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  });
  for (const row of deletableRows) {
    await deletePeriodOpeningApi(row.id, orgId.value);
  }
  ElMessage.success('批量删除完成');
  await loadRows();
};

watch(
  () => sessionStore.currentOrgId,
  () => {
    query.documentCode = '';
    query.warehouseName = '';
    query.periodType = '';
    query.startDate = '';
    query.endDate = '';
    query.status = '';
    currentPage.value = 1;
    selectedRows.value = [];
    rows.value = [];
    void refreshAll();
  },
);

onMounted(() => {
  const documentCode = String(route.query.documentCode ?? '').trim();
  if (documentCode) {
    query.documentCode = documentCode;
  }
  void refreshAll();
});

const handleSelectionChange = (selection: PeriodOpeningRow[]) => {
  selectedRows.value = selection;
};

const canEdit = (row: PeriodOpeningRow) => row.sourceType === 'MANUAL'
  && approvedStatus.value
  && row.status !== approvedStatus.value;

const canSubmit = (row: PeriodOpeningRow) => draftStatus.value && row.status === draftStatus.value;

const canApprove = (row: PeriodOpeningRow) => submittedStatus.value && row.status === submittedStatus.value;

const canDelete = (row: PeriodOpeningRow) => approvedStatus.value && row.status !== approvedStatus.value;
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="期初单号">
        <el-input v-model="query.documentCode" clearable placeholder="请输入期初单号" style="width: 180px" />
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="query.warehouseName" clearable filterable style="width: 180px">
          <el-option v-for="item in warehouseOptions" :key="item.name" :label="item.label" :value="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="周期类型">
        <el-select v-model="query.periodType" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option v-for="item in periodTypeOptions" :key="item.itemCode" :label="item.itemLabel" :value="item.itemCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 130px">
          <el-option label="全部" value="" />
          <el-option v-for="item in documentStatusOptions" :key="item.itemCode" :label="item.itemLabel" :value="item.itemCode" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="() => { currentPage = 1; void loadRows(); }">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="resetQuery">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </CommonQuerySection>

    <div class="table-toolbar">
      <el-button type="primary" @click="router.push({ name: 'PeriodOpeningCreate' })">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button @click="openGenerateDialog">从结存生成</el-button>
      <el-button @click="handleBatchDelete">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      empty-text="当前机构暂无期初库存"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentCode" label="期初单号" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button text type="primary" @click="router.push({ name: 'PeriodOpeningView', params: { id: row.id } })">
            {{ row.documentCode }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseName" label="仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="periodType" label="周期类型" min-width="100">
        <template #default="{ row }">{{ periodTypeLabelMap[row.periodType] ?? row.periodType }}</template>
      </el-table-column>
      <el-table-column prop="periodStartDate" label="周期开始" min-width="110" />
      <el-table-column prop="periodEndDate" label="周期结束" min-width="110" />
      <el-table-column prop="sourceType" label="来源" min-width="90">
        <template #default="{ row }">{{ row.sourceType === 'GENERATED' ? '结存生成' : '人工录入' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="100">
        <template #default="{ row }">{{ statusLabelMap[row.status] ?? row.status }}</template>
      </el-table-column>
      <el-table-column prop="totalQuantity" label="期初数量" min-width="110" show-overflow-tooltip />
      <el-table-column prop="totalAmount" label="期初金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="approvedAt" label="审核时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button text @click="router.push({ name: 'PeriodOpeningView', params: { id: row.id } })">查看</el-button>
          <el-button v-if="canEdit(row)" text @click="router.push({ name: 'PeriodOpeningEdit', params: { id: row.id } })">编辑</el-button>
          <el-button v-if="canSubmit(row)" text @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="canApprove(row)" text @click="handleApprove(row)">审核</el-button>
          <el-button v-if="canApprove(row)" text @click="handleReject(row)">驳回</el-button>
          <el-button v-if="canDelete(row)" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">已选 {{ selectedIds.length }} 条</div>
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="(page: number) => { currentPage = page; void loadRows(); }"
        @size-change="(size: number) => { pageSize = size; currentPage = 1; void loadRows(); }"
      />
    </div>

    <el-dialog v-model="generateVisible" title="从上一周期结存生成" width="520px">
      <el-form label-width="96px">
        <el-form-item label="仓库" required>
          <el-select v-model="generateForm.warehouseName" filterable style="width: 100%">
            <el-option v-for="item in warehouseOptions" :key="item.name" :label="item.label" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期类型" required>
          <el-select v-model="generateForm.periodType" style="width: 100%">
            <el-option v-for="item in periodTypeOptions" :key="item.itemCode" :label="item.itemLabel" :value="item.itemCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期开始" required>
          <el-date-picker v-model="generateForm.periodStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>
  </section>
</template>
