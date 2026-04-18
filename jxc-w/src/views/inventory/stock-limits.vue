<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import {
  Delete,
  Download,
  Plus,
  RefreshRight,
  Search,
  Setting,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';

type InventoryStatus = '全部' | '正常' | '偏低' | '偏高';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type StockLimitRow = {
  id: number;
  warehouseCode: string;
  warehouseName: string;
  itemCode: string;
  itemName: string;
  spec: string;
  unit: string;
  minDays: number;
  safeDays: number;
  maxDays: number;
  avg7: number;
  avg14: number;
  avg21: number;
  avg30: number;
  avg60: number;
  minQty: number;
  maxQty: number;
  safeQty: number;
  currentQty: number;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const categoryOptions = ['全部', '肉类', '蔬菜', '调料', '包材'];
const itemOptions = ['全部', '鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const statusOptions: InventoryStatus[] = ['全部', '正常', '偏低', '偏高'];

const query = reactive({
  warehouse: '',
  category: '全部',
  item: '全部',
  status: '全部' as InventoryStatus,
});

const tableData: StockLimitRow[] = [
  {
    id: 1,
    warehouseCode: 'WH-001',
    warehouseName: '中央成品仓',
    itemCode: 'IT-0001',
    itemName: '鸡胸肉',
    spec: '1kg/包',
    unit: '包',
    minDays: 3,
    safeDays: 5,
    maxDays: 10,
    avg7: 24,
    avg14: 22,
    avg21: 20,
    avg30: 18,
    avg60: 17,
    minQty: 72,
    maxQty: 240,
    safeQty: 120,
    currentQty: 98,
  },
  {
    id: 2,
    warehouseCode: 'WH-002',
    warehouseName: '北区原料仓',
    itemCode: 'IT-0002',
    itemName: '牛腩',
    spec: '2kg/包',
    unit: '包',
    minDays: 2,
    safeDays: 4,
    maxDays: 8,
    avg7: 18,
    avg14: 16,
    avg21: 15,
    avg30: 14,
    avg60: 13,
    minQty: 36,
    maxQty: 144,
    safeQty: 72,
    currentQty: 30,
  },
  {
    id: 3,
    warehouseCode: 'WH-003',
    warehouseName: '南区包材仓',
    itemCode: 'IT-0003',
    itemName: '包装盒',
    spec: '50个/箱',
    unit: '箱',
    minDays: 4,
    safeDays: 7,
    maxDays: 14,
    avg7: 12,
    avg14: 11,
    avg21: 10,
    avg30: 9,
    avg60: 8,
    minQty: 48,
    maxQty: 168,
    safeQty: 84,
    currentQty: 92,
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
  return tableData.filter((row) => {
    const matchedWarehouse = !query.warehouse || row.warehouseCode === query.warehouse;
    const matchedCategory = query.category === '全部' || row.itemName.includes(query.category);
    const matchedItem = query.item === '全部' || row.itemName === query.item;
    const matchedStatus = query.status === '全部'
      || (query.status === '偏低' && row.currentQty < row.safeQty)
      || (query.status === '偏高' && row.currentQty > row.maxQty)
      || (query.status === '正常' && row.currentQty >= row.safeQty && row.currentQty <= row.maxQty);
    return matchedWarehouse && matchedCategory && matchedItem && matchedStatus;
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
  query.category = '全部';
  query.item = '全部';
  query.status = '全部';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: StockLimitRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: StockLimitRow) => {
  ElMessage.info(`查看：${row.itemCode}`);
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
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="物品类别">
        <el-select v-model="query.category" style="width: 140px">
          <el-option v-for="option in categoryOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品信息">
        <el-select v-model="query.item" style="width: 140px">
          <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="库存状态">
        <el-select v-model="query.status" style="width: 140px">
          <el-option v-for="option in statusOptions" :key="option" :label="option" :value="option" />
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
      <el-button type="primary" @click="handleToolbarAction('新增')">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button @click="handleToolbarAction('导出')">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
      <el-button @click="handleToolbarAction('批量设置库存下限')">批量设置库存下限</el-button>
      <el-button @click="handleToolbarAction('批量设置库存上限')">批量设置库存上限</el-button>
      <el-button @click="handleToolbarAction('批量设置安全库存')">批量设置安全库存</el-button>
      <el-button @click="handleToolbarAction('批量删除')">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleToolbarAction('计算上下限和安全库存')">
        <el-icon><Setting /></el-icon>
        计算上下限和安全库存
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
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="warehouseCode" label="仓库编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="itemCode" label="物品编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemName" label="物品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="spec" label="规格型号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="unit" label="单位" min-width="80" show-overflow-tooltip />
      <el-table-column prop="minDays" label="最小库存天数" min-width="120" show-overflow-tooltip />
      <el-table-column prop="safeDays" label="安全库存天数" min-width="120" show-overflow-tooltip />
      <el-table-column prop="maxDays" label="最大库存天数" min-width="120" show-overflow-tooltip />
      <el-table-column prop="avg7" label="近7天日均出库量" min-width="140" show-overflow-tooltip />
      <el-table-column prop="avg14" label="近14天日均出库量" min-width="150" show-overflow-tooltip />
      <el-table-column prop="avg21" label="近21天日均出库量" min-width="150" show-overflow-tooltip />
      <el-table-column prop="avg30" label="近30天日均出库量" min-width="150" show-overflow-tooltip />
      <el-table-column prop="avg60" label="近60天日均出库量" min-width="150" show-overflow-tooltip />
      <el-table-column prop="minQty" label="库存下限数量" min-width="140" show-overflow-tooltip />
      <el-table-column prop="maxQty" label="库存上限数量" min-width="140" show-overflow-tooltip />
      <el-table-column prop="safeQty" label="安全库存" min-width="120" show-overflow-tooltip />
      <el-table-column prop="currentQty" label="当前库存" min-width="100" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
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
