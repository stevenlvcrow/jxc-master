import { apiClient } from '@/api/http-client';
const withOrgParams = (params, orgId) => ({
    ...(params ?? {}),
    ...(orgId ? { orgId } : {}),
});
export const fetchDishCategoryTreeApi = (orgId) => apiClient.get('/api/dishes/categories/tree', { params: withOrgParams(undefined, orgId) });
export const fetchDishesApi = (params, orgId) => apiClient.get('/api/dishes', { params: withOrgParams(params, orgId) });
