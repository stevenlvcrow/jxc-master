import type { RouteRecordRaw } from 'vue-router';

export const systemRoutes: RouteRecordRaw[] = [
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
          path: 'group/workflow-history',
          name: 'GroupWorkflowHistory',
          component: () => import('@/views/system/workflow-publish-history-management.vue'),
          meta: {
            title: '流程发布管理',
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
          path: 'system/menus',
          name: 'SystemMenus',
          component: () => import('@/views/system/menu-maintenance.vue'),
          meta: {
            title: '菜单维护',
          },
        },
  {
          path: 'system/user-roles',
          name: 'SystemUserRoles',
          component: () => import('@/views/system/user-management.vue'),
          meta: {
            title: '用户管理',
          },
        }
];
