import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { featureRoutes } from '@/config/menu';
import { authStorage } from '@/api/auth-storage';
import { pinia } from '@/stores';
import { useMenuStore } from '@/stores/menu';
import { useSessionStore } from '@/stores/session';
import type { AppMenuItem } from '@/config/menu';

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
        path: 'group/user-role',
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
        path: 'system/item-rule-create',
        name: 'ItemRuleCreate',
        component: () => import('@/views/system/warehouse-item-rule-create.vue'),
        meta: {
          title: '仓库物品规则新增',
          activeMenu: '/archive/7/2',
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
        path: 'archive/1/1/create',
        name: 'ItemCreate',
        component: () => import('@/views/items/create.vue'),
        meta: {
          title: '新增物品',
          activeMenu: '/archive/1/1',
          breadcrumbs: ['档案管理', '物品', '物品管理', '新增物品'],
          openKeys: ['m8', 'm8-m1'],
        },
      },
      {
        path: 'archive/2/1/create',
        name: 'FinishedCardCreate',
        component: () => import('@/views/cost/finished-card-create.vue'),
        meta: {
          title: '新增成品卡',
          activeMenu: '/archive/2/1',
          breadcrumbs: ['档案管理', '成本设置', '成本卡档案', '新增成品卡'],
          openKeys: ['m8', 'm8-m2'],
        },
      },
      {
        path: 'archive/3/1/create',
        name: 'SupplierCreate',
        component: () => import('@/views/supplier/create.vue'),
        meta: {
          title: '新增供应商',
          activeMenu: '/archive/3/1',
          breadcrumbs: ['档案管理', '供应商', '供应商档案', '新增供应商'],
          openKeys: ['m8', 'm8-m3'],
        },
      },
      {
        path: 'archive/3/1/edit/:id',
        name: 'SupplierEdit',
        component: () => import('@/views/supplier/create.vue'),
        meta: {
          title: '编辑供应商',
          activeMenu: '/archive/3/1',
          breadcrumbs: ['档案管理', '供应商', '供应商档案', '编辑供应商'],
          openKeys: ['m8', 'm8-m3'],
        },
      },
      {
        path: 'purchase/1/1/create',
        name: 'PurchasePricingCreate',
        component: () => import('@/views/purchase/pricing-create.vue'),
        meta: {
          title: '新增采购单定价',
          activeMenu: '/purchase/1/1',
          breadcrumbs: ['采购管理', '价格管理', '采购单定价', '新增采购单定价'],
          openKeys: ['m2', 'm2-m1'],
        },
      },
      {
        path: 'inventory/1/1/create',
        name: 'WarehouseOpeningBalanceCreate',
        component: () => import('@/views/inventory/warehouse-opening-balance-create.vue'),
        meta: {
          title: '新增仓库期初',
          activeMenu: '/inventory/1/1',
          breadcrumbs: ['库存管理', '库存单据', '仓库期初', '新增仓库期初'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/1/view/:id',
        name: 'WarehouseOpeningBalanceView',
        component: () => import('@/views/inventory/warehouse-opening-balance-create.vue'),
        meta: {
          title: '查看仓库期初',
          activeMenu: '/inventory/1/1',
          breadcrumbs: ['库存管理', '库存单据', '仓库期初', '查看仓库期初'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/1/edit/:id',
        name: 'WarehouseOpeningBalanceEdit',
        component: () => import('@/views/inventory/warehouse-opening-balance-create.vue'),
        meta: {
          title: '编辑仓库期初',
          activeMenu: '/inventory/1/1',
          breadcrumbs: ['库存管理', '库存单据', '仓库期初', '编辑仓库期初'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/2/create',
        name: 'PurchaseInboundCreate',
        component: () => import('@/views/inventory/purchase-inbound-create.vue'),
        meta: {
          title: '新增采购入库',
          activeMenu: '/inventory/1/2',
          breadcrumbs: ['库存管理', '库存单据', '采购入库', '新增采购入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/2/view/:id',
        name: 'PurchaseInboundView',
        component: () => import('@/views/inventory/purchase-inbound-create.vue'),
        meta: {
          title: '查看采购入库',
          activeMenu: '/inventory/1/2',
          breadcrumbs: ['库存管理', '库存单据', '采购入库', '查看采购入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/2/edit/:id',
        name: 'PurchaseInboundEdit',
        component: () => import('@/views/inventory/purchase-inbound-create.vue'),
        meta: {
          title: '编辑采购入库',
          activeMenu: '/inventory/1/2',
          breadcrumbs: ['库存管理', '库存单据', '采购入库', '编辑采购入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/3/create',
        name: 'PurchaseReturnOutboundCreate',
        component: () => import('@/views/inventory/purchase-return-outbound-create.vue'),
        meta: {
          title: '新增采购退货出库',
          activeMenu: '/inventory/1/3',
          breadcrumbs: ['库存管理', '库存单据', '采购退货出库', '新增采购退货出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/3/view/:id',
        name: 'PurchaseReturnOutboundView',
        component: () => import('@/views/inventory/purchase-return-outbound-create.vue'),
        meta: {
          title: '查看采购退货出库',
          activeMenu: '/inventory/1/3',
          breadcrumbs: ['库存管理', '库存单据', '采购退货出库', '查看采购退货出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/3/edit/:id',
        name: 'PurchaseReturnOutboundEdit',
        component: () => import('@/views/inventory/purchase-return-outbound-create.vue'),
        meta: {
          title: '编辑采购退货出库',
          activeMenu: '/inventory/1/3',
          breadcrumbs: ['库存管理', '库存单据', '采购退货出库', '编辑采购退货出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/4/create',
        name: 'DepartmentPickingCreate',
        component: () => import('@/views/inventory/department-picking-create.vue'),
        meta: {
          title: '新增部门领料',
          activeMenu: '/inventory/1/4',
          breadcrumbs: ['库存管理', '库存单据', '部门领料', '新增部门领料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/4/view/:id',
        name: 'DepartmentPickingView',
        component: () => import('@/views/inventory/department-picking-create.vue'),
        meta: {
          title: '查看部门领料',
          activeMenu: '/inventory/1/4',
          breadcrumbs: ['库存管理', '库存单据', '部门领料', '查看部门领料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/4/edit/:id',
        name: 'DepartmentPickingEdit',
        component: () => import('@/views/inventory/department-picking-create.vue'),
        meta: {
          title: '编辑部门领料',
          activeMenu: '/inventory/1/4',
          breadcrumbs: ['库存管理', '库存单据', '部门领料', '编辑部门领料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/5/create',
        name: 'DepartmentReturnCreate',
        component: () => import('@/views/inventory/department-return-create.vue'),
        meta: {
          title: '新增部门退料',
          activeMenu: '/inventory/1/5',
          breadcrumbs: ['库存管理', '库存单据', '部门退料', '新增部门退料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/5/view/:id',
        name: 'DepartmentReturnView',
        component: () => import('@/views/inventory/department-return-create.vue'),
        meta: {
          title: '查看部门退料',
          activeMenu: '/inventory/1/5',
          breadcrumbs: ['库存管理', '库存单据', '部门退料', '查看部门退料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/5/edit/:id',
        name: 'DepartmentReturnEdit',
        component: () => import('@/views/inventory/department-return-create.vue'),
        meta: {
          title: '编辑部门退料',
          activeMenu: '/inventory/1/5',
          breadcrumbs: ['库存管理', '库存单据', '部门退料', '编辑部门退料'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/6/create',
        name: 'StockTransferCreate',
        component: () => import('@/views/inventory/stock-transfer-create.vue'),
        meta: {
          title: '新增移库单',
          activeMenu: '/inventory/1/6',
          breadcrumbs: ['库存管理', '库存单据', '移库单', '新增移库单'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/6/view/:id',
        name: 'StockTransferView',
        component: () => import('@/views/inventory/stock-transfer-create.vue'),
        meta: {
          title: '查看移库单',
          activeMenu: '/inventory/1/6',
          breadcrumbs: ['库存管理', '库存单据', '移库单', '查看移库单'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/6/edit/:id',
        name: 'StockTransferEdit',
        component: () => import('@/views/inventory/stock-transfer-create.vue'),
        meta: {
          title: '编辑移库单',
          activeMenu: '/inventory/1/6',
          breadcrumbs: ['库存管理', '库存单据', '移库单', '编辑移库单'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/7/create',
        name: 'StockTransferInboundCreate',
        component: () => import('@/views/inventory/stock-transfer-inbound-create.vue'),
        meta: {
          title: '新增移库入库',
          activeMenu: '/inventory/1/7',
          breadcrumbs: ['库存管理', '库存单据', '移库入库', '新增移库入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/7/view/:id',
        name: 'StockTransferInboundView',
        component: () => import('@/views/inventory/stock-transfer-inbound-create.vue'),
        meta: {
          title: '查看移库入库',
          activeMenu: '/inventory/1/7',
          breadcrumbs: ['库存管理', '库存单据', '移库入库', '查看移库入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/7/edit/:id',
        name: 'StockTransferInboundEdit',
        component: () => import('@/views/inventory/stock-transfer-inbound-create.vue'),
        meta: {
          title: '编辑移库入库',
          activeMenu: '/inventory/1/7',
          breadcrumbs: ['库存管理', '库存单据', '移库入库', '编辑移库入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/8/create',
        name: 'DepartmentTransferCreate',
        component: () => import('@/views/inventory/department-transfer-create.vue'),
        meta: {
          title: '新增部门调拨',
          activeMenu: '/inventory/1/8',
          breadcrumbs: ['库存管理', '库存单据', '部门调拨', '新增部门调拨'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/8/view/:id',
        name: 'DepartmentTransferView',
        component: () => import('@/views/inventory/department-transfer-create.vue'),
        meta: {
          title: '查看部门调拨',
          activeMenu: '/inventory/1/8',
          breadcrumbs: ['库存管理', '库存单据', '部门调拨', '查看部门调拨'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/8/edit/:id',
        name: 'DepartmentTransferEdit',
        component: () => import('@/views/inventory/department-transfer-create.vue'),
        meta: {
          title: '编辑部门调拨',
          activeMenu: '/inventory/1/8',
          breadcrumbs: ['库存管理', '库存单据', '部门调拨', '编辑部门调拨'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/9/create',
        name: 'StoreTransferOutboundCreate',
        component: () => import('@/views/inventory/store-transfer-outbound-create.vue'),
        meta: {
          title: '新增店间调拨（调出）',
          activeMenu: '/inventory/1/9',
          breadcrumbs: ['库存管理', '库存单据', '店间调拨', '新增店间调拨（调出）'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/9/view/:id',
        name: 'StoreTransferOutboundView',
        component: () => import('@/views/inventory/store-transfer-outbound-create.vue'),
        meta: {
          title: '查看店间调拨（调出）',
          activeMenu: '/inventory/1/9',
          breadcrumbs: ['库存管理', '库存单据', '店间调拨', '查看店间调拨（调出）'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/9/edit/:id',
        name: 'StoreTransferOutboundEdit',
        component: () => import('@/views/inventory/store-transfer-outbound-create.vue'),
        meta: {
          title: '编辑店间调拨（调出）',
          activeMenu: '/inventory/1/9',
          breadcrumbs: ['库存管理', '库存单据', '店间调拨', '编辑店间调拨（调出）'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/10/create',
        name: 'DamageOutboundCreate',
        component: () => import('@/views/inventory/damage-outbound-create.vue'),
        meta: {
          title: '新增报损出库',
          activeMenu: '/inventory/1/10',
          breadcrumbs: ['库存管理', '库存单据', '报损出库', '新增报损出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/10/view/:id',
        name: 'DamageOutboundView',
        component: () => import('@/views/inventory/damage-outbound-create.vue'),
        meta: {
          title: '查看报损出库',
          activeMenu: '/inventory/1/10',
          breadcrumbs: ['库存管理', '库存单据', '报损出库', '查看报损出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/10/edit/:id',
        name: 'DamageOutboundEdit',
        component: () => import('@/views/inventory/damage-outbound-create.vue'),
        meta: {
          title: '编辑报损出库',
          activeMenu: '/inventory/1/10',
          breadcrumbs: ['库存管理', '库存单据', '报损出库', '编辑报损出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/11/create',
        name: 'OtherInboundCreate',
        component: () => import('@/views/inventory/other-inbound-create.vue'),
        meta: {
          title: '新增其他入库',
          activeMenu: '/inventory/1/11',
          breadcrumbs: ['库存管理', '库存单据', '其他入库', '新增其他入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/11/view/:id',
        name: 'OtherInboundView',
        component: () => import('@/views/inventory/other-inbound-create.vue'),
        meta: {
          title: '查看其他入库',
          activeMenu: '/inventory/1/11',
          breadcrumbs: ['库存管理', '库存单据', '其他入库', '查看其他入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/11/edit/:id',
        name: 'OtherInboundEdit',
        component: () => import('@/views/inventory/other-inbound-create.vue'),
        meta: {
          title: '编辑其他入库',
          activeMenu: '/inventory/1/11',
          breadcrumbs: ['库存管理', '库存单据', '其他入库', '编辑其他入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/12/create',
        name: 'OtherOutboundCreate',
        component: () => import('@/views/inventory/other-outbound-create.vue'),
        meta: {
          title: '新增其他出库',
          activeMenu: '/inventory/1/12',
          breadcrumbs: ['库存管理', '库存单据', '其他出库', '新增其他出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/12/view/:id',
        name: 'OtherOutboundView',
        component: () => import('@/views/inventory/other-outbound-create.vue'),
        meta: {
          title: '查看其他出库',
          activeMenu: '/inventory/1/12',
          breadcrumbs: ['库存管理', '库存单据', '其他出库', '查看其他出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/12/edit/:id',
        name: 'OtherOutboundEdit',
        component: () => import('@/views/inventory/other-outbound-create.vue'),
        meta: {
          title: '编辑其他出库',
          activeMenu: '/inventory/1/12',
          breadcrumbs: ['库存管理', '库存单据', '其他出库', '编辑其他出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/13/create',
        name: 'ProductionInboundCreate',
        component: () => import('@/views/inventory/production-inbound-create.vue'),
        meta: {
          title: '新增生产入库',
          activeMenu: '/inventory/1/13',
          breadcrumbs: ['库存管理', '库存单据', '生产入库', '新增生产入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/13/view/:id',
        name: 'ProductionInboundView',
        component: () => import('@/views/inventory/production-inbound-create.vue'),
        meta: {
          title: '查看生产入库',
          activeMenu: '/inventory/1/13',
          breadcrumbs: ['库存管理', '库存单据', '生产入库', '查看生产入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/13/edit/:id',
        name: 'ProductionInboundEdit',
        component: () => import('@/views/inventory/production-inbound-create.vue'),
        meta: {
          title: '编辑生产入库',
          activeMenu: '/inventory/1/13',
          breadcrumbs: ['库存管理', '库存单据', '生产入库', '编辑生产入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/14/create',
        name: 'CustomerSalesOutboundCreate',
        component: () => import('@/views/inventory/customer-sales-outbound-create.vue'),
        meta: {
          title: '新增客户销售出库',
          activeMenu: '/inventory/1/14',
          breadcrumbs: ['库存管理', '库存单据', '客户销售出库', '新增客户销售出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/14/view/:id',
        name: 'CustomerSalesOutboundView',
        component: () => import('@/views/inventory/customer-sales-outbound-create.vue'),
        meta: {
          title: '查看客户销售出库',
          activeMenu: '/inventory/1/14',
          breadcrumbs: ['库存管理', '库存单据', '客户销售出库', '查看客户销售出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/14/edit/:id',
        name: 'CustomerSalesOutboundEdit',
        component: () => import('@/views/inventory/customer-sales-outbound-create.vue'),
        meta: {
          title: '编辑客户销售出库',
          activeMenu: '/inventory/1/14',
          breadcrumbs: ['库存管理', '库存单据', '客户销售出库', '编辑客户销售出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/15/create',
        name: 'CustomerReturnInboundCreate',
        component: () => import('@/views/inventory/customer-return-inbound-create.vue'),
        meta: {
          title: '新增客户退货入库',
          activeMenu: '/inventory/1/15',
          breadcrumbs: ['库存管理', '库存单据', '客户退货入库', '新增客户退货入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/15/view/:id',
        name: 'CustomerReturnInboundView',
        component: () => import('@/views/inventory/customer-return-inbound-create.vue'),
        meta: {
          title: '查看客户退货入库',
          activeMenu: '/inventory/1/15',
          breadcrumbs: ['库存管理', '库存单据', '客户退货入库', '查看客户退货入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/15/edit/:id',
        name: 'CustomerReturnInboundEdit',
        component: () => import('@/views/inventory/customer-return-inbound-create.vue'),
        meta: {
          title: '编辑客户退货入库',
          activeMenu: '/inventory/1/15',
          breadcrumbs: ['库存管理', '库存单据', '客户退货入库', '编辑客户退货入库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/16/create',
        name: 'StockTransferOutboundCreate',
        component: () => import('@/views/inventory/stock-transfer-outbound-create.vue'),
        meta: {
          title: '新增移库出库',
          activeMenu: '/inventory/1/16',
          breadcrumbs: ['库存管理', '库存单据', '移库出库', '新增移库出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/16/view/:id',
        name: 'StockTransferOutboundView',
        component: () => import('@/views/inventory/stock-transfer-outbound-create.vue'),
        meta: {
          title: '查看移库出库',
          activeMenu: '/inventory/1/16',
          breadcrumbs: ['库存管理', '库存单据', '移库出库', '查看移库出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/1/16/edit/:id',
        name: 'StockTransferOutboundEdit',
        component: () => import('@/views/inventory/stock-transfer-outbound-create.vue'),
        meta: {
          title: '编辑移库出库',
          activeMenu: '/inventory/1/16',
          breadcrumbs: ['库存管理', '库存单据', '移库出库', '编辑移库出库'],
          openKeys: ['m4', 'm4-m1'],
        },
      },
      {
        path: 'inventory/2/1/create',
        name: 'InventoryCheckCreate',
        component: () => import('@/views/inventory/inventory-check-create.vue'),
        meta: {
          title: '新增盘点单',
          activeMenu: '/inventory/2/1',
          breadcrumbs: ['库存管理', '盘点管理', '盘点单', '新增盘点单'],
          openKeys: ['m4', 'm4-m2'],
        },
      },
      {
        path: 'inventory/2/1/view/:id',
        name: 'InventoryCheckView',
        component: () => import('@/views/inventory/inventory-check-create.vue'),
        meta: {
          title: '查看盘点单',
          activeMenu: '/inventory/2/1',
          breadcrumbs: ['库存管理', '盘点管理', '盘点单', '查看盘点单'],
          openKeys: ['m4', 'm4-m2'],
        },
      },
      {
        path: 'inventory/2/1/edit/:id',
        name: 'InventoryCheckEdit',
        component: () => import('@/views/inventory/inventory-check-create.vue'),
        meta: {
          title: '编辑盘点单',
          activeMenu: '/inventory/2/1',
          breadcrumbs: ['库存管理', '盘点管理', '盘点单', '编辑盘点单'],
          openKeys: ['m4', 'm4-m2'],
        },
      },
      {
        path: 'inventory/5/1/create',
        name: 'InventoryTemplateCreate',
        component: () => import('@/views/inventory/inventory-template-create.vue'),
        meta: {
          title: '新增库存模板',
          activeMenu: '/inventory/5/1',
          breadcrumbs: ['库存管理', '库存规则', '库存模板', '新增库存模板'],
          openKeys: ['m4', 'm4-m5'],
        },
      },
      ...featureRoutes,
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

const flattenMenuPaths = (items: AppMenuItem[]): string[] => items.flatMap((item) => {
  const children = item.children?.length ? flattenMenuPaths(item.children) : [];
  return item.path ? [item.path, ...children] : children;
});

const canResolvePath = (path: string) => {
  const normalizedPath = String(path ?? '').trim().replace(/\/+$/, '');
  if (!normalizedPath || !normalizedPath.startsWith('/')) {
    return false;
  }
  return router.resolve(normalizedPath).matched.length > 0;
};

const resolveFirstAvailableMenuPath = (items: AppMenuItem[]) => flattenMenuPaths(items)
  .find((path) => canResolvePath(path));

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
    } catch {
      menuStore.clearMenus();
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

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && sessionStore.hasSelectedOrg) {
    if (!authStorage.getAccessToken()) {
      return PROFILE_HOME_PATH;
    }
    const targetOrgId = sessionStore.currentOrgId;
    if (menuStore.loadedOrgId !== targetOrgId || !menuStore.menuItems.length) {
      try {
        await menuStore.loadMenus(targetOrgId);
      } catch {
        menuStore.clearMenus();
        return PROFILE_HOME_PATH;
      }
    }
    const allowedPaths = new Set(flattenMenuPaths(menuStore.menuItems));
    const activeMenuPath = typeof to.meta.activeMenu === 'string' ? to.meta.activeMenu : '';
    const canAccessWorkflowConfig = to.path === '/group/workflow-config'
      && allowedPaths.has('/group/workflow-history');
    const canAccessProfile = to.path === '/profile';
    const canAccess = allowedPaths.has(to.path)
      || (activeMenuPath && allowedPaths.has(activeMenuPath))
      || canAccessWorkflowConfig
      || canAccessProfile;
    if (allowedPaths.size > 0 && !canAccess) {
      return resolveMenuHomePath(sessionStore, menuStore);
    }
  }

  if (sessionStore.isLoggedIn && to.path !== SELECT_ORG_PATH && to.path !== '/login' && to.path !== '/profile' && !canResolvePath(to.path)) {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  return true;
});

router.afterEach((to) => {
  document.title = `${to.meta.title ?? '管理后台'} | 进销存管`;
});

export default router;
