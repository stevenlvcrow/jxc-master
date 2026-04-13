<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type LogisticsMode = '统配' | '直营网配' | '供应商直送';
type ExpenseType = '物流费' | '配送费' | '运输附加费';
type ExpenseRuleRow = {
  id: number;
  expenseCode: string;
  expenseRuleName: string;
  supplyOrg: string;
  logisticsMode: LogisticsMode;
  expenseType: ExpenseType;
};
type OrgTreeNode = {
  value: string;
  label: string;
  children?: OrgTreeNode[];
};

const logisticsModeOptions: LogisticsMode[] = ['统配', '直营网配', '供应商直送'];
const supplyOrgTree: OrgTreeNode[] = [
  {
    value: 'east-region',
    label: '华东大区',
    children: [
      { value: 'hongqiao-store', label: '虹桥门店' },
      { value: 'pudong-store', label: '浦东门店' },
    ],
  },
  {
    value: 'direct-center',
    label: '上海直营中心',
    children: [
      { value: 'xuhui-store', label: '徐汇门店' },
      { value: 'jingan-store', label: '静安门店' },
    ],
  },
];

const query = reactive({
  expenseCode: '',
  expenseRuleName: '',
  supplyOrg: '',
  logisticsMode: '',
});

const tableData: ExpenseRuleRow[] = [
  {
    id: 1,
    expenseCode: 'FEE-001',
    expenseRuleName: '虹桥门店统配基础物流费',
    supplyOrg: '虹桥门店',
    logisticsMode: '统配',
    expenseType: '物流费',
  },
  {
    id: 2,
    expenseCode: 'FEE-002',
    expenseRuleName: '浦东门店直营运输费',
    supplyOrg: '浦东门店',
    logisticsMode: '直营网配',
    expenseType: '配送费',
  },
  {
    id: 3,
    expenseCode: 'FEE-003',
    expenseRuleName: '徐汇门店直送附加费',
    supplyOrg: '徐汇门店',
    logisticsMode: '供应商直送',
    expenseType: '运输附加费',
  },
  {
    id: 4,
    expenseCode: 'FEE-004',
    expenseRuleName: '静安门店统配夜配费',
    supplyOrg: '静安门店',
    logisticsMode: '统配',
    expenseType: '配送费',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

const orgLabelMap = computed(() => {
  const map = new Map<string, string>();
  const walk = (nodes: OrgTreeNode[]) => {
    nodes.forEach((node) => {
      map.set(node.value, node.label);
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };
  walk(supplyOrgTree);
  return map;
});

const filteredRows = computed(() => {
  const expenseCodeKeyword = query.expenseCode.trim().toLowerCase();
  const ruleNameKeyword = query.expenseRuleName.trim().toLowerCase();
  const supplyOrgLabel = query.supplyOrg ? orgLabelMap.value.get(query.supplyOrg) ?? query.supplyOrg : '';

  return tableData.filter((row) => {
    const matchedCode = !expenseCodeKeyword || row.expenseCode.toLowerCase().includes(expenseCodeKeyword);
    const matchedName = !ruleNameKeyword || row.expenseRuleName.toLowerCase().includes(ruleNameKeyword);
    const matchedSupplyOrg = !supplyOrgLabel || row.supplyOrg === supplyOrgLabel;
    const matchedLogisticsMode = !query.logisticsMode || row.logisticsMode === query.logisticsMode;
    return matchedCode && matchedName && matchedSupplyOrg && matchedLogisticsMode;
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
  query.expenseCode = '';
  query.expenseRuleName = '';
  query.supplyOrg = '';
  query.logisticsMode = '';
  currentPage.value = 1;
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
      <el-form-item label="费用编号">
        <el-input
          v-model="query.expenseCode"
          placeholder="请输入费用编号"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="费用规则名称">
        <el-input
          v-model="query.expenseRuleName"
          placeholder="请输入费用规则名称"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="供货机构">
        <el-tree-select
          v-model="query.supplyOrg"
          :data="supplyOrgTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          check-strictly
          default-expand-all
          clearable
          placeholder="请选择供货机构"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="物流方式">
        <el-select
          v-model="query.logisticsMode"
          clearable
          placeholder="请选择物流方式"
          style="width: 160px"
        >
          <el-option
            v-for="option in logisticsModeOptions"
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
      <el-table-column prop="expenseCode" label="费用编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="expenseRuleName" label="费用规则名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="supplyOrg" label="供货机构" min-width="160" show-overflow-tooltip />
      <el-table-column prop="logisticsMode" label="物流方式" min-width="120" show-overflow-tooltip />
      <el-table-column prop="expenseType" label="费用类型" min-width="120" show-overflow-tooltip />
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
