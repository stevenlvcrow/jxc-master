import { apiClient } from '@/api/http-client';

export type DictionaryStatus = 'ENABLED' | 'DISABLED';

export type DictionaryTypeItem = {
  id: number;
  dictCode: string;
  dictName: string;
  category: string;
  status: DictionaryStatus;
  builtin: boolean;
  sortNo: number;
  remark: string | null;
};

export type DictionaryItem = {
  id: number;
  dictTypeId: number;
  parentId: number | null;
  itemKey: string;
  itemCode: string;
  itemLabel: string;
  status: DictionaryStatus;
  builtin: boolean;
  sortNo: number;
  extraJson: string | null;
  remark: string | null;
};

export type DictionaryOption = {
  id: number;
  parentId: number | null;
  itemKey: string;
  itemCode: string;
  itemLabel: string;
  sortNo: number;
  extraJson: string | null;
};

type PageData<T> = {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export type DictionaryTypePayload = {
  dictCode: string;
  dictName: string;
  category?: string;
  status?: DictionaryStatus;
  sortNo?: number;
  remark?: string;
};

export type DictionaryItemPayload = {
  parentId?: number | null;
  itemKey: string;
  itemCode: string;
  itemLabel: string;
  status?: DictionaryStatus;
  sortNo?: number;
  extraJson?: string;
  remark?: string;
};

export const fetchDictionariesApi = (params?: {
  keyword?: string;
  category?: string;
  pageNum?: number;
  pageSize?: number;
}) => apiClient.get<PageData<DictionaryTypeItem>>('/api/identity/admin/dictionaries', { params });

export const createDictionaryApi = (payload: DictionaryTypePayload) =>
  apiClient.post<{ id: number }>('/api/identity/admin/dictionaries', payload);

export const updateDictionaryApi = (id: number, payload: DictionaryTypePayload) =>
  apiClient.put<void>(`/api/identity/admin/dictionaries/${id}`, payload);

export const updateDictionaryStatusApi = (id: number, status: DictionaryStatus) =>
  apiClient.put<void>(`/api/identity/admin/dictionaries/${id}/status`, { status });

export const deleteDictionaryApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/dictionaries/${id}`);

export const fetchDictionaryItemsApi = (dictTypeId: number) =>
  apiClient.get<DictionaryItem[]>(`/api/identity/admin/dictionaries/${dictTypeId}/items`);

export const createDictionaryItemApi = (dictTypeId: number, payload: DictionaryItemPayload) =>
  apiClient.post<{ id: number }>(`/api/identity/admin/dictionaries/${dictTypeId}/items`, payload);

export const updateDictionaryItemApi = (itemId: number, payload: DictionaryItemPayload) =>
  apiClient.put<void>(`/api/identity/admin/dictionary-items/${itemId}`, payload);

export const updateDictionaryItemStatusApi = (itemId: number, status: DictionaryStatus) =>
  apiClient.put<void>(`/api/identity/admin/dictionary-items/${itemId}/status`, { status });

export const deleteDictionaryItemApi = (itemId: number) =>
  apiClient.delete<void>(`/api/identity/admin/dictionary-items/${itemId}`);

export const fetchDictionaryOptionsApi = (codes: string[]) =>
  apiClient.get<Record<string, DictionaryOption[]>>('/api/dictionaries/items', {
    params: { codes: codes.join(',') },
  });
