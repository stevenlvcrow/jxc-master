import { apiClient } from '@/api/http-client';

export type WorkflowNode = {
  nodeKey: string;
  nodeName: string;
  x?: number;
  y?: number;
  approverRoleCode?: string;
  approverUserId?: number;
  nodeType?: 'NORMAL' | 'CONDITION' | 'SUCCESS' | 'FAIL' | 'START' | 'END';
  triggerActions?: ('CREATE' | 'UPDATE' | 'DELETE')[];
};

export type WorkflowConfig = {
  scopeType: string;
  scopeId: number;
  businessCode: string;
  workflowCode: string;
  workflowName: string;
  status: string;
  versionNo: number;
  nodes: WorkflowNode[];
  processDefinitionKey?: string;
  processDefinitionId?: string;
  deployedAt?: string;
  inherited?: boolean;
  sourceScopeType?: string;
  sourceScopeId?: number;
};

export type WorkflowPublishHistory = {
  processDefinitionId: string;
  processDefinitionKey: string;
  version: number;
  deploymentId: string;
};

export type WorkflowPublishHistoryManageItem = {
  id: number;
  businessCode: string;
  workflowCode: string;
  workflowName: string;
  status: string;
  versionNo: number;
  savedAt: string;
};

export type WorkflowProcessItem = {
  id: number;
  process_code: string;
  businessName: string;
  templateId?: string;
  storeIds?: number[];
  storeNames?: string[];
  createdAt: string;
};

export type WorkflowProcessStoreOption = {
  storeId: number;
  storeCode: string;
  storeName: string;
};

export type WorkflowApprovalNotificationItem = {
  id: number;
  approvalNo: string;
  workflowName: string;
  approverName: string;
  approverRole: string;
  auditedAt: string;
  result: string;
  remark: string;
  routePath?: string;
  businessCode: string;
  businessId: number;
};

export type WorkflowApprovalNotificationPage = {
  list: WorkflowApprovalNotificationItem[];
  total: number;
  pageNum: number;
  pageSize: number;
  tabs: WorkflowApprovalNotificationTab[];
  activeTab: WorkflowApprovalNotificationTabKey | '';
};

export type WorkflowApprovalNotificationTabKey = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED_BY_ME' | 'SUBMITTED';

export type WorkflowApprovalNotificationTab = {
  key: WorkflowApprovalNotificationTabKey;
  label: string;
};

type OrgParams = {
  orgId?: string;
  businessCode: string;
  workflowCode: string;
};

const withParams = (params: OrgParams) => ({
  orgId: params.orgId,
  businessCode: params.businessCode,
  workflowCode: params.workflowCode,
});

export const fetchCurrentWorkflowConfigApi = (params: OrgParams) =>
  apiClient.get<WorkflowConfig>('/api/workflow/configs/current', {
    params: withParams(params),
  });

export const saveCurrentWorkflowConfigApi = (payload: {
  orgId?: string;
  businessCode: string;
  workflowCode: string;
  workflowName: string;
  nodes: WorkflowNode[];
}) => apiClient.put<void>(
  '/api/workflow/configs/current',
  {
    businessCode: payload.businessCode,
    workflowCode: payload.workflowCode,
    workflowName: payload.workflowName,
    nodes: payload.nodes,
  },
  {
    params: {
      orgId: payload.orgId,
    },
  },
);

export const publishWorkflowConfigApi = (params: OrgParams) =>
  apiClient.post<{
    processDefinitionId: string;
    processDefinitionKey: string;
    version: number;
    deployedAt: string;
  }>('/api/workflow/configs/current/publish', undefined, {
    params: withParams(params),
  });

export const fetchWorkflowPublishHistoryApi = (params: OrgParams & { limit?: number }) =>
  apiClient.get<WorkflowPublishHistory[]>('/api/workflow/configs/history', {
    params: {
      ...withParams(params),
      limit: params.limit,
    },
  });

export const fetchWorkflowPublishHistoryManageApi = (orgId?: string, limit = 200) =>
  apiClient.get<WorkflowPublishHistoryManageItem[]>('/api/workflow/configs/publish-histories', {
    params: { orgId, limit },
  });

export const deleteWorkflowConfigApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/workflow/configs/${id}`, {
    params: { orgId },
  });

export const fetchWorkflowProcessesApi = (orgId?: string) =>
  apiClient.get<WorkflowProcessItem[]>('/api/workflow/processes', {
    params: { orgId },
  });

export const createWorkflowProcessApi = (payload: {
  orgId?: string;
  process_code: string;
  businessName: string;
  templateId?: string;
}) => apiClient.post<{ id: number }>(
  '/api/workflow/processes',
  {
    process_code: payload.process_code,
    businessName: payload.businessName,
    templateId: payload.templateId,
  },
  { params: { orgId: payload.orgId } },
);

export const updateWorkflowProcessApi = (id: number, payload: {
  orgId?: string;
  process_code: string;
  businessName: string;
  templateId?: string;
}) => apiClient.put<void>(
  `/api/workflow/processes/${id}`,
  {
    process_code: payload.process_code,
    businessName: payload.businessName,
    templateId: payload.templateId,
  },
  { params: { orgId: payload.orgId } },
);

export const deleteWorkflowProcessApi = (id: number, orgId?: string) =>
  apiClient.delete<void>(`/api/workflow/processes/${id}`, { params: { orgId } });

export const bindWorkflowTemplateApi = (id: number, templateId: string, orgId?: string) =>
  apiClient.put<void>(
    `/api/workflow/processes/${id}/bind-template`,
    { templateId },
    { params: { orgId } },
  );

export const fetchWorkflowProcessStoresApi = (orgId?: string) =>
  apiClient.get<WorkflowProcessStoreOption[]>('/api/workflow/processes/stores', {
    params: { orgId },
    meta: { silent: true },
  });

export const bindWorkflowProcessStoresApi = (id: number, storeIds: number[], orgId?: string) =>
  apiClient.put<void>(
    `/api/workflow/processes/${id}/bind-stores`,
    { storeIds },
    { params: { orgId } },
  );

export const batchBindWorkflowProcessStoresApi = (processIds: number[], storeIds: number[], orgId?: string) =>
  apiClient.put<void>(
    '/api/workflow/processes/batch-bind-stores',
    { processIds, storeIds },
    { params: { orgId } },
  );

export const fetchWorkflowApprovalNotificationsApi = (params: {
  orgId?: string;
  tab?: WorkflowApprovalNotificationTabKey | '';
  pageNum?: number;
  pageSize?: number;
}) =>
  apiClient.get<WorkflowApprovalNotificationPage>('/api/workflow/notifications', {
    params: {
      orgId: params.orgId,
      tab: params.tab || undefined,
      pageNum: params.pageNum,
      pageSize: params.pageSize,
    },
  });

export const fetchWorkflowPendingNotificationCountApi = (params: { orgId?: string }) =>
  apiClient.get<number>('/api/workflow/notifications/pending-count', {
    params: {
      orgId: params.orgId,
    },
  });
