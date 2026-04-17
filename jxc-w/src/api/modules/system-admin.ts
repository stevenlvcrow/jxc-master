import { apiClient } from '@/api/http-client';

export type RoleAssignment = {
  roleId: number;
  roleCode: string;
  roleName: string;
  roleType: string;
  scopeType: string;
  scopeId: number | null;
  scopeName: string;
};

export type GroupAdminItem = {
  id: number;
  groupCode: string;
  groupName: string;
  status: 'ENABLED' | 'DISABLED';
  remark: string | null;
  createdAt: string;
};

export type GroupStoreItem = {
  id: number;
  groupId: number;
  storeCode: string;
  storeName: string;
  status: 'ENABLED' | 'DISABLED';
  contactName: string | null;
  contactPhone: string | null;
  address: string | null;
  remark: string | null;
  createdAt: string;
};

export type GroupAdminCandidateItem = {
  userId: number;
  realName: string;
  phone: string;
  storeId: number;
  storeCode: string;
  storeName: string;
};

export type UserAdminItem = {
  id: number;
  username: string;
  realName: string;
  phone: string;
  status: 'ENABLED' | 'DISABLED';
  createdAt: string;
  roles: RoleAssignment[];
};

export type RoleAdminItem = {
  id: number;
  roleCode: string;
  roleName: string;
  roleType: string;
  dataScopeType: string;
  description: string;
  status: 'ENABLED' | 'DISABLED';
  menuIds: number[];
  builtin?: boolean;
  editable?: boolean;
};

export type MenuAdminItem = {
  id: number;
  menuCode: string;
  menuName: string;
  parentId: number | null;
  menuType: 'DIRECTORY' | 'MENU' | 'BUTTON' | 'API';
  routePath: string | null;
  permissionCode: string | null;
  status: 'ENABLED' | 'DISABLED';
  sortNo: number;
};

export type RoleUpsertPayload = {
  roleCode?: string;
  roleName: string;
  roleType: string;
  dataScopeType: string;
  description?: string;
  status?: 'ENABLED' | 'DISABLED';
  menuIds?: number[];
};

export const fetchAdminUsersApi = () => apiClient.get<UserAdminItem[]>('/api/identity/admin/users');
export const fetchAdminGroupsApi = () => apiClient.get<GroupAdminItem[]>('/api/identity/admin/groups');

export const createAdminGroupApi = (payload: {
  groupCode?: string;
  groupName: string;
  status?: 'ENABLED' | 'DISABLED';
  remark?: string;
}) => apiClient.post<{ id: number }>('/api/identity/admin/groups', payload);

export const updateAdminGroupApi = (id: number, payload: {
  groupCode?: string;
  groupName: string;
  status?: 'ENABLED' | 'DISABLED';
  remark?: string;
}) => apiClient.put<void>(`/api/identity/admin/groups/${id}`, payload);

export const updateAdminGroupStatusApi = (id: number, status: 'ENABLED' | 'DISABLED') =>
  apiClient.put<void>(`/api/identity/admin/groups/${id}/status`, { status });

export const deleteAdminGroupApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/groups/${id}`);

export const bindGroupAdminApi = (groupId: number, payload: { phone: string; realName?: string }) =>
  apiClient.post<void>(`/api/identity/admin/groups/${groupId}/bind-admin`, payload);

export const fetchGroupStoresApi = (groupId: number) =>
  apiClient.get<GroupStoreItem[]>(`/api/identity/admin/groups/${groupId}/stores`);

export const fetchGroupAdminCandidatesApi = (groupId: number) =>
  apiClient.get<GroupAdminCandidateItem[]>(`/api/identity/admin/groups/${groupId}/admin-candidates`);

export const createGroupStoreApi = (groupId: number, payload: {
  storeCode?: string;
  storeName: string;
  status?: 'ENABLED' | 'DISABLED';
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
}) => apiClient.post<{ id: number }>(`/api/identity/admin/groups/${groupId}/stores`, payload);

export const createAdminUserApi = (payload: { realName: string; phone: string; status?: string }) =>
  apiClient.post<{ id: number }>('/api/identity/admin/users', payload);

export const updateAdminUserStatusApi = (id: number, status: 'ENABLED' | 'DISABLED') =>
  apiClient.put<void>(`/api/identity/admin/users/${id}/status`, { status });

export const assignAdminUserRolesApi = (
  id: number,
  assignments: Array<{ roleId: number; scopeType: string; scopeId: number | null }>,
) => apiClient.put<void>(`/api/identity/admin/users/${id}/roles`, { assignments });

export const fetchAdminRolesApi = () => apiClient.get<RoleAdminItem[]>('/api/identity/admin/roles');

export const createAdminRoleApi = (payload: RoleUpsertPayload) =>
  apiClient.post<{ id: number }>('/api/identity/admin/roles', payload);

export const updateAdminRoleApi = (id: number, payload: RoleUpsertPayload) =>
  apiClient.put<void>(`/api/identity/admin/roles/${id}`, payload);

export const updateAdminRoleStatusApi = (id: number, status: 'ENABLED' | 'DISABLED') =>
  apiClient.put<void>(`/api/identity/admin/roles/${id}/status`, { status });

export const fetchAdminMenusApi = () => apiClient.get<MenuAdminItem[]>('/api/identity/admin/menus');

export const assignAdminRoleMenusApi = (id: number, menuIds: number[]) =>
  apiClient.put<void>(`/api/identity/admin/roles/${id}/menus`, { menuIds });
