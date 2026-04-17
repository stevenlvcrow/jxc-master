import { apiClient } from '@/api/http-client';
import type { ApiPage } from '@/api/types';

export type SupplierStatus = '启用' | '停用';
export type SupplierBindStatus = '已绑定' | '未绑定';
export type SupplierSource = '集团' | '门店';
export type SupplierSupplyRelation = '有' | '无';

export type SupplierListRow = {
  id: number;
  index: number;
  supplierCode: string;
  supplierName: string;
  supplierCategory: string;
  contactPerson: string | null;
  contactPhone: string | null;
  settlementMethod: string | null;
  source: SupplierSource;
  supplyRelation: SupplierSupplyRelation;
  remark: string | null;
  status: SupplierStatus;
  operatedAt: string | null;
};

export type SupplierListParams = {
  pageNo: number;
  pageSize: number;
  supplierInfo?: string;
  status?: '启用' | '停用';
  bindStatus?: SupplierBindStatus;
  source?: SupplierSource;
  supplyRelation?: SupplierSupplyRelation;
  treeNode?: string;
};

export type SupplierCategoryTreeNode = {
  id: string;
  label: string;
  children?: SupplierCategoryTreeNode[];
};

export type SupplierCategoryCreatePayload = {
  categoryCode?: string;
  categoryName: string;
  parentCategory: string;
};

export type SupplierQualificationPayload = {
  fileName?: string;
  fileUrl?: string;
  qualificationType: string;
  validTo?: string;
  status?: string;
  remark?: string;
};

export type SupplierContractPayload = {
  attachmentName?: string;
  attachmentUrl?: string;
  contractName?: string;
  contractCode?: string;
  validTo?: string;
  status?: string;
};

export type SupplierFinancePayload = {
  bankAccount: string;
  accountName: string;
  bankName: string;
  defaultAccount: boolean;
};

export type CreateSupplierPayload = {
  supplierCode?: string;
  supplierName: string;
  supplierShortName?: string;
  supplierMnemonic?: string;
  supplierCategory: string;
  taxRate: number;
  status: SupplierStatus;
  contactPerson?: string;
  contactPhone?: string;
  email?: string;
  contactAddress?: string;
  remark?: string;
  settlementMethod: '预付款' | '货到付款' | '日结' | '月结';
  orderSummaryRule: '按机构' | '按仓库';
  inputBatchWhenDelivery: boolean;
  syncReceiptData: boolean;
  purchaseReceiptDependShipping: '不依赖' | '依赖';
  deliveryDependShipping: '不依赖' | '依赖';
  supplierManageInventory: boolean;
  controlOrderTime: boolean;
  allowCloseOrder: boolean;
  reconciliationMode: string;
  scopeControl: '开启' | '关闭';
  qualificationList: SupplierQualificationPayload[];
  contractList: SupplierContractPayload[];
  financeList: SupplierFinancePayload[];
  invoiceCompanyName?: string;
  taxpayerId?: string;
  invoicePhone?: string;
  invoiceAddress?: string;
};

export type SupplierDetailPayload = CreateSupplierPayload & {
  id: number;
};

const withOrgParams = <T extends Record<string, unknown>>(params?: T, orgId?: string) => ({
  ...(params ?? {}),
  ...(orgId ? { orgId } : {}),
});

export const fetchSuppliersApi = (params: SupplierListParams, orgId?: string) =>
  apiClient.get<ApiPage<SupplierListRow>>('/api/items/suppliers', {
    params: withOrgParams(params, orgId),
  });

export const createSupplierApi = (payload: CreateSupplierPayload, orgId?: string) =>
  apiClient.post<{ id: number }>('/api/items/suppliers', payload, {
    params: withOrgParams(undefined, orgId),
  });

export const fetchSupplierDetailApi = (id: number, orgId?: string) =>
  apiClient.get<SupplierDetailPayload>(`/api/items/suppliers/${id}`, {
    params: withOrgParams(undefined, orgId),
  });

export const updateSupplierApi = (id: number, payload: CreateSupplierPayload, orgId?: string) =>
  apiClient.put<{ id: number }>(`/api/items/suppliers/${id}`, payload, {
    params: withOrgParams(undefined, orgId),
  });

export const fetchSupplierCategoryTreeApi = (orgId?: string) =>
  apiClient.get<SupplierCategoryTreeNode[]>('/api/items/supplier-categories/tree', {
    params: withOrgParams(undefined, orgId),
  });

export const createSupplierCategoryApi = (payload: SupplierCategoryCreatePayload, orgId?: string) =>
  apiClient.post<{ id: number }>('/api/items/supplier-categories', payload, {
    params: withOrgParams(undefined, orgId),
  });
