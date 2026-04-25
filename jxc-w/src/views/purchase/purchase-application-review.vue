<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Calendar, CloseBold, EditPen, Finished, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import {
  batchPurchaseDocumentActionApi,
  createPurchaseDocumentApi,
  fetchPurchaseDocumentPageApi,
  reviewPurchaseApplicationLinesApi,
  updatePurchaseApplicationLinesApi,
  type PurchaseDocument,
  type PurchaseDocumentLine,
  type PurchaseDocumentSavePayload,
} from '@/api/modules/purchase';
import { resolveArchiveOrgId } from '@/views/items/org';

type ReviewStatus = '待审核' | '已审核' | '已驳回';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseApplicationReviewRow = {
  id: number;
  documentId: number;
  itemCode: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  supplier: string;
  purchaseUnit: string;
  applicationQty: number;
  reviewQty: number;
  purchasePrice: number;
  purchaseAmount: number;
  baseUnit: string;
  baseConversion: string;
  baseUnitQty: number;
  warehouse: string;
  expectedArrivalDate: string;
  remark: string;
  applicant: string;
  reviewStatus: ReviewStatus;
  applicationDate: string;
  applicationCode: string;
  rawDocument: PurchaseDocument;
  rawLine: PurchaseDocumentLine;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const reviewStatusOptions: ReviewStatus[] = ['待审核', '已审核', '已驳回'];
const querySchemeOptions = ['系统默认方案'];
const query = reactive({
  documentDateRange: [] as string[],
  applicationCode: '',
  warehouse: '',
  supplier: '',
  itemCategory: '',
  reviewStatus: '待审核' as ReviewStatus | '',
  itemCode: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedIds = ref<number[]>([]);
const loading = ref(false);
const optionLoading = ref(false);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemRows = ref<ItemVO[]>([]);
const tableData = ref<PurchaseApplicationReviewRow[]>([]);
const itemOptions = computed(() => itemRows.value.map((row) => ({
  value: row.code,
  label: `${row.code} / ${row.name}`,
})));
const orgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));
const selectedRows = computed(() => tableData.value.filter((row) => selectedIds.value.includes(row.id)));

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

const normalizeCategoryTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeCategoryTree(node.children) : undefined,
}));

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
      itemCategoryTree.value = [];
      itemRows.value = [];
      return;
    }
    const [categories, items] = await Promise.all([
      fetchItemCategoryTreeApi(orgId.value ?? undefined),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId.value ?? undefined)),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categories);
    itemRows.value = items;
  } catch {
    itemCategoryTree.value = [];
    itemRows.value = [];
    ElMessage.error('采购单申请审核筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const toReviewStatus = (document: PurchaseDocument, line: PurchaseDocumentLine): ReviewStatus => {
  const status = line.reviewStatus || document.reviewStatus || document.documentStatus;
  if (status === '已审核') {
    return '已审核';
  }
  if (status === '已驳回') {
    return '已驳回';
  }
  return '待审核';
};

const mapLine = (document: PurchaseDocument, line: PurchaseDocumentLine): PurchaseApplicationReviewRow => ({
  id: line.id,
  documentId: document.id,
  itemCode: line.itemCode,
  itemName: line.itemName,
  spec: line.spec,
  itemCategory: line.itemCategory,
  supplier: line.supplier || document.supplier,
  purchaseUnit: line.purchaseUnit,
  applicationQty: Number(line.quantity ?? 0),
  reviewQty: Number(line.reviewQty ?? line.quantity ?? 0),
  purchasePrice: Number(line.unitPrice ?? 0),
  purchaseAmount: Number(line.amount ?? 0),
  baseUnit: line.baseUnit,
  baseConversion: line.baseConversion,
  baseUnitQty: Number(line.quantity ?? 0),
  warehouse: line.warehouse || document.warehouse,
  expectedArrivalDate: line.expectedArrivalDate || document.expectedArrivalDate,
  remark: line.remark,
  applicant: document.applicant || document.creator,
  reviewStatus: toReviewStatus(document, line),
  applicationDate: document.documentDate,
  applicationCode: document.documentCode,
  rawDocument: document,
  rawLine: line,
});

const loadRows = async () => {
  loading.value = true;
  try {
    const page = await fetchPurchaseDocumentPageApi('application-reviews', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      startDate: query.documentDateRange[0],
      endDate: query.documentDateRange[1],
      applicationCode: query.applicationCode,
      warehouse: query.warehouse,
      supplier: query.supplier,
      reviewStatus: query.reviewStatus,
      itemCode: query.itemCode,
    }, orgId.value ?? undefined);
    const rows = page.list.flatMap((document) => document.items.map((line) => mapLine(document, line)));
    tableData.value = query.itemCategory
      ? rows.filter((row) => row.itemCategory === query.itemCategory)
      : rows;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
};

const ensureRows = (rows: PurchaseApplicationReviewRow[]) => {
  if (!rows.length) {
    ElMessage.warning('请先选择明细');
    return false;
  }
  return true;
};

const saveRows = async (rows: PurchaseApplicationReviewRow[]) => {
  if (!ensureRows(rows)) {
    return;
  }
  await updatePurchaseApplicationLinesApi(rows.map((row) => ({
    lineId: row.id,
    supplier: row.supplier,
    expectedArrivalDate: row.expectedArrivalDate,
    reviewQty: Number(row.reviewQty || 0),
    remark: row.remark,
  })), orgId.value ?? undefined);
  ElMessage.success('申请明细已保存');
  await loadRows();
};

const approveRows = async (rows: PurchaseApplicationReviewRow[]) => {
  if (!ensureRows(rows)) {
    return;
  }
  await reviewPurchaseApplicationLinesApi(rows.map((row) => ({
    lineId: row.id,
    reviewQty: Number(row.reviewQty || 0),
    remark: row.remark,
  })), true, orgId.value ?? undefined);
  ElMessage.success('已通过审批');
  selectedIds.value = [];
  await loadRows();
};

const rejectRows = async (rows: PurchaseApplicationReviewRow[]) => {
  if (!ensureRows(rows)) {
    return;
  }
  const result = await ElMessageBox.prompt('请输入驳回原因', '驳回申请明细', {
    inputType: 'textarea',
    inputPlaceholder: '驳回原因',
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
  });
  await reviewPurchaseApplicationLinesApi(rows.map((row) => ({
    lineId: row.id,
    reviewQty: Number(row.reviewQty || 0),
    remark: row.remark,
  })), false, orgId.value ?? undefined, result.value);
  ElMessage.success('已驳回');
  selectedIds.value = [];
  await loadRows();
};

const handleBatchSupplier = async () => {
  if (!ensureRows(selectedRows.value)) {
    return;
  }
  const result = await ElMessageBox.prompt('请输入供应商名称', '批量修改供应商', {
    inputPlaceholder: '供应商名称',
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  });
  selectedRows.value.forEach((row) => {
    row.supplier = result.value;
  });
  await saveRows(selectedRows.value);
};

const handleBatchArrivalDate = async () => {
  if (!ensureRows(selectedRows.value)) {
    return;
  }
  const result = await ElMessageBox.prompt('请输入期望到货日期，格式：YYYY-MM-DD', '批量修改期望到货日期', {
    inputPattern: /^\d{4}-\d{2}-\d{2}$/,
    inputErrorMessage: '日期格式必须为 YYYY-MM-DD',
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  });
  selectedRows.value.forEach((row) => {
    row.expectedArrivalDate = result.value;
  });
  await saveRows(selectedRows.value);
};

const buildOrderPayload = (rows: PurchaseApplicationReviewRow[]): PurchaseDocumentSavePayload => {
  const first = rows[0];
  const sourceCodes = [...new Set(rows.map((row) => row.applicationCode))].join(',');
  return {
    documentDate: new Date().toISOString().slice(0, 10),
    expectedArrivalDate: first.expectedArrivalDate,
    purchaseOrg: sessionStore.currentOrg?.name || '',
    warehouse: first.warehouse,
    supplier: first.supplier,
    sourceDocumentCode: sourceCodes,
    documentBizType: '申请转单',
    printStatus: '未打印',
    receiveStatus: '未收货',
    shipStatus: '未发货',
    submitter: sessionStore.userName || '',
    remark: `由采购单申请生成：${sourceCodes}`,
    items: rows.map((row) => ({
      itemCode: row.itemCode,
      itemName: row.itemName,
      spec: row.spec,
      itemCategory: row.itemCategory,
      supplier: row.supplier,
      purchaseUnit: row.purchaseUnit,
      baseUnit: row.baseUnit,
      baseConversion: row.baseConversion,
      quantity: Number(row.reviewQty || row.applicationQty || 0),
      reviewQty: Number(row.reviewQty || row.applicationQty || 0),
      unitPrice: Number(row.purchasePrice || 0),
      amount: Number(row.reviewQty || row.applicationQty || 0) * Number(row.purchasePrice || 0),
      warehouse: row.warehouse,
      expectedArrivalDate: row.expectedArrivalDate,
      remark: row.remark,
    })),
  };
};

const generateOrders = async (submit: boolean) => {
  if (!ensureRows(selectedRows.value)) {
    return;
  }
  await saveRows(selectedRows.value);
  const groups = new Map<string, PurchaseApplicationReviewRow[]>();
  selectedRows.value.forEach((row) => {
    const key = `${row.supplier || '-'}|${row.warehouse || '-'}`;
    groups.set(key, [...(groups.get(key) ?? []), row]);
  });
  const createdIds: number[] = [];
  for (const rows of groups.values()) {
    const created = await createPurchaseDocumentApi('orders', buildOrderPayload(rows), orgId.value ?? undefined);
    createdIds.push(created.id);
  }
  if (submit && createdIds.length) {
    await batchPurchaseDocumentActionApi('orders', 'submit', createdIds, orgId.value ?? undefined);
  }
  ElMessage.success(submit ? '采购订单已生成并提交审批流' : '采购订单已生成');
  selectedIds.value = [];
};

const handleSearch = () => {
  currentPage.value = 1;
  void loadRows();
};

const handleReset = () => {
  query.documentDateRange = [];
  query.applicationCode = '';
  query.warehouse = '';
  query.supplier = '';
  query.itemCategory = '';
  query.reviewStatus = '待审核';
  query.itemCode = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
  void loadRows();
};

const handleSelectionChange = (rows: PurchaseApplicationReviewRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
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
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  () => {
    void loadOptions();
    void loadRows();
  },
);

onMounted(() => {
  void loadOptions();
  void loadRows();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="申请日期">
        <el-date-picker v-model="query.documentDateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" value-format="YYYY-MM-DD" style="width: 260px" />
      </el-form-item>
      <el-form-item label="申请单号">
        <el-input v-model="query.applicationCode" clearable placeholder="请输入申请单号" style="width: 180px" />
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="query.warehouse" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 180px">
          <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" :loading="supplierLoading" clearable filterable placeholder="请选择" style="width: 180px">
          <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品类别">
        <el-tree-select v-model="query.itemCategory" :data="itemCategoryTree" clearable filterable placeholder="请选择" style="width: 180px" />
      </el-form-item>
      <el-form-item label="审核状态">
        <el-select v-model="query.reviewStatus" clearable placeholder="待审核" style="width: 140px">
          <el-option v-for="option in reviewStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemCode" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 220px">
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
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
      <el-button :disabled="!selectedIds.length" @click="handleBatchSupplier"><el-icon><EditPen /></el-icon>批量修改供应商</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleBatchArrivalDate"><el-icon><Calendar /></el-icon>批量修改期望到货日期</el-button>
      <el-button type="primary" :disabled="!tableData.length" @click="saveRows(selectedIds.length ? selectedRows : tableData)"><el-icon><Finished /></el-icon>保 存</el-button>
      <el-button :disabled="!selectedIds.length" @click="rejectRows(selectedRows)"><el-icon><CloseBold /></el-icon>批量驳回</el-button>
      <el-button :disabled="!selectedIds.length" @click="generateOrders(false)">生成采购订单</el-button>
      <el-button type="primary" :disabled="!selectedIds.length" @click="generateOrders(true)">生成并提交采购订单</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe class="erp-table" :fit="false" height="460" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="itemCode" label="物品编码" min-width="120" fixed="left" show-overflow-tooltip />
      <el-table-column prop="itemName" label="物品名称" min-width="130" fixed="left" show-overflow-tooltip />
      <el-table-column prop="spec" label="规格" min-width="110" show-overflow-tooltip />
      <el-table-column prop="itemCategory" label="物品类别" min-width="120" show-overflow-tooltip />
      <el-table-column label="供应商" min-width="180">
        <template #default="{ row }">
          <el-select v-model="row.supplier" filterable clearable placeholder="请选择" size="small" style="width: 160px">
            <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseUnit" label="采购单位" min-width="90" />
      <el-table-column prop="applicationQty" label="申请数量" min-width="100" align="right" />
      <el-table-column prop="reviewQty" label="审核数量" min-width="120" align="right">
        <template #default="{ row }">
          <el-input-number v-model="row.reviewQty" :min="0" controls-position="right" size="small" style="width: 110px" />
        </template>
      </el-table-column>
      <el-table-column prop="purchasePrice" label="采购单价" min-width="100" align="right" />
      <el-table-column label="采购金额" min-width="110" align="right">
        <template #default="{ row }">{{ (Number(row.reviewQty || 0) * Number(row.purchasePrice || 0)).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="baseUnit" label="基本单位" min-width="90" />
      <el-table-column prop="baseConversion" label="基本单位换算" min-width="130" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="130" show-overflow-tooltip />
      <el-table-column label="期望到货日期" min-width="150">
        <template #default="{ row }">
          <el-date-picker v-model="row.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 138px" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160">
        <template #default="{ row }"><el-input v-model="row.remark" clearable size="small" /></template>
      </el-table-column>
      <el-table-column prop="applicant" label="申请人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="审核状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.reviewStatus === '已审核' ? 'success' : row.reviewStatus === '已驳回' ? 'danger' : 'warning'" size="small">
            {{ row.reviewStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicationDate" label="申请日期" min-width="120" />
      <el-table-column prop="applicationCode" label="申请单号" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="approveRows([row])">审核</el-button>
          <el-button type="danger" link @click="rejectRows([row])">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <el-pagination :current-page="currentPage" :page-size="pageSize" :page-sizes="[10, 20, 50]" :total="total" background small layout="total, sizes, prev, pager, next, jumper" @current-change="handlePageChange" @size-change="handlePageSizeChange" />
    </div>
  </section>
</template>
