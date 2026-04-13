<script setup lang="ts">
import type { CategoryNode } from '../types';

const props = defineProps<{
  treeData: CategoryNode[];
  selectedLabel?: string;
}>();

const emit = defineEmits<{
  (event: 'select', label: string): void;
}>();

const handleNodeClick = (data: CategoryNode) => {
  emit('select', data.label);
};
</script>

<template>
  <aside class="panel category-panel">
    <el-tree
      :current-node-key="props.selectedLabel"
      :data="treeData"
      :expand-on-click-node="false"
      default-expand-all
      node-key="label"
      class="category-tree"
      highlight-current
      @node-click="handleNodeClick"
    />
  </aside>
</template>
