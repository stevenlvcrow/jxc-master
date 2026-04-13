import { apiClient } from '@/api/http-client';
import type { ApiPage } from '@/api/types';

export type ItemStatus = '启用' | '停用';

export type ItemVO = {
  id: string;
  index: number;
  code: string;
  name: string;
  spec: string;
  type: string;
  category: string;
  brand: string;
  productionCost: string;
  baseUnit: string;
  purchaseUnit: string;
  orderUnit: string;
  stockUnit: string;
  costUnit: string;
  suggestPrice: string;
  volume: string;
  weight: string;
  statType: string;
  thirdPartyCode: string;
  abcClass: string;
  stockMin: string;
  stockMax: string;
  source: string;
  status: ItemStatus;
  storageMode: string;
  tag: string;
  image: string;
  createdAt: string;
  updatedAt: string;
};

export type ItemListParams = {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  category?: string;
  status?: string;
  itemType?: string;
  statType?: string;
  storageMode?: string;
  tag?: string;
};

export const fetchItemsApi = async (params: ItemListParams, orgId?: string) => {
  return apiClient.get<ApiPage<ItemVO>>('/api/items', {
    params: withOrgParams(params, orgId),
  });
};

export const batchUpdateItemStatusApi = async (ids: string[], status: ItemStatus, orgId?: string) => {
  return apiClient.post<void>('/api/items/batch-status', {
    ids,
    status,
  }, { params: withOrgParams(undefined, orgId) });
};

export const batchDeleteItemsApi = async (ids: string[], orgId?: string) => {
  return apiClient.post<void>('/api/items/batch-delete', {
    ids,
  }, { params: withOrgParams(undefined, orgId) });
};

export type ItemCreatePayload = {
  name: string;
  code: string;
  category: string;
  spec?: string;
  status: string;
  brand?: string;
  mnemonicCode?: string;
  barcode?: string;
  thirdPartyCode?: string;
  defaultPurchaseUnit?: string;
  defaultOrderUnit?: string;
  defaultStockUnit?: string;
  defaultCostUnit?: string;
  stocktakeUnits?: string[];
  assistUnitEnabled?: boolean;
  productionRefCost?: string;
  monthEndUpdate?: boolean;
  suggestPurchasePrice?: string;
  storageMode: string;
  statType?: string;
  stockMin?: string;
  stockMax?: string;
  safeStock?: string;
  taxCode?: string;
  taxName?: string;
  taxRate?: string;
  taxBenefit?: string;
  consumeOnInbound?: string;
  disableStocktake?: string;
  defaultNoStocktake?: string;
  stocktakeTypes?: string[];
  purchaseReceiptRule?: string;
  purchaseRuleMaxRatio?: string;
  purchaseRuleMinRatio?: string;
  purchaseRuleBetweenStart?: string;
  purchaseRuleBetweenEnd?: string;
  requireAssemblyProcess?: boolean;
  requireSplitProcess?: boolean;
  requireBatchReport?: boolean;
  allowLossReport?: boolean;
  allowTransfer?: boolean;
  enablePrepare?: boolean;
  productCategory?: string;
  netContent?: string;
  ingredients?: string;
  itemDescription?: string;
  remark?: string;
  alias?: string;
  abcClass?: string;
  batchManagement?: boolean;
  shelfLifeEnabled?: boolean;
  shelfLifeDays?: number;
  warningDays?: number;
  stagnantDays?: number;
  tag?: string;
  unitSettingRows?: Array<{
    unit?: string;
    convertFrom?: string;
    convertTo?: string;
    volume?: string;
    volumeUnit?: string;
    weight?: string;
    weightUnit?: string;
    barcode?: string;
  }>;
  supplierRelationRows?: Array<{
    key?: number;
    supplier?: string;
    contact?: string;
    phone?: string;
  }>;
  defaultSupplierRowKey?: number;
  introImages?: Array<{
    id?: number;
    name?: string;
    url?: string;
  }>;
  nutritionHeaders?: {
    item?: string;
    per100g?: string;
    nrv?: string;
  };
  nutritionRows?: Array<{
    id?: number;
    item?: string;
    per100g?: string;
    nrv?: string;
  }>;
  extensionInfoRows?: Array<{
    id?: number;
    name?: string;
    value?: string;
  }>;
};

export const createItemApi = (payload: ItemCreatePayload, orgId?: string) =>
  apiClient.post<{ id: string }>('/api/items', payload, { params: withOrgParams(undefined, orgId) });

export const saveItemDraftApi = (payload: ItemCreatePayload, orgId?: string) =>
  apiClient.post<{ id: string }>('/api/items/drafts', payload, { params: withOrgParams(undefined, orgId) });

export const fetchItemDetailApi = (id: string, orgId?: string) =>
  apiClient.get<ItemCreatePayload>(`/api/items/${id}`, { params: withOrgParams(undefined, orgId) });

export const updateItemApi = (id: string, payload: ItemCreatePayload, orgId?: string) =>
  apiClient.put<void>(`/api/items/${id}`, payload, { params: withOrgParams(undefined, orgId) });

export type ItemCategoryStatus = '启用' | '停用';

export type ItemCategoryTreeNode = {
  label: string;
  children?: ItemCategoryTreeNode[];
};

export type ItemCategoryVO = {
  id: number;
  index: number;
  categoryCode: string;
  categoryName: string;
  parentCategory: string;
  status: ItemCategoryStatus;
  createdAt: string;
  remark: string | null;
};

export type ItemCategoryPage = {
  list: ItemCategoryVO[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type ItemCategoryListParams = {
  pageNo: number;
  pageSize: number;
  categoryInfo?: string;
  status?: '启用' | '停用';
  treeNode?: string;
  sortBy?: 'parentCategory';
};

export type ItemCategoryUpsertPayload = {
  categoryCode: string;
  categoryName: string;
  parentCategory: string;
  status: ItemCategoryStatus;
  remark?: string;
};

export type ItemCategoryBatchCreatePayload = {
  parentCategory: string;
  status: ItemCategoryStatus;
  items: Array<{
    categoryCode: string;
    categoryName: string;
    remark?: string;
  }>;
};

const withOrgParams = <T extends Record<string, unknown>>(params?: T, orgId?: string) => ({
  ...(params ?? {}),
  ...(orgId ? { orgId } : {}),
});

export const fetchItemCategoriesApi = (params: ItemCategoryListParams, orgId?: string) =>
  apiClient.get<ItemCategoryPage>('/api/items/categories', { params: withOrgParams(params, orgId) });

export const fetchItemCategoryTreeApi = (orgId?: string) =>
  apiClient.get<ItemCategoryTreeNode[]>('/api/items/categories/tree', { params: withOrgParams(undefined, orgId) });

export const createItemCategoryApi = (payload: ItemCategoryUpsertPayload, orgId?: string) =>
  apiClient.post<{ id: number }>('/api/items/categories', payload, { params: withOrgParams(undefined, orgId) });

export const batchCreateItemCategoriesApi = (payload: ItemCategoryBatchCreatePayload, orgId?: string) =>
  apiClient.post<{ createdCount: number }>('/api/items/categories/batch', payload, { params: withOrgParams(undefined, orgId) });

export const updateItemCategoryApi = (id: number, payload: ItemCategoryUpsertPayload, orgId?: string) =>
  apiClient.put<void>(`/api/items/categories/${id}`, payload, { params: withOrgParams(undefined, orgId) });

export const updateItemCategoryStatusApi = (id: number, status: ItemCategoryStatus, orgId?: string) =>
  apiClient.put<void>(`/api/items/categories/${id}/status`, { status }, { params: withOrgParams(undefined, orgId) });

export const deleteItemCategoryApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/items/categories/${id}`, { params: withOrgParams(undefined, orgId) });

export const batchDeleteItemCategoriesApi = (ids: number[], orgId?: string) =>
  apiClient.post<void>('/api/items/categories/batch-delete', { ids }, { params: withOrgParams(undefined, orgId) });

export type ItemStatisticsTypeRow = {
  id: number;
  index: number;
  code: string;
  name: string;
  statisticsCategory: string;
  createType: string;
  modifiedTime: string;
};

export type ItemStatisticsTypePage = {
  list: ItemStatisticsTypeRow[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type ItemStatisticsTypeListParams = {
  pageNo: number;
  pageSize: number;
  keyword?: string;
};

export type ItemStatisticsTypeDetail = {
  id: number;
  code: string;
  name: string;
  statisticsCategory: string;
  createType: string;
  modifiedTime: string;
  createdAt: string;
  updatedAt: string;
};

export type ItemStatisticsTypeCreatePayload = {
  name: string;
  statisticsCategory: string;
};

export type ItemStatisticsTypeCreateResult = {
  id: number;
  code: string;
};

export type ItemStatisticsTypeExportRow = {
  id: number;
  code: string;
  name: string;
  statisticsCategory: string;
  createType: string;
  modifiedTime: string;
};

export type ItemStatisticsTypeExportResult = {
  fileName: string;
  rows: ItemStatisticsTypeExportRow[];
};

export const fetchItemStatisticsTypesApi = (params: ItemStatisticsTypeListParams, orgId?: string) =>
  apiClient.get<ItemStatisticsTypePage>('/api/items/statistics-types', { params: withOrgParams(params, orgId) });

export const fetchItemStatisticsTypeDetailApi = (id: number, orgId?: string) =>
  apiClient.get<ItemStatisticsTypeDetail>(`/api/items/statistics-types/${id}`, { params: withOrgParams(undefined, orgId) });

export const createItemStatisticsTypeApi = (payload: ItemStatisticsTypeCreatePayload, orgId?: string) =>
  apiClient.post<ItemStatisticsTypeCreateResult>('/api/items/statistics-types', payload, { params: withOrgParams(undefined, orgId) });

export const exportItemStatisticsTypesApi = (ids?: number[], orgId?: string) =>
  apiClient.post<ItemStatisticsTypeExportResult>(
    '/api/items/statistics-types/batch-export',
    { ids: ids ?? [] },
    { params: withOrgParams(undefined, orgId) },
  );

export type ItemTagRow = {
  id: number;
  index: number;
  tagCode: string;
  tagName: string;
  itemName: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ItemTagPage = {
  list: ItemTagRow[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type ItemTagListParams = {
  pageNo: number;
  pageSize: number;
  tagCode?: string;
  tagName?: string;
  itemName?: string;
};

export type ItemTagUpsertPayload = {
  tagCode: string;
  tagName: string;
  itemName?: string;
};

export type ItemTagBatchImportPayload = {
  items: Array<{
    tagCode: string;
    tagName: string;
    itemName?: string;
  }>;
};

export type ItemTagBatchImportResult = {
  totalCount: number;
  insertedCount: number;
  skippedCount: number;
};

export const fetchItemTagsApi = (params: ItemTagListParams, orgId?: string) =>
  apiClient.get<ItemTagPage>('/api/items/tags', { params: withOrgParams(params, orgId) });

export const createItemTagApi = (payload: ItemTagUpsertPayload, orgId?: string) =>
  apiClient.post<{ id: number }>('/api/items/tags', payload, { params: withOrgParams(undefined, orgId) });

export const updateItemTagApi = (id: number, payload: ItemTagUpsertPayload, orgId?: string) =>
  apiClient.put<void>(`/api/items/tags/${id}`, payload, { params: withOrgParams(undefined, orgId) });

export const deleteItemTagApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/items/tags/${id}`, { params: withOrgParams(undefined, orgId) });

export const batchImportItemTagsApi = (payload: ItemTagBatchImportPayload, orgId?: string) =>
  apiClient.post<ItemTagBatchImportResult>('/api/items/tags/batch-import', payload, { params: withOrgParams(undefined, orgId) });

