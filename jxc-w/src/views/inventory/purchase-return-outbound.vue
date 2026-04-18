<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '出库日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type ReviewStatus = '全部' | '未复审' | '已复审';
type ReconciliationStatus = '未对账' | '部分对账' | '已对账';
type InvoiceStatus = '未开票' | '部分开票' | '已开票';
type PrintStatus = '全部' | '未打印' | '已打印';
type SplitStatus = '全部' | '未分账' | '已分账';
type PurchaseReturnOutboundRow = {
  id: number;
  documentCode: string;
  outboundDate: string;
  upstreamCode: string;
  supplier: string;
  amount: string;
  status: DocumentStatus;
  reviewStatus: Exclude<ReviewStatus, '全部'>;
  reconciliationStatus: ReconciliationStatus;
  invoiceStatus: InvoiceStatus;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

const timeTypeOptions: TimeType[] = ['出库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const reviewStatusOptions: ReviewStatus[] = ['全部', '未复审', '已复审'];
const reconciliationStatusOptions: ReconciliationStatus[] = ['未对账', '部分对账', '已对账'];
const splitStatusOptions: SplitStatus[] = ['全部', '未分账', '已分账'];
const invoiceStatusOptions: InvoiceStatus[] = ['未开票', '部分开票', '已开票'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const sessionStore = useSessionStore();
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

const query = reactive({
  timeType: '出库日期' as TimeType,
  startDate: '',
  endDate: '',
  warehouse: '',
  documentCode: '',
  supplier: '',
  itemName: '',
  documentStatus: '',
  reviewStatus: '全部' as ReviewStatus,
  reconciliationStatus: '',
  splitStatus: '全部' as SplitStatus,
  upstreamCode: '',
  invoiceStatus: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: PurchaseReturnOutboundRow[] = [
  {
    id: 1,
    documentCode: 'THCK-202604-001',
    outboundDate: '2026-04-13',
    upstreamCode: 'CGTH-202604-001',
    supplier: '鲜达食品',
    amount: '6,420.00',
    status: '已审核',
    reviewStatus: '已复审',
    reconciliationStatus: '已对账',
    invoiceStatus: '已开票',
    printStatus: '已打印',
    createdAt: '2026-04-13 09:42:00',
    creator: '张敏',
    remark: '原料退货出库',
  },
  {
    id: 2,
    documentCode: 'THCK-202604-002',
    outboundDate: '2026-04-12',
    upstreamCode: 'CGTH-202604-006',
    supplier: '优选农场',
    amount: '2,860.00',
    status: '已提交',
    reviewStatus: '未复审',
    reconciliationStatus: '未对账',
    invoiceStatus: '未开票',
    printStatus: '未打印',
    createdAt: '2026-04-12 14:20:00',
    creator: '李娜',
    remark: '蔬菜质量异常退货',
  },
  {
    id: 3,
    documentCode: 'THCK-202604-003',
    outboundDate: '2026-04-11',
    upstreamCode: 'CGTH-202604-009',
    supplier: '盒马包材',
    amount: '1,180.00',
    status: '草稿',
    reviewStatus: '未复审',
    reconciliationStatus: '部分对账',
    invoiceStatus: '部分开票',
    printStatus: '未打印',
    createdAt: '2026-04-11 11:06:00',
    creator: '王磊',
    remark: '包材数量差异退回',
  },
];

onMounted(() => {
  void loadWarehouseTree();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    void loadWarehouseTree();
  },
);

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const documentCodeKeyword = query.documentCode.trim().toLowerCase();
  const upstreamCodeKeyword = query.upstreamCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();

  return tableData.filter((row) => {
    const dateField = query.timeType === '出库日期' ? row.outboundDate : row.createdAt.slice(0, 10);
    const matchedStartDate = !query.startDate || dateField >= query.startDate;
    const matchedEndDate = !query.endDate || dateField <= query.endDate;
    const matchedDocumentCode = !documentCodeKeyword || row.documentCode.toLowerCase().includes(documentCodeKeyword);
    const matchedSupplier = !query.supplier || row.supplier === query.supplier;
    const matchedItem = !query.itemName || row.remark.includes(query.itemName);
    const matchedDocumentStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedReviewStatus = query.reviewStatus === '全部' || row.reviewStatus === query.reviewStatus;
    const matchedReconciliationStatus = !query.reconciliationStatus || row.reconciliationStatus === query.reconciliationStatus;
    const matchedSplitStatus = query.splitStatus === '全部' || query.splitStatus === '未分账';
    const matchedUpstreamCode = !upstreamCodeKeyword || row.upstreamCode.toLowerCase().includes(upstreamCodeKeyword);
    const matchedInvoiceStatus = !query.invoiceStatus || row.invoiceStatus === query.invoiceStatus;
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);

    return matchedStartDate
      && matchedEndDate
      && matchedDocumentCode
      && matchedSupplier
      && matchedItem
      && matchedDocumentStatus
      && matchedReviewStatus
      && matchedReconciliationStatus
      && matchedSplitStatus
      && matchedUpstreamCode
      && matchedInvoiceStatus
      && matchedPrintStatus
      && matchedRemark;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.timeType = '出库日期';
  query.startDate = '';
  query.endDate = '';
  query.warehouse = '';
  query.documentCode = '';
  query.supplier = '';
  query.itemName = '';
  query.documentStatus = '';
  query.reviewStatus = '全部';
  query.reconciliationStatus = '';
  query.splitStatus = '全部';
  query.upstreamCode = '';
  query.invoiceStatus = '';
  query.printStatus = '全部';
  query.remark = '';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleTableSettingCommand = (command: string | number | object) => {
  ElMessage.info(`表格设置：${String(command)}`);
};

const handleSelectionChange = (rows: PurchaseReturnOutboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: PurchaseReturnOutboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: PurchaseReturnOutboundRow) => {
  ElMessage.info(`编辑：${row.documentCode}`);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};
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
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="query.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker
          v-model="query.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束日期"
          style="width: 160px"
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
      <el-form-item label="复审状态">
        <el-select v-model="query.reviewStatus" style="width: 120px">
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
        <el-select v-model="query.splitStatus" style="width: 120px">
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
      <el-button type="primary" @click="handleToolbarAction('新增')">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button @click="handleToolbarAction('批量打印')">
        <el-icon><Printer /></el-icon>
        批量打印
      </el-button>
      <el-button @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleToolbarAction('批量审核')">批量审核</el-button>
      <el-button @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
      <el-button @click="handleToolbarAction('批量复审')">批量复审</el-button>
      <el-button @click="handleToolbarAction('批量取消复审')">批量取消复审</el-button>
      <el-dropdown @command="handleTableSettingCommand">
        <el-button>
          表格设置
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="column">列显示设置</el-dropdown-item>
            <el-dropdown-item command="density">表格密度</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-table
      :data="pagedRows"
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
      <el-table-column prop="outboundDate" label="出库日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="upstreamCode" label="上游单据号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="复审状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reconciliationStatus" label="对账状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="invoiceStatus" label="发票状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text @click="handleEdit(row)">编辑</el-button>
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
