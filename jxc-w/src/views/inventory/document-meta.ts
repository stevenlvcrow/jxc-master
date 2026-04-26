import type { GenericInventoryDocumentRow, GenericInventoryDocumentType } from '@/api/modules/inventory';
import type { WarehouseType } from '@/api/modules/warehouse';

export type InventoryDocumentFieldKind = 'warehouse' | 'text' | 'select';

export type InventoryDocumentFieldMeta = {
  key: string;
  label: string;
  kind: InventoryDocumentFieldKind;
  options?: string[];
  warehouseTypes?: WarehouseType[];
};

export type InventoryDocumentQueryOption = {
  label: string;
  value: string;
};

export type InventoryDocumentListColumn = {
  key: string;
  label: string;
  prop?: keyof GenericInventoryDocumentRow;
  minWidth?: number;
  width?: number;
  fixed?: 'left' | 'right';
  type?: 'index' | 'operation';
  formatter?: (row: GenericInventoryDocumentRow, context: { warehouseCodeByName: Record<string, string> }) => string;
};

export type InventoryDocumentListQueryField =
  | 'dateRange'
  | 'documentCode'
  | 'primaryName'
  | 'itemName'
  | 'status'
  | 'remark';

export type InventoryDocumentMeta = {
  type: GenericInventoryDocumentType;
  title: string;
  listRouteName: string;
  createRouteName: string;
  viewRouteName: string;
  editRouteName: string;
  dateLabel: string;
  primaryField?: InventoryDocumentFieldMeta;
  secondaryField?: InventoryDocumentFieldMeta;
  counterpartyField?: InventoryDocumentFieldMeta;
  counterpartyField2?: InventoryDocumentFieldMeta;
  reasonField?: InventoryDocumentFieldMeta;
  extraFields?: InventoryDocumentFieldMeta[];
  showAvailableQty?: boolean;
  showAttachment?: boolean;
  workflowEnabled?: boolean;
  noticeLines?: string[];
  listTableHeight?: number;
  listPrimaryQueryKind?: 'input' | 'select';
  listStatusOptions?: InventoryDocumentQueryOption[];
  listStatusLabelMap?: Record<string, string>;
  showToolbar?: boolean;
  listQueryFields?: InventoryDocumentListQueryField[];
  listColumns?: InventoryDocumentListColumn[];
  showSummary?: boolean;
  summaryFields?: Array<keyof GenericInventoryDocumentRow>;
  summaryLabel?: string;
  showDocumentCode?: boolean;
  remarkInputType?: 'input';
  showUpstreamCode?: boolean;
  itemTableStyle?: 'default' | 'purchase-inbound' | 'purchase-return-outbound';
};

export const inventoryDocumentMetaMap: Record<string, InventoryDocumentMeta> = {
  purchaseReturnOutbound: {
    type: 'purchase-return-outbound',
    title: '采购退货出库',
    listRouteName: 'PurchaseReturnOutbound',
    createRouteName: 'PurchaseReturnOutboundCreate',
    viewRouteName: 'PurchaseReturnOutboundView',
    editRouteName: 'PurchaseReturnOutboundEdit',
    dateLabel: '出库日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    counterpartyField: { key: 'counterpartyName', label: '供应商', kind: 'select' },
    reasonField: { key: 'reason', label: '退货原因', kind: 'select', options: ['质量问题退货', '数量差异退货', '临期退货', '采购协商退货'] },
    showAvailableQty: true,
    showAttachment: true,
    itemTableStyle: 'purchase-return-outbound',
    remarkInputType: 'input',
  },
  departmentPicking: {
    type: 'department-picking',
    title: '部门领料',
    listRouteName: 'DepartmentPicking',
    createRouteName: 'DepartmentPickingCreate',
    viewRouteName: 'DepartmentPickingView',
    editRouteName: 'DepartmentPickingEdit',
    dateLabel: '领料日期',
    primaryField: { key: 'primaryName', label: '出库仓库', kind: 'warehouse', warehouseTypes: ['普通仓库'] },
    counterpartyField: {
      key: 'counterpartyName',
      label: '领料部门',
      kind: 'warehouse',
      warehouseTypes: ['行政部门', '出品及生产部门'],
    },
  },
  departmentReturn: {
    type: 'department-return',
    title: '部门退料',
    listRouteName: 'DepartmentReturn',
    createRouteName: 'DepartmentReturnCreate',
    viewRouteName: 'DepartmentReturnView',
    editRouteName: 'DepartmentReturnEdit',
    dateLabel: '退料日期',
    primaryField: { key: 'primaryName', label: '入库仓库', kind: 'warehouse', warehouseTypes: ['普通仓库'] },
    counterpartyField: {
      key: 'counterpartyName',
      label: '退料部门',
      kind: 'warehouse',
      warehouseTypes: ['行政部门', '出品及生产部门'],
    },
  },
  stockTransfer: {
    type: 'stock-transfer',
    title: '移库单',
    listRouteName: 'StockTransfer',
    createRouteName: 'StockTransferCreate',
    viewRouteName: 'StockTransferView',
    editRouteName: 'StockTransferEdit',
    dateLabel: '移库日期',
    primaryField: {
      key: 'primaryName',
      label: '调出仓库',
      kind: 'warehouse',
      warehouseTypes: ['行政部门', '出品及生产部门'],
    },
    secondaryField: {
      key: 'secondaryName',
      label: '调入仓库',
      kind: 'warehouse',
      warehouseTypes: ['行政部门', '出品及生产部门'],
    },
    reasonField: { key: 'reason', label: '移库类型', kind: 'select', options: ['直接移库', '退库移库', '紧急调拨'] },
  },
  stockTransferInbound: {
    type: 'stock-transfer-inbound',
    title: '移库入库',
    listRouteName: 'StockTransferInbound',
    createRouteName: 'StockTransferInboundCreate',
    viewRouteName: 'StockTransferInboundView',
    editRouteName: 'StockTransferInboundEdit',
    dateLabel: '入库日期',
    primaryField: { key: 'primaryName', label: '入库仓库', kind: 'warehouse' },
    secondaryField: { key: 'secondaryName', label: '出库仓库', kind: 'warehouse' },
    counterpartyField: { key: 'counterpartyName', label: '上游移库单号', kind: 'text' },
  },
  departmentTransfer: {
    type: 'department-transfer',
    title: '部门调拨',
    listRouteName: 'DepartmentTransfer',
    createRouteName: 'DepartmentTransferCreate',
    viewRouteName: 'DepartmentTransferView',
    editRouteName: 'DepartmentTransferEdit',
    dateLabel: '调拨日期',
    primaryField: {
      key: 'primaryName',
      label: '调出仓库',
      kind: 'warehouse',
      warehouseTypes: ['行政部门', '出品及生产部门'],
    },
    secondaryField: {
      key: 'secondaryName',
      label: '调入仓库',
      kind: 'warehouse',
      warehouseTypes: ['行政部门', '出品及生产部门'],
    },
  },
  damageOutbound: {
    type: 'damage-outbound',
    title: '报损出库',
    listRouteName: 'DamageOutbound',
    createRouteName: 'DamageOutboundCreate',
    viewRouteName: 'DamageOutboundView',
    editRouteName: 'DamageOutboundEdit',
    dateLabel: '出库日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
  },
  otherInbound: {
    type: 'other-inbound',
    title: '其他入库',
    listRouteName: 'OtherInbound',
    createRouteName: 'OtherInboundCreate',
    viewRouteName: 'OtherInboundView',
    editRouteName: 'OtherInboundEdit',
    dateLabel: '入库日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    reasonField: { key: 'reason', label: '入库原因', kind: 'select', options: ['盘盈入库', '调整入库', '赠品入库', '其他入库'] },
  },
  otherOutbound: {
    type: 'other-outbound',
    title: '其他出库',
    listRouteName: 'OtherOutbound',
    createRouteName: 'OtherOutboundCreate',
    viewRouteName: 'OtherOutboundView',
    editRouteName: 'OtherOutboundEdit',
    dateLabel: '出库日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    reasonField: { key: 'reason', label: '出库原因', kind: 'select', options: ['盘亏出库', '调整出库', '赠送出库', '其他出库'] },
    showAvailableQty: true,
  },
  profitInbound: {
    type: 'profit-inbound',
    title: '盘盈单',
    listRouteName: 'ProfitInbound',
    createRouteName: 'ProfitInboundCreate',
    viewRouteName: 'ProfitInboundView',
    editRouteName: 'ProfitInboundEdit',
    dateLabel: '单据日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    reasonField: { key: 'reason', label: '盘盈原因', kind: 'text' },
    showToolbar: false,
    showUpstreamCode: true,
    listQueryFields: ['dateRange', 'documentCode', 'primaryName', 'itemName', 'status', 'remark'],
    listColumns: [
      { key: 'index', label: '序号', type: 'index', width: 70 },
      { key: 'documentCode', label: '单据编号', prop: 'documentCode', minWidth: 150 },
      { key: 'documentDate', label: '单据日期', prop: 'documentDate', minWidth: 110 },
      { key: 'primaryName', label: '仓库', prop: 'primaryName', minWidth: 140 },
      { key: 'status', label: '单据状态', prop: 'status', minWidth: 100 },
      { key: 'reviewStatus', label: '审核状态', prop: 'reviewStatus', minWidth: 100 },
      { key: 'amount', label: '金额', prop: 'amount', minWidth: 100 },
      { key: 'createdAt', label: '创建时间', prop: 'createdAt', minWidth: 160 },
      { key: 'creator', label: '创建人', prop: 'creator', minWidth: 100 },
      { key: 'remark', label: '备注', prop: 'remark', minWidth: 180 },
      { key: 'operation', label: '操作', type: 'operation', width: 100, fixed: 'right' },
    ],
  },
  lossOutbound: {
    type: 'loss-outbound',
    title: '盘亏单',
    listRouteName: 'LossOutbound',
    createRouteName: 'LossOutboundCreate',
    viewRouteName: 'LossOutboundView',
    editRouteName: 'LossOutboundEdit',
    dateLabel: '单据日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    reasonField: { key: 'reason', label: '盘亏原因', kind: 'text' },
    showAvailableQty: true,
    showToolbar: false,
    showUpstreamCode: true,
    listQueryFields: ['dateRange', 'documentCode', 'primaryName', 'itemName', 'status', 'remark'],
    listColumns: [
      { key: 'index', label: '序号', type: 'index', width: 70 },
      { key: 'documentCode', label: '单据编号', prop: 'documentCode', minWidth: 150 },
      { key: 'documentDate', label: '单据日期', prop: 'documentDate', minWidth: 110 },
      { key: 'primaryName', label: '仓库', prop: 'primaryName', minWidth: 140 },
      { key: 'status', label: '单据状态', prop: 'status', minWidth: 100 },
      { key: 'reviewStatus', label: '审核状态', prop: 'reviewStatus', minWidth: 100 },
      { key: 'amount', label: '金额', prop: 'amount', minWidth: 100 },
      { key: 'createdAt', label: '创建时间', prop: 'createdAt', minWidth: 160 },
      { key: 'creator', label: '创建人', prop: 'creator', minWidth: 100 },
      { key: 'remark', label: '备注', prop: 'remark', minWidth: 180 },
      { key: 'operation', label: '操作', type: 'operation', width: 100, fixed: 'right' },
    ],
  },
  productionInbound: {
    type: 'production-inbound',
    title: '生产入库',
    listRouteName: 'ProductionInbound',
    createRouteName: 'ProductionInboundCreate',
    viewRouteName: 'ProductionInboundView',
    editRouteName: 'ProductionInboundEdit',
    dateLabel: '入库日期',
    primaryField: {
      key: 'primaryName',
      label: '仓库',
      kind: 'warehouse',
      warehouseTypes: ['出品及生产部门', '普通仓库'],
    },
    counterpartyField: {
      key: 'counterpartyName',
      label: '加工间',
      kind: 'warehouse',
    },
  },
  customerSalesOutbound: {
    type: 'customer-sales-outbound',
    title: '客户销售出库',
    listRouteName: 'CustomerSalesOutbound',
    createRouteName: 'CustomerSalesOutboundCreate',
    viewRouteName: 'CustomerSalesOutboundView',
    editRouteName: 'CustomerSalesOutboundEdit',
    dateLabel: '出库日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    counterpartyField: { key: 'counterpartyName', label: '客户名称', kind: 'text' },
    extraFields: [
      { key: 'receiverName', label: '收货人', kind: 'text' },
      { key: 'receiverPhone', label: '联系电话', kind: 'text' },
      { key: 'receiverAddress', label: '收货地址', kind: 'text' },
    ],
    showAvailableQty: true,
  },
  customerReturnInbound: {
    type: 'customer-return-inbound',
    title: '客户退货入库',
    listRouteName: 'CustomerReturnInbound',
    createRouteName: 'CustomerReturnInboundCreate',
    viewRouteName: 'CustomerReturnInboundView',
    editRouteName: 'CustomerReturnInboundEdit',
    dateLabel: '入库日期',
    primaryField: { key: 'primaryName', label: '仓库', kind: 'warehouse' },
    counterpartyField: { key: 'counterpartyName', label: '客户名称', kind: 'text' },
    reasonField: { key: 'reason', label: '退货原因', kind: 'select', options: ['质量问题', '配送异常', '错发退货', '其他退货'] },
  },
  dishConsumptionOutbound: {
    type: 'dish-consumption-outbound',
    title: '菜品消耗出库',
    listRouteName: 'DishConsumptionOutbound',
    createRouteName: 'DishConsumptionOutboundCreate',
    viewRouteName: 'DishConsumptionOutboundView',
    editRouteName: 'DishConsumptionOutboundEdit',
    dateLabel: '消耗日期',
    primaryField: { key: 'primaryName', label: '扣减仓库', kind: 'warehouse' },
    counterpartyField: { key: 'counterpartyName', label: '来源单据', kind: 'text' },
    reasonField: { key: 'reason', label: '触发来源', kind: 'text' },
    showAvailableQty: true,
    showUpstreamCode: true,
    itemTableStyle: 'purchase-return-outbound',
  },
  storeTransfer: {
    type: 'store-transfer',
    title: '店间调拨',
    listRouteName: 'StoreTransfer',
    createRouteName: 'StoreTransferOutboundCreate',
    viewRouteName: 'StoreTransferOutboundView',
    editRouteName: 'StoreTransferOutboundEdit',
    dateLabel: '调拨日期',
    primaryField: { key: 'primaryName', label: '调出门店', kind: 'text' },
    secondaryField: { key: 'secondaryName', label: '调入门店', kind: 'text' },
    extraFields: [
      { key: 'sourceWarehouse', label: '调出仓库', kind: 'warehouse' },
      { key: 'targetWarehouse', label: '调入仓库', kind: 'text' },
    ],
    showAvailableQty: true,
  },
  stockTransferOutbound: {
    type: 'stock-transfer-outbound',
    title: '移库出库',
    listRouteName: 'StockTransferOutbound',
    createRouteName: 'StockTransferOutboundCreate',
    viewRouteName: 'StockTransferOutboundView',
    editRouteName: 'StockTransferOutboundEdit',
    dateLabel: '出库日期',
    primaryField: { key: 'primaryName', label: '出库仓库', kind: 'warehouse' },
    secondaryField: { key: 'secondaryName', label: '入库仓库', kind: 'warehouse' },
    showAvailableQty: true,
  },
};

export const workflowBusinessOptions = [
  { processCode: 'PURCHASE_APPLICATION', businessName: '采购单申请流程' },
  { processCode: 'PURCHASE_ORDER', businessName: '采购订单流程' },
  { processCode: 'PURCHASE_RECEIPT', businessName: '采购收货单流程' },
  { processCode: 'PURCHASE_RETURN', businessName: '采购退货单流程' },
  { processCode: 'PURCHASE_INBOUND', businessName: '采购入库流程' },
  { processCode: 'PURCHASE_RETURN_OUTBOUND', businessName: '采购退货出库流程' },
  { processCode: 'DEPARTMENT_PICKING', businessName: '部门领料流程' },
  { processCode: 'DEPARTMENT_RETURN', businessName: '部门退料流程' },
  { processCode: 'STOCK_TRANSFER', businessName: '移库单流程' },
  { processCode: 'STOCK_TRANSFER_INBOUND', businessName: '移库入库流程' },
  { processCode: 'DEPARTMENT_TRANSFER', businessName: '部门调拨流程' },
  { processCode: 'DAMAGE_OUTBOUND', businessName: '报损出库流程' },
  { processCode: 'OTHER_INBOUND', businessName: '其他入库流程' },
  { processCode: 'OTHER_OUTBOUND', businessName: '其他出库流程' },
  { processCode: 'PRODUCTION_INBOUND', businessName: '生产入库流程' },
  { processCode: 'CUSTOMER_SALES_OUTBOUND', businessName: '客户销售出库流程' },
  { processCode: 'CUSTOMER_RETURN_INBOUND', businessName: '客户退货入库流程' },
  { processCode: 'DISH_CONSUMPTION_OUTBOUND', businessName: '菜品消耗出库流程' },
  { processCode: 'PERIOD_OPENING_BALANCE', businessName: '期初库存流程' },
  { processCode: 'STORE_TRANSFER', businessName: '店间调拨流程' },
  { processCode: 'STOCK_TRANSFER_OUTBOUND', businessName: '移库出库流程' },
  { processCode: 'INVENTORY_CHECK', businessName: '盘点单流程' },
  { processCode: 'MULTI_INVENTORY_CHECK', businessName: '多人盘点单流程' },
];
