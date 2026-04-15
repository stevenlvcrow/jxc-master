import { apiClient } from '@/api/http-client';
import type { ApiPage } from '@/api/types';

export type DishTreeNode = {
  id: string | number;
  label: string;
  children?: DishTreeNode[];
};

export type DishListRow = {
  id: number;
  index: number;
  dishId: string;
  spuCode: string;
  dishName: string;
  spec: string;
  categoryId: number;
  category: string;
  dishType: string;
  deleted: 'Y' | 'N';
  linkedCostCard: '是' | '否';
  createdAt: string;
  updatedAt: string;
};

export type DishListParams = {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  deleted?: 'Y' | 'N' | '';
  categoryId?: string;
  dishType?: string;
};

const withOrgParams = <T extends Record<string, unknown>>(params?: T, orgId?: string) => ({
  ...(params ?? {}),
  ...(orgId ? { orgId } : {}),
});

export const fetchDishCategoryTreeApi = (orgId?: string) =>
  apiClient.get<DishTreeNode[]>('/api/dishes/categories/tree', { params: withOrgParams(undefined, orgId) });

export const fetchDishesApi = (params: DishListParams, orgId?: string) =>
  apiClient.get<ApiPage<DishListRow>>('/api/dishes', { params: withOrgParams(params, orgId) });
