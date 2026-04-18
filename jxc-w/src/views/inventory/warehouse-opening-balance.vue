<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';

type OpeningStatus = '全部' | '草稿' | '已提交' | '已完成';
type OpeningRow = {
  id: number;
  openingDate: string;
  warehouseCode: string;
  warehouseName: string;
  amount: string;
  status: Exclude<OpeningStatus, '全部'>;
  createdAt: string;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const statusOptions: OpeningStatus[] = ['全部', '草稿', '已提交', '已完成'];

const query = reactive({
  warehouse: '',
  status: '全部' as OpeningStatus,
});

const tableData: OpeningRow[] = [
  {
    id: 1,
    openingDate: '2026-04-01',
    warehouseCode: 'WH-001',
    warehouseName: '中央成品仓',
    amount: '128,560.00',
    status: '已完成',
    createdAt: '2026-04-01 09:15:00',
  },
  {
    id: 2,
    openingDate: '2026-04-01',
    warehouseCode: 'WH-002',
    warehouseName: '北区原料仓',
    amount: '96,300.00',
    status: '已提交',
    createdAt: '2026-04-01 09:40:00',
  },
  {
    id: 3,
    openingDate: '2026-04-01',
    warehouseCode: 'WH-003',
    warehouseName: '南区包材仓',
    amount: '35,880.00',
    status: '草稿',
    createdAt: '2026-04-01 10:05:00',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

onMounted(() => {
  void loadWarehouseTree();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    void loadWarehouseTree();
  },
);

const warehouseLabelMap = computed(() => {
  const map = new Map<string, string>();
  const walk = (nodes: WarehouseTreeNode[]) => {
    nodes.forEach((node) => {
      map.set(node.value, node.label);
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };
  walk(warehouseTree.value);
  return map;
});

const filteredRows = computed(() => {
  const warehouseLabel = query.warehouse ? warehouseLabelMap.value.get(query.warehouse) ?? query.warehouse : '';
  return tableData.filter((row) => {
    const matchedWarehouse = !warehouseLabel || row.warehouseName === warehouseLabel;
    const matchedStatus = query.status === '全部' || row.status === query.status;
    return matchedWarehouse && matchedStatus;
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
  query.warehouse = '';
  query.status = '全部';
  currentPage.value = 1;
};

const handleView = (row: OpeningRow) => {
  console.info('查看仓库期初', row.id);
};

const handleEdit = (row: OpeningRow) => {
  console.info('编辑仓库期初', row.id);
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
      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择仓库"
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" style="width: 120px">
          <el-option
            v-for="option in statusOptions"
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
      <el-table-column prop="openingDate" label="期初日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouseCode" label="仓库编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" min-width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
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
