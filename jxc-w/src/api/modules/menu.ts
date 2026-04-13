import { apiClient } from '@/api/http-client';

export type CurrentMenuItem = {
  id: number;
  menuCode: string;
  menuName: string;
  parentId: number | null;
  menuType: 'DIRECTORY' | 'MENU';
  routePath: string | null;
  icon: string | null;
  sortNo: number | null;
};

export const fetchCurrentMenusApi = (orgId?: string) => apiClient.get<CurrentMenuItem[]>('/api/identity/menus/current', {
  params: orgId ? { orgId } : undefined,
});
