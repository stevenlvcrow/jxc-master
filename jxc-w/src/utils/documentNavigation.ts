import { ElMessage } from 'element-plus';
import type { LocationQueryRaw, Router } from 'vue-router';

type DocumentRouteConfig = {
  path: string;
  queryKey: string;
};

export type DocumentListRoute = {
  path: string;
  query: LocationQueryRaw;
};

const DOCUMENT_ROUTE_BY_PREFIX: Record<string, DocumentRouteConfig> = {
  PA: { path: '/purchase/applications', queryKey: 'applicationCode' },
  PO: { path: '/purchase/orders', queryKey: 'documentCode' },
  PRC: { path: '/purchase/receipts', queryKey: 'documentCode' },
  PRT: { path: '/purchase/returns', queryKey: 'documentCode' },
  RK: { path: '/inventory/purchase-inbounds', queryKey: 'documentCode' },
  PURK: { path: '/inventory/purchase-inbounds', queryKey: 'documentCode' },
  THCK: { path: '/inventory/purchase-return-outbounds', queryKey: 'documentCode' },
  BM: { path: '/inventory/department-pickings', queryKey: 'documentCode' },
  BT: { path: '/inventory/department-returns', queryKey: 'documentCode' },
  YK: { path: '/inventory/stock-transfers', queryKey: 'documentCode' },
  YKRK: { path: '/inventory/stock-transfer-inbounds', queryKey: 'documentCode' },
  BMDB: { path: '/inventory/department-transfers', queryKey: 'documentCode' },
  BSCK: { path: '/inventory/damage-outbounds', queryKey: 'documentCode' },
  QTRK: { path: '/inventory/other-inbounds', queryKey: 'documentCode' },
  QTCK: { path: '/inventory/other-outbounds', queryKey: 'documentCode' },
  SCRK: { path: '/inventory/production-inbounds', queryKey: 'documentCode' },
  XSCK: { path: '/inventory/customer-sales-outbounds', queryKey: 'documentCode' },
  KHTH: { path: '/inventory/customer-return-inbounds', queryKey: 'documentCode' },
  CPC: { path: '/inventory/dish-consumption-outbounds', queryKey: 'documentCode' },
  QC: { path: '/inventory/period-openings', queryKey: 'documentCode' },
  DJDB: { path: '/inventory/store-transfers', queryKey: 'documentCode' },
  YKCK: { path: '/inventory/stock-transfer-outbounds', queryKey: 'documentCode' },
  PD: { path: '/inventory/inventory-checks', queryKey: 'documentCode' },
  MPD: { path: '/inventory/multi-inventory-checks', queryKey: 'documentCode' },
  SO: { path: '/order/order-documents', queryKey: 'documentCode' },
  TP: { path: '/order/third-party-transfer-documents', queryKey: 'documentCode' },
};

const DOCUMENT_ROUTE_BY_TYPE: Record<string, DocumentRouteConfig> = {
  采购单申请: { path: '/purchase/applications', queryKey: 'applicationCode' },
  采购申请: { path: '/purchase/applications', queryKey: 'applicationCode' },
  采购订单: { path: '/purchase/orders', queryKey: 'documentCode' },
  采购收货: { path: '/purchase/receipts', queryKey: 'documentCode' },
  采购收货单: { path: '/purchase/receipts', queryKey: 'documentCode' },
  采购退货: { path: '/purchase/returns', queryKey: 'documentCode' },
  采购退货单: { path: '/purchase/returns', queryKey: 'documentCode' },
  采购入库: { path: '/inventory/purchase-inbounds', queryKey: 'documentCode' },
  采购退货出库: { path: '/inventory/purchase-return-outbounds', queryKey: 'documentCode' },
  部门领料: { path: '/inventory/department-pickings', queryKey: 'documentCode' },
  部门退料: { path: '/inventory/department-returns', queryKey: 'documentCode' },
  移库单: { path: '/inventory/stock-transfers', queryKey: 'documentCode' },
  移库入库: { path: '/inventory/stock-transfer-inbounds', queryKey: 'documentCode' },
  部门调拨: { path: '/inventory/department-transfers', queryKey: 'documentCode' },
  报损出库: { path: '/inventory/damage-outbounds', queryKey: 'documentCode' },
  其他入库: { path: '/inventory/other-inbounds', queryKey: 'documentCode' },
  其他出库: { path: '/inventory/other-outbounds', queryKey: 'documentCode' },
  生产入库: { path: '/inventory/production-inbounds', queryKey: 'documentCode' },
  客户销售出库: { path: '/inventory/customer-sales-outbounds', queryKey: 'documentCode' },
  客户退货入库: { path: '/inventory/customer-return-inbounds', queryKey: 'documentCode' },
  菜品消耗出库: { path: '/inventory/dish-consumption-outbounds', queryKey: 'documentCode' },
  期初库存: { path: '/inventory/period-openings', queryKey: 'documentCode' },
  店间调拨: { path: '/inventory/store-transfers', queryKey: 'documentCode' },
  移库出库: { path: '/inventory/stock-transfer-outbounds', queryKey: 'documentCode' },
  盘点单: { path: '/inventory/inventory-checks', queryKey: 'documentCode' },
  多人盘点单: { path: '/inventory/multi-inventory-checks', queryKey: 'documentCode' },
  销售订单: { path: '/order/order-documents', queryKey: 'documentCode' },
  三方调拨单: { path: '/order/third-party-transfer-documents', queryKey: 'documentCode' },
};

export const normalizeDocumentCode = (value: unknown) => {
  const code = String(value ?? '').trim();
  if (!code || code === '-' || code === '--' || code === '无') {
    return '';
  }
  return code;
};

export const readQueryString = (value: unknown) => {
  const firstValue = Array.isArray(value) ? value[0] : value;
  return String(firstValue ?? '').trim();
};

const normalizeDocumentType = (value?: string | null) => String(value ?? '').replace(/\s/g, '');

const resolveRouteConfig = (documentCode: string, documentType?: string | null) => {
  const prefix = documentCode.includes('-')
    ? documentCode.slice(0, documentCode.indexOf('-')).toUpperCase()
    : '';
  const prefixConfig = prefix ? DOCUMENT_ROUTE_BY_PREFIX[prefix] : undefined;
  if (prefixConfig) {
    return prefixConfig;
  }
  const type = normalizeDocumentType(documentType);
  return type ? DOCUMENT_ROUTE_BY_TYPE[type] : undefined;
};

export const resolveDocumentListRouteByCode = (
  documentCodeValue: unknown,
  documentType?: string | null,
): DocumentListRoute | null => {
  const documentCode = normalizeDocumentCode(documentCodeValue);
  if (!documentCode) {
    return null;
  }
  const config = resolveRouteConfig(documentCode, documentType);
  if (!config) {
    return null;
  }
  return {
    path: config.path,
    query: {
      [config.queryKey]: documentCode,
    },
  };
};

export const pushDocumentListByCode = async (
  router: Router,
  documentCodeValue: unknown,
  documentType?: string | null,
) => {
  const route = resolveDocumentListRouteByCode(documentCodeValue, documentType);
  if (!route) {
    ElMessage.warning('无法识别上游单据类型');
    return;
  }
  await router.push(route);
};
