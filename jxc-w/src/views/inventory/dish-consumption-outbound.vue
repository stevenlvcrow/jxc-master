<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ArrowDown, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type OutboundStatus = '未出库' | '已出库' | '已撤销';
type CalculationStatus = '全部' | '已计算' | '未计算' | '计算失败';
type DishConsumptionRow = {
  id: number;
  documentCode: string;
  businessDate: string;
  orderTime: string;
  quantity: number;
  outboundStatus: OutboundStatus;
  calculationStatus: Exclude<CalculationStatus, '全部'>;
  missingCostCard: string;
  missingWarehouse: string;
};

const outboundStatusOptions: OutboundStatus[] = ['未出库', '已出库', '已撤销'];
const calculationStatusOptions: CalculationStatus[] = ['全部', '已计算', '未计算', '计算失败'];

const query = reactive({
  businessDate: '',
  outboundStatus: '',
  calculationStatus: '全部' as CalculationStatus,
  missingCostCard: '',
  missingWarehouse: '',
});

const tableData: DishConsumptionRow[] = [
  {
    id: 1,
    documentCode: 'DIS-202604-001',
    businessDate: '2026-04-13',
    orderTime: '2026-04-13 11:08:12',
    quantity: 284,
    outboundStatus: '已出库',
    calculationStatus: '已计算',
    missingCostCard: '无',
    missingWarehouse: '无',
  },
  {
    id: 2,
    documentCode: 'DIS-202604-002',
    businessDate: '2026-04-12',
    orderTime: '2026-04-12 20:45:31',
    quantity: 198,
    outboundStatus: '未出库',
    calculationStatus: '未计算',
    missingCostCard: '存在',
    missingWarehouse: '无',
  },
  {
    id: 3,
    documentCode: 'DIS-202604-003',
    businessDate: '2026-04-11',
    orderTime: '2026-04-11 18:02:14',
    quantity: 156,
    outboundStatus: '已撤销',
    calculationStatus: '计算失败',
    missingCostCard: '无',
    missingWarehouse: '存在',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  return tableData.filter((row) => {
    const matchedDate = !query.businessDate || row.businessDate === query.businessDate;
    const matchedOutboundStatus = !query.outboundStatus || row.outboundStatus === query.outboundStatus;
    const matchedCalcStatus =
      query.calculationStatus === '全部' || row.calculationStatus === query.calculationStatus;
    const matchedMissingCostCard =
      !query.missingCostCard || row.missingCostCard.includes(query.missingCostCard);
    const matchedMissingWarehouse =
      !query.missingWarehouse || row.missingWarehouse.includes(query.missingWarehouse);
    return matchedDate
      && matchedOutboundStatus
      && matchedCalcStatus
      && matchedMissingCostCard
      && matchedMissingWarehouse;
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
  query.businessDate = '';
  query.outboundStatus = '';
  query.calculationStatus = '全部';
  query.missingCostCard = '';
  query.missingWarehouse = '';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleTableSettingCommand = (command: string | number | object) => {
  ElMessage.info(`表格设置：${String(command)}`);
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
      <el-form-item label="营业日期">
        <el-date-picker
          v-model="query.businessDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择营业日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="出库状态">
        <el-select v-model="query.outboundStatus" clearable style="width: 140px">
          <el-option v-for="option in outboundStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="理论用量计算状态">
        <el-select v-model="query.calculationStatus" style="width: 180px">
          <el-option
            v-for="option in calculationStatusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="存在菜品未设置成本卡">
        <el-input v-model="query.missingCostCard" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="存在菜品未关联仓库">
        <el-input v-model="query.missingWarehouse" placeholder="请输入" clearable style="width: 180px" />
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
      <el-button type="primary" @click="handleToolbarAction('菜品消耗出库重算')">菜品消耗出库重算</el-button>
      <el-button @click="handleToolbarAction('批量出库')">批量出库</el-button>
      <el-button @click="handleToolbarAction('批量撤销出库')">批量撤销出库</el-button>
      <el-dropdown @command="handleTableSettingCommand">
        <el-button>
          表格设置
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="单据号">单据号</el-dropdown-item>
            <el-dropdown-item command="日期">日期</el-dropdown-item>
            <el-dropdown-item command="订单下单时间">订单下单时间</el-dropdown-item>
            <el-dropdown-item command="数量">数量</el-dropdown-item>
            <el-dropdown-item command="出库状态">出库状态</el-dropdown-item>
            <el-dropdown-item command="理论用量计算状态">理论用量计算状态</el-dropdown-item>
            <el-dropdown-item command="存在菜品未设置成本卡">存在菜品未设置成本卡</el-dropdown-item>
            <el-dropdown-item command="存在菜品未关联仓库">存在菜品未关联仓库</el-dropdown-item>
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
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentCode" label="单据号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="businessDate" label="日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="orderTime" label="订单下单时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="quantity" label="数量" min-width="90" show-overflow-tooltip />
      <el-table-column prop="outboundStatus" label="出库状态" min-width="110" show-overflow-tooltip />
      <el-table-column prop="calculationStatus" label="理论用量计算状态" min-width="160" show-overflow-tooltip />
      <el-table-column prop="missingCostCard" label="存在菜品未设置成本卡" min-width="170" show-overflow-tooltip />
      <el-table-column prop="missingWarehouse" label="存在菜品未关联仓库" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleToolbarAction(`查看 ${row.documentCode}`)">查看</el-button>
        </template>
      </el-table-column>
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
