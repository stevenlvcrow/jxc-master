<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';

type GoodsRow = {
  name: string;
  sku: string;
  stock: number;
  status: '上架中' | '待审核' | '已下架';
};

const query = reactive({
  keyword: '',
  status: '',
});
const currentPage = ref(1);
const pageSize = ref(10);
const tableHeight = 360;
const tableData: GoodsRow[] = [
  { name: '云南高山咖啡豆', sku: 'SKU-10001', stock: 188, status: '上架中' },
  { name: '冷萃萃取液', sku: 'SKU-10002', stock: 56, status: '待审核' },
  { name: '手冲分享壶', sku: 'SKU-10003', stock: 22, status: '已下架' },
];
const filteredData = computed(() => {
  const keyword = query.keyword.trim().toLowerCase();
  return tableData.filter((item) => {
    const matchedKeyword = !keyword || `${item.name}${item.sku}`.toLowerCase().includes(keyword);
    const matchedStatus = !query.status || item.status === query.status;
    return matchedKeyword && matchedStatus;
  });
});
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredData.value.slice(start, start + pageSize.value);
});

const handleSearch = () => {
  currentPage.value = 1;
};
const handleReset = () => {
  query.keyword = '';
  query.status = '';
  currentPage.value = 1;
};
const handleToolbarAction = () => {
  ElMessage.info('新增商品功能待接入');
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
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <el-form :model="query" inline class="filter-bar compact-filter-bar">
        <el-form-item label="商品信息">
          <el-input v-model="query.keyword" placeholder="名称/SKU" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 120px">
            <el-option label="上架中" value="上架中" />
            <el-option label="待审核" value="待审核" />
            <el-option label="已下架" value="已下架" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" @click="handleToolbarAction">新增商品</el-button>
      </div>

      <el-table
        :data="pagedData"
        :fit="false"
        border
        stripe
        scrollbar-always-on
        class="erp-table"
        :height="tableHeight"
      >
        <el-table-column prop="name" label="商品名称" min-width="240" />
        <el-table-column prop="sku" label="SKU" width="160" />
        <el-table-column prop="stock" label="库存" width="120" />
        <el-table-column prop="status" label="状态" width="140" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default>
            <el-button text type="primary">编辑</el-button>
            <el-button text>查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ItemPaginationSection
        :selected-count="0"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredData.length"
        @update:current-page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>
  </div>
</template>
