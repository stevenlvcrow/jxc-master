import { apiClient } from '@/api/http-client';
export const fetchCurrentMenusApi = (orgId) => apiClient.get('/api/identity/menus/current', {
    params: orgId ? { orgId } : undefined,
});
