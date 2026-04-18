import { apiClient } from '@/api/http-client';
export const fetchUnitsApi = (params, orgId) => apiClient.get('/api/identity/admin/units', {
    params: {
        ...params,
        orgId,
    },
});
export const createUnitApi = (payload, orgId) => apiClient.post('/api/identity/admin/units', payload, {
    params: { orgId },
});
export const updateUnitApi = (id, payload, orgId) => apiClient.put(`/api/identity/admin/units/${id}`, payload, {
    params: { orgId },
});
export const updateUnitStatusApi = (id, status, orgId) => apiClient.put(`/api/identity/admin/units/${id}/status`, { status }, {
    params: { orgId },
});
export const deleteUnitApi = (id, orgId) => apiClient.delete(`/api/identity/admin/units/${id}`, {
    params: { orgId },
});
