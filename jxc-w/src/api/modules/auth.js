import { apiClient } from '@/api/http-client';
export const loginApi = (payload) => apiClient.post('/api/identity/auth/login', payload, {
    meta: {
        skipAuth: true,
    },
});
export const logoutApi = () => apiClient.post('/api/identity/auth/logout', undefined, {
    meta: {
        silent: true,
    },
});
export const fetchCurrentUserRolesApi = (orgId) => apiClient.get('/api/identity/auth/me/roles', {
    params: orgId ? { orgId } : undefined,
});
