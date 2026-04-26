import { apiClient } from '@/api/http-client';

export type LoginRequest = {
  account: string;
  password: string;
};

export type LoginResult = {
  accessToken: string;
  refreshToken?: string;
  userName: string;
  account?: string;
  phone?: string;
  platformAdmin?: boolean;
};

export type CurrentUserProfile = {
  userId: number;
  userName: string;
  account: string;
  phone: string;
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

export const fetchCurrentUserProfileApi = () =>
  apiClient.get<CurrentUserProfile>('/api/identity/auth/me');

export const changeCurrentUserPasswordApi = (payload: { oldPassword: string; newPassword: string }) =>
  apiClient.put<void>('/api/identity/auth/me/password', payload);

export const changeCurrentUserPhoneApi = (payload: { phone: string }) =>
  apiClient.put<CurrentUserProfile>('/api/identity/auth/me/phone', payload);

export const changeCurrentUserAccountApi = (payload: { account: string }) =>
  apiClient.put<CurrentUserProfile>('/api/identity/auth/me/account', payload);
