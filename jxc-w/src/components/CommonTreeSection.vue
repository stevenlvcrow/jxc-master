<script setup lang="ts">
export type CommonTreeNode = {
  id: string | number;
  label: string;
  children?: CommonTreeNode[];
};

const props = withDefaults(defineProps<{
  data: CommonTreeNode[];
  nodeKey?: string;
  defaultExpandAll?: boolean;
  expandOnClickNode?: boolean;
  highlightCurrent?: boolean;
  currentNodeKey?: string | number;
}>(), {
  nodeKey: 'id',
  defaultExpandAll: true,
  expandOnClickNode: false,
  highlightCurrent: false,
  currentNodeKey: '',
});

const emit = defineEmits<{
  (event: 'node-click', node: CommonTreeNode): void;
}>();

const handleNodeClick = (node: CommonTreeNode) => {
  emit('node-click', node);
};
</script>

<template>
  <el-tree
    :data="props.data"
    :node-key="props.nodeKey"
    :default-expand-all="props.defaultExpandAll"
    :expand-on-click-node="props.expandOnClickNode"
    :highlight-current="props.highlightCurrent"
    :current-node-key="props.currentNodeKey"
    class="category-tree"
    @node-click="handleNodeClick"
  />
</template>
