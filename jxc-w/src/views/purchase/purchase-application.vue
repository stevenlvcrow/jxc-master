<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Delete, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import {
  batchPurchaseDocumentActionApi,
  createPurchaseDocumentApi,
  fetchPurchaseDocumentPageApi,
  updatePurchaseDocumentApi,
  type PurchaseDocument,
  type PurchaseDocumentSavePayload,
} from '@/api/modules/purchase';
import { resolveArchiveOrgId } from '@/views/items/org';

type DocumentStatus = '草稿' | '已提交' | '已审核' | '已关闭' | '已驳回';
type PurchaseApplicationRow = {
  id: number;
  documentDate: string;
  applicationCode: string;
  warehouse: string;
  itemCount: number;
  documentStatus: DocumentStatus;
  sourceDocumentCode: string;
  downstreamDocumentCode: string;
  createSource: string;
  creator: string;
  createdAt: string;
  raw: PurchaseDocument;
};
type ApplicationLineDraft = {
  key: number;
  itemCode: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  purchaseUnit: string;
  baseUnit: string;
  quantity: number;
  unitPrice: number;
  remark: string;
};

const sessionStore = useSessionStore();
const router = useRouter();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核', '已关闭', '已驳回'];
const querySchemeOptions = ['系统默认方案'];
const query = reactive({
  documentDateRange: [] as string[],
  applicationCode: '',
  warehouse: '',
  documentStatus: '',
  itemCode: '',
  creator: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedIds = ref<number[]>([]);
const loading = ref(false);
const optionLoading = ref(false);
const itemRows = ref<ItemVO[]>([]);
const itemOptions = computed(() => itemRows.value.map((row) => ({
  value: row.code,
  label: `${row.code} / ${row.name}`,
})));

const tableData = ref<PurchaseApplicationRow[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit' | 'view'>('create');
const editingId = ref<number | null>(null);
const lineKeySeed = ref(1);
const form = reactive({
  documentDate: new Date().toISOString().slice(0, 10),
  expectedArrivalDate: '',
  warehouse: '',
  sourceDocumentCode: '',
  downstreamDocumentCode: '',
  remark: '',
});
const lineRows = ref<ApplicationLineDraft[]>([]);

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
const orgId = computed(() => resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode));
const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增采购单申请' : dialogMode.value === 'edit' ? '编辑采购单申请' : '查看采购单申请'));
const dialogReadonly = computed(() => dialogMode.value === 'view');

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
    await loadWarehouseTree();
    if (!orgId.value) {
      itemRows.value = [];
      return;
    }
    itemRows.value = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId.value ?? undefined));
  } catch {
    itemRows.value = [];
    ElMessage.error('采购单申请筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const mapRow = (row: PurchaseDocument): PurchaseApplicationRow => ({
  id: row.id,
  documentDate: row.documentDate,
  applicationCode: row.documentCode,
  warehouse: row.warehouse,
  itemCount: row.itemCount,
  documentStatus: row.documentStatus as DocumentStatus,
  sourceDocumentCode: row.sourceDocumentCode,
  downstreamDocumentCode: row.downstreamDocumentCode,
  createSource: row.documentBizType,
  creator: row.creator || row.applicant,
  createdAt: row.createdAt,
  raw: row,
});

const loadRows = async () => {
  loading.value = true;
  try {
    const page = await fetchPurchaseDocumentPageApi('applications', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      startDate: query.documentDateRange[0],
      endDate: query.documentDateRange[1],
      applicationCode: query.applicationCode,
      warehouse: query.warehouse,
      documentStatus: query.documentStatus,
      itemCode: query.itemCode,
    }, orgId.value ?? undefined);
    tableData.value = page.list.map(mapRow);
    total.value = page.total;
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  editingId.value = null;
  form.documentDate = new Date().toISOString().slice(0, 10);
  form.expectedArrivalDate = '';
  form.warehouse = '';
  form.sourceDocumentCode = '';
  form.downstreamDocumentCode = '';
  form.remark = '';
  lineRows.value = [createEmptyLine()];
};

const createEmptyLine = (): ApplicationLineDraft => ({
  key: lineKeySeed.value++,
  itemCode: '',
  itemName: '',
  spec: '',
  itemCategory: '',
  purchaseUnit: '',
  baseUnit: '',
  quantity: 1,
  unitPrice: 0,
  remark: '',
});

const openCreateDialog = () => {
  router.push('/purchase/applications/create');
};

const openDialogFromRow = (row: PurchaseApplicationRow, mode: 'edit' | 'view') => {
  router.push(`/purchase/applications/${mode}/${row.id}`);
};

const handleItemChange = (line: ApplicationLineDraft) => {
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
};

const buildPayload = (): PurchaseDocumentSavePayload => ({
  documentDate: form.documentDate,
  expectedArrivalDate: form.expectedArrivalDate,
  warehouse: form.warehouse,
  sourceDocumentCode: form.sourceDocumentCode,
  downstreamDocumentCode: form.downstreamDocumentCode,
  documentBizType: '手工创建',
  applicant: sessionStore.currentOrg?.name || '',
  remark: form.remark,
  items: lineRows.value.map((line) => ({
    itemCode: line.itemCode,
    itemName: line.itemName,
    spec: line.spec,
    itemCategory: line.itemCategory,
    purchaseUnit: line.purchaseUnit,
    baseUnit: line.baseUnit,
    quantity: Number(line.quantity || 0),
    reviewQty: Number(line.quantity || 0),
    unitPrice: Number(line.unitPrice || 0),
    amount: Number(line.quantity || 0) * Number(line.unitPrice || 0),
    warehouse: form.warehouse,
    expectedArrivalDate: form.expectedArrivalDate,
    remark: line.remark,
  })),
});

const saveDialog = async () => {
  const payload = buildPayload();
  if (!payload.documentDate || !payload.warehouse) {
    ElMessage.warning('请填写单据日期和仓库');
    return;
  }
  if (payload.items.some((item) => !item.itemCode || !item.itemName || item.quantity <= 0)) {
    ElMessage.warning('请完整填写物品明细，数量必须大于 0');
    return;
  }
  if (dialogMode.value === 'create') {
    await createPurchaseDocumentApi('applications', payload, orgId.value ?? undefined);
    ElMessage.success('采购单申请已新增');
  } else if (editingId.value) {
    await updatePurchaseDocumentApi('applications', editingId.value, payload, orgId.value ?? undefined);
    ElMessage.success('采购单申请已更新');
  }
  dialogVisible.value = false;
  await loadRows();
};

const handleSearch = () => {
  currentPage.value = 1;
  void loadRows();
};

const handleReset = () => {
  query.documentDateRange = [];
  query.applicationCode = '';
  query.warehouse = '';
  query.documentStatus = '';
  query.itemCode = '';
  query.creator = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
  void loadRows();
};

const handleSelectionChange = (rows: PurchaseApplicationRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleBatchDelete = async () => {
  await ElMessageBox.confirm('确认删除选中的采购单申请？', '删除确认', { type: 'warning' });
  await batchPurchaseDocumentActionApi('applications', 'delete', selectedIds.value, orgId.value ?? undefined);
  ElMessage.success('已删除');
  selectedIds.value = [];
  await loadRows();
};

const handleBatchSubmit = async () => {
  await batchPurchaseDocumentActionApi('applications', 'submit', selectedIds.value, orgId.value ?? undefined);
  ElMessage.success('已提交审批流');
  selectedIds.value = [];
  await loadRows();
};

const handleView = (row: PurchaseApplicationRow) => openDialogFromRow(row, 'view');
const handleEdit = (row: PurchaseApplicationRow) => openDialogFromRow(row, 'edit');
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
      <el-form-item label="单据日期">
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
        <el-button :disabled="!selectedIds.length" @click="handleBatchSubmit">提交审批</el-button>
        <el-button :disabled="!selectedIds.length" @click="handleBatchDelete"><el-icon><Delete /></el-icon>删除</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe class="erp-table" :fit="false" height="460" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentDate" label="单据日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="applicationCode" label="申请单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" align="right" />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.documentStatus === '已审核' ? 'success' : row.documentStatus === '已驳回' ? 'danger' : row.documentStatus === '已提交' ? 'warning' : 'info'" size="small">
            {{ row.documentStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceDocumentCode" label="来源单据号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="downstreamDocumentCode" label="下游单据号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createSource" label="创建来源" min-width="110" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <el-pagination :current-page="currentPage" :page-size="pageSize" :page-sizes="[10, 20, 50]" :total="total" background small layout="total, sizes, prev, pager, next, jumper" @current-change="handlePageChange" @size-change="handlePageSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1040px" destroy-on-close>
      <el-form :model="form" label-width="110px" class="dialog-form">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="单据日期"><el-date-picker v-model="form.documentDate" :disabled="dialogReadonly" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="期望到货"><el-date-picker v-model="form.expectedArrivalDate" :disabled="dialogReadonly" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="仓库">
              <el-select v-model="form.warehouse" :disabled="dialogReadonly" filterable style="width: 100%">
                <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="来源单号"><el-input v-model="form.sourceDocumentCode" :disabled="dialogReadonly" clearable /></el-form-item>
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
        <el-table-column label="单价" min-width="130" align="right">
          <template #default="{ row }"><el-input-number v-model="row.unitPrice" :disabled="dialogReadonly" :min="0" controls-position="right" size="small" style="width: 110px" /></template>
        </el-table-column>
        <el-table-column label="金额" min-width="110" align="right">
          <template #default="{ row }">{{ (Number(row.quantity || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template>
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
