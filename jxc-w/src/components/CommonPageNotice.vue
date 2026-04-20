<script setup lang="ts">
const props = withDefaults(defineProps<{
  lines: string[];
  title?: string;
}>(), {
  title: '说明',
});
</script>

<template>
  <section v-if="props.lines.length" class="common-page-notice" role="note" :aria-label="props.title">
    <div class="common-page-notice__title">{{ props.title }}</div>
    <p v-for="line in props.lines" :key="line" class="common-page-notice__line">{{ line }}</p>
  </section>
</template>

<style scoped>
.common-page-notice {
  margin-bottom: 8px;
}

.common-page-notice__title {
  margin: 0 0 2px;
  color: #606266;
  font-size: 11px;
  line-height: 1.6;
  font-weight: 400;
}

.common-page-notice__line {
  margin: 0;
  color: #606266;
  line-height: 1.6;
  font-size: 11px;
}

.common-page-notice__line + .common-page-notice__line {
  margin-top: 2px;
}
</style>
