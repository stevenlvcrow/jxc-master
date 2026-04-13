import axios from 'axios';
import { authStorage } from '@/api/auth-storage';

type RefreshPayload = {
  refreshToken: string;
};

type RefreshResult = {
  accessToken: string;
  refreshToken?: string;
};

let refreshingPromise: Promise<string> | null = null;

const refreshTokenRequest = async (refreshToken: string) => {
  const { data } = await axios.post('/api/identity/auth/refresh', { refreshToken } satisfies RefreshPayload, {
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 12000,
  });

  const res: RefreshResult = data?.data ?? data;
  if (!res?.accessToken) {
    throw new Error('Invalid refresh response');
  }

  authStorage.setTokens(res.accessToken, res.refreshToken ?? refreshToken);
  return res.accessToken;
};

export const refreshAccessToken = async () => {
  if (refreshingPromise) {
    return refreshingPromise;
  }

  const refreshToken = authStorage.getRefreshToken();
  if (!refreshToken) {
    throw new Error('Missing refresh token');
  }

  refreshingPromise = refreshTokenRequest(refreshToken)
    .finally(() => {
      refreshingPromise = null;
    });

  return refreshingPromise;
};
