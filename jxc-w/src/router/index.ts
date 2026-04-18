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
