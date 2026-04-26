<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Download, RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';

type TreeNode = { value: string; label: string; children?: TreeNode[] };
type Row = { id: number; diffCode: string; processOrg: string; supplierOrg: string; deliveryOrg: string; sourceReceiptCode: string; sourceDeliveryCode: string; sourceOrderCode: string; businessMode: string; orderDate: string; shipDate: string; receiptDate: string; lastOperatedAt: string; receiver: string; diffAmount: number; refundAmount: number; originalAmount: number; promoDiscount: number; manualDiscount: number; couponDiscount: number; giftDiscount: number; finalAmount: number; status: string };

const sessionStore = useSessionStore();
const dateTypes = ['订单日期', '发货日期', '收货日期', '最后操作时间'];
const yesNo = ['全部', '是', '否'];
const orgTree = computed<TreeNode[]>(() => sessionStore.rootGroups.map((node) => ({ value: node.name, label: node.name, children: node.children?.map((child) => ({ value: child.name, label: child.name })) })));
const query = reactive({ dateType: '订单日期', startDate: '', endDate: '', diffCode: '', status: '', includePresale: '', share: '全部', processOrg: '', supplierOrg: '', deliveryOrg: '', sourceReceiptCode: '', sourceDeliveryCode: '', sourceOrderCode: '', businessMode: '', item: '', adjustedPrice: false });
const currentPage = ref(1);
const pageSize = ref(10);
const tableData = ref<Row[]>([]);
const filteredRows = computed(() => tableData.value.filter((row) => (!query.diffCode || row.diffCode.includes(query.diffCode)) && (!query.status || row.status === query.status)));
const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value));
const money = (value: number) => value.toFixed(2);
const reset = () => Object.assign(query, { dateType: '订单日期', startDate: '', endDate: '', diffCode: '', status: '', includePresale: '', share: '全部', processOrg: '', supplierOrg: '', deliveryOrg: '', sourceReceiptCode: '', sourceDeliveryCode: '', sourceOrderCode: '', businessMode: '', item: '', adjustedPrice: false });
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="日期"><el-select v-model="query.dateType" style="width:130px"><el-option v-for="x in dateTypes" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="开始日期"><el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="结束日期"><el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="差异处理单号"><el-input v-model="query.diffCode" clearable style="width:170px" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable style="width:120px"><el-option label="待处理" value="待处理" /><el-option label="已处理" value="已处理" /></el-select></el-form-item>
      <el-form-item label="是否包含预售"><el-select v-model="query.includePresale" clearable style="width:120px"><el-option v-for="x in yesNo" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="是否参与分账"><el-select v-model="query.share" style="width:120px"><el-option v-for="x in yesNo" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="处理机构"><el-tree-select v-model="query.processOrg" :data="orgTree" clearable filterable check-strictly default-expand-all style="width:170px" /></el-form-item>
      <el-form-item label="供货机构"><el-select v-model="query.supplierOrg" clearable style="width:150px"><el-option label="华东配送中心" value="华东配送中心" /></el-select></el-form-item>
      <el-form-item label="送货机构"><el-select v-model="query.deliveryOrg" clearable style="width:150px"><el-option label="朝阳门店" value="朝阳门店" /></el-select></el-form-item>
      <el-form-item label="来源配送收货单"><el-input v-model="query.sourceReceiptCode" clearable style="width:170px" /></el-form-item>
      <el-form-item label="来源配送单"><el-input v-model="query.sourceDeliveryCode" clearable style="width:150px" /></el-form-item>
      <el-form-item label="来源订货单"><el-input v-model="query.sourceOrderCode" clearable style="width:150px" /></el-form-item>
      <el-form-item label="业务模式"><el-select v-model="query.businessMode" clearable style="width:120px"><el-option label="统配" value="统配" /></el-select></el-form-item>
      <el-form-item label="物品"><el-select v-model="query.item" clearable style="width:150px"><el-option label="鸡胸肉" value="鸡胸肉" /></el-select></el-form-item>
      <el-form-item label="被调过价"><el-checkbox v-model="query.adjustedPrice" /></el-form-item>
      <el-form-item><el-button type="primary" @click="currentPage=1"><el-icon><Search /></el-icon>查询</el-button><el-button @click="reset"><el-icon><RefreshRight /></el-icon>重置</el-button></el-form-item>
    </CommonQuerySection>
    <div class="table-toolbar"><el-button disabled><el-icon><Download /></el-icon>批量导出</el-button></div>
    <el-table :data="pagedRows" border stripe class="erp-table" :fit="false" height="460">
      <el-table-column type="index" label="序号" width="56" fixed="left" /><el-table-column prop="diffCode" label="差异处理单号" min-width="150" fixed="left" /><el-table-column prop="processOrg" label="处理机构" min-width="120" /><el-table-column prop="supplierOrg" label="供货机构" min-width="130" /><el-table-column prop="deliveryOrg" label="送货机构" min-width="130" /><el-table-column prop="sourceReceiptCode" label="来源配送收货单" min-width="160" /><el-table-column prop="sourceDeliveryCode" label="来源配送单" min-width="150" /><el-table-column prop="sourceOrderCode" label="来源订货单" min-width="150" /><el-table-column prop="businessMode" label="业务模式" min-width="100" /><el-table-column prop="orderDate" label="订单日期" min-width="120" /><el-table-column prop="shipDate" label="发货日期" min-width="120" /><el-table-column prop="receiptDate" label="收货日期" min-width="120" /><el-table-column prop="lastOperatedAt" label="最后操作时间" min-width="170" /><el-table-column prop="receiver" label="收货人" min-width="100" />
      <el-table-column v-for="col in ['diffAmount','refundAmount','originalAmount','promoDiscount','manualDiscount','couponDiscount','giftDiscount','finalAmount']" :key="col" :prop="col" :label="{diffAmount:'差异金额',refundAmount:'退费金额-退货机构收',originalAmount:'折前金额',promoDiscount:'促销折扣',manualDiscount:'手动折扣',couponDiscount:'支付折扣-优惠券',giftDiscount:'支付折扣-赠送金额',finalAmount:'折后金额'}[col]" min-width="130" align="right"><template #default="{ row }">{{ money(row[col]) }}</template></el-table-column>
      <el-table-column prop="status" label="状态" min-width="100" />
    </el-table>
    <div class="table-pagination"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredRows.length" :page-sizes="[10,20,50]" background small layout="total, sizes, prev, pager, next, jumper" /></div>
  </section>
</template>
