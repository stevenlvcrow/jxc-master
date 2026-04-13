import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { useRoute } from 'vue-router';

type TabItem = {
  path: string;
  title: string;
  closable: boolean;
};

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false);
  const visitedTabs = ref<TabItem[]>([]);

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  };

  const addVisitedTab = (tab: TabItem) => {
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

  const removeVisitedTab = (path: string) => {
    visitedTabs.value = visitedTabs.value.filter((item) => item.path !== path || !item.closable);
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
    resetVisitedTabs,
    useActiveMenu,
  };
});
