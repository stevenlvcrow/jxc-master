export const normalizeItemOrgId = (orgId?: string | null) => String(orgId ?? '').trim().toLowerCase();

export const requireItemOrgId = (orgId?: string | null) => {
  const normalizedOrgId = normalizeItemOrgId(orgId);
  if (!normalizedOrgId) {
    throw new Error('请先选择机构');
  }
  return normalizedOrgId;
};
