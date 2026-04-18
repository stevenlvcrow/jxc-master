/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
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
const breadcrumbs = computed(() => route.meta.breadcrumbs ?? []);
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
if (__VLS_ctx.isItemManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof ItemManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(ItemManagementView, new ItemManagementView({}));
    const __VLS_1 = __VLS_0({}, ...__VLS_functionalComponentArgsRest(__VLS_0));
}
else if (__VLS_ctx.isItemCategoryManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof ItemCategoryManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_3 = __VLS_asFunctionalComponent(ItemCategoryManagementView, new ItemCategoryManagementView({}));
    const __VLS_4 = __VLS_3({}, ...__VLS_functionalComponentArgsRest(__VLS_3));
}
else if (__VLS_ctx.isItemTagManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof ItemTagManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_6 = __VLS_asFunctionalComponent(ItemTagManagementView, new ItemTagManagementView({}));
    const __VLS_7 = __VLS_6({}, ...__VLS_functionalComponentArgsRest(__VLS_6));
}
else if (__VLS_ctx.isStatisticsTypeManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StatisticsTypeManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(StatisticsTypeManagementView, new StatisticsTypeManagementView({}));
    const __VLS_10 = __VLS_9({}, ...__VLS_functionalComponentArgsRest(__VLS_9));
}
else if (__VLS_ctx.isUnitManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof UnitManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_12 = __VLS_asFunctionalComponent(UnitManagementView, new UnitManagementView({}));
    const __VLS_13 = __VLS_12({}, ...__VLS_functionalComponentArgsRest(__VLS_12));
}
else if (__VLS_ctx.isCostCardArchive) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof CostCardArchiveView, ]} */ ;
    // @ts-ignore
    const __VLS_15 = __VLS_asFunctionalComponent(CostCardArchiveView, new CostCardArchiveView({}));
    const __VLS_16 = __VLS_15({}, ...__VLS_functionalComponentArgsRest(__VLS_15));
}
else if (__VLS_ctx.isCostCardSetting) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof CostCardSettingView, ]} */ ;
    // @ts-ignore
    const __VLS_18 = __VLS_asFunctionalComponent(CostCardSettingView, new CostCardSettingView({}));
    const __VLS_19 = __VLS_18({}, ...__VLS_functionalComponentArgsRest(__VLS_18));
}
else if (__VLS_ctx.isDishWarehouseLink) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof DishWarehouseLinkView, ]} */ ;
    // @ts-ignore
    const __VLS_21 = __VLS_asFunctionalComponent(DishWarehouseLinkView, new DishWarehouseLinkView({}));
    const __VLS_22 = __VLS_21({}, ...__VLS_functionalComponentArgsRest(__VLS_21));
}
else if (__VLS_ctx.isExpenseWarehouseLink) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof ExpenseWarehouseLinkView, ]} */ ;
    // @ts-ignore
    const __VLS_24 = __VLS_asFunctionalComponent(ExpenseWarehouseLinkView, new ExpenseWarehouseLinkView({}));
    const __VLS_25 = __VLS_24({}, ...__VLS_functionalComponentArgsRest(__VLS_24));
}
else if (__VLS_ctx.isUserManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof UserManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_27 = __VLS_asFunctionalComponent(UserManagementView, new UserManagementView({}));
    const __VLS_28 = __VLS_27({}, ...__VLS_functionalComponentArgsRest(__VLS_27));
}
else if (__VLS_ctx.isRoleManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof RoleManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_30 = __VLS_asFunctionalComponent(RoleManagementView, new RoleManagementView({}));
    const __VLS_31 = __VLS_30({}, ...__VLS_functionalComponentArgsRest(__VLS_30));
}
else if (__VLS_ctx.isMenuPermissionManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof MenuPermissionManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_33 = __VLS_asFunctionalComponent(MenuPermissionManagementView, new MenuPermissionManagementView({}));
    const __VLS_34 = __VLS_33({}, ...__VLS_functionalComponentArgsRest(__VLS_33));
}
else if (__VLS_ctx.isSupplierManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof SupplierManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_36 = __VLS_asFunctionalComponent(SupplierManagementView, new SupplierManagementView({}));
    const __VLS_37 = __VLS_36({}, ...__VLS_functionalComponentArgsRest(__VLS_36));
}
else if (__VLS_ctx.isWarehouseManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof WarehouseManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_39 = __VLS_asFunctionalComponent(WarehouseManagementView, new WarehouseManagementView({}));
    const __VLS_40 = __VLS_39({}, ...__VLS_functionalComponentArgsRest(__VLS_39));
}
else if (__VLS_ctx.isWarehouseItemRuleManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof WarehouseItemRuleManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_42 = __VLS_asFunctionalComponent(WarehouseItemRuleManagementView, new WarehouseItemRuleManagementView({}));
    const __VLS_43 = __VLS_42({}, ...__VLS_functionalComponentArgsRest(__VLS_42));
}
else if (__VLS_ctx.isOrgManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof OrgManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(OrgManagementView, new OrgManagementView({}));
    const __VLS_46 = __VLS_45({}, ...__VLS_functionalComponentArgsRest(__VLS_45));
}
else if (__VLS_ctx.isPurchasePricing) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof PurchasePricingView, ]} */ ;
    // @ts-ignore
    const __VLS_48 = __VLS_asFunctionalComponent(PurchasePricingView, new PurchasePricingView({}));
    const __VLS_49 = __VLS_48({}, ...__VLS_functionalComponentArgsRest(__VLS_48));
}
else if (__VLS_ctx.isPricingAdjustmentManagement) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof PricingAdjustmentManagementView, ]} */ ;
    // @ts-ignore
    const __VLS_51 = __VLS_asFunctionalComponent(PricingAdjustmentManagementView, new PricingAdjustmentManagementView({}));
    const __VLS_52 = __VLS_51({}, ...__VLS_functionalComponentArgsRest(__VLS_51));
}
else if (__VLS_ctx.isWarehouseOpeningBalance) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof WarehouseOpeningBalanceView, ]} */ ;
    // @ts-ignore
    const __VLS_54 = __VLS_asFunctionalComponent(WarehouseOpeningBalanceView, new WarehouseOpeningBalanceView({}));
    const __VLS_55 = __VLS_54({}, ...__VLS_functionalComponentArgsRest(__VLS_54));
}
else if (__VLS_ctx.isPurchaseInbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof PurchaseInboundView, ]} */ ;
    // @ts-ignore
    const __VLS_57 = __VLS_asFunctionalComponent(PurchaseInboundView, new PurchaseInboundView({}));
    const __VLS_58 = __VLS_57({}, ...__VLS_functionalComponentArgsRest(__VLS_57));
}
else if (__VLS_ctx.isPurchaseReturnOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof PurchaseReturnOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_60 = __VLS_asFunctionalComponent(PurchaseReturnOutboundView, new PurchaseReturnOutboundView({}));
    const __VLS_61 = __VLS_60({}, ...__VLS_functionalComponentArgsRest(__VLS_60));
}
else if (__VLS_ctx.isStockTransfer) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StockTransferView, ]} */ ;
    // @ts-ignore
    const __VLS_63 = __VLS_asFunctionalComponent(StockTransferView, new StockTransferView({}));
    const __VLS_64 = __VLS_63({}, ...__VLS_functionalComponentArgsRest(__VLS_63));
}
else if (__VLS_ctx.isDepartmentPicking) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof DepartmentPickingView, ]} */ ;
    // @ts-ignore
    const __VLS_66 = __VLS_asFunctionalComponent(DepartmentPickingView, new DepartmentPickingView({}));
    const __VLS_67 = __VLS_66({}, ...__VLS_functionalComponentArgsRest(__VLS_66));
}
else if (__VLS_ctx.isDepartmentReturn) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof DepartmentReturnView, ]} */ ;
    // @ts-ignore
    const __VLS_69 = __VLS_asFunctionalComponent(DepartmentReturnView, new DepartmentReturnView({}));
    const __VLS_70 = __VLS_69({}, ...__VLS_functionalComponentArgsRest(__VLS_69));
}
else if (__VLS_ctx.isStockTransferInbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StockTransferInboundView, ]} */ ;
    // @ts-ignore
    const __VLS_72 = __VLS_asFunctionalComponent(StockTransferInboundView, new StockTransferInboundView({}));
    const __VLS_73 = __VLS_72({}, ...__VLS_functionalComponentArgsRest(__VLS_72));
}
else if (__VLS_ctx.isDepartmentTransfer) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof DepartmentTransferView, ]} */ ;
    // @ts-ignore
    const __VLS_75 = __VLS_asFunctionalComponent(DepartmentTransferView, new DepartmentTransferView({}));
    const __VLS_76 = __VLS_75({}, ...__VLS_functionalComponentArgsRest(__VLS_75));
}
else if (__VLS_ctx.isStoreTransfer) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StoreTransferView, ]} */ ;
    // @ts-ignore
    const __VLS_78 = __VLS_asFunctionalComponent(StoreTransferView, new StoreTransferView({}));
    const __VLS_79 = __VLS_78({}, ...__VLS_functionalComponentArgsRest(__VLS_78));
}
else if (__VLS_ctx.isDamageOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof DamageOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_81 = __VLS_asFunctionalComponent(DamageOutboundView, new DamageOutboundView({}));
    const __VLS_82 = __VLS_81({}, ...__VLS_functionalComponentArgsRest(__VLS_81));
}
else if (__VLS_ctx.isOtherInbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof OtherInboundView, ]} */ ;
    // @ts-ignore
    const __VLS_84 = __VLS_asFunctionalComponent(OtherInboundView, new OtherInboundView({}));
    const __VLS_85 = __VLS_84({}, ...__VLS_functionalComponentArgsRest(__VLS_84));
}
else if (__VLS_ctx.isOtherOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof OtherOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_87 = __VLS_asFunctionalComponent(OtherOutboundView, new OtherOutboundView({}));
    const __VLS_88 = __VLS_87({}, ...__VLS_functionalComponentArgsRest(__VLS_87));
}
else if (__VLS_ctx.isProductionInbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof ProductionInboundView, ]} */ ;
    // @ts-ignore
    const __VLS_90 = __VLS_asFunctionalComponent(ProductionInboundView, new ProductionInboundView({}));
    const __VLS_91 = __VLS_90({}, ...__VLS_functionalComponentArgsRest(__VLS_90));
}
else if (__VLS_ctx.isStockTransferOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StockTransferOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_93 = __VLS_asFunctionalComponent(StockTransferOutboundView, new StockTransferOutboundView({}));
    const __VLS_94 = __VLS_93({}, ...__VLS_functionalComponentArgsRest(__VLS_93));
}
else if (__VLS_ctx.isCustomerSalesOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof CustomerSalesOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_96 = __VLS_asFunctionalComponent(CustomerSalesOutboundView, new CustomerSalesOutboundView({}));
    const __VLS_97 = __VLS_96({}, ...__VLS_functionalComponentArgsRest(__VLS_96));
}
else if (__VLS_ctx.isCustomerReturnInbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof CustomerReturnInboundView, ]} */ ;
    // @ts-ignore
    const __VLS_99 = __VLS_asFunctionalComponent(CustomerReturnInboundView, new CustomerReturnInboundView({}));
    const __VLS_100 = __VLS_99({}, ...__VLS_functionalComponentArgsRest(__VLS_99));
}
else if (__VLS_ctx.isInventoryCheck) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof InventoryCheckView, ]} */ ;
    // @ts-ignore
    const __VLS_102 = __VLS_asFunctionalComponent(InventoryCheckView, new InventoryCheckView({}));
    const __VLS_103 = __VLS_102({}, ...__VLS_functionalComponentArgsRest(__VLS_102));
}
else if (__VLS_ctx.isMultiInventoryCheck) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof MultiInventoryCheckView, ]} */ ;
    // @ts-ignore
    const __VLS_105 = __VLS_asFunctionalComponent(MultiInventoryCheckView, new MultiInventoryCheckView({}));
    const __VLS_106 = __VLS_105({}, ...__VLS_functionalComponentArgsRest(__VLS_105));
}
else if (__VLS_ctx.isProfitInbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof ProfitInboundView, ]} */ ;
    // @ts-ignore
    const __VLS_108 = __VLS_asFunctionalComponent(ProfitInboundView, new ProfitInboundView({}));
    const __VLS_109 = __VLS_108({}, ...__VLS_functionalComponentArgsRest(__VLS_108));
}
else if (__VLS_ctx.isLossOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof LossOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(LossOutboundView, new LossOutboundView({}));
    const __VLS_112 = __VLS_111({}, ...__VLS_functionalComponentArgsRest(__VLS_111));
}
else if (__VLS_ctx.isDishConsumptionOutbound) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof DishConsumptionOutboundView, ]} */ ;
    // @ts-ignore
    const __VLS_114 = __VLS_asFunctionalComponent(DishConsumptionOutboundView, new DishConsumptionOutboundView({}));
    const __VLS_115 = __VLS_114({}, ...__VLS_functionalComponentArgsRest(__VLS_114));
}
else if (__VLS_ctx.isBatchAdjustment) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof BatchAdjustmentView, ]} */ ;
    // @ts-ignore
    const __VLS_117 = __VLS_asFunctionalComponent(BatchAdjustmentView, new BatchAdjustmentView({}));
    const __VLS_118 = __VLS_117({}, ...__VLS_functionalComponentArgsRest(__VLS_117));
}
else if (__VLS_ctx.isInventoryTemplate) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof InventoryTemplateView, ]} */ ;
    // @ts-ignore
    const __VLS_120 = __VLS_asFunctionalComponent(InventoryTemplateView, new InventoryTemplateView({}));
    const __VLS_121 = __VLS_120({}, ...__VLS_functionalComponentArgsRest(__VLS_120));
}
else if (__VLS_ctx.isTransferGroup) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof TransferGroupView, ]} */ ;
    // @ts-ignore
    const __VLS_123 = __VLS_asFunctionalComponent(TransferGroupView, new TransferGroupView({}));
    const __VLS_124 = __VLS_123({}, ...__VLS_functionalComponentArgsRest(__VLS_123));
}
else if (__VLS_ctx.isStockLimits) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StockLimitsView, ]} */ ;
    // @ts-ignore
    const __VLS_126 = __VLS_asFunctionalComponent(StockLimitsView, new StockLimitsView({}));
    const __VLS_127 = __VLS_126({}, ...__VLS_functionalComponentArgsRest(__VLS_126));
}
else if (__VLS_ctx.isStockLock) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StockLockView, ]} */ ;
    // @ts-ignore
    const __VLS_129 = __VLS_asFunctionalComponent(StockLockView, new StockLockView({}));
    const __VLS_130 = __VLS_129({}, ...__VLS_functionalComponentArgsRest(__VLS_129));
}
else if (__VLS_ctx.isStockLockLog) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid single" },
    });
    /** @type {[typeof StockLockLogView, ]} */ ;
    // @ts-ignore
    const __VLS_132 = __VLS_asFunctionalComponent(StockLockLogView, new StockLockLogView({}));
    const __VLS_133 = __VLS_132({}, ...__VLS_functionalComponentArgsRest(__VLS_132));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-grid" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "hero-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "eyebrow" },
    });
    (__VLS_ctx.breadcrumbs[0] ?? '业务中心');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
    (__VLS_ctx.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "hero-copy" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "hero-badge" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.section);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.routePath);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "panel" },
    });
    const __VLS_135 = {}.ElBreadcrumb;
    /** @type {[typeof __VLS_components.ElBreadcrumb, typeof __VLS_components.elBreadcrumb, typeof __VLS_components.ElBreadcrumb, typeof __VLS_components.elBreadcrumb, ]} */ ;
    // @ts-ignore
    const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({
        separator: "/",
    }));
    const __VLS_137 = __VLS_136({
        separator: "/",
    }, ...__VLS_functionalComponentArgsRest(__VLS_136));
    __VLS_138.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.breadcrumbs))) {
        const __VLS_139 = {}.ElBreadcrumbItem;
        /** @type {[typeof __VLS_components.ElBreadcrumbItem, typeof __VLS_components.elBreadcrumbItem, typeof __VLS_components.ElBreadcrumbItem, typeof __VLS_components.elBreadcrumbItem, ]} */ ;
        // @ts-ignore
        const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
            key: (item),
        }));
        const __VLS_141 = __VLS_140({
            key: (item),
        }, ...__VLS_functionalComponentArgsRest(__VLS_140));
        __VLS_142.slots.default;
        (item);
        var __VLS_142;
    }
    var __VLS_138;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "panel" },
    });
    const __VLS_143 = {}.ElDescriptions;
    /** @type {[typeof __VLS_components.ElDescriptions, typeof __VLS_components.elDescriptions, typeof __VLS_components.ElDescriptions, typeof __VLS_components.elDescriptions, ]} */ ;
    // @ts-ignore
    const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
        column: (1),
        border: true,
    }));
    const __VLS_145 = __VLS_144({
        column: (1),
        border: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_144));
    __VLS_146.slots.default;
    const __VLS_147 = {}.ElDescriptionsItem;
    /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
    // @ts-ignore
    const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({
        label: "当前功能",
    }));
    const __VLS_149 = __VLS_148({
        label: "当前功能",
    }, ...__VLS_functionalComponentArgsRest(__VLS_148));
    __VLS_150.slots.default;
    (__VLS_ctx.title);
    var __VLS_150;
    const __VLS_151 = {}.ElDescriptionsItem;
    /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
    // @ts-ignore
    const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({
        label: "所属模块",
    }));
    const __VLS_153 = __VLS_152({
        label: "所属模块",
    }, ...__VLS_functionalComponentArgsRest(__VLS_152));
    __VLS_154.slots.default;
    (__VLS_ctx.breadcrumbs[0] ?? '-');
    var __VLS_154;
    const __VLS_155 = {}.ElDescriptionsItem;
    /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
    // @ts-ignore
    const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
        label: "所属分组",
    }));
    const __VLS_157 = __VLS_156({
        label: "所属分组",
    }, ...__VLS_functionalComponentArgsRest(__VLS_156));
    __VLS_158.slots.default;
    (__VLS_ctx.section);
    var __VLS_158;
    const __VLS_159 = {}.ElDescriptionsItem;
    /** @type {[typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, typeof __VLS_components.ElDescriptionsItem, typeof __VLS_components.elDescriptionsItem, ]} */ ;
    // @ts-ignore
    const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({
        label: "路由路径",
    }));
    const __VLS_161 = __VLS_160({
        label: "路由路径",
    }, ...__VLS_functionalComponentArgsRest(__VLS_160));
    __VLS_162.slots.default;
    (__VLS_ctx.routePath);
    var __VLS_162;
    var __VLS_146;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
        ...{ class: "todo-list" },
    });
    for (const [task] of __VLS_getVForSourceType((__VLS_ctx.pendingTasks))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
            key: (task),
        });
        (task);
    }
}
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-card']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-copy']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['todo-list']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ItemManagementView: ItemManagementView,
            ItemCategoryManagementView: ItemCategoryManagementView,
            ItemTagManagementView: ItemTagManagementView,
            StatisticsTypeManagementView: StatisticsTypeManagementView,
            UnitManagementView: UnitManagementView,
            UserManagementView: UserManagementView,
            RoleManagementView: RoleManagementView,
            MenuPermissionManagementView: MenuPermissionManagementView,
            CostCardArchiveView: CostCardArchiveView,
            CostCardSettingView: CostCardSettingView,
            DishWarehouseLinkView: DishWarehouseLinkView,
            ExpenseWarehouseLinkView: ExpenseWarehouseLinkView,
            SupplierManagementView: SupplierManagementView,
            WarehouseManagementView: WarehouseManagementView,
            WarehouseItemRuleManagementView: WarehouseItemRuleManagementView,
            OrgManagementView: OrgManagementView,
            PurchasePricingView: PurchasePricingView,
            PricingAdjustmentManagementView: PricingAdjustmentManagementView,
            WarehouseOpeningBalanceView: WarehouseOpeningBalanceView,
            PurchaseInboundView: PurchaseInboundView,
            PurchaseReturnOutboundView: PurchaseReturnOutboundView,
            StockTransferView: StockTransferView,
            DepartmentPickingView: DepartmentPickingView,
            DepartmentReturnView: DepartmentReturnView,
            StockTransferInboundView: StockTransferInboundView,
            DepartmentTransferView: DepartmentTransferView,
            StoreTransferView: StoreTransferView,
            DamageOutboundView: DamageOutboundView,
            OtherInboundView: OtherInboundView,
            OtherOutboundView: OtherOutboundView,
            ProductionInboundView: ProductionInboundView,
            StockTransferOutboundView: StockTransferOutboundView,
            CustomerSalesOutboundView: CustomerSalesOutboundView,
            CustomerReturnInboundView: CustomerReturnInboundView,
            InventoryCheckView: InventoryCheckView,
            MultiInventoryCheckView: MultiInventoryCheckView,
            ProfitInboundView: ProfitInboundView,
            LossOutboundView: LossOutboundView,
            DishConsumptionOutboundView: DishConsumptionOutboundView,
            BatchAdjustmentView: BatchAdjustmentView,
            InventoryTemplateView: InventoryTemplateView,
            TransferGroupView: TransferGroupView,
            StockLimitsView: StockLimitsView,
            StockLockView: StockLockView,
            StockLockLogView: StockLockLogView,
            title: title,
            breadcrumbs: breadcrumbs,
            section: section,
            routePath: routePath,
            isItemManagement: isItemManagement,
            isItemCategoryManagement: isItemCategoryManagement,
            isItemTagManagement: isItemTagManagement,
            isStatisticsTypeManagement: isStatisticsTypeManagement,
            isUnitManagement: isUnitManagement,
            isCostCardArchive: isCostCardArchive,
            isCostCardSetting: isCostCardSetting,
            isDishWarehouseLink: isDishWarehouseLink,
            isExpenseWarehouseLink: isExpenseWarehouseLink,
            isUserManagement: isUserManagement,
            isRoleManagement: isRoleManagement,
            isMenuPermissionManagement: isMenuPermissionManagement,
            isSupplierManagement: isSupplierManagement,
            isWarehouseManagement: isWarehouseManagement,
            isWarehouseItemRuleManagement: isWarehouseItemRuleManagement,
            isOrgManagement: isOrgManagement,
            isPurchasePricing: isPurchasePricing,
            isPricingAdjustmentManagement: isPricingAdjustmentManagement,
            isWarehouseOpeningBalance: isWarehouseOpeningBalance,
            isPurchaseInbound: isPurchaseInbound,
            isPurchaseReturnOutbound: isPurchaseReturnOutbound,
            isStockTransfer: isStockTransfer,
            isDepartmentPicking: isDepartmentPicking,
            isDepartmentReturn: isDepartmentReturn,
            isStockTransferInbound: isStockTransferInbound,
            isDepartmentTransfer: isDepartmentTransfer,
            isStoreTransfer: isStoreTransfer,
            isDamageOutbound: isDamageOutbound,
            isOtherInbound: isOtherInbound,
            isOtherOutbound: isOtherOutbound,
            isProductionInbound: isProductionInbound,
            isStockTransferOutbound: isStockTransferOutbound,
            isCustomerSalesOutbound: isCustomerSalesOutbound,
            isCustomerReturnInbound: isCustomerReturnInbound,
            isInventoryCheck: isInventoryCheck,
            isMultiInventoryCheck: isMultiInventoryCheck,
            isProfitInbound: isProfitInbound,
            isLossOutbound: isLossOutbound,
            isDishConsumptionOutbound: isDishConsumptionOutbound,
            isBatchAdjustment: isBatchAdjustment,
            isInventoryTemplate: isInventoryTemplate,
            isTransferGroup: isTransferGroup,
            isStockLimits: isStockLimits,
            isStockLock: isStockLock,
            isStockLockLog: isStockLockLog,
            pendingTasks: pendingTasks,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
