<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, RefreshRight, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useSupplierArchiveOptions } from '@/composables/useSupplierArchiveOptions';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

type TreeNode = { value: string; label: string; children?: TreeNode[] };

type AdjustmentRow = {
  id: number;
  adjustmentCode: string;
  adjustmentReason: string;
  documentStatus: string;
  adjustmentResult: string;
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
  pricingDetailType: string;
  supplier: string;
  spotPrice: number;
  taxIncludedPrice: number;
  taxRate: number;
  taxExcludedPrice: number;
  priceLimit: string;
  effectiveDate: string;
  expireDate: string;
  enabledStatus: string;
  remark: string;
  ladderPrice: string;
};

const sessionStore = useSessionStore();
const {
  supplierOptions,
  supplierLoading,
  loadSupplierOptions,
} = useSupplierArchiveOptions();
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const COMMON_ENABLED_STATUS_DICT = 'common.enabled_status';
const COMMON_YES_NO_DICT = 'common.yes_no';
const { optionsOf } = useDictionaryOptions([
  INVENTORY_DOCUMENT_STATUS_DICT,
  COMMON_ENABLED_STATUS_DICT,
  COMMON_YES_NO_DICT,
]);

const documentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);
const enabledStatusOptions = optionsOf(COMMON_ENABLED_STATUS_DICT);
const ladderPriceOptions = optionsOf(COMMON_YES_NO_DICT, { enabled: true });
const adjustmentResultOptions = ref<Array<{ value: string; label: string }>>([]);
const pricingDetailTypeOptions = ref<Array<{ value: string; label: string }>>([]);
const reasonTree = ref<TreeNode[]>([]);
const creatorTree = ref<TreeNode[]>([]);

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
  ladderPrice: 'ALL',
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

const tableData = ref<AdjustmentRow[]>([]);

const pricingRows = ref<PricingDetailRow[]>([]);

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
  const matchedLadder = dialogQuery.ladderPrice === 'ALL' || row.ladderPrice === dialogQuery.ladderPrice;
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
  dialogQuery.ladderPrice = 'ALL';
  dialogQuery.enabledStatus = '';
};

const handleOpenCreate = () => {
  selectedPricingIds.value = [];
  dialogVisible.value = true;
};

const handleSelectionChange = (rows: AdjustmentRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handlePricingSelectionChange = (rows: PricingDetailRow[]) => {
  selectedPricingIds.value = rows.map((row) => row.id);
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
          <el-option
            v-for="option in documentStatusOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="调整结果">
        <el-select v-model="query.adjustmentResult" clearable placeholder="请选择" style="width: 130px">
          <el-option v-for="option in adjustmentResultOptions" :key="option.value" :label="option.label" :value="option.value" />
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
      <el-button disabled><el-icon><Upload /></el-icon>批量导入</el-button>
      <el-button disabled>批量提交</el-button>
      <el-button disabled><el-icon><Delete /></el-icon>批量删除</el-button>
      <el-button disabled>批量撤回</el-button>
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
      <el-table-column prop="adjustmentCode" label="调整单号" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.adjustmentCode">{{ row.adjustmentCode }}</template>
          <template v-else>-</template>
        </template>
      </el-table-column>
      <el-table-column prop="adjustmentReason" label="调整原因" min-width="120" show-overflow-tooltip />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="adjustmentResult" label="调整结果" min-width="100" show-overflow-tooltip />
      <el-table-column prop="documentDate" label="单据日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
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
            <el-option v-for="option in pricingDetailTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
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
            <el-option
              v-for="option in ladderPriceOptions"
              :key="option.itemCode"
              :label="option.itemLabel"
              :value="option.itemCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="dialogQuery.enabledStatus" clearable placeholder="请选择" style="width: 120px">
            <el-option
              v-for="option in enabledStatusOptions"
              :key="option.itemCode"
              :label="option.itemLabel"
              :value="option.itemCode"
            />
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
        <el-button type="primary" disabled>生成调整单</el-button>
      </template>
    </el-dialog>
  </section>
</template>
