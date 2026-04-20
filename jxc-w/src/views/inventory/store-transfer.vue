<script setup lang="ts">
import { computed, ref } from 'vue';
import PageTabsLayout, { type PageTabItem } from '@/components/PageTabsLayout.vue';
import InventoryDocumentListPage from '@/views/inventory/inventory-document-list-page.vue';
import { inventoryDocumentMetaMap } from '@/views/inventory/document-meta';

const tabs: PageTabItem[] = [
  { key: 'outbound', label: '调出' },
  { key: 'inbound', label: '调入' },
];

const activeTab = ref('outbound');
const outboundMeta = inventoryDocumentMetaMap.storeTransfer;
const inboundMeta = computed(() => ({
  ...inventoryDocumentMetaMap.storeTransfer,
  title: '店间调拨（调入）',
  primaryField: { key: 'primaryName', label: '调入门店', kind: 'text' as const },
  secondaryField: { key: 'secondaryName', label: '调出门店', kind: 'text' as const },
}));
</script>

<template>
  <PageTabsLayout :tabs="tabs" :active-tab="activeTab" @update:active-tab="activeTab = $event">
    <InventoryDocumentListPage v-if="activeTab === 'outbound'" :meta="outboundMeta" />
    <InventoryDocumentListPage v-else :key="activeTab" :meta="inboundMeta" />
  </PageTabsLayout>
</template>
