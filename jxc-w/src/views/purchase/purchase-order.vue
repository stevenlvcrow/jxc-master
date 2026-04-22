<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import {
  Delete,
  Download,
  Plus,
  Printer,
  RefreshLeft,
  RefreshRight,
  Search,
  Upload,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout, { type PageTabItem } from '@/components/PageTabsLayout.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type ActiveTab = 'document' | 'detail';
type DateType = '订单日期' | '创建时间' | '期望到货日期';
type DocumentStatus = '草稿' | '已提交' | '已审核' | '已关闭';
type DocumentType = '普通采购' | '申请转单' | '智能采购';
type ShipStatus = '未发货' | '部分发货' | '已发货';
type ReceiveStatus = '未收货' | '部分收货' | '已收货';
type SplitReceiptStatus = '不拆分' | '手动拆分' | '自动拆分';
type PrintStatus = '全部' | '未打印' | '已打印';
type GiftStatus = '是' | '否';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseOrderDocumentRow = {
  id: number;
  orderDate: string;
  documentCode: string;
  purchaseOrg: string;
  supplier: string;
  warehouse: string;
  sourceDocumentCode: string;
  orderAmount: number;
  documentStatus: DocumentStatus;
  shipStatus: ShipStatus;
  receiveStatus: ReceiveStatus;
  expectedArrivalDate: string;
  documentType: DocumentType;
  splitReceipt: SplitReceiptStatus;
  printStatus: Exclude<PrintStatus, '全部'>;
  submitter: string;
  createdAt: string;
  remark: string;
};
type PurchaseOrderDetailRow = PurchaseOrderDocumentRow & {
  lineId: number;
  itemCode: string;
  itemName: string;
  purchaseUnit: string;
  purchaseQty: number;
  receivedQty: number;
  isGift: GiftStatus;
  itemRemark: string;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const tabs: PageTabItem[] = [
  { key: 'document', label: '单据维度' },
  { key: 'detail', label: '明细维度' },
];
const activeTab = ref<ActiveTab>('document');

const dateTypeOptions: DateType[] = ['订单日期', '创建时间', '期望到货日期'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核', '已关闭'];
const documentTypeOptions: DocumentType[] = ['普通采购', '申请转单', '智能采购'];
const shipStatusOptions: ShipStatus[] = ['未发货', '部分发货', '已发货'];
const receiveStatusOptions: ReceiveStatus[] = ['未收货', '部分收货', '已收货'];
const splitReceiptOptions: SplitReceiptStatus[] = ['不拆分', '手动拆分', '自动拆分'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const querySchemeOptions = ['系统默认方案'];
const submitterTree: TreeNode[] = [
  {
    value: 'purchase-team',
    label: '采购组',
    children: [
      { value: '张敏', label: '张敏' },
      { value: '李娜', label: '李娜' },
    ],
  },
  {
    value: 'store-team',
    label: '门店组',
    children: [
      { value: '王磊', label: '王磊' },
      { value: '赵晨', label: '赵晨' },
    ],
  },
];

const query = reactive({
  dateType: '订单日期' as DateType,
  dateRange: [] as string[],
  documentCode: '',
  supplier: '',
  warehouse: '',
  documentStatus: '',
  itemCode: '',
  documentType: '',
  sourceDocumentCode: '',
  shipStatus: '',
  receiveStatus: '',
  submitter: '',
  splitReceipt: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const documentRows = ref<PurchaseOrderDocumentRow[]>([
  {
    id: 1,
    orderDate: '2026-04-20',
    documentCode: 'PO-202604-001',
    purchaseOrg: '华东采购中心',
    supplier: '鲜达食品',
    warehouse: '中央成品仓',
    sourceDocumentCode: 'PA-202604-001',
    orderAmount: 7340,
    documentStatus: '已审核',
    shipStatus: '部分发货',
    receiveStatus: '部分收货',
    expectedArrivalDate: '2026-04-24',
    documentType: '申请转单',
    splitReceipt: '手动拆分',
    printStatus: '已打印',
    submitter: '张敏',
    createdAt: '2026-04-20 15:10:00',
    remark: '周末备货',
  },
  {
    id: 2,
    orderDate: '2026-04-21',
    documentCode: 'PO-202604-002',
    purchaseOrg: '华东采购中心',
    supplier: '优选农场',
    warehouse: '北区原料仓',
    sourceDocumentCode: 'PA-202604-002',
    orderAmount: 3640,
    documentStatus: '已提交',
    shipStatus: '未发货',
    receiveStatus: '未收货',
    expectedArrivalDate: '2026-04-25',
    documentType: '普通采购',
    splitReceipt: '不拆分',
    printStatus: '未打印',
    submitter: '李娜',
    createdAt: '2026-04-21 09:42:00',
    remark: '按审核数量下单',
  },
  {
    id: 3,
    orderDate: '2026-04-21',
    documentCode: 'PO-202604-003',
    purchaseOrg: '华东采购中心',
    supplier: '盒马包材',
    warehouse: '南区包材仓',
    sourceDocumentCode: '-',
    orderAmount: 768,
    documentStatus: '草稿',
    shipStatus: '未发货',
    receiveStatus: '未收货',
    expectedArrivalDate: '2026-04-26',
    documentType: '智能采购',
    splitReceipt: '自动拆分',
    printStatus: '未打印',
    submitter: '王磊',
    createdAt: '2026-04-21 11:08:00',
    remark: '包材补货',
  },
]);

const detailRows = ref<PurchaseOrderDetailRow[]>([
  {
    ...documentRows.value[0],
    lineId: 101,
    itemCode: 'ITEM-001',
    itemName: '鸡胸肉',
    purchaseUnit: '箱',
    purchaseQty: 20,
    receivedQty: 12,
    isGift: '否',
    itemRemark: '冷链配送',
  },
  {
    ...documentRows.value[0],
    lineId: 102,
    itemCode: 'ITEM-002',
    itemName: '牛腩',
    purchaseUnit: '包',
    purchaseQty: 14,
    receivedQty: 5,
    isGift: '否',
    itemRemark: '按批次收货',
  },
  {
    ...documentRows.value[2],
    lineId: 103,
    itemCode: 'ITEM-003',
    itemName: '包装盒',
    purchaseUnit: '箱',
    purchaseQty: 8,
    receivedQty: 0,
    isGift: '否',
    itemRemark: '-',
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
    await Promise.all([loadWarehouseTree(), loadSupplierOptions()]);
    if (!orgId) {
      itemOptions.value = [];
      return;
    }
    const itemRows = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId));
    itemOptions.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemOptions.value = [];
    ElMessage.error('采购订单筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const matchCommonQuery = (row: PurchaseOrderDocumentRow) => {
  const documentCodeKeyword = query.documentCode.trim().toLowerCase();
  const sourceCodeKeyword = query.sourceDocumentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  const startDate = query.dateRange[0];
  const endDate = query.dateRange[1];
  const dateValue = query.dateType === '创建时间' ? row.createdAt.slice(0, 10) : query.dateType === '期望到货日期'
    ? row.expectedArrivalDate
    : row.orderDate;
  const matchedDate = (!startDate || dateValue >= startDate) && (!endDate || dateValue <= endDate);
  const matchedCode = !documentCodeKeyword || row.documentCode.toLowerCase().includes(documentCodeKeyword);
  const matchedSupplier = !query.supplier || row.supplier === query.supplier;
  const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
  const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
  const matchedType = !query.documentType || row.documentType === query.documentType;
  const matchedSource = !sourceCodeKeyword || row.sourceDocumentCode.toLowerCase().includes(sourceCodeKeyword);
  const matchedShip = !query.shipStatus || row.shipStatus === query.shipStatus;
  const matchedReceive = !query.receiveStatus || row.receiveStatus === query.receiveStatus;
  const matchedSubmitter = !query.submitter || row.submitter === query.submitter;
  const matchedSplit = !query.splitReceipt || row.splitReceipt === query.splitReceipt;
  const matchedPrint = query.printStatus === '全部' || row.printStatus === query.printStatus;
  const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
  return matchedDate
    && matchedCode
    && matchedSupplier
    && matchedWarehouse
    && matchedStatus
    && matchedType
    && matchedSource
    && matchedShip
    && matchedReceive
    && matchedSubmitter
    && matchedSplit
    && matchedPrint
    && matchedRemark;
};

const filteredDocumentRows = computed(() => documentRows.value.filter((row) => {
  const matchedItem = !query.itemCode || detailRows.value.some((detail) =>
    detail.documentCode === row.documentCode && detail.itemCode === query.itemCode);
  return matchCommonQuery(row) && matchedItem;
}));

const filteredDetailRows = computed(() => detailRows.value.filter((row) => {
  const matchedItem = !query.itemCode || row.itemCode === query.itemCode;
  return matchCommonQuery(row) && matchedItem;
}));

const activeRows = computed(() => (activeTab.value === 'document' ? filteredDocumentRows.value : filteredDetailRows.value));
const pagedDocumentRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredDocumentRows.value.slice(start, start + pageSize.value);
});
const pagedDetailRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredDetailRows.value.slice(start, start + pageSize.value);
});

const formatMoney = (value: number) => value.toFixed(2);
const formatQty = (value: number) => value.toFixed(2);

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.dateType = '订单日期';
  query.dateRange = [];
  query.documentCode = '';
  query.supplier = '';
  query.warehouse = '';
  query.documentStatus = '';
  query.itemCode = '';
  query.documentType = '';
  query.sourceDocumentCode = '';
  query.shipStatus = '';
  query.receiveStatus = '';
  query.submitter = '';
  query.splitReceipt = '';
  query.printStatus = '全部';
  query.remark = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: Array<PurchaseOrderDocumentRow | PurchaseOrderDetailRow>) => {
  selectedIds.value = rows.map((row) => ('lineId' in row ? row.lineId : row.id));
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleView = (row: PurchaseOrderDocumentRow) => {
  ElMessage.info(`查看：${row.documentCode}`);
};

const handleEdit = (row: PurchaseOrderDocumentRow) => {
  ElMessage.info(`编辑：${row.documentCode}`);
};

const handleTabChange = () => {
  selectedIds.value = [];
  currentPage.value = 1;
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const documentSummaries = ({ columns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return columns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    if (column.property === 'orderAmount') {
      return formatMoney(filteredDocumentRows.value.reduce((sum, row) => sum + row.orderAmount, 0));
    }
    return '';
  });
};

const detailSummaries = ({ columns }: { columns: Array<{ property?: string; type?: string }> }) => {
  let labelFilled = false;
  return columns.map((column) => {
    if (!labelFilled && column.type !== 'selection') {
      labelFilled = true;
      return '合计';
    }
    if (column.property === 'purchaseQty') {
      return formatQty(filteredDetailRows.value.reduce((sum, row) => sum + row.purchaseQty, 0));
    }
    if (column.property === 'receivedQty') {
      return formatQty(filteredDetailRows.value.reduce((sum, row) => sum + row.receivedQty, 0));
    }
    return '';
  });
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
    <PageTabsLayout v-model:active-tab="activeTab" :tabs="tabs" @change="handleTabChange">
      <CommonQuerySection :model="query">
        <el-form-item label="日期">
          <el-select v-model="query.dateType" style="width: 130px">
            <el-option v-for="option in dateTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>

        <el-form-item label="日期范围">
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

        <el-form-item label="单据编号">
          <el-input
            v-model="query.documentCode"
            clearable
            placeholder="根据单据编号进行检索"
            style="width: 210px"
          />
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

        <el-form-item label="单据状态">
          <el-select v-model="query.documentStatus" clearable placeholder="请选择" style="width: 140px">
            <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
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

        <el-form-item label="单据类型">
          <el-select v-model="query.documentType" clearable placeholder="请选择" style="width: 140px">
            <el-option v-for="option in documentTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="activeTab === 'document'" label="来源单据号">
          <el-input
            v-model="query.sourceDocumentCode"
            clearable
            placeholder="根据单据号进行检索"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="发货状态">
          <el-select v-model="query.shipStatus" clearable placeholder="请选择" style="width: 140px">
            <el-option v-for="option in shipStatusOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>

        <el-form-item label="收货状态">
          <el-select v-model="query.receiveStatus" clearable placeholder="请选择" style="width: 140px">
            <el-option v-for="option in receiveStatusOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>

        <el-form-item label="提交人">
          <el-tree-select
            v-model="query.submitter"
            :data="submitterTree"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            clearable
            check-strictly
            default-expand-all
            placeholder="请选择"
            style="width: 160px"
          />
        </el-form-item>

        <el-form-item label="手动拆分收货单相关选择">
          <el-select v-model="query.splitReceipt" clearable placeholder="请选择" style="width: 190px">
            <el-option v-for="option in splitReceiptOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>

        <el-form-item label="打印状态">
          <el-radio-group v-model="query.printStatus">
            <el-radio v-for="option in printStatusOptions" :key="option" :label="option" />
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="query.remark" clearable placeholder="请输入备注" style="width: 180px" />
        </el-form-item>

        <el-form-item label="查询方案">
          <el-select v-model="query.queryScheme" style="width: 150px">
            <el-option v-for="option in querySchemeOptions" :key="option" :label="option" :value="option" />
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
        <el-button @click="handleToolbarAction('批量导入')">
          <el-icon><Upload /></el-icon>
          批量导入
        </el-button>
        <el-button @click="handleToolbarAction('批量导出列表')">
          <el-icon><Download /></el-icon>
          批量导出列表
        </el-button>
        <el-button @click="handleToolbarAction('批量打印')">
          <el-icon><Printer /></el-icon>
          批量打印
        </el-button>
        <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量提交')">批量提交</el-button>
        <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量删除')">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量撤回')">
          <el-icon><RefreshLeft /></el-icon>
          批量撤回
        </el-button>
        <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
      </div>

      <el-table
        v-if="activeTab === 'document'"
        :data="pagedDocumentRows"
        border
        stripe
        class="erp-table"
        :fit="false"
        height="460"
        show-summary
        :summary-method="documentSummaries"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column type="index" label="序号" width="56" fixed="left" />
        <el-table-column prop="orderDate" label="订单日期" min-width="120" show-overflow-tooltip />
        <el-table-column prop="documentCode" label="单据编号" min-width="150" fixed="left" show-overflow-tooltip />
        <el-table-column prop="purchaseOrg" label="采购机构" min-width="130" show-overflow-tooltip />
        <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
        <el-table-column prop="warehouse" label="仓库" min-width="130" show-overflow-tooltip />
        <el-table-column prop="sourceDocumentCode" label="来源单据号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderAmount" label="订单金额（含税）" min-width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="shipStatus" label="发货状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="receiveStatus" label="收货状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="expectedArrivalDate" label="期望到货日期" min-width="140" show-overflow-tooltip />
        <el-table-column prop="documentType" label="单据类型" min-width="110" show-overflow-tooltip />
        <el-table-column prop="splitReceipt" label="手动拆分收货单" min-width="140" show-overflow-tooltip />
        <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="submitter" label="提交人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table
        v-else
        :data="pagedDetailRows"
        border
        stripe
        class="erp-table"
        :fit="false"
        height="460"
        show-summary
        :summary-method="detailSummaries"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column type="index" label="序号" width="56" fixed="left" />
        <el-table-column prop="orderDate" label="订单日期" min-width="120" show-overflow-tooltip />
        <el-table-column prop="documentCode" label="单据编号" min-width="150" fixed="left" show-overflow-tooltip />
        <el-table-column prop="purchaseOrg" label="采购机构" min-width="130" show-overflow-tooltip />
        <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sourceDocumentCode" label="来源单据号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="shipStatus" label="发货状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="receiveStatus" label="收货状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="expectedArrivalDate" label="期望到货日期" min-width="140" show-overflow-tooltip />
        <el-table-column prop="documentType" label="单据类型" min-width="110" show-overflow-tooltip />
        <el-table-column prop="splitReceipt" label="手动拆分收货单" min-width="140" show-overflow-tooltip />
        <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
        <el-table-column prop="submitter" label="提交人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
        <el-table-column prop="itemCode" label="物品编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="itemName" label="物品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="purchaseUnit" label="采购单位" min-width="100" show-overflow-tooltip />
        <el-table-column prop="purchaseQty" label="采购数量" min-width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.purchaseQty) }}</template>
        </el-table-column>
        <el-table-column prop="receivedQty" label="已收货数量" min-width="120" align="right">
          <template #default="{ row }">{{ formatQty(row.receivedQty) }}</template>
        </el-table-column>
        <el-table-column prop="isGift" label="是否赠品" min-width="100" show-overflow-tooltip />
        <el-table-column prop="warehouse" label="仓库" min-width="130" show-overflow-tooltip />
        <el-table-column prop="itemRemark" label="物品备注" min-width="160" show-overflow-tooltip />
      </el-table>

      <div class="table-pagination">
        <div class="table-pagination-meta">已选 {{ selectedIds.length }} 条</div>
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="activeRows.length"
          background
          small
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </PageTabsLayout>
  </section>
</template>
