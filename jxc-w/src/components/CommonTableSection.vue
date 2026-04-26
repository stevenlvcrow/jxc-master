<script setup lang="ts">
import type { SummaryMethod } from 'element-plus';

const props = withDefaults(defineProps<{
  data: Array<Record<string, unknown>>;
  rowKey?: string;
  loading?: boolean;
  height?: number | string;
  minHeight?: number | string;
  showSummary?: boolean;
  summaryMethod?: SummaryMethod<Record<string, unknown>>;
  fit?: boolean;
  border?: boolean;
  stripe?: boolean;
  emptyText?: string;
}>(), {
  rowKey: 'id',
  loading: false,
  height: undefined,
  minHeight: undefined,
  showSummary: false,
  summaryMethod: undefined,
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
  <div
    class="common-table-section"
    :style="{ minHeight: props.minHeight != null ? (typeof props.minHeight === 'number' ? `${props.minHeight}px` : props.minHeight) : undefined }"
  >
    <el-table
      :data="props.data"
      :row-key="props.rowKey"
      :loading="props.loading"
      :height="props.height"
      :show-summary="props.showSummary"
      :summary-method="props.summaryMethod"
      :fit="props.fit"
      :border="props.border"
      :stripe="props.stripe"
      :empty-text="props.emptyText"
      class="erp-table"
      @selection-change="handleSelectionChange"
    >
      <slot />
      <template v-if="$slots.append" #append>
        <slot name="append" />
      </template>
    </el-table>
  </div>
</template>

<style scoped>
.common-table-section {
  width: 100%;
  min-width: 0;
}
</style>
