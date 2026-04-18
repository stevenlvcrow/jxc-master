import { apiClient } from '@/api/http-client';
export const fetchItemsApi = async (params, orgId) => {
    return apiClient.get('/api/items', {
        params: withOrgParams(params, orgId),
    });
};
export const batchUpdateItemStatusApi = async (ids, status, orgId) => {
    return apiClient.post('/api/items/batch-status', {
        ids,
        status,
    }, { params: withOrgParams(undefined, orgId) });
};
export const batchDeleteItemsApi = async (ids, orgId) => {
    return apiClient.post('/api/items/batch-delete', {
        ids,
    }, { params: withOrgParams(undefined, orgId) });
};
export const createItemApi = (payload, orgId) => apiClient.post('/api/items', payload, { params: withOrgParams(undefined, orgId) });
export const saveItemDraftApi = (payload, orgId) => apiClient.post('/api/items/drafts', payload, { params: withOrgParams(undefined, orgId) });
export const fetchItemDetailApi = (id, orgId) => apiClient.get(`/api/items/${id}`, { params: withOrgParams(undefined, orgId) });
export const updateItemApi = (id, payload, orgId) => apiClient.put(`/api/items/${id}`, payload, { params: withOrgParams(undefined, orgId) });
const withOrgParams = (params, orgId) => ({
    ...(params ?? {}),
    ...(orgId ? { orgId } : {}),
});
export const fetchItemCategoriesApi = (params, orgId) => apiClient.get('/api/items/categories', { params: withOrgParams(params, orgId) });
export const fetchItemCategoryTreeApi = (orgId) => apiClient.get('/api/items/categories/tree', { params: withOrgParams(undefined, orgId) });
export const createItemCategoryApi = (payload, orgId) => apiClient.post('/api/items/categories', payload, { params: withOrgParams(undefined, orgId) });
export const batchCreateItemCategoriesApi = (payload, orgId) => apiClient.post('/api/items/categories/batch', payload, { params: withOrgParams(undefined, orgId) });
export const updateItemCategoryApi = (id, payload, orgId) => apiClient.put(`/api/items/categories/${id}`, payload, { params: withOrgParams(undefined, orgId) });
export const updateItemCategoryStatusApi = (id, status, orgId) => apiClient.put(`/api/items/categories/${id}/status`, { status }, { params: withOrgParams(undefined, orgId) });
export const deleteItemCategoryApi = (id, orgId) => apiClient.delete(`/api/items/categories/${id}`, { params: withOrgParams(undefined, orgId) });
export const batchDeleteItemCategoriesApi = (ids, orgId) => apiClient.post('/api/items/categories/batch-delete', { ids }, { params: withOrgParams(undefined, orgId) });
export const fetchItemStatisticsTypesApi = (params, orgId) => apiClient.get('/api/items/statistics-types', { params: withOrgParams(params, orgId) });
export const fetchItemStatisticsTypeDetailApi = (id, orgId) => apiClient.get(`/api/items/statistics-types/${id}`, { params: withOrgParams(undefined, orgId) });
export const createItemStatisticsTypeApi = (payload, orgId) => apiClient.post('/api/items/statistics-types', payload, { params: withOrgParams(undefined, orgId) });
export const exportItemStatisticsTypesApi = (ids, orgId) => apiClient.post('/api/items/statistics-types/batch-export', { ids: ids ?? [] }, { params: withOrgParams(undefined, orgId) });
export const fetchItemTagsApi = (params, orgId) => apiClient.get('/api/items/tags', { params: withOrgParams(params, orgId) });
export const createItemTagApi = (payload, orgId) => apiClient.post('/api/items/tags', payload, { params: withOrgParams(undefined, orgId) });
export const updateItemTagApi = (id, payload, orgId) => apiClient.put(`/api/items/tags/${id}`, payload, { params: withOrgParams(undefined, orgId) });
export const deleteItemTagApi = (id, orgId) => apiClient.delete(`/api/items/tags/${id}`, { params: withOrgParams(undefined, orgId) });
export const batchImportItemTagsApi = (payload, orgId) => apiClient.post('/api/items/tags/batch-import', payload, { params: withOrgParams(undefined, orgId) });
