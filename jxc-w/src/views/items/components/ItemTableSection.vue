<script setup lang="ts">
import type { ItemRow, ItemTableColumn } from '../types';

defineProps<{
  data: ItemRow[];
  columns: ItemTableColumn[];
  height: number;
  loading: boolean;
  emptyText: string;
}>();

const emit = defineEmits<{
  (event: 'selection-change', rows: ItemRow[]): void;
  (event: 'edit', row: ItemRow): void;
  (event: 'toggle-status', row: ItemRow): void;
  (event: 'delete', row: ItemRow): void;
}>();

const handleSelectionChange = (rows: ItemRow[]) => {
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
    <el-table-column label="操作" width="210" fixed="right">
      <template #default="{ row }">
        <el-button text type="primary" @click="emit('edit', row)">编辑</el-button>
        <el-button text>详情</el-button>
        <el-button text type="warning" @click="emit('toggle-status', row)">
          {{ row.status === '启用' ? '停用' : '启用' }}
        </el-button>
        <el-button text type="danger" @click="emit('delete', row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
