import { apiClient } from '@/api/http-client';

export type CostCardRow = {
  id: number;
  cardCode: string;
  cardName: string;
  cardType: string;
  status: string;
  effectiveVersionId: number | null;
  linkedDishCount: number;
  remark: string;
};

export type CostCardListParams = {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  status?: string;
};

export type CostCardPage = {
  list: CostCardRow[];
  total: number;
  pageNum: number;
  pageSize: number;
};

const withOrgParams = <T extends Record<string, unknown>>(params?: T, orgId?: string) => ({
  ...(params ?? {}),
  ...(orgId ? { orgId } : {}),
});

export const fetchCostCardsApi = (params: CostCardListParams, orgId?: string) =>
  apiClient.get<CostCardPage>('/api/cost/cards', {
    params: withOrgParams(params, orgId),
  });

export const activateCostCardVersionApi = (id: number, versionId: number, orgId?: string) =>
  apiClient.post<void>(`/api/cost/cards/${id}/versions/${versionId}/activate`, undefined, {
    params: withOrgParams(undefined, orgId),
  });

export const disableCostCardApi = (id: number, orgId?: string) =>
  apiClient.post<void>(`/api/cost/cards/${id}/disable`, undefined, {
    params: withOrgParams(undefined, orgId),
  });
