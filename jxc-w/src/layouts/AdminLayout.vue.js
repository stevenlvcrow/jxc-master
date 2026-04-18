/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown, Bell, Checked, Coin, Collection, DataAnalysis, Document, Expand, Files, Fold, Goods, Odometer, Setting, Tickets, } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import SidebarMenuItem from '@/components/SidebarMenuItem.vue';
import { logoutApi } from '@/api/modules/auth';
import { fetchOrgTreeApi } from '@/api/modules/org';
import { useAppStore } from '@/stores/app';
import { useMenuStore } from '@/stores/menu';
import { useSessionStore } from '@/stores/session';
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
const flattenMenuPaths = (items) => items.flatMap((item) => {
    const childPaths = item.children?.length ? flattenMenuPaths(item.children) : [];
    return item.path ? [item.path, ...childPaths] : childPaths;
});
const canResolvePath = (path) => {
    const normalizedPath = String(path ?? '').trim().replace(/\/+$/, '');
    if (!normalizedPath || !normalizedPath.startsWith('/')) {
        return false;
    }
    return router.resolve(normalizedPath).matched.length > 0;
};
const resolveFirstAvailableMenuPath = (items) => flattenMenuPaths(items)
    .find((path) => canResolvePath(path));
const findMenuTitleByPath = (items, path) => {
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
const openMenus = computed(() => route.meta.openKeys ?? []);
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
const draggingTabPath = ref('');
const dragOverTabPath = ref('');
const dragDropPosition = ref('');
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
const handleSelect = (path) => {
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
const handleTabClick = (path) => {
    router.push(String(path));
};
const clearTabDragState = () => {
    draggingTabPath.value = '';
    dragOverTabPath.value = '';
    dragDropPosition.value = '';
};
const resolveDropPosition = (event) => {
    const currentTarget = event.currentTarget;
    if (!(currentTarget instanceof HTMLElement)) {
        return 'after';
    }
    const rect = currentTarget.getBoundingClientRect();
    return event.clientX < rect.left + rect.width / 2 ? 'before' : 'after';
};
const handleTabDragStart = (event, path) => {
    draggingTabPath.value = path;
    dragOverTabPath.value = '';
    dragDropPosition.value = '';
    hideTabContextMenu();
    event.dataTransfer?.setData('text/plain', path);
    if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move';
    }
};
const handleTabDragOver = (event, path) => {
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
const handleTabDrop = (event, path) => {
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
const getTabDragClass = (path) => ({
    'is-dragging': draggingTabPath.value === path,
    'is-drag-over-before': dragOverTabPath.value === path && dragDropPosition.value === 'before',
    'is-drag-over-after': dragOverTabPath.value === path && dragDropPosition.value === 'after',
});
const hideTabContextMenu = () => {
    tabContextMenuVisible.value = false;
};
const resolveFallbackPathAfterClosing = (closedPath) => {
    const currentTabs = appStore.visitedTabs;
    const currentIndex = currentTabs.findIndex((item) => item.path === closedPath);
    if (currentIndex === -1) {
        return fallbackHomePath.value;
    }
    const nextTab = currentTabs[currentIndex + 1] ?? currentTabs[currentIndex - 1] ?? currentTabs[0];
    return nextTab?.path ?? fallbackHomePath.value;
};
const closeTab = (path) => {
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
const handleTabRemove = (path) => {
    closeTab(String(path));
};
const closeOtherTabs = (path) => {
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
const closeRightTabs = (path) => {
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
const handleTabContextMenu = (event, path) => {
    contextMenuTabPath.value = path;
    tabContextMenuPosition.value = {
        x: event.clientX,
        y: event.clientY,
    };
    tabContextMenuVisible.value = true;
};
const handleTabContextAction = async (action) => {
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
const switchOrg = async (orgId) => {
    sessionStore.selectOrg(orgId);
    appStore.resetVisitedTabs();
    menuStore.clearMenus();
    await router.replace('/profile');
};
const selectOrg = async (org) => {
    try {
        await switchOrg(org.id);
        ElMessage.success(`已切换到 ${org.name}`);
        orgDialogVisible.value = false;
    }
    catch {
        // Global error message handled in http interceptor.
    }
};
const handleUserCommand = (command) => {
    if (command === 'profile') {
        router.push('/profile');
        return;
    }
    if (command === 'logout') {
        void (async () => {
            try {
                await logoutApi();
            }
            catch {
                // Ignore logout API failure and continue local cleanup.
            }
            finally {
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
    }
    catch {
        // Global error message handled in http interceptor.
    }
};
watch(() => route.path, () => {
    syncVisitedTab();
    hideTabContextMenu();
}, { immediate: true });
watch(() => menuItems.value, () => {
    syncVisitedTab();
}, { deep: true });
watch(() => [sessionStore.currentOrgId, sessionStore.platformAdminMode, sessionStore.isLoggedIn], async ([orgId, isPlatformAdminMode, isLoggedIn]) => {
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
        const allowedPaths = new Set(flattenMenuPaths(menuStore.menuItems));
        if (route.path !== '/select-org' && route.path !== '/login' && route.path !== '/profile' && !allowedPaths.has(route.path)) {
            if (sessionStore.requiresOrgSelection && !sessionStore.hasSelectedOrg) {
                await router.replace('/select-org');
            }
            else {
                await router.replace('/profile');
            }
        }
    }
    catch {
        // Global error message handled in http interceptor.
    }
}, { immediate: true });
watch(() => sessionStore.isLoggedIn, (isLoggedIn) => {
    if (!isLoggedIn) {
        return;
    }
    syncOrgTree();
}, { immediate: true });
watch(rootGroups, (groups) => {
    if (!groups.length) {
        activeGroupId.value = '';
        return;
    }
    if (!activeGroupId.value || !groups.some((item) => item.id === activeGroupId.value)) {
        activeGroupId.value = groups[0].id;
    }
}, { immediate: true });
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
const __VLS_0 = {}.ElContainer;
/** @type {[typeof __VLS_components.ElContainer, typeof __VLS_components.elContainer, typeof __VLS_components.ElContainer, typeof __VLS_components.elContainer, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ class: "admin-layout" },
    ...{ style: ({ '--sidebar-width': __VLS_ctx.appStore.sidebarCollapsed ? '48px' : '160px' }) },
}));
const __VLS_2 = __VLS_1({
    ...{ class: "admin-layout" },
    ...{ style: ({ '--sidebar-width': __VLS_ctx.appStore.sidebarCollapsed ? '48px' : '160px' }) },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElAside;
/** @type {[typeof __VLS_components.ElAside, typeof __VLS_components.elAside, typeof __VLS_components.ElAside, typeof __VLS_components.elAside, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    width: (__VLS_ctx.appStore.sidebarCollapsed ? '48px' : '160px'),
    ...{ class: "sidebar" },
}));
const __VLS_6 = __VLS_5({
    width: (__VLS_ctx.appStore.sidebarCollapsed ? '48px' : '160px'),
    ...{ class: "sidebar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand-mark" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand-copy" },
});
__VLS_asFunctionalDirective(__VLS_directives.vShow)(null, { ...__VLS_directiveBindingRestFields, value: (!__VLS_ctx.appStore.sidebarCollapsed) }, null, null);
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
const __VLS_8 = {}.ElMenu;
/** @type {[typeof __VLS_components.ElMenu, typeof __VLS_components.elMenu, typeof __VLS_components.ElMenu, typeof __VLS_components.elMenu, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    ...{ 'onSelect': {} },
    collapse: (__VLS_ctx.appStore.sidebarCollapsed),
    defaultActive: (__VLS_ctx.activeMenu),
    defaultOpeneds: (__VLS_ctx.openMenus),
    ...{ class: "menu" },
}));
const __VLS_10 = __VLS_9({
    ...{ 'onSelect': {} },
    collapse: (__VLS_ctx.appStore.sidebarCollapsed),
    defaultActive: (__VLS_ctx.activeMenu),
    defaultOpeneds: (__VLS_ctx.openMenus),
    ...{ class: "menu" },
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
let __VLS_12;
let __VLS_13;
let __VLS_14;
const __VLS_15 = {
    onSelect: (__VLS_ctx.handleSelect)
};
__VLS_11.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.menuItems))) {
    /** @type {[typeof SidebarMenuItem, ]} */ ;
    // @ts-ignore
    const __VLS_16 = __VLS_asFunctionalComponent(SidebarMenuItem, new SidebarMenuItem({
        key: (item.key),
        item: (item),
        iconMap: (__VLS_ctx.iconMap),
        level: (1),
    }));
    const __VLS_17 = __VLS_16({
        key: (item.key),
        item: (item),
        iconMap: (__VLS_ctx.iconMap),
        level: (1),
    }, ...__VLS_functionalComponentArgsRest(__VLS_16));
}
var __VLS_11;
var __VLS_7;
const __VLS_19 = {}.ElContainer;
/** @type {[typeof __VLS_components.ElContainer, typeof __VLS_components.elContainer, typeof __VLS_components.ElContainer, typeof __VLS_components.elContainer, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    ...{ class: "main-shell" },
}));
const __VLS_21 = __VLS_20({
    ...{ class: "main-shell" },
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElHeader;
/** @type {[typeof __VLS_components.ElHeader, typeof __VLS_components.elHeader, typeof __VLS_components.ElHeader, typeof __VLS_components.elHeader, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    ...{ class: "topbar" },
}));
const __VLS_25 = __VLS_24({
    ...{ class: "topbar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "topbar-left" },
});
const __VLS_27 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    ...{ 'onClick': {} },
    text: true,
    circle: true,
}));
const __VLS_29 = __VLS_28({
    ...{ 'onClick': {} },
    text: true,
    circle: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
let __VLS_31;
let __VLS_32;
let __VLS_33;
const __VLS_34 = {
    onClick: (...[$event]) => {
        __VLS_ctx.appStore.toggleSidebar();
    }
};
__VLS_30.slots.default;
const __VLS_35 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    size: "18",
}));
const __VLS_37 = __VLS_36({
    size: "18",
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
__VLS_38.slots.default;
const __VLS_39 = ((__VLS_ctx.appStore.sidebarCollapsed ? __VLS_ctx.Expand : __VLS_ctx.Fold));
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(__VLS_39, new __VLS_39({}));
const __VLS_41 = __VLS_40({}, ...__VLS_functionalComponentArgsRest(__VLS_40));
var __VLS_38;
var __VLS_30;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-title" },
});
(__VLS_ctx.route.meta.title);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "topbar-right" },
});
if (__VLS_ctx.sessionStore.requiresOrgSelection) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.openOrgDialog) },
        ...{ class: "org-switch-trigger" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-switch-line" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-switch-sub" },
    });
    (__VLS_ctx.currentOrg?.merchantNo ?? '-');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-switch-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.currentOrg?.name ?? '请选择机构');
    const __VLS_43 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({}));
    const __VLS_45 = __VLS_44({}, ...__VLS_functionalComponentArgsRest(__VLS_44));
    __VLS_46.slots.default;
    const __VLS_47 = {}.ArrowDown;
    /** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
    // @ts-ignore
    const __VLS_48 = __VLS_asFunctionalComponent(__VLS_47, new __VLS_47({}));
    const __VLS_49 = __VLS_48({}, ...__VLS_functionalComponentArgsRest(__VLS_48));
    var __VLS_46;
}
const __VLS_51 = {}.ElDropdown;
/** @type {[typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, typeof __VLS_components.ElDropdown, typeof __VLS_components.elDropdown, ]} */ ;
// @ts-ignore
const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
    ...{ 'onCommand': {} },
    trigger: "click",
}));
const __VLS_53 = __VLS_52({
    ...{ 'onCommand': {} },
    trigger: "click",
}, ...__VLS_functionalComponentArgsRest(__VLS_52));
let __VLS_55;
let __VLS_56;
let __VLS_57;
const __VLS_58 = {
    onCommand: (__VLS_ctx.handleUserCommand)
};
__VLS_54.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "user-trigger" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.sessionStore.userName);
const __VLS_59 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({}));
const __VLS_61 = __VLS_60({}, ...__VLS_functionalComponentArgsRest(__VLS_60));
__VLS_62.slots.default;
const __VLS_63 = {}.ArrowDown;
/** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
// @ts-ignore
const __VLS_64 = __VLS_asFunctionalComponent(__VLS_63, new __VLS_63({}));
const __VLS_65 = __VLS_64({}, ...__VLS_functionalComponentArgsRest(__VLS_64));
var __VLS_62;
{
    const { dropdown: __VLS_thisSlot } = __VLS_54.slots;
    const __VLS_67 = {}.ElDropdownMenu;
    /** @type {[typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, typeof __VLS_components.ElDropdownMenu, typeof __VLS_components.elDropdownMenu, ]} */ ;
    // @ts-ignore
    const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({}));
    const __VLS_69 = __VLS_68({}, ...__VLS_functionalComponentArgsRest(__VLS_68));
    __VLS_70.slots.default;
    const __VLS_71 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_72 = __VLS_asFunctionalComponent(__VLS_71, new __VLS_71({
        command: "profile",
    }));
    const __VLS_73 = __VLS_72({
        command: "profile",
    }, ...__VLS_functionalComponentArgsRest(__VLS_72));
    __VLS_74.slots.default;
    var __VLS_74;
    const __VLS_75 = {}.ElDropdownItem;
    /** @type {[typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, typeof __VLS_components.ElDropdownItem, typeof __VLS_components.elDropdownItem, ]} */ ;
    // @ts-ignore
    const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
        command: "logout",
    }));
    const __VLS_77 = __VLS_76({
        command: "logout",
    }, ...__VLS_functionalComponentArgsRest(__VLS_76));
    __VLS_78.slots.default;
    var __VLS_78;
    var __VLS_70;
}
var __VLS_54;
var __VLS_26;
const __VLS_79 = {}.ElMain;
/** @type {[typeof __VLS_components.ElMain, typeof __VLS_components.elMain, typeof __VLS_components.ElMain, typeof __VLS_components.elMain, ]} */ ;
// @ts-ignore
const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
    ...{ class: "content" },
}));
const __VLS_81 = __VLS_80({
    ...{ class: "content" },
}, ...__VLS_functionalComponentArgsRest(__VLS_80));
__VLS_82.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tabbar" },
});
const __VLS_83 = {}.ElTabs;
/** @type {[typeof __VLS_components.ElTabs, typeof __VLS_components.elTabs, typeof __VLS_components.ElTabs, typeof __VLS_components.elTabs, ]} */ ;
// @ts-ignore
const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
    ...{ 'onTabChange': {} },
    ...{ 'onTabRemove': {} },
    modelValue: (__VLS_ctx.activeTab),
    type: "card",
    ...{ class: "workspace-tabs" },
}));
const __VLS_85 = __VLS_84({
    ...{ 'onTabChange': {} },
    ...{ 'onTabRemove': {} },
    modelValue: (__VLS_ctx.activeTab),
    type: "card",
    ...{ class: "workspace-tabs" },
}, ...__VLS_functionalComponentArgsRest(__VLS_84));
let __VLS_87;
let __VLS_88;
let __VLS_89;
const __VLS_90 = {
    onTabChange: (__VLS_ctx.handleTabClick)
};
const __VLS_91 = {
    onTabRemove: (__VLS_ctx.handleTabRemove)
};
__VLS_86.slots.default;
for (const [tab] of __VLS_getVForSourceType((__VLS_ctx.appStore.visitedTabs))) {
    const __VLS_92 = {}.ElTabPane;
    /** @type {[typeof __VLS_components.ElTabPane, typeof __VLS_components.elTabPane, typeof __VLS_components.ElTabPane, typeof __VLS_components.elTabPane, ]} */ ;
    // @ts-ignore
    const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
        key: (tab.path),
        name: (tab.path),
        closable: (tab.closable),
    }));
    const __VLS_94 = __VLS_93({
        key: (tab.path),
        name: (tab.path),
        closable: (tab.closable),
    }, ...__VLS_functionalComponentArgsRest(__VLS_93));
    __VLS_95.slots.default;
    {
        const { label: __VLS_thisSlot } = __VLS_95.slots;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onContextmenu: (...[$event]) => {
                    __VLS_ctx.handleTabContextMenu($event, tab.path);
                } },
            ...{ onDragstart: (...[$event]) => {
                    __VLS_ctx.handleTabDragStart($event, tab.path);
                } },
            ...{ onDragover: (...[$event]) => {
                    __VLS_ctx.handleTabDragOver($event, tab.path);
                } },
            ...{ onDrop: (...[$event]) => {
                    __VLS_ctx.handleTabDrop($event, tab.path);
                } },
            ...{ onDragend: (__VLS_ctx.handleTabDragEnd) },
            ...{ class: "workspace-tab-label" },
            ...{ class: (__VLS_ctx.getTabDragClass(tab.path)) },
            draggable: "true",
        });
        (tab.title);
    }
    var __VLS_95;
}
var __VLS_86;
if (__VLS_ctx.tabContextMenuVisible) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "tab-context-menu" },
        ...{ style: ({
                left: `${__VLS_ctx.tabContextMenuPosition.x}px`,
                top: `${__VLS_ctx.tabContextMenuPosition.y}px`,
            }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.tabContextMenuVisible))
                    return;
                __VLS_ctx.handleTabContextAction('close-current');
            } },
        ...{ class: "tab-context-menu__item" },
        disabled: (!__VLS_ctx.canCloseContextTab),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.tabContextMenuVisible))
                    return;
                __VLS_ctx.handleTabContextAction('close-others');
            } },
        ...{ class: "tab-context-menu__item" },
        disabled: (!__VLS_ctx.canCloseOthers),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.tabContextMenuVisible))
                    return;
                __VLS_ctx.handleTabContextAction('close-right');
            } },
        ...{ class: "tab-context-menu__item" },
        disabled: (!__VLS_ctx.canCloseRightTabs),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.tabContextMenuVisible))
                    return;
                __VLS_ctx.handleTabContextAction('close-all');
            } },
        ...{ class: "tab-context-menu__item" },
        disabled: (!__VLS_ctx.hasClosableTabs),
    });
}
const __VLS_96 = {}.RouterView;
/** @type {[typeof __VLS_components.RouterView, typeof __VLS_components.routerView, ]} */ ;
// @ts-ignore
const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({}));
const __VLS_98 = __VLS_97({}, ...__VLS_functionalComponentArgsRest(__VLS_97));
var __VLS_82;
var __VLS_22;
var __VLS_3;
if (__VLS_ctx.sessionStore.requiresOrgSelection) {
    const __VLS_100 = {}.ElDialog;
    /** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
    // @ts-ignore
    const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
        modelValue: (__VLS_ctx.orgDialogVisible),
        title: "选择机构",
        width: "960px",
        ...{ class: "org-dialog" },
    }));
    const __VLS_102 = __VLS_101({
        modelValue: (__VLS_ctx.orgDialogVisible),
        title: "选择机构",
        width: "960px",
        ...{ class: "org-dialog" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_101));
    __VLS_103.slots.default;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-dialog-tools" },
    });
    const __VLS_104 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
        modelValue: (__VLS_ctx.orgKeyword),
        placeholder: "请输入机构名称/编码/商户号",
        clearable: true,
    }));
    const __VLS_106 = __VLS_105({
        modelValue: (__VLS_ctx.orgKeyword),
        placeholder: "请输入机构名称/编码/商户号",
        clearable: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_105));
    const __VLS_108 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
        modelValue: (__VLS_ctx.orgCity),
        placeholder: "请选择城市",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_110 = __VLS_109({
        modelValue: (__VLS_ctx.orgCity),
        placeholder: "请选择城市",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_109));
    __VLS_111.slots.default;
    for (const [city] of __VLS_getVForSourceType((__VLS_ctx.cityOptions))) {
        const __VLS_112 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
            key: (city),
            label: (city),
            value: (city),
        }));
        const __VLS_114 = __VLS_113({
            key: (city),
            label: (city),
            value: (city),
        }, ...__VLS_functionalComponentArgsRest(__VLS_113));
    }
    var __VLS_111;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-dialog-content" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
        ...{ class: "org-dialog-left" },
    });
    for (const [group] of __VLS_getVForSourceType((__VLS_ctx.rootGroups))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.sessionStore.requiresOrgSelection))
                        return;
                    __VLS_ctx.activeGroupId = group.id;
                } },
            key: (group.id),
            ...{ class: "org-group-btn" },
            ...{ class: ({ active: group.id === __VLS_ctx.activeGroupId }) },
        });
        (group.name);
    }
    const __VLS_116 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
        data: (__VLS_ctx.activeGroupRows),
        border: true,
        height: "400",
    }));
    const __VLS_118 = __VLS_117({
        data: (__VLS_ctx.activeGroupRows),
        border: true,
        height: "400",
    }, ...__VLS_functionalComponentArgsRest(__VLS_117));
    __VLS_119.slots.default;
    const __VLS_120 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({
        prop: "name",
        label: "机构名称",
        minWidth: "220",
    }));
    const __VLS_122 = __VLS_121({
        prop: "name",
        label: "机构名称",
        minWidth: "220",
    }, ...__VLS_functionalComponentArgsRest(__VLS_121));
    const __VLS_124 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_125 = __VLS_asFunctionalComponent(__VLS_124, new __VLS_124({
        label: "机构类型",
        width: "120",
    }));
    const __VLS_126 = __VLS_125({
        label: "机构类型",
        width: "120",
    }, ...__VLS_functionalComponentArgsRest(__VLS_125));
    __VLS_127.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_127.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (row.type === 'group' ? '集团' : row.type === 'trial' ? '试店' : '门店');
    }
    var __VLS_127;
    const __VLS_128 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_129 = __VLS_asFunctionalComponent(__VLS_128, new __VLS_128({
        prop: "code",
        label: "机构编码",
        width: "140",
    }));
    const __VLS_130 = __VLS_129({
        prop: "code",
        label: "机构编码",
        width: "140",
    }, ...__VLS_functionalComponentArgsRest(__VLS_129));
    const __VLS_132 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_133 = __VLS_asFunctionalComponent(__VLS_132, new __VLS_132({
        prop: "merchantNo",
        label: "商户号",
        width: "140",
    }));
    const __VLS_134 = __VLS_133({
        prop: "merchantNo",
        label: "商户号",
        width: "140",
    }, ...__VLS_functionalComponentArgsRest(__VLS_133));
    const __VLS_136 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_137 = __VLS_asFunctionalComponent(__VLS_136, new __VLS_136({
        label: "操作",
        width: "120",
        fixed: "right",
        align: "center",
    }));
    const __VLS_138 = __VLS_137({
        label: "操作",
        width: "120",
        fixed: "right",
        align: "center",
    }, ...__VLS_functionalComponentArgsRest(__VLS_137));
    __VLS_139.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_139.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        if (row.id === __VLS_ctx.sessionStore.currentOrgId) {
            const __VLS_140 = {}.ElTag;
            /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
            // @ts-ignore
            const __VLS_141 = __VLS_asFunctionalComponent(__VLS_140, new __VLS_140({
                type: "warning",
                size: "small",
            }));
            const __VLS_142 = __VLS_141({
                type: "warning",
                size: "small",
            }, ...__VLS_functionalComponentArgsRest(__VLS_141));
            __VLS_143.slots.default;
            var __VLS_143;
        }
        else {
            const __VLS_144 = {}.ElButton;
            /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
            // @ts-ignore
            const __VLS_145 = __VLS_asFunctionalComponent(__VLS_144, new __VLS_144({
                ...{ 'onClick': {} },
                link: true,
                type: "primary",
            }));
            const __VLS_146 = __VLS_145({
                ...{ 'onClick': {} },
                link: true,
                type: "primary",
            }, ...__VLS_functionalComponentArgsRest(__VLS_145));
            let __VLS_148;
            let __VLS_149;
            let __VLS_150;
            const __VLS_151 = {
                onClick: (...[$event]) => {
                    if (!(__VLS_ctx.sessionStore.requiresOrgSelection))
                        return;
                    if (!!(row.id === __VLS_ctx.sessionStore.currentOrgId))
                        return;
                    __VLS_ctx.selectOrg(row);
                }
            };
            __VLS_147.slots.default;
            var __VLS_147;
        }
    }
    var __VLS_139;
    var __VLS_119;
    var __VLS_103;
}
/** @type {__VLS_StyleScopedClasses['admin-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['sidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['brand']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-mark']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-copy']} */ ;
/** @type {__VLS_StyleScopedClasses['menu']} */ ;
/** @type {__VLS_StyleScopedClasses['main-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar-left']} */ ;
/** @type {__VLS_StyleScopedClasses['page-title']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar-right']} */ ;
/** @type {__VLS_StyleScopedClasses['org-switch-trigger']} */ ;
/** @type {__VLS_StyleScopedClasses['org-switch-line']} */ ;
/** @type {__VLS_StyleScopedClasses['org-switch-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['org-switch-title']} */ ;
/** @type {__VLS_StyleScopedClasses['user-trigger']} */ ;
/** @type {__VLS_StyleScopedClasses['content']} */ ;
/** @type {__VLS_StyleScopedClasses['tabbar']} */ ;
/** @type {__VLS_StyleScopedClasses['workspace-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['workspace-tab-label']} */ ;
/** @type {__VLS_StyleScopedClasses['tab-context-menu']} */ ;
/** @type {__VLS_StyleScopedClasses['tab-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['tab-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['tab-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['tab-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['org-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['org-dialog-tools']} */ ;
/** @type {__VLS_StyleScopedClasses['org-dialog-content']} */ ;
/** @type {__VLS_StyleScopedClasses['org-dialog-left']} */ ;
/** @type {__VLS_StyleScopedClasses['org-group-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ArrowDown: ArrowDown,
            Expand: Expand,
            Fold: Fold,
            SidebarMenuItem: SidebarMenuItem,
            route: route,
            appStore: appStore,
            sessionStore: sessionStore,
            iconMap: iconMap,
            menuItems: menuItems,
            activeMenu: activeMenu,
            openMenus: openMenus,
            activeTab: activeTab,
            orgDialogVisible: orgDialogVisible,
            orgKeyword: orgKeyword,
            orgCity: orgCity,
            activeGroupId: activeGroupId,
            tabContextMenuVisible: tabContextMenuVisible,
            tabContextMenuPosition: tabContextMenuPosition,
            cityOptions: cityOptions,
            currentOrg: currentOrg,
            rootGroups: rootGroups,
            activeGroupRows: activeGroupRows,
            hasClosableTabs: hasClosableTabs,
            canCloseContextTab: canCloseContextTab,
            canCloseOthers: canCloseOthers,
            canCloseRightTabs: canCloseRightTabs,
            handleSelect: handleSelect,
            handleTabClick: handleTabClick,
            handleTabDragStart: handleTabDragStart,
            handleTabDragOver: handleTabDragOver,
            handleTabDrop: handleTabDrop,
            handleTabDragEnd: handleTabDragEnd,
            getTabDragClass: getTabDragClass,
            handleTabRemove: handleTabRemove,
            handleTabContextMenu: handleTabContextMenu,
            handleTabContextAction: handleTabContextAction,
            openOrgDialog: openOrgDialog,
            selectOrg: selectOrg,
            handleUserCommand: handleUserCommand,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
