<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '单据日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type ProfitInboundRow = {
  id: number;
  documentCode: string;
  sourceCode: string;
  documentDate: string;
  warehouse: string;
  itemCount: number;
  status: DocumentStatus;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};

const timeTypeOptions: TimeType[] = ['单据日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const query = reactive({
  timeType: '单据日期' as TimeType,
  startDate: '',
  endDate: '',
  warehouse: '',
  documentCode: '',
  sourceCode: '',
  itemName: '',
  documentStatus: '',
  thirdPartyCode: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const tableData: ProfitInboundRow[] = [
  {
    id: 1,
    documentCode: 'PY-202604-001',
    sourceCode: 'PD-202604-021',
    documentDate: '2026-04-13',
    warehouse: '中央成品仓',
    itemCount: 12,
    status: '已审核',
    printStatus: '已打印',
    createdAt: '2026-04-13 11:20:00',
    creator: '张敏',
    remark: '盘盈转入',
  },
  {
    id: 2,
    documentCode: 'PY-202604-002',
    sourceCode: 'PD-202604-022',
    documentDate: '2026-04-12',
    warehouse: '北区原料仓',
    itemCount: 6,
    status: '已提交',
    printStatus: '未打印',
    createdAt: '2026-04-12 16:32:00',
    creator: '李娜',
    remark: '',
  },
  {
    id: 3,
    documentCode: 'PY-202604-003',
    sourceCode: 'PD-202604-023',
    documentDate: '2026-04-11',
    warehouse: '南区包材仓',
    itemCount: 9,
    status: '草稿',
    printStatus: '未打印',
    createdAt: '2026-04-11 09:18:00',
    creator: '王磊',
    remark: '盘盈补录',
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
  const sourceKeyword = query.sourceCode.trim().toLowerCase();
  const thirdPartyKeyword = query.thirdPartyCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '单据日期' ? row.documentDate : row.createdAt.slice(0, 10);
    const matchedStartDate = !query.startDate || dateField >= query.startDate;
    const matchedEndDate = !query.endDate || dateField <= query.endDate;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedSource = !sourceKeyword || row.sourceCode.toLowerCase().includes(sourceKeyword);
    const matchedItem = !query.itemName || row.documentCode.includes(query.itemName);
    const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedThirdParty = !thirdPartyKeyword || row.documentCode.toLowerCase().includes(thirdPartyKeyword);
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedStartDate
      && matchedEndDate
      && matchedWarehouse
      && matchedCode
      && matchedSource
      && matchedItem
      && matchedStatus
      && matchedThirdParty
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
  query.timeType = '单据日期';
  query.startDate = '';
  query.endDate = '';
  query.warehouse = '';
  query.documentCode = '';
  query.sourceCode = '';
  query.itemName = '';
  query.documentStatus = '';
  query.thirdPartyCode = '';
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

const handleSelectionChange = (rows: ProfitInboundRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: ProfitInboundRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: ProfitInboundRow) => {
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
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="来源单号">
        <el-input v-model="query.sourceCode" placeholder="请输入来源单号" clearable style="width: 160px" />
      </el-form-item>
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
      <el-form-item label="第三方单据号">
        <el-input v-model="query.thirdPartyCode" placeholder="请输入第三方单据号" clearable style="width: 160px" />
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
      <el-dropdown @command="handleTableSettingCommand">
        <el-button>
          表格设置
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="单据编号">单据编号</el-dropdown-item>
            <el-dropdown-item command="来源单号">来源单号</el-dropdown-item>
            <el-dropdown-item command="单据日期">单据日期</el-dropdown-item>
            <el-dropdown-item command="仓库">仓库</el-dropdown-item>
            <el-dropdown-item command="物品（项）">物品（项）</el-dropdown-item>
            <el-dropdown-item command="状态">状态</el-dropdown-item>
            <el-dropdown-item command="打印状态">打印状态</el-dropdown-item>
            <el-dropdown-item command="创建时间">创建时间</el-dropdown-item>
            <el-dropdown-item command="创建人">创建人</el-dropdown-item>
            <el-dropdown-item command="备注">备注</el-dropdown-item>
            <el-dropdown-item command="操作">操作</el-dropdown-item>
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
      <el-table-column prop="sourceCode" label="来源单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="documentDate" label="单据日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
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
