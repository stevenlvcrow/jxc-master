<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, Printer, RefreshLeft, RefreshRight, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type ReturnReason = '质量问题' | '到货差异' | '价格调整' | '其他';
type DocumentStatus = '草稿' | '已提交' | '已审核' | '已关闭';
type ReconciliationStatus = '未对账' | '部分对账' | '已对账';
type SupplierSplitStatus = '未分账' | '已分账';
type DocumentType = '普通退货' | '按单退货' | '退补退货';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseReturnRow = {
  id: number;
  returnDate: string;
  documentCode: string;
  sourceCode: string;
  supplier: string;
  returnWarehouse: string;
  returnReason: ReturnReason;
  itemCount: number;
  returnAmount: number;
  documentStatus: DocumentStatus;
  reconciliationStatus: ReconciliationStatus;
  supplierSplit: SupplierSplitStatus;
  documentType: DocumentType;
  printStatus: Exclude<PrintStatus, '全部'>;
  submitter: string;
  remark: string;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const returnReasonOptions: ReturnReason[] = ['质量问题', '到货差异', '价格调整', '其他'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核', '已关闭'];
const reconciliationStatusOptions: ReconciliationStatus[] = ['未对账', '部分对账', '已对账'];
const supplierSplitOptions: SupplierSplitStatus[] = ['未分账', '已分账'];
const documentTypeOptions: DocumentType[] = ['普通退货', '按单退货', '退补退货'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const querySchemeOptions = ['系统默认方案'];
const submitterTree: TreeNode[] = [
  {
    value: 'purchase-team',
    label: '采购组',
    children: [
      { value: '张敏', label: '张敏' },
      { value: '李娜', label: '李娜' },
    ],
  },
  {
    value: 'warehouse-team',
    label: '仓储组',
    children: [
      { value: '王磊', label: '王磊' },
      { value: '赵晨', label: '赵晨' },
    ],
  },
];

const query = reactive({
  returnDateRange: [] as string[],
  documentCode: '',
  supplier: '',
  returnWarehouse: '',
  returnReason: '',
  documentStatus: '',
  reconciliationStatus: '',
  supplierSplit: '',
  documentType: '',
  itemCode: '',
  sourceCode: '',
  submitter: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const tableData = ref<PurchaseReturnRow[]>([
  {
    id: 1,
    returnDate: '2026-04-23',
    documentCode: 'PRT-202604-001',
    sourceCode: 'PR-202604-001',
    supplier: '鲜达食品',
    returnWarehouse: '中央成品仓',
    returnReason: '质量问题',
    itemCount: 1,
    returnAmount: 925,
    documentStatus: '已审核',
    reconciliationStatus: '未对账',
    supplierSplit: '未分账',
    documentType: '按单退货',
    printStatus: '已打印',
    submitter: '张敏',
    remark: '冷链温度异常',
  },
  {
    id: 2,
    returnDate: '2026-04-24',
    documentCode: 'PRT-202604-002',
    sourceCode: 'PO-202604-002',
    supplier: '优选农场',
    returnWarehouse: '北区原料仓',
    returnReason: '到货差异',
    itemCount: 2,
    returnAmount: 520,
    documentStatus: '已提交',
    reconciliationStatus: '未对账',
    supplierSplit: '未分账',
    documentType: '普通退货',
    printStatus: '未打印',
    submitter: '李娜',
    remark: '数量差异待确认',
  },
  {
    id: 3,
    returnDate: '2026-04-24',
    documentCode: 'PRT-202604-003',
    sourceCode: '-',
    supplier: '盒马包材',
    returnWarehouse: '南区包材仓',
    returnReason: '价格调整',
    itemCount: 1,
    returnAmount: 96,
    documentStatus: '草稿',
    reconciliationStatus: '部分对账',
    supplierSplit: '已分账',
    documentType: '退补退货',
    printStatus: '未打印',
    submitter: '王磊',
    remark: '退补差价',
  },
]);

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
const supplierTree = computed<TreeNode[]>(() => supplierOptions.value.map((item) => ({
  value: item.value,
  label: item.label,
})));

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
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await Promise.all([loadWarehouseTree(), loadSupplierOptions()]);
    if (!orgId) {
      itemOptions.value = [];
      return;
    }
    const itemRows = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId));
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemOptions.value = [];
    ElMessage.error('采购退货单筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const filteredRows = computed(() => {
  const documentCodeKeyword = query.documentCode.trim().toLowerCase();
  const sourceCodeKeyword = query.sourceCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  const startDate = query.returnDateRange[0];
  const endDate = query.returnDateRange[1];
  return tableData.value.filter((row) => {
    const matchedDate = (!startDate || row.returnDate >= startDate) && (!endDate || row.returnDate <= endDate);
    const matchedCode = !documentCodeKeyword || row.documentCode.toLowerCase().includes(documentCodeKeyword);
    const matchedSupplier = !query.supplier || row.supplier === query.supplier;
    const matchedWarehouse = !query.returnWarehouse || row.returnWarehouse === query.returnWarehouse;
    const matchedReason = !query.returnReason || row.returnReason === query.returnReason;
    const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedReconciliation = !query.reconciliationStatus || row.reconciliationStatus === query.reconciliationStatus;
    const matchedSplit = !query.supplierSplit || row.supplierSplit === query.supplierSplit;
    const matchedType = !query.documentType || row.documentType === query.documentType;
    const matchedItem = !query.itemCode || row.itemCount > 0;
    const matchedSource = !sourceCodeKeyword || row.sourceCode.toLowerCase().includes(sourceCodeKeyword);
    const matchedSubmitter = !query.submitter || row.submitter === query.submitter;
    const matchedPrint = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDate
      && matchedCode
      && matchedSupplier
      && matchedWarehouse
      && matchedReason
      && matchedStatus
      && matchedReconciliation
      && matchedSplit
      && matchedType
      && matchedItem
      && matchedSource
      && matchedSubmitter
      && matchedPrint
      && matchedRemark;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const formatMoney = (value: number) => value.toFixed(2);

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.returnDateRange = [];
  query.documentCode = '';
  query.supplier = '';
  query.returnWarehouse = '';
  query.returnReason = '';
  query.documentStatus = '';
  query.reconciliationStatus = '';
  query.supplierSplit = '';
  query.documentType = '';
  query.itemCode = '';
  query.sourceCode = '';
  query.submitter = '';
  query.printStatus = '全部';
  query.remark = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: PurchaseReturnRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleView = (row: PurchaseReturnRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: PurchaseReturnRow) => {
  ElMessage.info(`编辑：${row.documentCode}`);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  () => {
    void loadOptions();
  },
);

onMounted(() => {
  void loadOptions();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="退货日期">
        <el-date-picker
          v-model="query.returnDateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" clearable placeholder="请输入单据编号" style="width: 180px" />
      </el-form-item>
      <el-form-item label="供应商">
        <el-tree-select
          v-model="query.supplier"
          :data="supplierTree"
          :loading="supplierLoading"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          filterable
          check-strictly
          placeholder="请选择"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item label="退货仓库">
        <el-select
          v-model="query.returnWarehouse"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
          style="width: 180px"
        >
          <el-option
            v-for="option in warehouseOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="退货原因">
        <el-select v-model="query.returnReason" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in returnReasonOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="对账状态">
        <el-select v-model="query.reconciliationStatus" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in reconciliationStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商分账">
        <el-select v-model="query.supplierSplit" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in supplierSplitOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据类型">
        <el-select v-model="query.documentType" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in documentTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select
          v-model="query.itemCode"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
          style="width: 220px"
        >
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源单号">
        <el-input v-model="query.sourceCode" clearable placeholder="请输入来源单号" style="width: 180px" />
      </el-form-item>
      <el-form-item label="提交人">
        <el-tree-select
          v-model="query.submitter"
          :data="submitterTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="打印状态">
        <el-radio-group v-model="query.printStatus">
          <el-radio v-for="option in printStatusOptions" :key="option" :label="option" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" clearable placeholder="请输入备注" style="width: 180px" />
      </el-form-item>
      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
        </el-select>
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
      <el-button type="primary" @click="handleToolbarAction('新增')">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button @click="handleToolbarAction('按单退货')">
        <el-icon><Upload /></el-icon>
        按单退货
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量打印')">
        <el-icon><Printer /></el-icon>
        批量打印
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量提交')">批量提交</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量发货')">批量发货</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量撤销发货')">
        <el-icon><RefreshLeft /></el-icon>
        批量撤销发货
      </el-button>
    </div>

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      height="460"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="returnDate" label="退货日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="documentCode" label="单据编号" min-width="150" fixed="left" show-overflow-tooltip />
      <el-table-column prop="sourceCode" label="来源单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
      <el-table-column prop="returnWarehouse" label="退货仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="returnReason" label="退货原因" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" align="right" />
      <el-table-column prop="returnAmount" label="退货金额（含税）" min-width="150" align="right">
        <template #default="{ row }">{{ formatMoney(row.returnAmount) }}</template>
      </el-table-column>
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reconciliationStatus" label="对账状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentType" label="单据类型" min-width="110" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="submitter" label="提交人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">已选 {{ selectedIds.length }} 条</div>
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
