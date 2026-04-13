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
import { fetchOrgTreeApi } from '@/api/modules/org';
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
const fallbackHomePath = computed(() => {
  const collectPaths = (items: typeof menuItems.value): string[] => items.flatMap((item) => {
    const childPaths = item.children?.length ? collectPaths(item.children) : [];
    return item.path ? [item.path, ...childPaths] : childPaths;
  });
  const firstPath = collectPaths(menuItems.value)[0];
  return firstPath ?? (sessionStore.requiresOrgSelection ? '/dashboard' : '/system/groups');
});

const activeMenu = computed(() => String(route.meta.activeMenu ?? route.path));
const openMenus = computed(() => (route.meta.openKeys as string[] | undefined) ?? []);
const activeTab = computed(() => route.path);
const orgDialogVisible = ref(false);
const orgKeyword = ref('');
const orgCity = ref('');
const activeGroupId = ref('');
const tabContextMenuVisible = ref(false);
const tabContextMenuPosition = ref({
  x: 0,
  y: 0,
});
const contextMenuTabPath = ref('');

const cityOptions = computed(() => Array.from(new Set(sessionStore.flatOrgs.map((item) => item.city))));
const currentOrg = computed(() => sessionStore.currentOrg);
const rootGroups = computed(() => sessionStore.rootGroups);

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
  appStore.addVisitedTab({
    path: route.path,
    title: String(route.meta.title ?? '未命名页面'),
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

const handleSelect = (path: string) => {
  if (!path.startsWith('/')) {
    return;
  }
  const resolved = router.resolve(path);
  if (!resolved.matched.length) {
    ElMessage.info('该菜单页面尚未配置路由');
    return;
  }
  router.push(path);
};

const handleTabClick = (path: string | number) => {
  router.push(String(path));
};

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

const handleTabContextMenu = (event: MouseEvent, path: string) => {
  contextMenuTabPath.value = path;
  tabContextMenuPosition.value = {
    x: event.clientX,
    y: event.clientY,
  };
  tabContextMenuVisible.value = true;
};

const handleTabContextAction = async (action: 'close-current' | 'close-others' | 'close-all') => {
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

  closeAllTabs();
};

const openOrgDialog = () => {
  if (!activeGroupId.value) {
    activeGroupId.value = rootGroups.value[0]?.id ?? '';
  }
  orgDialogVisible.value = true;
};

const selectOrg = (org: OrgNode) => {
  sessionStore.selectOrg(org.id);
  ElMessage.success(`已切换到 ${org.name}`);
  orgDialogVisible.value = false;
};

const handleUserCommand = (command: string | number | object) => {
  if (command === 'profile') {
    ElMessage.info('个人中心开发中');
    return;
  }

  if (command === 'logout') {
    sessionStore.logout();
    menuStore.clearMenus();
    appStore.resetVisitedTabs();
    router.replace('/login');
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
  () => [sessionStore.currentOrgId, sessionStore.platformAdminMode, sessionStore.isLoggedIn] as const,
  async ([orgId, isPlatformAdminMode, isLoggedIn]) => {
    if (!isLoggedIn) {
      return;
    }
    const targetOrgId = isPlatformAdminMode ? '' : orgId;
    if (!targetOrgId && sessionStore.requiresOrgSelection) {
      return;
    }
    if (menuStore.loadedOrgId === targetOrgId && menuStore.menuItems.length) {
      return;
    }
    try {
      await menuStore.loadMenus(targetOrgId || undefined);
    } catch {
      // Global error message handled in http interceptor.
    }
  },
  { immediate: true },
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
          <strong>JXC Admin</strong>
          <span>Vue 3 管理平台</span>
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
          <button v-if="sessionStore.requiresOrgSelection" class="org-switch-trigger" @click="openOrgDialog">
            <div class="org-switch-line">
              <div class="org-switch-sub">商户号: {{ currentOrg?.merchantNo ?? '-' }}</div>
              <div class="org-switch-title">
                <span>{{ currentOrg?.name ?? '请选择机构' }}</span>
                <el-icon>
                  <ArrowDown />
                </el-icon>
              </div>
            </div>
          </button>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="user-trigger">
              <span>{{ sessionStore.userName }}</span>
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
                <div class="workspace-tab-label" @contextmenu.prevent="handleTabContextMenu($event, tab.path)">
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
            <span>{{ row.type === 'group' ? '集团' : row.type === 'trial' ? '试店' : '门店' }}</span>
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
</template>
