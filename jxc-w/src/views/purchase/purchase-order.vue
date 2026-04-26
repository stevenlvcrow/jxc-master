<script setup lang="ts">
import PurchaseDocumentPage from './components/PurchaseDocumentPage.vue';
</script>

<template>
  <PurchaseDocumentPage
    document-type="orders"
    title="采购订单"
    date-label="订单日期"
    code-label="订单号"
    amount-label="订单金额"
    default-biz-type="普通采购"
    show-receive-actions
  />
</template>
