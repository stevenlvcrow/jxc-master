<script setup lang="ts">
import { computed, useSlots } from 'vue';

export type PageTabItem = {
  key: string;
  label: string;
  disabled?: boolean;
};

const props = withDefaults(defineProps<{
  tabs: ReadonlyArray<PageTabItem>;
  activeTab: string;
  bodyClass?: string;
}>(), {
  bodyClass: '',
});

const emit = defineEmits<{
  (event: 'update:activeTab', key: string): void;
  (event: 'change', key: string): void;
}>();

const slots = useSlots();

const hasActions = computed(() => Boolean(slots.actions));
const hasNotice = computed(() => Boolean(slots.notice));

const handleTabClick = (tab: PageTabItem) => {
  if (tab.disabled || tab.key === props.activeTab) {
    return;
  }
  emit('update:activeTab', tab.key);
  emit('change', tab.key);
};
</script>

<template>
  <section class="page-tabs-layout">
    <header class="page-tabs-layout__header">
      <div class="page-tabs-layout__tabs" role="tablist" aria-label="页面页签">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="page-tabs-layout__tab"
          :class="{ 'is-active': tab.key === activeTab }"
          :disabled="tab.disabled"
          role="tab"
          :aria-selected="tab.key === activeTab"
          @click="handleTabClick(tab)"
        >
          <span>{{ tab.label }}</span>
        </button>
      </div>

      <div v-if="hasActions" class="page-tabs-layout__actions">
        <slot name="actions" />
      </div>
    </header>

    <div v-if="hasNotice" class="page-tabs-layout__notice">
      <slot name="notice" />
    </div>

    <div class="page-tabs-layout__body" :class="bodyClass">
      <slot />
    </div>
  </section>
</template>

<style scoped lang="scss">
.page-tabs-layout {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid #d8e0eb;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.page-tabs-layout__header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 0 16px;
  border-bottom: 1px solid #e4e9f1;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(249, 251, 253, 0.98) 100%);
}

.page-tabs-layout__tabs {
  display: flex;
  align-items: stretch;
  gap: 28px;
  min-width: 0;
  overflow-x: auto;
}

.page-tabs-layout__tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 58px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #475467;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s ease;
}

.page-tabs-layout__tab::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 999px 999px 0 0;
  background: transparent;
  transition: background-color 0.2s ease;
}

.page-tabs-layout__tab:hover {
  color: #111827;
}

.page-tabs-layout__tab.is-active {
  color: var(--brand);
  font-weight: 700;
}

.page-tabs-layout__tab.is-active::after {
  background: var(--brand);
}

.page-tabs-layout__tab:disabled {
  color: #98a2b3;
  cursor: not-allowed;
}

.page-tabs-layout__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
  padding: 10px 0;
  color: #475467;
}

.page-tabs-layout__notice {
  padding: 8px 16px 0;
}

.page-tabs-layout__body {
  min-width: 0;
  padding: 12px 16px 16px;
}

@media (max-width: 960px) {
  .page-tabs-layout__header {
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    padding: 0 12px 10px;
  }

  .page-tabs-layout__tabs {
    gap: 18px;
  }

  .page-tabs-layout__tab {
    min-height: 46px;
    font-size: 13px;
  }

  .page-tabs-layout__actions {
    justify-content: flex-start;
    padding: 8px 0 0;
  }

  .page-tabs-layout__body {
    padding: 12px;
  }
}
</style>
