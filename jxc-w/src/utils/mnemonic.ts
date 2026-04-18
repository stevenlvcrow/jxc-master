import { pinyin } from 'pinyin-pro';

export const buildMnemonicCode = (value?: string | null) => {
  const text = String(value ?? '').trim();
  if (!text) {
    return '';
  }

  const initials = pinyin(text, {
    toneType: 'none',
    pattern: 'first',
    type: 'array',
  });

  return initials.join('').replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
};
