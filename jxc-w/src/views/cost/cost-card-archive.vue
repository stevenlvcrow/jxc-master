<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { fetchCostCardsApi, type CostCardRow } from '@/api/modules/cost';
import { useSessionStore } from '@/stores/session';
import { normalizeOrgId } from '@/utils/org';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

const router = useRouter();
const sessionStore = useSessionStore();
const loading = ref(false);
const rows = ref<CostCardRow[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const { optionsOf } = useDictionaryOptions(['common.enabled_status']);
const statusOptions = optionsOf('common.enabled_status');
const query = reactive({
  keyword: '',
  status: '',
});

const currentOrgId = () => normalizeOrgId(sessionStore.currentOrgId) || undefined;

const loadRows = async () => {
  const orgId = currentOrgId();
  if (!orgId) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const result = await fetchCostCardsApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    }, orgId);
    rows.value = result.list;
    total.value = result.total;
  } catch {
    rows.value = [];
    total.value = 0;
    ElMessage.error('成本卡列表加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  currentPage.value = 1;
  await loadRows();
};

const handleReset = async () => {
  query.keyword = '';
  query.status = '';
  currentPage.value = 1;
  await loadRows();
};

const handleCreate = () => {
  router.push('/archive/cost-card-archives/create');
};

watch(() => sessionStore.currentOrgId, () => {
  currentPage.value = 1;
  void loadRows();
});

onMounted(() => {
  void loadRows();
});
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="成本卡信息">
        <el-input
          v-model="query.keyword"
          placeholder="成本卡名称/编码"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 120px">
          <el-option
            v-for="item in statusOptions"
            :key="item.itemKey"
            :label="item.itemLabel"
            :value="item.itemCode"
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
      <el-button type="primary" @click="handleCreate">新增成本卡</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      :fit="false"
      border
      stripe
      class="erp-table"
      :height="420"
      empty-text="当前机构暂无成本卡"
    >
      <el-table-column type="index" label="序号" width="56" />
      <el-table-column prop="cardCode" label="成本卡编码" width="150" show-overflow-tooltip />
      <el-table-column prop="cardName" label="成本卡名称" width="180" show-overflow-tooltip />
      <el-table-column prop="cardType" label="成本卡类型" width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column prop="effectiveVersionId" label="生效版本" width="100" />
      <el-table-column prop="linkedDishCount" label="关联菜品数" width="100" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">共 {{ total }} 条</div>
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="(page: number) => { currentPage = page; void loadRows(); }"
        @size-change="(size: number) => { pageSize = size; currentPage = 1; void loadRows(); }"
      />
    </div>
  </section>
</template>
