import { apiClient } from '@/api/http-client';

type OrgNodeType = 'group' | 'store';

export type OrgNodeDto = {
  id: string;
  name: string;
  merchantNo: string;
  code: string;
  city: string;
  type: OrgNodeType;
  children?: OrgNodeDto[];
};

export const fetchOrgTreeApi = () => apiClient.get<OrgNodeDto[]>('/api/identity/org/tree');
