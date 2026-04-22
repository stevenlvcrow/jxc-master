<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ArrowDown, CloseBold, Download, Finished, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type DateType = '创建时间' | '收货日期' | '最后操作时间';
type ShipStatus = '未发货' | '部分发货' | '已发货';
type DocumentStatus = '待收货' | '已收货' | '已关闭' | '已取消';
type ReconciliationStatus = '未对账' | '部分对账' | '已对账';
type SupplierSplitStatus = '未分账' | '已分账';
type DocumentType = '普通收货' | '采购订单收货' | '退补收货';
type InspectionStatus = '无需质检' | '待质检' | '质检完成';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseReceiptRow = {
  id: number;
  receiptDate: string;
  receiptCode: string;
  purchaseOrderCode: string;
  supplier: string;
  expectedArrivalDate: string;
  receiptWarehouse: string;
  itemCount: number;
  receiptAmount: number;
  shipStatus: ShipStatus;
  documentStatus: DocumentStatus;
  reconciliationStatus: ReconciliationStatus;
  documentType: DocumentType;
  inspectionCount: number;
  printStatus: Exclude<PrintStatus, '全部'>;
  creator: string;
  createdAt: string;
  lastOperator: string;
  lastOperatedAt: string;
  remark: string;
  supplierSplit: SupplierSplitStatus;
  inspectionStatus: InspectionStatus;
  adjustedPrice: boolean;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();

const dateTypeOptions: DateType[] = ['创建时间', '收货日期', '最后操作时间'];
const shipStatusOptions: ShipStatus[] = ['未发货', '部分发货', '已发货'];
const documentStatusTree: TreeNode[] = [
  { value: '待收货', label: '待收货' },
  { value: '已收货', label: '已收货' },
  { value: '已关闭', label: '已关闭' },
  { value: '已取消', label: '已取消' },
];
const reconciliationStatusOptions: ReconciliationStatus[] = ['未对账', '部分对账', '已对账'];
const supplierSplitOptions: SupplierSplitStatus[] = ['未分账', '已分账'];
const documentTypeOptions: DocumentType[] = ['普通收货', '采购订单收货', '退补收货'];
const inspectionStatusOptions: InspectionStatus[] = ['无需质检', '待质检', '质检完成'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const querySchemeOptions = ['系统默认方案'];
const creatorTree: TreeNode[] = [
  {
    value: 'purchase-team',
    label: '采购组',
    children: [
      { value: '张敏', label: '张敏' },
      { value: '李娜', label: '李娜' },
    ],
  },
  {
    value: 'warehouse-team',
    label: '仓储组',
    children: [
      { value: '王磊', label: '王磊' },
      { value: '赵晨', label: '赵晨' },
    ],
  },
];

const query = reactive({
  dateType: '创建时间' as DateType,
  dateRange: [] as string[],
  receiptCode: '',
  supplier: '',
  itemCode: '',
  purchaseOrderCode: '',
  receiptWarehouse: '',
  shipStatus: '',
  documentStatus: '',
  reconciliationStatus: '',
  supplierSplit: '',
  documentType: '',
  inspectionStatus: '',
  creator: '',
  adjustedPrice: false,
  printStatus: '全部' as PrintStatus,
  remark: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const tableData = ref<PurchaseReceiptRow[]>([
  {
    id: 1,
    receiptDate: '2026-04-22',
    receiptCode: 'PR-202604-001',
    purchaseOrderCode: 'PO-202604-001',
    supplier: '鲜达食品',
    expectedArrivalDate: '2026-04-24',
    receiptWarehouse: '中央成品仓',
    itemCount: 2,
    receiptAmount: 4260,
    shipStatus: '部分发货',
    documentStatus: '已收货',
    reconciliationStatus: '部分对账',
    documentType: '采购订单收货',
    inspectionCount: 1,
    printStatus: '已打印',
    creator: '张敏',
    createdAt: '2026-04-22 10:15:00',
    lastOperator: '王磊',
    lastOperatedAt: '2026-04-22 16:30:00',
    remark: '首批到货',
    supplierSplit: '未分账',
    inspectionStatus: '质检完成',
    adjustedPrice: false,
  },
  {
    id: 2,
    receiptDate: '2026-04-23',
    receiptCode: 'PR-202604-002',
    purchaseOrderCode: 'PO-202604-002',
    supplier: '优选农场',
    expectedArrivalDate: '2026-04-25',
    receiptWarehouse: '北区原料仓',
    itemCount: 1,
    receiptAmount: 1820,
    shipStatus: '未发货',
    documentStatus: '待收货',
    reconciliationStatus: '未对账',
    documentType: '采购订单收货',
    inspectionCount: 0,
    printStatus: '未打印',
    creator: '李娜',
    createdAt: '2026-04-23 09:35:00',
    lastOperator: '李娜',
    lastOperatedAt: '2026-04-23 09:35:00',
    remark: '等待供应商发货',
    supplierSplit: '未分账',
    inspectionStatus: '待质检',
    adjustedPrice: true,
  },
  {
    id: 3,
    receiptDate: '2026-04-23',
    receiptCode: 'PR-202604-003',
    purchaseOrderCode: 'PO-202604-003',
    supplier: '盒马包材',
    expectedArrivalDate: '2026-04-26',
    receiptWarehouse: '南区包材仓',
    itemCount: 1,
    receiptAmount: 768,
    shipStatus: '已发货',
    documentStatus: '已关闭',
    reconciliationStatus: '已对账',
    documentType: '普通收货',
    inspectionCount: 0,
    printStatus: '未打印',
    creator: '赵晨',
    createdAt: '2026-04-23 13:08:00',
    lastOperator: '赵晨',
    lastOperatedAt: '2026-04-23 14:02:00',
    remark: '包材补货',
    supplierSplit: '已分账',
    inspectionStatus: '无需质检',
    adjustedPrice: false,
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
    ElMessage.error('采购收货单筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const filteredRows = computed(() => {
  const receiptCodeKeyword = query.receiptCode.trim().toLowerCase();
  const purchaseOrderCodeKeyword = query.purchaseOrderCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  const startDate = query.dateRange[0];
  const endDate = query.dateRange[1];
  return tableData.value.filter((row) => {
    const dateValue = query.dateType === '收货日期' ? row.receiptDate : query.dateType === '最后操作时间'
      ? row.lastOperatedAt.slice(0, 10)
      : row.createdAt.slice(0, 10);
    const matchedDate = (!startDate || dateValue >= startDate) && (!endDate || dateValue <= endDate);
    const matchedReceiptCode = !receiptCodeKeyword || row.receiptCode.toLowerCase().includes(receiptCodeKeyword);
    const matchedSupplier = !query.supplier || row.supplier === query.supplier;
    const matchedItem = !query.itemCode || row.itemCount > 0;
    const matchedOrderCode = !purchaseOrderCodeKeyword || row.purchaseOrderCode.toLowerCase().includes(purchaseOrderCodeKeyword);
    const matchedWarehouse = !query.receiptWarehouse || row.receiptWarehouse === query.receiptWarehouse;
    const matchedShip = !query.shipStatus || row.shipStatus === query.shipStatus;
    const matchedDocument = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedReconciliation = !query.reconciliationStatus || row.reconciliationStatus === query.reconciliationStatus;
    const matchedSplit = !query.supplierSplit || row.supplierSplit === query.supplierSplit;
    const matchedType = !query.documentType || row.documentType === query.documentType;
    const matchedInspection = !query.inspectionStatus || row.inspectionStatus === query.inspectionStatus;
    const matchedCreator = !query.creator || row.creator === query.creator;
    const matchedAdjusted = !query.adjustedPrice || row.adjustedPrice;
    const matchedPrint = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDate
      && matchedReceiptCode
      && matchedSupplier
      && matchedItem
      && matchedOrderCode
      && matchedWarehouse
      && matchedShip
      && matchedDocument
      && matchedReconciliation
      && matchedSplit
      && matchedType
      && matchedInspection
      && matchedCreator
      && matchedAdjusted
      && matchedPrint
      && matchedRemark;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const formatMoney = (value: number) => value.toFixed(2);

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.dateType = '创建时间';
  query.dateRange = [];
  query.receiptCode = '';
  query.supplier = '';
  query.itemCode = '';
  query.purchaseOrderCode = '';
  query.receiptWarehouse = '';
  query.shipStatus = '';
  query.documentStatus = '';
  query.reconciliationStatus = '';
  query.supplierSplit = '';
  query.documentType = '';
  query.inspectionStatus = '';
  query.creator = '';
  query.adjustedPrice = false;
  query.printStatus = '全部';
  query.remark = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: PurchaseReceiptRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleDropdownCommand = (command: string | number | object) => {
  ElMessage.info(`${String(command)}功能待接入`);
};

const handleView = (row: PurchaseReceiptRow) => {
  ElMessage.info(`查看：${row.receiptCode}`);
};

const handleEdit = (row: PurchaseReceiptRow) => {
  ElMessage.info(`编辑：${row.receiptCode}`);
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
      <el-form-item label="日期">
        <el-select v-model="query.dateType" style="width: 130px">
          <el-option v-for="option in dateTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="开始日期~结束日期">
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

      <el-form-item label="收货单号">
        <el-input v-model="query.receiptCode" clearable placeholder="请输入收货单号" style="width: 180px" />
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

      <el-form-item label="采购单号">
        <el-input v-model="query.purchaseOrderCode" clearable placeholder="请输入采购单号" style="width: 180px" />
      </el-form-item>

      <el-form-item label="收货仓库">
        <el-select
          v-model="query.receiptWarehouse"
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

      <el-form-item label="发货状态">
        <el-select v-model="query.shipStatus" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in shipStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="单据状态">
        <el-tree-select
          v-model="query.documentStatus"
          :data="documentStatusTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          placeholder="请选择"
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item label="对账状态">
        <el-select v-model="query.reconciliationStatus" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in reconciliationStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="供应商分账">
        <el-select v-model="query.supplierSplit" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in supplierSplitOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="单据类型">
        <el-select v-model="query.documentType" clearable placeholder="请选择" style="width: 150px">
          <el-option v-for="option in documentTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="质检状态">
        <el-select v-model="query.inspectionStatus" clearable placeholder="请选择" style="width: 140px">
          <el-option v-for="option in inspectionStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>

      <el-form-item label="创建人">
        <el-tree-select
          v-model="query.creator"
          :data="creatorTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 160px"
        />
      </el-form-item>

      <el-form-item label="被调过价">
        <el-checkbox v-model="query.adjustedPrice" />
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
      <el-button type="primary" :disabled="!selectedIds.length" @click="handleToolbarAction('批量收货')">
        <el-icon><Finished /></el-icon>
        批量收货
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量取消收货')">
        <el-icon><CloseBold /></el-icon>
        批量取消收货
      </el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量关闭')">批量关闭</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量取消关闭')">批量取消关闭</el-button>
      <el-dropdown @command="handleDropdownCommand">
        <el-button>
          <el-icon><Download /></el-icon>
          批量导出
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="批量导出(无明细)">无明细</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown @command="handleDropdownCommand">
        <el-button>
          <el-icon><Printer /></el-icon>
          单据打印
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="单据打印(无明细)">无明细</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
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
      <el-table-column prop="receiptDate" label="收货日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="receiptCode" label="收货单号" min-width="150" fixed="left" show-overflow-tooltip />
      <el-table-column prop="purchaseOrderCode" label="采购单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
      <el-table-column prop="expectedArrivalDate" label="期望到货日期" min-width="140" show-overflow-tooltip />
      <el-table-column prop="receiptWarehouse" label="收货仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" align="right" />
      <el-table-column prop="receiptAmount" label="收货金额（含税）" min-width="150" align="right">
        <template #default="{ row }">{{ formatMoney(row.receiptAmount) }}</template>
      </el-table-column>
      <el-table-column prop="shipStatus" label="发货状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="reconciliationStatus" label="对账状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentType" label="单据类型" min-width="120" show-overflow-tooltip />
      <el-table-column prop="inspectionCount" label="质检次数" min-width="100" align="right" />
      <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="lastOperator" label="最后操作人" min-width="110" show-overflow-tooltip />
      <el-table-column prop="lastOperatedAt" label="最后操作时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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
