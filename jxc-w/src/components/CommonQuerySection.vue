<script setup lang="ts">
defineProps<{
  model?: Record<string, unknown>;
}>();
</script>

<template>
  <el-form :model="model" inline class="filter-bar compact-filter-bar">
    <slot />
  </el-form>
</template>
