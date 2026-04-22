<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Download, Finished, Printer, RefreshRight, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';

type TreeNode = { value: string; label: string; children?: TreeNode[] };
type Row = {
  id: number; receiptCode: string; supplierOrg: string; deliveryOrg: string; orderOrg: string; sourceDeliveryCode: string;
  sourcePurchaseCode: string; sourceOrderCode: string; shipAmount: number; receiptAmount: number; freightAmount: number;
  documentStatus: string; reviewStatus: string; businessMode: string; expressLogistics: string; receiptMode: string;
  orderDate: string; expectedArrivalDate: string; shipDate: string; estimatedArrivalDate: string; receiptDate: string;
  lastOperatedAt: string; remark: string; printStatus: string; arrivalStatus: string;
};

const sessionStore = useSessionStore();
const dateTypes = ['订单日期', '期望到货日期', '发货日期', '预计到货日期', '收货日期', '最后操作时间'];
const statusOptions = ['待收货', '已收货', '已关闭'];
const simpleOptions = ['全部', '是', '否'];
const printOptions = ['全部', '未打印', '已打印'];
const treeOptions = (items: string[]): TreeNode[] => items.map((item) => ({ value: item, label: item }));
const orgTree = computed<TreeNode[]>(() => sessionStore.rootGroups.map((node) => ({
  value: node.name,
  label: node.name,
  children: node.children?.map((child) => ({ value: child.name, label: child.name })),
})));
const query = reactive({
  dateType: '订单日期',
  startDate: '',
  endDate: '',
  receiptCode: '',
  documentStatus: '待收货',
  transportStatus: '',
  includePresale: '',
  reviewStatus: '',
  supplierOrg: '',
  shippingWarehouse: '',
  deliveryCode: '',
  purchaseCode: '',
  orderCode: '',
  deliveryOrg: '',
  businessMode: '',
  receiptMode: '',
  receiptWarehouse: '',
  paymentMode: '',
  paymentStatus: '',
  item: '',
  arrivalStatus: '',
  returnStatus: '',
  adjustedPrice: false,
  documentTag: '',
  printStatus: '全部',
  remark: '',
});
const selectedIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const tableData = ref<Row[]>([
  { id: 1, receiptCode: 'RC-202604-001', supplierOrg: '华东配送中心', deliveryOrg: '朝阳门店', orderOrg: '朝阳门店', sourceDeliveryCode: 'DN-202604-001', sourcePurchaseCode: 'PO-202604-001', sourceOrderCode: 'SO-202604-001', shipAmount: 1380, receiptAmount: 1380, freightAmount: 60, documentStatus: '待收货', reviewStatus: '未复审', businessMode: '统配', expressLogistics: '顺丰', receiptMode: '按单收货', orderDate: '2026-04-20', expectedArrivalDate: '2026-04-24', shipDate: '2026-04-22', estimatedArrivalDate: '2026-04-24', receiptDate: '-', lastOperatedAt: '2026-04-22 10:30:00', remark: '常规补货', printStatus: '未打印', arrivalStatus: '待到货' },
]);
const filteredRows = computed(() => tableData.value.filter((row) => {
  const code = query.receiptCode.trim().toLowerCase();
  return (!code || row.receiptCode.toLowerCase().includes(code))
    && (!query.documentStatus || row.documentStatus === query.documentStatus)
    && (query.printStatus === '全部' || row.printStatus === query.printStatus);
}));
const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value));
const money = (value: number) => value.toFixed(2);
const reset = () => {
  Object.assign(query, { dateType: '订单日期', startDate: '', endDate: '', receiptCode: '', documentStatus: '待收货', transportStatus: '', includePresale: '', reviewStatus: '', supplierOrg: '', shippingWarehouse: '', deliveryCode: '', purchaseCode: '', orderCode: '', deliveryOrg: '', businessMode: '', receiptMode: '', receiptWarehouse: '', paymentMode: '', paymentStatus: '', item: '', arrivalStatus: '', returnStatus: '', adjustedPrice: false, documentTag: '', printStatus: '全部', remark: '' });
  currentPage.value = 1;
};
const action = (name: string) => ElMessage.info(`${name}功能待接入`);
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="日期"><el-select v-model="query.dateType" style="width:140px"><el-option v-for="x in dateTypes" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="开始日期"><el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="结束日期"><el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="配送收货单号"><el-input v-model="query.receiptCode" clearable style="width:170px" /></el-form-item>
      <el-form-item label="单据状态"><el-select v-model="query.documentStatus" style="width:130px"><el-option v-for="x in statusOptions" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="运输状态"><el-tree-select v-model="query.transportStatus" :data="treeOptions(['待运输','运输中','已送达'])" clearable check-strictly style="width:130px" /></el-form-item>
      <el-form-item label="是否包含预售"><el-select v-model="query.includePresale" clearable style="width:120px"><el-option v-for="x in simpleOptions" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="复审状态"><el-select v-model="query.reviewStatus" clearable style="width:120px"><el-option label="未复审" value="未复审" /><el-option label="已复审" value="已复审" /></el-select></el-form-item>
      <el-form-item label="供货机构"><el-tree-select v-model="query.supplierOrg" :data="orgTree" clearable filterable check-strictly default-expand-all style="width:170px" /></el-form-item>
      <el-form-item label="发货仓库"><el-tree-select v-model="query.shippingWarehouse" :data="treeOptions(['中央仓','成品仓'])" clearable check-strictly style="width:140px" /></el-form-item>
      <el-form-item label="配送单号"><el-input v-model="query.deliveryCode" clearable style="width:150px" /></el-form-item>
      <el-form-item label="采购单号(配送中心)"><el-input v-model="query.purchaseCode" clearable style="width:170px" /></el-form-item>
      <el-form-item label="订货单号"><el-input v-model="query.orderCode" clearable style="width:150px" /></el-form-item>
      <el-form-item label="送货机构"><el-tree-select v-model="query.deliveryOrg" :data="orgTree" clearable filterable check-strictly default-expand-all style="width:170px" /></el-form-item>
      <el-form-item label="业务模式"><el-select v-model="query.businessMode" clearable style="width:120px"><el-option label="统配" value="统配" /><el-option label="直送" value="直送" /></el-select></el-form-item>
      <el-form-item label="收货方式"><el-select v-model="query.receiptMode" clearable style="width:120px"><el-option label="按单收货" value="按单收货" /><el-option label="盲收" value="盲收" /></el-select></el-form-item>
      <el-form-item label="收货仓库"><el-tree-select v-model="query.receiptWarehouse" :data="treeOptions(['门店仓','暂存仓'])" clearable check-strictly style="width:140px" /></el-form-item>
      <el-form-item label="付款方式"><el-select v-model="query.paymentMode" clearable style="width:120px"><el-option label="线上" value="线上" /><el-option label="线下" value="线下" /></el-select></el-form-item>
      <el-form-item label="付款状态"><el-select v-model="query.paymentStatus" clearable style="width:120px"><el-option label="未付款" value="未付款" /><el-option label="已付款" value="已付款" /></el-select></el-form-item>
      <el-form-item label="物品"><el-tree-select v-model="query.item" :data="treeOptions(['鸡胸肉','包装盒'])" clearable filterable check-strictly style="width:150px" /></el-form-item>
      <el-form-item label="到货状态"><el-tree-select v-model="query.arrivalStatus" :data="treeOptions(['待到货','已到货','异常'])" clearable check-strictly style="width:130px" /></el-form-item>
      <el-form-item label="退货情况"><el-tree-select v-model="query.returnStatus" :data="treeOptions(['无退货','部分退货','全部退货'])" clearable check-strictly style="width:130px" /></el-form-item>
      <el-form-item label="被调过价"><el-checkbox v-model="query.adjustedPrice" /></el-form-item>
      <el-form-item label="单据标签"><el-tree-select v-model="query.documentTag" :data="treeOptions(['常规','加急'])" clearable check-strictly style="width:120px" /></el-form-item>
      <el-form-item label="打印状态"><el-radio-group v-model="query.printStatus"><el-radio v-for="x in printOptions" :key="x" :label="x" /></el-radio-group></el-form-item>
      <el-form-item label="备注"><el-input v-model="query.remark" clearable style="width:160px" /></el-form-item>
      <el-form-item><el-button type="primary" @click="currentPage=1"><el-icon><Search /></el-icon>查询</el-button><el-button @click="reset"><el-icon><RefreshRight /></el-icon>重置</el-button></el-form-item>
    </CommonQuerySection>
    <div class="table-toolbar">
      <el-button @click="action('批量打印')"><el-icon><Printer /></el-icon>批量打印</el-button><el-button @click="action('批量导出单据明细')"><el-icon><Download /></el-icon>批量导出单据明细</el-button><el-button @click="action('批量导出单据列表')"><el-icon><Download /></el-icon>批量导出单据列表</el-button><el-button type="primary" @click="action('批量收货')"><el-icon><Finished /></el-icon>批量收货</el-button><el-button @click="action('批量复审')">批量复审</el-button><el-button @click="action('批量取消复审')">批量取消复审</el-button><el-button @click="action('批量关闭')">批量关闭</el-button><el-button @click="action('批量取消关闭')">批量取消关闭</el-button><el-button @click="action('批量修改到货状态')">批量修改到货状态</el-button>
    </div>
    <el-table :data="pagedRows" border stripe class="erp-table" :fit="false" height="460" @selection-change="(rows: Row[]) => selectedIds = rows.map(row => row.id)">
      <el-table-column type="selection" width="44" fixed="left" /><el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="receiptCode" label="收货单号" min-width="150" fixed="left" show-overflow-tooltip /><el-table-column prop="supplierOrg" label="供货机构" min-width="130" /><el-table-column prop="deliveryOrg" label="送货机构" min-width="130" /><el-table-column prop="orderOrg" label="订货机构" min-width="130" /><el-table-column prop="sourceDeliveryCode" label="来源配送单" min-width="150" /><el-table-column prop="sourcePurchaseCode" label="来源采购订单（配送中心）" min-width="190" /><el-table-column prop="sourceOrderCode" label="来源订货单" min-width="150" /><el-table-column label="发货金额" min-width="110" align="right"><template #default="{ row }">{{ money(row.shipAmount) }}</template></el-table-column><el-table-column label="收货金额" min-width="110" align="right"><template #default="{ row }">{{ money(row.receiptAmount) }}</template></el-table-column><el-table-column label="合计配送运费" min-width="130" align="right"><template #default="{ row }">{{ money(row.freightAmount) }}</template></el-table-column><el-table-column prop="documentStatus" label="单据状态" min-width="100" /><el-table-column prop="reviewStatus" label="复审状态" min-width="100" /><el-table-column prop="businessMode" label="业务模式" min-width="100" /><el-table-column prop="expressLogistics" label="快递配送物流" min-width="130" /><el-table-column prop="receiptMode" label="收货方式" min-width="110" /><el-table-column prop="orderDate" label="订单日期" min-width="120" /><el-table-column prop="expectedArrivalDate" label="期望到货日期" min-width="140" /><el-table-column prop="shipDate" label="发货日期" min-width="120" /><el-table-column prop="estimatedArrivalDate" label="预计到货日期" min-width="140" /><el-table-column prop="receiptDate" label="收货日期" min-width="120" /><el-table-column prop="lastOperatedAt" label="最后操作时间" min-width="170" /><el-table-column prop="remark" label="备注" min-width="150" /><el-table-column prop="printStatus" label="打印状态" min-width="100" /><el-table-column prop="arrivalStatus" label="到货状态" min-width="100" /><el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="action(`查看：${row.receiptCode}`)">查看</el-button></template></el-table-column>
    </el-table>
    <div class="table-pagination"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="filteredRows.length" background small layout="total, sizes, prev, pager, next, jumper" /></div>
  </section>
</template>
