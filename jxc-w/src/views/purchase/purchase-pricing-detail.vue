<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type PricingDetailType = '供应商定价' | '仓库定价' | '活动定价';
type EnabledStatus = '启用' | '停用';
type LadderPriceStatus = '全部' | '是' | '否';
type TreeNode = { value: string; label: string; children?: TreeNode[] };
type PricingDetailRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  purchaseUnit: string;
  pricingDetailType: PricingDetailType;
  supplier: string;
  spotPrice: number;
  taxIncludedPrice: number;
  taxRate: number;
  taxExcludedPrice: number;
  priceLimit: string;
  effectiveDate: string;
  expireDate: string;
  enabledStatus: EnabledStatus;
  remark: string;
  ladderPrice: Exclude<LadderPriceStatus, '全部'>;
};

const sessionStore = useSessionStore();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const pricingDetailTypeOptions: PricingDetailType[] = ['供应商定价', '仓库定价', '活动定价'];
const ladderPriceOptions: LadderPriceStatus[] = ['全部', '是', '否'];
const enabledStatusOptions: EnabledStatus[] = ['启用', '停用'];

const query = reactive({
  priceDate: '',
  pricingDetailType: '',
  supplier: '',
  itemCode: '',
  ladderPrice: '全部' as LadderPriceStatus,
  enabledStatus: '',
});

const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const selectedIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const tableData = ref<PricingDetailRow[]>([
  {
    id: 1,
    itemCode: 'ITEM-001',
    itemName: '鸡胸肉',
    spec: '10kg/箱',
    itemCategory: '生鲜原料',
    purchaseUnit: '箱',
    pricingDetailType: '供应商定价',
    supplier: '鲜达食品',
    spotPrice: 182,
    taxIncludedPrice: 185,
    taxRate: 9,
    taxExcludedPrice: 169.72,
    priceLimit: '170.00-195.00',
    effectiveDate: '2026-04-01',
    expireDate: '2026-04-30',
    enabledStatus: '启用',
    remark: '四月统配价',
    ladderPrice: '否',
  },
  {
    id: 2,
    itemCode: 'ITEM-002',
    itemName: '牛腩',
    spec: '5kg/包',
    itemCategory: '生鲜原料',
    purchaseUnit: '包',
    pricingDetailType: '供应商定价',
    supplier: '优选农场',
    spotPrice: 255,
    taxIncludedPrice: 260,
    taxRate: 9,
    taxExcludedPrice: 238.53,
    priceLimit: '245.00-275.00',
    effectiveDate: '2026-04-08',
    expireDate: '2026-05-08',
    enabledStatus: '启用',
    remark: '阶梯价待接入',
    ladderPrice: '是',
  },
  {
    id: 3,
    itemCode: 'ITEM-003',
    itemName: '包装盒',
    spec: '500个/箱',
    itemCategory: '包材',
    purchaseUnit: '箱',
    pricingDetailType: '活动定价',
    supplier: '盒马包材',
    spotPrice: 96,
    taxIncludedPrice: 96,
    taxRate: 13,
    taxExcludedPrice: 84.96,
    priceLimit: '90.00-105.00',
    effectiveDate: '2026-04-10',
    expireDate: '2026-06-30',
    enabledStatus: '停用',
    remark: '活动价',
    ladderPrice: '否',
  },
]);

const supplierTree = computed<TreeNode[]>(() => supplierOptions.value.map((item) => ({
  value: item.value,
  label: item.label,
})));

const fetchAllPages = async <T,>(
  loader: (pageNo: number, pageSizeValue: number) => Promise<{ list?: T[]; total?: number; pageSize?: number }>,
  pageSizeValue = 200,
) => {
  const rows: T[] = [];
  let pageNo = 1;
  let totalValue: number;
  do {
    const page = await loader(pageNo, pageSizeValue);
    const list = Array.isArray(page.list) ? page.list : [];
    rows.push(...list);
    totalValue = Number(page.total ?? rows.length);
    if (!list.length || Number(page.pageSize ?? 0) <= 0) {
      break;
    }
    pageNo += 1;
  } while (rows.length < totalValue);
  return rows;
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await loadSupplierOptions();
    if (!orgId) {
      itemOptions.value = [];
      return;
    }
    const itemRows = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId));
    itemOptions.value = itemRows.map((row) => ({ value: row.code, label: `${row.code} / ${row.name}` }));
  } catch {
    itemOptions.value = [];
    ElMessage.error('采购定价明细筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const filteredRows = computed(() => tableData.value.filter((row) => {
  const matchedDate = !query.priceDate || (row.effectiveDate <= query.priceDate && row.expireDate >= query.priceDate);
  const matchedType = !query.pricingDetailType || row.pricingDetailType === query.pricingDetailType;
  const matchedSupplier = !query.supplier || row.supplier === query.supplier;
  const matchedItem = !query.itemCode || row.itemCode === query.itemCode;
  const matchedLadder = query.ladderPrice === '全部' || row.ladderPrice === query.ladderPrice;
  const matchedEnabled = !query.enabledStatus || row.enabledStatus === query.enabledStatus;
  return matchedDate && matchedType && matchedSupplier && matchedItem && matchedLadder && matchedEnabled;
}));

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const formatMoney = (value: number) => value.toFixed(2);
const formatRate = (value: number) => `${value.toFixed(2)}%`;

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.priceDate = '';
  query.pricingDetailType = '';
  query.supplier = '';
  query.itemCode = '';
  query.ladderPrice = '全部';
  query.enabledStatus = '';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: PricingDetailRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleEdit = (row: PricingDetailRow) => {
  ElMessage.info(`调整定价：${row.itemName}`);
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  () => {
    void loadOptions();
  },
);

onMounted(() => {
  void loadOptions();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="取价日期">
        <el-date-picker v-model="query.priceDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择取价日期" style="width: 160px" />
      </el-form-item>
      <el-form-item label="定价明细类型">
        <el-select v-model="query.pricingDetailType" clearable placeholder="请选择" style="width: 150px">
          <el-option v-for="option in pricingDetailTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商">
        <el-tree-select
          v-model="query.supplier"
          :data="supplierTree"
          :loading="supplierLoading"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          filterable
          check-strictly
          placeholder="请选择"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemCode" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 220px">
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否阶梯价">
        <el-select v-model="query.ladderPrice" style="width: 120px">
          <el-option v-for="option in ladderPriceOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态">
        <el-select v-model="query.enabledStatus" clearable placeholder="请选择" style="width: 120px">
          <el-option v-for="option in enabledStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="handleReset"><el-icon><RefreshRight /></el-icon>重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <div class="table-toolbar">
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量调整')">批量调整</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量启用')">批量启用</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量停用')">批量停用</el-button>
      <el-button @click="handleToolbarAction('导出')"><el-icon><Download /></el-icon>导出</el-button>
    </div>

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      height="460"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="itemCode" label="物品编码" min-width="130" fixed="left" show-overflow-tooltip />
      <el-table-column prop="itemName" label="物品名称" min-width="140" fixed="left" show-overflow-tooltip />
      <el-table-column prop="spec" label="规格型号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCategory" label="物品类别" min-width="120" show-overflow-tooltip />
      <el-table-column prop="purchaseUnit" label="采购单位" min-width="100" show-overflow-tooltip />
      <el-table-column prop="pricingDetailType" label="定价明细类型" min-width="130" show-overflow-tooltip />
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
      <el-table-column prop="spotPrice" label="时价" min-width="90" align="right">
        <template #default="{ row }">{{ formatMoney(row.spotPrice) }}</template>
      </el-table-column>
      <el-table-column prop="taxIncludedPrice" label="定价（含税）" min-width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.taxIncludedPrice) }}</template>
      </el-table-column>
      <el-table-column prop="taxRate" label="税率" min-width="90" align="right">
        <template #default="{ row }">{{ formatRate(row.taxRate) }}</template>
      </el-table-column>
      <el-table-column prop="taxExcludedPrice" label="定价（不含税）" min-width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.taxExcludedPrice) }}</template>
      </el-table-column>
      <el-table-column prop="priceLimit" label="单价限制" min-width="120" show-overflow-tooltip />
      <el-table-column prop="effectiveDate" label="价格生效日期" min-width="130" show-overflow-tooltip />
      <el-table-column prop="expireDate" label="价格失效日期" min-width="130" show-overflow-tooltip />
      <el-table-column prop="enabledStatus" label="启用状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">调整</el-button>
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
