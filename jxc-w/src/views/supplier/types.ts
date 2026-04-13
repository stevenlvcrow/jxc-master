export type SupplierQuery = {
  supplierInfo: string;
  status: string;
  bindStatus: string;
  source: string;
  supplyRelation: string;
};

export type SupplierRow = {
  id: number;
  index: number;
  supplierCode: string;
  supplierName: string;
  supplierCategory: string;
  contactPerson: string;
  contactPhone: string;
  settlementMethod: string;
  source: string;
  supplyRelation: '有' | '无';
  remark: string;
  status: '启用' | '停用';
  bindStatus: '已绑定' | '未绑定';
  operatedAt: string;
};

export type SupplierTableColumn = {
  prop: keyof SupplierRow;
  label: string;
  width: number;
  fixed?: 'left' | 'right';
};

export type CategoryNode = {
  id: string;
  label: string;
  children?: CategoryNode[];
};
