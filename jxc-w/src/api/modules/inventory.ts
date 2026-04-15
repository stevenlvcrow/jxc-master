import { apiClient } from '@/api/http-client';

export type PurchaseInboundRow = {
  id: number;
  documentCode: string;
  inboundDate: string;
  upstreamCode: string;
  warehouse: string;
  supplier: string;
  amountTaxIncluded: string;
  status: string;
  reviewStatus: string;
  reconciliationStatus: string;
  invoiceStatus: string;
  printStatus: string;
  inspectionCount: string;
  createdAt: string;
  creator: string;
  remark: string;
};

export type PurchaseInboundListParams = {
  pageNo: number;
  pageSize: number;
  timeType?: string;
  startDate?: string;
  endDate?: string;
  warehouse?: string;
  documentCode?: string;
  supplier?: string;
  itemName?: string;
  documentStatus?: string;
  reviewStatus?: string;
  reconciliationStatus?: string;
  splitStatus?: string;
  upstreamCode?: string;
  invoiceStatus?: string;
  adjustedPrice?: boolean;
  inspectionCount?: string;
  printStatus?: string;
  remark?: string;
};

export type PurchaseInboundPage = {
  list: PurchaseInboundRow[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type PurchaseInboundLinePayload = {
  itemCode: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
};

export type CreatePurchaseInboundPayload = {
  inboundDate: string;
  warehouse: string;
  supplier: string;
  upstreamCode?: string;
  remark?: string;
  items: PurchaseInboundLinePayload[];
};

const withOrgParams = <T extends Record<string, unknown>>(params?: T, orgId?: string) => ({
  ...(params ?? {}),
  ...(orgId ? { orgId } : {}),
});

export const fetchPurchaseInboundPageApi = (params: PurchaseInboundListParams, orgId?: string) =>
  apiClient.get<PurchaseInboundPage>('/api/inventory/purchase-inbound', { params: withOrgParams(params, orgId) });

export const createPurchaseInboundApi = (payload: CreatePurchaseInboundPayload, orgId?: string) =>
  apiClient.post<{ id: number; documentCode: string }>(
    '/api/inventory/purchase-inbound',
    payload,
    { params: withOrgParams(undefined, orgId) },
  );

export const batchApprovePurchaseInboundApi = (ids: number[], orgId?: string) =>
  apiClient.post<void>(
    '/api/inventory/purchase-inbound/batch-approve',
    { ids },
    { params: withOrgParams(undefined, orgId) },
  );

export const batchUnapprovePurchaseInboundApi = (ids: number[], orgId?: string) =>
  apiClient.post<void>(
    '/api/inventory/purchase-inbound/batch-unapprove',
    { ids },
    { params: withOrgParams(undefined, orgId) },
  );
