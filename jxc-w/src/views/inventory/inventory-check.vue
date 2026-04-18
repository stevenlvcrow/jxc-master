<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type TimeType = '盘点日期' | '创建时间';
type DocumentStatus = '草稿' | '已提交' | '已审核';
type PrintStatus = '全部' | '未打印' | '已打印';
type DiffStatus = '全部' | '盘盈' | '盘亏' | '无差异';
type CheckType = '常规盘点' | '抽盘' | '循环盘点';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type InventoryCheckRow = {
  id: number;
  documentCode: string;
  checkDate: string;
  warehouse: string;
  itemCount: number;
  bookAmount: string;
  actualAmount: string;
  diffAmount: string;
  checkType: CheckType;
  status: DocumentStatus;
  diffStatus: Exclude<DiffStatus, '全部'>;
  auditDate: string;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
};

const timeTypeOptions: TimeType[] = ['盘点日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const diffStatusOptions: DiffStatus[] = ['全部', '盘盈', '盘亏', '无差异'];
const checkTypeOptions: CheckType[] = ['常规盘点', '抽盘', '循环盘点'];
const router = useRouter();
const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const itemTree: TreeNode[] = [
  {
    value: 'food',
    label: '食品',
    children: [
      { value: '鸡胸肉', label: '鸡胸肉' },
      { value: '牛腩', label: '牛腩' },
    ],
  },
  {
    value: 'pack',
    label: '包材',
    children: [
      { value: '包装盒', label: '包装盒' },
      { value: '封口膜', label: '封口膜' },
    ],
  },
];

const query = reactive({
  timeType: '盘点日期' as TimeType,
  startDate: '',
  endDate: '',
  documentCode: '',
  warehouse: '',
  diffStatus: '全部' as DiffStatus,
  itemName: '',
  documentStatus: '',
  checkType: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
  planCode: '',
});

const tableData: InventoryCheckRow[] = [
  {
    id: 1,
    documentCode: 'PD-202604-001',
    checkDate: '2026-04-13',
    warehouse: '中央成品仓',
    itemCount: 128,
    bookAmount: '86,200.00',
    actualAmount: '86,980.00',
    diffAmount: '+780.00',
    checkType: '常规盘点',
    status: '已审核',
    diffStatus: '盘盈',
    auditDate: '2026-04-13',
    printStatus: '已打印',
    createdAt: '2026-04-13 10:18:00',
    creator: '张敏',
  },
  {
    id: 2,
    documentCode: 'PD-202604-002',
    checkDate: '2026-04-12',
    warehouse: '北区原料仓',
    itemCount: 86,
    bookAmount: '42,100.00',
    actualAmount: '41,620.00',
    diffAmount: '-480.00',
    checkType: '抽盘',
    status: '已提交',
    diffStatus: '盘亏',
    auditDate: '-',
    printStatus: '未打印',
    createdAt: '2026-04-12 16:40:00',
    creator: '李娜',
  },
  {
    id: 3,
    documentCode: 'PD-202604-003',
    checkDate: '2026-04-11',
    warehouse: '南区包材仓',
    itemCount: 42,
    bookAmount: '12,600.00',
    actualAmount: '12,600.00',
    diffAmount: '0.00',
    checkType: '循环盘点',
    status: '草稿',
    diffStatus: '无差异',
    auditDate: '-',
    printStatus: '未打印',
    createdAt: '2026-04-11 09:46:00',
    creator: '王磊',
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
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const dateField = query.timeType === '盘点日期' ? row.checkDate : row.createdAt.slice(0, 10);
    const matchedStartDate = !query.startDate || dateField >= query.startDate;
    const matchedEndDate = !query.endDate || dateField <= query.endDate;
    const matchedCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedDiffStatus = query.diffStatus === '全部' || row.diffStatus === query.diffStatus;
    const matchedItem = !query.itemName || row.documentCode.includes(query.itemName);
    const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedCheckType = !query.checkType || row.checkType === query.checkType;
    const matchedPrintStatus = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.documentCode.toLowerCase().includes(remarkKeyword);
    const matchedPlanCode = !query.planCode || row.documentCode.toLowerCase().includes(query.planCode.toLowerCase());
    return matchedStartDate
      && matchedEndDate
      && matchedCode
      && matchedWarehouse
      && matchedDiffStatus
      && matchedItem
      && matchedStatus
      && matchedCheckType
      && matchedPrintStatus
      && matchedRemark
      && matchedPlanCode;
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
  query.timeType = '盘点日期';
  query.startDate = '';
  query.endDate = '';
  query.documentCode = '';
  query.warehouse = '';
  query.diffStatus = '全部';
  query.itemName = '';
  query.documentStatus = '';
  query.checkType = '';
  query.printStatus = '全部';
  query.remark = '';
  query.planCode = '';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  if (action === '新增') {
    router.push({ name: 'InventoryCheckCreate' });
    return;
  }
  ElMessage.info(`${action}功能待接入`);
};

const handleTableSettingCommand = (command: string | number | object) => {
  ElMessage.info(`表格设置：${String(command)}`);
};

const handleSelectionChange = (rows: InventoryCheckRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: InventoryCheckRow) => {
  router.push({ name: 'InventoryCheckView', params: { id: row.id } });
};

const handleEdit = (row: InventoryCheckRow) => {
  router.push({ name: 'InventoryCheckEdit', params: { id: row.id } });
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
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
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
      <el-form-item label="盘点差异">
        <el-select v-model="query.diffStatus" style="width: 120px">
          <el-option v-for="option in diffStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-tree-select
          v-model="query.itemName"
          :data="itemTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable style="width: 120px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="盘点类型">
        <el-select v-model="query.checkType" clearable style="width: 120px">
          <el-option v-for="option in checkTypeOptions" :key="option" :label="option" :value="option" />
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
      <el-form-item label="盘点方案编号">
        <el-input v-model="query.planCode" placeholder="请输入盘点方案编号" clearable style="width: 160px" />
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
      <el-button @click="handleToolbarAction('批量导出')">批量导出</el-button>
      <el-button @click="handleToolbarAction('批量导出单据列表')">批量导出单据列表</el-button>
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
      <el-button @click="handleToolbarAction('操作日志')">操作日志</el-button>
      <el-button @click="handleToolbarAction('常见问题')">常见问题</el-button>
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
      <el-table-column prop="checkDate" label="盘点日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品数" min-width="90" show-overflow-tooltip />
      <el-table-column prop="bookAmount" label="账面金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="actualAmount" label="实盘金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="diffAmount" label="盈亏金额" min-width="110" show-overflow-tooltip />
      <el-table-column prop="checkType" label="盘点类型" min-width="110" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="diffStatus" label="盘点差异" min-width="110" show-overflow-tooltip />
      <el-table-column prop="auditDate" label="审核日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
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
