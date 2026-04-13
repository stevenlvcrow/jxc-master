<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type OperationType = '全部' | '锁库' | '解锁';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type StockLockLogRow = {
  id: number;
  documentType: string;
  documentCode: string;
  lockReason: string;
  upstreamCode: string;
  upstreamType: string;
  orgName: string;
  warehouse: string;
  itemCode: string;
  itemName: string;
  spec: string;
  stockUnit: string;
  stockUnitQty: number;
  baseUnit: string;
  baseUnitQty: number;
  operationDate: string;
  remark: string;
};

const warehouseTree: TreeNode[] = [
  {
    value: 'warehouse-root',
    label: '仓库中心',
    children: [
      { value: 'WH-001', label: '中央成品仓' },
      { value: 'WH-002', label: '北区原料仓' },
      { value: 'WH-003', label: '南区包材仓' },
    ],
  },
];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const reasonTree: TreeNode[] = [
  {
    value: 'reason-root',
    label: '锁库原因',
    children: [
      { value: '品质异常', label: '品质异常' },
      { value: '盘点冻结', label: '盘点冻结' },
      { value: '外部稽核', label: '外部稽核' },
    ],
  },
];
const operationTypeOptions: OperationType[] = ['全部', '锁库', '解锁'];

const query = reactive({
  operationDateRange: [] as string[],
  warehouse: '',
  documentCode: '',
  upstreamCode: '',
  itemName: '',
  lockReason: '',
  operationType: '全部' as OperationType,
});

const tableData: StockLockLogRow[] = [
  {
    id: 1,
    documentType: '锁库单',
    documentCode: 'LK-202604-001',
    lockReason: '盘点冻结',
    upstreamCode: 'PD-202604-021',
    upstreamType: '盘点单',
    orgName: '总部',
    warehouse: '中央成品仓',
    itemCode: 'IT-0001',
    itemName: '鸡胸肉',
    spec: '1kg/包',
    stockUnit: '包',
    stockUnitQty: 18,
    baseUnit: '千克',
    baseUnitQty: 18,
    operationDate: '2026-04-13 09:12:00',
    remark: '',
  },
  {
    id: 2,
    documentType: '解锁单',
    documentCode: 'UL-202604-003',
    lockReason: '品质异常',
    upstreamCode: 'QA-202604-005',
    upstreamType: '质检单',
    orgName: '华东分公司',
    warehouse: '北区原料仓',
    itemCode: 'IT-0002',
    itemName: '牛腩',
    spec: '2kg/包',
    stockUnit: '包',
    stockUnitQty: 6,
    baseUnit: '千克',
    baseUnitQty: 12,
    operationDate: '2026-04-12 16:40:00',
    remark: '复检通过',
  },
  {
    id: 3,
    documentType: '锁库单',
    documentCode: 'LK-202604-003',
    lockReason: '外部稽核',
    upstreamCode: 'AD-202604-013',
    upstreamType: '调整单',
    orgName: '华南分公司',
    warehouse: '南区包材仓',
    itemCode: 'IT-0003',
    itemName: '包装盒',
    spec: '50个/箱',
    stockUnit: '箱',
    stockUnitQty: 4,
    baseUnit: '个',
    baseUnitQty: 200,
    operationDate: '2026-04-11 10:05:00',
    remark: '稽核冻结',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  const [startDate, endDate] = query.operationDateRange;
  return tableData.filter((row) => {
    const matchedStart = !startDate || row.operationDate.slice(0, 10) >= startDate;
    const matchedEnd = !endDate || row.operationDate.slice(0, 10) <= endDate;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedDocument = !query.documentCode || row.documentCode.includes(query.documentCode);
    const matchedUpstream = !query.upstreamCode || row.upstreamCode.includes(query.upstreamCode);
    const matchedItem = !query.itemName || row.itemName === query.itemName;
    const matchedReason = !query.lockReason || row.lockReason === query.lockReason;
    const matchedOperation =
      query.operationType === '全部'
      || (query.operationType === '锁库' && row.documentType === '锁库单')
      || (query.operationType === '解锁' && row.documentType === '解锁单');
    return matchedStart
      && matchedEnd
      && matchedWarehouse
      && matchedDocument
      && matchedUpstream
      && matchedItem
      && matchedReason
      && matchedOperation;
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
  query.operationDateRange = [];
  query.warehouse = '';
  query.documentCode = '';
  query.upstreamCode = '';
  query.itemName = '';
  query.lockReason = '';
  query.operationType = '全部';
  currentPage.value = 1;
};

const handleExport = () => {
  ElMessage.info('批量导出单据列表功能待接入');
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
      <el-form-item label="操作日期">
        <el-date-picker
          v-model="query.operationDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="~"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
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
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="上游单据号">
        <el-input v-model="query.upstreamCode" placeholder="请输入上游单据号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemName" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="锁库原因">
        <el-tree-select
          v-model="query.lockReason"
          :data="reasonTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择锁库原因"
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="query.operationType" style="width: 120px">
          <el-option
            v-for="option in operationTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
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
      <el-button @click="handleExport">
        <el-icon><Download /></el-icon>
        批量导出单据列表
      </el-button>
    </div>

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="400"
      :empty-text="'当前机构暂无数据'"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentType" label="单据类型" min-width="120" show-overflow-tooltip />
      <el-table-column prop="documentCode" label="单据编号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="lockReason" label="锁库原因" min-width="120" show-overflow-tooltip />
      <el-table-column prop="upstreamCode" label="上游单据号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="upstreamType" label="上游单据类型" min-width="140" show-overflow-tooltip />
      <el-table-column prop="orgName" label="机构" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCode" label="物品编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemName" label="物品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="spec" label="规格型号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="stockUnit" label="库存单位" min-width="100" show-overflow-tooltip />
      <el-table-column
        prop="stockUnitQty"
        label="库存单位锁库/解锁数量"
        min-width="170"
        show-overflow-tooltip
      />
      <el-table-column prop="baseUnit" label="基准单位" min-width="100" show-overflow-tooltip />
      <el-table-column
        prop="baseUnitQty"
        label="基准单位锁库/解锁数量"
        min-width="170"
        show-overflow-tooltip
      />
      <el-table-column prop="operationDate" label="操作日期" min-width="170" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
    </el-table>

    <div class="table-pagination">
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
