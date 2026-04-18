<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { fetchOrgTreeApi } from '@/api/modules/org';
import { useSessionStore, type OrgNode } from '@/stores/session';

type OrgRow = {
  id: string;
  orgCode: string;
  orgName: string;
  parentOrg: string;
  city: string;
};

const sessionStore = useSessionStore();
const useRealOrgApi = import.meta.env.VITE_USE_REAL_ORG_API === '1';
const query = reactive({
  orgCode: '',
  orgName: '',
});

const currentPage = ref(1);
const pageSize = ref(10);
const selectedGroupId = ref('');

const groupOptions = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'group'));

const resolveParentGroup = (org: OrgNode | null) => {
  if (!org) {
    return null;
  }
  if (org.type === 'group') {
    return org;
  }
  return sessionStore.rootGroups.find((group) => group.children?.some((child) => child.id === org.id)) ?? null;
};

const syncSelectedGroup = () => {
  const currentGroup = resolveParentGroup(sessionStore.currentOrg);
  selectedGroupId.value = currentGroup?.id ?? groupOptions.value[0]?.id ?? '';
};

watch(
  () => [sessionStore.currentOrgId, groupOptions.value.length],
  () => {
    syncSelectedGroup();
  },
  { immediate: true },
);

watch(selectedGroupId, () => {
  currentPage.value = 1;
});

const activeGroup = computed(() => {
  return groupOptions.value.find((group) => group.id === selectedGroupId.value)
    ?? resolveParentGroup(sessionStore.currentOrg)
    ?? groupOptions.value[0]
    ?? null;
});

const filteredRows = computed(() => {
  const orgCodeKeyword = query.orgCode.trim().toLowerCase();
  const orgNameKeyword = query.orgName.trim().toLowerCase();
  const group = activeGroup.value;
  const rows = group?.children ?? [];

  return rows.map((row) => ({
    id: row.id,
    orgCode: row.code,
    orgName: row.name,
    parentOrg: group?.name ?? '-',
    city: row.city,
  })).filter((row) => {
    const matchedCode = !orgCodeKeyword || row.orgCode.toLowerCase().includes(orgCodeKeyword);
    const matchedName = !orgNameKeyword || row.orgName.toLowerCase().includes(orgNameKeyword);
    return matchedCode && matchedName;
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
  syncSelectedGroup();
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

const loadOrgTree = async () => {
  if (!useRealOrgApi) {
    return;
  }
  try {
    const tree = await fetchOrgTreeApi();
    sessionStore.setOrgTree(tree);
  } catch {
    // Global error message handled in http interceptor.
  }
};

onMounted(() => {
  loadOrgTree();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="所属集团">
        <el-select
          v-model="selectedGroupId"
          style="width: 240px"
          filterable
          :disabled="groupOptions.length <= 1"
        >
          <el-option
            v-for="group in groupOptions"
            :key="group.id"
            :label="group.name"
            :value="group.id"
          />
        </el-select>
      </el-form-item>
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
      <el-table-column prop="parentOrg" label="上级机构" min-width="160" show-overflow-tooltip />
      <el-table-column prop="city" label="城市" min-width="120" show-overflow-tooltip />
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
