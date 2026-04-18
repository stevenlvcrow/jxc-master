<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, ArrowUp, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '入库日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type ReviewStatus = '全部' | '未复审' | '已复审';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type OtherInboundRow = {
  id: number;
  documentCode: string;
  inboundDate: string;
  warehouse: string;
  inboundReason: string;
  amount: string;
  status: DocumentStatus;
  reviewStatus: Exclude<ReviewStatus, '全部'>;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['入库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const reviewStatusOptions: ReviewStatus[] = ['全部', '未复审', '已复审'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const inboundReasonTree: TreeNode[] = [
  {
    value: 'reason-root',
    label: '入库原因',
    children: [
      { value: '盘盈入库', label: '盘盈入库' },
      { value: '盘点调整', label: '盘点调整' },
      { value: '业务补入', label: '业务补入' },
      { value: '供应商补料', label: '供应商补料' },
    ],
  },
];

const query = reactive({
  timeType: '入库日期' as TimeType,
  date: '',
  warehouse: '',
  documentCode: '',
  itemName: '',
  documentStatus: '',
  reviewStatus: '全部' as ReviewStatus,
  inboundReason: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: OtherInboundRow[] = [
  {
    id: 1,
    documentCode: 'QTRK-202604-001',
    inboundDate: '2026-04-13',
    warehouse: '北区原料仓',
    inboundReason: '盘盈入库',
    amount: '1,520.00',
    status: '已审核',
    reviewStatus: '已复审',
    printStatus: '已打印',
    createdAt: '2026-04-13 11:10:00',
    creator: '张敏',
    remark: '盘点盈余补入',
  },
  {
    id: 2,
    documentCode: 'QTRK-202604-002',
    inboundDate: '2026-04-12',
    warehouse: '中央成品仓',
    inboundReason: '业务补入',
    amount: '860.00',
    status: '已提交',
    reviewStatus: '未复审',
    printStatus: '未打印',
    createdAt: '2026-04-12 16:18:00',
    creator: '李娜',
    remark: '业务补入入库',
  },
  {
    id: 3,
    documentCode: 'QTRK-202604-003',
    inboundDate: '2026-04-11',
    warehouse: '南区包材仓',
    inboundReason: '供应商补料',
    amount: '420.00',
    status: '草稿',
    reviewStatus: '未复审',
    printStatus: '未打印',
    createdAt: '2026-04-11 09:44:00',
    creator: '王磊',
    remark: '补料入库',
  },
];

const filtersCollapsed = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const codeKeyword = query.documentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '入库日期' ? row.inboundDate : row.createdAt.slice(0, 10);
    const matchedDate = !query.date || dateField === query.date;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedItem = !query.itemName || row.remark.includes(query.itemName);
    const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedReviewStatus = query.reviewStatus === '全部' || row.reviewStatus === query.reviewStatus;
    const matchedReason = !query.inboundReason || row.inboundReason === query.inboundReason;
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDate
      && matchedWarehouse
      && matchedDocumentCode
      && matchedItem
      && matchedStatus
      && matchedReviewStatus
      && matchedReason
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
  query.warehouse = '';
  query.documentCode = '';
  query.itemName = '';
  query.documentStatus = '';
  query.reviewStatus = '全部';
  query.inboundReason = '';
  query.printStatus = '全部';
  query.remark = '';
  filtersCollapsed.value = true;
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: OtherInboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: OtherInboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: OtherInboundRow) => {
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
      <el-form-item label="日期">
        <el-date-picker
          v-model="query.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择日期"
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
        <el-form-item label="物品">
          <el-select v-model="query.itemName" clearable style="width: 120px">
            <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="单据状态">
          <el-select v-model="query.documentStatus" clearable style="width: 120px">
            <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="复审状态">
          <el-select v-model="query.reviewStatus" style="width: 120px">
            <el-option v-for="option in reviewStatusOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库原因">
          <el-tree-select
            v-model="query.inboundReason"
            :data="inboundReasonTree"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            clearable
            check-strictly
            default-expand-all
            style="width: 160px"
          />
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
      <el-button @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleToolbarAction('批量提交')">批量提交</el-button>
      <el-button @click="handleToolbarAction('批量审核')">批量审核</el-button>
      <el-button @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
      <el-button @click="handleToolbarAction('批量复审')">批量复审</el-button>
      <el-button @click="handleToolbarAction('批量取消复审')">批量取消复审</el-button>
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
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="inboundReason" label="入库原因" min-width="140" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="复审状态" min-width="100" show-overflow-tooltip />
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
