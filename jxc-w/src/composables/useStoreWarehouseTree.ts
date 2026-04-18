import { computed, ref } from 'vue';
import { useSessionStore } from '@/stores/session';
import { fetchStoreWarehousesApi, type WarehouseRow } from '@/api/modules/warehouse';

export type WarehouseTreeNode = {
  value: string;
  label: string;
  children?: WarehouseTreeNode[];
};

export const useStoreWarehouseTree = () => {
  const sessionStore = useSessionStore();
  const warehouseTree = ref<WarehouseTreeNode[]>([]);

  const resolveStoreId = (): number | null => {
    const currentOrg = sessionStore.currentOrg;
    if (!currentOrg) {
      return null;
    }
    if (currentOrg.type === 'store') {
      const storeId = Number(String(currentOrg.id).slice('store-'.length));
      return Number.isNaN(storeId) ? null : storeId;
    }
    if (currentOrg.type === 'group') {
      const firstStore = currentOrg.children?.[0];
      if (!firstStore) {
        return null;
      }
      const storeId = Number(String(firstStore.id).slice('store-'.length));
      return Number.isNaN(storeId) ? null : storeId;
    }
    return null;
  };

  const warehouseLabelMap = computed(() => {
    const map = new Map<string, string>();
    const walk = (nodes: WarehouseTreeNode[]) => {
      nodes.forEach((node) => {
        map.set(node.value, node.label);
        if (node.children?.length) {
          walk(node.children);
        }
      });
    };
    walk(warehouseTree.value);
    return map;
  });

  const loadWarehouseTree = async () => {
    const storeId = resolveStoreId();
    if (!storeId) {
      warehouseTree.value = [];
      return;
    }
    const rows = await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' });
    const grouped = rows.length ? [{
      value: 'warehouse-root',
      label: '当前门店仓库',
      children: rows.map((row: WarehouseRow) => ({
        value: row.warehouseName,
        label: row.warehouseCode ? `${row.warehouseName} / ${row.warehouseCode}` : row.warehouseName,
      })),
    }] : [];
    warehouseTree.value = grouped;
  };

  return {
    warehouseTree,
    warehouseLabelMap,
    loadWarehouseTree,
    resolveStoreId,
  };
};
