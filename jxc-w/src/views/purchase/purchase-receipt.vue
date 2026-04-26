<script setup lang="ts">
import PurchaseDocumentPage from './components/PurchaseDocumentPage.vue';
</script>

<template>
  <PurchaseDocumentPage
    document-type="receipts"
    title="采购收货"
    date-label="收货日期"
    code-label="收货单号"
    warehouse-label="收货仓库"
    amount-label="收货金额"
    default-biz-type="采购订单收货"
    show-receive-actions
  />
</template>
