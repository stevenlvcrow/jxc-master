import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { staticRoutes } from '@/router/static-routes';
import { authStorage } from '@/api/auth-storage';
import { pinia } from '@/stores';
import { useMenuStore } from '@/stores/menu';
import { useSessionStore } from '@/stores/session';
import type { AppMenuItem } from '@/config/menu';
import { resolveMenuView } from '@/router/menu-view-registry';

const routes: RouteRecordRaw[] = staticRoutes;

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

export const syncRuntimeMenuRoutes = (items: AppMenuItem[]) => {
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
  const normalizedToPath = normalizeMenuPath(to.path);
  const routeWasUnmatched = to.matched.length === 0;

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
        syncRuntimeMenuRoutes(menuStore.menuItems);
        if (routeWasUnmatched && canResolvePath(normalizedToPath || to.path)) {
          return to.fullPath;
        }
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
