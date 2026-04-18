<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import {
  batchApprovePurchaseInboundApi,
  batchDeletePurchaseInboundApi,
  batchUnapprovePurchaseInboundApi,
  deletePurchaseInboundApi,
  fetchPurchaseInboundPageApi,
  fetchPurchaseInboundPermissionApi,
  type PurchaseInboundRow,
} from '@/api/modules/inventory';
import { useSessionStore } from '@/stores/session';

type TimeType = '入库日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type ReviewStatus = '未复审' | '已复审';
type ReconciliationStatus = '未对账' | '部分对账' | '已对账';
type InvoiceStatus = '未开票' | '部分开票' | '已开票';
type PrintStatus = '全部' | '未打印' | '已打印';
type SplitStatus = '未分账' | '已分账';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

const timeTypeOptions: TimeType[] = ['入库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const reviewStatusOptions: ReviewStatus[] = ['未复审', '已复审'];
const reconciliationStatusOptions: ReconciliationStatus[] = ['未对账', '部分对账', '已对账'];
const splitStatusOptions: SplitStatus[] = ['未分账', '已分账'];
const invoiceStatusOptions: InvoiceStatus[] = ['未开票', '部分开票', '已开票'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const supplierTree: TreeNode[] = [
  {
    value: 'supplier-group',
    label: '供应商组',
    children: [
      { value: '鲜达食品', label: '鲜达食品' },
      { value: '优选农场', label: '优选农场' },
      { value: '盒马包材', label: '盒马包材' },
    ],
  },
];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();
const router = useRouter();

const query = reactive({
  timeType: '入库日期' as TimeType,
  dateRange: [] as string[],
  warehouse: '',
  documentCode: '',
  supplier: '',
  itemName: '',
  documentStatus: '',
  reviewStatus: '',
  reconciliationStatus: '',
  splitStatus: '',
  upstreamCode: '',
  invoiceStatus: '',
  inspectionCount: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedIds = ref<number[]>([]);
const tableData = ref<PurchaseInboundRow[]>([]);
const loading = ref(false);
const canCreate = ref(false);
const canUpdate = ref(false);
const canDelete = ref(false);
const canApprove = ref(false);
const canUnapprove = ref(false);

const resolvePurchaseInboundOrgId = () => {
  const currentOrgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!currentOrgId || !currentOrgId.startsWith('store-')) {
    return undefined;
  }
  return currentOrgId;
};

onMounted(() => {
  void loadWarehouseTree();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    void loadWarehouseTree();
  },
);

const resolveOrgId = () => {
  return resolvePurchaseInboundOrgId();
};

const fetchTableData = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
    tableData.value = [];
    total.value = 0;
    selectedIds.value = [];
    return;
  }
  loading.value = true;
  try {
    const result = await fetchPurchaseInboundPageApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      timeType: query.timeType,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      warehouse: query.warehouse || undefined,
      documentCode: query.documentCode || undefined,
      supplier: query.supplier || undefined,
      itemName: query.itemName || undefined,
      documentStatus: query.documentStatus || undefined,
      reviewStatus: query.reviewStatus || undefined,
      reconciliationStatus: query.reconciliationStatus || undefined,
      splitStatus: query.splitStatus || undefined,
      upstreamCode: query.upstreamCode || undefined,
      invoiceStatus: query.invoiceStatus || undefined,
      inspectionCount: query.inspectionCount || undefined,
      printStatus: query.printStatus === '全部' ? undefined : query.printStatus,
      remark: query.remark || undefined,
    }, orgId);
    tableData.value = result.list;
    total.value = result.total;
    selectedIds.value = [];
  } finally {
    loading.value = false;
  }
};

const loadPermission = async () => {
  const orgId = resolveOrgId();
  if (!orgId) {
    canCreate.value = false;
    canUpdate.value = false;
    canDelete.value = false;
    canApprove.value = false;
    canUnapprove.value = false;
    return;
  }
  try {
    const result = await fetchPurchaseInboundPermissionApi(orgId);
    canCreate.value = Boolean(result.canCreate);
    canUpdate.value = Boolean(result.canUpdate);
    canDelete.value = Boolean(result.canDelete);
    canApprove.value = Boolean(result.canApprove);
    canUnapprove.value = Boolean(result.canUnapprove);
  } catch {
    canCreate.value = false;
    canUpdate.value = false;
    canDelete.value = false;
    canApprove.value = false;
    canUnapprove.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchTableData();
};

const handleReset = async () => {
  query.timeType = '入库日期';
  query.dateRange = [];
  query.warehouse = '';
  query.documentCode = '';
  query.supplier = '';
  query.itemName = '';
  query.documentStatus = '';
  query.reviewStatus = '';
  query.reconciliationStatus = '';
  query.splitStatus = '';
  query.upstreamCode = '';
  query.invoiceStatus = '';
  query.inspectionCount = '';
  query.printStatus = '全部';
  query.remark = '';
  currentPage.value = 1;
  await fetchTableData();
};

const handleToolbarAction = async (action: string) => {
  if (action === '新增') {
    router.push('/inventory/1/2/create');
    return;
  }
  if (action === '批量删除') {
    if (!selectedIds.value.length) {
      ElMessage.warning('请先选择单据');
      return;
    }
    try {
      await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条单据吗？`, '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      });
    } catch {
      return;
    }
    await batchDeletePurchaseInboundApi(selectedIds.value, resolveOrgId());
    ElMessage.success('批量删除成功');
    await fetchTableData();
    return;
  }
  if (action === '批量审核') {
    if (!selectedIds.value.length) {
      ElMessage.warning('请先选择单据');
      return;
    }
    await batchApprovePurchaseInboundApi(selectedIds.value, resolveOrgId());
    ElMessage.success('批量审核成功');
    await fetchTableData();
    return;
  }
  if (action === '批量取消审核') {
    if (!selectedIds.value.length) {
      ElMessage.warning('请先选择单据');
      return;
    }
    try {
      const { value } = await ElMessageBox.prompt('请输入拒审原因', '拒审确认', {
        confirmButtonText: '确认拒审',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入拒审原因',
        inputValidator: (value: string) => {
          if (!value.trim()) {
            return '请填写拒审原因';
          }
          return true;
        },
        inputErrorMessage: '请填写拒审原因',
        closeOnClickModal: false,
      });
      await batchUnapprovePurchaseInboundApi(selectedIds.value, value.trim(), resolveOrgId());
    } catch {
      return;
    }
    ElMessage.success('批量取消审核成功');
    await fetchTableData();
    return;
  }
  ElMessage.info(`${action}功能待接入`);
};

const handleDelete = async (row: PurchaseInboundRow) => {
  try {
    await ElMessageBox.confirm(`确认删除单据“${row.documentCode}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  const orgId = resolveOrgId();
  if (!orgId) {
    ElMessage.warning('请选择门店后再操作');
    return;
  }
  await deletePurchaseInboundApi(row.id, orgId);
  ElMessage.success('删除成功');
  await fetchTableData();
};

const handleSelectionChange = (rows: PurchaseInboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: PurchaseInboundRow) => {
  router.push(`/inventory/1/2/view/${row.id}`);
};

const handleEdit = (row: PurchaseInboundRow) => {
  router.push(`/inventory/1/2/edit/${row.id}`);
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchTableData();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchTableData();
};

onMounted(async () => {
  await loadPermission();
  await fetchTableData();
});

watch(
  () => sessionStore.currentOrgId,
  async () => {
    await loadPermission();
    await fetchTableData();
  },
);
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="时间类型">
        <el-select v-model="query.timeType" style="width: 120px">
          <el-option
            v-for="option in timeTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期~结束日期">
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
      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="供应商">
        <el-tree-select
          v-model="query.supplier"
          :data="supplierTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemName" clearable style="width: 120px">
          <el-option
            v-for="option in itemOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable style="width: 120px">
          <el-option
            v-for="option in documentStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态">
        <el-select v-model="query.reviewStatus" clearable style="width: 120px">
          <el-option
            v-for="option in reviewStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="对账状态">
        <el-select v-model="query.reconciliationStatus" clearable style="width: 120px">
          <el-option
            v-for="option in reconciliationStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商分账">
        <el-select v-model="query.splitStatus" clearable style="width: 120px">
          <el-option
            v-for="option in splitStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="上游单据号">
        <el-input v-model="query.upstreamCode" placeholder="请输入上游单据号" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="发票状态">
        <el-select v-model="query.invoiceStatus" clearable style="width: 120px">
          <el-option
            v-for="option in invoiceStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="质检次数">
        <el-input v-model="query.inspectionCount" placeholder="请输入质检次数" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="打印状态">
        <el-select v-model="query.printStatus" style="width: 120px">
          <el-option
            v-for="option in printStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" placeholder="请输入备注" clearable style="width: 150px" />
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
      <el-button v-if="canCreate" type="primary" @click="handleToolbarAction('新增')">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button @click="handleToolbarAction('批量打印')">
        <el-icon><Printer /></el-icon>
        批量打印
      </el-button>
      <el-button v-if="canDelete" @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button v-if="canApprove" @click="handleToolbarAction('批量审核')">批量审核</el-button>
      <el-button v-if="canUnapprove" @click="handleToolbarAction('批量取消审核')">批量取消审核</el-button>
      <el-button @click="handleToolbarAction('批量导出单据列表')">批量导出单据列表</el-button>
    </div>

    <el-table
      :data="tableData"
      border
      stripe
      class="erp-table"
      v-loading="loading"
      :fit="false"
      :height="360"
      :empty-text="'当前机构暂无数据'"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentCode" label="单据编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="inboundDate" label="入库日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="upstreamCode" label="上游单据号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
      <el-table-column prop="amountTaxIncluded" label="金额（含税）" min-width="110" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="审核状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reconciliationStatus" label="对账状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="invoiceStatus" label="发票状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="inspectionCount" label="质检次数" min-width="90" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button v-if="canUpdate" text @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="canDelete" text type="danger" @click="handleDelete(row)">删除</el-button>
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
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>
</template>
