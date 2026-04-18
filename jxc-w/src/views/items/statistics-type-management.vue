<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import ItemPaginationSection from './components/ItemPaginationSection.vue';
import {
  createItemStatisticsTypeApi,
  exportItemStatisticsTypesApi,
  fetchItemStatisticsTypeDetailApi,
  fetchItemStatisticsTypesApi,
  type ItemStatisticsTypeDetail,
  type ItemStatisticsTypeRow,
} from '@/api/modules/item';
import { useSessionStore } from '@/stores/session';
import { requireItemOrgId } from './org';

const sessionStore = useSessionStore();
const tableData = ref<ItemStatisticsTypeRow[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const tableHeight = 360;
const tableLoading = ref(false);
const createSubmitting = ref(false);
const detailLoading = ref(false);
const exportLoading = ref(false);
const toolbarButtons = ['新增统计类型', '批量导出'];

const createDialogVisible = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive({
  name: '',
  statisticsCategory: '成本类',
});
const createFormRules: FormRules = {
  name: [{ required: true, message: '请输入统计类型名称', trigger: 'blur' }],
  statisticsCategory: [{ required: true, message: '请选择统计分类', trigger: 'change' }],
};

const detailDialogVisible = ref(false);
const detailData = ref<ItemStatisticsTypeDetail | null>(null);

const resolveItemOrgId = () => {
  return requireItemOrgId(sessionStore.currentOrgId);
};

const fetchList = async () => {
  tableLoading.value = true;
  try {
    const result = await fetchItemStatisticsTypesApi(
      {
        pageNo: currentPage.value,
        pageSize: pageSize.value,
      },
      resolveItemOrgId(),
    );
    tableData.value = result.list ?? [];
    total.value = result.total ?? 0;
  } finally {
    tableLoading.value = false;
  }
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchList();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchList();
};

const openCreateDialog = () => {
  createForm.name = '';
  createForm.statisticsCategory = '成本类';
  createDialogVisible.value = true;
};

const submitCreate = async () => {
  if (!createFormRef.value) {
    return;
  }
  const valid = await createFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }
  createSubmitting.value = true;
  try {
    await createItemStatisticsTypeApi(
      {
        name: createForm.name.trim(),
        statisticsCategory: createForm.statisticsCategory,
      },
      resolveItemOrgId(),
    );
    ElMessage.success('新增统计类型成功');
    createDialogVisible.value = false;
    currentPage.value = 1;
    await fetchList();
  } finally {
    createSubmitting.value = false;
  }
};

const openDetailDialog = async (row: ItemStatisticsTypeRow) => {
  detailDialogVisible.value = true;
  detailLoading.value = true;
  detailData.value = null;
  try {
    detailData.value = await fetchItemStatisticsTypeDetailApi(row.id, resolveItemOrgId());
  } finally {
    detailLoading.value = false;
  }
};

const toCsvCell = (value: unknown) => {
  const text = String(value ?? '');
  const escaped = text.replace(/"/g, '""');
  return `"${escaped}"`;
};

const handleExport = async () => {
  exportLoading.value = true;
  try {
    const result = await exportItemStatisticsTypesApi(undefined, resolveItemOrgId());
    const header = ['编码', '名称', '统计分类', '创建类型', '修改时间'];
    const lines = [header.map(toCsvCell).join(',')];
    (result.rows ?? []).forEach((row) => {
      lines.push([
        row.code,
        row.name,
        row.statisticsCategory,
        row.createType,
        row.modifiedTime,
      ].map(toCsvCell).join(','));
    });

    const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = result.fileName || 'statistics-types.csv';
    document.body.appendChild(link);
    link.click();
    URL.revokeObjectURL(link.href);
    document.body.removeChild(link);
    ElMessage.success('导出成功');
  } finally {
    exportLoading.value = false;
  }
};

const handleToolbarAction = async (action: string) => {
  if (action === '新增统计类型') {
    openCreateDialog();
    return;
  }
  if (action === '批量导出') {
    await handleExport();
    return;
  }
};

watch(
  () => sessionStore.currentOrgId,
  async () => {
    currentPage.value = 1;
    await fetchList();
  },
);

onMounted(async () => {
  await fetchList();
});
</script>

<template>
  <section class="panel item-main-panel">
    <div class="table-toolbar">
      <el-button
        v-for="(button, index) in toolbarButtons"
        :key="button"
        :type="index === 0 ? 'primary' : 'default'"
        :loading="button === '批量导出' ? exportLoading : false"
        @click="handleToolbarAction(button)"
      >
        {{ button }}
      </el-button>
    </div>

    <el-table
      v-loading="tableLoading"
      :data="tableData"
      :fit="false"
      border
      stripe
      scrollbar-always-on
      class="erp-table"
      :height="tableHeight"
    >
      <el-table-column prop="index" label="序号" width="60" align="center" />
      <el-table-column prop="code" label="编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="statisticsCategory" label="统计分类" min-width="120" show-overflow-tooltip />
      <el-table-column prop="createType" label="创建类型" min-width="100" show-overflow-tooltip />
      <el-table-column prop="modifiedTime" label="修改时间" min-width="150" align="center" />
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDialog(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <ItemPaginationSection
      :selected-count="0"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:current-page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />
  </section>

  <el-dialog
    v-model="createDialogVisible"
    title="新增统计类型"
    width="520px"
    destroy-on-close
  >
    <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="100px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="createForm.name" maxlength="64" placeholder="请输入统计类型名称" />
      </el-form-item>
      <el-form-item label="统计分类" prop="statisticsCategory">
        <el-select v-model="createForm.statisticsCategory" style="width: 100%">
          <el-option label="成本类" value="成本类" />
          <el-option label="费用类" value="费用类" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="detailDialogVisible"
    title="统计类型详情"
    width="560px"
    destroy-on-close
  >
    <el-skeleton :rows="5" animated :loading="detailLoading">
      <template #default>
        <el-descriptions v-if="detailData" :column="1" border>
          <el-descriptions-item label="编码">{{ detailData.code }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detailData.name }}</el-descriptions-item>
          <el-descriptions-item label="统计分类">{{ detailData.statisticsCategory }}</el-descriptions-item>
          <el-descriptions-item label="创建类型">{{ detailData.createType }}</el-descriptions-item>
          <el-descriptions-item label="修改时间">{{ detailData.modifiedTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-skeleton>
  </el-dialog>
</template>
