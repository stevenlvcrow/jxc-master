const ACCESS_TOKEN_KEY = 'jxc-access-token';
const REFRESH_TOKEN_KEY = 'jxc-refresh-token';

const isBrowser = () => typeof window !== 'undefined';

const read = (key: string): string => {
  if (!isBrowser()) {
    return '';
  }
  return localStorage.getItem(key) ?? '';
};

const write = (key: string, value: string) => {
  if (!isBrowser()) {
    return;
  }
  if (!value) {
    localStorage.removeItem(key);
    return;
  }
  localStorage.setItem(key, value);
};

export const authStorage = {
  getAccessToken: () => read(ACCESS_TOKEN_KEY),
  getRefreshToken: () => read(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken = '') => {
    write(ACCESS_TOKEN_KEY, accessToken);
    write(REFRESH_TOKEN_KEY, refreshToken);
  },
  clearTokens: () => {
    write(ACCESS_TOKEN_KEY, '');
    write(REFRESH_TOKEN_KEY, '');
  },
};
