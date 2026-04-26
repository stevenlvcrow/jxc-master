import type { RouteRecordRaw } from 'vue-router';
import { archiveRoutes } from '@/router/routes/archive-routes';
import { inventoryRoutes } from '@/router/routes/inventory-routes';
import { orderRoutes } from '@/router/routes/order-routes';
import { profileRoutes } from '@/router/routes/profile-routes';
import { purchaseRoutes } from '@/router/routes/purchase-routes';
import { systemRoutes } from '@/router/routes/system-routes';

const adminChildren: RouteRecordRaw[] = [
  ...profileRoutes,
  ...systemRoutes,
  ...archiveRoutes,
  ...purchaseRoutes,
  ...inventoryRoutes,
  ...orderRoutes,
];

export const staticRoutes: RouteRecordRaw[] = [
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
    children: adminChildren,
  },
];
