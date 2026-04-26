<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Download, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type TimeType = '订单日期' | '预计发货日期' | '预计到货日期' | '期望到货日期' | '单据提交时间' | '付款时间';
type DocumentStatus = '草稿' | '已提交' | '已确认' | '已关闭';
type PresaleStatus = '全部' | '是' | '否';
type SubstituteStatus = '全部' | '是' | '否';
type PaymentStatus = '未付款' | '部分付款' | '已付款';
type CreateMode = '手工创建' | '模板生成' | '代店下单';
type LogisticsMode = '自提' | '配送' | '三方物流';
type DocumentTag = '常规' | '加急' | '活动';
type PrintStatus = '全部' | '未打印' | '已打印';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type OrderDocumentRow = {
  id: number;
  documentCode: string;
  orderDate: string;
  paymentAmount: number;
  orderAmount: number;
  freightAmount: number;
  documentStatus: DocumentStatus;
  logisticsMode: LogisticsMode;
  createMode: CreateMode;
  paymentStatus: PaymentStatus;
  paymentInfo: string;
  downstreamDocument: string;
  expectedArrivalDate: string;
  submittedAt: string;
  estimatedShipDate: string;
  estimatedArrivalDate: string;
  creator: string;
  orderTemplate: string;
  supplierOrg: string;
  isPresale: Exclude<PresaleStatus, '全部'>;
  substituteOrder: Exclude<SubstituteStatus, '全部'>;
  documentTag: DocumentTag;
  printStatus: Exclude<PrintStatus, '全部'>;
  remark: string;
};

const sessionStore = useSessionStore();
const router = useRouter();

const timeTypeOptions: TimeType[] = ['订单日期', '预计发货日期', '预计到货日期', '期望到货日期', '单据提交时间', '付款时间'];
const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已确认', '已关闭'];
const yesNoOptions = ['全部', '是', '否'] as const;
const paymentStatusOptions: PaymentStatus[] = ['未付款', '部分付款', '已付款'];
const createModeOptions: CreateMode[] = ['手工创建', '模板生成', '代店下单'];
const orderTemplateOptions = ['系统默认模板', '门店补货模板', '活动订货模板'];
const logisticsModeOptions: LogisticsMode[] = ['自提', '配送', '三方物流'];
const documentTagOptions: DocumentTag[] = ['常规', '加急', '活动'];
const printStatusOptions: PrintStatus[] = ['全部', '未打印', '已打印'];

const query = reactive({
  timeType: '订单日期' as TimeType,
  dateRange: [] as string[],
  documentCode: '',
  documentStatus: '',
  isPresale: '全部' as PresaleStatus,
  supplierOrg: '',
  substituteOrder: '全部' as SubstituteStatus,
  paymentStatus: '',
  itemCode: '',
  createMode: '',
  orderTemplate: '',
  logisticsMode: '',
  documentTag: '',
  printStatus: '全部' as PrintStatus,
  remark: '',
});

const loading = ref(false);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const supplierOrgTree = computed<TreeNode[]>(() => sessionStore.rootGroups.map((node) => ({
  value: node.id,
  label: node.name,
  children: node.children?.map((child) => ({
    value: child.id,
    label: child.name,
  })),
})));

const tableData = ref<OrderDocumentRow[]>([]);

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
    ElMessage.error('订货单筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const resolveDateValue = (row: OrderDocumentRow) => {
  if (query.timeType === '预计发货日期') {
    return row.estimatedShipDate;
  }
  if (query.timeType === '预计到货日期') {
    return row.estimatedArrivalDate;
  }
  if (query.timeType === '期望到货日期') {
    return row.expectedArrivalDate;
  }
  if (query.timeType === '单据提交时间') {
    return row.submittedAt.slice(0, 10);
  }
  if (query.timeType === '付款时间') {
    return row.orderDate;
  }
  return row.orderDate;
};

const filteredRows = computed(() => {
  const documentCodeKeyword = query.documentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  const startDate = query.dateRange[0];
  const endDate = query.dateRange[1];
  return tableData.value.filter((row) => {
    const dateValue = resolveDateValue(row);
    const matchedDate = (!startDate || dateValue >= startDate) && (!endDate || dateValue <= endDate);
    const matchedCode = !documentCodeKeyword || row.documentCode.toLowerCase().includes(documentCodeKeyword);
    const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedPresale = query.isPresale === '全部' || row.isPresale === query.isPresale;
    const matchedSupplierOrg = !query.supplierOrg || row.supplierOrg === query.supplierOrg;
    const matchedSubstitute = query.substituteOrder === '全部' || row.substituteOrder === query.substituteOrder;
    const matchedPayment = !query.paymentStatus || row.paymentStatus === query.paymentStatus;
    const matchedItem = !query.itemCode || row.orderAmount > 0;
    const matchedCreateMode = !query.createMode || row.createMode === query.createMode;
    const matchedTemplate = !query.orderTemplate || row.orderTemplate === query.orderTemplate;
    const matchedLogistics = !query.logisticsMode || row.logisticsMode === query.logisticsMode;
    const matchedTag = !query.documentTag || row.documentTag === query.documentTag;
    const matchedPrint = query.printStatus === '全部' || row.printStatus === query.printStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedDate
      && matchedCode
      && matchedStatus
      && matchedPresale
      && matchedSupplierOrg
      && matchedSubstitute
      && matchedPayment
      && matchedItem
      && matchedCreateMode
      && matchedTemplate
      && matchedLogistics
      && matchedTag
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
  query.timeType = '订单日期';
  query.dateRange = [];
  query.documentCode = '';
  query.documentStatus = '';
  query.isPresale = '全部';
  query.supplierOrg = '';
  query.substituteOrder = '全部';
  query.paymentStatus = '';
  query.itemCode = '';
  query.createMode = '';
  query.orderTemplate = '';
  query.logisticsMode = '';
  query.documentTag = '';
  query.printStatus = '全部';
  query.remark = '';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: OrderDocumentRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleToolbarAction = (action: string) => {
  if (action === '新增') {
    router.push('/order/order-documents/create');
    return;
  }
};

const handleView = (row: OrderDocumentRow) => {
  router.push(`/order/order-documents/view/${row.id}`);
};

const handleEdit = (row: OrderDocumentRow) => {
  router.push(`/order/order-documents/edit/${row.id}`);
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
      <el-form-item label="时间类型">
        <el-select v-model="query.timeType" style="width: 150px">
          <el-option v-for="option in timeTypeOptions" :key="option" :label="option" :value="option" />
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
      <el-form-item label="单据号">
        <el-input v-model="query.documentCode" clearable placeholder="请输入单据号" style="width: 180px" />
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否预售">
        <el-select v-model="query.isPresale" style="width: 110px">
          <el-option v-for="option in yesNoOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="供货机构">
        <el-tree-select
          v-model="query.supplierOrg"
          :data="supplierOrgTree"
          :props="{ label: 'label', value: 'label', children: 'children' }"
          clearable
          filterable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="是否代店下单">
        <el-select v-model="query.substituteOrder" style="width: 110px">
          <el-option v-for="option in yesNoOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="付款状态">
        <el-select v-model="query.paymentStatus" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in paymentStatusOptions" :key="option" :label="option" :value="option" />
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
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建方式">
        <el-select v-model="query.createMode" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in createModeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="订货模板">
        <el-select v-model="query.orderTemplate" clearable placeholder="请选择" style="width: 160px">
          <el-option v-for="option in orderTemplateOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物流方式">
        <el-select v-model="query.logisticsMode" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in logisticsModeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据标签">
        <el-select v-model="query.documentTag" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in documentTagOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="打印状态">
        <el-select v-model="query.printStatus" style="width: 120px">
          <el-option v-for="option in printStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" clearable placeholder="请输入备注" style="width: 180px" />
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

    <div class="table-toolbar">
      <el-button type="primary" @click="handleToolbarAction('新增')">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button disabled>
        <el-icon><Printer /></el-icon>
        批量打印
      </el-button>
      <el-button disabled>
        <el-icon><Download /></el-icon>
        批量导出单据明细
      </el-button>
      <el-button disabled>
        <el-icon><Download /></el-icon>
        批量导出单据列表
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
      <el-table-column prop="documentCode" label="单据号" min-width="150" fixed="left" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button v-if="row.documentCode" type="primary" link @click="handleView(row)">
            {{ row.documentCode }}
          </el-button>
          <template v-else>-</template>
        </template>
      </el-table-column>
      <el-table-column prop="orderDate" label="订单日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="paymentAmount" label="付款金额" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.paymentAmount) }}</template>
      </el-table-column>
      <el-table-column prop="orderAmount" label="订货金额" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.orderAmount) }}</template>
      </el-table-column>
      <el-table-column prop="freightAmount" label="运费金额" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.freightAmount) }}</template>
      </el-table-column>
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="logisticsMode" label="物流方式" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createMode" label="创建方式" min-width="110" show-overflow-tooltip />
      <el-table-column prop="paymentStatus" label="付款状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="paymentInfo" label="付款信息" min-width="120" show-overflow-tooltip />
      <el-table-column prop="downstreamDocument" label="下游单据" min-width="140" show-overflow-tooltip />
      <el-table-column prop="expectedArrivalDate" label="期望到货日期" min-width="140" show-overflow-tooltip />
      <el-table-column prop="submittedAt" label="单据提交时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="estimatedShipDate" label="预计发货日期" min-width="140" show-overflow-tooltip />
      <el-table-column prop="estimatedArrivalDate" label="预计到货日期" min-width="140" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="orderTemplate" label="订货模板" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
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
