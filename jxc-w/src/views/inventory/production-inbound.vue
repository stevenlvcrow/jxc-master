<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, ArrowUp, Delete, Plus, Printer, RefreshRight, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '入库日期' | '创建时间';
type DocumentStatus = '' | '草稿' | '已提交' | '已审核';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type ProductionInboundRow = {
  id: number;
  documentCode: string;
  inboundDate: string;
  upstreamCode: string;
  workshop: string;
  warehouse: string;
  amount: string;
  status: Exclude<DocumentStatus, ''>;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['入库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['', '草稿', '已提交', '已审核'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const productOptions = ['半成品酱汁', '预制牛腩', '调味包', '饮品基底'];
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const workshopTree: TreeNode[] = [
  {
    value: 'workshop-root',
    label: '加工间',
    children: [
      { value: '中央加工间', label: '中央加工间' },
      { value: '热菜加工间', label: '热菜加工间' },
      { value: '冷菜加工间', label: '冷菜加工间' },
    ],
  },
];
const query = reactive({
  timeType: '入库日期' as TimeType,
  date: '',
  workshop: '',
  warehouse: '',
  documentCode: '',
  productName: '',
  documentStatus: '' as DocumentStatus,
  upstreamCode: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: ProductionInboundRow[] = [
  {
    id: 1,
    documentCode: 'SCRK-202604-001',
    inboundDate: '2026-04-13',
    upstreamCode: 'SC-202604-011',
    workshop: '中央加工间',
    warehouse: '中央成品仓',
    amount: '6,820.00',
    status: '已审核',
    printStatus: '已打印',
    createdAt: '2026-04-13 09:46:00',
    creator: '张敏',
    remark: '中央加工批次入库',
  },
  {
    id: 2,
    documentCode: 'SCRK-202604-002',
    inboundDate: '2026-04-12',
    upstreamCode: 'SC-202604-013',
    workshop: '热菜加工间',
    warehouse: '北区原料仓',
    amount: '2,480.00',
    status: '已提交',
    printStatus: '未打印',
    createdAt: '2026-04-12 15:30:00',
    creator: '李娜',
    remark: '热菜半成品入库',
  },
  {
    id: 3,
    documentCode: 'SCRK-202604-003',
    inboundDate: '2026-04-11',
    upstreamCode: 'SC-202604-016',
    workshop: '冷菜加工间',
    warehouse: '南区包材仓',
    amount: '1,360.00',
    status: '草稿',
    printStatus: '未打印',
    createdAt: '2026-04-11 10:12:00',
    creator: '王磊',
    remark: '冷菜加工入库',
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

const filtersCollapsed = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const codeKeyword = query.documentCode.trim().toLowerCase();
  const upstreamKeyword = query.upstreamCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '入库日期' ? row.inboundDate : row.createdAt.slice(0, 10);
    const matchedDate = !query.date || dateField === query.date;
    const matchedWorkshop = !query.workshop || row.workshop === query.workshop;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedProduct = !query.productName || row.remark.includes(query.productName);
    const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedUpstream = !upstreamKeyword || row.upstreamCode.toLowerCase().includes(upstreamKeyword);
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDate
      && matchedWorkshop
      && matchedWarehouse
      && matchedDocumentCode
      && matchedProduct
      && matchedStatus
      && matchedUpstream
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
  query.timeType = '入库日期';
  query.date = '';
  query.workshop = '';
  query.warehouse = '';
  query.documentCode = '';
  query.productName = '';
  query.documentStatus = '';
  query.upstreamCode = '';
  query.printStatus = '全部';
  query.remark = '';
  filtersCollapsed.value = true;
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: ProductionInboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: ProductionInboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: ProductionInboundRow) => {
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
      <el-form-item label="日期">
        <el-date-picker
          v-model="query.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="加工间">
        <el-tree-select
          v-model="query.workshop"
          :data="workshopTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
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
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <template v-if="!filtersCollapsed">
        <el-form-item label="加工品">
          <el-select v-model="query.productName" clearable style="width: 140px">
            <el-option v-for="option in productOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="单据状态">
          <el-select v-model="query.documentStatus" clearable placeholder="请选择单据状态" style="width: 140px">
            <el-option v-for="option in documentStatusOptions" :key="option || 'empty'" :label="option || '请选择单据状态'" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="上游单据号">
          <el-input v-model="query.upstreamCode" placeholder="请输入上游单据号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="打印状态">
          <el-select v-model="query.printStatus" style="width: 120px">
            <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="query.remark" placeholder="请输入备注" clearable style="width: 160px" />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button text type="primary" @click="filtersCollapsed = !filtersCollapsed">
          <el-icon>
            <ArrowDown v-if="filtersCollapsed" />
            <ArrowUp v-else />
          </el-icon>
          {{ filtersCollapsed ? '展开筛选' : '收起筛选' }}
        </el-button>
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
      <el-button @click="handleToolbarAction('标签打印')">
        <el-icon><Tickets /></el-icon>
        标签打印
      </el-button>
      <el-button @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleToolbarAction('批量审核')">批量审核</el-button>
      <el-button @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
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
      <el-table-column prop="upstreamCode" label="上游单据号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="workshop" label="加工间" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
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
