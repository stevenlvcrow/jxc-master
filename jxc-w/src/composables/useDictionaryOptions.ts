import { computed, onMounted, ref, unref, watch, type MaybeRef } from 'vue';
import { fetchDictionaryOptionsApi, type DictionaryOption } from '@/api/modules/dictionary';

export type DictionaryTreeOption = DictionaryOption & {
  children?: DictionaryTreeOption[];
};

type AllOptionConfig = {
  enabled?: boolean;
  label?: string;
  value?: string;
};

const optionCache = new Map<string, DictionaryOption[]>();

const normalizeCodes = (codes: MaybeRef<string[]>) => (
  Array.from(new Set((unref(codes) ?? []).map((code) => code.trim()).filter(Boolean)))
);

const withAllOption = (options: DictionaryOption[], config?: AllOptionConfig) => {
  if (!config?.enabled) {
    return options;
  }
  return [
    {
      id: 0,
      parentId: null,
      itemKey: 'ALL',
      itemCode: config.value ?? 'ALL',
      itemLabel: config.label ?? '全部',
      sortNo: -1,
      extraJson: null,
    },
    ...options,
  ];
};

const buildTree = (options: DictionaryOption[]) => {
  const map = new Map<number, DictionaryTreeOption>();
  const roots: DictionaryTreeOption[] = [];
  options.forEach((option) => {
    map.set(option.id, { ...option });
  });
  map.forEach((option) => {
    if (option.parentId && map.has(option.parentId)) {
      const parent = map.get(option.parentId);
      parent!.children = parent!.children ?? [];
      parent!.children.push(option);
      return;
    }
    roots.push(option);
  });
  return roots;
};

export const refreshDictionaryCache = async (codes: string[]) => {
  const normalizedCodes = normalizeCodes(codes);
  if (!normalizedCodes.length) {
    return {};
  }
  const data = await fetchDictionaryOptionsApi(normalizedCodes);
  Object.entries(data ?? {}).forEach(([code, options]) => {
    optionCache.set(code, Array.isArray(options) ? options : []);
  });
  return data;
};

export const useDictionaryOptions = (codes: MaybeRef<string[]>) => {
  const loading = ref(false);
  const dictionaries = ref<Record<string, DictionaryOption[]>>({});

  const syncFromCache = () => {
    const next: Record<string, DictionaryOption[]> = {};
    normalizeCodes(codes).forEach((code) => {
      next[code] = optionCache.get(code) ?? [];
    });
    dictionaries.value = next;
  };

  const load = async (force = false) => {
    const normalizedCodes = normalizeCodes(codes);
    const missingCodes = force
      ? normalizedCodes
      : normalizedCodes.filter((code) => !optionCache.has(code));
    if (!missingCodes.length) {
      syncFromCache();
      return;
    }
    loading.value = true;
    try {
      await refreshDictionaryCache(missingCodes);
      syncFromCache();
    } finally {
      loading.value = false;
    }
  };

  const refresh = async () => {
    await load(true);
  };

  const optionsOf = (code: string, config?: AllOptionConfig) => computed(() => (
    withAllOption(dictionaries.value[code] ?? [], config)
  ));

  const treeOf = (code: string, config?: AllOptionConfig) => computed(() => (
    buildTree(withAllOption(dictionaries.value[code] ?? [], config))
  ));

  onMounted(() => {
    void load();
  });

  watch(
    () => normalizeCodes(codes).join(','),
    () => {
      void load();
    },
  );

  return {
    loading,
    dictionaries,
    load,
    refresh,
    optionsOf,
    treeOf,
  };
};
