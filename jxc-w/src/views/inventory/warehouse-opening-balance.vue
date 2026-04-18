<script setup lang="ts">
import InventoryDocumentListPage from '@/views/inventory/inventory-document-list-page.vue';
import { inventoryDocumentMetaMap } from '@/views/inventory/document-meta';

const meta = inventoryDocumentMetaMap.warehouseOpeningBalance;
</script>

<template>
  <InventoryDocumentListPage :meta="meta" />
</template>
