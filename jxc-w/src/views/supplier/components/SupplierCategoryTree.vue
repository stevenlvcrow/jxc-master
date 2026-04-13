<script setup lang="ts">
import type { CategoryNode } from '../types';

defineProps<{
  treeData: CategoryNode[];
  selectedId?: string;
}>();

const emit = defineEmits<{
  (event: 'select', id: string): void;
  (event: 'add-subcategory'): void;
}>();

const handleNodeClick = (data: CategoryNode) => {
  emit('select', data.id);
};
</script>

<template>
  <aside class="panel category-panel">
    <div class="table-toolbar">
      <el-button type="primary" @click="emit('add-subcategory')">新增子类</el-button>
    </div>
    <el-tree
      :current-node-key="selectedId"
      :data="treeData"
      :expand-on-click-node="false"
      default-expand-all
      node-key="id"
      class="category-tree"
      highlight-current
      @node-click="handleNodeClick"
    />
  </aside>
</template>

