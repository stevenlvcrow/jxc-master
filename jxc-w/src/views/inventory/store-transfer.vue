<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Delete, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout, { type PageTabItem } from '@/components/PageTabsLayout.vue';

type ReviewStatus = '' | '未复审' | '已复审';
type TransferStatus = '' | '全部' | '草稿' | '已提交' | '已发货' | '已收货';
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};
type StoreTransferOutRow = {
  id: number;
  transferCode: string;
  transferDate: string;
  inboundStore: string;
  amount: string;
  status: Exclude<TransferStatus, '' | '全部'>;
  reviewStatus: Exclude<ReviewStatus, ''>;
  createdAt: string;
  creator: string;
};
type StoreTransferInRow = {
  id: number;
  transferCode: string;
  transferDate: string;
  outboundStore: string;
  amount: string;
  status: Exclude<TransferStatus, '' | '全部'>;
  reviewStatus: Exclude<ReviewStatus, ''>;
  createdAt: string;
  creator: string;
};

const tabs: PageTabItem[] = [
  { key: 'outbound', label: '调出' },
  { key: 'inbound', label: '调入' },
];

const activeTab = ref('outbound');
const statusOptions: TransferStatus[] = ['', '全部', '草稿', '已提交', '已发货', '已收货'];
const reviewStatusOptions: ReviewStatus[] = ['', '未复审', '已复审'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const storeTree: TreeNode[] = [
  {
    value: 'store-root',
    label: '门店中心',
    children: [
      { value: '国贸店', label: '国贸店' },
      { value: '望京店', label: '望京店' },
      { value: '三里屯店', label: '三里屯店' },
      { value: '五道口店', label: '五道口店' },
    ],
  },
];

const outboundQuery = reactive({
  inboundStore: '',
  status: '' as TransferStatus,
  reviewStatus: '' as ReviewStatus,
  transferCode: '',
  transferDate: '',
  itemName: '',
});

const inboundQuery = reactive({
  outboundStore: '',
  status: '全部' as TransferStatus,
  reviewStatus: '' as ReviewStatus,
  transferCode: '',
  transferDate: '',
  itemName: '',
});

const outboundRows: StoreTransferOutRow[] = [
  {
    id: 1,
    transferCode: 'DJDB-202604-001',
    transferDate: '2026-04-13',
    inboundStore: '望京店',
    amount: '6,820.00',
    status: '已发货',
    reviewStatus: '已复审',
    createdAt: '2026-04-13 09:16:00',
    creator: '张敏',
  },
  {
    id: 2,
    transferCode: 'DJDB-202604-002',
    transferDate: '2026-04-12',
    inboundStore: '三里屯店',
    amount: '3,420.00',
    status: '已提交',
    reviewStatus: '未复审',
    createdAt: '2026-04-12 15:08:00',
    creator: '李娜',
  },
  {
    id: 3,
    transferCode: 'DJDB-202604-003',
    transferDate: '2026-04-11',
    inboundStore: '五道口店',
    amount: '1,960.00',
    status: '草稿',
    reviewStatus: '未复审',
    createdAt: '2026-04-11 10:22:00',
    creator: '王磊',
  },
];

const inboundRows: StoreTransferInRow[] = [
  {
    id: 1,
    transferCode: 'DJDB-202604-001',
    transferDate: '2026-04-13',
    outboundStore: '国贸店',
    amount: '6,820.00',
    status: '已收货',
    reviewStatus: '已复审',
    createdAt: '2026-04-13 09:16:00',
    creator: '张敏',
  },
  {
    id: 2,
    transferCode: 'DJDB-202604-004',
    transferDate: '2026-04-12',
    outboundStore: '望京店',
    amount: '2,760.00',
    status: '已发货',
    reviewStatus: '未复审',
    createdAt: '2026-04-12 16:28:00',
    creator: '李娜',
  },
  {
    id: 3,
    transferCode: 'DJDB-202604-005',
    transferDate: '2026-04-11',
    outboundStore: '三里屯店',
    amount: '1,340.00',
    status: '已提交',
    reviewStatus: '未复审',
    createdAt: '2026-04-11 11:06:00',
    creator: '王磊',
  },
];

const outboundCurrentPage = ref(1);
const inboundCurrentPage = ref(1);
const pageSize = ref(10);
const outboundSelectedIds = ref<number[]>([]);
const inboundSelectedIds = ref<number[]>([]);

const outboundFilteredRows = computed(() => {
  const transferCodeKeyword = outboundQuery.transferCode.trim().toLowerCase();
  return outboundRows.filter((row) => {
    const matchedInboundStore = !outboundQuery.inboundStore || row.inboundStore === outboundQuery.inboundStore;
    const matchedStatus = !outboundQuery.status || outboundQuery.status === '全部' || row.status === outboundQuery.status;
    const matchedReviewStatus = !outboundQuery.reviewStatus || row.reviewStatus === outboundQuery.reviewStatus;
    const matchedTransferCode = !transferCodeKeyword || row.transferCode.toLowerCase().includes(transferCodeKeyword);
    const matchedTransferDate = !outboundQuery.transferDate || row.transferDate === outboundQuery.transferDate;
    const matchedItem = !outboundQuery.itemName || row.transferCode.includes(outboundQuery.itemName) || row.inboundStore.includes(outboundQuery.itemName);
    return matchedInboundStore && matchedStatus && matchedReviewStatus && matchedTransferCode && matchedTransferDate && matchedItem;
  });
});

const inboundFilteredRows = computed(() => {
  const transferCodeKeyword = inboundQuery.transferCode.trim().toLowerCase();
  return inboundRows.filter((row) => {
    const matchedOutboundStore = !inboundQuery.outboundStore || row.outboundStore === inboundQuery.outboundStore;
    const matchedStatus = !inboundQuery.status || inboundQuery.status === '全部' || row.status === inboundQuery.status;
    const matchedReviewStatus = !inboundQuery.reviewStatus || row.reviewStatus === inboundQuery.reviewStatus;
    const matchedTransferCode = !transferCodeKeyword || row.transferCode.toLowerCase().includes(transferCodeKeyword);
    const matchedTransferDate = !inboundQuery.transferDate || row.transferDate === inboundQuery.transferDate;
    const matchedItem = !inboundQuery.itemName || row.transferCode.includes(inboundQuery.itemName) || row.outboundStore.includes(inboundQuery.itemName);
    return matchedOutboundStore && matchedStatus && matchedReviewStatus && matchedTransferCode && matchedTransferDate && matchedItem;
  });
});

const outboundPagedRows = computed(() => {
  const start = (outboundCurrentPage.value - 1) * pageSize.value;
  return outboundFilteredRows.value.slice(start, start + pageSize.value);
});

const inboundPagedRows = computed(() => {
  const start = (inboundCurrentPage.value - 1) * pageSize.value;
  return inboundFilteredRows.value.slice(start, start + pageSize.value);
});

const handleOutboundSearch = () => {
  outboundCurrentPage.value = 1;
};

const handleInboundSearch = () => {
  inboundCurrentPage.value = 1;
};

const handleOutboundReset = () => {
  outboundQuery.inboundStore = '';
  outboundQuery.status = '';
  outboundQuery.reviewStatus = '';
  outboundQuery.transferCode = '';
  outboundQuery.transferDate = '';
  outboundQuery.itemName = '';
  outboundCurrentPage.value = 1;
};

const handleInboundReset = () => {
  inboundQuery.outboundStore = '';
  inboundQuery.status = '全部';
  inboundQuery.reviewStatus = '';
  inboundQuery.transferCode = '';
  inboundQuery.transferDate = '';
  inboundQuery.itemName = '';
  inboundCurrentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleOutboundSelectionChange = (rows: StoreTransferOutRow[]) => {
  outboundSelectedIds.value = rows.map((row) => row.id);
};

const handleInboundSelectionChange = (rows: StoreTransferInRow[]) => {
  inboundSelectedIds.value = rows.map((row) => row.id);
};

const handleOutboundPageChange = (page: number) => {
  outboundCurrentPage.value = page;
};

const handleInboundPageChange = (page: number) => {
  inboundCurrentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  outboundCurrentPage.value = 1;
  inboundCurrentPage.value = 1;
};
</script>

<template>
  <section class="panel item-main-panel store-transfer-page">
    <PageTabsLayout v-model:active-tab="activeTab" :tabs="tabs" body-class="store-transfer-page__body">
      <template v-if="activeTab === 'outbound'">
        <CommonQuerySection :model="outboundQuery">
          <el-form-item label="调入门店">
            <el-tree-select
              v-model="outboundQuery.inboundStore"
              :data="storeTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              clearable
              check-strictly
              default-expand-all
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="单据状态">
            <el-select v-model="outboundQuery.status" clearable style="width: 140px">
              <el-option v-for="option in statusOptions" :key="option || 'empty'" :label="option || '请选择'" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="复审状态">
            <el-select v-model="outboundQuery.reviewStatus" clearable placeholder="请选择复审状态" style="width: 150px">
              <el-option v-for="option in reviewStatusOptions" :key="option || 'empty'" :label="option || '请选择复审状态'" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="调拨单号">
            <el-input v-model="outboundQuery.transferCode" placeholder="请输入调拨单号" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="调拨日期">
            <el-date-picker
              v-model="outboundQuery.transferDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择调拨日期"
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="物品">
            <el-select v-model="outboundQuery.itemName" clearable style="width: 120px">
              <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleOutboundSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleOutboundReset">
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
          <el-button @click="handleToolbarAction('批量打印')">
            <el-icon><Printer /></el-icon>
            批量打印
          </el-button>
          <el-button @click="handleToolbarAction('批量删除')">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
          <el-button @click="handleToolbarAction('批量提交')">批量提交</el-button>
          <el-button @click="handleToolbarAction('批量撤回')">批量撤回</el-button>
          <el-button @click="handleToolbarAction('批量发货')">批量发货</el-button>
          <el-button @click="handleToolbarAction('批量取消发货')">批量取消发货</el-button>
          <el-button @click="handleToolbarAction('批量复审')">批量复审</el-button>
          <el-button @click="handleToolbarAction('批量取消复审')">批量取消复审</el-button>
        </div>

        <el-table
          :data="outboundPagedRows"
          border
          stripe
          class="erp-table"
          :fit="false"
          :height="360"
          :empty-text="'当前机构暂无数据'"
          @selection-change="handleOutboundSelectionChange"
        >
          <el-table-column type="selection" width="44" fixed="left" />
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column prop="transferCode" label="调拨单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="transferDate" label="调拨日期" min-width="110" show-overflow-tooltip />
          <el-table-column prop="inboundStore" label="调入门店" min-width="120" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="reviewStatus" label="复审状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
          <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleToolbarAction(`查看：${row.transferCode}`)">查看</el-button>
              <el-button text @click="handleToolbarAction(`编辑：${row.transferCode}`)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <div class="table-pagination-meta">已选 {{ outboundSelectedIds.length }} 条</div>
          <el-pagination
            :current-page="outboundCurrentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="outboundFilteredRows.length"
            background
            small
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleOutboundPageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </template>

      <template v-else>
        <CommonQuerySection :model="inboundQuery">
          <el-form-item label="调出门店">
            <el-tree-select
              v-model="inboundQuery.outboundStore"
              :data="storeTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              clearable
              check-strictly
              default-expand-all
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="单据状态">
            <el-select v-model="inboundQuery.status" style="width: 140px">
              <el-option v-for="option in statusOptions" :key="option || 'empty'" :label="option || '请选择'" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="复审状态">
            <el-select v-model="inboundQuery.reviewStatus" clearable placeholder="请选择复审状态" style="width: 150px">
              <el-option v-for="option in reviewStatusOptions" :key="option || 'empty'" :label="option || '请选择复审状态'" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="调拨单号">
            <el-input v-model="inboundQuery.transferCode" placeholder="请输入调拨单号" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="调拨日期">
            <el-date-picker
              v-model="inboundQuery.transferDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择调拨日期"
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="物品">
            <el-select v-model="inboundQuery.itemName" clearable style="width: 120px">
              <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleInboundSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleInboundReset">
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
          <el-button @click="handleToolbarAction('批量收货')">批量收货</el-button>
          <el-button @click="handleToolbarAction('批量取消收货')">批量取消收货</el-button>
        </div>

        <el-table
          :data="inboundPagedRows"
          border
          stripe
          class="erp-table"
          :fit="false"
          :height="360"
          :empty-text="'当前机构暂无数据'"
          @selection-change="handleInboundSelectionChange"
        >
          <el-table-column type="selection" width="44" fixed="left" />
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column prop="transferCode" label="调拨单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="transferDate" label="调拨日期" min-width="110" show-overflow-tooltip />
          <el-table-column prop="outboundStore" label="调出门店" min-width="120" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" min-width="100" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="reviewStatus" label="复审状态" min-width="100" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
          <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleToolbarAction(`查看：${row.transferCode}`)">查看</el-button>
              <el-button text @click="handleToolbarAction(`编辑：${row.transferCode}`)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <div class="table-pagination-meta">已选 {{ inboundSelectedIds.length }} 条</div>
          <el-pagination
            :current-page="inboundCurrentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="inboundFilteredRows.length"
            background
            small
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleInboundPageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </template>
    </PageTabsLayout>
  </section>
</template>

<style scoped lang="scss">
.store-transfer-page {
  padding: 8px;
}

.store-transfer-page__body {
  padding-top: 10px;
}
</style>
