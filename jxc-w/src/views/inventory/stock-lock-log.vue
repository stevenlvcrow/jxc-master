<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useRequiredOrgScope } from '@/composables/useRequiredOrgScope';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

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

const { orgId } = useRequiredOrgScope();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const rows = ref<StockLockLogRow[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const reasonTree: TreeNode[] = [
  {
    value: 'inventory-lock',
    label: '锁库原因',
    children: [],
  },
];
const operationTypeOptions: TreeNode[] = [
  { value: 'LOCK', label: '锁库' },
  { value: 'UNLOCK', label: '解锁' },
];

const query = reactive({
  operationDateRange: [] as string[],
  warehouse: '',
  documentCode: '',
  upstreamCode: '',
  itemKeyword: '',
  lockReason: '',
  operationType: '',
});

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.operationDateRange = [];
  query.warehouse = '';
  query.documentCode = '';
  query.upstreamCode = '';
  query.itemKeyword = '';
  query.lockReason = '';
  query.operationType = '';
  currentPage.value = 1;
};

watch(orgId, () => {
  rows.value = [];
  currentPage.value = 1;
  void loadWarehouseTree();
});

onMounted(() => {
  void loadWarehouseTree();
});
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
        <el-input v-model="query.itemKeyword" placeholder="请输入物品编码/名称" clearable style="width: 180px" />
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
        <el-tree-select
          v-model="query.operationType"
          :data="operationTypeOptions"
          :props="{ label: 'label', value: 'value' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 120px"
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

    <el-table
      :data="rows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="400"
      empty-text="暂无锁库日志数据"
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
      <el-table-column prop="stockUnitQty" label="库存单位锁库/解锁数量" min-width="170" show-overflow-tooltip />
      <el-table-column prop="baseUnit" label="基准单位" min-width="100" show-overflow-tooltip />
      <el-table-column prop="baseUnitQty" label="基准单位锁库/解锁数量" min-width="170" show-overflow-tooltip />
      <el-table-column prop="operationDate" label="操作日期" min-width="170" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
    </el-table>

    <div class="table-pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="rows.length"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="(page: number) => { currentPage = page; }"
        @size-change="(size: number) => { pageSize = size; currentPage = 1; }"
      />
    </div>
  </section>
</template>
