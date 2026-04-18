import { apiClient } from '@/api/http-client';
// ==================== API Functions ====================
/** 获取规则列表 */
export const fetchItemRulesApi = (groupId) => apiClient.get(`/api/identity/admin/groups/${groupId}/item-rules`);
/** 获取规则详情（含物品/分类/仓库明细） */
export const getItemRuleDetailApi = (id) => apiClient.get(`/api/identity/admin/item-rules/${id}`);
/** 新建规则 */
export const createItemRuleApi = (groupId, payload) => apiClient.post(`/api/identity/admin/groups/${groupId}/item-rules`, payload);
/** 更新规则 */
export const updateItemRuleApi = (id, payload) => apiClient.put(`/api/identity/admin/item-rules/${id}`, payload);
/** 删除规则 */
export const deleteItemRuleApi = (id) => apiClient.delete(`/api/identity/admin/item-rules/${id}`);
