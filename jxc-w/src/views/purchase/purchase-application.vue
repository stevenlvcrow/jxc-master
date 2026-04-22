<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useStoreWarehouseTree, type WarehouseTreeNode } from '@/composables/useStoreWarehouseTree';
import { useSessionStore } from '@/stores/session';
import { fetchItemsApi, type ItemVO } from '@/api/modules/item';
import { resolveArchiveOrgId } from '@/views/items/org';

type DocumentStatus = '草稿' | '已提交' | '已审核' | '已关闭';
type CreateSource = '手工创建' | '智能采购' | '模板生成';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type PurchaseApplicationRow = {
  id: number;
  documentDate: string;
  applicationCode: string;
  warehouse: string;
  itemCount: number;
  documentStatus: DocumentStatus;
  sourceDocumentCode: string;
  downstreamDocumentCode: string;
  createSource: CreateSource;
  creator: string;
  createdAt: string;
};

const sessionStore = useSessionStore();
const { warehouseTree, loadWarehouseTree } = useStoreWarehouseTree();

const documentStatusOptions: DocumentStatus[] = ['草稿', '已提交', '已审核', '已关闭'];
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
    value: 'store-team',
    label: '门店组',
    children: [
      { value: '王磊', label: '王磊' },
      { value: '赵晨', label: '赵晨' },
    ],
  },
];

const query = reactive({
  documentDateRange: [] as string[],
  applicationCode: '',
  warehouse: '',
  documentStatus: '',
  itemCode: '',
  creator: '',
  queryScheme: '系统默认方案',
});

const currentPage = ref(1);
const pageSize = ref(10);
const selectedIds = ref<number[]>([]);
const optionLoading = ref(false);
const itemOptions = ref<Array<{ value: string; label: string }>>([]);

const tableData = ref<PurchaseApplicationRow[]>([
  {
    id: 1,
    documentDate: '2026-04-18',
    applicationCode: 'PA-202604-001',
    warehouse: '中央成品仓',
    itemCount: 5,
    documentStatus: '已审核',
    sourceDocumentCode: '-',
    downstreamDocumentCode: 'PO-202604-001',
    createSource: '手工创建',
    creator: '张敏',
    createdAt: '2026-04-18 09:30:00',
  },
  {
    id: 2,
    documentDate: '2026-04-19',
    applicationCode: 'PA-202604-002',
    warehouse: '北区原料仓',
    itemCount: 3,
    documentStatus: '已提交',
    sourceDocumentCode: 'SP-202604-008',
    downstreamDocumentCode: '-',
    createSource: '智能采购',
    creator: '李娜',
    createdAt: '2026-04-19 14:12:00',
  },
  {
    id: 3,
    documentDate: '2026-04-20',
    applicationCode: 'PA-202604-003',
    warehouse: '南区包材仓',
    itemCount: 2,
    documentStatus: '草稿',
    sourceDocumentCode: '-',
    downstreamDocumentCode: '-',
    createSource: '模板生成',
    creator: '王磊',
    createdAt: '2026-04-20 11:05:00',
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
    await loadWarehouseTree();
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
    ElMessage.error('采购单申请筛选项加载失败');
  } finally {
    optionLoading.value = false;
  }
};

const filteredRows = computed(() => {
  const applicationCodeKeyword = query.applicationCode.trim().toLowerCase();
  const startDate = query.documentDateRange[0];
  const endDate = query.documentDateRange[1];
  return tableData.value.filter((row) => {
    const matchedDate = (!startDate || row.documentDate >= startDate) && (!endDate || row.documentDate <= endDate);
    const matchedCode = !applicationCodeKeyword || row.applicationCode.toLowerCase().includes(applicationCodeKeyword);
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedStatus = !query.documentStatus || row.documentStatus === query.documentStatus;
    const matchedItem = !query.itemCode || row.itemCount > 0;
    const matchedCreator = !query.creator || row.creator === query.creator;
    return matchedDate && matchedCode && matchedWarehouse && matchedStatus && matchedItem && matchedCreator;
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
  query.documentDateRange = [];
  query.applicationCode = '';
  query.warehouse = '';
  query.documentStatus = '';
  query.itemCode = '';
  query.creator = '';
  query.queryScheme = '系统默认方案';
  currentPage.value = 1;
};

const handleSelectionChange = (rows: PurchaseApplicationRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleView = (row: PurchaseApplicationRow) => {
  ElMessage.info(`查看：${row.applicationCode}`);
};

const handleEdit = (row: PurchaseApplicationRow) => {
  ElMessage.info(`编辑：${row.applicationCode}`);
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
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item label="申请单号">
        <el-input
          v-model="query.applicationCode"
          clearable
          placeholder="请输入申请单号"
          style="width: 180px"
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

      <el-form-item label="查询方案">
        <el-select v-model="query.queryScheme" style="width: 150px">
          <el-option
            v-for="option in querySchemeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
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
      <div class="table-toolbar__left">
        <el-button type="primary" @click="handleToolbarAction('新增')">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
        <el-button :disabled="!selectedIds.length" @click="handleToolbarAction('删除')">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
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
      <el-table-column prop="documentDate" label="单据日期" min-width="120" show-overflow-tooltip />
      <el-table-column prop="applicationCode" label="申请单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="warehouse" label="仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" align="right" />
      <el-table-column prop="documentStatus" label="单据状态" min-width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.documentStatus === '已审核' ? 'success' : row.documentStatus === '已提交' ? 'warning' : 'info'"
            size="small"
          >
            {{ row.documentStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceDocumentCode" label="来源单据号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="downstreamDocumentCode" label="下游单据号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createSource" label="创建来源" min-width="110" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
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
