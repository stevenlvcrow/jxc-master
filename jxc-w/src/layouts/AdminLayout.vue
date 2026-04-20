<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ArrowDown,
  Bell,
  Checked,
  Coin,
  Collection,
  DataAnalysis,
  Document,
  Expand,
  Files,
  Fold,
  Goods,
  Odometer,
  Setting,
  Tickets,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import SidebarMenuItem from '@/components/SidebarMenuItem.vue';
import { fetchCurrentUserRolesApi, logoutApi, type CurrentUserRole } from '@/api/modules/auth';
import { fetchOrgTreeApi } from '@/api/modules/org';
import {
  fetchWorkflowApprovalNotificationsApi,
  fetchWorkflowPendingNotificationCountApi,
  type WorkflowApprovalNotificationItem,
} from '@/api/modules/workflow';
import { useAppStore } from '@/stores/app';
import { useMenuStore } from '@/stores/menu';
import { useSessionStore, type OrgNode } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const menuStore = useMenuStore();
const sessionStore = useSessionStore();
const useRealOrgApi = import.meta.env.VITE_USE_REAL_ORG_API === '1';

const iconMap = {
  Odometer,
  Tickets,
  Goods,
  Collection,
  Files,
  Coin,
  Checked,
  DataAnalysis,
  Document,
  Bell,
  Setting,
};

const menuItems = computed(() => [...menuStore.menuItems]);
const flattenMenuPaths = (items: typeof menuItems.value): string[] => items.flatMap((item) => {
  const childPaths = item.children?.length ? flattenMenuPaths(item.children) : [];
  return item.path ? [item.path, ...childPaths] : childPaths;
});
const canResolvePath = (path: string) => {
  const normalizedPath = String(path ?? '').trim().replace(/\/+$/, '');
  if (!normalizedPath || !normalizedPath.startsWith('/')) {
    return false;
  }
  return router.resolve(normalizedPath).matched.length > 0;
};
const resolveFirstAvailableMenuPath = (items: typeof menuItems.value) => flattenMenuPaths(items)
  .find((path) => canResolvePath(path));
const findMenuTitleByPath = (items: typeof menuItems.value, path: string): string | null => {
  for (const item of items) {
    if (item.path === path) {
      return item.title;
    }
    if (item.children?.length) {
      const childTitle = findMenuTitleByPath(item.children, path);
      if (childTitle) {
        return childTitle;
      }
    }
  }
  return null;
};
const fallbackHomePath = computed(() => {
  const firstPath = resolveFirstAvailableMenuPath(menuItems.value);
  return firstPath ?? '/profile';
});

const activeMenu = computed(() => String(route.meta.activeMenu ?? route.path));
const openMenus = computed(() => (route.meta.openKeys as string[] | undefined) ?? []);
const activeTab = computed(() => route.path);
const orgDialogVisible = ref(false);
const workflowNoticeDialogVisible = ref(false);
const orgKeyword = ref('');
const orgCity = ref('');
const activeGroupId = ref('');
const tabContextMenuVisible = ref(false);
const tabContextMenuPosition = ref({
  x: 0,
  y: 0,
});
const contextMenuTabPath = ref('');
const draggingTabPath = ref('');
const dragOverTabPath = ref('');
const dragDropPosition = ref<'before' | 'after' | ''>('');
const workflowNoticeLoading = ref(false);
const workflowNoticePageNum = ref(1);
const workflowNoticePageSize = ref(8);
const workflowNoticeTotal = ref(0);
const workflowNoticeBadgeCount = ref(0);
const workflowNoticeRows = ref<WorkflowApprovalNotificationItem[]>([]);
const currentUserRoles = ref<CurrentUserRole[]>([]);

const cityOptions = computed(() => Array.from(new Set(sessionStore.flatOrgs.map((item) => item.city))));
const currentOrg = computed(() => sessionStore.currentOrg);
const currentOrgLabel = computed(() => {
  const org = currentOrg.value;
  if (!org) {
    return sessionStore.requiresOrgSelection ? '请选择机构' : '平台模式';
  }
  const orgLabel = org.type === 'group' ? '集团名' : '门店名';
  const merchantLabel = org.type === 'group' ? '集团号' : '商户号';
  return `${orgLabel}：${org.name}（${merchantLabel}：${org.merchantNo || '-'}）`;
});
const currentRoleText = computed(() => {
  if (sessionStore.platformAdminMode) {
    return '平台管理员';
  }
  if (!currentUserRoles.value.length) {
    return '暂无角色';
  }
  return currentUserRoles.value.map((item) => item.roleName).join(' / ');
});
const rootGroups = computed(() => sessionStore.rootGroups);
const workflowNoticeOrgId = computed(() => {
  if (!sessionStore.isLoggedIn) {
    return undefined;
  }
  if (sessionStore.platformAdminMode) {
    return 'platform';
  }
  return sessionStore.currentOrgId || undefined;
});

const activeGroup = computed(() => {
  const list = rootGroups.value;
  return list.find((item) => item.id === activeGroupId.value) ?? list[0] ?? null;
});

const activeGroupRows = computed(() => {
  const group = activeGroup.value;
  if (!group) {
    return [];
  }

  const rows = [group, ...(group.children ?? [])];
  return rows.filter((row) => {
    const keywordMatched = orgKeyword.value
      ? `${row.name}${row.code}${row.merchantNo}`.toLowerCase().includes(orgKeyword.value.trim().toLowerCase())
      : true;
    const cityMatched = orgCity.value ? row.city === orgCity.value : true;
    return keywordMatched && cityMatched;
  });
});

const syncVisitedTab = () => {
  const menuTitle = findMenuTitleByPath(menuItems.value, route.path);
  appStore.addVisitedTab({
    path: route.path,
    title: String(menuTitle ?? route.meta.title ?? '未命名页面'),
    closable: true,
  });
};

const contextMenuTab = computed(() => appStore.visitedTabs.find((item) => item.path === contextMenuTabPath.value) ?? null);
const hasClosableTabs = computed(() => appStore.visitedTabs.some((item) => item.closable));
const canCloseContextTab = computed(() => Boolean(contextMenuTab.value?.closable));
const canCloseOthers = computed(() => {
  const targetPath = contextMenuTabPath.value;
  return appStore.visitedTabs.some((item) => item.closable && item.path !== targetPath);
});
const canCloseRightTabs = computed(() => {
  const targetPath = contextMenuTabPath.value;
  if (!targetPath) {
    return false;
  }
  const targetIndex = appStore.visitedTabs.findIndex((item) => item.path === targetPath);
  if (targetIndex < 0) {
    return false;
  }
  return appStore.visitedTabs.some((item, index) => index > targetIndex && item.closable);
});

const handleSelect = (path: string) => {
  const normalizedPath = String(path ?? '').trim().replace(/\/+$/, '');
  if (!normalizedPath.startsWith('/')) {
    return;
  }
  const resolved = router.resolve(normalizedPath);
  if (!resolved.matched.length) {
    ElMessage.info('该菜单页面尚未配置路由');
    return;
  }
  router.push(normalizedPath);
};

const handleTabClick = (path: string | number) => {
  router.push(String(path));
};

const clearTabDragState = () => {
  draggingTabPath.value = '';
  dragOverTabPath.value = '';
  dragDropPosition.value = '';
};

const resolveDropPosition = (event: DragEvent) => {
  const currentTarget = event.currentTarget;
  if (!(currentTarget instanceof HTMLElement)) {
    return 'after' as const;
  }
  const rect = currentTarget.getBoundingClientRect();
  return event.clientX < rect.left + rect.width / 2 ? 'before' as const : 'after' as const;
};

const handleTabDragStart = (event: DragEvent, path: string) => {
  draggingTabPath.value = path;
  dragOverTabPath.value = '';
  dragDropPosition.value = '';
  hideTabContextMenu();
  event.dataTransfer?.setData('text/plain', path);
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
  }
};

const handleTabDragOver = (event: DragEvent, path: string) => {
  if (!draggingTabPath.value || draggingTabPath.value === path) {
    return;
  }
  event.preventDefault();
  dragOverTabPath.value = path;
  dragDropPosition.value = resolveDropPosition(event);
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
};

const handleTabDrop = (event: DragEvent, path: string) => {
  event.preventDefault();
  const sourcePath = draggingTabPath.value || event.dataTransfer?.getData('text/plain') || '';
  if (!sourcePath || sourcePath === path) {
    clearTabDragState();
    return;
  }
  const position = dragOverTabPath.value === path && dragDropPosition.value
    ? dragDropPosition.value
    : resolveDropPosition(event);
  appStore.moveVisitedTab(sourcePath, path, position);
  clearTabDragState();
};

const handleTabDragEnd = () => {
  clearTabDragState();
};

const getTabDragClass = (path: string) => ({
  'is-dragging': draggingTabPath.value === path,
  'is-drag-over-before': dragOverTabPath.value === path && dragDropPosition.value === 'before',
  'is-drag-over-after': dragOverTabPath.value === path && dragDropPosition.value === 'after',
});

const hideTabContextMenu = () => {
  tabContextMenuVisible.value = false;
};

const resolveFallbackPathAfterClosing = (closedPath: string) => {
  const currentTabs = appStore.visitedTabs;
  const currentIndex = currentTabs.findIndex((item) => item.path === closedPath);
  if (currentIndex === -1) {
    return fallbackHomePath.value;
  }

  const nextTab = currentTabs[currentIndex + 1] ?? currentTabs[currentIndex - 1] ?? currentTabs[0];
  return nextTab?.path ?? fallbackHomePath.value;
};

const closeTab = (path: string) => {
  const targetTab = appStore.visitedTabs.find((item) => item.path === path);
  if (!targetTab?.closable) {
    return;
  }

  const nextPath = resolveFallbackPathAfterClosing(path);
  appStore.removeVisitedTab(path);

  if (route.path !== path) {
    return;
  }

  router.push(nextPath);
};

const handleTabRemove = (path: string | number) => {
  closeTab(String(path));
};

const closeOtherTabs = (path: string) => {
  const targetTab = appStore.visitedTabs.find((item) => item.path === path);
  if (!targetTab) {
    return;
  }

  appStore.removeOtherVisitedTabs(path);

  if (route.path !== path) {
    router.push(path);
  }
};

const closeAllTabs = () => {
  appStore.removeAllClosableTabs();
  const remainingActiveTab = appStore.visitedTabs.find((item) => item.path === route.path);
  if (remainingActiveTab) {
    return;
  }
  router.push(appStore.visitedTabs[0]?.path ?? fallbackHomePath.value);
};

const closeRightTabs = (path: string) => {
  const tabs = appStore.visitedTabs;
  const targetIndex = tabs.findIndex((item) => item.path === path);
  if (targetIndex < 0) {
    return;
  }
  const removedPaths = tabs
    .filter((item, index) => index > targetIndex && item.closable)
    .map((item) => item.path);
  if (!removedPaths.length) {
    return;
  }
  appStore.removeRightVisitedTabs(path);
  if (removedPaths.includes(route.path)) {
    router.push(path);
  }
};

const handleTabContextMenu = (event: MouseEvent, path: string) => {
  contextMenuTabPath.value = path;
  tabContextMenuPosition.value = {
    x: event.clientX,
    y: event.clientY,
  };
  tabContextMenuVisible.value = true;
};

const handleTabContextAction = async (action: 'close-current' | 'close-others' | 'close-right' | 'close-all') => {
  const targetPath = contextMenuTabPath.value;
  hideTabContextMenu();
  if (!targetPath) {
    return;
  }

  if (action === 'close-current') {
    closeTab(targetPath);
    return;
  }

  if (action === 'close-others') {
    closeOtherTabs(targetPath);
    return;
  }

  if (action === 'close-right') {
    closeRightTabs(targetPath);
    return;
  }

  closeAllTabs();
};

const openOrgDialog = () => {
  if (!activeGroupId.value) {
    activeGroupId.value = rootGroups.value[0]?.id ?? '';
  }
  orgDialogVisible.value = true;
};

const openWorkflowNoticeDialog = async () => {
  workflowNoticeDialogVisible.value = true;
  workflowNoticePageNum.value = 1;
};

const loadWorkflowNotifications = async (pageNum = workflowNoticePageNum.value) => {
  const orgId = workflowNoticeOrgId.value;
  if (!orgId) {
    workflowNoticeRows.value = [];
    workflowNoticeTotal.value = 0;
    return;
  }
  workflowNoticeLoading.value = true;
  try {
    const result = await fetchWorkflowApprovalNotificationsApi({
      orgId,
      pageNum,
      pageSize: workflowNoticePageSize.value,
    });
    workflowNoticeRows.value = result.list ?? [];
    workflowNoticeTotal.value = Number(result.total ?? 0);
    workflowNoticePageNum.value = result.pageNum ?? pageNum;
    workflowNoticePageSize.value = result.pageSize ?? workflowNoticePageSize.value;
  } finally {
    workflowNoticeLoading.value = false;
  }
};

const loadWorkflowNoticeBadgeCount = async () => {
  const orgId = workflowNoticeOrgId.value;
  if (!orgId) {
    workflowNoticeBadgeCount.value = 0;
    return;
  }
  try {
    workflowNoticeBadgeCount.value = Number(
      await fetchWorkflowPendingNotificationCountApi({ orgId }),
    );
  } catch {
    workflowNoticeBadgeCount.value = 0;
  }
};

const resolveWorkflowNoticeRoute = (row: WorkflowApprovalNotificationItem) => {
  if (row.routePath) {
    return row.routePath;
  }
  if (row.businessCode === 'PURCHASE_INBOUND') {
    return `/inventory/1/2/view/${row.businessId}`;
  }
  return '';
};

const handleWorkflowNoticeRowClick = (row: WorkflowApprovalNotificationItem) => {
  const targetPath = resolveWorkflowNoticeRoute(row);
  if (!targetPath) {
    ElMessage.info('当前消息未配置跳转页面');
    return;
  }
  workflowNoticeDialogVisible.value = false;
  router.push(targetPath);
};

const handleWorkflowNoticePageChange = async (page: number) => {
  await loadWorkflowNotifications(page);
};

const handleWorkflowNoticePageSizeChange = async (size: number) => {
  workflowNoticePageSize.value = size;
  workflowNoticePageNum.value = 1;
  await loadWorkflowNotifications(1);
};

const loadCurrentUserRoles = async () => {
  if (!sessionStore.isLoggedIn || sessionStore.platformAdminMode) {
    currentUserRoles.value = [];
    return;
  }
  try {
    currentUserRoles.value = await fetchCurrentUserRolesApi(sessionStore.currentOrgId || undefined);
  } catch {
    currentUserRoles.value = [];
  }
};

const switchOrg = async (orgId: string) => {
  sessionStore.selectOrg(orgId);
  appStore.resetVisitedTabs();
  menuStore.clearMenus();

  await router.replace('/profile');
};

const selectOrg = async (org: OrgNode) => {
  try {
    await switchOrg(org.id);
    ElMessage.success(`已切换到 ${org.name}`);
    orgDialogVisible.value = false;
  } catch {
    // Global error message handled in http interceptor.
  }
};

const handleUserCommand = (command: string | number | object) => {
  if (command === 'profile') {
    router.push('/profile');
    return;
  }

  if (command === 'logout') {
    void (async () => {
      try {
        await logoutApi();
      } catch {
        // Ignore logout API failure and continue local cleanup.
      } finally {
        sessionStore.logout();
        menuStore.clearMenus();
        appStore.resetVisitedTabs();
        router.replace('/login');
      }
    })();
  }
};

const syncOrgTree = async () => {
  if (!useRealOrgApi || !sessionStore.isLoggedIn) {
    return;
  }
  try {
    const tree = await fetchOrgTreeApi();
    sessionStore.setOrgTree(tree);
  } catch {
    // Global error message handled in http interceptor.
  }
};

watch(
  () => route.path,
  () => {
    syncVisitedTab();
    hideTabContextMenu();
  },
  { immediate: true },
);

watch(
  () => menuItems.value,
  () => {
    syncVisitedTab();
  },
  { deep: true },
);

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode, sessionStore.isLoggedIn] as const,
  async ([orgId, isPlatformAdminMode, isLoggedIn]) => {
    if (!isLoggedIn) {
      return;
    }
    const targetOrgId = isPlatformAdminMode ? 'platform' : orgId;
    if (!targetOrgId && sessionStore.requiresOrgSelection) {
      return;
    }
    if (menuStore.loadedOrgId === targetOrgId && menuStore.menuItems.length) {
      return;
    }
    try {
      await menuStore.loadMenus(targetOrgId || undefined);
      const allowedPaths = new Set(flattenMenuPaths(menuStore.menuItems));
      if (route.path !== '/select-org' && route.path !== '/login' && route.path !== '/profile' && !allowedPaths.has(route.path)) {
        if (sessionStore.requiresOrgSelection && !sessionStore.hasSelectedOrg) {
          await router.replace('/select-org');
        } else {
          await router.replace('/profile');
        }
      }
    } catch {
      // Global error message handled in http interceptor.
    }
  },
  { immediate: true },
);

watch(
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode, sessionStore.isLoggedIn] as const,
  async ([orgId, isPlatformAdminMode, isLoggedIn]) => {
    if (!isLoggedIn) {
      workflowNoticeBadgeCount.value = 0;
      return;
    }
    const targetOrgId = isPlatformAdminMode ? 'platform' : orgId;
    if (!targetOrgId && sessionStore.requiresOrgSelection) {
      workflowNoticeBadgeCount.value = 0;
      return;
    }
    await loadWorkflowNoticeBadgeCount();
  },
  { immediate: true },
);

watch(
  () => workflowNoticeDialogVisible.value,
  (visible) => {
    if (visible) {
      void loadWorkflowNoticeBadgeCount();
      void loadWorkflowNotifications(1);
    }
  },
);

watch(
  () => sessionStore.isLoggedIn,
  (isLoggedIn) => {
    if (!isLoggedIn) {
      return;
    }
    syncOrgTree();
  },
  { immediate: true },
);

watch(
  rootGroups,
  (groups) => {
    if (!groups.length) {
      activeGroupId.value = '';
      return;
    }
    if (!activeGroupId.value || !groups.some((item) => item.id === activeGroupId.value)) {
      activeGroupId.value = groups[0].id;
    }
  },
  { immediate: true },
);

watch(
  () => [sessionStore.isLoggedIn, sessionStore.currentOrgId, sessionStore.platformAdminMode] as const,
  () => {
    void loadCurrentUserRoles();
  },
  { immediate: true },
);

onMounted(() => {
  window.addEventListener('click', hideTabContextMenu);
  window.addEventListener('resize', hideTabContextMenu);
  window.addEventListener('blur', hideTabContextMenu);
});

onBeforeUnmount(() => {
  window.removeEventListener('click', hideTabContextMenu);
  window.removeEventListener('resize', hideTabContextMenu);
  window.removeEventListener('blur', hideTabContextMenu);
});
</script>

<template>
  <el-container class="admin-layout" :style="{ '--sidebar-width': appStore.sidebarCollapsed ? '48px' : '160px' }">
    <el-aside :width="appStore.sidebarCollapsed ? '48px' : '160px'" class="sidebar">
      <div class="brand">
        <div class="brand-mark">J</div>
        <div v-show="!appStore.sidebarCollapsed" class="brand-copy">
          <strong>BoBo</strong>
          <span>进销存管理平台</span>
        </div>
      </div>

      <el-menu
        :collapse="appStore.sidebarCollapsed"
        :default-active="activeMenu"
        :default-openeds="openMenus"
        class="menu"
        @select="handleSelect"
      >
        <SidebarMenuItem
          v-for="item in menuItems"
          :key="item.key"
          :item="item"
          :icon-map="iconMap"
          :level="1"
        />
      </el-menu>
    </el-aside>

    <el-container class="main-shell">
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button text circle @click="appStore.toggleSidebar()">
            <el-icon size="18">
              <component :is="appStore.sidebarCollapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <div class="page-title">
            {{ route.meta.title }}
          </div>
        </div>

        <div class="topbar-right">
          <button v-if="sessionStore.requiresOrgSelection" class="topbar-item org-switch-trigger" @click="openOrgDialog">
            <span class="org-switch-text">{{ currentOrgLabel }}</span>
            <el-icon class="org-switch-arrow">
              <ArrowDown />
            </el-icon>
          </button>

          <div class="topbar-item user-role-text" :title="currentRoleText">
            {{ currentRoleText }}
          </div>

          <el-dropdown trigger="click" class="topbar-item user-dropdown" @command="handleUserCommand">
            <button class="user-trigger">
              <span class="user-trigger__name">{{ sessionStore.userName }}</span>
              <el-icon>
                <ArrowDown />
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-badge :value="workflowNoticeBadgeCount" :hidden="workflowNoticeBadgeCount === 0" :max="99" class="workflow-notice-badge topbar-item">
            <button class="workflow-notice-trigger" @click="openWorkflowNoticeDialog" aria-label="审批提醒">
              <el-icon>
                <Bell />
              </el-icon>
            </button>
          </el-badge>
        </div>
      </el-header>

      <el-main class="content">
        <div class="tabbar">
          <el-tabs
            :model-value="activeTab"
            type="card"
            class="workspace-tabs"
            @tab-change="handleTabClick"
            @tab-remove="handleTabRemove"
          >
            <el-tab-pane
              v-for="tab in appStore.visitedTabs"
              :key="tab.path"
              :name="tab.path"
              :closable="tab.closable"
            >
              <template #label>
                <div
                  class="workspace-tab-label"
                  :class="getTabDragClass(tab.path)"
                  draggable="true"
                  @contextmenu.prevent="handleTabContextMenu($event, tab.path)"
                  @dragstart="handleTabDragStart($event, tab.path)"
                  @dragover="handleTabDragOver($event, tab.path)"
                  @drop="handleTabDrop($event, tab.path)"
                  @dragend="handleTabDragEnd"
                >
                  {{ tab.title }}
                </div>
              </template>
            </el-tab-pane>
          </el-tabs>
          <div
            v-if="tabContextMenuVisible"
            class="tab-context-menu"
            :style="{
              left: `${tabContextMenuPosition.x}px`,
              top: `${tabContextMenuPosition.y}px`,
            }"
          >
            <button
              class="tab-context-menu__item"
              :disabled="!canCloseContextTab"
              @click.stop="handleTabContextAction('close-current')"
            >
              关闭当前
            </button>
            <button
              class="tab-context-menu__item"
              :disabled="!canCloseOthers"
              @click.stop="handleTabContextAction('close-others')"
            >
              关闭其他
            </button>
            <button
              class="tab-context-menu__item"
              :disabled="!canCloseRightTabs"
              @click.stop="handleTabContextAction('close-right')"
            >
              关闭右侧
            </button>
            <button
              class="tab-context-menu__item"
              :disabled="!hasClosableTabs"
              @click.stop="handleTabContextAction('close-all')"
            >
              关闭全部
            </button>
          </div>
        </div>
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog
    v-if="sessionStore.requiresOrgSelection"
    v-model="orgDialogVisible"
    title="选择机构"
    width="960px"
    class="org-dialog"
  >
    <div class="org-dialog-tools">
      <el-input
        v-model="orgKeyword"
        placeholder="请输入机构名称/编码/商户号"
        clearable
      />
      <el-select
        v-model="orgCity"
        placeholder="请选择城市"
        clearable
        style="width: 200px"
      >
        <el-option
          v-for="city in cityOptions"
          :key="city"
          :label="city"
          :value="city"
        />
      </el-select>
    </div>

    <div class="org-dialog-content">
      <aside class="org-dialog-left">
        <button
          v-for="group in rootGroups"
          :key="group.id"
          class="org-group-btn"
          :class="{ active: group.id === activeGroupId }"
          @click="activeGroupId = group.id"
        >
          {{ group.name }}
        </button>
      </aside>

      <el-table :data="activeGroupRows" border height="400">
        <el-table-column prop="name" label="机构名称" min-width="220" />
        <el-table-column label="机构类型" width="120">
          <template #default="{ row }">
            <span>{{ row.type === 'group' ? '集团' : '门店' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="机构编码" width="140" />
        <el-table-column prop="merchantNo" label="商户号" width="140" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.id === sessionStore.currentOrgId" type="warning" size="small">当前</el-tag>
            <el-button v-else link type="primary" @click="selectOrg(row)">
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>

  <el-dialog
    v-model="workflowNoticeDialogVisible"
    title="审批提醒"
    width="1120px"
    class="workflow-notice-dialog"
    append-to-body
  >
    <div class="workflow-notice-content">
      <el-table
        :data="workflowNoticeRows"
        border
        stripe
        height="420"
        v-loading="workflowNoticeLoading"
        empty-text="暂无审批消息"
        class="workflow-notice-table"
        :row-class-name="() => 'workflow-notice-row'"
        @row-click="handleWorkflowNoticeRowClick"
      >
        <el-table-column prop="approvalNo" label="审批编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="workflowName" label="流程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="approverName" label="审批人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="approverRole" label="审批角色" min-width="120" show-overflow-tooltip />
        <el-table-column prop="auditedAt" label="审核时间" min-width="170" show-overflow-tooltip />
        <el-table-column prop="result" label="结果（通过/拒绝）" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag
            :type="row.result === '拒绝' ? 'danger' : row.result === '待审核' ? 'warning' : 'success'"
            effect="light"
          >
            {{ row.result }}
          </el-tag>
        </template>
      </el-table-column>
        <el-table-column prop="remark" label="备注（拒绝原因）" min-width="220" show-overflow-tooltip />
      </el-table>
      <div class="workflow-notice-footer">
        <el-pagination
          layout="total, prev, pager, next, sizes"
          :total="workflowNoticeTotal"
          :current-page="workflowNoticePageNum"
          :page-size="workflowNoticePageSize"
          :page-sizes="[8, 10, 20]"
          @current-change="handleWorkflowNoticePageChange"
          @size-change="handleWorkflowNoticePageSizeChange"
        />
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
  justify-content: flex-end;
  min-width: 0;
  font-size: 14px;
  font-weight: 400;
}

.topbar-item {
  flex: 0 0 auto;
  height: 24px;
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

.user-dropdown {
  line-height: 1;
  font-size: inherit;
  font-weight: inherit;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1 1 auto;
}

.page-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 400;
}

.org-switch-trigger,
.workflow-notice-trigger,
.user-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  background: transparent;
  color: var(--el-text-color-primary);
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 0;
  font-size: inherit;
  font-weight: inherit;
}

.org-switch-trigger:hover,
.workflow-notice-trigger:hover,
.user-trigger:hover {
  color: var(--el-color-primary);
}

.org-switch-trigger {
  padding: 0;
  min-width: 0;
  max-width: 340px;
  height: 24px;
  text-align: left;
  background: transparent;
  line-height: 1;
  gap: 4px;
}

.org-switch-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: inherit;
  font-weight: inherit;
}

.org-switch-arrow {
  flex: 0 0 auto;
  margin-left: -2px;
}

.workflow-notice-badge {
  display: inline-flex;
}

.workflow-notice-trigger {
  width: 24px;
  padding: 0;
  border-radius: 50%;
  border: none;
  background: transparent;
  box-shadow: none;
}

.user-trigger {
  height: 24px;
  padding: 0 4px;
  max-width: 160px;
  font-size: inherit;
  font-weight: inherit;
  line-height: 1;
}

.user-trigger__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: inherit;
  font-weight: inherit;
}

.user-role-text {
  min-width: 0;
  white-space: nowrap;
  color: var(--el-text-color-primary);
  font-size: inherit;
  font-weight: inherit;
  flex: 0 0 auto;
  line-height: 1;
  padding: 0;
}

.workflow-notice-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workflow-notice-table :deep(.workflow-notice-row) {
  cursor: pointer;
}

.workflow-notice-footer {
  display: flex;
  justify-content: flex-end;
}

.org-dialog :deep(.el-dialog__body),
.workflow-notice-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}

.workflow-notice-dialog :deep(.el-tag) {
  min-width: 68px;
  justify-content: center;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: nowrap;
  min-height: 48px;
  height: auto;
  padding: 6px 16px;
}

@media (max-width: 1200px) {
  .topbar-left {
    flex-basis: 100%;
  }

  .topbar-right {
    flex-basis: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
