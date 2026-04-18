<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import {
  batchApproveGenericInventoryDocumentApi,
  batchDeleteGenericInventoryDocumentApi,
  batchUnapproveGenericInventoryDocumentApi,
  deleteGenericInventoryDocumentApi,
  fetchGenericInventoryDocumentPageApi,
  fetchGenericInventoryDocumentPermissionApi,
  type GenericInventoryDocumentRow,
} from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';
import type { InventoryDocumentMeta } from '@/views/inventory/document-meta';
import { normalizeOrgId } from '@/utils/org';

const props = defineProps<{
  meta: InventoryDocumentMeta;
}>();

const sessionStore = useSessionStore();
const router = useRouter();
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const rows = ref<GenericInventoryDocumentRow[]>([]);
const selectedIds = ref<number[]>([]);
const permissions = reactive({
  canCreate: false,
  canUpdate: false,
  canDelete: false,
  canApprove: false,
  canUnapprove: false,
});

const query = reactive({
  dateRange: [] as string[],
  documentCode: '',
  primaryName: '',
  itemName: '',
  status: '',
  remark: '',
});

const orgId = computed(() => normalizeOrgId(sessionStore.currentOrgId) || undefined);

const loadPermissions = async () => {
  if (!orgId.value) {
    permissions.canCreate = false;
    permissions.canUpdate = false;
    permissions.canDelete = false;
    permissions.canApprove = false;
    permissions.canUnapprove = false;
    return;
  }
  try {
    const result = await fetchGenericInventoryDocumentPermissionApi(props.meta.type, orgId.value);
    permissions.canCreate = Boolean(result.canCreate);
    permissions.canUpdate = Boolean(result.canUpdate);
    permissions.canDelete = Boolean(result.canDelete);
    permissions.canApprove = Boolean(result.canApprove);
    permissions.canUnapprove = Boolean(result.canUnapprove);
  } catch {
    permissions.canCreate = false;
    permissions.canUpdate = false;
    permissions.canDelete = false;
    permissions.canApprove = false;
    permissions.canUnapprove = false;
  }
};

const loadRows = async () => {
  if (!orgId.value) {
    rows.value = [];
    total.value = 0;
    selectedIds.value = [];
    return;
  }
  loading.value = true;
  try {
    const result = await fetchGenericInventoryDocumentPageApi(props.meta.type, {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      documentCode: query.documentCode || undefined,
      primaryName: query.primaryName || undefined,
      itemName: query.itemName || undefined,
      status: query.status || undefined,
      remark: query.remark || undefined,
    }, orgId.value);
    rows.value = result.list;
    total.value = result.total;
    selectedIds.value = [];
  } catch {
    rows.value = [];
    total.value = 0;
    selectedIds.value = [];
    ElMessage.error(`${props.meta.title}列表加载失败`);
  } finally {
    loading.value = false;
  }
};

const refreshAll = async () => {
  await Promise.all([loadPermissions(), loadRows()]);
};

const handleSearch = async () => {
  currentPage.value = 1;
  await loadRows();
};

const handleReset = async () => {
  query.dateRange = [];
  query.documentCode = '';
  query.primaryName = '';
  query.itemName = '';
  query.status = '';
  query.remark = '';
  currentPage.value = 1;
  await loadRows();
};

const handleToolbarAction = async (action: '新增' | '批量删除' | '批量审核' | '批量取消审核') => {
  if (action === '新增') {
    router.push({ name: props.meta.createRouteName });
    return;
  }
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择单据');
    return;
  }
  if (action === '批量删除') {
    try {
      await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条单据吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      });
    } catch {
      return;
    }
    try {
      await batchDeleteGenericInventoryDocumentApi(props.meta.type, selectedIds.value, orgId.value);
      ElMessage.success('批量删除成功');
      await loadRows();
    } catch {
      ElMessage.error('批量删除失败');
    }
    return;
  }
  if (action === '批量审核') {
    try {
      await batchApproveGenericInventoryDocumentApi(props.meta.type, selectedIds.value, orgId.value);
      ElMessage.success('批量审核成功');
      await loadRows();
    } catch {
      ElMessage.error('批量审核失败');
    }
    return;
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入拒审原因', '拒审确认', {
      confirmButtonText: '确认拒审',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入拒审原因',
      inputValidator: (input: string) => input.trim() ? true : '请填写拒审原因',
    });
    try {
      await batchUnapproveGenericInventoryDocumentApi(props.meta.type, selectedIds.value, value.trim(), orgId.value);
      ElMessage.success('批量取消审核成功');
      await loadRows();
    } catch {
      ElMessage.error('批量取消审核失败');
    }
  } catch {
    // 用户取消时不提示
  }
};

const handleDelete = async (row: GenericInventoryDocumentRow) => {
  try {
    await ElMessageBox.confirm(`确认删除单据 ${row.documentCode} 吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  try {
    await deleteGenericInventoryDocumentApi(props.meta.type, row.id, orgId.value);
    ElMessage.success('删除成功');
    await loadRows();
  } catch {
    ElMessage.error('删除失败');
  }
};

const handleSelectionChange = (items: GenericInventoryDocumentRow[]) => {
  selectedIds.value = items.map((item) => item.id);
};

const handleView = (row: GenericInventoryDocumentRow) => {
  router.push({ name: props.meta.viewRouteName, params: { id: row.id } });
};

const handleEdit = (row: GenericInventoryDocumentRow) => {
  router.push({ name: props.meta.editRouteName, params: { id: row.id } });
};

watch(() => sessionStore.currentOrgId, () => {
  rows.value = [];
  total.value = 0;
  selectedIds.value = [];
  currentPage.value = 1;
  void refreshAll();
});

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item :label="props.meta.dateLabel">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="~"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item :label="props.meta.primaryField?.label ?? '主体'">
        <el-input v-model="query.primaryName" :placeholder="`请输入${props.meta.primaryField?.label ?? '主体'}`" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="物品">
        <el-input v-model="query.itemName" placeholder="请输入物品编码/名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 140px">
          <el-option label="草稿" value="草稿" />
          <el-option label="已提交" value="已提交" />
          <el-option label="已审核" value="已审核" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" placeholder="请输入备注" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </CommonQuerySection>

    <div class="table-toolbar">
      <div class="table-toolbar-left">
        <el-button v-if="permissions.canCreate" type="primary" @click="handleToolbarAction('新增')">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
        <el-button v-if="permissions.canDelete" @click="handleToolbarAction('批量删除')">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button v-if="permissions.canApprove" @click="handleToolbarAction('批量审核')">批量审核</el-button>
        <el-button v-if="permissions.canUnapprove" @click="handleToolbarAction('批量取消审核')">批量取消审核</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="420"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="documentCode" label="单据编号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="documentDate" :label="props.meta.dateLabel" min-width="110" show-overflow-tooltip />
      <el-table-column prop="primaryName" :label="props.meta.primaryField?.label ?? '主体一'" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="props.meta.secondaryField" prop="secondaryName" :label="props.meta.secondaryField.label" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="props.meta.counterpartyField" prop="counterpartyName" :label="props.meta.counterpartyField.label" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="审核状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button v-if="permissions.canUpdate" text @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="permissions.canDelete" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ total }} 条</div>
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
  </section>
</template>
