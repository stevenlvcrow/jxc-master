import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { authStorage } from '@/api/auth-storage';

export type OrgNodeType = 'group' | 'store' | 'trial';

export type OrgNode = {
  id: string;
  name: string;
  merchantNo: string;
  code: string;
  city: string;
  type: OrgNodeType;
  children?: OrgNode[];
};

const STORAGE_LOGIN_KEY = 'jxc-login';
const STORAGE_ORG_KEY = 'jxc-current-org';
const STORAGE_PLATFORM_ADMIN_MODE_KEY = 'jxc-platform-admin-mode';

type LoginOptions = {
  platformAdminMode?: boolean;
};

const defaultOrgTree: OrgNode[] = [
  {
    id: 'trial-1',
    name: '测试美味火锅',
    merchantNo: '53568192',
    code: 'MD00002',
    city: '成都',
    type: 'trial',
  },
  {
    id: 'group-1',
    name: 'BOBO集团总部',
    merchantNo: '88766801',
    code: 'GP00001',
    city: '武汉',
    type: 'group',
    children: [
      {
        id: 'store-1',
        name: 'BOBO居民中心（麻城园林社区店）',
        merchantNo: '58957053',
        code: 'MD00001',
        city: '武汉',
        type: 'store',
      },
      {
        id: 'store-2',
        name: 'BOBO居民中心（南湖店）',
        merchantNo: '58329332',
        code: 'MD00002',
        city: '武汉',
        type: 'store',
      },
    ],
  },
  {
    id: 'group-2',
    name: '对外线上测试集团',
    merchantNo: '77889900',
    code: 'GP00002',
    city: '上海',
    type: 'group',
    children: [
      {
        id: 'store-3',
        name: '线上体验店',
        merchantNo: '66880123',
        code: 'MD00003',
        city: '上海',
        type: 'store',
      },
    ],
  },
];

const flattenNodes = (nodes: OrgNode[]): OrgNode[] => {
  const result: OrgNode[] = [];
  nodes.forEach((node) => {
    result.push(node);
    if (node.children?.length) {
      result.push(...flattenNodes(node.children));
    }
  });
  return result;
};

const normalizeOrgType = (value: unknown): OrgNodeType => {
  const normalized = String(value ?? '').trim().toLowerCase();
  if (normalized === 'group' || normalized === 'store' || normalized === 'trial') {
    return normalized;
  }
  return 'store';
};

const normalizeOrgId = (rawId: unknown, type: OrgNodeType): string => {
  const normalized = String(rawId ?? '').trim();
  if (!normalized) {
    return '';
  }
  if (/^(group|store|trial)-/i.test(normalized)) {
    return normalized.toLowerCase();
  }
  return `${type}-${normalized}`;
};

const normalizeOrgTree = (nodes: OrgNode[]): OrgNode[] => nodes.map((node) => {
  const type = normalizeOrgType(node.type);
  return {
    id: normalizeOrgId(node.id, type),
    name: String(node.name ?? ''),
    merchantNo: String(node.merchantNo ?? ''),
    code: String(node.code ?? ''),
    city: String(node.city ?? ''),
    type,
    children: node.children?.length ? normalizeOrgTree(node.children) : undefined,
  };
});

export const useSessionStore = defineStore('session', () => {
  const useRealAuthApi = import.meta.env.VITE_USE_REAL_AUTH_API === '1';
  const accessToken = ref(authStorage.getAccessToken());
  const refreshToken = ref(authStorage.getRefreshToken());
  const isLoggedIn = ref(
    useRealAuthApi ? Boolean(accessToken.value) : localStorage.getItem(STORAGE_LOGIN_KEY) === '1',
  );
  const userName = ref('李智杰');
  const orgTree = ref<OrgNode[]>(normalizeOrgTree(defaultOrgTree));
  const currentOrgId = ref((localStorage.getItem(STORAGE_ORG_KEY) ?? '').trim().toLowerCase());
  const platformAdminMode = ref(localStorage.getItem(STORAGE_PLATFORM_ADMIN_MODE_KEY) === '1');

  const flatOrgs = computed(() => flattenNodes(orgTree.value));
  const rootGroups = computed(() => orgTree.value);
  const currentOrg = computed(() => flatOrgs.value.find((item) => item.id === currentOrgId.value) ?? null);
  const hasSelectedOrg = computed(() => Boolean(currentOrg.value));
  const requiresOrgSelection = computed(() => !platformAdminMode.value);

  const persistLogin = () => {
    localStorage.setItem(STORAGE_LOGIN_KEY, isLoggedIn.value ? '1' : '0');
  };

  const persistOrg = () => {
    if (currentOrgId.value) {
      localStorage.setItem(STORAGE_ORG_KEY, currentOrgId.value);
      return;
    }
    localStorage.removeItem(STORAGE_ORG_KEY);
  };

  const persistPlatformAdminMode = () => {
    if (platformAdminMode.value) {
      localStorage.setItem(STORAGE_PLATFORM_ADMIN_MODE_KEY, '1');
      return;
    }
    localStorage.removeItem(STORAGE_PLATFORM_ADMIN_MODE_KEY);
  };

  const login = (name = '李智杰', account = '', options: LoginOptions = {}) => {
    isLoggedIn.value = true;
    userName.value = name;
    platformAdminMode.value = Boolean(options.platformAdminMode);
    if (platformAdminMode.value) {
      currentOrgId.value = '';
      persistOrg();
    }
    persistPlatformAdminMode();
    persistLogin();
  };

  const setAuth = (tokens: { accessToken: string; refreshToken?: string }) => {
    accessToken.value = tokens.accessToken;
    refreshToken.value = tokens.refreshToken ?? '';
    authStorage.setTokens(accessToken.value, refreshToken.value);
  };

  const logout = () => {
    isLoggedIn.value = false;
    currentOrgId.value = '';
    platformAdminMode.value = false;
    accessToken.value = '';
    refreshToken.value = '';
    persistLogin();
    persistOrg();
    persistPlatformAdminMode();
    authStorage.clearTokens();
  };

  const selectOrg = (orgId: string | number) => {
    const normalizedOrgId = String(orgId ?? '').trim().toLowerCase();
    if (!normalizedOrgId) {
      return;
    }
    const target = flatOrgs.value.find((item) => item.id === normalizedOrgId);
    if (!target) {
      return;
    }
    currentOrgId.value = normalizedOrgId;
    persistOrg();
  };

  const setOrgTree = (tree: OrgNode[]) => {
    orgTree.value = normalizeOrgTree(tree);
    if (currentOrgId.value && !flatOrgs.value.some((item) => item.id === currentOrgId.value)) {
      currentOrgId.value = '';
      persistOrg();
    }
  };

  return {
    isLoggedIn,
    userName,
    orgTree,
    rootGroups,
    flatOrgs,
    currentOrgId,
    currentOrg,
    hasSelectedOrg,
    requiresOrgSelection,
    platformAdminMode,
    accessToken,
    refreshToken,
    login,
    setAuth,
    logout,
    selectOrg,
    setOrgTree,
  };
});
