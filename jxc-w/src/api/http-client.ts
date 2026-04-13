import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage } from 'element-plus';

import { authStorage } from '@/api/auth-storage';
import { ApiError } from '@/api/errors';
import { refreshAccessToken } from '@/api/refresh-token';
import type { ApiFailure, ApiRequestConfig, ApiSuccess } from '@/api/types';

type ApiAxiosRequestConfig = InternalAxiosRequestConfig & {
  meta?: ApiRequestConfig['meta'];
  _retry?: boolean;
};

const baseConfig: AxiosRequestConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 12000,
};

const shouldUnwrap = (payload: unknown): payload is ApiSuccess<unknown> => {
  if (!payload || typeof payload !== 'object') {
    return false;
  }
  return 'code' in payload && 'data' in payload;
};

type LegacyApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

const isLegacyResponse = (payload: unknown): payload is LegacyApiResponse<unknown> => {
  if (!payload || typeof payload !== 'object') {
    return false;
  }
  return 'success' in payload && 'message' in payload;
};

const mapBusinessError = (payload: ApiFailure, status?: number) => {
  const message = payload.message ?? '请求失败，请稍后重试';
  const code = payload.code ?? status ?? -1;
  return new ApiError(message, code, payload.traceId, status);
};

const toApiError = (error: unknown) => {
  if (error instanceof ApiError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data as ApiFailure | undefined;
    return mapBusinessError(data ?? {}, status);
  }

  return new ApiError('请求失败，请稍后重试');
};

const handleResponse = (response: AxiosResponse) => {
  const payload = response.data;
  if (!shouldUnwrap(payload)) {
    if (isLegacyResponse(payload)) {
      if (payload.success) {
        return payload.data;
      }
      throw mapBusinessError({ message: payload.message }, response.status);
    }
    return payload;
  }

  if (payload.code === 0 || payload.code === 200) {
    return payload.data;
  }

  throw mapBusinessError(payload, response.status);
};

const createHttpClient = () => {
  const client = axios.create(baseConfig);

  client.interceptors.request.use((config: ApiAxiosRequestConfig) => {
    const skipAuth = config.meta?.skipAuth;
    if (!skipAuth) {
      const token = authStorage.getAccessToken();
      if (token) {
        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  });

  client.interceptors.response.use(
    handleResponse,
    async (error: AxiosError<ApiFailure>) => {
      const config = error.config as ApiAxiosRequestConfig | undefined;
      const status = error.response?.status;

      if (status === 401 && config && !config.meta?.skipAuth && !config._retry) {
        config._retry = true;
        try {
          const newToken = await refreshAccessToken();
          config.headers = config.headers ?? {};
          config.headers.Authorization = `Bearer ${newToken}`;
          return client.request(config);
        } catch (refreshError) {
          authStorage.clearTokens();
          return Promise.reject(toApiError(refreshError));
        }
      }

      const mappedError = toApiError(error);
      if (!config?.meta?.silent) {
        ElMessage.error(mappedError.message);
      }
      return Promise.reject(mappedError);
    },
  );

  return client;
};

export const httpRaw: AxiosInstance = createHttpClient();

const request = async <T>(
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
  url: string,
  config: ApiRequestConfig = {},
): Promise<T> => {
  const { params, data, headers, timeout, meta } = config;
  return httpRaw.request<T, T>({
    method,
    url,
    params,
    data,
    headers,
    timeout,
    meta,
  } as AxiosRequestConfig & { meta?: ApiRequestConfig['meta'] });
};

export const apiClient = {
  get: <T>(url: string, config?: ApiRequestConfig) => request<T>('GET', url, config),
  post: <T>(url: string, data?: unknown, config?: Omit<ApiRequestConfig, 'data'>) =>
    request<T>('POST', url, { ...config, data }),
  put: <T>(url: string, data?: unknown, config?: Omit<ApiRequestConfig, 'data'>) =>
    request<T>('PUT', url, { ...config, data }),
  patch: <T>(url: string, data?: unknown, config?: Omit<ApiRequestConfig, 'data'>) =>
    request<T>('PATCH', url, { ...config, data }),
  delete: <T>(url: string, config?: ApiRequestConfig) => request<T>('DELETE', url, config),
};

export type { ApiRequestConfig };
