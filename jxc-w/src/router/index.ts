import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { authStorage } from '@/api/auth-storage';
import { pinia } from '@/stores';
import { useMenuStore } from '@/stores/menu';
import { useSessionStore } from '@/stores/session';
import type { AppMenuItem } from '@/config/menu';
import { resolveMenuView } from '@/router/menu-view-registry';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/login.vue'),
    meta: {
      title: '登录',
      public: true,
    },
  },
  {
    path: '/select-org',
    name: 'SelectOrg',
    component: () => import('@/views/auth/select-org.vue'),
    meta: {
      title: '选择机构',
    },
  },
  {
    path: '/',
    name: 'AdminRoot',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/profile',
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: {
          title: '个人中心',
        },
      },
      {
        path: 'system/groups',
        name: 'SystemGroups',
        component: () => import('@/views/system/group-management.vue'),
        meta: {
          title: '集团管理',
        },
      },
      {
        path: 'group/stores',
        name: 'GroupStores',
        component: () => import('@/views/system/store-management.vue'),
        meta: {
          title: '门店管理',
        },
      },
      {
        path: 'group/users',
        name: 'GroupUserRoles',
        component: () => import('@/views/system/user-management.vue'),
        meta: {
          title: '用户管理',
        },
      },
      {
        path: 'group/roles',
        name: 'GroupRoles',
        component: () => import('@/views/system/role-management.vue'),
        meta: {
          title: '角色管理',
        },
      },
      {
        path: 'group/menu-permissions',
        name: 'GroupMenuPermissions',
        component: () => import('@/views/system/menu-permission-management.vue'),
        meta: {
          title: '菜单权限管理',
        },
      },
      {
        path: 'group/workflow-config',
        name: 'GroupWorkflowTemplateConfig',
        component: () => import('@/views/system/workflow-config.vue'),
        meta: {
          title: '流程模板配置',
        },
      },
      {
        path: 'order/order-documents/create',
        name: 'OrderDocumentCreate',
        component: () => import('@/views/order/order-document-create.vue'),
        meta: {
          title: '新增订货单',
          activeMenu: '/order/order-documents',
          breadcrumbs: ['订货管理', '单据', '订货单', '新增订货单'],
          openKeys: ['m1', 'm1-m3'],
        },
      },
      {
        path: 'group/workflow-history',
        name: 'GroupWorkflowHistory',
        component: () => import('@/views/system/workflow-publish-history-management.vue'),
        meta: {
          title: '流程发布历史管理',
        },
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/user-management.vue'),
        meta: {
          title: '用户管理',
        },
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/role-management.vue'),
        meta: {
          title: '角色管理',
        },
      },
      {
        path: 'system/dictionaries',
        name: 'SystemDictionaries',
        component: () => import('@/views/system/dictionary-management.vue'),
        meta: {
          title: '字典管理',
        },
      },
      {
        path: 'system/item-rule-create',
        name: 'ItemRuleCreate',
        component: () => import('@/views/system/warehouse-item-rule-create.vue'),
        meta: {
          title: '仓库物品规则新增',
          activeMenu: '/archive/warehouse-item-rules',
          breadcrumbs: ['档案管理', '仓库', '仓库物品规则', '仓库物品规则新增'],
          openKeys: ['m8', 'm8-m7'],
        },
      },
      {
        path: 'system/menu-permissions',
        name: 'SystemMenuPermissions',
        component: () => import('@/views/system/menu-permission-management.vue'),
        meta: {
          title: '菜单权限管理',
        },
      },
      {
        path: 'system/user-roles',
        name: 'SystemUserRoles',
        component: () => import('@/views/system/user-management.vue'),
        meta: {
          title: '用户管理',
        },
      },
      {
        path: 'archive/items/create',
        name: 'ItemCreate',
        component: () => import('@/views/items/create.vue'),
        meta: {
          title: '新增物品',
          activeMenu: '/archive/items',
          breadcrumbs: ['档案管理', '物品', '物品管理', '新增物品'],
          openKeys: ['m8', 'm8-m1'],
        },
      },
      {
        path: 'archive/cost-card-archives/create',
        name: 'FinishedCardCreate',
        component: () => import('@/views/cost/finished-card-create.vue'),
        meta: {
          title: '新增成品卡',
          activeMenu: '/archive/cost-card-archives',
          breadcrumbs: ['档案管理', '成本设置', '成本卡档案', '新增成品卡'],
          openKeys: ['m8', 'm8-m2'],
        },
      },
      {
        path: 'archive/suppliers/create',
        name: 'SupplierCreate',
        component: () => import('@/views/supplier/create.vue'),
        meta: {
          title: '新增供应商',
          activeMenu: '/archive/suppliers',
          breadcrumbs: ['档案管理', '供应商', '供应商档案', '新增供应商'],
          openKeys: ['m8', 'm8-m3'],
        },
      },
      {
        path: 'archive/suppliers/edit/:id',
        name: 'SupplierEdit',
        component: () => import('@/views/supplier/create.vue'),
        meta: {
          title: '编辑供应商',
          activeMenu: '/archive/suppliers',
          breadcrumbs: ['档案管理', '供应商', '供应商档案', '编辑供应商'],
          openKeys: ['m8', 'm8-m3'],
        },
      },
      {
        path: 'purchase/pricing/create',
        name: 'PurchasePricingCreate',
        component: () => import('@/views/purchase/pricing-create.vue'),
        meta: {
          title: '新增采购单定价',
          activeMenu: '/purchase/pricing',
          breadcrumbs: ['采购管理', '价格管理', '采购单定价', '新增采购单定价'],
          openKeys: ['m2', 'm2-m1'],
        },
      },
      {
        path: 'inventory/warehouse-opening-balances/create',
        name: 'WarehouseOpeningBalanceCreate',
        component: () => import('@/views/inventory/warehouse-opening-balance-create.vue'),
        meta: {
          title: '新增仓库期初',
          activeMenu: '/inventory/warehouse-opening-balances',
          breadcrumbs: ['库存管理', '库存单据', '仓库期初', '新增仓库期初'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/warehouse-opening-balances/view/:id',
        name: 'WarehouseOpeningBalanceView',
        component: () => import('@/views/inventory/warehouse-opening-balance-create.vue'),
        meta: {
          title: '查看仓库期初',
          activeMenu: '/inventory/warehouse-opening-balances',
          breadcrumbs: ['库存管理', '库存单据', '仓库期初', '查看仓库期初'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/warehouse-opening-balances/edit/:id',
        name: 'WarehouseOpeningBalanceEdit',
        component: () => import('@/views/inventory/warehouse-opening-balance-create.vue'),
        meta: {
          title: '编辑仓库期初',
          activeMenu: '/inventory/warehouse-opening-balances',
          breadcrumbs: ['库存管理', '库存单据', '仓库期初', '编辑仓库期初'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/purchase-inbounds/create',
        name: 'PurchaseInboundCreate',
        component: () => import('@/views/inventory/purchase-inbound-create.vue'),
        meta: {
          title: '新增采购入库',
          activeMenu: '/inventory/purchase-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '采购入库', '新增采购入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/purchase-inbounds/view/:id',
        name: 'PurchaseInboundView',
        component: () => import('@/views/inventory/purchase-inbound-create.vue'),
        meta: {
          title: '查看采购入库',
          activeMenu: '/inventory/purchase-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '采购入库', '查看采购入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/purchase-inbounds/edit/:id',
        name: 'PurchaseInboundEdit',
        component: () => import('@/views/inventory/purchase-inbound-create.vue'),
        meta: {
          title: '编辑采购入库',
          activeMenu: '/inventory/purchase-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '采购入库', '编辑采购入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/purchase-return-outbounds/create',
        name: 'PurchaseReturnOutboundCreate',
        component: () => import('@/views/inventory/purchase-return-outbound-create.vue'),
        meta: {
          title: '新增采购退货出库',
          activeMenu: '/inventory/purchase-return-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '采购退货出库', '新增采购退货出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/purchase-return-outbounds/view/:id',
        name: 'PurchaseReturnOutboundView',
        component: () => import('@/views/inventory/purchase-return-outbound-create.vue'),
        meta: {
          title: '查看采购退货出库',
          activeMenu: '/inventory/purchase-return-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '采购退货出库', '查看采购退货出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/purchase-return-outbounds/edit/:id',
        name: 'PurchaseReturnOutboundEdit',
        component: () => import('@/views/inventory/purchase-return-outbound-create.vue'),
        meta: {
          title: '编辑采购退货出库',
          activeMenu: '/inventory/purchase-return-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '采购退货出库', '编辑采购退货出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-pickings/create',
        name: 'DepartmentPickingCreate',
        component: () => import('@/views/inventory/department-picking-create.vue'),
        meta: {
          title: '新增部门领料',
          activeMenu: '/inventory/department-pickings',
          breadcrumbs: ['库存管理', '库存单据', '部门领料', '新增部门领料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-pickings/view/:id',
        name: 'DepartmentPickingView',
        component: () => import('@/views/inventory/department-picking-create.vue'),
        meta: {
          title: '查看部门领料',
          activeMenu: '/inventory/department-pickings',
          breadcrumbs: ['库存管理', '库存单据', '部门领料', '查看部门领料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-pickings/edit/:id',
        name: 'DepartmentPickingEdit',
        component: () => import('@/views/inventory/department-picking-create.vue'),
        meta: {
          title: '编辑部门领料',
          activeMenu: '/inventory/department-pickings',
          breadcrumbs: ['库存管理', '库存单据', '部门领料', '编辑部门领料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-returns/create',
        name: 'DepartmentReturnCreate',
        component: () => import('@/views/inventory/department-return-create.vue'),
        meta: {
          title: '新增部门退料',
          activeMenu: '/inventory/department-returns',
          breadcrumbs: ['库存管理', '库存单据', '部门退料', '新增部门退料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-returns/view/:id',
        name: 'DepartmentReturnView',
        component: () => import('@/views/inventory/department-return-create.vue'),
        meta: {
          title: '查看部门退料',
          activeMenu: '/inventory/department-returns',
          breadcrumbs: ['库存管理', '库存单据', '部门退料', '查看部门退料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-returns/edit/:id',
        name: 'DepartmentReturnEdit',
        component: () => import('@/views/inventory/department-return-create.vue'),
        meta: {
          title: '编辑部门退料',
          activeMenu: '/inventory/department-returns',
          breadcrumbs: ['库存管理', '库存单据', '部门退料', '编辑部门退料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfers/create',
        name: 'StockTransferCreate',
        component: () => import('@/views/inventory/stock-transfer-create.vue'),
        meta: {
          title: '新增移库单',
          activeMenu: '/inventory/stock-transfers',
          breadcrumbs: ['库存管理', '库存单据', '移库单', '新增移库单'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfers/view/:id',
        name: 'StockTransferView',
        component: () => import('@/views/inventory/stock-transfer-create.vue'),
        meta: {
          title: '查看移库单',
          activeMenu: '/inventory/stock-transfers',
          breadcrumbs: ['库存管理', '库存单据', '移库单', '查看移库单'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfers/edit/:id',
        name: 'StockTransferEdit',
        component: () => import('@/views/inventory/stock-transfer-create.vue'),
        meta: {
          title: '编辑移库单',
          activeMenu: '/inventory/stock-transfers',
          breadcrumbs: ['库存管理', '库存单据', '移库单', '编辑移库单'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfer-inbounds/create',
        name: 'StockTransferInboundCreate',
        component: () => import('@/views/inventory/stock-transfer-inbound-create.vue'),
        meta: {
          title: '新增移库入库',
          activeMenu: '/inventory/stock-transfer-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '移库入库', '新增移库入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfer-inbounds/view/:id',
        name: 'StockTransferInboundView',
        component: () => import('@/views/inventory/stock-transfer-inbound-create.vue'),
        meta: {
          title: '查看移库入库',
          activeMenu: '/inventory/stock-transfer-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '移库入库', '查看移库入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfer-inbounds/edit/:id',
        name: 'StockTransferInboundEdit',
        component: () => import('@/views/inventory/stock-transfer-inbound-create.vue'),
        meta: {
          title: '编辑移库入库',
          activeMenu: '/inventory/stock-transfer-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '移库入库', '编辑移库入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-transfers/create',
        name: 'DepartmentTransferCreate',
        component: () => import('@/views/inventory/department-transfer-create.vue'),
        meta: {
          title: '新增部门调拨',
          activeMenu: '/inventory/department-transfers',
          breadcrumbs: ['库存管理', '库存单据', '部门调拨', '新增部门调拨'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-transfers/view/:id',
        name: 'DepartmentTransferView',
        component: () => import('@/views/inventory/department-transfer-create.vue'),
        meta: {
          title: '查看部门调拨',
          activeMenu: '/inventory/department-transfers',
          breadcrumbs: ['库存管理', '库存单据', '部门调拨', '查看部门调拨'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/department-transfers/edit/:id',
        name: 'DepartmentTransferEdit',
        component: () => import('@/views/inventory/department-transfer-create.vue'),
        meta: {
          title: '编辑部门调拨',
          activeMenu: '/inventory/department-transfers',
          breadcrumbs: ['库存管理', '库存单据', '部门调拨', '编辑部门调拨'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/store-transfers/create',
        name: 'StoreTransferOutboundCreate',
        component: () => import('@/views/inventory/store-transfer-outbound-create.vue'),
        meta: {
          title: '新增店间调拨（调出）',
          activeMenu: '/inventory/store-transfers',
          breadcrumbs: ['库存管理', '库存单据', '店间调拨', '新增店间调拨（调出）'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/store-transfers/view/:id',
        name: 'StoreTransferOutboundView',
        component: () => import('@/views/inventory/store-transfer-outbound-create.vue'),
        meta: {
          title: '查看店间调拨（调出）',
          activeMenu: '/inventory/store-transfers',
          breadcrumbs: ['库存管理', '库存单据', '店间调拨', '查看店间调拨（调出）'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/store-transfers/edit/:id',
        name: 'StoreTransferOutboundEdit',
        component: () => import('@/views/inventory/store-transfer-outbound-create.vue'),
        meta: {
          title: '编辑店间调拨（调出）',
          activeMenu: '/inventory/store-transfers',
          breadcrumbs: ['库存管理', '库存单据', '店间调拨', '编辑店间调拨（调出）'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/damage-outbounds/create',
        name: 'DamageOutboundCreate',
        component: () => import('@/views/inventory/damage-outbound-create.vue'),
        meta: {
          title: '新增报损出库',
          activeMenu: '/inventory/damage-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '报损出库', '新增报损出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/damage-outbounds/view/:id',
        name: 'DamageOutboundView',
        component: () => import('@/views/inventory/damage-outbound-create.vue'),
        meta: {
          title: '查看报损出库',
          activeMenu: '/inventory/damage-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '报损出库', '查看报损出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/damage-outbounds/edit/:id',
        name: 'DamageOutboundEdit',
        component: () => import('@/views/inventory/damage-outbound-create.vue'),
        meta: {
          title: '编辑报损出库',
          activeMenu: '/inventory/damage-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '报损出库', '编辑报损出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/other-inbounds/create',
        name: 'OtherInboundCreate',
        component: () => import('@/views/inventory/other-inbound-create.vue'),
        meta: {
          title: '新增其他入库',
          activeMenu: '/inventory/other-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '其他入库', '新增其他入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/other-inbounds/view/:id',
        name: 'OtherInboundView',
        component: () => import('@/views/inventory/other-inbound-create.vue'),
        meta: {
          title: '查看其他入库',
          activeMenu: '/inventory/other-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '其他入库', '查看其他入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/other-inbounds/edit/:id',
        name: 'OtherInboundEdit',
        component: () => import('@/views/inventory/other-inbound-create.vue'),
        meta: {
          title: '编辑其他入库',
          activeMenu: '/inventory/other-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '其他入库', '编辑其他入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/other-outbounds/create',
        name: 'OtherOutboundCreate',
        component: () => import('@/views/inventory/other-outbound-create.vue'),
        meta: {
          title: '新增其他出库',
          activeMenu: '/inventory/other-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '其他出库', '新增其他出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/other-outbounds/view/:id',
        name: 'OtherOutboundView',
        component: () => import('@/views/inventory/other-outbound-create.vue'),
        meta: {
          title: '查看其他出库',
          activeMenu: '/inventory/other-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '其他出库', '查看其他出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/other-outbounds/edit/:id',
        name: 'OtherOutboundEdit',
        component: () => import('@/views/inventory/other-outbound-create.vue'),
        meta: {
          title: '编辑其他出库',
          activeMenu: '/inventory/other-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '其他出库', '编辑其他出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/production-inbounds/create',
        name: 'ProductionInboundCreate',
        component: () => import('@/views/inventory/production-inbound-create.vue'),
        meta: {
          title: '新增生产入库',
          activeMenu: '/inventory/production-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '生产入库', '新增生产入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/production-inbounds/view/:id',
        name: 'ProductionInboundView',
        component: () => import('@/views/inventory/production-inbound-create.vue'),
        meta: {
          title: '查看生产入库',
          activeMenu: '/inventory/production-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '生产入库', '查看生产入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/production-inbounds/edit/:id',
        name: 'ProductionInboundEdit',
        component: () => import('@/views/inventory/production-inbound-create.vue'),
        meta: {
          title: '编辑生产入库',
          activeMenu: '/inventory/production-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '生产入库', '编辑生产入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/customer-sales-outbounds/create',
        name: 'CustomerSalesOutboundCreate',
        component: () => import('@/views/inventory/customer-sales-outbound-create.vue'),
        meta: {
          title: '新增客户销售出库',
          activeMenu: '/inventory/customer-sales-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '客户销售出库', '新增客户销售出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/customer-sales-outbounds/view/:id',
        name: 'CustomerSalesOutboundView',
        component: () => import('@/views/inventory/customer-sales-outbound-create.vue'),
        meta: {
          title: '查看客户销售出库',
          activeMenu: '/inventory/customer-sales-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '客户销售出库', '查看客户销售出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/customer-sales-outbounds/edit/:id',
        name: 'CustomerSalesOutboundEdit',
        component: () => import('@/views/inventory/customer-sales-outbound-create.vue'),
        meta: {
          title: '编辑客户销售出库',
          activeMenu: '/inventory/customer-sales-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '客户销售出库', '编辑客户销售出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/customer-return-inbounds/create',
        name: 'CustomerReturnInboundCreate',
        component: () => import('@/views/inventory/customer-return-inbound-create.vue'),
        meta: {
          title: '新增客户退货入库',
          activeMenu: '/inventory/customer-return-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '客户退货入库', '新增客户退货入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/customer-return-inbounds/view/:id',
        name: 'CustomerReturnInboundView',
        component: () => import('@/views/inventory/customer-return-inbound-create.vue'),
        meta: {
          title: '查看客户退货入库',
          activeMenu: '/inventory/customer-return-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '客户退货入库', '查看客户退货入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/customer-return-inbounds/edit/:id',
        name: 'CustomerReturnInboundEdit',
        component: () => import('@/views/inventory/customer-return-inbound-create.vue'),
        meta: {
          title: '编辑客户退货入库',
          activeMenu: '/inventory/customer-return-inbounds',
          breadcrumbs: ['库存管理', '库存单据', '客户退货入库', '编辑客户退货入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfer-outbounds/create',
        name: 'StockTransferOutboundCreate',
        component: () => import('@/views/inventory/stock-transfer-outbound-create.vue'),
        meta: {
          title: '新增移库出库',
          activeMenu: '/inventory/stock-transfer-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '移库出库', '新增移库出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfer-outbounds/view/:id',
        name: 'StockTransferOutboundView',
        component: () => import('@/views/inventory/stock-transfer-outbound-create.vue'),
        meta: {
          title: '查看移库出库',
          activeMenu: '/inventory/stock-transfer-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '移库出库', '查看移库出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/stock-transfer-outbounds/edit/:id',
        name: 'StockTransferOutboundEdit',
        component: () => import('@/views/inventory/stock-transfer-outbound-create.vue'),
        meta: {
          title: '编辑移库出库',
          activeMenu: '/inventory/stock-transfer-outbounds',
          breadcrumbs: ['库存管理', '库存单据', '移库出库', '编辑移库出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/inventory-checks/create',
        name: 'InventoryCheckCreate',
        component: () => import('@/views/inventory/inventory-check-create.vue'),
        meta: {
          title: '新增盘点单',
          activeMenu: '/inventory/inventory-checks',
          breadcrumbs: ['库存管理', '盘点管理', '盘点单', '新增盘点单'],
          openKeys: ['m4', 'm4-m2'],
        },
      },
      {
        path: 'inventory/inventory-checks/view/:id',
        name: 'InventoryCheckView',
        component: () => import('@/views/inventory/inventory-check-create.vue'),
        meta: {
          title: '查看盘点单',
          activeMenu: '/inventory/inventory-checks',
          breadcrumbs: ['库存管理', '盘点管理', '盘点单', '查看盘点单'],
          openKeys: ['m4', 'm4-m2'],
        },
      },
      {
        path: 'inventory/inventory-checks/edit/:id',
        name: 'InventoryCheckEdit',
        component: () => import('@/views/inventory/inventory-check-create.vue'),
        meta: {
          title: '编辑盘点单',
          activeMenu: '/inventory/inventory-checks',
          breadcrumbs: ['库存管理', '盘点管理', '盘点单', '编辑盘点单'],
          openKeys: ['m4', 'm4-m2'],
        },
      },
      {
        path: 'inventory/inventory-templates/create',
        name: 'InventoryTemplateCreate',
        component: () => import('@/views/inventory/inventory-template-create.vue'),
        meta: {
          title: '新增库存模板',
          activeMenu: '/inventory/inventory-templates',
          breadcrumbs: ['库存管理', '库存规则', '库存模板', '新增库存模板'],
          openKeys: ['m4', 'm4-m5'],
        },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

const PROFILE_HOME_PATH = '/profile';
const SELECT_ORG_PATH = '/select-org';
const runtimeMenuRouteNames = new Set<string>();

const normalizeMenuPath = (path?: string | null) => {
  const normalizedPath = String(path ?? '').trim().replace(/\/+$/, '');
  if (!normalizedPath.startsWith('/')) {
    return '';
  }
  return normalizedPath;
};

const toRuntimeRouteName = (item: AppMenuItem) => `RuntimeMenu_${item.menuCode ?? item.key}`;

const removeRuntimeMenuRoutes = () => {
  runtimeMenuRouteNames.forEach((name) => {
    if (router.hasRoute(name)) {
      router.removeRoute(name);
    }
  });
  runtimeMenuRouteNames.clear();
};

const buildRuntimeMenuRoutes = (
  items: AppMenuItem[],
  parentTrail: AppMenuItem[] = [],
): RouteRecordRaw[] => items.flatMap((item) => {
  const currentTrail = [...parentTrail, item];
  const children = item.children?.length ? buildRuntimeMenuRoutes(item.children, currentTrail) : [];
  const path = normalizeMenuPath(item.path);

  if (!path) {
    return children;
  }

  const route: RouteRecordRaw = {
    path: path.slice(1),
    name: toRuntimeRouteName(item),
    component: resolveMenuView(item.componentKey),
    meta: {
      title: item.title,
      activeMenu: path,
      breadcrumbs: currentTrail.map((trailItem) => trailItem.title),
      openKeys: parentTrail.map((trailItem) => trailItem.key),
      menuCode: item.menuCode,
      componentKey: item.componentKey,
    },
  };

  return [route, ...children];
});

const syncRuntimeMenuRoutes = (items: AppMenuItem[]) => {
  removeRuntimeMenuRoutes();
  buildRuntimeMenuRoutes(items).forEach((route) => {
    router.addRoute('AdminRoot', route);
    if (typeof route.name === 'string') {
      runtimeMenuRouteNames.add(route.name);
    }
  });
};

const flattenMenuPaths = (items: AppMenuItem[]): string[] => items.flatMap((item) => {
  const children = item.children?.length ? flattenMenuPaths(item.children) : [];
  const currentPath = normalizeMenuPath(item.path);
  return currentPath ? [currentPath, ...children] : children;
});

const canResolvePath = (path: string) => {
  const normalizedPath = String(path ?? '').trim().replace(/\/+$/, '');
  if (!normalizedPath || !normalizedPath.startsWith('/')) {
    return false;
  }
  return router.resolve(normalizedPath).matched.length > 0;
};

const resolveMenuHomePath = async (
  sessionStore: ReturnType<typeof useSessionStore>,
  menuStore: ReturnType<typeof useMenuStore>,
) => {
  if (!sessionStore.isLoggedIn) {
    return '/login';
  }
  if (!sessionStore.requiresOrgSelection) {
    return PROFILE_HOME_PATH;
  }
  if (!sessionStore.hasSelectedOrg) {
    return SELECT_ORG_PATH;
  }
  if (!authStorage.getAccessToken()) {
    return PROFILE_HOME_PATH;
  }
  const targetOrgId = sessionStore.currentOrgId;
  if (targetOrgId && (menuStore.loadedOrgId !== targetOrgId || !menuStore.menuItems.length)) {
    try {
      await menuStore.loadMenus(targetOrgId);
      syncRuntimeMenuRoutes(menuStore.menuItems);
    } catch {
      menuStore.clearMenus();
      removeRuntimeMenuRoutes();
    }
  }
  return PROFILE_HOME_PATH;
};

router.beforeEach(async (to) => {
  const sessionStore = useSessionStore(pinia);
  const menuStore = useMenuStore(pinia);
  const isPublic = Boolean(to.meta.public);

  if (!sessionStore.isLoggedIn && !isPublic) {
    return '/login';
  }

  if (sessionStore.isLoggedIn && to.path === '/login') {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && !sessionStore.hasSelectedOrg && to.path !== SELECT_ORG_PATH) {
    return SELECT_ORG_PATH;
  }

  if (sessionStore.isLoggedIn && !sessionStore.requiresOrgSelection && to.path === SELECT_ORG_PATH) {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && sessionStore.hasSelectedOrg && to.path === SELECT_ORG_PATH) {
    return PROFILE_HOME_PATH;
  }

  if (to.path === '/dashboard') {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  const normalizedToPath = normalizeMenuPath(to.path);

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && sessionStore.hasSelectedOrg) {
    if (!authStorage.getAccessToken()) {
      return PROFILE_HOME_PATH;
    }
    const targetOrgId = sessionStore.currentOrgId;
    if (menuStore.loadedOrgId !== targetOrgId || !menuStore.menuItems.length) {
      try {
        await menuStore.loadMenus(targetOrgId);
        syncRuntimeMenuRoutes(menuStore.menuItems);
      } catch {
        menuStore.clearMenus();
        removeRuntimeMenuRoutes();
        return PROFILE_HOME_PATH;
      }
    }
    const allowedPaths = new Set(flattenMenuPaths(menuStore.menuItems));
    const activeMenuPath = typeof to.meta.activeMenu === 'string'
      ? normalizeMenuPath(to.meta.activeMenu)
      : '';
    const canAccessWorkflowConfig = to.path === '/group/workflow-config'
      && allowedPaths.has('/group/workflow-history');
    const canAccessProfile = to.path === '/profile';
    const canAccess = allowedPaths.has(normalizedToPath || to.path)
      || (activeMenuPath && allowedPaths.has(activeMenuPath))
      || canAccessWorkflowConfig
      || canAccessProfile;
    if (allowedPaths.size > 0 && !canAccess) {
      return resolveMenuHomePath(sessionStore, menuStore);
    }
  }

  if (sessionStore.isLoggedIn && to.path !== SELECT_ORG_PATH && to.path !== '/login' && to.path !== '/profile' && !canResolvePath(normalizedToPath || to.path)) {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  return true;
});

router.afterEach((to) => {
  document.title = `${to.meta.title ?? '管理后台'} | 进销存管`;
});

export default router;
