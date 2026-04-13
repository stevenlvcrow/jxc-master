<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ArrowDown, ArrowUp, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout, { type PageTabItem } from '@/components/PageTabsLayout.vue';

type DocumentTimeType = '入库日期' | '创建时间';
type ItemTimeType = '移库日期' | '创建时间';
type DocumentStatus = '全部' | '草稿' | '已提交' | '已审核';
type TransferType = '全部' | '普通移库' | '退库移库' | '紧急调拨';
type InboundStatus = '全部' | '未入库' | '部分入库' | '已入库';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type TransferInboundDocumentRow = {
  id: number;
  documentCode: string;
  inboundDate: string;
  upstreamCode: string;
  outboundWarehouse: string;
  inboundWarehouse: string;
  amount: string;
  documentStatus: Exclude<DocumentStatus, '全部'>;
  transferType: Exclude<TransferType, '全部'>;
  printStatus: Exclude<PrintStatus, '全部'>;
  createdAt: string;
  creator: string;
  remark: string;
};
type TransferInboundItemRow = {
  id: number;
  documentCode: string;
  inboundDate: string;
  outboundWarehouse: string;
  inboundWarehouse: string;
  documentStatus: Exclude<DocumentStatus, '全部'>;
  transferType: Exclude<TransferType, '全部'>;
  inboundStatus: Exclude<InboundStatus, '全部'>;
  printStatus: Exclude<PrintStatus, '全部'>;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  unit: string;
  inboundQuantity: string;
  price: string;
  amount: string;
  createdAt: string;
  creator: string;
  remark: string;
};

const tabs: PageTabItem[] = [
  { key: 'document', label: '单据维度' },
  { key: 'item', label: '物品维度' },
];

const documentTimeTypeOptions: DocumentTimeType[] = ['入库日期', '创建时间'];
const itemTimeTypeOptions: ItemTimeType[] = ['移库日期', '创建时间'];
const documentStatusOptions: DocumentStatus[] = ['全部', '草稿', '已提交', '已审核'];
const transferTypeOptions: TransferType[] = ['全部', '普通移库', '退库移库', '紧急调拨'];
const inboundStatusOptions: InboundStatus[] = ['全部', '未入库', '部分入库', '已入库'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const warehouseTree: TreeNode[] = [
  {
    value: 'warehouse-root',
    label: '仓库中心',
    children: [
      { value: '中央成品仓', label: '中央成品仓' },
      { value: '北区原料仓', label: '北区原料仓' },
      { value: '南区包材仓', label: '南区包材仓' },
      { value: '东区备货仓', label: '东区备货仓' },
    ],
  },
];

const activeTab = ref('document');
const documentFiltersCollapsed = ref(true);
const itemFiltersCollapsed = ref(true);
const documentCurrentPage = ref(1);
const itemCurrentPage = ref(1);
const pageSize = ref(10);
const documentSelectedIds = ref<number[]>([]);
const itemSelectedIds = ref<number[]>([]);

const documentQuery = reactive({
  timeType: '入库日期' as DocumentTimeType,
  dateRange: [] as string[],
  outboundWarehouse: '',
  inboundWarehouse: '',
  documentCode: '',
  upstreamCode: '',
  itemName: '',
  documentStatus: '全部' as DocumentStatus,
  transferType: '全部' as TransferType,
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const itemQuery = reactive({
  timeType: '移库日期' as ItemTimeType,
  dateRange: [] as string[],
  outboundWarehouse: '',
  inboundWarehouse: '',
  documentCode: '',
  itemName: '',
  documentStatus: '全部' as DocumentStatus,
  transferType: '全部' as TransferType,
  inboundStatus: '全部' as InboundStatus,
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const documentRows: TransferInboundDocumentRow[] = [
  {
    id: 1,
    documentCode: 'YKRK-202604-001',
    inboundDate: '2026-04-13',
    upstreamCode: 'YK-202604-001',
    outboundWarehouse: '北区原料仓',
    inboundWarehouse: '中央成品仓',
    amount: '8,620.00',
    documentStatus: '已审核',
    transferType: '普通移库',
    printStatus: '已打印',
    createdAt: '2026-04-13 10:12:00',
    creator: '张敏',
    remark: '中央厨房补货入库',
  },
  {
    id: 2,
    documentCode: 'YKRK-202604-002',
    inboundDate: '2026-04-12',
    upstreamCode: 'YK-202604-002',
    outboundWarehouse: '南区包材仓',
    inboundWarehouse: '东区备货仓',
    amount: '2,480.00',
    documentStatus: '已提交',
    transferType: '紧急调拨',
    printStatus: '未打印',
    createdAt: '2026-04-12 15:06:00',
    creator: '李娜',
    remark: '外卖高峰备货入库',
  },
  {
    id: 3,
    documentCode: 'YKRK-202604-003',
    inboundDate: '2026-04-11',
    upstreamCode: 'YK-202604-003',
    outboundWarehouse: '中央成品仓',
    inboundWarehouse: '北区原料仓',
    amount: '1,960.00',
    documentStatus: '草稿',
    transferType: '退库移库',
    printStatus: '未打印',
    createdAt: '2026-04-11 11:20:00',
    creator: '王磊',
    remark: '半成品回库入库',
  },
];

const itemRows: TransferInboundItemRow[] = [
  {
    id: 1,
    documentCode: 'YKRK-202604-001',
    inboundDate: '2026-04-13',
    outboundWarehouse: '北区原料仓',
    inboundWarehouse: '中央成品仓',
    documentStatus: '已审核',
    transferType: '普通移库',
    inboundStatus: '已入库',
    printStatus: '已打印',
    itemCode: 'I0001',
    itemName: '鸡胸肉',
    spec: '2kg/袋',
    category: '生鲜原料',
    unit: '袋',
    inboundQuantity: '16',
    price: '186.00',
    amount: '2,976.00',
    createdAt: '2026-04-13 10:12:00',
    creator: '张敏',
    remark: '中央厨房补货入库',
  },
  {
    id: 2,
    documentCode: 'YKRK-202604-001',
    inboundDate: '2026-04-13',
    outboundWarehouse: '北区原料仓',
    inboundWarehouse: '中央成品仓',
    documentStatus: '已审核',
    transferType: '普通移库',
    inboundStatus: '已入库',
    printStatus: '已打印',
    itemCode: 'I0002',
    itemName: '牛腩',
    spec: '1kg/袋',
    category: '生鲜原料',
    unit: '袋',
    inboundQuantity: '8',
    price: '218.00',
    amount: '1,744.00',
    createdAt: '2026-04-13 10:12:00',
    creator: '张敏',
    remark: '中央厨房补货入库',
  },
  {
    id: 3,
    documentCode: 'YKRK-202604-002',
    inboundDate: '2026-04-12',
    outboundWarehouse: '南区包材仓',
    inboundWarehouse: '东区备货仓',
    documentStatus: '已提交',
    transferType: '紧急调拨',
    inboundStatus: '部分入库',
    printStatus: '未打印',
    itemCode: 'I0101',
    itemName: '包装盒',
    spec: '1000ml',
    category: '包材',
    unit: '箱',
    inboundQuantity: '12',
    price: '68.00',
    amount: '816.00',
    createdAt: '2026-04-12 15:06:00',
    creator: '李娜',
    remark: '外卖高峰备货入库',
  },
];

const matchesDateRange = (dateRange: string[], businessDate: string, createdAt: string, isCreatedTime: boolean) => {
  if (dateRange.length !== 2) {
    return true;
  }
  const current = isCreatedTime ? createdAt : businessDate;
  return current >= dateRange[0] && current <= dateRange[1];
};

const documentFilteredRows = computed(() => {
  const codeKeyword = documentQuery.documentCode.trim().toLowerCase();
  const upstreamKeyword = documentQuery.upstreamCode.trim().toLowerCase();
  const remarkKeyword = documentQuery.remark.trim().toLowerCase();
  return documentRows.filter((row) => {
    const matchedDateRange = matchesDateRange(
      documentQuery.dateRange,
      row.inboundDate,
      row.createdAt.slice(0, 10),
      documentQuery.timeType === '创建时间',
    );
    const matchedOutboundWarehouse = !documentQuery.outboundWarehouse || row.outboundWarehouse === documentQuery.outboundWarehouse;
    const matchedInboundWarehouse = !documentQuery.inboundWarehouse || row.inboundWarehouse === documentQuery.inboundWarehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedUpstreamCode = !upstreamKeyword || row.upstreamCode.toLowerCase().includes(upstreamKeyword);
    const matchedItem = !documentQuery.itemName || row.remark.includes(documentQuery.itemName);
    const matchedDocumentStatus = documentQuery.documentStatus === '全部' || row.documentStatus === documentQuery.documentStatus;
    const matchedTransferType = documentQuery.transferType === '全部' || row.transferType === documentQuery.transferType;
    const matchedPrintStatus = documentQuery.printStatus === '全部' || row.printStatus === documentQuery.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDateRange
      && matchedOutboundWarehouse
      && matchedInboundWarehouse
      && matchedDocumentCode
      && matchedUpstreamCode
      && matchedItem
      && matchedDocumentStatus
      && matchedTransferType
      && matchedPrintStatus
      && matchedRemark;
  });
});

const itemFilteredRows = computed(() => {
  const codeKeyword = itemQuery.documentCode.trim().toLowerCase();
  const remarkKeyword = itemQuery.remark.trim().toLowerCase();
  return itemRows.filter((row) => {
    const matchedDateRange = matchesDateRange(
      itemQuery.dateRange,
      row.inboundDate,
      row.createdAt.slice(0, 10),
      itemQuery.timeType === '创建时间',
    );
    const matchedOutboundWarehouse = !itemQuery.outboundWarehouse || row.outboundWarehouse === itemQuery.outboundWarehouse;
    const matchedInboundWarehouse = !itemQuery.inboundWarehouse || row.inboundWarehouse === itemQuery.inboundWarehouse;
    const matchedDocumentCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedItem = !itemQuery.itemName || row.itemName === itemQuery.itemName;
    const matchedDocumentStatus = itemQuery.documentStatus === '全部' || row.documentStatus === itemQuery.documentStatus;
    const matchedTransferType = itemQuery.transferType === '全部' || row.transferType === itemQuery.transferType;
    const matchedInboundStatus = itemQuery.inboundStatus === '全部' || row.inboundStatus === itemQuery.inboundStatus;
    const matchedPrintStatus = itemQuery.printStatus === '全部' || row.printStatus === itemQuery.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDateRange
      && matchedOutboundWarehouse
      && matchedInboundWarehouse
      && matchedDocumentCode
      && matchedItem
      && matchedDocumentStatus
      && matchedTransferType
      && matchedInboundStatus
      && matchedPrintStatus
      && matchedRemark;
  });
});

const documentPagedRows = computed(() => {
  const start = (documentCurrentPage.value - 1) * pageSize.value;
  return documentFilteredRows.value.slice(start, start + pageSize.value);
});

const itemPagedRows = computed(() => {
  const start = (itemCurrentPage.value - 1) * pageSize.value;
  return itemFilteredRows.value.slice(start, start + pageSize.value);
});

const handleDocumentSearch = () => {
  documentCurrentPage.value = 1;
};

const handleItemSearch = () => {
  itemCurrentPage.value = 1;
};

const handleDocumentReset = () => {
  documentQuery.timeType = '入库日期';
  documentQuery.dateRange = [];
  documentQuery.outboundWarehouse = '';
  documentQuery.inboundWarehouse = '';
  documentQuery.documentCode = '';
  documentQuery.upstreamCode = '';
  documentQuery.itemName = '';
  documentQuery.documentStatus = '全部';
  documentQuery.transferType = '全部';
  documentQuery.printStatus = '全部';
  documentQuery.remark = '';
  documentFiltersCollapsed.value = true;
  documentCurrentPage.value = 1;
};

const handleItemReset = () => {
  itemQuery.timeType = '移库日期';
  itemQuery.dateRange = [];
  itemQuery.outboundWarehouse = '';
  itemQuery.inboundWarehouse = '';
  itemQuery.documentCode = '';
  itemQuery.itemName = '';
  itemQuery.documentStatus = '全部';
  itemQuery.transferType = '全部';
  itemQuery.inboundStatus = '全部';
  itemQuery.printStatus = '全部';
  itemQuery.remark = '';
  itemFiltersCollapsed.value = true;
  itemCurrentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleDocumentSelectionChange = (rows: TransferInboundDocumentRow[]) => {
  documentSelectedIds.value = rows.map((row) => row.id);
};

const handleItemSelectionChange = (rows: TransferInboundItemRow[]) => {
  itemSelectedIds.value = rows.map((row) => row.id);
};

const handleDocumentPageChange = (page: number) => {
  documentCurrentPage.value = page;
};

const handleItemPageChange = (page: number) => {
  itemCurrentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  documentCurrentPage.value = 1;
  itemCurrentPage.value = 1;
};
</script>

<template>
  <section class="panel item-main-panel stock-transfer-inbound-page">
    <PageTabsLayout v-model:active-tab="activeTab" :tabs="tabs" body-class="stock-transfer-inbound-page__body">
      <template v-if="activeTab === 'document'">
        <CommonQuerySection :model="documentQuery">
          <el-form-item label="时间类型">
            <el-select v-model="documentQuery.timeType" style="width: 120px">
              <el-option v-for="option in documentTimeTypeOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="documentQuery.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="~"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item label="出库仓库">
            <el-tree-select
              v-model="documentQuery.outboundWarehouse"
              :data="warehouseTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              clearable
              check-strictly
              default-expand-all
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="入库仓库">
            <el-tree-select
              v-model="documentQuery.inboundWarehouse"
              :data="warehouseTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              clearable
              check-strictly
              default-expand-all
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="单据编号">
            <el-input v-model="documentQuery.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
          </el-form-item>
          <template v-if="!documentFiltersCollapsed">
            <el-form-item label="上游单据号">
              <el-input v-model="documentQuery.upstreamCode" placeholder="请输入上游单据号" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="物品">
              <el-select v-model="documentQuery.itemName" clearable style="width: 120px">
                <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="单据状态">
              <el-select v-model="documentQuery.documentStatus" style="width: 120px">
                <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="移库类型">
              <el-select v-model="documentQuery.transferType" style="width: 120px">
                <el-option v-for="option in transferTypeOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="打印状态">
              <el-select v-model="documentQuery.printStatus" style="width: 120px">
                <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="documentQuery.remark" placeholder="请输入备注" clearable style="width: 160px" />
            </el-form-item>
          </template>
          <el-form-item>
            <el-button text type="primary" @click="documentFiltersCollapsed = !documentFiltersCollapsed">
              <el-icon>
                <ArrowDown v-if="documentFiltersCollapsed" />
                <ArrowUp v-else />
              </el-icon>
              {{ documentFiltersCollapsed ? '展开筛选' : '收起筛选' }}
            </el-button>
            <el-button type="primary" @click="handleDocumentSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleDocumentReset">
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </CommonQuerySection>

        <div class="table-toolbar">
          <el-button @click="handleToolbarAction('批量打印')">
            <el-icon><Printer /></el-icon>
            批量打印
          </el-button>
          <el-button @click="handleToolbarAction('批量审核')">批量审核</el-button>
          <el-button @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
        </div>

        <el-table
          :data="documentPagedRows"
          border
          stripe
          class="erp-table"
          :fit="false"
          :height="360"
          :empty-text="'当前机构暂无数据'"
          @selection-change="handleDocumentSelectionChange"
        >
          <el-table-column type="selection" width="44" fixed="left" />
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column prop="documentCode" label="单据编号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="inboundDate" label="入库日期" min-width="110" show-overflow-tooltip />
          <el-table-column prop="upstreamCode" label="上游单据号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="outboundWarehouse" label="出库仓库" min-width="120" show-overflow-tooltip />
          <el-table-column prop="inboundWarehouse" label="入库仓库" min-width="120" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
          <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="transferType" label="移库类型" min-width="100" show-overflow-tooltip />
          <el-table-column prop="printStatus" label="打印状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
          <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleToolbarAction(`查看：${row.documentCode}`)">查看</el-button>
              <el-button text @click="handleToolbarAction(`编辑：${row.documentCode}`)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <div class="table-pagination-meta">已选 {{ documentSelectedIds.length }} 条</div>
          <el-pagination
            :current-page="documentCurrentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="documentFilteredRows.length"
            background
            small
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleDocumentPageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </template>

      <template v-else>
        <CommonQuerySection :model="itemQuery">
          <el-form-item label="时间类型">
            <el-select v-model="itemQuery.timeType" style="width: 120px">
              <el-option v-for="option in itemTimeTypeOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="itemQuery.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="~"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item label="出库仓库">
            <el-tree-select
              v-model="itemQuery.outboundWarehouse"
              :data="warehouseTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              clearable
              check-strictly
              default-expand-all
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="入库仓库">
            <el-tree-select
              v-model="itemQuery.inboundWarehouse"
              :data="warehouseTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              clearable
              check-strictly
              default-expand-all
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="单据编号">
            <el-input v-model="itemQuery.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
          </el-form-item>
          <template v-if="!itemFiltersCollapsed">
            <el-form-item label="物品">
              <el-select v-model="itemQuery.itemName" clearable style="width: 120px">
                <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="单据状态">
              <el-select v-model="itemQuery.documentStatus" style="width: 120px">
                <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="移库类型">
              <el-select v-model="itemQuery.transferType" style="width: 120px">
                <el-option v-for="option in transferTypeOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="入库状态">
              <el-select v-model="itemQuery.inboundStatus" style="width: 120px">
                <el-option v-for="option in inboundStatusOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="打印状态">
              <el-select v-model="itemQuery.printStatus" style="width: 120px">
                <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="itemQuery.remark" placeholder="请输入备注" clearable style="width: 160px" />
            </el-form-item>
          </template>
          <el-form-item>
            <el-button text type="primary" @click="itemFiltersCollapsed = !itemFiltersCollapsed">
              <el-icon>
                <ArrowDown v-if="itemFiltersCollapsed" />
                <ArrowUp v-else />
              </el-icon>
              {{ itemFiltersCollapsed ? '展开筛选' : '收起筛选' }}
            </el-button>
            <el-button type="primary" @click="handleItemSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleItemReset">
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </CommonQuerySection>

        <div class="table-toolbar">
          <el-button @click="handleToolbarAction('批量打印')">
            <el-icon><Printer /></el-icon>
            批量打印
          </el-button>
          <el-button @click="handleToolbarAction('批量审核')">批量审核</el-button>
          <el-button @click="handleToolbarAction('批量反审核')">批量反审核</el-button>
          <el-button @click="handleToolbarAction('批量导出物品明细')">批量导出物品明细</el-button>
        </div>

        <el-table
          :data="itemPagedRows"
          border
          stripe
          class="erp-table"
          :fit="false"
          :height="360"
          :empty-text="'当前机构暂无数据'"
          @selection-change="handleItemSelectionChange"
        >
          <el-table-column type="selection" width="44" fixed="left" />
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column prop="documentCode" label="单据编号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="inboundDate" label="入库日期" min-width="110" show-overflow-tooltip />
          <el-table-column prop="outboundWarehouse" label="出库仓库" min-width="120" show-overflow-tooltip />
          <el-table-column prop="inboundWarehouse" label="入库仓库" min-width="120" show-overflow-tooltip />
          <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="itemCode" label="物品编码" min-width="120" show-overflow-tooltip />
          <el-table-column prop="itemName" label="物品名称" min-width="130" show-overflow-tooltip />
          <el-table-column prop="spec" label="规格型号" min-width="120" show-overflow-tooltip />
          <el-table-column prop="category" label="物品类别" min-width="120" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" min-width="80" show-overflow-tooltip />
          <el-table-column prop="inboundQuantity" label="入库数量" min-width="90" show-overflow-tooltip />
          <el-table-column prop="price" label="单价" min-width="90" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
        </el-table>

        <div class="table-pagination">
          <div class="table-pagination-meta">已选 {{ itemSelectedIds.length }} 条</div>
          <el-pagination
            :current-page="itemCurrentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="itemFilteredRows.length"
            background
            small
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleItemPageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </template>
    </PageTabsLayout>
  </section>
</template>

<style scoped lang="scss">
.stock-transfer-inbound-page {
  padding: 8px;
}

.stock-transfer-inbound-page__body {
  padding-top: 10px;
}
</style>
