import { apiClient } from '@/api/http-client';
export const fetchAdminUsersApi = () => apiClient.get('/api/identity/admin/users');
export const fetchStoreSalesmenApi = (orgId) => apiClient.get('/api/identity/admin/users/salesmen', {
    params: { orgId },
});
export const fetchAdminGroupsApi = () => apiClient.get('/api/identity/admin/groups');
export const createAdminGroupApi = (payload) => apiClient.post('/api/identity/admin/groups', payload);
export const updateAdminGroupApi = (id, payload) => apiClient.put(`/api/identity/admin/groups/${id}`, payload);
export const updateAdminGroupStatusApi = (id, status) => apiClient.put(`/api/identity/admin/groups/${id}/status`, { status });
export const deleteAdminGroupApi = (id) => apiClient.delete(`/api/identity/admin/groups/${id}`);
export const bindGroupAdminApi = (groupId, payload) => apiClient.post(`/api/identity/admin/groups/${groupId}/bind-admin`, payload);
export const fetchGroupStoresApi = (groupId) => apiClient.get(`/api/identity/admin/groups/${groupId}/stores`);
export const fetchGroupAdminCandidatesApi = (groupId) => apiClient.get(`/api/identity/admin/groups/${groupId}/admin-candidates`);
export const createGroupStoreApi = (groupId, payload) => apiClient.post(`/api/identity/admin/groups/${groupId}/stores`, payload);
export const updateGroupStoreApi = (groupId, storeId, payload) => apiClient.put(`/api/identity/admin/groups/${groupId}/stores/${storeId}`, payload);
export const deleteGroupStoreApi = (groupId, storeId) => apiClient.delete(`/api/identity/admin/groups/${groupId}/stores/${storeId}`);
export const createAdminUserApi = (payload) => apiClient.post('/api/identity/admin/users', payload);
export const updateAdminUserApi = (id, payload) => apiClient.put(`/api/identity/admin/users/${id}`, payload);
export const updateAdminUserStatusApi = (id, status) => apiClient.put(`/api/identity/admin/users/${id}/status`, { status });
export const deleteAdminUserApi = (id) => apiClient.delete(`/api/identity/admin/users/${id}`);
export const batchDeleteAdminUsersApi = (ids) => apiClient.delete('/api/identity/admin/users', { data: { ids } });
export const assignAdminUserRolesApi = (id, assignments) => apiClient.put(`/api/identity/admin/users/${id}/roles`, { assignments });
export const fetchAdminRolesApi = (orgId) => apiClient.get('/api/identity/admin/roles', { params: orgId ? { orgId } : undefined });
export const createAdminRoleApi = (payload, orgId) => apiClient.post('/api/identity/admin/roles', payload, {
    params: orgId ? { orgId } : undefined,
});
export const updateAdminRoleApi = (id, payload, orgId) => apiClient.put(`/api/identity/admin/roles/${id}`, payload, {
    params: orgId ? { orgId } : undefined,
});
export const updateAdminRoleStatusApi = (id, status) => apiClient.put(`/api/identity/admin/roles/${id}/status`, { status });
export const fetchAdminMenusApi = (orgId) => apiClient.get('/api/identity/admin/menus', { params: orgId ? { orgId } : undefined });
export const assignAdminRoleMenusApi = (id, menuIds) => apiClient.put(`/api/identity/admin/roles/${id}/menus`, { menuIds });
