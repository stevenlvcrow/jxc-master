<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore, type OrgNode } from '@/stores/session';
import {
  fetchItemCategoryTreeApi,
  type ItemCategoryTreeNode,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type StatisticMode = '从本店调出' | '调入本店';
type DateType = '调拨日期' | '调拨入库日期';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type TransferDetailRow = {
  id: string;
  transferNo: string;
  itemCode: string;
  itemName: string;
  documentStatus: string;
  transferDate: string;
  outboundAuditTime: string;
  sourceStore: string;
  sourceWarehouse: string;
  inboundDate: string;
  inboundAuditTime: string;
  targetStore: string;
  targetWarehouse: string;
  specModel: string;
  itemCategory: string;
  baseUnit: string;
  transferBaseQty: number;
  businessUnit: string;
  transferQty: number;
  inboundAmountTaxIncluded: number;
  outboundCostAmountExTax: number;
  outboundSettlementAmountTaxIncluded: number;
  inboundPriceTaxIncluded: number;
  outboundCostPriceExTax: number;
  outboundSettlementPriceTaxIncluded: number;
  remark: string;
};

type ReportColumn = {
  key: keyof TransferDetailRow;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const statisticModeOptions: StatisticMode[] = ['从本店调出', '调入本店'];
const dateTypeOptions: DateType[] = ['调拨日期', '调拨入库日期'];
const statusTree: TreeNode[] = [
  { value: '草稿', label: '草稿' },
  { value: '已提交', label: '已提交' },
  { value: '已审核', label: '已审核' },
  { value: '已取消', label: '已取消' },
];

const query = reactive({
  statisticMode: '从本店调出' as StatisticMode,
  dateType: '调拨日期' as DateType,
  dateRange: [] as string[],
  targetStore: '',
  sourceStore: '',
  itemName: '',
  itemCategory: '',
  sourceWarehouse: '',
  targetWarehouse: '',
  documentStatus: '',
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<TransferDetailRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const isOutboundMode = computed(() => query.statisticMode === '从本店调出');

const storeTree = computed<TreeNode[]>(() => {
  const normalize = (node: OrgNode): TreeNode | null => {
    if (node.type === 'store') {
      return {
        value: node.id,
        label: node.code ? `${node.name} / ${node.code}` : node.name,
      };
    }
    const children = (node.children ?? [])
      .map((child) => normalize(child))
      .filter((child): child is TreeNode => Boolean(child));
    if (!children.length) {
      return null;
    }
    return {
      value: node.id,
      label: node.name,
      children,
    };
  };
  return sessionStore.rootGroups
    .map((node) => normalize(node))
    .filter((node): node is TreeNode => Boolean(node));
});

const columns: ReportColumn[] = [
  { key: 'transferNo', label: '调拨单号', minWidth: 150, fixed: 'left' },
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'documentStatus', label: '单据状态', minWidth: 100 },
  { key: 'transferDate', label: '调拨日期', minWidth: 120 },
  { key: 'outboundAuditTime', label: '调拨出库审核时间', minWidth: 170 },
  { key: 'sourceStore', label: '调出门店', minWidth: 130 },
  { key: 'sourceWarehouse', label: '调出仓库', minWidth: 130 },
  { key: 'inboundDate', label: '调拨入库日期', minWidth: 130 },
  { key: 'inboundAuditTime', label: '调拨入库审核时间', minWidth: 170 },
  { key: 'targetStore', label: '调入门店', minWidth: 130 },
  { key: 'targetWarehouse', label: '调入仓库', minWidth: 130 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'baseUnit', label: '基准单位', minWidth: 100 },
  { key: 'transferBaseQty', label: '调拨数量（基准单位）', minWidth: 170, align: 'right' },
  { key: 'businessUnit', label: '业务单位', minWidth: 100 },
  { key: 'transferQty', label: '调拨数量', minWidth: 110, align: 'right' },
  { key: 'inboundAmountTaxIncluded', label: '调入金额（含税）', minWidth: 150, align: 'right' },
  { key: 'outboundCostAmountExTax', label: '调出成本金额（不含税）', minWidth: 180, align: 'right' },
  { key: 'outboundSettlementAmountTaxIncluded', label: '调出结算金额（含税）', minWidth: 180, align: 'right' },
  { key: 'inboundPriceTaxIncluded', label: '调入单价（含税）', minWidth: 150, align: 'right' },
  { key: 'outboundCostPriceExTax', label: '调出成本单价（不含税）', minWidth: 180, align: 'right' },
  { key: 'outboundSettlementPriceTaxIncluded', label: '调出结算单价（含税）', minWidth: 180, align: 'right' },
  { key: 'remark', label: '备注', minWidth: 160 },
];

const quantityKeys: Array<keyof TransferDetailRow> = ['transferBaseQty', 'transferQty'];
const moneyKeys: Array<keyof TransferDetailRow> = [
  'inboundAmountTaxIncluded',
  'outboundCostAmountExTax',
  'outboundSettlementAmountTaxIncluded',
  'inboundPriceTaxIncluded',
  'outboundCostPriceExTax',
  'outboundSettlementPriceTaxIncluded',
];

const normalizeCategoryTree = (nodes: ItemCategoryTreeNode[]): TreeNode[] => nodes.map((node) => ({
  value: node.label,
  label: node.label,
  children: node.children?.length ? normalizeCategoryTree(node.children) : undefined,
}));

const formatCell = (row: TransferDetailRow, key: keyof TransferDetailRow) => {
  const value = row[key];
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return moneyKeys.includes(key) ? value.toFixed(2) : value.toFixed(4);
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await loadWarehouseTree();
    if (!orgId) {
      itemCategoryTree.value = [];
      return;
    }
    const categoryRows = await fetchItemCategoryTreeApi(orgId);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
  } catch {
    itemCategoryTree.value = [];
    ElMessage.error('机构间调拨明细表筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const fetchReport = async () => {
  loading.value = true;
  try {
    tableRows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.statisticMode = '从本店调出';
  query.dateType = '调拨日期';
  query.dateRange = [];
  query.targetStore = '';
  query.sourceStore = '';
  query.itemName = '';
  query.itemCategory = '';
  query.sourceWarehouse = '';
  query.targetWarehouse = '';
  query.documentStatus = '';
  currentPage.value = 1;
  await fetchReport();
};

const handlePageChange = async (page: number) => {
  currentPage.value = page;
  await fetchReport();
};

const handlePageSizeChange = async (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  await fetchReport();
};

const getSummaries = ({ columns: summaryColumns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return summaryColumns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    const property = column.property as keyof TransferDetailRow | undefined;
    if (!property || (!quantityKeys.includes(property) && !moneyKeys.includes(property))) {
      return '';
    }
    const totalValue = tableRows.value.reduce((sum, row) => sum + Number(row[property] ?? 0), 0);
    return moneyKeys.includes(property) ? totalValue.toFixed(2) : totalValue.toFixed(4);
  });
};

watch(
  () => query.statisticMode,
  () => {
    query.sourceStore = '';
    query.targetStore = '';
    query.sourceWarehouse = '';
    query.targetWarehouse = '';
    currentPage.value = 1;
  },
);

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  async () => {
    await loadOptions();
    await fetchReport();
  },
);

onMounted(async () => {
  await loadOptions();
  await fetchReport();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="统计方式">
        <el-select v-model="query.statisticMode" style="width: 140px">
          <el-option v-for="option in statisticModeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="日期">
        <el-select v-model="query.dateType" style="width: 140px">
          <el-option v-for="option in dateTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="日期">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item v-if="isOutboundMode" label="调入门店">
        <el-tree-select
          v-model="query.targetStore"
          :data="storeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item v-else label="调出门店">
        <el-tree-select
          v-model="query.sourceStore"
          :data="storeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="物品名称">
        <el-input v-model="query.itemName" clearable placeholder="请输入" style="width: 160px" />
      </el-form-item>

      <el-form-item label="物品类别">
        <el-tree-select
          v-model="query.itemCategory"
          :data="itemCategoryTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          :loading="optionLoading"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item v-if="isOutboundMode" label="调出仓库">
        <el-tree-select
          v-model="query.sourceWarehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item v-else label="调入仓库">
        <el-tree-select
          v-model="query.targetWarehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="单据状态">
        <el-tree-select
          v-model="query.documentStatus"
          :data="statusTree"
          :props="{ label: 'label', value: 'value' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonTableSection
      :data="tableRows"
      row-key="id"
      :loading="loading"
      :height="460"
      :show-summary="true"
      :summary-method="getSummaries"
      empty-text="暂无机构间调拨明细数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatCell(row, column.key) }}
        </template>
      </el-table-column>
    </CommonTableSection>

    <div class="table-pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>
</template>
