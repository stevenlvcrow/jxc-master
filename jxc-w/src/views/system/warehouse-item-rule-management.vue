<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type WarehouseItemRuleRow = {
  id: number;
  ruleCode: string;
  ruleName: string;
  warehouseName: string;
  creator: string;
  createdAt: string;
  updatedAt: string;
};

const warehouseOptions = ['中央成品仓', '北区原料仓', '南区包材仓', '东区冷藏仓', '西区原料仓'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤', '蔬菜拼盘'];

const query = reactive({
  ruleCode: '',
  ruleName: '',
  warehouseName: '',
  itemName: '',
});

const tableData: WarehouseItemRuleRow[] = [
  {
    id: 1,
    ruleCode: 'RULE-001',
    ruleName: '中央成品仓默认成品规则',
    warehouseName: '中央成品仓',
    creator: '张敏',
    createdAt: '2026-04-12 10:20:00',
    updatedAt: '2026-04-13 09:18:00',
  },
  {
    id: 2,
    ruleCode: 'RULE-002',
    ruleName: '北区原料仓肉类控制规则',
    warehouseName: '北区原料仓',
    creator: '王磊',
    createdAt: '2026-04-11 14:30:00',
    updatedAt: '2026-04-12 16:05:00',
  },
  {
    id: 3,
    ruleCode: 'RULE-003',
    ruleName: '南区包材仓耗材绑定规则',
    warehouseName: '南区包材仓',
    creator: '李娜',
    createdAt: '2026-04-10 09:12:00',
    updatedAt: '2026-04-11 10:40:00',
  },
  {
    id: 4,
    ruleCode: 'RULE-004',
    ruleName: '东区冷藏仓冷链规则',
    warehouseName: '东区冷藏仓',
    creator: '周凯',
    createdAt: '2026-04-09 13:00:00',
    updatedAt: '2026-04-10 15:32:00',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  const ruleCodeKeyword = query.ruleCode.trim().toLowerCase();
  const ruleNameKeyword = query.ruleName.trim().toLowerCase();
  return tableData.filter((row) => {
    const matchedRuleCode = !ruleCodeKeyword || row.ruleCode.toLowerCase().includes(ruleCodeKeyword);
    const matchedRuleName = !ruleNameKeyword || row.ruleName.toLowerCase().includes(ruleNameKeyword);
    const matchedWarehouse = !query.warehouseName || row.warehouseName === query.warehouseName;
    const matchedItem = !query.itemName || row.ruleName.includes(query.itemName);
    return matchedRuleCode && matchedRuleName && matchedWarehouse && matchedItem;
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
  query.ruleCode = '';
  query.ruleName = '';
  query.warehouseName = '';
  query.itemName = '';
  currentPage.value = 1;
};

const handleView = (row: WarehouseItemRuleRow) => {
  ElMessage.info(`查看：${row.ruleName}`);
};

const handleEdit = (row: WarehouseItemRuleRow) => {
  ElMessage.info(`编辑：${row.ruleName}`);
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
      <el-form-item label="规则编码">
        <el-input
          v-model="query.ruleCode"
          placeholder="请输入规则编码"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="规则名称">
        <el-input
          v-model="query.ruleName"
          placeholder="请输入规则名称"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="仓库名称">
        <el-select
          v-model="query.warehouseName"
          clearable
          placeholder="请选择仓库名称"
          style="width: 180px"
        >
          <el-option
            v-for="option in warehouseOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select
          v-model="query.itemName"
          clearable
          placeholder="请选择物品"
          style="width: 180px"
        >
          <el-option
            v-for="option in itemOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      :empty-text="'当前机构暂无数据'"
    >
      <el-table-column type="index" label="序号" width="56" />
      <el-table-column prop="ruleCode" label="规则编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="ruleName" label="规则名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库" min-width="140" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="最新修改日期" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ filteredRows.length }} 条</div>
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
