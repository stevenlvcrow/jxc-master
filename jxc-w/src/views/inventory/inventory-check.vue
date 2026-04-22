<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import {
  batchApproveInventoryCheckApi,
  batchDeleteInventoryCheckApi,
  batchPrintInventoryCheckApi,
  batchSubmitInventoryCheckApi,
  batchUnapproveInventoryCheckApi,
  deleteInventoryCheckApi,
  fetchInventoryCheckPageApi,
  fetchInventoryCheckPermissionApi,
  type InventoryCheckRow,
} from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { normalizeOrgId } from '@/utils/org';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

type TimeType = '盘点日期' | '创建时间';
type PrintFilter = '' | 'UNPRINTED' | 'PRINTED';

const router = useRouter();
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const INVENTORY_CHECK_RANGE_TYPE_DICT = 'inventory.check_range_type';
const DOCUMENT_PRINT_STATUS_DICT = 'document.print_status';
const { optionsOf } = useDictionaryOptions([
  INVENTORY_DOCUMENT_STATUS_DICT,
  INVENTORY_CHECK_RANGE_TYPE_DICT,
  DOCUMENT_PRINT_STATUS_DICT,
]);
const documentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);
const checkRangeTypeOptions = optionsOf(INVENTORY_CHECK_RANGE_TYPE_DICT);
const printStatusOptions = computed(() => [
  { label: '全部', value: '' },
  ...optionsOf(DOCUMENT_PRINT_STATUS_DICT).value.map((item) => ({ label: item.itemLabel, value: item.itemCode })),
]);
const statusLabelMap = computed(() =>
  documentStatusOptions.value.reduce<Record<string, string>>((result, item) => {
    result[item.itemCode] = item.itemLabel;
    return result;
  }, {}),
);
const checkRangeLabelMap = computed(() =>
  checkRangeTypeOptions.value.reduce<Record<string, string>>((result, item) => {
    result[item.itemCode] = item.itemLabel;
    return result;
  }, {}),
);
const printStatusLabelMap = computed(() =>
  optionsOf(DOCUMENT_PRINT_STATUS_DICT).value.reduce<Record<string, string>>((result, item) => {
    result[item.itemCode] = item.itemLabel;
    return result;
  }, {}),
);

const query = reactive({
  timeType: '盘点日期' as TimeType,
  startDate: '',
  endDate: '',
  documentCode: '',
  warehouse: '',
  checkRangeType: '',
  itemName: '',
  status: '',
  printStatus: '' as PrintFilter,
  remark: '',
});

const loading = ref(false);
const rows = ref<InventoryCheckRow[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);
const permissions = reactive({
  canCreate: false,
  canUpdate: false,
  canDelete: false,
  canApprove: false,
  canUnapprove: false,
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
  const result = await fetchInventoryCheckPermissionApi('inventory-checks', orgId.value);
  permissions.canCreate = Boolean(result.canCreate);
  permissions.canUpdate = Boolean(result.canUpdate);
  permissions.canDelete = Boolean(result.canDelete);
  permissions.canApprove = Boolean(result.canApprove);
  permissions.canUnapprove = Boolean(result.canUnapprove);
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
    const result = await fetchInventoryCheckPageApi('inventory-checks', {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      timeType: query.timeType,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined,
      documentCode: query.documentCode || undefined,
      warehouse: query.warehouse || undefined,
      itemName: query.itemName || undefined,
      status: query.status || undefined,
      checkRangeType: query.checkRangeType || undefined,
      printStatus: query.printStatus || undefined,
      remark: query.remark || undefined,
    }, orgId.value);
    rows.value = result.list;
    total.value = Number(result.total ?? 0);
    selectedIds.value = [];
  } catch {
    rows.value = [];
    total.value = 0;
    selectedIds.value = [];
    ElMessage.error('盘点单列表加载失败');
  } finally {
    loading.value = false;
  }
};

const refreshAll = async () => {
  await Promise.all([loadPermissions(), loadRows(), loadWarehouseTree()]);
};

const handleSearch = async () => {
  currentPage.value = 1;
  await loadRows();
};

const handleReset = async () => {
  query.timeType = '盘点日期';
  query.startDate = '';
  query.endDate = '';
  query.documentCode = '';
  query.warehouse = '';
  query.checkRangeType = '';
  query.itemName = '';
  query.status = '';
  query.printStatus = '';
  query.remark = '';
  currentPage.value = 1;
  await loadRows();
};

const handleToolbarAction = async (action: '新增' | '批量打印' | '批量删除' | '批量提交' | '批量审核' | '批量反审核') => {
  if (action === '新增') {
    router.push({ name: 'InventoryCheckCreate' });
    return;
  }
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择单据');
    return;
  }
  if (action === '批量删除') {
    try {
      await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条盘点单吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      });
    } catch {
      return;
    }
    await batchDeleteInventoryCheckApi('inventory-checks', selectedIds.value, orgId.value);
    ElMessage.success('删除成功');
    await loadRows();
    return;
  }
  if (action === '批量提交') {
    await batchSubmitInventoryCheckApi('inventory-checks', selectedIds.value, orgId.value);
    ElMessage.success('提交成功');
    await loadRows();
    return;
  }
  if (action === '批量审核') {
    await batchApproveInventoryCheckApi('inventory-checks', selectedIds.value, orgId.value);
    ElMessage.success('审核成功');
    await loadRows();
    return;
  }
  if (action === '批量反审核') {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒审原因', '反审核确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入拒审原因',
        inputValidator: (input: string) => (input.trim() ? true : '请填写拒审原因'),
      });
      await batchUnapproveInventoryCheckApi('inventory-checks', selectedIds.value, value.trim(), orgId.value);
      ElMessage.success('反审核成功');
      await loadRows();
    } catch {
      // 用户取消
    }
    return;
  }
  await batchPrintInventoryCheckApi('inventory-checks', selectedIds.value, orgId.value);
  ElMessage.success('打印状态已更新');
  await loadRows();
};

const handleSelectionChange = (selection: InventoryCheckRow[]) => {
  selectedIds.value = selection.map((item) => item.id);
};

const handleView = (row: InventoryCheckRow) => {
  router.push({ name: 'InventoryCheckView', params: { id: row.id } });
};

const handleEdit = (row: InventoryCheckRow) => {
  router.push({ name: 'InventoryCheckEdit', params: { id: row.id } });
};

const handleDelete = async (row: InventoryCheckRow) => {
  try {
    await ElMessageBox.confirm(`确认删除盘点单 ${row.documentCode} 吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteInventoryCheckApi('inventory-checks', row.id, orgId.value);
  ElMessage.success('删除成功');
  await loadRows();
};

watch(
  () => sessionStore.currentOrgId,
  () => {
    currentPage.value = 1;
    selectedIds.value = [];
    void refreshAll();
  },
);

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="时间类型">
        <el-select v-model="query.timeType" style="width: 120px">
          <el-option label="盘点日期" value="盘点日期" />
          <el-option label="创建时间" value="创建时间" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始日期" style="width: 160px" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束日期" style="width: 160px" />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="盘点范围">
        <el-select v-model="query.checkRangeType" clearable style="width: 160px">
          <el-option label="全部" value="" />
          <el-option
            v-for="item in checkRangeTypeOptions"
            :key="item.itemCode"
            :label="item.itemLabel"
            :value="item.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-input v-model="query.itemName" placeholder="请输入物品编码/名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option
            v-for="item in documentStatusOptions"
            :key="item.itemCode"
            :label="item.itemLabel"
            :value="item.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="打印状态">
        <el-select v-model="query.printStatus" clearable style="width: 120px">
          <el-option v-for="item in printStatusOptions" :key="String(item.value)" :label="item.label" :value="String(item.value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" placeholder="请输入备注" clearable style="width: 160px" />
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
      <el-button v-if="permissions.canCreate" type="primary" @click="handleToolbarAction('新增')">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button v-if="permissions.canDelete" @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button v-if="permissions.canApprove" @click="handleToolbarAction('批量提交')">批量提交</el-button>
      <el-button v-if="permissions.canApprove" @click="handleToolbarAction('批量审核')">批量审核</el-button>
      <el-button v-if="permissions.canUnapprove" @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
      <el-button @click="handleToolbarAction('批量打印')">
        <el-icon><Printer /></el-icon>
        批量打印
      </el-button>
      <el-dropdown>
        <el-button>
          表格设置
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>单据编号</el-dropdown-item>
            <el-dropdown-item>盘点日期</el-dropdown-item>
            <el-dropdown-item>仓库</el-dropdown-item>
            <el-dropdown-item>物品数</el-dropdown-item>
            <el-dropdown-item>状态</el-dropdown-item>
            <el-dropdown-item>审核日期</el-dropdown-item>
            <el-dropdown-item>打印状态</el-dropdown-item>
            <el-dropdown-item>创建时间</el-dropdown-item>
            <el-dropdown-item>创建人</el-dropdown-item>
            <el-dropdown-item>备注</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      :empty-text="'当前机构暂无数据'"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentCode" label="单据编号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="checkDate" label="盘点日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品数" min-width="90" show-overflow-tooltip />
      <el-table-column prop="totalBookAmount" label="账面金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="totalActualAmount" label="实盘金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="totalDiffAmount" label="盈亏金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="checkRangeType" label="盘点范围" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ checkRangeLabelMap[row.checkRangeType] ?? row.checkRangeType }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ statusLabelMap[row.status] ?? row.status }}</template>
      </el-table-column>
      <el-table-column prop="diffStatus" label="盘点差异" min-width="110" show-overflow-tooltip />
      <el-table-column prop="auditDate" label="审核日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ printStatusLabelMap[row.printStatus] ?? row.printStatus }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button v-if="permissions.canUpdate" text @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="permissions.canDelete" text type="danger" @click="handleDelete(row)">删除</el-button>
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
  </section>
</template>
