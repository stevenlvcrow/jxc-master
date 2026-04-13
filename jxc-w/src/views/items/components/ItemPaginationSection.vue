<script setup lang="ts">
defineProps<{
  selectedCount: number;
  currentPage: number;
  pageSize: number;
  total: number;
}>();

const emit = defineEmits<{
  (event: 'update:currentPage', page: number): void;
  (event: 'update:pageSize', size: number): void;
}>();

const handleCurrentChange = (page: number) => {
  emit('update:currentPage', page);
};

const handleSizeChange = (size: number) => {
  emit('update:pageSize', size);
};
</script>

<template>
  <div class="table-pagination">
    <div class="table-pagination-meta">
      已选 {{ selectedCount }} 条
    </div>
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      :total="total"
      background
      small
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>
