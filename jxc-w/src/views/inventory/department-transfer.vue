<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ArrowDown, ArrowUp, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type TimeType = '调拨日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type ReviewStatus = '全部' | '未复审' | '已复审';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type DepartmentTransferRow = {
  id: number;
  documentCode: string;
  transferDate: string;
  outboundDepartment: string;
  inboundDepartment: string;
  amount: string;
  documentStatus: DocumentStatus;
  reviewStatus: Exclude<ReviewStatus, '全部'>;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['调拨日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const reviewStatusOptions: ReviewStatus[] = ['全部', '未复审', '已复审'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];

const departmentTree: TreeNode[] = [
  {
    value: 'dept-root',
    label: '部门中心',
    children: [
      { value: '中央厨房', label: '中央厨房' },
      { value: '热菜档口', label: '热菜档口' },
      { value: '饮品吧台', label: '饮品吧台' },
      { value: '冷菜间', label: '冷菜间' },
    ],
  },
];

const query = reactive({
  timeType: '调拨日期' as TimeType,
  dateRange: [] as string[],
  outboundDepartment: '',
  inboundDepartment: '',
  documentCode: '',
  itemName: '',
  documentStatus: '',
  reviewStatus: '全部' as ReviewStatus,
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: DepartmentTransferRow[] = [
  {
    id: 1,
    documentCode: 'BMDB-202604-001',
    transferDate: '2026-04-13',
    outboundDepartment: '中央厨房',
    inboundDepartment: '热菜档口',
    amount: '3,860.00',
    documentStatus: '已审核',
    reviewStatus: '已复审',
    printStatus: '已打印',
    createdAt: '2026-04-13 09:18:00',
    creator: '张敏',
    remark: '午市菜品调拨',
  },
  {
    id: 2,
    documentCode: 'BMDB-202604-002',
    transferDate: '2026-04-12',
    outboundDepartment: '饮品吧台',
    inboundDepartment: '冷菜间',
    amount: '1,260.00',
    documentStatus: '已提交',
    reviewStatus: '未复审',
    printStatus: '未打印',
    createdAt: '2026-04-12 15:42:00',
    creator: '李娜',
    remark: '联动活动调拨',
  },
  {
    id: 3,
    documentCode: 'BMDB-202604-003',
    transferDate: '2026-04-11',
    outboundDepartment: '热菜档口',
    inboundDepartment: '中央厨房',
    amount: '920.00',
    documentStatus: '草稿',
    reviewStatus: '未复审',
    printStatus: '未打印',
    createdAt: '2026-04-11 10:36:00',
    creator: '王磊',
    remark: '物料回调',
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
    const dateField = query.timeType === '调拨日期' ? row.transferDate : row.createdAt.slice(0, 10);
    const matchedDateRange = query.dateRange.length !== 2 || (dateField >= query.dateRange[0] && dateField <= query.dateRange[1]);
    const matchedOutboundDepartment = !query.outboundDepartment || row.outboundDepartment === query.outboundDepartment;
    const matchedInboundDepartment = !query.inboundDepartment || row.inboundDepartment === query.inboundDepartment;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedItem = !query.itemName || row.remark.includes(query.itemName);
    const matchedDocumentStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedReviewStatus = query.reviewStatus === '全部' || row.reviewStatus === query.reviewStatus;
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDateRange
      && matchedOutboundDepartment
      && matchedInboundDepartment
      && matchedDocumentCode
      && matchedItem
      && matchedDocumentStatus
      && matchedReviewStatus
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
  query.timeType = '调拨日期';
  query.dateRange = [];
  query.outboundDepartment = '';
  query.inboundDepartment = '';
  query.documentCode = '';
  query.itemName = '';
  query.documentStatus = '';
  query.reviewStatus = '全部';
  query.printStatus = '全部';
  query.remark = '';
  filtersCollapsed.value = true;
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: DepartmentTransferRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: DepartmentTransferRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: DepartmentTransferRow) => {
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
      <el-form-item label="出库部门">
        <el-tree-select
          v-model="query.outboundDepartment"
          :data="departmentTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="入库部门">
        <el-tree-select
          v-model="query.inboundDepartment"
          :data="departmentTree"
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
      <el-table-column prop="transferDate" label="调拨日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="outboundDepartment" label="出库部门" min-width="120" show-overflow-tooltip />
      <el-table-column prop="inboundDepartment" label="入库部门" min-width="120" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
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
