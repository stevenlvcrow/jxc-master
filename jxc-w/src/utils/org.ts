export const normalizeOrgId = (value?: string | null) => String(value ?? '').trim().toLowerCase();

export const isGroupOrgId = (value?: string | null) => normalizeOrgId(value).startsWith('group-');

export const isStoreOrgId = (value?: string | null) => normalizeOrgId(value).startsWith('store-');

export const parseStoreId = (value?: string | null) => {
  const normalized = normalizeOrgId(value);
  if (!normalized.startsWith('store-')) {
    return null;
  }
  const parsed = Number(normalized.slice('store-'.length));
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
};
