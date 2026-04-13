<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';

type OrgType = '门店';
type OrgRow = {
  id: number;
  orgCode: string;
  orgName: string;
  orgType: OrgType;
  parentOrg: string;
};

const orgTypeOptions: Array<'门店'> = ['门店'];
const query = reactive({
  orgCode: '',
  orgName: '',
  orgType: '门店' as OrgType,
});

const tableData: OrgRow[] = [
  { id: 1, orgCode: 'STORE-001', orgName: '虹桥门店', orgType: '门店', parentOrg: '华东大区' },
  { id: 2, orgCode: 'STORE-002', orgName: '浦东门店', orgType: '门店', parentOrg: '华东大区' },
  { id: 3, orgCode: 'STORE-003', orgName: '徐汇门店', orgType: '门店', parentOrg: '上海直营中心' },
  { id: 4, orgCode: 'STORE-004', orgName: '静安门店', orgType: '门店', parentOrg: '上海直营中心' },
];

const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  const orgCodeKeyword = query.orgCode.trim().toLowerCase();
  const orgNameKeyword = query.orgName.trim().toLowerCase();
  return tableData.filter((row) => {
    const matchedCode = !orgCodeKeyword || row.orgCode.toLowerCase().includes(orgCodeKeyword);
    const matchedName = !orgNameKeyword || row.orgName.toLowerCase().includes(orgNameKeyword);
    const matchedType = row.orgType === query.orgType;
    return matchedCode && matchedName && matchedType;
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
  query.orgCode = '';
  query.orgName = '';
  query.orgType = '门店';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleView = (row: OrgRow) => {
  ElMessage.info(`查看：${row.orgName}`);
};

const handleEdit = (row: OrgRow) => {
  ElMessage.info(`编辑：${row.orgName}`);
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
      <el-form-item label="机构编码">
        <el-input
          v-model="query.orgCode"
          placeholder="请输入机构编码"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="机构名称">
        <el-input
          v-model="query.orgName"
          placeholder="请输入机构名称"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="机构类型">
        <el-select v-model="query.orgType" style="width: 120px">
          <el-option
            v-for="option in orgTypeOptions"
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
      <el-button @click="handleToolbarAction('批量导出')">
        <el-icon><Download /></el-icon>
        批量导出
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
      <el-table-column type="index" label="序号" width="56" />
      <el-table-column prop="orgCode" label="机构编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="orgName" label="机构名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="orgType" label="机构类型" min-width="100" show-overflow-tooltip />
      <el-table-column prop="parentOrg" label="上级机构" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ filteredRows.length }} 条</div>
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
