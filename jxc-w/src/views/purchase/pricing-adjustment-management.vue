<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ArrowDown, Delete, Plus, RefreshRight, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type AdjustmentStatus = '草稿' | '已提交' | '已审核';
type AdjustmentResult = '成功' | '失败' | '处理中';
type AdjustmentRow = {
  id: number;
  adjustmentCode: string;
  adjustmentReason: string;
  documentStatus: AdjustmentStatus;
  adjustmentResult: AdjustmentResult;
  documentDate: string;
  remark: string;
  creator: string;
  createdAt: string;
};
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

const statusTree: TreeNode[] = [
  { value: '草稿', label: '草稿' },
  { value: '已提交', label: '已提交' },
  { value: '已审核', label: '已审核' },
];
const resultTree: TreeNode[] = [
  { value: '成功', label: '成功' },
  { value: '失败', label: '失败' },
  { value: '处理中', label: '处理中' },
];
const supplierTree: TreeNode[] = [
  {
    value: 'group-a',
    label: '华东供应商组',
    children: [
      { value: '鲜达食品', label: '鲜达食品' },
      { value: '优选农场', label: '优选农场' },
    ],
  },
];
const reasonTree: TreeNode[] = [
  { value: '合同调价', label: '合同调价' },
  { value: '市场波动', label: '市场波动' },
  { value: '活动临调', label: '活动临调' },
];
const itemTree: TreeNode[] = [
  {
    value: '原料',
    label: '原料',
    children: [
      { value: '鸡胸肉', label: '鸡胸肉' },
      { value: '牛腩', label: '牛腩' },
    ],
  },
  {
    value: '饮品',
    label: '饮品',
    children: [
      { value: '酸梅汤', label: '酸梅汤' },
    ],
  },
];
const creatorTree: TreeNode[] = [
  {
    value: 'pricing-team',
    label: '定价组',
    children: [
      { value: '张敏', label: '张敏' },
      { value: '李娜', label: '李娜' },
    ],
  },
];

const query = reactive({
  documentDate: '',
  adjustmentCode: '',
  documentStatus: '',
  adjustmentResult: '',
  remark: '',
  supplier: '',
  adjustmentReason: '',
  itemName: '',
  creator: '',
});

const tableData: AdjustmentRow[] = [
  {
    id: 1,
    adjustmentCode: 'ADJ-202604-001',
    adjustmentReason: '合同调价',
    documentStatus: '已审核',
    adjustmentResult: '成功',
    documentDate: '2026-04-13',
    remark: '四月统配价格调整',
    creator: '张敏',
    createdAt: '2026-04-13 10:22:00',
  },
  {
    id: 2,
    adjustmentCode: 'ADJ-202604-002',
    adjustmentReason: '市场波动',
    documentStatus: '已提交',
    adjustmentResult: '处理中',
    documentDate: '2026-04-12',
    remark: '牛肉原料上浮',
    creator: '李娜',
    createdAt: '2026-04-12 14:18:00',
  },
  {
    id: 3,
    adjustmentCode: 'ADJ-202604-003',
    adjustmentReason: '活动临调',
    documentStatus: '草稿',
    adjustmentResult: '失败',
    documentDate: '2026-04-11',
    remark: '活动价未生效',
    creator: '王磊',
    createdAt: '2026-04-11 09:05:00',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const adjustmentCodeKeyword = query.adjustmentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.filter((row) => {
    const matchedDate = !query.documentDate || row.documentDate === query.documentDate;
    const matchedCode = !adjustmentCodeKeyword || row.adjustmentCode.toLowerCase().includes(adjustmentCodeKeyword);
    const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedResult = !query.adjustmentResult || row.adjustmentResult === query.adjustmentResult;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    const matchedReason = !query.adjustmentReason || row.adjustmentReason === query.adjustmentReason;
    const matchedCreator = !query.creator || row.creator === query.creator;
    return matchedDate && matchedCode && matchedStatus && matchedResult && matchedRemark && matchedReason && matchedCreator;
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
  query.documentDate = '';
  query.adjustmentCode = '';
  query.documentStatus = '';
  query.adjustmentResult = '';
  query.remark = '';
  query.supplier = '';
  query.adjustmentReason = '';
  query.itemName = '';
  query.creator = '';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleTableSettingCommand = (command: string | number | object) => {
  ElMessage.info(`表格设置：${String(command)}`);
};

const handleSelectionChange = (rows: AdjustmentRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: AdjustmentRow) => {
  ElMessage.info(`查看：${row.adjustmentCode}`);
};

const handleEdit = (row: AdjustmentRow) => {
  ElMessage.info(`编辑：${row.adjustmentCode}`);
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
      <el-form-item label="单据日期">
        <el-date-picker
          v-model="query.documentDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择单据日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="调整单号">
        <el-input
          v-model="query.adjustmentCode"
          placeholder="请输入调整单号"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="单据状态">
        <el-tree-select
          v-model="query.documentStatus"
          :data="statusTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="调整结果">
        <el-tree-select
          v-model="query.adjustmentResult"
          :data="resultTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="query.remark"
          placeholder="请输入备注"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="供应商">
        <el-tree-select
          v-model="query.supplier"
          :data="supplierTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="调整原因">
        <el-tree-select
          v-model="query.adjustmentReason"
          :data="reasonTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="物品">
        <el-tree-select
          v-model="query.itemName"
          :data="itemTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="创建人">
        <el-tree-select
          v-model="query.creator"
          :data="creatorTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 150px"
        />
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
      <el-button @click="handleToolbarAction('批量导入')">
        <el-icon><Upload /></el-icon>
        批量导入
      </el-button>
      <el-button @click="handleToolbarAction('批量提交')">批量提交</el-button>
      <el-button @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleToolbarAction('批量撤回')">批量撤回</el-button>
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
      <el-table-column prop="adjustmentCode" label="调整单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="adjustmentReason" label="调整原因" min-width="120" show-overflow-tooltip />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="adjustmentResult" label="调整结果" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentDate" label="单据日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
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
