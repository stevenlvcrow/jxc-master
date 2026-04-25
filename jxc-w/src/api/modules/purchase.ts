import { apiClient } from '@/api/http-client';

export type PurchaseDocumentType = 'applications' | 'application-reviews' | 'orders' | 'receipts' | 'returns';

export type PurchaseDocumentLine = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  itemCategory: string;
  supplier: string;
  purchaseUnit: string;
  baseUnit: string;
  baseConversion: string;
  quantity: number;
  reviewQty: number;
  receivedQty: number;
  unitPrice: number;
  taxRate: number;
  amount: number;
  isGift: boolean;
  warehouse: string;
  expectedArrivalDate: string;
  reviewStatus: string;
  remark: string;
};

export type PurchaseDocument = {
  id: number;
  documentType: string;
  documentCode: string;
  documentDate: string;
  expectedArrivalDate: string;
  purchaseOrg: string;
  warehouse: string;
  supplier: string;
  sourceDocumentCode: string;
  downstreamDocumentCode: string;
  documentStatus: string;
  reviewStatus: string;
  shipStatus: string;
  receiveStatus: string;
  reconciliationStatus: string;
  supplierSplit: string;
  documentBizType: string;
  splitReceipt: string;
  printStatus: string;
  returnReason: string;
  inspectionStatus: string;
  inspectionCount: number;
  adjustedPrice: boolean;
  creator: string;
  submitter: string;
  applicant: string;
  lastOperator: string;
  lastOperatedAt: string;
  remark: string;
  rejectionReason: string;
  amount: number;
  itemCount: number;
  workflowStatus: string;
  workflowTaskName: string;
  items: PurchaseDocumentLine[];
  createdAt: string;
};

export type PurchaseDocumentPage = {
  list: PurchaseDocument[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type PurchaseDocumentListParams = {
  pageNo: number;
  pageSize: number;
  startDate?: string;
  endDate?: string;
  documentCode?: string;
  applicationCode?: string;
  receiptCode?: string;
  sourceCode?: string;
  sourceDocumentCode?: string;
  purchaseOrderCode?: string;
  warehouse?: string;
  receiptWarehouse?: string;
  returnWarehouse?: string;
  supplier?: string;
  documentStatus?: string;
  reviewStatus?: string;
  shipStatus?: string;
  receiveStatus?: string;
  reconciliationStatus?: string;
  supplierSplit?: string;
  documentType?: string;
  returnReason?: string;
  inspectionStatus?: string;
  printStatus?: string;
  itemCode?: string;
  remark?: string;
};

export type PurchaseDocumentSavePayload = {
  documentDate: string;
  expectedArrivalDate?: string;
  purchaseOrg?: string;
  warehouse: string;
  supplier?: string;
  sourceDocumentCode?: string;
  downstreamDocumentCode?: string;
  documentBizType?: string;
  shipStatus?: string;
  receiveStatus?: string;
  reconciliationStatus?: string;
  supplierSplit?: string;
  splitReceipt?: string;
  printStatus?: string;
  returnReason?: string;
  inspectionStatus?: string;
  adjustedPrice?: boolean;
  applicant?: string;
  submitter?: string;
  remark?: string;
  items: Array<{
    itemCode: string;
    itemName: string;
    spec?: string;
    itemCategory?: string;
    supplier?: string;
    purchaseUnit?: string;
    baseUnit?: string;
    baseConversion?: string;
    quantity: number;
    reviewQty?: number;
    receivedQty?: number;
    unitPrice: number;
    taxRate?: number;
    amount?: number;
    isGift?: boolean;
    warehouse?: string;
    expectedArrivalDate?: string;
    reviewStatus?: string;
    remark?: string;
  }>;
};

const withOrgParams = <T extends Record<string, unknown>>(params?: T, orgId?: string) => ({
  ...(params ?? {}),
  ...(orgId ? { orgId } : {}),
});

export const fetchPurchaseDocumentPageApi = (type: PurchaseDocumentType, params: PurchaseDocumentListParams, orgId?: string) =>
  apiClient.get<PurchaseDocumentPage>(`/api/purchase/documents/${type}`, {
    params: withOrgParams(params, orgId),
  });

export const fetchPurchaseDocumentDetailApi = (type: PurchaseDocumentType, id: number, orgId?: string) =>
  apiClient.get<PurchaseDocument>(`/api/purchase/documents/${type}/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const createPurchaseDocumentApi = (type: PurchaseDocumentType, payload: PurchaseDocumentSavePayload, orgId?: string) =>
  apiClient.post<{ id: number; documentCode: string }>(`/api/purchase/documents/${type}`, payload, {
    params: withOrgParams(undefined, orgId),
  });

export const updatePurchaseDocumentApi = (type: PurchaseDocumentType, id: number, payload: PurchaseDocumentSavePayload, orgId?: string) =>
  apiClient.put<void>(`/api/purchase/documents/${type}/${id}`, payload, {
    params: withOrgParams(undefined, orgId),
  });

export const deletePurchaseDocumentApi = (type: PurchaseDocumentType, id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/purchase/documents/${type}/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const batchPurchaseDocumentActionApi = (
  type: PurchaseDocumentType,
  action: 'delete' | 'submit' | 'approve' | 'reject' | 'unapprove' | 'print' | 'close' | 'cancel-close' | 'receive' | 'cancel-receive',
  ids: number[],
  orgId?: string,
  rejectionReason?: string,
) =>
  apiClient.post<void>(`/api/purchase/documents/${type}/batch-${action}`, { ids, rejectionReason }, {
    params: withOrgParams(undefined, orgId),
  });

export const reviewPurchaseApplicationLinesApi = (
  lineReviews: Array<{ lineId: number; reviewQty: number; remark?: string }>,
  approved: boolean,
  orgId?: string,
  rejectionReason?: string,
) =>
  apiClient.post<void>('/api/purchase/documents/applications/review-lines', {
    approved,
    rejectionReason,
    lineReviews,
  }, {
    params: withOrgParams(undefined, orgId),
  });

export const updatePurchaseApplicationLinesApi = (
  lineUpdates: Array<{ lineId: number; supplier?: string; expectedArrivalDate?: string; reviewQty?: number; remark?: string }>,
  orgId?: string,
) =>
  apiClient.post<void>('/api/purchase/documents/applications/update-lines', {
    lineUpdates,
  }, {
    params: withOrgParams(undefined, orgId),
  });
