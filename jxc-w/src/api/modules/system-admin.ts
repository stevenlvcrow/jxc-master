import { apiClient } from '@/api/http-client';

type AdminPageData<T> = {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
};

const ADMIN_LIST_PAGE_SIZE = 200;

const fetchAdminPagedList = async <T>(
  url: string,
  params?: Record<string, unknown>,
) => {
  const rows: T[] = [];
  let pageNum = 1;
  let total: number;

  do {
    const page = await apiClient.get<AdminPageData<T>>(url, {
      params: {
        ...params,
        pageNum,
        pageSize: ADMIN_LIST_PAGE_SIZE,
      },
    });
    const list = Array.isArray(page?.list) ? page.list : [];
    rows.push(...list);
    total = Number(page?.total ?? rows.length);

    if (!list.length || Number(page?.pageSize ?? 0) <= 0) {
      break;
    }
    pageNum += 1;
  } while (rows.length < total);

  return rows;
};

export type RoleAssignment = {
  roleId: number;
  roleCode: string;
  roleName: string;
  roleType: string;
  scopeType: string;
  scopeId: number | null;
  scopeName: string;
  builtin: boolean;
};

export type GroupAdminItem = {
  id: number;
  groupCode: string;
  groupName: string;
  status: string;
  remark: string | null;
  createdAt: string;
};

export type GroupStoreItem = {
  id: number;
  groupId: number;
  storeCode: string;
  storeName: string;
  status: string;
  contactName: string | null;
  contactPhone: string | null;
  address: string | null;
  remark: string | null;
  createdAt: string;
};

export type UserAdminItem = {
  id: number;
  username: string;
  realName: string;
  phone: string;
  status: string;
  createdAt: string;
  groups: Array<{ groupId: number; groupName: string }>;
  roles: RoleAssignment[];
};

export type GroupUserOptionItem = {
  userId: number;
  username: string;
  realName: string;
  phone: string;
  status: string;
};

export type SalesmanCandidateItem = {
  userId: number;
  realName: string;
  phone: string;
};

export type RoleAdminItem = {
  id: number;
  roleCode: string;
  tenantGroupId: number;
  tenantGroupName: string | null;
  roleName: string;
  roleType: string;
  dataScopeType: string;
  description: string;
  status: string;
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
  componentKey: string | null;
  permissionCode: string | null;
  icon: string | null;
  status: string;
  visible?: boolean;
  sortNo: number;
};

export type MenuMaintenancePayload = {
  menuCode: string;
  menuName: string;
  parentId: number | null;
  menuType: 'DIRECTORY' | 'MENU' | 'BUTTON' | 'API';
  routePath?: string | null;
  componentKey?: string | null;
  permissionCode?: string | null;
  icon?: string | null;
  sortNo: number;
  visible: boolean;
  status: string;
};

export type RoleUpsertPayload = {
  roleCode?: string;
  roleName: string;
  builtin?: boolean;
  roleType: string;
  dataScopeType: string;
  description?: string;
  status?: string;
  menuIds?: number[];
};

export const fetchAdminUsersApi = () => fetchAdminPagedList<UserAdminItem>('/api/identity/admin/users');
export const fetchStoreSalesmenApi = (orgId: string) =>
  fetchAdminPagedList<SalesmanCandidateItem>('/api/identity/admin/users/salesmen', { orgId });
export const fetchAdminGroupsApi = () => fetchAdminPagedList<GroupAdminItem>('/api/identity/admin/groups');

export const createAdminGroupApi = (payload: {
  groupCode?: string;
  groupName: string;
  adminRealName: string;
  adminPhone: string;
  status?: string;
  remark?: string;
}) => apiClient.post<{ id: number }>('/api/identity/admin/groups', payload);

export const updateAdminGroupApi = (id: number, payload: {
  groupCode?: string;
  groupName: string;
  status?: string;
  remark?: string;
}) => apiClient.put<void>(`/api/identity/admin/groups/${id}`, payload);

export const updateAdminGroupStatusApi = (id: number, status: string) =>
  apiClient.put<void>(`/api/identity/admin/groups/${id}/status`, { status });

export const deleteAdminGroupApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/groups/${id}`);

export const fetchGroupStoresApi = (groupId: number) =>
  fetchAdminPagedList<GroupStoreItem>(`/api/identity/admin/groups/${groupId}/stores`);

export const fetchGroupUsersApi = (groupId: number) =>
  fetchAdminPagedList<GroupUserOptionItem>(`/api/identity/admin/groups/${groupId}/users`);

export const createGroupStoreApi = (groupId: number, payload: {
  storeCode?: string;
  storeName: string;
  adminUserId: number;
  status?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
}) => apiClient.post<{ id: number }>(`/api/identity/admin/groups/${groupId}/stores`, payload);

export const updateGroupStoreApi = (groupId: number, storeId: number, payload: {
  storeName: string;
  status?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  remark?: string;
}) => apiClient.put<void>(`/api/identity/admin/groups/${groupId}/stores/${storeId}`, payload);

export const deleteGroupStoreApi = (groupId: number, storeId: number) =>
  apiClient.delete<void>(`/api/identity/admin/groups/${groupId}/stores/${storeId}`);

export const createAdminUserApi = (
  payload: { realName: string; phone: string; status?: string },
  orgId?: string,
) => apiClient.post<{ id: number }>('/api/identity/admin/users', payload, {
  params: orgId ? { orgId } : undefined,
});

export const updateAdminUserApi = (id: number, payload: { realName: string; phone: string; status?: string }) =>
  apiClient.put<void>(`/api/identity/admin/users/${id}`, payload);

export const updateAdminUserStatusApi = (id: number, status: string) =>
  apiClient.put<void>(`/api/identity/admin/users/${id}/status`, { status });

export const deleteAdminUserApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/users/${id}`);

export const batchDeleteAdminUsersApi = (ids: number[]) =>
  apiClient.delete<void>('/api/identity/admin/users', { data: { ids } });

export const assignAdminUserRolesApi = (
  id: number,
  assignments: Array<{ roleId: number; scopeType: string; scopeId: number | null }>,
) => apiClient.put<void>(`/api/identity/admin/users/${id}/roles`, { assignments });

export const fetchAdminRolesApi = (orgId?: string) =>
  fetchAdminPagedList<RoleAdminItem>('/api/identity/admin/roles', orgId ? { orgId } : undefined);

export const createAdminRoleApi = (payload: RoleUpsertPayload, orgId?: string) =>
  apiClient.post<{ id: number }>('/api/identity/admin/roles', payload, {
    params: orgId ? { orgId } : undefined,
  });

export const updateAdminRoleApi = (id: number, payload: RoleUpsertPayload, orgId?: string) =>
  apiClient.put<void>(`/api/identity/admin/roles/${id}`, payload, {
    params: orgId ? { orgId } : undefined,
  });

export const updateAdminRoleStatusApi = (id: number, status: string) =>
  apiClient.put<void>(`/api/identity/admin/roles/${id}/status`, { status });

export const deleteAdminRoleApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/identity/admin/roles/${id}`, {
    params: orgId ? { orgId } : undefined,
  });

export const fetchAdminMenusApi = (orgId?: string) =>
  fetchAdminPagedList<MenuAdminItem>('/api/identity/admin/menus', orgId ? { orgId } : undefined);

export const assignAdminRoleMenusApi = (id: number, menuIds: number[]) =>
  apiClient.put<void>(`/api/identity/admin/roles/${id}/menus`, { menuIds });

export const fetchMenuMaintenanceApi = () =>
  apiClient.get<MenuAdminItem[]>('/api/identity/admin/menu-maintenance');

export const createMenuMaintenanceApi = (payload: MenuMaintenancePayload) =>
  apiClient.post<{ id: number }>('/api/identity/admin/menu-maintenance', payload);

export const updateMenuMaintenanceApi = (id: number, payload: MenuMaintenancePayload) =>
  apiClient.put<void>(`/api/identity/admin/menu-maintenance/${id}`, payload);

export const deleteMenuMaintenanceApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/menu-maintenance/${id}`);

export const sortMenuMaintenanceApi = (items: Array<{ id: number; parentId: number | null; sortNo: number }>) =>
  apiClient.put<void>('/api/identity/admin/menu-maintenance/sort', { items });
