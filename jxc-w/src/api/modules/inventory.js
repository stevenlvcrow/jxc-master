import { apiClient } from '@/api/http-client';
const withOrgParams = (params, orgId) => ({
    ...(params ?? {}),
    ...(orgId ? { orgId } : {}),
});
export const fetchPurchaseInboundPageApi = (params, orgId) => apiClient.get('/api/inventory/purchase-inbound', { params: withOrgParams(params, orgId) });
export const createPurchaseInboundApi = (payload, orgId) => apiClient.post('/api/inventory/purchase-inbound', payload, { params: withOrgParams(undefined, orgId) });
export const fetchPurchaseInboundDetailApi = (id, orgId) => apiClient.get(`/api/inventory/purchase-inbound/${id}`, {
    params: withOrgParams(undefined, orgId),
});
export const fetchPurchaseInboundPermissionApi = (orgId) => apiClient.get('/api/inventory/purchase-inbound/permissions', {
    params: withOrgParams(undefined, orgId),
});
export const updatePurchaseInboundApi = (id, payload, orgId) => apiClient.put(`/api/inventory/purchase-inbound/${id}`, payload, {
    params: withOrgParams(undefined, orgId),
});
export const deletePurchaseInboundApi = (id, orgId) => apiClient.delete(`/api/inventory/purchase-inbound/${id}`, {
    params: withOrgParams(undefined, orgId),
});
export const batchDeletePurchaseInboundApi = (ids, orgId) => apiClient.delete('/api/inventory/purchase-inbound', {
    params: withOrgParams(undefined, orgId),
    data: { ids },
});
export const batchApprovePurchaseInboundApi = (ids, orgId) => apiClient.post('/api/inventory/purchase-inbound/batch-approve', { ids }, { params: withOrgParams(undefined, orgId) });
export const batchUnapprovePurchaseInboundApi = (ids, orgId) => apiClient.post('/api/inventory/purchase-inbound/batch-unapprove', { ids }, { params: withOrgParams(undefined, orgId) });
