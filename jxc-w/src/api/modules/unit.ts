import { apiClient } from '@/api/http-client';

export type UnitType = 'STANDARD' | 'AUXILIARY';
export type UnitStatus = 'ENABLED' | 'DISABLED';

export type UnitItem = {
  id: number;
  code: string;
  name: string;
  type: UnitType;
  status: UnitStatus;
  remark?: string | null;
  createdAt: string;
};

export type UnitQuery = {
  keyword?: string;
  status?: UnitStatus | 'ALL';
  unitType?: UnitType | 'ALL';
};

export type UnitUpsertPayload = {
  code?: string;
  name: string;
  type: UnitType;
  status?: UnitStatus;
  remark?: string;
};

export const fetchUnitsApi = (params: UnitQuery, orgId?: string) =>
  apiClient.get<UnitItem[]>('/api/identity/admin/units', {
    params: {
      ...params,
      orgId,
    },
  });

export const createUnitApi = (payload: UnitUpsertPayload, orgId?: string) =>
  apiClient.post<{ id: number }>('/api/identity/admin/units', payload, {
    params: { orgId },
  });

export const updateUnitApi = (id: number, payload: UnitUpsertPayload, orgId?: string) =>
  apiClient.put<void>(`/api/identity/admin/units/${id}`, payload, {
    params: { orgId },
  });

export const updateUnitStatusApi = (id: number, status: UnitStatus, orgId?: string) =>
  apiClient.put<void>(`/api/identity/admin/units/${id}/status`, { status }, {
    params: { orgId },
  });

export const deleteUnitApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/identity/admin/units/${id}`, {
    params: { orgId },
  });
