<script setup lang="ts">
defineProps<{
  title: string;
}>();
</script>

<template>
  <section class="form-section-block">
    <div class="common-form-section__header">
      <div class="form-section-title">{{ title }}</div>
      <div v-if="$slots.action" class="common-form-section__action">
        <slot name="action" />
      </div>
    </div>
    <slot />
  </section>
</template>

<style scoped>
.common-form-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.common-form-section__action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
