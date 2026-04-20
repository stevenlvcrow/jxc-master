<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonPageNotice from '@/components/CommonPageNotice.vue';
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
import { fetchStoreWarehousesApi, type WarehouseRow } from '@/api/modules/warehouse';
import { useSessionStore } from '@/stores/session';
import type { InventoryDocumentListColumn, InventoryDocumentMeta } from '@/views/inventory/document-meta';
import { normalizeOrgId, parseStoreId } from '@/utils/org';

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
const tableHeight = computed(() => props.meta.listTableHeight ?? 350);
const showToolbar = computed(() => props.meta.showToolbar !== false);
const primaryQueryUsesSelect = computed(() => props.meta.listPrimaryQueryKind === 'select');
const visibleQueryFields = computed(() => props.meta.listQueryFields ?? [
  'dateRange',
  'documentCode',
  'primaryName',
  'itemName',
  'status',
  'remark',
]);
const statusOptions = computed(() => props.meta.listStatusOptions ?? [
  { label: '草稿', value: '草稿' },
  { label: '已提交', value: '已提交' },
  { label: '已审核', value: '已审核' },
]);
const showSelectionColumn = computed(() =>
  showToolbar.value && (permissions.canDelete || permissions.canApprove || permissions.canUnapprove),
);
const warehouseOptions = ref<Array<{ id: number; code: string; name: string; label: string }>>([]);
const warehouseCodeByName = computed<Record<string, string>>(() =>
  warehouseOptions.value.reduce<Record<string, string>>((result, item) => {
    result[item.name] = item.code;
    return result;
  }, {}),
);
const statusLabelMap = computed(() => props.meta.listStatusLabelMap ?? {});
const defaultColumns = computed<InventoryDocumentListColumn[]>(() => {
  const columns: InventoryDocumentListColumn[] = [
    { key: 'documentCode', label: '单据编号', prop: 'documentCode', minWidth: 150 },
    { key: 'documentDate', label: props.meta.dateLabel, prop: 'documentDate', minWidth: 110 },
    { key: 'primaryName', label: props.meta.primaryField?.label ?? '主体一', prop: 'primaryName', minWidth: 140 },
  ];
  if (props.meta.secondaryField) {
    columns.push({ key: 'secondaryName', label: props.meta.secondaryField.label, prop: 'secondaryName', minWidth: 140 });
  }
  if (props.meta.counterpartyField) {
    columns.push({ key: 'counterpartyName', label: props.meta.counterpartyField.label, prop: 'counterpartyName', minWidth: 140 });
  }
  columns.push(
    { key: 'status', label: '单据状态', prop: 'status', minWidth: 100 },
    { key: 'reviewStatus', label: '审核状态', prop: 'reviewStatus', minWidth: 100 },
    { key: 'amount', label: '金额', prop: 'amount', minWidth: 100 },
    { key: 'createdAt', label: '创建时间', prop: 'createdAt', minWidth: 160 },
    { key: 'creator', label: '创建人', prop: 'creator', minWidth: 100 },
    { key: 'remark', label: '备注', prop: 'remark', minWidth: 180 },
    { key: 'operation', label: '操作', type: 'operation', width: 160, fixed: 'right' },
  );
  return columns;
});
const resolvedColumns = computed(() => props.meta.listColumns ?? defaultColumns.value);
const isWarehouseOpeningBalance = computed(() => props.meta.type === 'warehouse-opening-balance');
const canConfirmCurrentType = computed(() => isWarehouseOpeningBalance.value || permissions.canApprove);
const showSummary = computed(() => props.meta.showSummary === true);
const summaryFields = computed(() => new Set(props.meta.summaryFields ?? []));

const indexMethod = (index: number) => (currentPage.value - 1) * pageSize.value + index + 1;

const formatColumnValue = (column: InventoryDocumentListColumn, row: GenericInventoryDocumentRow) => {
  if (column.formatter) {
    return column.formatter(row, { warehouseCodeByName: warehouseCodeByName.value });
  }
  if (!column.prop) {
    return '';
  }
  const rawValue = row[column.prop];
  if (column.prop === 'status') {
    return statusLabelMap.value[String(rawValue ?? '')] ?? String(rawValue ?? '');
  }
  return rawValue == null ? '' : String(rawValue);
};

const getSummaries = ({ columns, data }: { columns: Array<{ property?: string; type?: string }>; data: GenericInventoryDocumentRow[] }) => {
  let summaryLabelFilled = false;
  return columns.map((column) => {
    if (!summaryLabelFilled && column.type !== 'selection') {
      summaryLabelFilled = true;
      return props.meta.summaryLabel ?? '合计';
    }
    const property = column.property as keyof GenericInventoryDocumentRow | undefined;
    if (!property || !summaryFields.value.has(property)) {
      return '';
    }
    const totalAmount = data.reduce((sum, row) => {
      const value = Number(row[property] ?? 0);
      return Number.isFinite(value) ? sum + value : sum;
    }, 0);
    return totalAmount.toFixed(2);
  });
};

const loadWarehouseOptions = async () => {
  if (!primaryQueryUsesSelect.value) {
    warehouseOptions.value = [];
    return;
  }
  const storeId = parseStoreId(orgId.value);
  if (!storeId) {
    warehouseOptions.value = [];
    return;
  }
  try {
    const result = await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' });
    warehouseOptions.value = result.map((item: WarehouseRow) => ({
      id: item.id,
      code: item.warehouseCode,
      name: item.warehouseName,
      label: `${item.warehouseName}（${item.warehouseCode}）`,
    }));
  } catch {
    warehouseOptions.value = [];
    ElMessage.error('仓库列表加载失败');
  }
};

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
  await Promise.all([loadPermissions(), loadRows(), loadWarehouseOptions()]);
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

const handleApprove = async (row: GenericInventoryDocumentRow) => {
  try {
    await ElMessageBox.confirm('确认期初后，会改变库存数量，是否确认？', '确认期初', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  try {
    await batchApproveGenericInventoryDocumentApi(props.meta.type, [row.id], orgId.value);
    ElMessage.success('确认期初成功');
    await loadRows();
  } catch {
    ElMessage.error('确认期初失败');
  }
};

const handleSelectionChange = (items: GenericInventoryDocumentRow[]) => {
  selectedIds.value = items.map((item) => item.id);
};

const handleView = (row: GenericInventoryDocumentRow) => {
  if (isWarehouseOpeningBalance.value && row.status === 'UNINITIALIZED') {
    router.push({
      name: props.meta.createRouteName,
      query: {
        warehouseId: row.primaryId != null ? String(row.primaryId) : undefined,
        warehouseName: row.primaryName || undefined,
      },
    });
    return;
  }
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
    <CommonPageNotice v-if="props.meta.noticeLines?.length" :lines="props.meta.noticeLines" />
    <CommonQuerySection :model="query">
      <el-form-item v-if="visibleQueryFields.includes('dateRange')" :label="props.meta.dateLabel">
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
      <el-form-item v-if="visibleQueryFields.includes('documentCode')" label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item v-if="visibleQueryFields.includes('primaryName')" :label="props.meta.primaryField?.label ?? '主体'">
        <el-select
          v-if="primaryQueryUsesSelect"
          v-model="query.primaryName"
          clearable
          filterable
          style="width: 180px"
        >
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.label" :value="item.name" />
        </el-select>
        <el-input v-else v-model="query.primaryName" :placeholder="`请输入${props.meta.primaryField?.label ?? '主体'}`" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item v-if="visibleQueryFields.includes('itemName')" label="物品">
        <el-input v-model="query.itemName" placeholder="请输入物品编码/名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item v-if="visibleQueryFields.includes('status')" label="状态">
        <el-select v-model="query.status" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="`${item.label}-${item.value}`" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="visibleQueryFields.includes('remark')" label="备注">
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

    <div v-if="showToolbar" class="table-toolbar">
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
      :height="tableHeight"
      :show-summary="showSummary"
      :summary-method="getSummaries"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="showSelectionColumn" type="selection" width="48" />
      <template v-for="column in resolvedColumns" :key="column.key">
        <el-table-column
          v-if="column.type === 'index'"
          type="index"
          :label="column.label"
          :width="column.width ?? 60"
          :index="indexMethod"
        />
        <el-table-column
          v-else-if="column.type === 'operation'"
          :label="column.label"
          :width="isWarehouseOpeningBalance ? 240 : (column.width ?? 160)"
          :fixed="column.fixed ?? 'right'"
        >
          <template #default="{ row }">
            <template v-if="isWarehouseOpeningBalance">
              <el-button
                v-if="row.status === 'UNINITIALIZED'"
                text
                type="primary"
                @click="handleView(row)"
              >
                添加期初
              </el-button>
              <template v-else-if="row.status === '已提交'">
                <el-button v-if="permissions.canUpdate" text @click="handleEdit(row)">编辑</el-button>
                <el-button v-if="permissions.canDelete" text type="danger" @click="handleDelete(row)">删除</el-button>
                <el-button v-if="canConfirmCurrentType" text type="primary" @click="handleApprove(row)">确认期初</el-button>
              </template>
            </template>
            <template v-else>
              <el-button text type="primary" @click="handleView(row)">查看</el-button>
              <el-button v-if="permissions.canUpdate" text @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="permissions.canDelete" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column
          v-else
          :prop="column.prop"
          :label="column.label"
          :min-width="column.minWidth"
          :width="column.width"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ formatColumnValue(column, row) }}
          </template>
        </el-table-column>
      </template>
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
