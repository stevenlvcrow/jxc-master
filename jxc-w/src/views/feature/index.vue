<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import ItemManagementView from '@/views/items/management.vue';
import ItemCategoryManagementView from '@/views/items/category-management.vue';
import ItemTagManagementView from '@/views/items/tag-management.vue';
import StatisticsTypeManagementView from '@/views/items/statistics-type-management.vue';
import UnitManagementView from '@/views/system/unit-management.vue';
import UserManagementView from '@/views/system/user-management.vue';
import RoleManagementView from '@/views/system/role-management.vue';
import MenuPermissionManagementView from '@/views/system/menu-permission-management.vue';
import CostCardArchiveView from '@/views/cost/cost-card-archive.vue';
import CostCardSettingView from '@/views/cost/cost-card-setting.vue';
import DishWarehouseLinkView from '@/views/cost/dish-warehouse-link.vue';
import ExpenseWarehouseLinkView from '@/views/cost/expense-warehouse-link.vue';
import SupplierManagementView from '@/views/supplier/management.vue';
import WarehouseManagementView from '@/views/system/warehouse-management.vue';
import WarehouseItemRuleManagementView from '@/views/system/warehouse-item-rule-management.vue';
import OrgManagementView from '@/views/system/org-management.vue';
import PurchasePricingView from '@/views/purchase/purchase-pricing.vue';
import PricingAdjustmentManagementView from '@/views/purchase/pricing-adjustment-management.vue';
import WarehouseOpeningBalanceView from '@/views/inventory/warehouse-opening-balance.vue';
import PurchaseInboundView from '@/views/inventory/purchase-inbound.vue';
import PurchaseReturnOutboundView from '@/views/inventory/purchase-return-outbound.vue';
import StockTransferView from '@/views/inventory/stock-transfer.vue';
import DepartmentPickingView from '@/views/inventory/department-picking.vue';
import DepartmentReturnView from '@/views/inventory/department-return.vue';
import StockTransferInboundView from '@/views/inventory/stock-transfer-inbound.vue';
import DepartmentTransferView from '@/views/inventory/department-transfer.vue';
import StoreTransferView from '@/views/inventory/store-transfer.vue';
import DamageOutboundView from '@/views/inventory/damage-outbound.vue';
import OtherInboundView from '@/views/inventory/other-inbound.vue';
import OtherOutboundView from '@/views/inventory/other-outbound.vue';
import ProductionInboundView from '@/views/inventory/production-inbound.vue';
import StockTransferOutboundView from '@/views/inventory/stock-transfer-outbound.vue';
import CustomerSalesOutboundView from '@/views/inventory/customer-sales-outbound.vue';
import CustomerReturnInboundView from '@/views/inventory/customer-return-inbound.vue';
import InventoryCheckView from '@/views/inventory/inventory-check.vue';
import MultiInventoryCheckView from '@/views/inventory/multi-inventory-check.vue';
import ProfitInboundView from '@/views/inventory/profit-inbound.vue';
import LossOutboundView from '@/views/inventory/loss-outbound.vue';
import DishConsumptionOutboundView from '@/views/inventory/dish-consumption-outbound.vue';
import BatchAdjustmentView from '@/views/inventory/batch-adjustment.vue';
import InventoryTemplateView from '@/views/inventory/inventory-template.vue';
import TransferGroupView from '@/views/inventory/transfer-group.vue';
import StockLimitsView from '@/views/inventory/stock-limits.vue';
import StockLockView from '@/views/inventory/stock-lock.vue';
import StockLockLogView from '@/views/inventory/stock-lock-log.vue';

const route = useRoute();

const title = computed(() => String(route.meta.title ?? '功能页面'));
const breadcrumbs = computed(() => (route.meta.breadcrumbs as string[] | undefined) ?? []);
const section = computed(() => {
  const currentBreadcrumbs = breadcrumbs.value;
  return currentBreadcrumbs[currentBreadcrumbs.length - 2] ?? currentBreadcrumbs[0] ?? '业务模块';
});
const routePath = computed(() => route.path);
const isItemManagement = computed(() => title.value === '物品管理');
const isItemCategoryManagement = computed(() => title.value === '物品类别管理');
const isItemTagManagement = computed(() => title.value === '物品标签管理');
const isStatisticsTypeManagement = computed(() => title.value === '统计类型管理');
const isUnitManagement = computed(() => title.value === '单位管理');
const isCostCardArchive = computed(() => title.value === '成本卡档案');
const isCostCardSetting = computed(() => title.value === '成本卡设置');
const isDishWarehouseLink = computed(() => title.value === '菜品关联仓库');
const isExpenseWarehouseLink = computed(() => title.value === '费用关联仓库');
const isUserManagement = computed(() => title.value === '用户管理');
const isRoleManagement = computed(() => title.value === '角色管理');
const isMenuPermissionManagement = computed(() => title.value === '菜单权限管理');
const isSupplierManagement = computed(() => title.value === '供应商档案');
const isWarehouseManagement = computed(() => title.value === '仓库档案');
const isWarehouseItemRuleManagement = computed(() => title.value === '仓库物品规则');
const isOrgManagement = computed(() => title.value === '机构管理');
const isPurchasePricing = computed(() => title.value === '采购单定价');
const isPricingAdjustmentManagement = computed(() => title.value === '采购定价明细调整单');
const isWarehouseOpeningBalance = computed(() => title.value === '仓库期初');
const isPurchaseInbound = computed(() => title.value === '采购入库');
const isPurchaseReturnOutbound = computed(() => title.value === '采购退货出库');
const isStockTransfer = computed(() => title.value === '移库单');
const isDepartmentPicking = computed(() => title.value === '部门领料');
const isDepartmentReturn = computed(() => title.value === '部门退料');
const isStockTransferInbound = computed(() => title.value === '移库入库');
const isDepartmentTransfer = computed(() => title.value === '部门调拨');
const isStoreTransfer = computed(() => title.value === '店间调拨');
const isDamageOutbound = computed(() => title.value === '报损出库');
const isOtherInbound = computed(() => title.value === '其他入库');
const isOtherOutbound = computed(() => title.value === '其他出库');
const isProductionInbound = computed(() => title.value === '生产入库');
const isStockTransferOutbound = computed(() => title.value === '移库出库');
const isCustomerSalesOutbound = computed(() => title.value === '客户销售出库');
const isCustomerReturnInbound = computed(() => title.value === '客户退货入库');
const isInventoryCheck = computed(() => title.value === '盘点单');
const isMultiInventoryCheck = computed(() => title.value === '多人盘点单');
const isProfitInbound = computed(() => title.value === '盘盈单');
const isLossOutbound = computed(() => title.value === '盘亏单');
const isDishConsumptionOutbound = computed(() => title.value === '菜品消耗出库');
const isBatchAdjustment = computed(() => title.value === '批次调整单');
const isInventoryTemplate = computed(() => title.value === '库存模板');
const isTransferGroup = computed(() => title.value === '调拨分组');
const isStockLimits = computed(() => title.value === '库存上下限');
const isStockLock = computed(() => title.value === '库存锁库');
const isStockLockLog = computed(() => title.value === '锁库日志');

const pendingTasks = [
  '补充查询条件与默认筛选项',
  '接入列表接口与分页能力',
  '补充新增、编辑、审核等业务动作',
];
</script>

<template>
  <div v-if="isItemManagement" class="page-grid single">
    <ItemManagementView />
  </div>
  <div v-else-if="isItemCategoryManagement" class="page-grid single">
    <ItemCategoryManagementView />
  </div>
  <div v-else-if="isItemTagManagement" class="page-grid single">
    <ItemTagManagementView />
  </div>
  <div v-else-if="isStatisticsTypeManagement" class="page-grid single">
    <StatisticsTypeManagementView />
  </div>
  <div v-else-if="isUnitManagement" class="page-grid single">
    <UnitManagementView />
  </div>
  <div v-else-if="isCostCardArchive" class="page-grid single">
    <CostCardArchiveView />
  </div>
  <div v-else-if="isCostCardSetting" class="page-grid single">
    <CostCardSettingView />
  </div>
  <div v-else-if="isDishWarehouseLink" class="page-grid single">
    <DishWarehouseLinkView />
  </div>
  <div v-else-if="isExpenseWarehouseLink" class="page-grid single">
    <ExpenseWarehouseLinkView />
  </div>
  <div v-else-if="isUserManagement" class="page-grid single">
    <UserManagementView />
  </div>
  <div v-else-if="isRoleManagement" class="page-grid single">
    <RoleManagementView />
  </div>
  <div v-else-if="isMenuPermissionManagement" class="page-grid single">
    <MenuPermissionManagementView />
  </div>
  <div v-else-if="isSupplierManagement" class="page-grid single">
    <SupplierManagementView />
  </div>
  <div v-else-if="isWarehouseManagement" class="page-grid single">
    <WarehouseManagementView />
  </div>
  <div v-else-if="isWarehouseItemRuleManagement" class="page-grid single">
    <WarehouseItemRuleManagementView />
  </div>
  <div v-else-if="isOrgManagement" class="page-grid single">
    <OrgManagementView />
  </div>
  <div v-else-if="isPurchasePricing" class="page-grid single">
    <PurchasePricingView />
  </div>
  <div v-else-if="isPricingAdjustmentManagement" class="page-grid single">
    <PricingAdjustmentManagementView />
  </div>
  <div v-else-if="isWarehouseOpeningBalance" class="page-grid single">
    <WarehouseOpeningBalanceView />
  </div>
  <div v-else-if="isPurchaseInbound" class="page-grid single">
    <PurchaseInboundView />
  </div>
  <div v-else-if="isPurchaseReturnOutbound" class="page-grid single">
    <PurchaseReturnOutboundView />
  </div>
  <div v-else-if="isStockTransfer" class="page-grid single">
    <StockTransferView />
  </div>
  <div v-else-if="isDepartmentPicking" class="page-grid single">
    <DepartmentPickingView />
  </div>
  <div v-else-if="isDepartmentReturn" class="page-grid single">
    <DepartmentReturnView />
  </div>
  <div v-else-if="isStockTransferInbound" class="page-grid single">
    <StockTransferInboundView />
  </div>
  <div v-else-if="isDepartmentTransfer" class="page-grid single">
    <DepartmentTransferView />
  </div>
  <div v-else-if="isStoreTransfer" class="page-grid single">
    <StoreTransferView />
  </div>
  <div v-else-if="isDamageOutbound" class="page-grid single">
    <DamageOutboundView />
  </div>
  <div v-else-if="isOtherInbound" class="page-grid single">
    <OtherInboundView />
  </div>
  <div v-else-if="isOtherOutbound" class="page-grid single">
    <OtherOutboundView />
  </div>
  <div v-else-if="isProductionInbound" class="page-grid single">
    <ProductionInboundView />
  </div>
  <div v-else-if="isStockTransferOutbound" class="page-grid single">
    <StockTransferOutboundView />
  </div>
  <div v-else-if="isCustomerSalesOutbound" class="page-grid single">
    <CustomerSalesOutboundView />
  </div>
  <div v-else-if="isCustomerReturnInbound" class="page-grid single">
    <CustomerReturnInboundView />
  </div>
  <div v-else-if="isInventoryCheck" class="page-grid single">
    <InventoryCheckView />
  </div>
  <div v-else-if="isMultiInventoryCheck" class="page-grid single">
    <MultiInventoryCheckView />
  </div>
  <div v-else-if="isProfitInbound" class="page-grid single">
    <ProfitInboundView />
  </div>
  <div v-else-if="isLossOutbound" class="page-grid single">
    <LossOutboundView />
  </div>
  <div v-else-if="isDishConsumptionOutbound" class="page-grid single">
    <DishConsumptionOutboundView />
  </div>
  <div v-else-if="isBatchAdjustment" class="page-grid single">
    <BatchAdjustmentView />
  </div>
  <div v-else-if="isInventoryTemplate" class="page-grid single">
    <InventoryTemplateView />
  </div>
  <div v-else-if="isTransferGroup" class="page-grid single">
    <TransferGroupView />
  </div>
  <div v-else-if="isStockLimits" class="page-grid single">
    <StockLimitsView />
  </div>
  <div v-else-if="isStockLock" class="page-grid single">
    <StockLockView />
  </div>
  <div v-else-if="isStockLockLog" class="page-grid single">
    <StockLockLogView />
  </div>

  <div v-else class="page-grid">
    <section class="hero-card">
      <div>
        <p class="eyebrow">{{ breadcrumbs[0] ?? '业务中心' }}</p>
        <h1>{{ title }}</h1>
        <p class="hero-copy">
          当前页面已按思维导图挂载到菜单与路由，可继续在此基础上接入表格、表单、审批流和接口联调。
        </p>
      </div>
      <div class="hero-badge">
        <span>{{ section }}</span>
        <span>{{ routePath }}</span>
      </div>
    </section>

    <section class="panel">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </section>

    <section class="panel">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="当前功能">{{ title }}</el-descriptions-item>
        <el-descriptions-item label="所属模块">{{ breadcrumbs[0] ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属分组">{{ section }}</el-descriptions-item>
        <el-descriptions-item label="路由路径">{{ routePath }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="panel">
      <ul class="todo-list">
        <li v-for="task in pendingTasks" :key="task">{{ task }}</li>
      </ul>
    </section>
  </div>
</template>

