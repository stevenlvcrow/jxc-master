<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type Status = '启用' | '停用';
type InventoryTemplateRow = {
  id: number;
  templateCode: string;
  templateName: string;
  itemCount: number;
  orgName: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
};

const statusOptions: Status[] = ['启用', '停用'];
const orgOptions = ['总部', '华东分公司', '华南分公司'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const router = useRouter();

const query = reactive({
  keyword: '',
  orgName: '',
  status: '',
  itemName: '',
});

const tableData: InventoryTemplateRow[] = [
  {
    id: 1,
    templateCode: 'TMP-INV-001',
    templateName: '中央仓盘点模板',
    itemCount: 128,
    orgName: '总部',
    status: '启用',
    createdAt: '2026-04-10 09:20:00',
    updatedAt: '2026-04-12 15:30:00',
  },
  {
    id: 2,
    templateCode: 'TMP-INV-002',
    templateName: '北区原料盘点模板',
    itemCount: 64,
    orgName: '华东分公司',
    status: '停用',
    createdAt: '2026-04-08 13:40:00',
    updatedAt: '2026-04-11 17:05:00',
  },
  {
    id: 3,
    templateCode: 'TMP-INV-003',
    templateName: '门店成品盘点模板',
    itemCount: 36,
    orgName: '华南分公司',
    status: '启用',
    createdAt: '2026-04-07 10:12:00',
    updatedAt: '2026-04-12 09:48:00',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  const keyword = query.keyword.trim().toLowerCase();
  return tableData.filter((row) => {
    const matchedKeyword =
      !keyword
      || row.templateCode.toLowerCase().includes(keyword)
      || row.templateName.toLowerCase().includes(keyword);
    const matchedOrg = !query.orgName || row.orgName === query.orgName;
    const matchedStatus = !query.status || row.status === query.status;
    const matchedItem = !query.itemName || row.templateName.includes(query.itemName);
    return matchedKeyword && matchedOrg && matchedStatus && matchedItem;
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
  query.keyword = '';
  query.orgName = '';
  query.status = '';
  query.itemName = '';
  currentPage.value = 1;
};

const handleAdd = () => {
  router.push('/inventory/5/1/create');
};

const handleView = (row: InventoryTemplateRow) => {
  ElMessage.info(`查看：${row.templateCode}`);
};

const handleEdit = (row: InventoryTemplateRow) => {
  ElMessage.info(`编辑：${row.templateCode}`);
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="模板编号/名称">
        <el-input v-model="query.keyword" placeholder="请输入模板编号/名称" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="创建机构">
        <el-select v-model="query.orgName" clearable style="width: 160px">
          <el-option v-for="option in orgOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 120px">
          <el-option v-for="option in statusOptions" :key="option" :label="option" :value="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-select v-model="query.itemName" clearable style="width: 120px">
          <el-option v-for="option in itemOptions" :key="option" :label="option" :value="option" />
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
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增
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
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="templateCode" label="模板编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" show-overflow-tooltip />
      <el-table-column prop="orgName" label="创建机构" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="90" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="最后修改时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text @click="handleEdit(row)">编辑</el-button>
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
