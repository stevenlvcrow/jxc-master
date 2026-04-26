import type { RouteRecordRaw } from 'vue-router';

export const archiveRoutes: RouteRecordRaw[] = [
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
        }
];
