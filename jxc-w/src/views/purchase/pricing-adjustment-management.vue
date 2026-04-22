<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, RefreshRight, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type DocumentStatus = '草稿' | '已提交' | '已审核' | '已撤回';
type AdjustmentResult = '全部成功' | '部分成功' | '失败' | '处理中';
type PricingDetailType = '供应商定价' | '仓库定价' | '活动定价';
type EnabledStatus = '启用' | '停用';
type LadderPriceStatus = '全部' | '是' | '否';
type TreeNode = { value: string; label: string; children?: TreeNode[] };

type AdjustmentRow = {
  id: number;
  adjustmentCode: string;
  adjustmentReason: string;
  documentStatus: DocumentStatus;
  adjustmentResult: AdjustmentResult;
  documentDate: string;
  remark: string;
  supplier: string;
  itemCode: string;
  creator: string;
  createdAt: string;
};

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

const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核', '已撤回'];
const adjustmentResultOptions: AdjustmentResult[] = ['全部成功', '部分成功', '失败', '处理中'];
const pricingDetailTypeOptions: PricingDetailType[] = ['供应商定价', '仓库定价', '活动定价'];
const ladderPriceOptions: LadderPriceStatus[] = ['全部', '是', '否'];
const enabledStatusOptions: EnabledStatus[] = ['启用', '停用'];

const reasonTree: TreeNode[] = [
  { value: '合同调价', label: '合同调价' },
  { value: '市场波动', label: '市场波动' },
  { value: '促销调价', label: '促销调价' },
  { value: '供应商调价', label: '供应商调价' },
];
const creatorTree: TreeNode[] = [
  {
    value: 'pricing-team',
    label: '定价组',
    children: [
      { value: '张敏', label: '张敏' },
      { value: '李娜', label: '李娜' },
      { value: '王磊', label: '王磊' },
    ],
  },
];

const query = reactive({
  documentDateRange: [] as string[],
  adjustmentCode: '',
  documentStatus: '',
  adjustmentResult: '',
  remark: '',
  supplier: '',
  adjustmentReason: '',
  itemCode: '',
  creator: '',
});

const dialogQuery = reactive({
  priceDate: '',
  pricingDetailType: '',
  supplier: '',
  itemCode: '',
  ladderPrice: '全部' as LadderPriceStatus,
  enabledStatus: '',
});

const expandFilters = ref(false);
const dialogVisible = ref(false);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);
const selectedIds = ref<number[]>([]);
const selectedPricingIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const tableData = ref<AdjustmentRow[]>([
  {
    id: 1,
    adjustmentCode: 'ADJ-202604-001',
    adjustmentReason: '合同调价',
    documentStatus: '已审核',
    adjustmentResult: '全部成功',
    documentDate: '2026-04-13',
    remark: '四月统配价格调整',
    supplier: '鲜达食品',
    itemCode: 'ITEM-001',
    creator: '张敏',
    createdAt: '2026-04-13 10:22:00',
  },
  {
    id: 2,
    adjustmentCode: 'ADJ-202604-002',
    adjustmentReason: '市场波动',
    documentStatus: '已提交',
    adjustmentResult: '处理中',
    documentDate: '2026-04-12',
    remark: '牛肉原料上浮',
    supplier: '优选农场',
    itemCode: 'ITEM-002',
    creator: '李娜',
    createdAt: '2026-04-12 14:18:00',
  },
  {
    id: 3,
    adjustmentCode: 'ADJ-202604-003',
    adjustmentReason: '促销调价',
    documentStatus: '草稿',
    adjustmentResult: '失败',
    documentDate: '2026-04-11',
    remark: '活动价未生效',
    supplier: '盒马包材',
    itemCode: 'ITEM-003',
    creator: '王磊',
    createdAt: '2026-04-11 09:05:00',
  },
]);

const pricingRows = ref<PricingDetailRow[]>([
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
    ElMessage.error('采购定价明细调整单筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const matchedDateRange = (date: string, range: string[]) => {
  if (!Array.isArray(range) || range.length !== 2) {
    return true;
  }
  return date >= range[0] && date <= range[1];
};

const filteredRows = computed(() => {
  const adjustmentCodeKeyword = query.adjustmentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.value.filter((row) => {
    const matchedCode = !adjustmentCodeKeyword || row.adjustmentCode.toLowerCase().includes(adjustmentCodeKeyword);
    const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedResult = !query.adjustmentResult || row.adjustmentResult === query.adjustmentResult;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    const matchedSupplier = !query.supplier || row.supplier === query.supplier;
    const matchedReason = !query.adjustmentReason || row.adjustmentReason === query.adjustmentReason;
    const matchedItem = !query.itemCode || row.itemCode === query.itemCode;
    const matchedCreator = !query.creator || row.creator === query.creator;
    return matchedDateRange(row.documentDate, query.documentDateRange)
      && matchedCode
      && matchedStatus
      && matchedResult
      && matchedRemark
      && matchedSupplier
      && matchedReason
      && matchedItem
      && matchedCreator;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const filteredPricingRows = computed(() => pricingRows.value.filter((row) => {
  const matchedDate = !dialogQuery.priceDate || (row.effectiveDate <= dialogQuery.priceDate && row.expireDate >= dialogQuery.priceDate);
  const matchedType = !dialogQuery.pricingDetailType || row.pricingDetailType === dialogQuery.pricingDetailType;
  const matchedSupplier = !dialogQuery.supplier || row.supplier === dialogQuery.supplier;
  const matchedItem = !dialogQuery.itemCode || row.itemCode === dialogQuery.itemCode;
  const matchedLadder = dialogQuery.ladderPrice === '全部' || row.ladderPrice === dialogQuery.ladderPrice;
  const matchedEnabled = !dialogQuery.enabledStatus || row.enabledStatus === dialogQuery.enabledStatus;
  return matchedDate && matchedType && matchedSupplier && matchedItem && matchedLadder && matchedEnabled;
}));

const formatMoney = (value: number) => value.toFixed(2);
const formatRate = (value: number) => `${value.toFixed(2)}%`;

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.documentDateRange = [];
  query.adjustmentCode = '';
  query.documentStatus = '';
  query.adjustmentResult = '';
  query.remark = '';
  query.supplier = '';
  query.adjustmentReason = '';
  query.itemCode = '';
  query.creator = '';
  currentPage.value = 1;
};

const handleDialogReset = () => {
  dialogQuery.priceDate = '';
  dialogQuery.pricingDetailType = '';
  dialogQuery.supplier = '';
  dialogQuery.itemCode = '';
  dialogQuery.ladderPrice = '全部';
  dialogQuery.enabledStatus = '';
};

const handleOpenCreate = () => {
  selectedPricingIds.value = [];
  dialogVisible.value = true;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: AdjustmentRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handlePricingSelectionChange = (rows: PricingDetailRow[]) => {
  selectedPricingIds.value = rows.map((row) => row.id);
};

const handleView = (row: AdjustmentRow) => {
  ElMessage.info(`查看：${row.adjustmentCode}`);
};

const handleEdit = (row: AdjustmentRow) => {
  ElMessage.info(`编辑：${row.adjustmentCode}`);
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
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="调整单号">
        <el-input v-model="query.adjustmentCode" placeholder="请输入调整单号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in documentStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="调整结果">
        <el-select v-model="query.adjustmentResult" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in adjustmentResultOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" placeholder="请输入备注" clearable style="width: 160px" />
      </el-form-item>
      <template v-if="expandFilters">
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
        <el-form-item label="调整原因">
          <el-tree-select
            v-model="query.adjustmentReason"
            :data="reasonTree"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            clearable
            check-strictly
            placeholder="请选择"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="物品">
          <el-select v-model="query.itemCode" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 220px">
            <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
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
            style="width: 150px"
          />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button @click="expandFilters = !expandFilters">{{ expandFilters ? '收起筛选' : '展开筛选' }}</el-button>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="handleReset"><el-icon><RefreshRight /></el-icon>重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <div class="table-toolbar">
      <el-button type="primary" @click="handleOpenCreate"><el-icon><Plus /></el-icon>新增</el-button>
      <el-button @click="handleToolbarAction('批量导入')"><el-icon><Upload /></el-icon>批量导入</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量提交')">批量提交</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量删除')"><el-icon><Delete /></el-icon>批量删除</el-button>
      <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('批量撤回')">批量撤回</el-button>
    </div>

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      height="420"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="adjustmentCode" label="调整单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="adjustmentReason" label="调整原因" min-width="120" show-overflow-tooltip />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="adjustmentResult" label="调整结果" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentDate" label="单据日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增采购定价明细调整单" width="92vw" destroy-on-close>
      <CommonQuerySection :model="dialogQuery">
        <el-form-item label="取价日期">
          <el-date-picker v-model="dialogQuery.priceDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择取价日期" style="width: 160px" />
        </el-form-item>
        <el-form-item label="定价明细类型">
          <el-select v-model="dialogQuery.pricingDetailType" clearable placeholder="请选择" style="width: 150px">
            <el-option v-for="option in pricingDetailTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-tree-select
            v-model="dialogQuery.supplier"
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
          <el-select v-model="dialogQuery.itemCode" :loading="optionLoading" clearable filterable placeholder="请选择" style="width: 220px">
            <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否阶梯价">
          <el-select v-model="dialogQuery.ladderPrice" style="width: 120px">
            <el-option v-for="option in ladderPriceOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="dialogQuery.enabledStatus" clearable placeholder="请选择" style="width: 120px">
            <el-option v-for="option in enabledStatusOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="handleDialogReset"><el-icon><RefreshRight /></el-icon>重置</el-button>
        </el-form-item>
      </CommonQuerySection>

      <el-table
        :data="filteredPricingRows"
        border
        stripe
        class="erp-table"
        :fit="false"
        height="420"
        @selection-change="handlePricingSelectionChange"
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
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedPricingIds.length" @click="handleToolbarAction('生成调整单')">生成调整单</el-button>
      </template>
    </el-dialog>
  </section>
</template>
