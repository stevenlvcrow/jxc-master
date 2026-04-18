import { apiClient } from '@/api/http-client';
export const fetchOrgTreeApi = () => apiClient.get('/api/identity/org/tree');
