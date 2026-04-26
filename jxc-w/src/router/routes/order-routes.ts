import type { RouteRecordRaw } from 'vue-router';

export const orderRoutes: RouteRecordRaw[] = [
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
        }
];
