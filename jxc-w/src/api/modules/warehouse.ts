import { apiClient } from '@/api/http-client';

export type WarehouseStatus = 'ENABLED' | 'DISABLED';
export type WarehouseType = '出品及生产部门' | '行政部门' | '普通仓库';

export type WarehouseRow = {
  id: number;
  warehouseCode: string;
  warehouseName: string;
  department: string | null;
  status: WarehouseStatus;
  warehouseType: WarehouseType;
  contactName: string | null;
  contactPhone: string | null;
  address: string;
  targetGrossMargin: string | null;
  idealPurchaseSaleRatio: string | null;
  isDefault: boolean;
  updatedAt: string;
};

export type WarehouseCreatePayload = {
  warehouseCode?: string;
  warehouseName: string;
  department?: string;
  status?: 'ENABLED' | 'DISABLED';
  warehouseType?: WarehouseType;
  contactName?: string;
  contactPhone?: string;
  regionPath?: string;
  address?: string;
  targetGrossMargin?: string;
  idealPurchaseSaleRatio?: string;
};

export type WarehouseUpdatePayload = {
  warehouseName: string;
  department?: string;
  status?: 'ENABLED' | 'DISABLED';
  warehouseType?: WarehouseType;
  contactName?: string;
  contactPhone?: string;
  regionPath?: string;
  address?: string;
  targetGrossMargin?: string;
  idealPurchaseSaleRatio?: string;
};

export const fetchWarehousesApi = (groupId: number, params?: { keyword?: string; status?: string; warehouseType?: string }) => {
  const search = new URLSearchParams();
  if (params?.keyword) search.set('keyword', params.keyword);
  if (params?.status && params.status !== '全部') search.set('status', params.status);
  if (params?.warehouseType && params.warehouseType !== '全部') search.set('warehouseType', params.warehouseType);
  const qs = search.toString() ? `?${search.toString()}` : '';
  return apiClient.get<WarehouseRow[]>(`/api/identity/admin/groups/${groupId}/warehouses${qs}`);
};

export const createWarehouseApi = (groupId: number, payload: WarehouseCreatePayload) =>
  apiClient.post<{ id: number }>(`/api/identity/admin/groups/${groupId}/warehouses`, payload);

export const updateWarehouseApi = (id: number, payload: WarehouseUpdatePayload) =>
  apiClient.put<void>(`/api/identity/admin/warehouses/${id}`, payload);

export const deleteWarehouseApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/warehouses/${id}`);

export const setWarehouseDefaultApi = (id: number) =>
  apiClient.put<void>(`/api/identity/admin/warehouses/${id}/default`);

export const updateWarehouseStatusApi = (id: number, status: 'ENABLED' | 'DISABLED') =>
  apiClient.put<void>(`/api/identity/admin/warehouses/${id}/status`, { status });
