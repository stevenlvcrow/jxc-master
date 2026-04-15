<script setup lang="ts">
const props = withDefaults(defineProps<{
  data: Array<Record<string, unknown>>;
  rowKey?: string;
  loading?: boolean;
  height?: number | string;
  fit?: boolean;
  border?: boolean;
  stripe?: boolean;
  emptyText?: string;
}>(), {
  rowKey: 'id',
  loading: false,
  height: 320,
  fit: false,
  border: true,
  stripe: true,
  emptyText: '暂无数据',
});

const emit = defineEmits<{
  (event: 'selection-change', rows: Array<Record<string, unknown>>): void;
}>();

const handleSelectionChange = (rows: Array<Record<string, unknown>>) => {
  emit('selection-change', rows);
};
</script>

<template>
  <el-table
    :data="props.data"
    :row-key="props.rowKey"
    :loading="props.loading"
    :height="props.height"
    :fit="props.fit"
    :border="props.border"
    :stripe="props.stripe"
    :empty-text="props.emptyText"
    class="erp-table"
    @selection-change="handleSelectionChange"
  >
    <slot />
  </el-table>
</template>
