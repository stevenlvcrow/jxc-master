import axios from 'axios';
import { authStorage } from '@/api/auth-storage';

type RefreshPayload = {
  refreshToken: string;
};

type RefreshResult = {
  accessToken: string;
  refreshToken?: string;
};

type RefreshResponse = {
  code: number;
  message: string;
  data: RefreshResult;
};

let refreshingPromise: Promise<string> | null = null;

const isRefreshResponse = (payload: unknown): payload is RefreshResponse => (
  Boolean(payload)
  && typeof payload === 'object'
  && typeof (payload as RefreshResponse).code === 'number'
  && typeof (payload as RefreshResponse).message === 'string'
  && Boolean((payload as RefreshResponse).data)
  && typeof (payload as RefreshResponse).data.accessToken === 'string'
);

const refreshTokenRequest = async (refreshToken: string) => {
  const { data } = await axios.post('/api/identity/auth/refresh', { refreshToken } satisfies RefreshPayload, {
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 12000,
  });

  if (!isRefreshResponse(data) || (data.code !== 0 && data.code !== 200)) {
    throw new Error('Invalid refresh response');
  }

  const res = data.data;
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
