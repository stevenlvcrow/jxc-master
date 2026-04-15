<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection, { type ToolbarButton } from '@/components/CommonToolbarSection.vue';
import { useSessionStore } from '@/stores/session';
import {
  deleteItemRuleApi,
  fetchItemRulesApi,
  type ItemRuleRow,
} from '@/api/modules/warehouse-item-rule';

type RuleStatus = 'ENABLED' | 'DISABLED';

const router = useRouter();
const sessionStore = useSessionStore();
const loading = ref(false);

const toolbarButtons: ToolbarButton[] = [
  { key: '新增', label: '新增', type: 'primary' },
];

const query = reactive({
  ruleCode: '',
  ruleName: '',
});

const tableData = ref<ItemRuleRow[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const resolveGroupId = (): number | null => {
  const orgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!orgId.startsWith('group-')) return null;
  const id = Number(orgId.slice('group-'.length));
  return Number.isNaN(id) ? null : id;
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
};

const filteredRows = computed(() => {
  const codeKeyword = query.ruleCode.trim().toLowerCase();
  const nameKeyword = query.ruleName.trim().toLowerCase();
  return tableData.value.filter((row) => {
    const matchCode = !codeKeyword || row.ruleCode.toLowerCase().includes(codeKeyword);
    const matchName = !nameKeyword || row.ruleName.toLowerCase().includes(nameKeyword);
    return matchCode && matchName;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const loadData = async () => {
  const groupId = resolveGroupId();
  if (!groupId) {
    tableData.value = [];
    return;
  }
  loading.value = true;
  try {
    tableData.value = await fetchItemRulesApi(groupId);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.ruleCode = '';
  query.ruleName = '';
  currentPage.value = 1;
};

const handleView = (row: ItemRuleRow) => {
  ElMessage.info(`查看功能待接入，规则：${row.ruleName}`);
};

const handleEdit = (row: ItemRuleRow) => {
  ElMessage.info(`编辑功能待接入，规则：${row.ruleName}`);
};

const handleDelete = async (row: ItemRuleRow) => {
  await ElMessageBox.confirm(`确认删除规则「${row.ruleName}」吗？`, '提示', { type: 'warning' });
  await deleteItemRuleApi(row.id);
  ElMessage.success('删除成功');
  await loadData();
};

const statusText = (status: RuleStatus) => (status === 'ENABLED' ? '启用' : '停用');

const handleToolbarAction = (action: string) => {
  if (action === '新增') {
    router.push({ name: 'ItemRuleCreate' });
  }
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

watch(() => sessionStore.currentOrgId, () => {
  currentPage.value = 1;
  loadData();
});

onMounted(loadData);
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="规则编码">
        <el-input v-model="query.ruleCode" placeholder="请输入规则编码" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="规则名称">
        <el-input v-model="query.ruleName" placeholder="请输入规则名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      :empty-text="'当前机构暂无数据'"
      v-loading="loading"
    >
      <el-table-column type="index" label="序号" width="56" />
      <el-table-column prop="ruleCode" label="规则编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="ruleName" label="规则名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="业务管控" width="90" align="center">
        <template #default="{ row }">{{ row.businessControl ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">{{ statusText(row.status as RuleStatus) }}</template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column label="创建时间" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="最新修改日期" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text @click="handleEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ filteredRows.length }} 条</div>
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredRows.length"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>
</template>
