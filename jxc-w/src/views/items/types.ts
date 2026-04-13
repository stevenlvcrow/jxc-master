export type ItemQuery = {
  keyword: string;
  status: string;
  itemType: string;
  statType: string;
  storageMode: string;
  tag: string;
};

export type ItemRow = {
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
  status: string;
  storageMode: string;
  tag: string;
  image: string;
  createdAt: string;
  updatedAt: string;
};

export type ItemTableColumn = {
  prop: keyof ItemRow;
  label: string;
  width: number;
  fixed?: 'left' | 'right';
};

export type CategoryNode = {
  label: string;
  children?: CategoryNode[];
};
