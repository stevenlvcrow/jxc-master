<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '出库日期' | '创建时间';
type DocumentStatus = '全部' | '草稿' | '已提交' | '已审核';
type TransferType = '全部' | '普通移库' | '退库移库' | '紧急调拨';
type PrintStatus = '全部' | '未打印' | '已打印';
type TransferOutboundRow = {
  id: number;
  documentCode: string;
  outboundDate: string;
  upstreamCode: string;
  outboundWarehouse: string;
  inboundWarehouse: string;
  amount: string;
  documentStatus: Exclude<DocumentStatus, '全部'>;
  transferType: Exclude<TransferType, '全部'>;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['出库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['全部', '草稿', '已提交', '已审核'];
const transferTypeOptions: TransferType[] = ['全部', '普通移库', '退库移库', '紧急调拨'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const query = reactive({
  timeType: '出库日期' as TimeType,
  dateRange: [] as string[],
  outboundWarehouse: '',
  inboundWarehouse: '',
  documentCode: '',
  upstreamCode: '',
  itemName: '',
  documentStatus: '全部' as DocumentStatus,
  transferType: '全部' as TransferType,
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: TransferOutboundRow[] = [
  {
    id: 1,
    documentCode: 'YKCK-202604-001',
    outboundDate: '2026-04-13',
    upstreamCode: 'YK-202604-001',
    outboundWarehouse: '北区原料仓',
    inboundWarehouse: '中央成品仓',
    amount: '8,620.00',
    documentStatus: '已审核',
    transferType: '普通移库',
    printStatus: '已打印',
    createdAt: '2026-04-13 09:10:00',
    creator: '张敏',
    remark: '中央厨房补货出库',
  },
  {
    id: 2,
    documentCode: 'YKCK-202604-002',
    outboundDate: '2026-04-12',
    upstreamCode: 'YK-202604-002',
    outboundWarehouse: '南区包材仓',
    inboundWarehouse: '东区备货仓',
    amount: '2,480.00',
    documentStatus: '已提交',
    transferType: '紧急调拨',
    printStatus: '未打印',
    createdAt: '2026-04-12 14:52:00',
    creator: '李娜',
    remark: '外卖高峰备货出库',
  },
  {
    id: 3,
    documentCode: 'YKCK-202604-003',
    outboundDate: '2026-04-11',
    upstreamCode: 'YK-202604-003',
    outboundWarehouse: '中央成品仓',
    inboundWarehouse: '北区原料仓',
    amount: '1,960.00',
    documentStatus: '草稿',
    transferType: '退库移库',
    printStatus: '未打印',
    createdAt: '2026-04-11 10:04:00',
    creator: '王磊',
    remark: '半成品回库出库',
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
  const codeKeyword = query.documentCode.trim().toLowerCase();
  const upstreamKeyword = query.upstreamCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '出库日期' ? row.outboundDate : row.createdAt.slice(0, 10);
    const matchedDateRange = query.dateRange.length !== 2
      || (dateField >= query.dateRange[0] && dateField <= query.dateRange[1]);
    const matchedOutboundWarehouse = !query.outboundWarehouse || row.outboundWarehouse === query.outboundWarehouse;
    const matchedInboundWarehouse = !query.inboundWarehouse || row.inboundWarehouse === query.inboundWarehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedUpstreamCode = !upstreamKeyword || row.upstreamCode.toLowerCase().includes(upstreamKeyword);
    const matchedItem = !query.itemName || row.remark.includes(query.itemName);
    const matchedStatus = query.documentStatus === '全部' || row.documentStatus === query.documentStatus;
    const matchedTransferType = query.transferType === '全部' || row.transferType === query.transferType;
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDateRange
      && matchedOutboundWarehouse
      && matchedInboundWarehouse
      && matchedDocumentCode
      && matchedUpstreamCode
      && matchedItem
      && matchedStatus
      && matchedTransferType
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
  query.dateRange = [];
  query.outboundWarehouse = '';
  query.inboundWarehouse = '';
  query.documentCode = '';
  query.upstreamCode = '';
  query.itemName = '';
  query.documentStatus = '全部';
  query.transferType = '全部';
  query.printStatus = '全部';
  query.remark = '';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handlePrintCommand = (command: string | number | object) => {
  ElMessage.info(`打印设置：${String(command)}`);
};

const handleTableSettingCommand = (command: string | number | object) => {
  ElMessage.info(`表格设置：${String(command)}`);
};

const handleSelectionChange = (rows: TransferOutboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: TransferOutboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: TransferOutboundRow) => {
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
          <el-option v-for="option in timeTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
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
      <el-form-item label="出库仓库">
        <el-tree-select
          v-model="query.outboundWarehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="入库仓库">
        <el-tree-select
          v-model="query.inboundWarehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="上游单据号">
        <el-input v-model="query.upstreamCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemName" clearable placeholder="请选择" style="width: 120px">
          <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" style="width: 120px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="移库类型">
        <el-select v-model="query.transferType" style="width: 120px">
          <el-option v-for="option in transferTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="打印状态">
        <el-select v-model="query.printStatus" style="width: 120px">
          <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
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
      <el-button @click="handleToolbarAction('批量打印')">
        <el-icon><Printer /></el-icon>
        批量打印
      </el-button>
      <el-dropdown @command="handlePrintCommand">
        <el-button>
          打印设置
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="template">模板设置</el-dropdown-item>
            <el-dropdown-item command="device">打印机设置</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
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
      <el-table-column prop="outboundWarehouse" label="出库仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="inboundWarehouse" label="入库仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="transferType" label="移库类型" min-width="100" show-overflow-tooltip />
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
