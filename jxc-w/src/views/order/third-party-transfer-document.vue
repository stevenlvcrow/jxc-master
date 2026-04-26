<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';

type TreeNode = { value: string; label: string; children?: TreeNode[] };
type Row = { id: number; documentCode: string; transferDate: string; documentStatus: string; supplierOrg: string; inboundStore: string; amount: number; createType: string; creator: string; createdAt: string; printStatus: string; remark: string };

const sessionStore = useSessionStore();
const timeTypes = ['调拨日期', '提交日期', '审核日期'];
const printOptions = ['全部', '未打印', '已打印'];
const orgTree = computed<TreeNode[]>(() => sessionStore.rootGroups.map((node) => ({ value: node.name, label: node.name, children: node.children?.map((child) => ({ value: child.name, label: child.name })) })));
const query = reactive({ timeType: '调拨日期', startDate: '', endDate: '', documentCode: '', documentStatus: '', supplierOrg: '', inboundStore: '', createType: '全部', item: '', remark: '', printStatus: '全部' });
const currentPage = ref(1);
const pageSize = ref(10);
const tableData = ref<Row[]>([]);
const filteredRows = computed(() => tableData.value.filter((row) => (!query.documentCode || row.documentCode.includes(query.documentCode)) && (!query.documentStatus || row.documentStatus === query.documentStatus) && (query.printStatus === '全部' || row.printStatus === query.printStatus)));
const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value));
const reset = () => Object.assign(query, { timeType: '调拨日期', startDate: '', endDate: '', documentCode: '', documentStatus: '', supplierOrg: '', inboundStore: '', createType: '全部', item: '', remark: '', printStatus: '全部' });
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="时间类型"><el-select v-model="query.timeType" style="width:120px"><el-option v-for="x in timeTypes" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item label="开始日期"><el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="结束日期"><el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" style="width:150px" /></el-form-item>
      <el-form-item label="单据编号"><el-input v-model="query.documentCode" clearable style="width:160px" /></el-form-item>
      <el-form-item label="单据状态"><el-select v-model="query.documentStatus" clearable placeholder="请选择" style="width:120px"><el-option label="草稿" value="草稿" /><el-option label="已提交" value="已提交" /><el-option label="已审核" value="已审核" /></el-select></el-form-item>
      <el-form-item label="供货机构"><el-tree-select v-model="query.supplierOrg" :data="orgTree" clearable filterable check-strictly default-expand-all placeholder="请选择" style="width:170px" /></el-form-item>
      <el-form-item label="调入门店"><el-tree-select v-model="query.inboundStore" :data="orgTree" clearable filterable check-strictly default-expand-all placeholder="请选择" style="width:170px" /></el-form-item>
      <el-form-item label="创建类型"><el-select v-model="query.createType" style="width:120px"><el-option label="全部" value="全部" /><el-option label="按单调拨" value="按单调拨" /></el-select></el-form-item>
      <el-form-item label="物品"><el-select v-model="query.item" clearable placeholder="请选择" style="width:150px"><el-option label="鸡胸肉" value="鸡胸肉" /></el-select></el-form-item>
      <el-form-item label="备注"><el-input v-model="query.remark" clearable style="width:150px" /></el-form-item>
      <el-form-item label="打印状态"><el-select v-model="query.printStatus" style="width:120px"><el-option v-for="x in printOptions" :key="x" :label="x" :value="x" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="currentPage=1"><el-icon><Search /></el-icon>查询</el-button><el-button @click="reset"><el-icon><RefreshRight /></el-icon>重置</el-button></el-form-item>
    </CommonQuerySection>
    <div class="table-toolbar"><el-button type="primary" disabled><el-icon><Plus /></el-icon>新增-按单调拨</el-button></div>
    <el-table :data="pagedRows" border stripe class="erp-table" :fit="false" height="460">
      <el-table-column type="index" label="序号" width="56" fixed="left" /><el-table-column prop="documentCode" label="单据编号" min-width="150" fixed="left" /><el-table-column prop="transferDate" label="调拨日期" min-width="120" /><el-table-column prop="documentStatus" label="单据状态" min-width="100" /><el-table-column prop="supplierOrg" label="供货机构" min-width="140" /><el-table-column prop="inboundStore" label="调入门店" min-width="130" /><el-table-column label="金额" min-width="110" align="right"><template #default="{ row }">{{ row.amount.toFixed(2) }}</template></el-table-column><el-table-column prop="createType" label="创建类型" min-width="110" /><el-table-column prop="creator" label="创建人" min-width="100" /><el-table-column prop="createdAt" label="创建日期" min-width="170" /><el-table-column prop="printStatus" label="打印状态" min-width="100" /><el-table-column prop="remark" label="备注" min-width="160" />
    </el-table>
    <div class="table-pagination"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredRows.length" :page-sizes="[10,20,50]" background small layout="total, sizes, prev, pager, next, jumper" /></div>
  </section>
</template>
