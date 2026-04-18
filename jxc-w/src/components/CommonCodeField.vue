<script setup lang="ts">
defineProps<{
  label: string;
  value?: string | number | null;
  placeholder?: string;
}>();
</script>

<template>
  <el-form-item :label="label">
    <div class="common-code-field">
      {{ value == null || String(value).trim() === '' ? (placeholder ?? '保存后生成') : value }}
    </div>
  </el-form-item>
</template>

<style scoped>
.common-code-field {
  display: flex;
  align-items: center;
  min-height: 22px;
  padding: 0 6px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font-size: 11px;
  line-height: 1;
}
</style>
