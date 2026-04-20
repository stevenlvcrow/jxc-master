export const normalizeItemOrgId = (orgId?: string | null) => String(orgId ?? '').trim().toLowerCase();

export const resolveArchiveOrgId = (orgId?: string | null, _platformAdminMode = false) => {
  const normalizedOrgId = normalizeItemOrgId(orgId);
  if (!normalizedOrgId || !normalizedOrgId.startsWith('store-')) {
    return null;
  }
  return normalizedOrgId;
};

export const requireItemOrgId = (orgId?: string | null, platformAdminMode = false) => {
  const resolvedOrgId = resolveArchiveOrgId(orgId, platformAdminMode);
  if (!resolvedOrgId) {
    throw new Error('请先选择门店机构');
  }
  return resolvedOrgId;
};
