import { apiClient } from '@/api/http-client';
const withOrgParams = (params, orgId) => ({
    ...(params ?? {}),
    ...(orgId ? { orgId } : {}),
});
export const fetchSuppliersApi = (params, orgId) => apiClient.get('/api/items/suppliers', {
    params: withOrgParams(params, orgId),
});
export const createSupplierApi = (payload, orgId) => apiClient.post('/api/items/suppliers', payload, {
    params: withOrgParams(undefined, orgId),
});
export const fetchSupplierDetailApi = (id, orgId) => apiClient.get(`/api/items/suppliers/${id}`, {
    params: withOrgParams(undefined, orgId),
});
export const updateSupplierApi = (id, payload, orgId) => apiClient.put(`/api/items/suppliers/${id}`, payload, {
    params: withOrgParams(undefined, orgId),
});
export const fetchSupplierCategoryTreeApi = (orgId) => apiClient.get('/api/items/supplier-categories/tree', {
    params: withOrgParams(undefined, orgId),
});
export const createSupplierCategoryApi = (payload, orgId) => apiClient.post('/api/items/supplier-categories', payload, {
    params: withOrgParams(undefined, orgId),
});
