import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchCurrentMenusApi, type CurrentMenuItem } from '@/api/modules/menu';
import type { AppMenuItem } from '@/config/menu';

const normalizeIcon = (icon?: string | null) => {
  if (!icon) {
    return 'Document';
  }
  const lower = icon.toLowerCase();
  if (lower.includes('ticket')) {
    return 'Tickets';
  }
  if (lower.includes('goods') || lower.includes('box')) {
    return 'Goods';
  }
  if (lower.includes('collection')) {
    return 'Collection';
  }
  if (lower.includes('files')) {
    return 'Files';
  }
  if (lower.includes('coin') || lower.includes('money')) {
    return 'Coin';
  }
  if (lower.includes('check')) {
    return 'Checked';
  }
  if (lower.includes('bell') || lower.includes('alert')) {
    return 'Bell';
  }
  if (lower.includes('setting')) {
    return 'Setting';
  }
  if (lower.includes('shop')) {
    return 'Goods';
  }
  if (lower.includes('office') || lower.includes('building')) {
    return 'Collection';
  }
  if (lower.includes('data') || lower.includes('histogram')) {
    return 'DataAnalysis';
  }
  if (lower.includes('user') || lower.includes('team')) {
    return 'Document';
  }
  return 'Document';
};

const buildMenuTree = (rows: CurrentMenuItem[]): AppMenuItem[] => {
  const sorted = [...rows].sort((a, b) => {
    const left = a.sortNo ?? 0;
    const right = b.sortNo ?? 0;
    if (left !== right) {
      return left - right;
    }
    return a.id - b.id;
  });

  const map = new Map<number, AppMenuItem>();
  const roots: AppMenuItem[] = [];

  sorted.forEach((row) => {
    const normalizedTitle = row.routePath === '/group/user-role' ? '用户管理' : row.menuName;
    map.set(row.id, {
      key: `menu-${row.id}`,
      title: normalizedTitle,
      path: row.routePath ?? undefined,
      icon: undefined,
      menuType: row.menuType,
      children: [],
    });
  });

  sorted.forEach((row) => {
    const node = map.get(row.id);
    if (!node) {
      return;
    }
    if (row.parentId == null) {
      node.icon = node.icon ?? normalizeIcon(row.icon);
      roots.push(node);
      return;
    }
    const parent = map.get(row.parentId);
    if (!parent) {
      node.icon = node.icon ?? normalizeIcon(row.icon);
      roots.push(node);
      return;
    }
    parent.children = parent.children ?? [];
    parent.children.push(node);
  });

  const prune = (nodes: AppMenuItem[]): AppMenuItem[] => nodes
    .map((node) => {
      const children = node.children?.length ? prune(node.children) : undefined;
      return {
        ...node,
        children: children?.length ? children : undefined,
      };
    })
    .filter((node) => node.path || node.children?.length || node.menuType === 'DIRECTORY');

  return prune(roots);
};

export const useMenuStore = defineStore('menu', () => {
  const loading = ref(false);
  const loadedOrgId = ref<string>('');
  const rawMenus = ref<CurrentMenuItem[]>([]);
  const menuItems = computed<AppMenuItem[]>(() => buildMenuTree(rawMenus.value));

  const loadMenus = async (orgId?: string) => {
    const currentOrgId = orgId ?? '';
    loading.value = true;
    try {
      rawMenus.value = await fetchCurrentMenusApi(currentOrgId || undefined);
      loadedOrgId.value = currentOrgId;
    } finally {
      loading.value = false;
    }
  };

  const clearMenus = () => {
    rawMenus.value = [];
    loadedOrgId.value = '';
  };

  return {
    loading,
    loadedOrgId,
    menuItems,
    loadMenus,
    clearMenus,
  };
});
