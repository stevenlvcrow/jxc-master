import { apiClient } from '@/api/http-client';

export type LoginRequest = {
  account: string;
  password: string;
};

export type LoginResult = {
  accessToken: string;
  refreshToken?: string;
  userName: string;
};

export const loginApi = (payload: LoginRequest) =>
  apiClient.post<LoginResult>('/api/identity/auth/login', payload, {
    meta: {
      skipAuth: true,
    },
  });
