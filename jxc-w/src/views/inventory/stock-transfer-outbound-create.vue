<script setup lang="ts">
import InventoryDocumentCreatePage from '@/views/inventory/inventory-document-create-page.vue';
import { inventoryDocumentMetaMap } from '@/views/inventory/document-meta';

const meta = inventoryDocumentMetaMap.stockTransferOutbound;
</script>

<template>
  <InventoryDocumentCreatePage :meta="meta" />
</template>
