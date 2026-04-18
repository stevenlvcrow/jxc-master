import { computed, ref } from 'vue';
import { useSessionStore } from '@/stores/session';
import { fetchStoreWarehousesApi } from '@/api/modules/warehouse';
export const useStoreWarehouseTree = () => {
    const sessionStore = useSessionStore();
    const warehouseTree = ref([]);
    const resolveStoreId = () => {
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
        const map = new Map();
        const walk = (nodes) => {
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
                children: rows.map((row) => ({
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
