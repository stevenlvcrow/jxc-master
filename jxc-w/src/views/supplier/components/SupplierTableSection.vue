<script setup lang="ts">
import type { SupplierRow, SupplierTableColumn } from '../types';

defineProps<{
  data: SupplierRow[];
  columns: SupplierTableColumn[];
  height: number;
  loading: boolean;
  emptyText: string;
}>();

const emit = defineEmits<{
  (event: 'selection-change', rows: SupplierRow[]): void;
  (event: 'edit', row: SupplierRow): void;
  (event: 'bind', row: SupplierRow): void;
  (event: 'delete', row: SupplierRow): void;
}>();

const handleSelectionChange = (rows: SupplierRow[]) => {
  emit('selection-change', rows);
};
</script>

<template>
  <el-table
    :data="data"
    :fit="false"
    border
    stripe
    scrollbar-always-on
    class="erp-table"
    v-loading="loading"
    row-key="id"
    :height="height"
    :empty-text="emptyText"
    @selection-change="handleSelectionChange"
  >
    <el-table-column type="selection" width="44" fixed="left" />
    <el-table-column
      v-for="column in columns"
      :key="column.prop"
      :prop="column.prop"
      :label="column.label"
      :width="column.width"
      :fixed="column.fixed"
      show-overflow-tooltip
    />
    <el-table-column label="操作" width="180" fixed="right">
      <template #default="{ row }">
        <el-button text type="primary" @click="emit('edit', row)">编辑</el-button>
        <el-button text @click="emit('bind', row)">绑定</el-button>
        <el-button text type="danger" @click="emit('delete', row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
