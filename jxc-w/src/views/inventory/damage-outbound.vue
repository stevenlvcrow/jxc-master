<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ArrowDown, ArrowUp, Delete, Plus, Printer, RefreshRight, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type TimeType = '出库日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type DamageOutboundRow = {
  id: number;
  documentCode: string;
  outboundDate: string;
  upstreamCode: string;
  orgCode: string;
  warehouse: string;
  amount: string;
  status: DocumentStatus;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['出库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const damageReasonOptions = ['破损', '过期', '变质', '盘点差异'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const warehouseTree: TreeNode[] = [
  {
    value: 'warehouse-root',
    label: '仓库中心',
    children: [
      { value: '中央成品仓', label: '中央成品仓' },
      { value: '北区原料仓', label: '北区原料仓' },
      { value: '南区包材仓', label: '南区包材仓' },
    ],
  },
];

const query = reactive({
  timeType: '出库日期' as TimeType,
  date: '',
  warehouse: '',
  documentCode: '',
  itemName: '',
  documentStatus: '',
  damageReason: '',
  upstreamCode: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: DamageOutboundRow[] = [
  {
    id: 1,
    documentCode: 'BSCK-202604-001',
    outboundDate: '2026-04-13',
    upstreamCode: 'PD-202604-001',
    orgCode: 'ORG-1001',
    warehouse: '北区原料仓',
    amount: '1,820.00',
    status: '已审核',
    printStatus: '已打印',
    createdAt: '2026-04-13 10:10:00',
    creator: '张敏',
    remark: '原料破损报损',
  },
  {
    id: 2,
    documentCode: 'BSCK-202604-002',
    outboundDate: '2026-04-12',
    upstreamCode: 'PD-202604-004',
    orgCode: 'ORG-1001',
    warehouse: '中央成品仓',
    amount: '920.00',
    status: '已提交',
    printStatus: '未打印',
    createdAt: '2026-04-12 16:04:00',
    creator: '李娜',
    remark: '过期处理',
  },
  {
    id: 3,
    documentCode: 'BSCK-202604-003',
    outboundDate: '2026-04-11',
    upstreamCode: 'PD-202604-006',
    orgCode: 'ORG-1002',
    warehouse: '南区包材仓',
    amount: '460.00',
    status: '草稿',
    printStatus: '未打印',
    createdAt: '2026-04-11 09:40:00',
    creator: '王磊',
    remark: '包材破损',
  },
];

const filtersCollapsed = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const codeKeyword = query.documentCode.trim().toLowerCase();
  const upstreamKeyword = query.upstreamCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '出库日期' ? row.outboundDate : row.createdAt.slice(0, 10);
    const matchedDate = !query.date || dateField === query.date;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedItem = !query.itemName || row.remark.includes(query.itemName);
    const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedReason = !query.damageReason || row.remark.includes(query.damageReason);
    const matchedUpstream = !upstreamKeyword || row.upstreamCode.toLowerCase().includes(upstreamKeyword);
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDate
      && matchedWarehouse
      && matchedDocumentCode
      && matchedItem
      && matchedStatus
      && matchedReason
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
  query.timeType = '出库日期';
  query.date = '';
  query.warehouse = '';
  query.documentCode = '';
  query.itemName = '';
  query.documentStatus = '';
  query.damageReason = '';
  query.upstreamCode = '';
  query.printStatus = '全部';
  query.remark = '';
  filtersCollapsed.value = true;
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleExportCommand = (command: string | number | object) => {
  ElMessage.info(`导出：${String(command)}`);
};

const handleSelectionChange = (rows: DamageOutboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: DamageOutboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: DamageOutboundRow) => {
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
        <el-form-item label="报损原因">
          <el-select v-model="query.damageReason" clearable style="width: 120px">
            <el-option v-for="option in damageReasonOptions" :key="option" :label="option" :value="option" />
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
      <el-button @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleToolbarAction('批量提交')">批量提交</el-button>
      <el-button @click="handleToolbarAction('批量审核')">批量审核</el-button>
      <el-button @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
      <el-dropdown @command="handleExportCommand">
        <el-button>
          批量导出
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="list">导出单据列表</el-dropdown-item>
            <el-dropdown-item command="detail">导出明细</el-dropdown-item>
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
      <el-table-column prop="orgCode" label="机构编码" min-width="120" show-overflow-tooltip />
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
