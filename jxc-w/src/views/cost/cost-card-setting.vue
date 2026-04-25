<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import {
  fetchDishCategoryTreeApi,
  fetchDishesApi,
  type DishListRow,
  type DishTreeNode,
} from '@/api/modules/dish';
import { useSessionStore } from '@/stores/session';
import { normalizeOrgId } from '@/utils/org';

const sessionStore = useSessionStore();
const loading = ref(false);
const treeLoading = ref(false);
const rows = ref<DishListRow[]>([]);
const treeData = ref<DishTreeNode[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedCategoryId = ref('');
const query = reactive({
  keyword: '',
});

const currentOrgId = () => normalizeOrgId(sessionStore.currentOrgId) || undefined;

const loadTree = async () => {
  const orgId = currentOrgId();
  if (!orgId) {
    treeData.value = [];
    return;
  }
  treeLoading.value = true;
  try {
    treeData.value = await fetchDishCategoryTreeApi(orgId);
  } catch {
    treeData.value = [];
    ElMessage.error('菜品分类加载失败');
  } finally {
    treeLoading.value = false;
  }
};

const loadRows = async () => {
  const orgId = currentOrgId();
  if (!orgId) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const result = await fetchDishesApi({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      categoryId: selectedCategoryId.value || undefined,
    }, orgId);
    rows.value = result.list;
    total.value = result.total;
  } catch {
    rows.value = [];
    total.value = 0;
    ElMessage.error('菜品成本卡设置列表加载失败');
  } finally {
    loading.value = false;
  }
};

const refreshAll = async () => {
  await Promise.all([loadTree(), loadRows()]);
};

const handleSearch = async () => {
  currentPage.value = 1;
  await loadRows();
};

const handleReset = async () => {
  query.keyword = '';
  selectedCategoryId.value = '';
  currentPage.value = 1;
  await loadRows();
};

const handleTreeNodeClick = async (data: DishTreeNode) => {
  selectedCategoryId.value = data.id === 'all' ? '' : String(data.id);
  currentPage.value = 1;
  await loadRows();
};

watch(() => sessionStore.currentOrgId, () => {
  query.keyword = '';
  selectedCategoryId.value = '';
  currentPage.value = 1;
  void refreshAll();
});

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <div class="item-management-layout">
    <section class="panel category-panel">
      <el-tree
        v-loading="treeLoading"
        :data="treeData"
        node-key="id"
        default-expand-all
        class="category-tree"
        @node-click="handleTreeNodeClick"
      />
    </section>

    <section class="panel item-main-panel">
      <CommonQuerySection :model="query">
        <el-form-item label="菜品信息">
          <el-input
            v-model="query.keyword"
            placeholder="编码/名称/规格"
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

      <el-table
        v-loading="loading"
        :data="rows"
        :fit="false"
        border
        stripe
        class="erp-table"
        :height="420"
        empty-text="当前机构暂无菜品"
      >
        <el-table-column type="index" label="序号" width="56" />
        <el-table-column prop="spuCode" label="菜品SPU编码" width="130" show-overflow-tooltip />
        <el-table-column prop="dishName" label="菜品名称" width="160" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="category" label="菜品分类" width="120" show-overflow-tooltip />
        <el-table-column prop="dishType" label="菜品类型" width="120" show-overflow-tooltip />
        <el-table-column prop="linkedCostCard" label="是否关联成本卡" width="140" />
        <el-table-column prop="deleted" label="是否删除" width="100" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" show-overflow-tooltip />
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
  </div>
</template>
