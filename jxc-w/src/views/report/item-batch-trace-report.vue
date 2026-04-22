<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Search, Setting } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
import { useStoreWarehouseTree } from '@/composables/useStoreWarehouseTree';
import { useSessionStore, type OrgNode } from '@/stores/session';
import {
  fetchItemsApi,
  type ItemVO,
} from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type UnitType = '基准单位' | '库存单位';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

type SourceRow = {
  id: string;
  sourceOrg: string;
  inboundOrg: string;
  inboundWarehouse: string;
  inboundType: string;
  inboundDocumentNo: string;
  upstreamDocumentNo: string;
  inboundDate: string;
  inboundCreatedAt: string;
  batchNo: string;
  manufacturer: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  unit: string;
  inboundQty: number;
};

type InternalFlowRow = {
  id: string;
  orgName: string;
  warehouse: string;
  inoutType: string;
  documentNo: string;
  upstreamDocumentNo: string;
  documentDate: string;
  documentCreatedAt: string;
  batchNo: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  unit: string;
  inoutQty: number;
  currentBalanceQty: number;
};

type ExternalFlowRow = {
  id: string;
  orgName: string;
  warehouse: string;
  targetOrg: string;
  outboundType: string;
  outboundDocumentNo: string;
  upstreamDocumentNo: string;
  outboundDate: string;
  outboundCreatedAt: string;
  batchNo: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  unit: string;
  inoutQty: number;
  targetBalanceQty: number;
};

type ReportColumn<T> = {
  key: keyof T;
  label: string;
  minWidth: number;
  align?: 'left' | 'right';
  fixed?: 'left';
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const unitTypeOptions: UnitType[] = ['基准单位', '库存单位'];

const query = reactive({
  businessDateRange: [] as string[],
  warehouses: [] as string[],
  itemCode: '',
  batchNo: '',
  sourceOrg: '全部',
  unitType: '基准单位' as UnitType,
});

const loading = ref(false);
const optionLoading = ref(false);
const sourceRows = ref<SourceRow[]>([]);
const internalFlowRows = ref<InternalFlowRow[]>([]);
const externalFlowRows = ref<ExternalFlowRow[]>([]);
const itemTree = ref<TreeNode[]>([]);

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
  return [
    { value: '全部', label: '全部' },
    ...sessionStore.rootGroups
      .map((node) => normalize(node))
      .filter((node): node is TreeNode => Boolean(node)),
  ];
});

const sourceColumns: Array<ReportColumn<SourceRow>> = [
  { key: 'sourceOrg', label: '来源机构', minWidth: 130 },
  { key: 'inboundOrg', label: '入库机构', minWidth: 130 },
  { key: 'inboundWarehouse', label: '入库仓库', minWidth: 130 },
  { key: 'inboundType', label: '入库类型', minWidth: 120 },
  { key: 'inboundDocumentNo', label: '入库单据号', minWidth: 150 },
  { key: 'upstreamDocumentNo', label: '上游单据号', minWidth: 150 },
  { key: 'inboundDate', label: '入库日期', minWidth: 120 },
  { key: 'inboundCreatedAt', label: '入库单创建时间', minWidth: 160 },
  { key: 'batchNo', label: '批次号', minWidth: 130 },
  { key: 'manufacturer', label: '生产商', minWidth: 130 },
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'inboundQty', label: '入库数量', minWidth: 110, align: 'right' },
];

const internalFlowColumns: Array<ReportColumn<InternalFlowRow>> = [
  { key: 'orgName', label: '机构', minWidth: 130 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'inoutType', label: '出入库类型', minWidth: 120 },
  { key: 'documentNo', label: '出入库单据号', minWidth: 150 },
  { key: 'upstreamDocumentNo', label: '上游单据号', minWidth: 150 },
  { key: 'documentDate', label: '出入库日期', minWidth: 120 },
  { key: 'documentCreatedAt', label: '出入库单创建时间', minWidth: 170 },
  { key: 'batchNo', label: '批次号', minWidth: 130 },
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'inoutQty', label: '出入库数量', minWidth: 120, align: 'right' },
  { key: 'currentBalanceQty', label: '当前结存数量', minWidth: 130, align: 'right' },
];

const externalFlowColumns: Array<ReportColumn<ExternalFlowRow>> = [
  { key: 'orgName', label: '机构', minWidth: 130 },
  { key: 'warehouse', label: '仓库', minWidth: 130 },
  { key: 'targetOrg', label: '流向机构', minWidth: 130 },
  { key: 'outboundType', label: '出库类型', minWidth: 120 },
  { key: 'outboundDocumentNo', label: '出库单据号', minWidth: 150 },
  { key: 'upstreamDocumentNo', label: '上游单据号', minWidth: 150 },
  { key: 'outboundDate', label: '出库日期', minWidth: 120 },
  { key: 'outboundCreatedAt', label: '出库单创建时间', minWidth: 160 },
  { key: 'batchNo', label: '批次号', minWidth: 130 },
  { key: 'itemCode', label: '物品编码', minWidth: 130 },
  { key: 'itemName', label: '物品名称', minWidth: 140 },
  { key: 'specModel', label: '规格型号', minWidth: 120 },
  { key: 'unit', label: '单位', minWidth: 90 },
  { key: 'inoutQty', label: '出入库数量', minWidth: 120, align: 'right' },
  { key: 'targetBalanceQty', label: '流向机构的当前结存数量', minWidth: 190, align: 'right' },
];

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

const formatValue = (value: unknown) => {
  if (typeof value !== 'number') {
    return String(value || '-');
  }
  return value.toFixed(4);
};

const loadOptions = async () => {
  const orgId = resolveArchiveOrgId(sessionStore.currentOrgId, sessionStore.platformAdminMode);
  optionLoading.value = true;
  try {
    await loadWarehouseTree();
    if (!orgId) {
      itemTree.value = [];
      return;
    }
    const itemRows = await fetchAllPages<ItemVO>((pageNo, pageSizeValue) =>
      fetchItemsApi({ pageNo, pageSize: pageSizeValue, status: '全部', itemType: '全部' }, orgId));
    itemTree.value = itemRows.map((row) => ({
      value: row.code,
      label: `${row.code} / ${row.name}`,
    }));
  } catch {
    itemTree.value = [];
    ElMessage.error('物品批次全流程跟踪表筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const validateQuery = () => {
  if (!query.itemCode) {
    ElMessage.warning('请选择物品');
    return false;
  }
  if (!query.batchNo.trim()) {
    ElMessage.warning('请输入批次号');
    return false;
  }
  return true;
};

const fetchReport = async () => {
  if (!validateQuery()) {
    return;
  }
  loading.value = true;
  try {
    sourceRows.value = [];
    internalFlowRows.value = [];
    externalFlowRows.value = [];
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  await fetchReport();
};

const handleReset = () => {
  query.businessDateRange = [];
  query.warehouses = [];
  query.itemCode = '';
  query.batchNo = '';
  query.sourceOrg = '全部';
  query.unitType = '基准单位';
  sourceRows.value = [];
  internalFlowRows.value = [];
  externalFlowRows.value = [];
};

const handleSourceAdvanced = () => {
  ElMessage.info('来源机构高级筛选待接入');
};

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode],
  async () => {
    await loadOptions();
    handleReset();
  },
);

onMounted(async () => {
  await loadOptions();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="业务日期">
        <el-date-picker
          v-model="query.businessDateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item label="仓库">
        <el-tree-select
          v-model="query.warehouses"
          :data="warehouseTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          multiple
          filterable
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择"
          style="width: 220px"
        />
      </el-form-item>

      <el-form-item label="物品" required>
        <el-tree-select
          v-model="query.itemCode"
          :data="itemTree"
          :props="{ label: 'label', value: 'value' }"
          :loading="optionLoading"
          filterable
          clearable
          check-strictly
          default-expand-all
          placeholder="请选择物品"
          style="width: 220px"
        />
      </el-form-item>

      <el-form-item label="批次号" required>
        <el-input v-model="query.batchNo" clearable placeholder="请输入批次号" style="width: 180px" />
      </el-form-item>

      <el-form-item label="来源机构">
        <el-input-group>
          <el-tree-select
            v-model="query.sourceOrg"
            :data="storeTree"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            filterable
            clearable
            check-strictly
            default-expand-all
            style="width: 180px"
          />
          <el-button @click="handleSourceAdvanced">
            <el-icon><Setting /></el-icon>
            高级
          </el-button>
        </el-input-group>
      </el-form-item>

      <el-form-item label="单位类型">
        <el-tree-select
          v-model="query.unitType"
          :data="unitTypeOptions.map((option) => ({ value: option, label: option }))"
          :props="{ label: 'label', value: 'value' }"
          check-strictly
          default-expand-all
          style="width: 140px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <h3 class="report-section-title">批次物品来源表</h3>
    <CommonTableSection
      :data="sourceRows"
      row-key="id"
      :loading="loading"
      :height="260"
      empty-text="暂无批次物品来源数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in sourceColumns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatValue(row[column.key]) }}
        </template>
      </el-table-column>
    </CommonTableSection>

    <h3 class="report-section-title">该批次物品在本机构的流转和结存表</h3>
    <CommonTableSection
      :data="internalFlowRows"
      row-key="id"
      :loading="loading"
      :height="260"
      empty-text="暂无本机构流转和结存数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in internalFlowColumns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatValue(row[column.key]) }}
        </template>
      </el-table-column>
    </CommonTableSection>

    <h3 class="report-section-title">批次物品流向外部机构及结存表</h3>
    <CommonTableSection
      :data="externalFlowRows"
      row-key="id"
      :loading="loading"
      :height="260"
      empty-text="暂无流向外部机构及结存数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column
        v-for="column in externalFlowColumns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatValue(row[column.key]) }}
        </template>
      </el-table-column>
    </CommonTableSection>
  </section>
</template>

<style scoped>
.report-section-title {
  margin: 16px 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
</style>
