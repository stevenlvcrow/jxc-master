import { apiClient } from '@/api/http-client';

export type LoginRequest = {
  account: string;
  password: string;
};

export type LoginResult = {
  accessToken: string;
  refreshToken?: string;
  userName: string;
  platformAdmin?: boolean;
};

export type CurrentUserRole = {
  roleCode: string;
  roleName: string;
  scopeType: string;
  scopeName: string;
};

export const loginApi = (payload: LoginRequest) =>
  apiClient.post<LoginResult>('/api/identity/auth/login', payload, {
    meta: {
      skipAuth: true,
    },
  });

export const logoutApi = () =>
  apiClient.post<void>('/api/identity/auth/logout', undefined, {
    meta: {
      silent: true,
    },
  });

export const fetchCurrentUserRolesApi = (orgId?: string) =>
  apiClient.get<CurrentUserRole[]>('/api/identity/auth/me/roles', {
    params: orgId ? { orgId } : undefined,
  });
