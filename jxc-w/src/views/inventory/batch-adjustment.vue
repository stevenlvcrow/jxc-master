<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Download, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type DateType = '调整日期' | '创建时间';
type BatchAdjustmentRow = {
  id: number;
  documentCode: string;
  adjustDate: string;
  warehouse: string;
  itemCount: number;
  status: string;
  creator: string;
  createdAt: string;
  remark: string;
};

const dateTypeOptions: DateType[] = ['调整日期', '创建时间'];
const INVENTORY_DOCUMENT_STATUS_DICT = 'inventory.document_status';
const { optionsOf } = useDictionaryOptions([INVENTORY_DOCUMENT_STATUS_DICT]);
const documentStatusOptions = optionsOf(INVENTORY_DOCUMENT_STATUS_DICT);
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();
const sessionStore = useSessionStore();
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const query = reactive({
  dateType: '调整日期' as DateType,
  startDate: '',
  endDate: '',
  warehouse: '',
  documentCode: '',
  documentStatus: '',
  itemName: '',
  remark: '',
});

const tableData = ref<BatchAdjustmentRow[]>([]);

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);

const filteredRows = computed(() => {
  const codeKeyword = query.documentCode.trim().toLowerCase();
  const remarkKeyword = query.remark.trim().toLowerCase();
  return tableData.value.filter((row) => {
    const dateField = query.dateType === '调整日期' ? row.adjustDate : row.createdAt.slice(0, 10);
    const matchedStartDate = !query.startDate || dateField >= query.startDate;
    const matchedEndDate = !query.endDate || dateField <= query.endDate;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedCode = !codeKeyword || row.documentCode.toLowerCase().includes(codeKeyword);
    const matchedStatus = !query.documentStatus || row.status === query.documentStatus;
    const matchedRemark = !remarkKeyword || row.remark.toLowerCase().includes(remarkKeyword);
    return matchedStartDate
      && matchedEndDate
      && matchedWarehouse
      && matchedCode
      && matchedStatus
      && matchedRemark;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.dateType = '调整日期';
  query.startDate = '';
  query.endDate = '';
  query.warehouse = '';
  query.documentCode = '';
  query.documentStatus = '';
  query.itemName = '';
  query.remark = '';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: BatchAdjustmentRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

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

const loadItemOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  if (!orgId) {
    itemOptions.value = [];
    return;
  }
  optionLoading.value = true;
  try {
    const rows = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId));
    itemOptions.value = rows.map((item) => ({
      value: item.code,
      label: item.name ? `${item.code} / ${item.name}` : item.code,
    }));
  } catch {
    itemOptions.value = [];
  } finally {
    optionLoading.value = false;
  }
};

onMounted(() => {
  void loadWarehouseTree();
  void loadItemOptions();
});

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  () => {
    void loadWarehouseTree();
    void loadItemOptions();
  },
);
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="日期类型">
        <el-select v-model="query.dateType" style="width: 120px">
          <el-option v-for="option in dateTypeOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="query.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker
          v-model="query.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束日期"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouse"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="单据编号">
        <el-input v-model="query.documentCode" placeholder="请输入单据编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="query.documentStatus" clearable style="width: 120px">
          <el-option
            v-for="option in documentStatusOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemName" :loading="optionLoading" clearable filterable style="width: 180px">
          <el-option v-for="option in itemOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="query.remark" placeholder="请输入备注" clearable style="width: 160px" />
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
      <el-button type="primary" disabled>
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
      <el-button disabled>
        <el-icon><Delete /></el-icon>
        批量删除
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
      :height="360"
      :empty-text="'当前机构暂无数据'"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="documentCode" label="单据编号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="adjustDate" label="调整日期" min-width="110" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="120" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
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
