import { apiClient } from '@/api/http-client';

export type WarehouseStatus = string;
export type WarehouseType = string;
type WarehouseQueryParams = { keyword?: string; status?: string; warehouseType?: string };
type WarehousePageData<T> = {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
};
const WAREHOUSE_LIST_PAGE_SIZE = 200;

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
  status?: string;
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
  status?: string;
  warehouseType?: WarehouseType;
  contactName?: string;
  contactPhone?: string;
  regionPath?: string;
  address?: string;
  targetGrossMargin?: string;
  idealPurchaseSaleRatio?: string;
};

const normalizeWarehouseQueryParams = (params?: WarehouseQueryParams) => ({
  keyword: params?.keyword || undefined,
  status: params?.status && params.status !== '全部' ? params.status : undefined,
  warehouseType: params?.warehouseType && params.warehouseType !== '全部' ? params.warehouseType : undefined,
});

const fetchWarehousePagedList = async (url: string, params?: WarehouseQueryParams) => {
  const rows: WarehouseRow[] = [];
  let pageNum = 1;
  let total: number;
  const normalizedParams = normalizeWarehouseQueryParams(params);

  do {
    const page = await apiClient.get<WarehousePageData<WarehouseRow>>(url, {
      params: {
        ...normalizedParams,
        pageNum,
        pageSize: WAREHOUSE_LIST_PAGE_SIZE,
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

export const fetchWarehousesApi = (groupId: number, params?: WarehouseQueryParams) => {
  return fetchWarehousePagedList(`/api/identity/admin/groups/${groupId}/warehouses`, params);
};

export const createWarehouseApi = (groupId: number, payload: WarehouseCreatePayload) =>
  apiClient.post<{ id: number }>(`/api/identity/admin/groups/${groupId}/warehouses`, payload);

export const fetchStoreWarehousesApi = (storeId: number, params?: WarehouseQueryParams) => {
  return fetchWarehousePagedList(`/api/identity/admin/stores/${storeId}/warehouses`, params);
};

export const createStoreWarehouseApi = (storeId: number, payload: WarehouseCreatePayload) =>
  apiClient.post<{ id: number }>(`/api/identity/admin/stores/${storeId}/warehouses`, payload);

export const updateWarehouseApi = (id: number, payload: WarehouseUpdatePayload) =>
  apiClient.put<void>(`/api/identity/admin/warehouses/${id}`, payload);

export const deleteWarehouseApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/warehouses/${id}`);

export const setWarehouseDefaultApi = (id: number) =>
  apiClient.put<void>(`/api/identity/admin/warehouses/${id}/default`);

export const updateWarehouseStatusApi = (id: number, status: string) =>
  apiClient.put<void>(`/api/identity/admin/warehouses/${id}/status`, { status });
