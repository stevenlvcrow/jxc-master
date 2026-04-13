import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { featureRoutes } from '@/config/menu';
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
    redirect: '/system/groups',
    children: [
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
          title: '用户角色管理',
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
      ...featureRoutes,
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

const SYSTEM_ADMIN_HOME = '/system/groups';

const flattenMenuPaths = (items: AppMenuItem[]): string[] => items.flatMap((item) => {
  const children = item.children?.length ? flattenMenuPaths(item.children) : [];
  return item.path ? [item.path, ...children] : children;
});

const resolveMenuHomePath = async (
  sessionStore: ReturnType<typeof useSessionStore>,
  menuStore: ReturnType<typeof useMenuStore>,
) => {
  if (!sessionStore.isLoggedIn) {
    return '/login';
  }
  if (!sessionStore.requiresOrgSelection) {
    return SYSTEM_ADMIN_HOME;
  }
  if (!sessionStore.hasSelectedOrg) {
    return '/select-org';
  }
  const targetOrgId = sessionStore.currentOrgId;
  if (!targetOrgId) {
    return '/select-org';
  }
  if (menuStore.loadedOrgId !== targetOrgId || !menuStore.menuItems.length) {
    await menuStore.loadMenus(targetOrgId);
  }
  const firstPath = flattenMenuPaths(menuStore.menuItems)[0];
  return firstPath ?? '/select-org';
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

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && !sessionStore.hasSelectedOrg && to.path !== '/select-org') {
    return '/select-org';
  }

  if (sessionStore.isLoggedIn && !sessionStore.requiresOrgSelection && to.path === '/select-org') {
    return SYSTEM_ADMIN_HOME;
  }

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && sessionStore.hasSelectedOrg && to.path === '/select-org') {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  if (to.path === '/dashboard') {
    return resolveMenuHomePath(sessionStore, menuStore);
  }

  if (sessionStore.isLoggedIn && sessionStore.requiresOrgSelection && sessionStore.hasSelectedOrg) {
    const targetOrgId = sessionStore.currentOrgId;
    if (menuStore.loadedOrgId !== targetOrgId || !menuStore.menuItems.length) {
      await menuStore.loadMenus(targetOrgId);
    }
    const allowedPaths = new Set(flattenMenuPaths(menuStore.menuItems));
    const activeMenuPath = typeof to.meta.activeMenu === 'string' ? to.meta.activeMenu : '';
    const canAccess = allowedPaths.has(to.path) || (activeMenuPath && allowedPaths.has(activeMenuPath));
    if (allowedPaths.size > 0 && !canAccess) {
      return resolveMenuHomePath(sessionStore, menuStore);
    }
  }

  return true;
});

router.afterEach((to) => {
  document.title = `${to.meta.title ?? '管理后台'} | ${import.meta.env.VITE_APP_TITLE}`;
});

export default router;
