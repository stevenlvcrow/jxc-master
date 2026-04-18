import axios from 'axios';
import { ElMessage } from 'element-plus';
import { authStorage } from '@/api/auth-storage';
import { ApiError } from '@/api/errors';
import { refreshAccessToken } from '@/api/refresh-token';
const baseConfig = {
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 12000,
};
const shouldUnwrap = (payload) => {
    if (!payload || typeof payload !== 'object') {
        return false;
    }
    return 'code' in payload && 'data' in payload;
};
const isLegacyResponse = (payload) => {
    if (!payload || typeof payload !== 'object') {
        return false;
    }
    return 'success' in payload && 'message' in payload;
};
const mapBusinessError = (payload, status) => {
    const message = payload.message ?? '请求失败，请稍后重试';
    const code = payload.code ?? status ?? -1;
    return new ApiError(message, code, payload.traceId, status);
};
const toApiError = (error) => {
    if (error instanceof ApiError) {
        return error;
    }
    if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        const data = error.response?.data;
        return mapBusinessError(data ?? {}, status);
    }
    return new ApiError('请求失败，请稍后重试');
};
const handleResponse = (response) => {
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
    client.interceptors.request.use((config) => {
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
    client.interceptors.response.use(handleResponse, async (error) => {
        const config = error.config;
        const status = error.response?.status;
        if (status === 401 && config && !config.meta?.skipAuth && !config._retry) {
            const hasRefreshToken = Boolean(authStorage.getRefreshToken());
            if (!hasRefreshToken) {
                const mapped401Error = toApiError(error);
                if (!config.meta?.silent) {
                    ElMessage.error(mapped401Error.message);
                }
                return Promise.reject(mapped401Error);
            }
            config._retry = true;
            try {
                const newToken = await refreshAccessToken();
                config.headers = config.headers ?? {};
                config.headers.Authorization = `Bearer ${newToken}`;
                return client.request(config);
            }
            catch (refreshError) {
                authStorage.clearTokens();
                return Promise.reject(toApiError(refreshError));
            }
        }
        const mappedError = toApiError(error);
        if (!config?.meta?.silent) {
            ElMessage.error(mappedError.message);
        }
        return Promise.reject(mappedError);
    });
    return client;
};
export const httpRaw = createHttpClient();
const request = async (method, url, config = {}) => {
    const { params, data, headers, timeout, meta } = config;
    return httpRaw.request({
        method,
        url,
        params,
        data,
        headers,
        timeout,
        meta,
    });
};
export const apiClient = {
    get: (url, config) => request('GET', url, config),
    post: (url, data, config) => request('POST', url, { ...config, data }),
    put: (url, data, config) => request('PUT', url, { ...config, data }),
    patch: (url, data, config) => request('PATCH', url, { ...config, data }),
    delete: (url, config) => request('DELETE', url, config),
};
