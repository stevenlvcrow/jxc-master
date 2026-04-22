import { computed, ref } from 'vue';
import { fetchSuppliersApi, type SupplierListRow } from '@/api/modules/supplier';
import { useSessionStore } from '@/stores/session';
import { normalizeOrgId } from '@/utils/org';

export type SupplierArchiveOption = {
  id: number;
  value: string;
  label: string;
  code: string;
};

export const useSupplierArchiveOptions = () => {
  const sessionStore = useSessionStore();
  const supplierOptions = ref<SupplierArchiveOption[]>([]);
  const supplierLoading = ref(false);

  const resolveSupplierOrgId = () => {
    const orgId = normalizeOrgId(sessionStore.currentOrgId);
    return orgId || undefined;
  };

  const loadSupplierOptions = async () => {
    const orgId = resolveSupplierOrgId();
    if (!orgId) {
      supplierOptions.value = [];
      return;
    }
    supplierLoading.value = true;
    try {
      const result = await fetchSuppliersApi({
        pageNo: 1,
        pageSize: 500,
      }, orgId);
      supplierOptions.value = (result.list ?? [])
        .map((item: SupplierListRow) => ({
          id: item.id,
          value: item.supplierName,
          label: item.supplierCode ? `${item.supplierName} / ${item.supplierCode}` : item.supplierName,
          code: item.supplierCode,
        }))
        .sort((left, right) => left.label.localeCompare(right.label, 'zh-Hans-CN'));
    } finally {
      supplierLoading.value = false;
    }
  };

  const supplierLabelMap = computed(() => new Map(
    supplierOptions.value.map((option) => [option.value, option.label]),
  ));

  return {
    supplierOptions,
    supplierLoading,
    supplierLabelMap,
    loadSupplierOptions,
    resolveSupplierOrgId,
  };
};
