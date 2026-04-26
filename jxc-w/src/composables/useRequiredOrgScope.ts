import { computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useSessionStore } from '@/stores/session';
import { normalizeOrgId, parseStoreId } from '@/utils/org';

export const useRequiredOrgScope = () => {
  const sessionStore = useSessionStore();
  const orgId = computed(() => normalizeOrgId(sessionStore.currentOrgId) || undefined);
  const storeId = computed(() => parseStoreId(orgId.value));
  const storeOrgId = computed(() => (storeId.value ? orgId.value : undefined));

  const requireOrgId = (message = '未选择机构') => {
    if (orgId.value) {
      return orgId.value;
    }
    ElMessage.warning(message);
    return '';
  };

  const requireStoreOrgId = (message = '请选择门店机构') => {
    if (storeOrgId.value) {
      return storeOrgId.value;
    }
    ElMessage.warning(message);
    return '';
  };

  const requireStoreId = (message = '请选择门店机构') => {
    if (storeId.value) {
      return storeId.value;
    }
    ElMessage.warning(message);
    return null;
  };

  return {
    orgId,
    storeId,
    storeOrgId,
    requireOrgId,
    requireStoreOrgId,
    requireStoreId,
  };
};
