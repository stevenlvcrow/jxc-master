<script setup lang="ts">
import PurchaseDocumentPage from './components/PurchaseDocumentPage.vue';
</script>

<template>
  <PurchaseDocumentPage
    document-type="returns"
    title="采购退货"
    date-label="退货日期"
    code-label="退货单号"
    warehouse-label="退货仓库"
    amount-label="退货金额"
    default-biz-type="采购退货"
    show-return-reason
  />
</template>
