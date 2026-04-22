<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Calendar, CloseBold, EditPen, Finished, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  type ItemCategoryTreeNode,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type ReviewStatus = '待审核' | '已审核' | '已驳回';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseApplicationReviewRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  supplier: string;
  purchaseUnit: string;
  applicationQty: number;
  reviewQty: number;
  purchasePrice: number;
  purchaseAmount: number;
  baseUnit: string;
  baseConversion: string;
  baseUnitQty: number;
  warehouse: string;
  expectedArrivalDate: string;
  remark: string;
  applicant: string;
  reviewStatus: ReviewStatus;
  applicationDate: string;
  applicationCode: string;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const reviewStatusOptions: ReviewStatus[] = ['待审核', '已审核', '已驳回'];
const querySchemeOptions = ['系统默认方案'];

const query = reactive({
  documentDateRange: [] as string[],
  applicationCode: '',
  warehouse: '',
  supplier: '',
  itemCategory: '',
  reviewStatus: '待审核' as ReviewStatus,
  itemCode: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);
const optionLoading = ref(false);
const itemCategoryTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const tableData = ref<PurchaseApplicationReviewRow[]>([
  {
    id: 1,
    itemCode: 'ITEM-001',
    itemName: '鸡胸肉',
    spec: '10kg/箱',
    itemCategory: '生鲜原料',
    supplier: '鲜达食品',
    purchaseUnit: '箱',
    applicationQty: 20,
    reviewQty: 20,
    purchasePrice: 185,
    purchaseAmount: 3700,
    baseUnit: 'kg',
    baseConversion: '1箱=10kg',
    baseUnitQty: 200,
    warehouse: '中央成品仓',
    expectedArrivalDate: '2026-04-24',
    remark: '周末备货',
    applicant: '王磊',
    reviewStatus: '待审核',
    applicationDate: '2026-04-20',
    applicationCode: 'PA-202604-001',
  },
  {
    id: 2,
    itemCode: 'ITEM-002',
    itemName: '牛腩',
    spec: '5kg/包',
    itemCategory: '生鲜原料',
    supplier: '优选农场',
    purchaseUnit: '包',
    applicationQty: 16,
    reviewQty: 14,
    purchasePrice: 260,
    purchaseAmount: 3640,
    baseUnit: 'kg',
    baseConversion: '1包=5kg',
    baseUnitQty: 70,
    warehouse: '北区原料仓',
    expectedArrivalDate: '2026-04-25',
    remark: '按审核数量下单',
    applicant: '赵晨',
    reviewStatus: '待审核',
    applicationDate: '2026-04-20',
    applicationCode: 'PA-202604-002',
  },
  {
    id: 3,
    itemCode: 'ITEM-003',
    itemName: '包装盒',
    spec: '500个/箱',
    itemCategory: '包材',
    supplier: '盒马包材',
    purchaseUnit: '箱',
    applicationQty: 8,
    reviewQty: 8,
    purchasePrice: 96,
    purchaseAmount: 768,
    baseUnit: '个',
    baseConversion: '1箱=500个',
    baseUnitQty: 4000,
    warehouse: '南区包材仓',
    expectedArrivalDate: '2026-04-26',
    remark: '-',
    applicant: '李娜',
    reviewStatus: '已审核',
    applicationDate: '2026-04-19',
    applicationCode: 'PA-202604-003',
  },
]);

const flattenWarehouseTree = (nodes: WarehouseTreeNode[]) => {
  const rows: Array<{ value: string; label: string }> = [];
  const walk = (currentNodes: WarehouseTreeNode[]) => {
    currentNodes.forEach((node) => {
      if (node.children?.length) {
        walk(node.children);
        return;
      }
      rows.push({ value: node.value, label: node.label });
    });
  };
  walk(nodes);
  return rows;
};

const warehouseOptions = computed(() => flattenWarehouseTree(warehouseTree.value));

const normalizeCategoryTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeCategoryTree(node.children) : undefined,
}));

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
    await Promise.all([loadWarehouseTree(), loadSupplierOptions()]);
    if (!orgId) {
      itemCategoryTree.value = [];
      itemOptions.value = [];
      return;
    }
    const [categoryRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId)),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    itemOptions.value = [];
    ElMessage.error('采购单申请审核筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const filteredRows = computed(() => {
  const applicationCodeKeyword = query.applicationCode.trim().toLowerCase();
  const startDate = query.documentDateRange[0];
  const endDate = query.documentDateRange[1];
  return tableData.value.filter((row) => {
    const matchedDate = (!startDate || row.applicationDate >= startDate) && (!endDate || row.applicationDate <= endDate);
    const matchedCode = !applicationCodeKeyword || row.applicationCode.toLowerCase().includes(applicationCodeKeyword);
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedSupplier = !query.supplier || row.supplier === query.supplier;
    const matchedCategory = !query.itemCategory || row.itemCategory === query.itemCategory;
    const matchedStatus = !query.reviewStatus || row.reviewStatus === query.reviewStatus;
    const matchedItem = !query.itemCode || row.itemCode === query.itemCode;
    return matchedDate
      && matchedCode
      && matchedWarehouse
      && matchedSupplier
      && matchedCategory
      && matchedStatus
      && matchedItem;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const formatMoney = (value: number) => value.toFixed(2);
const formatQty = (value: number) => value.toFixed(2);

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.documentDateRange = [];
  query.applicationCode = '';
  query.warehouse = '';
  query.supplier = '';
  query.itemCategory = '';
  query.reviewStatus = '待审核';
  query.itemCode = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: PurchaseApplicationReviewRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleReview = (row: PurchaseApplicationReviewRow) => {
  ElMessage.info(`审核：${row.applicationCode} / ${row.itemName}`);
};

const handleReject = (row: PurchaseApplicationReviewRow) => {
  ElMessage.info(`驳回：${row.applicationCode} / ${row.itemName}`);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
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
      <el-form-item label="单据日期">
        <el-date-picker
          v-model="query.documentDateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item label="申请单号">
        <el-input
          v-model="query.applicationCode"
          clearable
          placeholder="请输入申请单号"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="仓库">
        <el-select
          v-model="query.warehouse"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
          style="width: 180px"
        >
          <el-option
            v-for="option in warehouseOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="供应商">
        <el-tree-select
          v-model="query.supplier"
          :data="supplierOptions.map((item) => ({ value: item.value, label: item.label }))"
          :loading="supplierLoading"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          filterable
          check-strictly
          placeholder="请选择"
          style="width: 190px"
        />
      </el-form-item>

      <el-form-item label="物品类别">
        <el-select
          v-model="query.itemCategory"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="选择物品分类"
          style="width: 180px"
        >
          <el-option
            v-for="option in itemCategoryTree"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="审核状态">
        <el-select v-model="query.reviewStatus" clearable placeholder="待审核" style="width: 140px">
          <el-option v-for="option in reviewStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="物品">
        <el-select
          v-model="query.itemCode"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="请选择"
          style="width: 220px"
        >
          <el-option
            v-for="option in itemOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option
            v-for="option in querySchemeOptions"
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
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量修改供应商')">
        <el-icon><EditPen /></el-icon>
        批量修改供应商
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量修改期望到货日期')">
        <el-icon><Calendar /></el-icon>
        批量修改期望到货日期
      </el-button>
      <el-button type="primary" @click="handleToolbarAction('保存')">
        <el-icon><Finished /></el-icon>
        保 存
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量驳回')">
        <el-icon><CloseBold /></el-icon>
        批量驳回
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('生成采购订单')">
        生成采购订单
      </el-button>
      <el-button type="primary" :disabled="!selectedIds.length" @click="handleToolbarAction('生成并提交采购订单')">
        生成并提交采购订单
      </el-button>
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
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
      <el-table-column prop="purchaseUnit" label="采购单位" min-width="100" show-overflow-tooltip />
      <el-table-column prop="applicationQty" label="申请数量" min-width="110" align="right">
        <template #default="{ row }">{{ formatQty(row.applicationQty) }}</template>
      </el-table-column>
      <el-table-column prop="reviewQty" label="审核数量" min-width="110" align="right">
        <template #default="{ row }">
          <el-input-number v-model="row.reviewQty" :min="0" controls-position="right" size="small" style="width: 110px" />
        </template>
      </el-table-column>
      <el-table-column prop="purchasePrice" label="采购单价" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.purchasePrice) }}</template>
      </el-table-column>
      <el-table-column prop="purchaseAmount" label="采购金额" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.purchaseAmount) }}</template>
      </el-table-column>
      <el-table-column prop="baseUnit" label="基准单位" min-width="100" show-overflow-tooltip />
      <el-table-column prop="baseConversion" label="基准换算关系" min-width="130" show-overflow-tooltip />
      <el-table-column prop="baseUnitQty" label="基准单位数量" min-width="120" align="right">
        <template #default="{ row }">{{ formatQty(row.baseUnitQty) }}</template>
      </el-table-column>
      <el-table-column prop="warehouse" label="仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="expectedArrivalDate" label="期望到货日期" min-width="140" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="applicant" label="申请人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="审核状态" min-width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.reviewStatus === '已审核' ? 'success' : row.reviewStatus === '已驳回' ? 'danger' : 'warning'"
            size="small"
          >
            {{ row.reviewStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicationDate" label="申请日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="applicationCode" label="申请单号" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleReview(row)">审核</el-button>
          <el-button type="danger" link @click="handleReject(row)">驳回</el-button>
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
