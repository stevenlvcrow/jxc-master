import axios, {
  type AxiosError,
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
const ORG_SCOPE_INVALID_CODE = 40301;
const LOGIN_PATH = '/login';
const SELECT_ORG_PATH = '/select-org';

let authRedirecting = false;
let orgRedirecting = false;

const isRecord = (payload: unknown): payload is Record<string, unknown> => (
  Boolean(payload) && typeof payload === 'object'
);

const isUnifiedResponse = (payload: unknown): payload is ApiSuccess<unknown> => {
  if (!isRecord(payload)) {
    return false;
  }
  return typeof payload.code === 'number'
    && typeof payload.message === 'string'
    && 'data' in payload;
};

const mapBusinessError = (payload: ApiFailure, status?: number) => {
  const message = payload.message ?? '请求失败，请稍后重试';
  const code = payload.code ?? status ?? -1;
  return new ApiError(message, code, payload.traceId, status);
};

const isOrgScopeInvalid = (error: ApiError) => error.status === 403 && error.code === ORG_SCOPE_INVALID_CODE;

const resetRedirectLockLater = (type: 'auth' | 'org') => {
  window.setTimeout(() => {
    if (type === 'auth') {
      authRedirecting = false;
      return;
    }
    orgRedirecting = false;
  }, 500);
};

const redirectToLogin = async (message: string, silent?: boolean) => {
  if (authRedirecting) {
    return;
  }
  authRedirecting = true;
  try {
    const [
      { default: router },
      { pinia },
      { useSessionStore },
      { useMenuStore },
      { useAppStore },
    ] = await Promise.all([
      import('@/router'),
      import('@/stores'),
      import('@/stores/session'),
      import('@/stores/menu'),
      import('@/stores/app'),
    ]);
    const sessionStore = useSessionStore(pinia);
    const menuStore = useMenuStore(pinia);
    const appStore = useAppStore(pinia);
    sessionStore.logout();
    menuStore.clearMenus();
    appStore.resetVisitedTabs();
    if (router.currentRoute.value.path !== LOGIN_PATH) {
      await router.replace(LOGIN_PATH);
    }
    if (!silent) {
      ElMessage.error(message);
    }
  } finally {
    resetRedirectLockLater('auth');
  }
};

const redirectToSelectOrg = async (message: string, silent?: boolean) => {
  if (orgRedirecting) {
    return;
  }
  orgRedirecting = true;
  try {
    const [
      { default: router },
      { pinia },
      { useSessionStore },
      { useMenuStore },
      { useAppStore },
    ] = await Promise.all([
      import('@/router'),
      import('@/stores'),
      import('@/stores/session'),
      import('@/stores/menu'),
      import('@/stores/app'),
    ]);
    const sessionStore = useSessionStore(pinia);
    const menuStore = useMenuStore(pinia);
    const appStore = useAppStore(pinia);
    if (!sessionStore.isLoggedIn) {
      await redirectToLogin(message, silent);
      return;
    }
    sessionStore.clearSelectedOrg();
    menuStore.clearMenus();
    appStore.resetVisitedTabs();
    if (router.currentRoute.value.path !== SELECT_ORG_PATH) {
      await router.replace(SELECT_ORG_PATH);
    }
    if (!silent) {
      ElMessage.error(message);
    }
  } finally {
    resetRedirectLockLater('org');
  }
};

const toApiError = (error: unknown) => {
  if (error instanceof ApiError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data;
    if (data != null && !isUnifiedResponse(data)) {
      return new ApiError('接口响应结构不符合统一规范', -1, undefined, status);
    }
    return mapBusinessError((data ?? {}) as ApiFailure, status);
  }

  return new ApiError('请求失败，请稍后重试');
};

const handleResponse = (response: AxiosResponse) => {
  const payload = response.data;
  if (!isUnifiedResponse(payload)) {
    throw new ApiError('接口响应结构不符合统一规范', -1, undefined, response.status);
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
    handleResponse as unknown as (response: AxiosResponse) => AxiosResponse,
    async (error: AxiosError<ApiFailure>) => {
      const config = error.config as ApiAxiosRequestConfig | undefined;
      const status = error.response?.status;

      if (status === 401 && config && !config.meta?.skipAuth && !config._retry) {
        const hasRefreshToken = Boolean(authStorage.getRefreshToken());
        if (!hasRefreshToken) {
          const mapped401Error = toApiError(error);
          await redirectToLogin(mapped401Error.message, config.meta?.silent);
          return Promise.reject(mapped401Error);
        }
        config._retry = true;
        try {
          const newToken = await refreshAccessToken();
          config.headers = config.headers ?? {};
          config.headers.Authorization = `Bearer ${newToken}`;
          return client.request(config);
        } catch (refreshError) {
          const mappedRefreshError = toApiError(refreshError);
          await redirectToLogin(mappedRefreshError.message, config.meta?.silent);
          return Promise.reject(mappedRefreshError);
        }
      }

      const mappedError = toApiError(error);
      if (mappedError.status === 401) {
        await redirectToLogin(mappedError.message, config?.meta?.silent);
        return Promise.reject(mappedError);
      }
      if (isOrgScopeInvalid(mappedError)) {
        await redirectToSelectOrg(mappedError.message, config?.meta?.silent);
        return Promise.reject(mappedError);
      }
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
