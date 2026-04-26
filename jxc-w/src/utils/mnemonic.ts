export const buildMnemonicCode = async (value?: string | null) => {
  const text = String(value ?? '').trim();
  if (!text) {
    return '';
  }

  const { pinyin } = await import('pinyin-pro');
  const initials = pinyin(text, {
    toneType: 'none',
    pattern: 'first',
    type: 'array',
  });

  return initials.join('').replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
};
