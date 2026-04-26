<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import {
  fetchInventoryInoutDetailReportApi,
  type InventoryInoutDetailReportRow,
  type InventoryInoutDetailStatisticDimension,
} from '@/api/modules/inventory';
import {
  fetchItemCategoryTreeApi,
  fetchItemsApi,
  fetchItemStatisticsTypesApi,
  type ItemCategoryTreeNode,
  type ItemStatisticsTypeRow,
  type ItemVO,
} from '@/api/modules/item';

type UnitType = '业务单位' | '基准单位' | '库存单位';
type QueryScheme = '系统默认方案';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type ReportColumn = {
  key?: keyof InventoryInoutDetailReportRow;
  label: string;
  minWidth?: number;
  align?: 'left' | 'right';
  fixed?: 'left';
  children?: ReportColumn[];
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const statisticDimensionOptions: InventoryInoutDetailStatisticDimension[] = ['单据 + 物品', '单据'];
const warehouseTypeTree: TreeNode[] = [
  { value: '出品及生产部门', label: '出品及生产部门' },
  { value: '行政部门', label: '行政部门' },
  { value: '普通仓库', label: '普通仓库' },
];
const inoutTypeTree: TreeNode[] = [
  {
    value: '入库',
    label: '入库',
    children: [
      { value: '采购入库', label: '采购入库' },
      { value: '其他入库', label: '其他入库' },
      { value: '生产入库', label: '生产入库' },
      { value: '客户退货入库', label: '客户退货入库' },
    ],
  },
  {
    value: '出库',
    label: '出库',
    children: [
      { value: '采购退货出库', label: '采购退货出库' },
      { value: '报损出库', label: '报损出库' },
      { value: '其他出库', label: '其他出库' },
      { value: '客户销售出库', label: '客户销售出库' },
      { value: '移库出库', label: '移库出库' },
      { value: '菜品消耗出库', label: '菜品消耗出库' },
    ],
  },
];
const upstreamDocumentTypeTree: TreeNode[] = [
  { value: '采购订单', label: '采购订单' },
  { value: '销售订单', label: '销售订单' },
  { value: '调拨单', label: '调拨单' },
  { value: '盘点单', label: '盘点单' },
];
const adjustmentDocumentTree: TreeNode[] = [
  { value: '是', label: '是' },
  { value: '否', label: '否' },
];
const inoutDirectionTree: TreeNode[] = [
  { value: '入库', label: '入库' },
  { value: '出库', label: '出库' },
];
const giftTree: TreeNode[] = [
  { value: '是', label: '是' },
  { value: '否', label: '否' },
];
const crossMonthOptions = ['是', '否'];
const reasonTypeOptions = ['盘盈', '盘亏', '报损', '退货', '调拨', '调整', '赠品', '其他'];
const unitTypeOptions: UnitType[] = ['业务单位', '基准单位', '库存单位'];
const querySchemeOptions: QueryScheme[] = ['系统默认方案'];

const query = reactive({
  statisticDimension: '单据 + 物品' as InventoryInoutDetailStatisticDimension,
  warehouse: '',
  warehouseType: '',
  dateRange: [] as string[],
  dateText: '',
  auditTimeRange: [] as string[],
  inoutType: '',
  upstreamDocumentType: '',
  itemCategory: '',
  statisticType: '',
  itemCode: '',
  reasonType: '',
  adjustmentDocument: '',
  oppositeOrg: '',
  documentNo: '',
  crossMonthDocument: '',
  inoutDirection: '',
  gift: '',
  unitType: '业务单位' as UnitType,
  queryScheme: '系统默认方案' as QueryScheme,
});

const loading = ref(false);
const optionLoading = ref(false);
const tableRows = ref<InventoryInoutDetailReportRow[]>([]);
const itemCategoryTree = ref<TreeNode[]>([]);
const statisticTypeTree = ref<TreeNode[]>([]);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const currentOrgId = computed(() => String(sessionStore.currentOrgId ?? '').trim().toLowerCase());
const isDocumentItemDimension = computed(() => query.statisticDimension === '单据 + 物品');

const documentItemColumns: ReportColumn[] = [
  { key: 'itemCode', label: '物品编码', minWidth: 130, fixed: 'left' },
  { key: 'itemName', label: '物品名称', minWidth: 140, fixed: 'left' },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'itemCategory', label: '物品类别', minWidth: 120 },
  { key: 'statisticType', label: '统计类型', minWidth: 120 },
  { key: 'baseUnit', label: '基准单位', minWidth: 100 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'orgName', label: '机构', minWidth: 130 },
  { key: 'orgCode', label: '机构编码', minWidth: 120 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'warehouseType', label: '仓库类型', minWidth: 120 },
  { key: 'upstreamDocumentNo', label: '上游单据号', minWidth: 150 },
  { key: 'upstreamDocumentType', label: '上游单据类型', minWidth: 140 },
  { key: 'documentNo', label: '出入库单据号', minWidth: 150 },
  { key: 'inoutType', label: '出入库类型', minWidth: 130 },
  { key: 'reasonType', label: '原因类型', minWidth: 110 },
  { key: 'adjustmentDocument', label: '是否调整单', minWidth: 110 },
  { key: 'oppositeOrg', label: '对方机构', minWidth: 130 },
  { key: 'oppositeOrgCode', label: '对方机构编码', minWidth: 130 },
  { key: 'upstreamDocumentDate', label: '上游单据日期', minWidth: 130 },
  { key: 'documentDate', label: '出入库单据日期', minWidth: 140 },
  { key: 'documentCreatedAt', label: '出入库单据创建时间', minWidth: 170 },
  { key: 'documentCreator', label: '出入库单据创建人', minWidth: 150 },
  { key: 'documentAuditTime', label: '出入库单据审核时间', minWidth: 170 },
  {
    label: '入库',
    children: [
      { key: 'inboundBaseQty', label: '数量(基准单位)', minWidth: 130, align: 'right' },
      { key: 'inboundQty', label: '数量', minWidth: 100, align: 'right' },
      { key: 'inboundCostUnitPriceTaxIncluded', label: '成本单价(含税)', minWidth: 140, align: 'right' },
      { key: 'inboundCostAmountTaxIncluded', label: '成本金额(含税)', minWidth: 150, align: 'right' },
      { key: 'inboundSettlementUnitPriceTaxIncluded', label: '结算单价(含税)', minWidth: 140, align: 'right' },
      { key: 'inboundSettlementAmountTaxIncluded', label: '结算金额(含税)', minWidth: 150, align: 'right' },
      { key: 'inboundDiscountSettlementUnitPriceTaxIncluded', label: '折后结算单价(含税)', minWidth: 170, align: 'right' },
      { key: 'inboundDiscountSettlementAmountTaxIncluded', label: '折后结算金额(含税)', minWidth: 170, align: 'right' },
    ],
  },
  {
    label: '出库',
    children: [
      { key: 'outboundBaseQty', label: '数量(基准单位)', minWidth: 130, align: 'right' },
      { key: 'outboundQty', label: '数量', minWidth: 100, align: 'right' },
      { key: 'outboundCostUnitPriceTaxIncluded', label: '成本单价(含税)', minWidth: 140, align: 'right' },
      { key: 'outboundCostAmountTaxIncluded', label: '成本金额(含税)', minWidth: 150, align: 'right' },
      { key: 'outboundSettlementUnitPriceTaxIncluded', label: '结算单价(含税)', minWidth: 140, align: 'right' },
      { key: 'outboundSettlementAmountTaxIncluded', label: '结算金额(含税)', minWidth: 150, align: 'right' },
      { key: 'outboundDiscountSettlementUnitPriceTaxIncluded', label: '折后结算单价(含税)', minWidth: 170, align: 'right' },
      { key: 'outboundDiscountSettlementAmountTaxIncluded', label: '折后结算金额(含税)', minWidth: 170, align: 'right' },
      { key: 'outboundDiscountGrossProfitUnitPriceTaxIncluded', label: '折后毛利额(含税)', minWidth: 160, align: 'right' },
      { key: 'outboundDiscountGrossProfitTaxIncluded', label: '折后毛利率', minWidth: 120, align: 'right' },
    ],
  },
  { key: 'remark', label: '备注', minWidth: 160 },
  { key: 'returnDifferenceAmount', label: '退返货差异金额', minWidth: 140, align: 'right' },
];

const documentColumns: ReportColumn[] = [
  { key: 'documentNo', label: '出入库单据号', minWidth: 150, fixed: 'left' },
  { key: 'inoutType', label: '出入库类型', minWidth: 130 },
  { key: 'upstreamDocumentNo', label: '上游单据号', minWidth: 150 },
  { key: 'upstreamDocumentType', label: '上游单据类型', minWidth: 140 },
  { key: 'orgName', label: '机构', minWidth: 130 },
  { key: 'orgCode', label: '机构编码', minWidth: 120 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'warehouseType', label: '仓库类型', minWidth: 120 },
  { key: 'oppositeOrg', label: '对方机构', minWidth: 130 },
  { key: 'oppositeOrgCode', label: '对方机构编码', minWidth: 130 },
  { key: 'adjustmentDocument', label: '是否调整单', minWidth: 110 },
  { key: 'upstreamDocumentDate', label: '上游单据日期', minWidth: 130 },
  { key: 'documentDate', label: '出入库单据日期', minWidth: 140 },
  { key: 'documentCreatedAt', label: '出入库单据创建时间', minWidth: 170 },
  { key: 'documentCreator', label: '出入库单据创建人', minWidth: 150 },
  { key: 'documentAuditTime', label: '出入库单据审核时间', minWidth: 170 },
  { key: 'inboundCostAmountTaxIncluded', label: '入库成本金额 (含税)', minWidth: 160, align: 'right' },
  { key: 'inboundSettlementAmountTaxIncluded', label: '入库结算金额 (含税)', minWidth: 170, align: 'right' },
  { key: 'inboundDiscountSettlementAmountTaxIncluded', label: '入库折后结算金额 (含税)', minWidth: 190, align: 'right' },
  { key: 'outboundCostAmountTaxIncluded', label: '出库成本金额 (含税)', minWidth: 160, align: 'right' },
  { key: 'outboundSettlementAmountTaxIncluded', label: '出库结算金额 (含税)', minWidth: 170, align: 'right' },
  { key: 'outboundDiscountSettlementAmountTaxIncluded', label: '出库折后结算金额 (含税)', minWidth: 190, align: 'right' },
  { key: 'outboundDiscountGrossProfitTaxIncluded', label: '出库折后毛利额 (含税)', minWidth: 180, align: 'right' },
];

const visibleColumns = computed(() => (isDocumentItemDimension.value ? documentItemColumns : documentColumns));

const normalizeText = (value: unknown) => (typeof value === 'string' ? value.trim() : '');

const parseNumber = (value: unknown) => {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0;
  }
  const parsed = Number(normalizeText(value));
  return Number.isFinite(parsed) ? parsed : 0;
};

const moneyKeys: Array<keyof InventoryInoutDetailReportRow> = [
  'returnDifferenceAmount',
  'inboundCostUnitPriceTaxIncluded',
  'inboundCostAmountTaxIncluded',
  'inboundSettlementUnitPriceTaxIncluded',
  'inboundSettlementAmountTaxIncluded',
  'inboundDiscountSettlementUnitPriceTaxIncluded',
  'inboundDiscountSettlementAmountTaxIncluded',
  'outboundCostUnitPriceTaxIncluded',
  'outboundCostAmountTaxIncluded',
  'outboundSettlementUnitPriceTaxIncluded',
  'outboundSettlementAmountTaxIncluded',
  'outboundDiscountSettlementUnitPriceTaxIncluded',
  'outboundDiscountSettlementAmountTaxIncluded',
  'outboundDiscountGrossProfitUnitPriceTaxIncluded',
  'outboundDiscountGrossProfitTaxIncluded',
];

const quantityKeys: Array<keyof InventoryInoutDetailReportRow> = [
  'inboundBaseQty',
  'inboundQty',
  'outboundBaseQty',
  'outboundQty',
];

const columnRenderKey = (column: ReportColumn) => column.key ?? column.label;

const formatCell = (row: InventoryInoutDetailReportRow, key: keyof InventoryInoutDetailReportRow) => {
  if (moneyKeys.includes(key)) {
    return parseNumber(row[key]).toFixed(2);
  }
  if (quantityKeys.includes(key)) {
    const value = parseNumber(row[key]);
    return Number.isInteger(value) ? String(value) : value.toFixed(4);
  }
  return String(row[key] ?? '-');
};

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

const loadOptionData = async () => {
  if (!currentOrgId.value) {
    itemCategoryTree.value = [];
    statisticTypeTree.value = [];
    itemOptions.value = [];
    return;
  }
  optionLoading.value = true;
  try {
    const orgId = currentOrgId.value;
    const [categoryRows, statisticRows, itemRows] = await Promise.all([
      fetchItemCategoryTreeApi(orgId),
      fetchAllPages<ItemStatisticsTypeRow>((pageNo, pageSizeValue) =>
        fetchItemStatisticsTypesApi({ pageNo, pageSize: pageSizeValue }, orgId)),
      fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
        fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId)),
      loadWarehouseTree(),
    ]);
    itemCategoryTree.value = normalizeCategoryTree(categoryRows ?? []);
    statisticTypeTree.value = statisticRows.map((row) => ({
      value: row.name,
      label: `${row.code} / ${row.name}`,
    }));
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemCategoryTree.value = [];
    statisticTypeTree.value = [];
    itemOptions.value = [];
    ElMessage.error('出入库明细筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const buildParams = () => {
  const [startDate, endDate] = query.dateRange;
  const [auditStartTime, auditEndTime] = query.auditTimeRange;
  return {
    pageNo: currentPage.value,
    pageSize: pageSize.value,
    statisticDimension: query.statisticDimension,
    warehouse: query.warehouse || undefined,
    warehouseType: query.warehouseType || undefined,
    startDate,
    endDate,
    dateText: normalizeText(query.dateText) || undefined,
    auditStartTime,
    auditEndTime,
    inoutType: query.inoutType || undefined,
    upstreamDocumentType: query.upstreamDocumentType || undefined,
    itemCategory: isDocumentItemDimension.value && query.itemCategory ? query.itemCategory : undefined,
    statisticType: isDocumentItemDimension.value && query.statisticType ? query.statisticType : undefined,
    itemCode: isDocumentItemDimension.value && query.itemCode ? query.itemCode : undefined,
    reasonType: query.reasonType || undefined,
    adjustmentDocument: query.adjustmentDocument || undefined,
    oppositeOrg: normalizeText(query.oppositeOrg) || undefined,
    documentNo: normalizeText(query.documentNo) || undefined,
    crossMonthDocument: query.crossMonthDocument || undefined,
    inoutDirection: query.inoutDirection || undefined,
    gift: isDocumentItemDimension.value && query.gift ? query.gift : undefined,
    unitType: isDocumentItemDimension.value ? query.unitType : undefined,
    queryScheme: query.queryScheme,
  };
};

const fetchReport = async () => {
  if (!currentOrgId.value) {
    tableRows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const page = await fetchInventoryInoutDetailReportApi(buildParams(), currentOrgId.value);
    tableRows.value = page.list ?? [];
    total.value = Number(page.total ?? 0);
  } catch {
    tableRows.value = [];
    total.value = 0;
    ElMessage.error('出入库明细表加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await fetchReport();
};

const handleReset = async () => {
  query.warehouse = '';
  query.warehouseType = '';
  query.dateRange = [];
  query.dateText = '';
  query.auditTimeRange = [];
  query.inoutType = '';
  query.upstreamDocumentType = '';
  query.itemCategory = '';
  query.statisticType = '';
  query.itemCode = '';
  query.reasonType = '';
  query.adjustmentDocument = '';
  query.oppositeOrg = '';
  query.documentNo = '';
  query.crossMonthDocument = '';
  query.inoutDirection = '';
  query.gift = '';
  query.unitType = '业务单位';
  query.queryScheme = '系统默认方案';
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

watch(
  () => query.statisticDimension,
  async () => {
    currentPage.value = 1;
    await fetchReport();
  },
);

watch(
  () => sessionStore.currentOrgId,
  async () => {
    currentPage.value = 1;
    await loadOptionData();
    await fetchReport();
  },
);

onMounted(async () => {
  await loadOptionData();
  await fetchReport();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="统计维度">
        <el-select v-model="query.statisticDimension" style="width: 140px">
          <el-option v-for="option in statisticDimensionOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="无"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item label="仓库类型">
        <el-tree-select
          v-model="query.warehouseType"
          :data="warehouseTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="无"
          style="width: 150px"
        />
      </el-form-item>

      <el-form-item label="日期">
        <el-date-picker
          v-if="isDocumentItemDimension"
          v-model="query.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-input v-else v-model="query.dateText" clearable placeholder="无" style="width: 160px" />
      </el-form-item>

      <el-form-item v-if="isDocumentItemDimension" label="单据审核时间">
        <el-date-picker
          v-model="query.auditTimeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
      </el-form-item>

      <el-form-item label="出入库类型">
        <el-tree-select
          v-model="query.inoutType"
          :data="inoutTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="无"
          style="width: 170px"
        />
      </el-form-item>

      <el-form-item label="上游单据类型">
        <el-tree-select
          v-model="query.upstreamDocumentType"
          :data="upstreamDocumentTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="无"
          style="width: 160px"
        />
      </el-form-item>

      <el-form-item v-if="isDocumentItemDimension" label="物品类别">
        <el-tree-select
          v-model="query.itemCategory"
          :data="itemCategoryTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="无"
          style="width: 170px"
        />
      </el-form-item>

      <el-form-item v-if="isDocumentItemDimension" label="统计类型">
        <el-tree-select
          v-model="query.statisticType"
          :data="statisticTypeTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="无"
          style="width: 180px"
        />
      </el-form-item>

      <el-form-item v-if="isDocumentItemDimension" label="物品">
        <el-select
          v-model="query.itemCode"
          :loading="optionLoading"
          clearable
          filterable
          placeholder="无"
          style="width: 190px"
        >
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="原因类型">
        <el-select v-model="query.reasonType" clearable placeholder="无" style="width: 130px">
          <el-option v-for="option in reasonTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="是否调整单">
        <el-tree-select
          v-model="query.adjustmentDocument"
          :data="adjustmentDocumentTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          placeholder="无"
          style="width: 130px"
        />
      </el-form-item>

      <el-form-item label="对方机构">
        <el-input v-model="query.oppositeOrg" clearable placeholder="无" style="width: 150px" />
      </el-form-item>

      <el-form-item label="出入库单据号">
        <el-input v-model="query.documentNo" clearable placeholder="无" style="width: 170px" />
      </el-form-item>

      <el-form-item label="跨月单据">
        <el-select v-model="query.crossMonthDocument" clearable placeholder="无" style="width: 120px">
          <el-option v-for="option in crossMonthOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="出入库方向">
        <el-tree-select
          v-model="query.inoutDirection"
          :data="inoutDirectionTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          placeholder="无"
          style="width: 130px"
        />
      </el-form-item>

      <el-form-item v-if="isDocumentItemDimension" label="是否赠品">
        <el-tree-select
          v-model="query.gift"
          :data="giftTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          placeholder="无"
          style="width: 120px"
        />
      </el-form-item>

      <el-form-item v-if="isDocumentItemDimension" label="单位类型">
        <el-select v-model="query.unitType" style="width: 130px">
          <el-option v-for="option in unitTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
        </el-select>
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
      :height="420"
      empty-text="暂无出入库明细数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <template v-for="column in visibleColumns" :key="columnRenderKey(column)">
        <el-table-column v-if="column.children?.length" :label="column.label" :fixed="column.fixed">
          <el-table-column
            v-for="child in column.children"
            :key="columnRenderKey(child)"
            :prop="child.key"
            :label="child.label"
            :min-width="child.minWidth"
            :align="child.align"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ child.key ? formatCell(row, child.key) : '-' }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column
          v-else
          :prop="column.key"
          :label="column.label"
          :min-width="column.minWidth"
          :align="column.align"
          :fixed="column.fixed"
          show-overflow-tooltip
        >
        <template #default="{ row }">
          {{ column.key ? formatCell(row, column.key) : '-' }}
        </template>
        </el-table-column>
      </template>
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
