import { apiClient } from '@/api/http-client';
export const fetchWarehousesApi = (groupId, params) => {
    const search = new URLSearchParams();
    if (params?.keyword)
        search.set('keyword', params.keyword);
    if (params?.status && params.status !== '全部')
        search.set('status', params.status);
    if (params?.warehouseType && params.warehouseType !== '全部')
        search.set('warehouseType', params.warehouseType);
    const qs = search.toString() ? `?${search.toString()}` : '';
    return apiClient.get(`/api/identity/admin/groups/${groupId}/warehouses${qs}`);
};
export const createWarehouseApi = (groupId, payload) => apiClient.post(`/api/identity/admin/groups/${groupId}/warehouses`, payload);
export const fetchStoreWarehousesApi = (storeId, params) => {
    const search = new URLSearchParams();
    if (params?.keyword)
        search.set('keyword', params.keyword);
    if (params?.status && params.status !== '全部')
        search.set('status', params.status);
    if (params?.warehouseType && params.warehouseType !== '全部')
        search.set('warehouseType', params.warehouseType);
    const qs = search.toString() ? `?${search.toString()}` : '';
    return apiClient.get(`/api/identity/admin/stores/${storeId}/warehouses${qs}`);
};
export const createStoreWarehouseApi = (storeId, payload) => apiClient.post(`/api/identity/admin/stores/${storeId}/warehouses`, payload);
export const updateWarehouseApi = (id, payload) => apiClient.put(`/api/identity/admin/warehouses/${id}`, payload);
export const deleteWarehouseApi = (id) => apiClient.delete(`/api/identity/admin/warehouses/${id}`);
export const setWarehouseDefaultApi = (id) => apiClient.put(`/api/identity/admin/warehouses/${id}/default`);
export const updateWarehouseStatusApi = (id, status) => apiClient.put(`/api/identity/admin/warehouses/${id}/status`, { status });
