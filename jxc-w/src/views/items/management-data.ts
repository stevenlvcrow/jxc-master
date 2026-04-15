import type { CategoryNode, ItemQuery, ItemTableColumn } from './types';

export const defaultItemQuery: ItemQuery = {
  keyword: '',
  status: '全部',
  itemType: '全部',
  statType: '全部',
  storageMode: '',
  tag: '',
};

export const statusOptions = ['全部', '启用', '停用'];
export const itemTypeOptions = ['全部', '普通物品'];
export const statTypeOptions = [
  '全部',
  '原料类 (成本类)',
  '酒水类 (成本类)',
  '调料料类 (成本类)',
  '半成品类 (成本类)',
  '低值易耗品类 (费用类)',
  '固定资产类 (费用类)',
];
export const storageModeOptions = ['冷藏', '冷冻', '常温'];

export const itemToolbarButtons = [
  '新增物品',
  '从菜品导入',
  '物品排序',
  '批量导入',
  '批量导出',
  '批量启用',
  '批量停用',
  '批量删除',
  '批量修改',
  '批量传图',
];

export const itemCategoryTree: CategoryNode[] = [
  {
    label: '物品类别',
    children: [
      { label: '蔬菜' },
      { label: '奶茶' },
      { label: '肉类' },
      { label: '调料' },
      { label: '面点' },
      { label: '熟食' },
      { label: '日用百货' },
      { label: '水产' },
      { label: '河粉' },
      { label: '面食' },
      { label: '预制菜' },
      { label: '酒水' },
      { label: '一次性用品' },
      { label: '前厅类' },
    ],
  },
];

export const itemColumns: ItemTableColumn[] = [
  { prop: 'index', label: '序号', width: 46, fixed: 'left' },
  { prop: 'code', label: '物品编码', width: 90, fixed: 'left' },
  { prop: 'name', label: '物品名称', width: 120, fixed: 'left' },
  { prop: 'spec', label: '规格型号', width: 96 },
  { prop: 'type', label: '物品类型', width: 74 },
  { prop: 'category', label: '物品类别', width: 88 },
  { prop: 'brand', label: '物品品牌', width: 88 },
  { prop: 'productionCost', label: '生产参考成本', width: 92 },
  { prop: 'baseUnit', label: '基准单位', width: 68 },
  { prop: 'purchaseUnit', label: '默认采购单位', width: 86 },
  { prop: 'orderUnit', label: '默认订货单位', width: 86 },
  { prop: 'stockUnit', label: '默认库存单位', width: 86 },
  { prop: 'costUnit', label: '默认成本单位', width: 86 },
  { prop: 'suggestPrice', label: '建议采购价格', width: 92 },
  { prop: 'volume', label: '基准单位体积', width: 84 },
  { prop: 'weight', label: '基准单位重量', width: 84 },
  { prop: 'statType', label: '统计类型', width: 78 },
  { prop: 'thirdPartyCode', label: '第三方编码', width: 94 },
  { prop: 'abcClass', label: 'ABC 分类', width: 72 },
  { prop: 'stockMin', label: '库存下限', width: 72 },
  { prop: 'stockMax', label: '库存上限', width: 72 },
  { prop: 'source', label: '来源', width: 66 },
  { prop: 'status', label: '状态', width: 66 },
  { prop: 'storageMode', label: '储存方式', width: 76 },
  { prop: 'tag', label: '物品标签', width: 90 },
  { prop: 'image', label: '物品图片', width: 76 },
  { prop: 'createdAt', label: '创建时间', width: 126 },
  { prop: 'updatedAt', label: '操作时间', width: 126 },
];
