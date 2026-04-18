import { apiClient } from '@/api/http-client';
const withParams = (params) => ({
    orgId: params.orgId,
    businessCode: params.businessCode,
    workflowCode: params.workflowCode,
});
export const fetchCurrentWorkflowConfigApi = (params) => apiClient.get('/api/workflow/configs/current', {
    params: withParams(params),
});
export const saveCurrentWorkflowConfigApi = (payload) => apiClient.put('/api/workflow/configs/current', {
    businessCode: payload.businessCode,
    workflowCode: payload.workflowCode,
    workflowName: payload.workflowName,
    nodes: payload.nodes,
}, {
    params: {
        orgId: payload.orgId,
    },
});
export const publishWorkflowConfigApi = (params) => apiClient.post('/api/workflow/configs/publish', undefined, {
    params: withParams(params),
});
export const fetchWorkflowPublishHistoryApi = (params) => apiClient.get('/api/workflow/configs/history', {
    params: {
        ...withParams(params),
        limit: params.limit,
    },
});
export const fetchWorkflowPublishHistoryManageApi = (orgId, limit = 200) => apiClient.get('/api/workflow/configs/publish-histories', {
    params: { orgId, limit },
});
export const deleteWorkflowConfigApi = (id, orgId) => apiClient.delete(`/api/workflow/configs/${id}`, {
    params: { orgId },
});
export const fetchWorkflowProcessesApi = (orgId) => apiClient.get('/api/workflow/processes', {
    params: { orgId },
});
export const createWorkflowProcessApi = (payload) => apiClient.post('/api/workflow/processes', {
    processCode: payload.processCode,
    businessName: payload.businessName,
    templateId: payload.templateId,
}, { params: { orgId: payload.orgId } });
export const updateWorkflowProcessApi = (id, payload) => apiClient.put(`/api/workflow/processes/${id}`, {
    processCode: payload.processCode,
    businessName: payload.businessName,
    templateId: payload.templateId,
}, { params: { orgId: payload.orgId } });
export const deleteWorkflowProcessApi = (id, orgId) => apiClient.delete(`/api/workflow/processes/${id}`, { params: { orgId } });
export const bindWorkflowTemplateApi = (id, templateId, orgId) => apiClient.put(`/api/workflow/processes/${id}/bind-template`, { templateId }, { params: { orgId } });
export const fetchWorkflowProcessStoresApi = (orgId) => apiClient.get('/api/workflow/processes/stores', {
    params: { orgId },
    meta: { silent: true },
});
export const bindWorkflowProcessStoresApi = (id, storeIds, orgId) => apiClient.put(`/api/workflow/processes/${id}/bind-stores`, { storeIds }, { params: { orgId } });
