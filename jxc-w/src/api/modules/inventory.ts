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
  spec?: string;
  category?: string;
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
  items: Array<{
    itemCode: string;
    itemName: string;
    spec: string;
    category: string;
    quantity: number;
    unitPrice: number;
    taxRate?: number;
  }>;
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

export type PeriodOpeningRow = {
  id: number;
  documentCode: string;
  warehouseName: string;
  periodType: string;
  periodStartDate: string;
  periodEndDate: string;
  sourceType: string;
  status: string;
  totalQuantity: number;
  totalAmount: number;
  remark: string;
  createdAt: string;
  approvedAt: string;
};

export type PeriodOpeningPage = {
  list: PeriodOpeningRow[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export type PeriodOpeningListParams = {
  pageNum: number;
  pageSize: number;
  documentCode?: string;
  warehouseName?: string;
  periodType?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
};

export type PeriodOpeningLinePayload = {
  itemCode: string;
  itemName: string;
  spec?: string;
  category?: string;
  unitName?: string;
  openingQty: number;
  openingAmount: number;
  remark?: string;
};

export type PeriodOpeningSavePayload = {
  warehouseName: string;
  periodType: string;
  periodStartDate: string;
  remark?: string;
  items: PeriodOpeningLinePayload[];
};

export type PeriodOpeningGeneratePayload = {
  warehouseName: string;
  periodType: string;
  periodStartDate: string;
  remark?: string;
};

export type PeriodOpeningDetail = {
  id: number;
  documentCode: string;
  warehouseName: string;
  periodType: string;
  periodStartDate: string;
  periodEndDate: string;
  sourceType: string;
  status: string;
  totalQuantity: number;
  totalAmount: number;
  remark: string;
  rejectionReason: string;
  creator: string;
  createdAt: string;
  auditor: string;
  approvedAt: string;
  items: Array<PeriodOpeningLinePayload & {
    id: number;
    openingAvgCost: number;
  }>;
};

export const fetchPeriodOpeningPageApi = (params: PeriodOpeningListParams, orgId?: string) =>
  apiClient.get<PeriodOpeningPage>('/api/inventory/period-openings', {
    params: withOrgParams(params, orgId),
  });

export const createPeriodOpeningApi = (payload: PeriodOpeningSavePayload, orgId?: string) =>
  apiClient.post<{ id: number; documentCode: string }>('/api/inventory/period-openings', payload, {
    params: withOrgParams(undefined, orgId),
  });

export const generatePeriodOpeningApi = (payload: PeriodOpeningGeneratePayload, orgId?: string) =>
  apiClient.post<{ id: number; documentCode: string }>('/api/inventory/period-openings/generate', payload, {
    params: withOrgParams(undefined, orgId),
  });

export const fetchPeriodOpeningDetailApi = (id: number, orgId?: string) =>
  apiClient.get<PeriodOpeningDetail>(`/api/inventory/period-openings/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const updatePeriodOpeningApi = (id: number, payload: PeriodOpeningSavePayload, orgId?: string) =>
  apiClient.put<void>(`/api/inventory/period-openings/${id}`, payload, {
    params: withOrgParams(undefined, orgId),
  });

export const deletePeriodOpeningApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/inventory/period-openings/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const submitPeriodOpeningApi = (id: number, orgId?: string) =>
  apiClient.post<void>(`/api/inventory/period-openings/${id}/submit`, undefined, {
    params: withOrgParams(undefined, orgId),
  });

export const approvePeriodOpeningApi = (id: number, orgId?: string) =>
  apiClient.post<void>(`/api/inventory/period-openings/${id}/approve`, undefined, {
    params: withOrgParams(undefined, orgId),
  });

export const rejectPeriodOpeningApi = (id: number, rejectionReason: string, orgId?: string) =>
  apiClient.post<void>(`/api/inventory/period-openings/${id}/reject`, { rejectionReason }, {
    params: withOrgParams(undefined, orgId),
  });

export type PeriodOpeningPermission = {
  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  canApprove: boolean;
  canReject: boolean;
};

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
  | 'profit-inbound'
  | 'loss-outbound'
  | 'production-inbound'
  | 'customer-sales-outbound'
  | 'customer-return-inbound'
  | 'dish-consumption-outbound'
  | 'store-transfer'
  | 'stock-transfer-outbound';

export type GenericInventoryDocumentRow = {
  id: number;
  documentCode: string;
  documentDate: string;
  primaryId?: number | null;
  primaryCode?: string;
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
  creator: string;
  createdAt: string;
  auditor: string;
  auditedAt: string;
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

export type InventoryBalanceQueryParams = {
  pageNum: number;
  pageSize: number;
  warehouse?: string;
  itemName?: string;
  checkDate?: string;
};

export type InventoryBalanceRow = {
  warehouse: string;
  itemCode: string;
  itemName: string;
  quantity: string;
  updatedAt: string;
};

export type InventoryBalancePage = {
  list: InventoryBalanceRow[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export const fetchInventoryBalancesApi = (params: InventoryBalanceQueryParams, orgId?: string) =>
  apiClient.get<InventoryBalancePage>('/api/inventory/balances', {
    params: withOrgParams(params, orgId),
  });

export type DishConsumptionOutboundReportDimension = '菜品消耗单明细' | '菜品消耗汇总';

export type DishConsumptionOutboundReportParams = {
  pageNo: number;
  pageSize: number;
  dimension: DishConsumptionOutboundReportDimension;
  startDate?: string;
  endDate?: string;
  warehouse?: string;
  dishName?: string;
  itemCode?: string;
  deductionType?: string;
  unitType?: string;
  queryScheme?: string;
};

export type DishConsumptionOutboundReportRow = {
  id?: string | number;
  consumptionNo: string;
  businessDate: string;
  dishSpuCode: string;
  dishSkuCode: string;
  dishName: string;
  dishSpec: string;
  costCard: string;
  orderSource: string;
  dishCategory: string;
  deductionType: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  warehouse: string;
  itemUnit: string;
  dishQty: number | string;
  theoreticalQty: number | string;
  outboundQty: number | string;
  pendingOutboundQty: number | string;
  unlinkedWarehousePendingQty?: number | string;
  otherReasonPendingQty?: number | string;
};

export type DishConsumptionOutboundReportSummary = {
  dishQty?: number | string;
  theoreticalQty?: number | string;
  outboundQty?: number | string;
  pendingOutboundQty?: number | string;
  unlinkedWarehousePendingQty?: number | string;
  otherReasonPendingQty?: number | string;
};

export type DishConsumptionOutboundReportPage = {
  list: DishConsumptionOutboundReportRow[];
  total: number;
  pageNo: number;
  pageSize: number;
  summary?: DishConsumptionOutboundReportSummary;
};

export const fetchDishConsumptionOutboundReportApi = (
  params: DishConsumptionOutboundReportParams,
  orgId?: string,
) => apiClient.get<DishConsumptionOutboundReportPage>('/api/inventory/dish-consumption-outbound/report', {
  params: withOrgParams(params, orgId),
});

export type InventoryInoutDetailStatisticDimension = '单据 + 物品' | '单据';

export type InventoryInoutDetailReportParams = {
  pageNo: number;
  pageSize: number;
  statisticDimension: InventoryInoutDetailStatisticDimension;
  warehouse?: string;
  warehouseType?: string;
  startDate?: string;
  endDate?: string;
  dateText?: string;
  auditStartTime?: string;
  auditEndTime?: string;
  inoutType?: string;
  upstreamDocumentType?: string;
  itemCategory?: string;
  statisticType?: string;
  itemCode?: string;
  reasonType?: string;
  adjustmentDocument?: string;
  oppositeOrg?: string;
  documentNo?: string;
  crossMonthDocument?: string;
  inoutDirection?: string;
  gift?: string;
  unitType?: string;
  queryScheme?: string;
};

export type InventoryInoutDetailReportRow = {
  id?: string | number;
  itemCode?: string;
  itemName?: string;
  specModel?: string;
  itemCategory?: string;
  statisticType?: string;
  baseUnit?: string;
  unit?: string;
  orgName?: string;
  orgCode?: string;
  warehouse?: string;
  warehouseType?: string;
  upstreamDocumentNo?: string;
  upstreamDocumentType?: string;
  documentNo?: string;
  inoutType?: string;
  reasonType?: string;
  adjustmentDocument?: string;
  oppositeOrg?: string;
  oppositeOrgCode?: string;
  upstreamDocumentDate?: string;
  documentDate?: string;
  documentCreatedAt?: string;
  documentCreator?: string;
  documentAuditTime?: string;
  inboundBaseQty?: number | string;
  inboundQty?: number | string;
  outboundQty?: number | string;
  remark?: string;
  returnDifferenceAmount?: number | string;
  inboundCostUnitPriceTaxIncluded?: number | string;
  inboundCostAmountTaxIncluded?: number | string;
  inboundSettlementUnitPriceTaxIncluded?: number | string;
  inboundSettlementAmountTaxIncluded?: number | string;
  inboundDiscountSettlementUnitPriceTaxIncluded?: number | string;
  inboundDiscountSettlementAmountTaxIncluded?: number | string;
  outboundBaseQty?: number | string;
  outboundCostAmountTaxIncluded?: number | string;
  outboundCostUnitPriceTaxIncluded?: number | string;
  outboundSettlementUnitPriceTaxIncluded?: number | string;
  outboundSettlementAmountTaxIncluded?: number | string;
  outboundDiscountSettlementUnitPriceTaxIncluded?: number | string;
  outboundDiscountSettlementAmountTaxIncluded?: number | string;
  outboundDiscountGrossProfitUnitPriceTaxIncluded?: number | string;
  outboundDiscountGrossProfitTaxIncluded?: number | string;
};

export type InventoryInoutDetailReportPage = {
  list: InventoryInoutDetailReportRow[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export const fetchInventoryInoutDetailReportApi = (
  params: InventoryInoutDetailReportParams,
  orgId?: string,
) => apiClient.get<InventoryInoutDetailReportPage>('/api/inventory/inout-detail/report', {
  params: withOrgParams(params, orgId),
});

export type ItemBatchTraceReportParams = {
  startDate?: string;
  endDate?: string;
  warehouses?: string;
  itemCode: string;
  batchNo: string;
  sourceOrg?: string;
  unitType?: string;
};

export type ItemBatchTraceSourceRow = {
  id: string;
  sourceOrg: string;
  inboundOrg: string;
  inboundWarehouse: string;
  inboundType: string;
  inboundDocumentNo: string;
  upstreamDocumentNo: string;
  inboundDate: string;
  inboundCreatedAt: string;
  batchNo: string;
  manufacturer: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  unit: string;
  inboundQty: number;
};

export type ItemBatchTraceInternalFlowRow = {
  id: string;
  orgName: string;
  warehouse: string;
  inoutType: string;
  documentNo: string;
  upstreamDocumentNo: string;
  documentDate: string;
  documentCreatedAt: string;
  batchNo: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  unit: string;
  inoutQty: number;
  currentBalanceQty: number;
};

export type ItemBatchTraceExternalFlowRow = {
  id: string;
  orgName: string;
  warehouse: string;
  targetOrg: string;
  outboundType: string;
  outboundDocumentNo: string;
  upstreamDocumentNo: string;
  outboundDate: string;
  outboundCreatedAt: string;
  batchNo: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  unit: string;
  inoutQty: number;
  targetBalanceQty: number;
};

export type ItemBatchTraceReport = {
  sourceRows: ItemBatchTraceSourceRow[];
  internalFlowRows: ItemBatchTraceInternalFlowRow[];
  externalFlowRows: ItemBatchTraceExternalFlowRow[];
};

export const fetchItemBatchTraceReportApi = (params: ItemBatchTraceReportParams, orgId?: string) =>
  apiClient.get<ItemBatchTraceReport>('/api/inventory/item-batch-trace/report', {
    params: withOrgParams(params, orgId),
  });

export type InventoryReportPage<T> = {
  list: T[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type StockWarningReportParams = {
  pageNo: number;
  pageSize: number;
  statisticDimension?: string;
  warehouse?: string;
  itemCategory?: string;
  itemCode?: string;
  itemStatus?: string;
  warningStatus?: string;
  unitType?: string;
};

export type StockWarningReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  unit: string;
  itemCategory: string;
  warehouse?: string;
  currentStock: number;
  stockUpperLimit: number;
  stockLowerLimit: number;
  warningStatus: string;
  itemStatus: string;
};

export const fetchStockWarningReportApi = (params: StockWarningReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<StockWarningReportRow>>('/api/inventory/stock-warning/report', {
    params: withOrgParams(params, orgId),
  });

export type StagnantStockReportParams = {
  pageNo: number;
  pageSize: number;
  warehouse?: string;
  itemCode?: string;
  itemCategory?: string;
  itemStatus?: string;
  stagnant?: string;
  stagnantDaysGreaterThan?: string;
  unitType?: string;
};

export type StagnantStockReportRow = {
  id: string;
  warehouse: string;
  itemName: string;
  itemCode: string;
  specModel: string;
  unit: string;
  firstInboundTime: string;
  latestInboundTime: string;
  latestOutboundTime: string;
  latestInboundQty: number;
  latestOutboundQty: number;
  stockQty: number;
  retainedDays: number;
  itemStagnantDays: number;
  stagnant: string;
  itemStatus: string;
  itemCategory: string;
};

export const fetchStagnantStockReportApi = (params: StagnantStockReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<StagnantStockReportRow>>('/api/inventory/stagnant-stock/report', {
    params: withOrgParams(params, orgId),
  });

export type InventoryProfitLossReportParams = {
  pageNo: number;
  pageSize: number;
  warehouse?: string;
  dateRangeStart?: string;
  dateRangeEnd?: string;
  itemCategory?: string;
  statisticsType?: string;
  itemKeyword?: string;
  checkType?: string;
  profitLossResult?: string;
  unitType?: string;
};

export type InventoryProfitLossReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  statisticsType: string;
  unit: string;
  checkDocumentNo: string;
  checkType: string;
  stockDocumentNo: string;
  orgName: string;
  orgCode: string;
  warehouse: string;
  checkTime: string;
  auditTime: string;
  auditor: string;
  bookQty: number;
  bookAmount: number;
  actualQty: number;
  actualAmount: number;
  profitLossQty: number;
  profitLossAmount: number;
  profitLossQtyAbs: number;
  profitLossAmountAbs: number;
  adjustmentAmount: number;
  profitLossResult: string;
  profitInboundPrice: number;
  lossOutboundPrice: number;
  checkReason: string;
  remark: string;
};

export const fetchInventoryProfitLossReportApi = (params: InventoryProfitLossReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<InventoryProfitLossReportRow>>('/api/inventory/inventory-profit-loss/report', {
    params: withOrgParams(params, orgId),
  });

export type InventoryInoutSummaryReportParams = {
  pageNo: number;
  pageSize: number;
  statisticDimension?: string;
  warehouse?: string;
  warehouseType?: string;
  startDate?: string;
  endDate?: string;
  itemCode?: string;
  itemCategory?: string;
  statisticType?: string;
  itemStatus?: string;
  inoutType?: string;
  unitType?: string;
  hideNoInout?: boolean | string;
  queryScheme?: string;
};

export type InventoryInoutSummaryReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  statisticType: string;
  unit: string;
  orgName: string;
  orgCode: string;
  warehouse: string;
  warehouseType: string;
  openingQty: number;
  openingCostAmountExTax: number;
  openingAvgCostExTax: number;
  inboundQty: number;
  inboundCostAmountExTax: number;
  inboundAvgCostExTax: number;
  outboundQty: number;
  outboundCostAmountExTax: number;
  outboundAvgCostExTax: number;
  closingQty: number;
  closingCostAmountExTax: number;
  closingAvgCostExTax: number;
  inventoryProfitLossQty: number;
  inventoryProfitLossCostAmountTaxIncluded: number;
  inventoryProfitLossCostAmountExTax: number;
  inventoryCheckQty: number;
  inventoryCheckCostAmountTaxIncluded: number;
  closingCheckDiffQty: number;
  closingCheckDiffAmountExTax: number;
  returnDifferenceQty: number;
  returnDifferenceCostAmountExTax: number;
  inoutType: string;
};

export const fetchInventoryInoutSummaryReportApi = (params: InventoryInoutSummaryReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<InventoryInoutSummaryReportRow>>('/api/inventory/inventory-inout-summary/report', {
    params: withOrgParams(params, orgId),
  });

export type StockInoutSummaryReportParams = {
  pageNo: number;
  pageSize: number;
  statisticMode?: string;
  dateDimension?: string;
  startDate?: string;
  endDate?: string;
  statisticDimension?: string;
  warehouse?: string;
  warehouseType?: string;
  targetStore?: string;
  itemKeyword?: string;
  itemCategory?: string;
  statisticType?: string;
  itemStatus?: string;
  inoutType?: string;
  inoutDirection?: string;
  oppositeOrg?: string;
  unitType?: string;
  queryScheme?: string;
};

export type StockInoutSummaryReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  statisticType: string;
  unit: string;
  inoutType: string;
  warehouse: string;
  warehouseType: string;
  oppositeOrg: string;
  oppositeWarehouse: string;
  inboundQty: number;
  inboundCostAmountTaxIncluded: number;
  inboundAvgCostTaxIncluded: number;
  inboundSettlementAmountTaxIncluded: number;
  inboundAvgSettlementTaxIncluded: number;
  outboundQty: number;
  outboundCostAmountTaxIncluded: number;
  outboundAvgCostTaxIncluded: number;
  outboundSettlementAmountTaxIncluded: number;
  outboundAvgSettlementTaxIncluded: number;
  statisticMode: string;
};

export const fetchStockInoutSummaryReportApi = (params: StockInoutSummaryReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<StockInoutSummaryReportRow>>('/api/inventory/stock-inout-summary/report', {
    params: withOrgParams(params, orgId),
  });

export type OtherInoutSummaryReportParams = {
  pageNo: number;
  pageSize: number;
  startDate?: string;
  endDate?: string;
  warehouse?: string;
  itemCategory?: string;
  itemCode?: string;
  inoutType?: string;
  reasonType?: string;
  itemStatus?: string;
};

export type OtherInoutSummaryReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  specModel: string;
  itemCategory: string;
  baseUnit: string;
  warehouse: string;
  inoutType: string;
  reasonType: string;
  quantity: number;
  amountExTax: number;
};

export const fetchOtherInoutSummaryReportApi = (params: OtherInoutSummaryReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<OtherInoutSummaryReportRow>>('/api/inventory/other-inout-summary/report', {
    params: withOrgParams(params, orgId),
  });

export type InterOrgTransferDetailReportParams = {
  pageNo: number;
  pageSize: number;
  statisticMode?: string;
  dateType?: string;
  startDate?: string;
  endDate?: string;
  targetStore?: string;
  sourceStore?: string;
  itemName?: string;
  itemCategory?: string;
  sourceWarehouse?: string;
  targetWarehouse?: string;
  documentStatus?: string;
};

export type InterOrgTransferDetailReportRow = {
  id: string;
  transferNo: string;
  itemCode: string;
  itemName: string;
  documentStatus: string;
  transferDate: string;
  outboundAuditTime: string;
  sourceStore: string;
  sourceWarehouse: string;
  inboundDate: string;
  inboundAuditTime: string;
  targetStore: string;
  targetWarehouse: string;
  specModel: string;
  itemCategory: string;
  baseUnit: string;
  transferBaseQty: number;
  businessUnit: string;
  transferQty: number;
  inboundAmountTaxIncluded: number;
  outboundCostAmountExTax: number;
  outboundSettlementAmountTaxIncluded: number;
  inboundPriceTaxIncluded: number;
  outboundCostPriceExTax: number;
  outboundSettlementPriceTaxIncluded: number;
  remark: string;
  statisticMode: string;
};

export const fetchInterOrgTransferDetailReportApi = (params: InterOrgTransferDetailReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<InterOrgTransferDetailReportRow>>('/api/inventory/inter-org-transfer-detail/report', {
    params: withOrgParams(params, orgId),
  });

export type InterOrgTransferSummaryReportParams = {
  pageNo: number;
  pageSize: number;
  statisticMode?: string;
  dateType?: string;
  startDate?: string;
  endDate?: string;
  statisticDimension?: string;
  targetStore?: string;
  itemKeyword?: string;
  itemCategory?: string;
  unitType?: string;
  queryScheme?: string;
};

export type InterOrgTransferSummaryReportRow = {
  id: string;
  itemCode: string;
  itemName: string;
  sourceStore: string;
  targetStore: string;
  specModel: string;
  itemCategory: string;
  unit: string;
  transferQty: number;
  inboundAmountTaxIncluded: number;
  outboundCostAmountExTax: number;
  outboundSettlementAmountTaxIncluded: number;
  inboundAvgPriceTaxIncluded: number;
  outboundCostAvgPriceExTax: number;
  outboundSettlementAvgPriceTaxIncluded: number;
};

export const fetchInterOrgTransferSummaryReportApi = (params: InterOrgTransferSummaryReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<InterOrgTransferSummaryReportRow>>('/api/inventory/inter-org-transfer-summary/report', {
    params: withOrgParams(params, orgId),
  });

export type StockTurnoverRateReportParams = {
  pageNo: number;
  pageSize: number;
  statisticDimension?: string;
  statisticMethod?: string;
  startDate?: string;
  endDate?: string;
  warehouse?: string;
  itemCategory?: string;
  itemCode?: string;
  itemStatus?: string;
  unitType?: string;
};

export type StockTurnoverRateReportRow = {
  id: string;
  orgName: string;
  warehouse: string;
  itemName: string;
  itemCode: string;
  unit: string;
  itemCategory: string;
  itemStatus: string;
  openingAmount: number;
  closingAmount: number;
  avgStockAmount: number;
  outboundAmount: number;
  turnoverRate: number;
  turnoverDays: number;
};

export const fetchStockTurnoverRateReportApi = (params: StockTurnoverRateReportParams, orgId?: string) =>
  apiClient.get<InventoryReportPage<StockTurnoverRateReportRow>>('/api/inventory/stock-turnover-rate/report', {
    params: withOrgParams(params, orgId),
  });

export type InventoryCheckRow = {
  id: number;
  documentCode: string;
  checkDate: string;
  warehouseName: string;
  itemCount: number;
  totalBookAmount: string;
  totalActualAmount: string;
  totalDiffAmount: string;
  checkRangeType: string;
  status: string;
  diffStatus: string;
  auditDate: string;
  printStatus: string;
  generatedStatus: string;
  createdAt: string;
  creator: string;
  remark: string;
};

export type InventoryCheckListParams = {
  pageNum: number;
  pageSize: number;
  timeType?: string;
  startDate?: string;
  endDate?: string;
  warehouse?: string;
  documentCode?: string;
  itemName?: string;
  status?: string;
  checkRangeType?: string;
  printStatus?: string;
  generatedStatus?: string;
  remark?: string;
};

export type InventoryCheckPage = {
  list: InventoryCheckRow[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export type InventoryCheckLinePayload = {
  itemCode: string;
  itemName: string;
  spec?: string;
  category?: string;
  unitName?: string;
  availableQty?: number | null;
  bookQty?: number | null;
  actualQty?: number | null;
  bookPrice?: number | null;
  profitLossReason?: string;
  remark?: string;
  extraFields?: Record<string, string>;
};

export type InventoryCheckSavePayload = {
  checkDate: string;
  warehouseName: string;
  checkRangeType: string;
  freezeStock?: boolean;
  collaborativeFlag?: boolean;
  planName?: string;
  thirdPartyDocument?: string;
  salesmanUserId?: number;
  salesmanName?: string;
  remark?: string;
  submitted?: boolean;
  items: InventoryCheckLinePayload[];
};

export type InventoryCheckDetail = {
  id: number;
  documentCode: string;
  status: string;
  checkDate: string;
  warehouseName: string;
  checkRangeType: string;
  freezeStock: boolean;
  collaborativeFlag: boolean;
  planName: string;
  thirdPartyDocument: string;
  salesmanUserId: number | null;
  salesmanName: string;
  remark: string;
  rejectionReason: string;
  generatedStatus: string;
  printStatus: string;
  extraFields: Record<string, string>;
  items: Array<{
    itemCode: string;
    itemName: string;
    spec: string;
    category: string;
    unitName: string;
    availableQty: number | null;
    bookQty: number | null;
    actualQty: number | null;
    bookPrice: number | null;
    bookAmount: number | null;
    actualAmount: number | null;
    diffQty: number | null;
    diffAmount: number | null;
    profitQty: number | null;
    lossQty: number | null;
    profitLossReason: string;
    profitInboundPrice: number | null;
    profitAmount: number | null;
    lossOutboundPrice: number | null;
    lossAmount: number | null;
    abnormalFlag: string;
    remark: string;
    extraFields: Record<string, string>;
  }>;
};

export type InventoryCheckPermission = {
  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  canApprove: boolean;
  canUnapprove: boolean;
};

const inventoryCheckBasePath = (documentType: 'inventory-checks' | 'multi-inventory-checks') => `/api/inventory/checks/${documentType}`;

export const fetchInventoryCheckPageApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  params: InventoryCheckListParams,
  orgId?: string,
) => apiClient.get<InventoryCheckPage>(inventoryCheckBasePath(documentType), {
  params: withOrgParams(params, orgId),
});

export const fetchInventoryCheckPermissionApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  orgId?: string,
) => apiClient.get<InventoryCheckPermission>(`${inventoryCheckBasePath(documentType)}/permissions`, {
  params: withOrgParams(undefined, orgId),
});

export const createInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  payload: InventoryCheckSavePayload,
  orgId?: string,
) => apiClient.post<{ id: number; documentCode: string }>(inventoryCheckBasePath(documentType), payload, {
  params: withOrgParams(undefined, orgId),
});

export const fetchInventoryCheckDetailApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  id: number,
  orgId?: string,
) => apiClient.get<InventoryCheckDetail>(`${inventoryCheckBasePath(documentType)}/${id}`, {
  params: withOrgParams(undefined, orgId),
});

export const updateInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  id: number,
  payload: InventoryCheckSavePayload,
  orgId?: string,
) => apiClient.put<void>(`${inventoryCheckBasePath(documentType)}/${id}`, payload, {
  params: withOrgParams(undefined, orgId),
});

export const deleteInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  id: number,
  orgId?: string,
) => apiClient.delete<void>(`${inventoryCheckBasePath(documentType)}/${id}`, {
  params: withOrgParams(undefined, orgId),
});

export const batchDeleteInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  ids: number[],
  orgId?: string,
) => apiClient.delete<void>(inventoryCheckBasePath(documentType), {
  params: withOrgParams(undefined, orgId),
  data: { ids },
});

export const batchSubmitInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  ids: number[],
  orgId?: string,
) => apiClient.post<void>(`${inventoryCheckBasePath(documentType)}/batch-submit`, { ids }, {
  params: withOrgParams(undefined, orgId),
});

export const batchApproveInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  ids: number[],
  orgId?: string,
) => apiClient.post<void>(`${inventoryCheckBasePath(documentType)}/batch-approve`, { ids }, {
  params: withOrgParams(undefined, orgId),
});

export const batchUnapproveInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  ids: number[],
  rejectionReason: string,
  orgId?: string,
) => apiClient.post<void>(`${inventoryCheckBasePath(documentType)}/batch-unapprove`, { ids, rejectionReason }, {
  params: withOrgParams(undefined, orgId),
});

export const batchPrintInventoryCheckApi = (
  documentType: 'inventory-checks' | 'multi-inventory-checks',
  ids: number[],
  orgId?: string,
) => apiClient.post<void>(`${inventoryCheckBasePath(documentType)}/batch-print`, { ids }, {
  params: withOrgParams(undefined, orgId),
});

export const generateMultiInventoryCheckApi = (ids: number[], orgId?: string) =>
  apiClient.post<void>('/api/inventory/multi-inventory-checks/generate', { ids }, {
    params: withOrgParams(undefined, orgId),
  });

