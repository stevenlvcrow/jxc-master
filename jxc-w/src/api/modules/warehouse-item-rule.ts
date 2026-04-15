import { apiClient } from '@/api/http-client';

// ==================== Types ====================

export type ItemRuleRow = {
  id: number;
  ruleCode: string;
  ruleName: string;
  businessControl: boolean;
  status: 'ENABLED' | 'DISABLED';
  createdBy: string;
  createdAt: string;
  updatedBy: string;
  updatedAt: string;
};

export type RuleDetailView = {
  id: number;
  ruleCode: string;
  ruleName: string;
  businessControl: boolean;
  controlOrder: boolean;
  controlPurchaseInbound: boolean;
  controlTransferInbound: boolean;
  status: string;
  createdBy: string;
  createdAt: string;
  updatedBy: string;
  updatedAt: string;
  items: RuleItemDetail[];
  categories: RuleCategoryDetail[];
  warehouses: RuleWarehouseDetail[];
};

export type RuleItemDetail = {
  id: number;
  itemCode?: string;
  itemName?: string;
  specModel?: string;
  itemCategory?: string;
};

export type RuleCategoryDetail = {
  id: number;
  categoryCode?: string;
  categoryName?: string;
  parentCategory?: string;
  childCategory?: string;
};

export type RuleWarehouseDetail = {
  id: number;
  warehouseId?: number;
  warehouseName?: string;
};

export type RuleCreatePayload = {
  ruleCode: string;
  ruleName: string;
  businessControl: boolean;
  controlOrder: boolean;
  controlPurchaseInbound: boolean;
  controlTransferInbound: boolean;
  items: RuleItemPayload[];
  categories: RuleCategoryPayload[];
  warehouses: RuleWarehousePayload[];
};

export type RuleUpdatePayload = {
  ruleName: string;
  businessControl: boolean;
  controlOrder: boolean;
  controlPurchaseInbound: boolean;
  controlTransferInbound: boolean;
  items: RuleItemPayload[];
  categories: RuleCategoryPayload[];
  warehouses: RuleWarehousePayload[];
};

export type RuleItemPayload = {
  itemCode?: string;
  itemName?: string;
  specModel?: string;
  itemCategory?: string;
};

export type RuleCategoryPayload = {
  categoryCode?: string;
  categoryName?: string;
  parentCategory?: string;
  childCategory?: string;
};

export type RuleWarehousePayload = {
  warehouseId?: number;
};

// ==================== API Functions ====================

/** 获取规则列表 */
export const fetchItemRulesApi = (groupId: number) =>
  apiClient.get<ItemRuleRow[]>(`/api/identity/admin/groups/${groupId}/item-rules`);

/** 获取规则详情（含物品/分类/仓库明细） */
export const getItemRuleDetailApi = (id: number) =>
  apiClient.get<RuleDetailView>(`/api/identity/admin/item-rules/${id}`);

/** 新建规则 */
export const createItemRuleApi = (groupId: number, payload: RuleCreatePayload) =>
  apiClient.post<{ id: number }>(`/api/identity/admin/groups/${groupId}/item-rules`, payload);

/** 更新规则 */
export const updateItemRuleApi = (id: number, payload: RuleUpdatePayload) =>
  apiClient.put<void>(`/api/identity/admin/item-rules/${id}`, payload);

/** 删除规则 */
export const deleteItemRuleApi = (id: number) =>
  apiClient.delete<void>(`/api/identity/admin/item-rules/${id}`);
