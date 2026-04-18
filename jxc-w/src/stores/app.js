import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { useRoute } from 'vue-router';
export const useAppStore = defineStore('app', () => {
    const sidebarCollapsed = ref(false);
    const visitedTabs = ref([]);
    const toggleSidebar = () => {
        sidebarCollapsed.value = !sidebarCollapsed.value;
    };
    const addVisitedTab = (tab) => {
        const existingTab = visitedTabs.value.find((item) => item.path === tab.path);
        if (existingTab) {
            existingTab.title = tab.title;
            existingTab.closable = tab.closable;
            return;
        }
        visitedTabs.value.push({
            ...tab,
            closable: visitedTabs.value.length > 0 && tab.closable,
        });
    };
    const removeVisitedTab = (path) => {
        visitedTabs.value = visitedTabs.value.filter((item) => item.path !== path || !item.closable);
    };
    const removeOtherVisitedTabs = (path) => {
        visitedTabs.value = visitedTabs.value.filter((item) => item.path === path || !item.closable);
    };
    const removeRightVisitedTabs = (path) => {
        const targetIndex = visitedTabs.value.findIndex((item) => item.path === path);
        if (targetIndex < 0) {
            return;
        }
        visitedTabs.value = visitedTabs.value.filter((item, index) => index <= targetIndex || !item.closable);
    };
    const removeAllClosableTabs = () => {
        visitedTabs.value = visitedTabs.value.filter((item) => !item.closable);
    };
    const moveVisitedTab = (sourcePath, targetPath, position) => {
        if (!sourcePath || !targetPath || sourcePath === targetPath) {
            return;
        }
        const sourceIndex = visitedTabs.value.findIndex((item) => item.path === sourcePath);
        const targetIndex = visitedTabs.value.findIndex((item) => item.path === targetPath);
        if (sourceIndex < 0 || targetIndex < 0) {
            return;
        }
        const [sourceTab] = visitedTabs.value.splice(sourceIndex, 1);
        if (!sourceTab) {
            return;
        }
        let adjustedTargetIndex = targetIndex;
        if (sourceIndex < targetIndex) {
            adjustedTargetIndex -= 1;
        }
        const insertIndex = position === 'before' ? adjustedTargetIndex : adjustedTargetIndex + 1;
        visitedTabs.value.splice(insertIndex, 0, sourceTab);
    };
    const resetVisitedTabs = () => {
        visitedTabs.value = [];
    };
    const useActiveMenu = () => {
        const route = useRoute();
        return computed(() => route.path);
    };
    return {
        sidebarCollapsed,
        visitedTabs,
        toggleSidebar,
        addVisitedTab,
        removeVisitedTab,
        removeOtherVisitedTabs,
        removeRightVisitedTabs,
        removeAllClosableTabs,
        moveVisitedTab,
        resetVisitedTabs,
        useActiveMenu,
    };
});
