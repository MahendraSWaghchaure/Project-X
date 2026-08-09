import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from './authStore';
import type { ApiResponse } from '@/types/api';
import type { TokenResponse } from '@/types/auth';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Dedupe concurrent refreshes: if five requests 401 at once, only one /auth/refresh call goes out
// and the rest wait on the same promise instead of racing each other into token rotation.
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }
  const response = await axios.post<ApiResponse<TokenResponse>>(
    `${apiClient.defaults.baseURL}/auth/refresh`,
    { refreshToken },
  );
  const tokens = response.data.data;
  if (!tokens) {
    throw new Error('Refresh response had no token payload');
  }
  useAuthStore.getState().setSession({
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    role: tokens.role,
  });
  return tokens.accessToken;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;

    if (error.response?.status === 401 && originalRequest && !originalRequest._retried) {
      originalRequest._retried = true;
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
        const newAccessToken = await refreshPromise;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch {
        useAuthStore.getState().clearSession();
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

export function unwrap<T>(response: { data: ApiResponse<T> }): T {
  if (!response.data.success) {
    throw new Error(response.data.error?.message ?? 'Request failed');
  }
  // Void endpoints (DELETE/terminate/retire) legitimately succeed with data: null.
  return response.data.data as T;
}
