<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  CloseBold,
  Delete,
  Download,
  Finished,
  Plus,
  Printer,
  RefreshLeft,
  RefreshRight,
  Search,
  Upload,
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import {
  batchPurchaseDocumentActionApi,
  createPurchaseDocumentApi,
  fetchPurchaseDocumentPageApi,
  updatePurchaseDocumentApi,
  type PurchaseDocument,
  type PurchaseDocumentSavePayload,
  type PurchaseDocumentType,
} from '@/api/modules/purchase';
import { resolveArchiveOrgId } from '@/views/items/org';
import {
  normalizeDocumentCode,
  pushDocumentListByCode,
  readQueryString,
} from '@/utils/documentNavigation';

type ManagedDocumentType = Extract<PurchaseDocumentType, 'orders' | 'receipts' | 'returns'>;
type DialogMode = 'create' | 'edit' | 'view';

type Props = {
  documentType: ManagedDocumentType;
  title: string;
  dateLabel: string;
  codeLabel: string;
  warehouseLabel?: string;
  amountLabel?: string;
  defaultBizType: string;
  showReceiveActions?: boolean;
  showReturnReason?: boolean;
};

type LineDraft = {
  key: number;
  itemCode: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  supplier: string;
  purchaseUnit: string;
  baseUnit: string;
  baseConversion: string;
  quantity: number;
  reviewQty: number;
  receivedQty: number;
  unitPrice: number;
  taxRate: number;
  isGift: boolean;
  warehouse: string;
  expectedArrivalDate: string;
  remark: string;
};

const props = withDefaults(defineProps<Props>(), {
  warehouseLabel: '仓库',
  amountLabel: '单据金额',
  showReceiveActions: false,
  showReturnReason: false,
});

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const documentStatusOptions = ['草稿', '已提交', '已审核', '已驳回', '已关闭', '待收货', '已收货'];
const printStatusOptions = ['全部', '未打印', '已打印'];
const returnReasonOptions = ['质量问题退货', '数量差异退货', '临期退货', '采购协商退货'];
const querySchemeOptions = ['系统默认方案'];

const query = reactive({
  dateRange: [] as string[],
  documentCode: '',
  supplier: '',
  warehouse: '',
  documentStatus: '',
  itemCode: '',
  sourceDocumentCode: '',
  printStatus: '全部',
  returnReason: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const optionLoading = ref(false);
const selectedIds = ref<number[]>([]);
const tableData = ref<PurchaseDocument[]>([]);
const itemRows = ref<ItemVO[]>([]);
const itemOptions = computed(() => itemRows.value.map((row) => ({
  value: row.code,
  label: `${row.code} / ${row.name}`,
})));

const dialogVisible = ref(false);
const dialogMode = ref<DialogMode>('create');
const editingId = ref<number | null>(null);
const lineKeySeed = ref(1);
const form = reactive({
  documentDate: new Date().toISOString().slice(0, 10),
  expectedArrivalDate: '',
  purchaseOrg: '',
  warehouse: '',
  supplier: '',
  sourceDocumentCode: '',
  downstreamDocumentCode: '',
  documentBizType: props.defaultBizType,
  shipStatus: '',
  receiveStatus: '',
  reconciliationStatus: '',
  supplierSplit: '',
  splitReceipt: '',
  printStatus: '未打印',
  returnReason: '',
  inspectionStatus: '无需质检',
  adjustedPrice: false,
  submitter: '',
  remark: '',
});
const lineRows = ref<LineDraft[]>([]);

const orgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));
const listPathMap: Record<ManagedDocumentType, string> = {
  orders: '/purchase/orders',
  receipts: '/purchase/receipts',
  returns: '/purchase/returns',
};
const listPath = computed(() => listPathMap[props.documentType]);
const dialogReadonly = computed(() => dialogMode.value === 'view');
const dialogTitle = computed(() => {
  if (dialogMode.value === 'create') {
    return `新增${props.title}`;
  }
  return dialogMode.value === 'edit' ? `编辑${props.title}` : `查看${props.title}`;
});

const flattenWarehouseTree = (nodes: WarehouseTreeNode[]) => {
  const rows: Array<{ value: string; label: string }> = [];
  const walk = (currentNodes: WarehouseTreeNode[]) => {
    currentNodes.forEach((node) => {
      if (node.children?.length) {
        walk(node.children);
        return;
      }
      rows.push({ value: node.value, label: node.label });
    });
  };
  walk(nodes);
  return rows;
};
const warehouseOptions = computed(() => flattenWarehouseTree(warehouseTree.value));

const fetchAllPages = async <T,>(
  loader: (pageNo: number, pageSizeValue: number) => Promise<{ list?: T[]; total?: number; pageSize?: number }>,
  pageSizeValue = 200,
) => {
  const rows: T[] = [];
  let pageNo = 1;
  let totalValue: number;
  do {
    const page = await loader(pageNo, pageSizeValue);
    const list = Array.isArray(page.list) ? page.list : [];
    rows.push(...list);
    totalValue = Number(page.total ?? rows.length);
    if (!list.length || Number(page.pageSize ?? 0) <= 0) {
      break;
    }
    pageNo += 1;
  } while (rows.length < totalValue);
  return rows;
};

const loadOptions = async () => {
  optionLoading.value = true;
  try {
    await Promise.all([loadWarehouseTree(), loadSupplierOptions()]);
    if (!orgId.value) {
      itemRows.value = [];
      return;
    }
    itemRows.value = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId.value ?? undefined));
  } catch {
    itemRows.value = [];
    ElMessage.error(`${props.title}筛选项加载失败`);
  } finally {
    optionLoading.value = false;
  }
};

const loadRows = async () => {
  loading.value = true;
  try {
    const page = await fetchPurchaseDocumentPageApi(props.documentType, {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1],
      documentCode: query.documentCode,
      supplier: query.supplier,
      warehouse: query.warehouse,
      documentStatus: query.documentStatus,
      itemCode: query.itemCode,
      sourceDocumentCode: query.sourceDocumentCode,
      printStatus: query.printStatus === '全部' ? '' : query.printStatus,
      returnReason: query.returnReason,
    }, orgId.value ?? undefined);
    tableData.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
};

const applyRouteQuery = () => {
  const documentCode = readQueryString(route.query.documentCode);
  const sourceDocumentCode = readQueryString(route.query.sourceDocumentCode);
  const changed = query.documentCode !== documentCode || query.sourceDocumentCode !== sourceDocumentCode;
  if (!changed) {
    return false;
  }
  query.documentCode = documentCode;
  query.sourceDocumentCode = sourceDocumentCode;
  currentPage.value = 1;
  return true;
};

const createEmptyLine = (): LineDraft => ({
  key: lineKeySeed.value++,
  itemCode: '',
  itemName: '',
  spec: '',
  itemCategory: '',
  supplier: form.supplier,
  purchaseUnit: '',
  baseUnit: '',
  baseConversion: '',
  quantity: 1,
  reviewQty: 1,
  receivedQty: props.documentType === 'receipts' ? 1 : 0,
  unitPrice: 0,
  taxRate: 0,
  isGift: false,
  warehouse: form.warehouse,
  expectedArrivalDate: form.expectedArrivalDate,
  remark: '',
});

const openCreateDialog = () => {
  router.push(`${listPath.value}/create`);
};

const openDialogFromRow = (row: PurchaseDocument, mode: DialogMode) => {
  router.push(`${listPath.value}/${mode}/${row.id}`);
};

const handleSourceDocumentClick = (documentCode: string) => {
  void pushDocumentListByCode(router, documentCode);
};

const handleItemChange = (line: LineDraft) => {
  const item = itemRows.value.find((row) => row.code === line.itemCode);
  if (!item) {
    return;
  }
  line.itemName = item.name;
  line.spec = item.spec;
  line.itemCategory = item.category;
  line.purchaseUnit = item.purchaseUnit || item.baseUnit;
  line.baseUnit = item.baseUnit;
  line.unitPrice = Number(item.suggestPrice || 0);
  line.supplier = form.supplier;
  line.warehouse = form.warehouse;
  line.expectedArrivalDate = form.expectedArrivalDate;
};

const buildPayload = (): PurchaseDocumentSavePayload => ({
  documentDate: form.documentDate,
  expectedArrivalDate: form.expectedArrivalDate,
  purchaseOrg: form.purchaseOrg,
  warehouse: form.warehouse,
  supplier: form.supplier,
  sourceDocumentCode: form.sourceDocumentCode,
  downstreamDocumentCode: form.downstreamDocumentCode,
  documentBizType: form.documentBizType || props.defaultBizType,
  shipStatus: form.shipStatus,
  receiveStatus: form.receiveStatus,
  reconciliationStatus: form.reconciliationStatus,
  supplierSplit: form.supplierSplit,
  splitReceipt: form.splitReceipt,
  printStatus: form.printStatus,
  returnReason: form.returnReason,
  inspectionStatus: form.inspectionStatus,
  adjustedPrice: form.adjustedPrice,
  submitter: form.submitter,
  remark: form.remark,
  items: lineRows.value.map((line) => ({
    itemCode: line.itemCode,
    itemName: line.itemName,
    spec: line.spec,
    itemCategory: line.itemCategory,
    supplier: line.supplier || form.supplier,
    purchaseUnit: line.purchaseUnit,
    baseUnit: line.baseUnit,
    baseConversion: line.baseConversion,
    quantity: Number(line.quantity || 0),
    reviewQty: Number(line.reviewQty || line.quantity || 0),
    receivedQty: Number(line.receivedQty || 0),
    unitPrice: Number(line.unitPrice || 0),
    taxRate: Number(line.taxRate || 0),
    amount: Number(line.quantity || 0) * Number(line.unitPrice || 0),
    isGift: line.isGift,
    warehouse: line.warehouse || form.warehouse,
    expectedArrivalDate: line.expectedArrivalDate || form.expectedArrivalDate,
    remark: line.remark,
  })),
});

const saveDialog = async () => {
  const payload = buildPayload();
  if (!payload.documentDate || !payload.warehouse) {
    ElMessage.warning(`请填写${props.dateLabel}和${props.warehouseLabel}`);
    return;
  }
  if (payload.items.some((item) => !item.itemCode || !item.itemName || item.quantity <= 0)) {
    ElMessage.warning('请完整填写物品明细，数量必须大于 0');
    return;
  }
  if (dialogMode.value === 'create') {
    await createPurchaseDocumentApi(props.documentType, payload, orgId.value ?? undefined);
    ElMessage.success(`${props.title}已新增`);
  } else if (editingId.value) {
    await updatePurchaseDocumentApi(props.documentType, editingId.value, payload, orgId.value ?? undefined);
    ElMessage.success(`${props.title}已更新`);
  }
  dialogVisible.value = false;
  await loadRows();
};

const handleSelectionChange = (rows: PurchaseDocument[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const runBatchAction = async (
  action: Parameters<typeof batchPurchaseDocumentActionApi>[1],
  successText: string,
  confirmText?: string,
  rejectionReason?: string,
) => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择单据');
    return;
  }
  if (confirmText) {
    await ElMessageBox.confirm(confirmText, '操作确认', { type: 'warning' });
  }
  await batchPurchaseDocumentActionApi(props.documentType, action, selectedIds.value, orgId.value ?? undefined, rejectionReason);
  ElMessage.success(successText);
  selectedIds.value = [];
  await loadRows();
};

const handleReject = async () => {
  const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回审批', {
    inputType: 'textarea',
    inputPlaceholder: '驳回原因',
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
  });
  await runBatchAction('reject', '已驳回', undefined, reason.value);
};

const exportRows = () => {
  const header = [
    props.dateLabel,
    props.codeLabel,
    '供应商',
    props.warehouseLabel,
    props.amountLabel,
    '单据状态',
    '审核状态',
    '来源单号',
    '打印状态',
    '备注',
  ];
  const rows = tableData.value.map((row) => [
    row.documentDate,
    row.documentCode,
    row.supplier,
    row.warehouse,
    String(row.amount ?? 0),
    row.documentStatus,
    row.reviewStatus,
    row.sourceDocumentCode,
    row.printStatus,
    row.remark,
  ]);
  const csv = [header, ...rows]
    .map((columns) => columns.map((value) => `"${String(value ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\n');
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${props.title}.csv`;
  link.click();
  URL.revokeObjectURL(url);
};

const statusTagType = (status: string) => {
  if (status === '已审核' || status === '已收货') {
    return 'success';
  }
  if (status === '已驳回') {
    return 'danger';
  }
  if (status === '已提交' || status === '待收货') {
    return 'warning';
  }
  return 'info';
};

const handleSearch = () => {
  currentPage.value = 1;
  void loadRows();
};

const handleReset = () => {
  query.dateRange = [];
  query.documentCode = '';
  query.supplier = '';
  query.warehouse = '';
  query.documentStatus = '';
  query.itemCode = '';
  query.sourceDocumentCode = '';
  query.printStatus = '全部';
  query.returnReason = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
  void loadRows();
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  void loadRows();
};
const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  void loadRows();
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode, props.documentType],
  () => {
    void loadOptions();
    void loadRows();
  },
);

watch(
  () => [route.query.documentCode, route.query.sourceDocumentCode],
  () => {
    if (applyRouteQuery()) {
      void loadRows();
    }
  },
);

onMounted(() => {
  applyRouteQuery();
  void loadOptions();
  void loadRows();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item :label="dateLabel">
        <el-date-picker v-model="query.dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" value-format="YYYY-MM-DD" style="width: 260px" />
      </el-form-item>
      <el-form-item :label="codeLabel">
        <el-input v-model="query.documentCode" clearable :placeholder="`请输入${codeLabel}`" style="width: 180px" />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" :loading="supplierLoading" clearable filterable placeholder="请选择" style="width: 180px">
          <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="warehouseLabel">
        <el-select v-model="query.warehouse" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 180px">
          <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemCode" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 220px">
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源单号">
        <el-input v-model="query.sourceDocumentCode" clearable placeholder="请输入来源单号" style="width: 160px" />
      </el-form-item>
      <el-form-item label="打印状态">
        <el-select v-model="query.printStatus" style="width: 120px">
          <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="showReturnReason" label="退货原因">
        <el-select v-model="query.returnReason" clearable placeholder="请选择" style="width: 160px">
          <el-option v-for="option in returnReasonOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="handleReset"><el-icon><RefreshRight /></el-icon>重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <div class="table-toolbar">
      <div class="table-toolbar__left">
        <el-button type="primary" @click="openCreateDialog"><el-icon><Plus /></el-icon>新增</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('submit', '已提交审批流')"><el-icon><Upload /></el-icon>提交审批</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('approve', '审批已通过')"><el-icon><Finished /></el-icon>审核通过</el-button>
        <el-button :disabled="!selectedIds.length" @click="handleReject"><el-icon><CloseBold /></el-icon>驳回</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('unapprove', '已反审核')"><el-icon><RefreshLeft /></el-icon>反审核</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('print', '已标记打印')"><el-icon><Printer /></el-icon>打印</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('close', '已关闭')">关闭</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('cancel-close', '已取消关闭')">取消关闭</el-button>
        <el-button v-if="showReceiveActions" :disabled="!selectedIds.length" @click="runBatchAction('receive', '已确认收货')">收货</el-button>
        <el-button v-if="showReceiveActions" :disabled="!selectedIds.length" @click="runBatchAction('cancel-receive', '已取消收货')">取消收货</el-button>
        <el-button :disabled="!selectedIds.length" @click="runBatchAction('delete', '已删除', `确认删除选中的${title}？`)"><el-icon><Delete /></el-icon>删除</el-button>
      </div>
      <div class="table-toolbar__right">
        <el-button @click="exportRows"><el-icon><Download /></el-icon>导出</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe class="erp-table" :fit="false" height="460" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentDate" :label="dateLabel" min-width="120" show-overflow-tooltip />
      <el-table-column prop="documentCode" :label="codeLabel" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button v-if="row.documentCode" type="primary" link @click="openDialogFromRow(row, 'view')">
            {{ row.documentCode }}
          </el-button>
          <template v-else>-</template>
        </template>
      </el-table-column>
      <el-table-column prop="supplier" label="供应商" min-width="150" show-overflow-tooltip />
      <el-table-column prop="warehouse" :label="warehouseLabel" min-width="140" show-overflow-tooltip />
      <el-table-column prop="amount" :label="amountLabel" min-width="110" align="right" />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" align="right" />
      <el-table-column prop="documentStatus" label="单据状态" min-width="105">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.documentStatus)" size="small">{{ row.documentStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reviewStatus" label="审核状态" min-width="100" />
      <el-table-column prop="workflowTaskName" label="当前审批节点" min-width="130" show-overflow-tooltip />
      <el-table-column v-if="showReceiveActions" prop="receiveStatus" label="收货状态" min-width="100" />
      <el-table-column prop="sourceDocumentCode" label="来源单号" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button
            v-if="normalizeDocumentCode(row.sourceDocumentCode)"
            type="primary"
            link
            @click.stop="handleSourceDocumentClick(row.sourceDocumentCode)"
          >
            {{ row.sourceDocumentCode }}
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="downstreamDocumentCode" label="下游单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="documentBizType" label="单据类型" min-width="120" show-overflow-tooltip />
      <el-table-column v-if="showReturnReason" prop="returnReason" label="退货原因" min-width="130" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDialogFromRow(row, 'edit')">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <el-pagination :current-page="currentPage" :page-size="pageSize" :page-sizes="[10, 20, 50]" :total="total" background small layout="total, sizes, prev, pager, next, jumper" @current-change="handlePageChange" @size-change="handlePageSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1120px" destroy-on-close>
      <el-form :model="form" label-width="104px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item :label="dateLabel"><el-date-picker v-model="form.documentDate" :disabled="dialogReadonly" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="期望到货"><el-date-picker v-model="form.expectedArrivalDate" :disabled="dialogReadonly" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="warehouseLabel">
              <el-select v-model="form.warehouse" :disabled="dialogReadonly" filterable style="width: 100%">
                <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="供应商">
              <el-select v-model="form.supplier" :disabled="dialogReadonly" filterable clearable style="width: 100%">
                <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="采购组织"><el-input v-model="form.purchaseOrg" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="来源单号"><el-input v-model="form.sourceDocumentCode" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="下游单号"><el-input v-model="form.downstreamDocumentCode" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="单据类型"><el-input v-model="form.documentBizType" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col v-if="showReceiveActions" :span="6">
            <el-form-item label="发货状态"><el-input v-model="form.shipStatus" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
          <el-col v-if="showReceiveActions" :span="6">
            <el-form-item label="收货状态"><el-input v-model="form.receiveStatus" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
          <el-col v-if="documentType === 'orders'" :span="6">
            <el-form-item label="拆单方式"><el-input v-model="form.splitReceipt" :disabled="dialogReadonly" clearable /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="打印状态">
              <el-select v-model="form.printStatus" :disabled="dialogReadonly" style="width: 100%">
                <el-option label="未打印" value="未打印" />
                <el-option label="已打印" value="已打印" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="showReturnReason" :span="6">
            <el-form-item label="退货原因">
              <el-select v-model="form.returnReason" :disabled="dialogReadonly" filterable clearable style="width: 100%">
                <el-option v-for="option in returnReasonOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" :disabled="dialogReadonly" clearable /></el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button v-if="!dialogReadonly" @click="lineRows.push(createEmptyLine())"><el-icon><Plus /></el-icon>添加物品</el-button>
      </div>
      <el-table :data="lineRows" border stripe class="erp-table" :fit="false" height="320">
        <el-table-column type="index" label="序号" width="56" fixed="left" />
        <el-table-column label="物品" min-width="240" fixed="left">
          <template #default="{ row }">
            <el-select v-model="row.itemCode" :disabled="dialogReadonly" filterable placeholder="请选择物品" style="width: 220px" @change="handleItemChange(row)">
              <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" min-width="110" show-overflow-tooltip />
        <el-table-column prop="purchaseUnit" label="采购单位" min-width="100" />
        <el-table-column label="数量" min-width="130" align="right">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :disabled="dialogReadonly" :min="0" controls-position="right" size="small" style="width: 110px" /></template>
        </el-table-column>
        <el-table-column v-if="documentType === 'receipts'" label="收货数量" min-width="130" align="right">
          <template #default="{ row }"><el-input-number v-model="row.receivedQty" :disabled="dialogReadonly" :min="0" controls-position="right" size="small" style="width: 110px" /></template>
        </el-table-column>
        <el-table-column label="单价" min-width="130" align="right">
          <template #default="{ row }"><el-input-number v-model="row.unitPrice" :disabled="dialogReadonly" :min="0" controls-position="right" size="small" style="width: 110px" /></template>
        </el-table-column>
        <el-table-column label="金额" min-width="110" align="right">
          <template #default="{ row }">{{ (Number(row.quantity || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="赠品" min-width="80">
          <template #default="{ row }"><el-checkbox v-model="row.isGift" :disabled="dialogReadonly" /></template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }"><el-input v-model="row.remark" :disabled="dialogReadonly" clearable /></template>
        </el-table-column>
        <el-table-column v-if="!dialogReadonly" label="操作" width="80" fixed="right">
          <template #default="{ $index }"><el-button type="danger" link @click="lineRows.splice($index, 1)">删除</el-button></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!dialogReadonly" type="primary" @click="saveDialog">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
