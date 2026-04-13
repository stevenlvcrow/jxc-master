<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type TransferGroupRow = {
  id: number;
  groupCode: string;
  groupName: string;
  groupType: string;
  orgCount: number;
  creator: string;
  createdAt: string;
};

const orgTree = [
  {
    value: 'org-root',
    label: '总部',
    children: [
      { value: 'north-region', label: '华北大区' },
      { value: 'east-region', label: '华东大区' },
      { value: 'south-region', label: '华南大区' },
    ],
  },
];

const query = reactive({
  groupCode: '',
  groupName: '',
  orgId: '',
});

const tableData: TransferGroupRow[] = [
  {
    id: 1,
    groupCode: 'TG-001',
    groupName: '华东调拨组',
    groupType: '门店调拨',
    orgCount: 8,
    creator: '张敏',
    createdAt: '2026-04-10 09:12:00',
  },
  {
    id: 2,
    groupCode: 'TG-002',
    groupName: '华北仓库调拨',
    groupType: '仓库调拨',
    orgCount: 5,
    creator: '李娜',
    createdAt: '2026-04-09 15:20:00',
  },
  {
    id: 3,
    groupCode: 'TG-003',
    groupName: '华南联合调拨',
    groupType: '门店调拨',
    orgCount: 6,
    creator: '王磊',
    createdAt: '2026-04-08 11:30:00',
  },
];

const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  const codeKeyword = query.groupCode.trim().toLowerCase();
  const nameKeyword = query.groupName.trim().toLowerCase();
  return tableData.filter((row) => {
    const matchedCode = !codeKeyword || row.groupCode.toLowerCase().includes(codeKeyword);
    const matchedName = !nameKeyword || row.groupName.toLowerCase().includes(nameKeyword);
    const matchedOrg = !query.orgId || row.groupName.includes(query.orgId);
    return matchedCode && matchedName && matchedOrg;
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
  query.groupCode = '';
  query.groupName = '';
  query.orgId = '';
  currentPage.value = 1;
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
      <el-form-item label="分组编号">
        <el-input v-model="query.groupCode" placeholder="请输入分组编号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="分组名称">
        <el-input v-model="query.groupName" placeholder="请输入分组名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="调拨机构">
        <el-tree-select
          v-model="query.orgId"
          :data="orgTree"
          :props="{ label: 'label', value: 'value', children: 'children' }"
          clearable
          check-strictly
          default-expand-all
          style="width: 180px"
        />
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
      <el-table-column prop="groupCode" label="分组编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="groupName" label="分组名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="groupType" label="分组类型" min-width="120" show-overflow-tooltip />
      <el-table-column prop="orgCount" label="机构数" min-width="100" show-overflow-tooltip />
      <el-table-column prop="creator" label="创建人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
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
