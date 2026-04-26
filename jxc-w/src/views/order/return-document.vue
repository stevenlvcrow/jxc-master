<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Download, Plus, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';

type TreeNode = { value: string; label: string; children?: TreeNode[] };
type Row = { id: number; documentCode: string; processOrg: string; supplierOrg: string; deliveryOrg: string; returnAmount: number; originalAmount: number; promoDiscount: number; manualDiscount: number; couponDiscount: number; giftDiscount: number; lossAmount: number; finalAmount: number; documentStatus: string; refundStatus: string; returnMode: string; businessMode: string; returnType: string; returnDate: string; shipDate: string; lastOperatedAt: string; remark: string; printStatus: string; reconciliationStatus: string; submitter: string };

const sessionStore = useSessionStore();
const dateTypes = ['退货日期', '发货日期', '最后操作时间'];
const allOptions = ['全部', '未打印', '已打印'];
const orgTree = computed<TreeNode[]>(() => sessionStore.rootGroups.map((node) => ({ value: node.name, label: node.name, children: node.children?.map((child) => ({ value: child.name, label: child.name })) })));
const query = reactive({ dateType: '退货日期', startDate: '', endDate: '', documentCode: '', documentStatus: '', refundStatus: '全部', returnMode: '全部', includePresale: '', processOrg: '', supplierOrg: '', deliveryOrg: '', businessMode: '', returnType: '', createType: '', documentTag: '', item: '', reconciliationStatus: '', returnReason: '', adjustedPrice: '', printStatus: '全部', remark: '', queryScheme: '系统默认方案' });
const currentPage = ref(1);
const pageSize = ref(10);
const rows = ref<Row[]>([]);
const filteredRows = computed(() => rows.value.filter((row) => (!query.documentCode || row.documentCode.includes(query.documentCode)) && (!query.documentStatus || row.documentStatus === query.documentStatus)));
const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value));
const money = (v: number) => v.toFixed(2);
const reset = () => Object.assign(query, { dateType: '退货日期', startDate: '', endDate: '', documentCode: '', documentStatus: '', refundStatus: '全部', returnMode: '全部', includePresale: '', processOrg: '', supplierOrg: '', deliveryOrg: '', businessMode: '', returnType: '', createType: '', documentTag: '', item: '', reconciliationStatus: '', returnReason: '', adjustedPrice: '', printStatus: '全部', remark: '', queryScheme: '系统默认方案' });
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="日期"><el-select v-model="query.dateType" style="width:120px"><el-option v-for="x in dateTypes" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="开始日期"><el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="单据号"><el-input v-model="query.documentCode" clearable style="width:160px" /></el-form-item>
      <el-form-item label="单据状态"><el-select v-model="query.documentStatus" clearable style="width:120px"><el-option label="草稿" value="草稿" /><el-option label="已提交" value="已提交" /></el-select></el-form-item>
      <el-form-item label="退款状态"><el-select v-model="query.refundStatus" style="width:120px"><el-option label="全部" value="全部" /><el-option label="未退款" value="未退款" /><el-option label="已退款" value="已退款" /></el-select></el-form-item>
      <el-form-item label="返货模式"><el-select v-model="query.returnMode" style="width:120px"><el-option label="全部" value="全部" /><el-option label="按单返货" value="按单返货" /></el-select></el-form-item>
      <el-form-item label="是否包含预售"><el-select v-model="query.includePresale" clearable style="width:120px"><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></el-form-item>
      <el-form-item label="处理机构"><el-tree-select v-model="query.processOrg" :data="orgTree" clearable check-strictly default-expand-all style="width:170px" /></el-form-item>
      <el-form-item label="供货机构"><el-select v-model="query.supplierOrg" clearable style="width:150px"><el-option label="华东配送中心" value="华东配送中心" /></el-select></el-form-item>
      <el-form-item label="送货机构"><el-select v-model="query.deliveryOrg" clearable style="width:150px"><el-option label="朝阳门店" value="朝阳门店" /></el-select></el-form-item>
      <el-form-item label="业务模式"><el-select v-model="query.businessMode" clearable style="width:120px"><el-option label="统配" value="统配" /></el-select></el-form-item>
      <el-form-item label="返货单类型"><el-select v-model="query.returnType" clearable style="width:130px"><el-option label="普通返货" value="普通返货" /></el-select></el-form-item>
      <el-form-item label="创建类型"><el-select v-model="query.createType" clearable style="width:120px"><el-option label="手工" value="手工" /></el-select></el-form-item>
      <el-form-item label="单据标签"><el-select v-model="query.documentTag" clearable style="width:120px"><el-option label="常规" value="常规" /></el-select></el-form-item>
      <el-form-item label="物品"><el-select v-model="query.item" clearable style="width:150px"><el-option label="鸡胸肉" value="鸡胸肉" /></el-select></el-form-item>
      <el-form-item label="对账状态"><el-select v-model="query.reconciliationStatus" clearable style="width:120px"><el-option label="未对账" value="未对账" /></el-select></el-form-item>
      <el-form-item label="退货原因"><el-select v-model="query.returnReason" clearable style="width:120px"><el-option label="质量问题" value="质量问题" /></el-select></el-form-item>
      <el-form-item label="被调过价"><el-input v-model="query.adjustedPrice" clearable style="width:100px" /></el-form-item>
      <el-form-item label="打印状态"><el-select v-model="query.printStatus" style="width:120px"><el-option v-for="x in allOptions" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="备注"><el-input v-model="query.remark" clearable style="width:150px" /></el-form-item>
      <el-form-item label="查询方案"><el-select v-model="query.queryScheme" style="width:150px"><el-option label="系统默认方案" value="系统默认方案" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="currentPage=1"><el-icon><Search /></el-icon>查询</el-button><el-button @click="reset"><el-icon><RefreshRight /></el-icon>重置</el-button></el-form-item>
    </CommonQuerySection>
    <div class="table-toolbar"><el-button type="primary" disabled><el-icon><Plus /></el-icon>新增-按单返货</el-button><el-button disabled><el-icon><Printer /></el-icon>批量打印</el-button><el-button disabled><el-icon><Download /></el-icon>批量导出</el-button></div>
    <el-table :data="pagedRows" border stripe class="erp-table" :fit="false" height="460">
      <el-table-column type="index" label="序号" width="56" fixed="left" /><el-table-column prop="documentCode" label="单据号" min-width="150" fixed="left" /><el-table-column prop="processOrg" label="处理机构" min-width="120" /><el-table-column prop="supplierOrg" label="供货机构" min-width="130" /><el-table-column prop="deliveryOrg" label="送货机构" min-width="130" />
      <el-table-column v-for="col in ['returnAmount','originalAmount','promoDiscount','manualDiscount','couponDiscount','giftDiscount','lossAmount','finalAmount']" :key="col" :prop="col" :label="{returnAmount:'返货金额',originalAmount:'折前金额',promoDiscount:'促销折扣',manualDiscount:'手动折扣',couponDiscount:'支付折扣-优惠券',giftDiscount:'支付折扣-赠送金额',lossAmount:'折损金额',finalAmount:'折后金额'}[col]" min-width="130" align="right"><template #default="{ row }">{{ money(row[col]) }}</template></el-table-column>
      <el-table-column prop="documentStatus" label="单据状态" min-width="100" /><el-table-column prop="refundStatus" label="退款状态" min-width="100" /><el-table-column prop="returnMode" label="返货模式" min-width="110" /><el-table-column prop="businessMode" label="业务模式" min-width="100" /><el-table-column prop="returnType" label="返货单类型" min-width="120" /><el-table-column prop="returnDate" label="退货日期" min-width="120" /><el-table-column prop="shipDate" label="发货日期" min-width="120" /><el-table-column prop="lastOperatedAt" label="最后操作时间" min-width="170" /><el-table-column prop="remark" label="备注" min-width="150" /><el-table-column prop="printStatus" label="打印状态" min-width="100" /><el-table-column prop="reconciliationStatus" label="对账状态" min-width="100" /><el-table-column prop="submitter" label="提交人" min-width="100" />
    </el-table>
    <div class="table-pagination"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredRows.length" :page-sizes="[10,20,50]" background small layout="total, sizes, prev, pager, next, jumper" /></div>
  </section>
</template>
