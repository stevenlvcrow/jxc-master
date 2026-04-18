<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '入库日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type InvoiceStatus = '未开票' | '部分开票' | '已开票';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type CustomerReturnInboundRow = {
  id: number;
  documentCode: string;
  inboundDate: string;
  customerName: string;
  warehouse: string;
  amount: string;
  status: DocumentStatus;
  invoiceStatus: InvoiceStatus;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['入库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const invoiceStatusOptions: InvoiceStatus[] = ['未开票', '部分开票', '已开票'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();

const customerTree: TreeNode[] = [
  {
    value: 'customer-root',
    label: '客户分组',
    children: [
      { value: '盒马便利', label: '盒马便利' },
      { value: '本地商超', label: '本地商超' },
      { value: '企业团餐', label: '企业团餐' },
    ],
  },
];
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const salesOptions = ['张敏', '李娜', '王磊'];

const query = reactive({
  timeType: '入库日期' as TimeType,
  startDate: '',
  endDate: '',
  customer: '',
  warehouse: '',
  documentCode: '',
  printStatus: '全部' as PrintStatus,
  invoiceStatus: '',
  documentStatus: '',
  salesPerson: '',
  itemName: '',
  remark: '',
});

const tableData: CustomerReturnInboundRow[] = [
  {
    id: 1,
    documentCode: 'KHTH-202604-001',
    inboundDate: '2026-04-13',
    customerName: '盒马便利',
    warehouse: '中央成品仓',
    amount: '3,280.00',
    status: '已审核',
    invoiceStatus: '已开票',
    printStatus: '已打印',
    createdAt: '2026-04-13 09:58:00',
    creator: '张敏',
    remark: '客户退货入库',
  },
  {
    id: 2,
    documentCode: 'KHTH-202604-002',
    inboundDate: '2026-04-12',
    customerName: '本地商超',
    warehouse: '北区原料仓',
    amount: '1,460.00',
    status: '已提交',
    invoiceStatus: '部分开票',
    printStatus: '未打印',
    createdAt: '2026-04-12 16:10:00',
    creator: '李娜',
    remark: '退货补录',
  },
  {
    id: 3,
    documentCode: 'KHTH-202604-003',
    inboundDate: '2026-04-11',
    customerName: '企业团餐',
    warehouse: '南区包材仓',
    amount: '680.00',
    status: '草稿',
    invoiceStatus: '未开票',
    printStatus: '未打印',
    createdAt: '2026-04-11 10:22:00',
    creator: '王磊',
    remark: '团餐退货',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const codeKeyword = query.documentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '入库日期' ? row.inboundDate : row.createdAt.slice(0, 10);
    const matchedStartDate = !query.startDate || dateField >= query.startDate;
    const matchedEndDate = !query.endDate || dateField <= query.endDate;
    const matchedCustomer = !query.customer || row.customerName === query.customer;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedInvoiceStatus = !query.invoiceStatus || row.invoiceStatus === query.invoiceStatus;
    const matchedDocumentStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedSales = !query.salesPerson || row.creator === query.salesPerson;
    const matchedItem = !query.itemName || row.remark.includes(query.itemName);
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedStartDate
      && matchedEndDate
      && matchedCustomer
      && matchedWarehouse
      && matchedDocumentCode
      && matchedPrintStatus
      && matchedInvoiceStatus
      && matchedDocumentStatus
      && matchedSales
      && matchedItem
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
  query.timeType = '入库日期';
  query.startDate = '';
  query.endDate = '';
  query.customer = '';
  query.warehouse = '';
  query.documentCode = '';
  query.printStatus = '全部';
  query.invoiceStatus = '';
  query.documentStatus = '';
  query.salesPerson = '';
  query.itemName = '';
  query.remark = '';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleTableSettingCommand = (command: string | number | object) => {
  ElMessage.info(`表格设置：${String(command)}`);
};

const handleSelectionChange = (rows: CustomerReturnInboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: CustomerReturnInboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: CustomerReturnInboundRow) => {
  ElMessage.info(`编辑：${row.documentCode}`);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

onMounted(loadWarehouseTree);

watch(
  () => sessionStore.currentOrgId,
  () => {
    loadWarehouseTree();
  },
);
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="时间类型">
        <el-select v-model="query.timeType" style="width: 120px">
          <el-option v-for="option in timeTypeOptions" :key="option" :label="option" :value="option" />
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
      <el-form-item label="客户名称">
        <el-tree-select
          v-model="query.customer"
          :data="customerTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="入库仓库">
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
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="打印状态">
        <el-select v-model="query.printStatus" style="width: 120px">
          <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="发票状态">
        <el-select v-model="query.invoiceStatus" clearable style="width: 120px">
          <el-option v-for="option in invoiceStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable style="width: 120px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务员">
        <el-select v-model="query.salesPerson" clearable style="width: 120px">
          <el-option v-for="option in salesOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemName" clearable style="width: 120px">
          <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
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
      <el-table-column prop="inboundDate" label="入库日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
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
