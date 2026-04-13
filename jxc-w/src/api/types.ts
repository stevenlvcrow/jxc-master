export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export type ApiSuccess<T> = {
  code: number;
  message: string;
  data: T;
  traceId?: string;
};

export type ApiFailure = {
  code?: number;
  message?: string;
  traceId?: string;
};

export type ApiPage<T> = {
  list: T[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type RequestMeta = {
  silent?: boolean;
  skipAuth?: boolean;
  rawResponse?: boolean;
};

export type ApiRequestConfig = {
  params?: Record<string, unknown>;
  data?: unknown;
  headers?: Record<string, string>;
  timeout?: number;
  meta?: RequestMeta;
};
