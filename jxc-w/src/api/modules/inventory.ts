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
  salesmanUserId?: number;
  salesmanName?: string;
  upstreamCode?: string;
  remark?: string;
  items: PurchaseInboundLinePayload[];
};

export type PurchaseInboundDetail = {
  id: number;
  documentCode: string;
  status: string;
  inboundDate: string;
  warehouse: string;
  supplier: string;
  salesmanUserId: number | null;
  salesmanName: string;
  upstreamCode: string;
  remark: string;
  rejectionReason: string;
  items: PurchaseInboundLinePayload[];
};

export type PurchaseInboundPermission = {
  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  canApprove: boolean;
  canUnapprove: boolean;
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

export const fetchPurchaseInboundDetailApi = (id: number, orgId?: string) =>
  apiClient.get<PurchaseInboundDetail>(`/api/inventory/purchase-inbound/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const fetchPurchaseInboundPermissionApi = (orgId?: string) =>
  apiClient.get<PurchaseInboundPermission>('/api/inventory/purchase-inbound/permissions', {
    params: withOrgParams(undefined, orgId),
  });

export const updatePurchaseInboundApi = (id: number, payload: CreatePurchaseInboundPayload, orgId?: string) =>
  apiClient.put<void>(`/api/inventory/purchase-inbound/${id}`, payload, {
    params: withOrgParams(undefined, orgId),
  });

export const deletePurchaseInboundApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/inventory/purchase-inbound/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const batchDeletePurchaseInboundApi = (ids: number[], orgId?: string) =>
  apiClient.delete<void>('/api/inventory/purchase-inbound', {
    params: withOrgParams(undefined, orgId),
    data: { ids },
  });

export const batchApprovePurchaseInboundApi = (ids: number[], orgId?: string) =>
  apiClient.post<void>(
    '/api/inventory/purchase-inbound/batch-approve',
    { ids },
    { params: withOrgParams(undefined, orgId) },
  );

export const batchUnapprovePurchaseInboundApi = (ids: number[], rejectionReason: string, orgId?: string) =>
  apiClient.post<void>(
    '/api/inventory/purchase-inbound/batch-unapprove',
    { ids, rejectionReason },
    { params: withOrgParams(undefined, orgId) },
  );

export type GenericInventoryDocumentType =
  | 'purchase-return-outbound'
  | 'department-picking'
  | 'department-return'
  | 'stock-transfer'
  | 'stock-transfer-inbound'
  | 'department-transfer'
  | 'damage-outbound'
  | 'other-inbound'
  | 'other-outbound'
  | 'production-inbound'
  | 'customer-sales-outbound'
  | 'customer-return-inbound'
  | 'warehouse-opening-balance'
  | 'store-transfer'
  | 'stock-transfer-outbound';

export type GenericInventoryDocumentRow = {
  id: number;
  documentCode: string;
  documentDate: string;
  primaryName: string;
  secondaryName: string;
  counterpartyName: string;
  status: string;
  reviewStatus: string;
  amount: string;
  createdAt: string;
  creator: string;
  remark: string;
};

export type GenericInventoryDocumentPage = {
  list: GenericInventoryDocumentRow[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export type GenericInventoryDocumentLinePayload = {
  itemCode: string;
  itemName: string;
  spec?: string;
  category?: string;
  unitName?: string;
  availableQty?: number | null;
  quantity?: number | null;
  unitPrice?: number | null;
  amount?: number | null;
  lineReason?: string;
  remark?: string;
  extraFields?: Record<string, string>;
};

export type GenericInventoryDocumentSavePayload = {
  documentDate: string;
  primaryName?: string;
  secondaryName?: string;
  counterpartyName?: string;
  counterpartyName2?: string;
  reason?: string;
  upstreamCode?: string;
  salesmanUserId?: number;
  salesmanName?: string;
  remark?: string;
  extraFields?: Record<string, string>;
  items: GenericInventoryDocumentLinePayload[];
};

export type GenericInventoryDocumentDetail = {
  id: number;
  documentCode: string;
  status: string;
  documentDate: string;
  primaryName: string;
  secondaryName: string;
  counterpartyName: string;
  counterpartyName2: string;
  reason: string;
  upstreamCode: string;
  salesmanUserId: number | null;
  salesmanName: string;
  remark: string;
  rejectionReason: string;
  extraFields: Record<string, string>;
  items: Array<{
    itemCode: string;
    itemName: string;
    spec: string;
    category: string;
    unitName: string;
    availableQty: number | null;
    quantity: number | null;
    unitPrice: number | null;
    amount: number | null;
    lineReason: string;
    remark: string;
    extraFields: Record<string, string>;
  }>;
};

export type GenericInventoryDocumentPermission = {
  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  canApprove: boolean;
  canUnapprove: boolean;
};

export type GenericInventoryDocumentListParams = {
  pageNum: number;
  pageSize: number;
  startDate?: string;
  endDate?: string;
  documentCode?: string;
  primaryName?: string;
  itemName?: string;
  status?: string;
  remark?: string;
};

const genericDocumentBasePath = (documentType: GenericInventoryDocumentType) => `/api/inventory/${documentType}`;

export const fetchGenericInventoryDocumentPageApi = (
  documentType: GenericInventoryDocumentType,
  params: GenericInventoryDocumentListParams,
  orgId?: string,
) => apiClient.get<GenericInventoryDocumentPage>(genericDocumentBasePath(documentType), {
  params: withOrgParams(params, orgId),
});

export const fetchGenericInventoryDocumentPermissionApi = (
  documentType: GenericInventoryDocumentType,
  orgId?: string,
) => apiClient.get<GenericInventoryDocumentPermission>(`${genericDocumentBasePath(documentType)}/permissions`, {
  params: withOrgParams(undefined, orgId),
});

export const createGenericInventoryDocumentApi = (
  documentType: GenericInventoryDocumentType,
  payload: GenericInventoryDocumentSavePayload,
  orgId?: string,
) => apiClient.post<{ id: number; documentCode: string }>(
  genericDocumentBasePath(documentType),
  payload,
  { params: withOrgParams(undefined, orgId) },
);

export const fetchGenericInventoryDocumentDetailApi = (
  documentType: GenericInventoryDocumentType,
  id: number,
  orgId?: string,
) => apiClient.get<GenericInventoryDocumentDetail>(`${genericDocumentBasePath(documentType)}/${id}`, {
  params: withOrgParams(undefined, orgId),
});

export const updateGenericInventoryDocumentApi = (
  documentType: GenericInventoryDocumentType,
  id: number,
  payload: GenericInventoryDocumentSavePayload,
  orgId?: string,
) => apiClient.put<void>(`${genericDocumentBasePath(documentType)}/${id}`, payload, {
  params: withOrgParams(undefined, orgId),
});

export const deleteGenericInventoryDocumentApi = (
  documentType: GenericInventoryDocumentType,
  id: number,
  orgId?: string,
) => apiClient.delete<void>(`${genericDocumentBasePath(documentType)}/${id}`, {
  params: withOrgParams(undefined, orgId),
});

export const batchDeleteGenericInventoryDocumentApi = (
  documentType: GenericInventoryDocumentType,
  ids: number[],
  orgId?: string,
) => apiClient.delete<void>(genericDocumentBasePath(documentType), {
  params: withOrgParams(undefined, orgId),
  data: { ids },
});

export const batchApproveGenericInventoryDocumentApi = (
  documentType: GenericInventoryDocumentType,
  ids: number[],
  orgId?: string,
) => apiClient.post<void>(`${genericDocumentBasePath(documentType)}/batch-approve`, { ids }, {
  params: withOrgParams(undefined, orgId),
});

export const batchUnapproveGenericInventoryDocumentApi = (
  documentType: GenericInventoryDocumentType,
  ids: number[],
  rejectionReason: string,
  orgId?: string,
) => apiClient.post<void>(`${genericDocumentBasePath(documentType)}/batch-unapprove`, { ids, rejectionReason }, {
  params: withOrgParams(undefined, orgId),
});
