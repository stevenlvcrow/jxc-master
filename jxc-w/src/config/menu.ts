import type { RouteRecordRaw } from 'vue-router';
import rawMenu from './jxc-menu.json';

type RawMenuNode = {
  text: string;
  children?: RawMenuNode[];
};

export type AppMenuItem = {
  key: string;
  title: string;
  path?: string;
  icon?: string;
  menuType?: 'DIRECTORY' | 'MENU';
  children?: AppMenuItem[];
};

type FeatureMeta = {
  title: string;
  breadcrumbs: string[];
  menuKey: string;
  openKeys: string[];
};

const iconMap: Record<string, string> = {
  订货管理: 'Tickets',
  采购管理: 'Goods',
  生产管理: 'Collection',
  库存管理: 'Files',
  财务管理: 'Coin',
  质量管理: 'Checked',
  报表管理: 'DataAnalysis',
  档案管理: 'Document',
  预警管理: 'Bell',
  系统设置: 'Setting',
};

const segmentMap: Record<string, string> = {
  订货管理: 'order',
  采购管理: 'purchase',
  生产管理: 'production',
  库存管理: 'inventory',
  财务管理: 'finance',
  质量管理: 'quality',
  报表管理: 'report',
  档案管理: 'archive',
  预警管理: 'warning',
  系统设置: 'system',
};

const menuTree = rawMenu.children as RawMenuNode[];

const buildKey = (indexes: number[]) => indexes.map((index) => `m${index}`).join('-');

const buildPath = (segment: string, indexes: number[]) => `/${segment}/${indexes.slice(1).join('/')}`;

const normalizeRoutePath = (value?: string | null) => {
  const raw = String(value ?? '').trim().replace(/\/+$/, '');
  if (!raw) {
    return '';
  }
  return raw.startsWith('/') ? raw : `/${raw}`;
};

const buildMenuItems = (
  nodes: RawMenuNode[],
  indexes: number[] = [],
  segment = '',
): AppMenuItem[] => nodes.map((node, index) => {
  const currentIndexes = [...indexes, index + 1];
  const key = buildKey(currentIndexes);
  const isTopLevel = indexes.length === 0;
  const currentSegment = isTopLevel ? (segmentMap[node.text] ?? `module-${index + 1}`) : segment;
  const children = node.children?.length
    ? buildMenuItems(node.children, currentIndexes, currentSegment)
    : undefined;

  return {
    key,
    title: node.text,
    icon: isTopLevel ? (iconMap[node.text] ?? 'Document') : undefined,
    path: children ? undefined : normalizeRoutePath(buildPath(currentSegment, currentIndexes)),
    children,
  };
});

const buildRoutes = (
  nodes: RawMenuNode[],
  indexes: number[] = [],
  breadcrumbs: string[] = [],
  segment = '',
): RouteRecordRaw[] => nodes.flatMap((node, index) => {
  const currentIndexes = [...indexes, index + 1];
  const currentBreadcrumbs = [...breadcrumbs, node.text];
  const key = buildKey(currentIndexes);
  const isTopLevel = indexes.length === 0;
  const currentSegment = isTopLevel ? (segmentMap[node.text] ?? `module-${index + 1}`) : segment;
  const children = node.children ?? [];

  if (children.length) {
    return buildRoutes(children, currentIndexes, currentBreadcrumbs, currentSegment);
  }

  const meta: FeatureMeta = {
    title: node.text,
    breadcrumbs: currentBreadcrumbs,
    menuKey: key,
    openKeys: currentIndexes.slice(0, -1).map((_, currentIndex) => buildKey(currentIndexes.slice(0, currentIndex + 1))),
  };

  return [{
    path: buildPath(currentSegment, currentIndexes).slice(1),
    name: `Feature${currentIndexes.join('_')}`,
    component: () => import('@/views/feature/index.vue'),
    meta,
  }];
});

export const businessMenuItems = buildMenuItems(menuTree);

export const featureRoutes = buildRoutes(menuTree);
