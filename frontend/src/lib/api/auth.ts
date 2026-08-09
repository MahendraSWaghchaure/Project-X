import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { TokenResponse } from '@/types/auth';

export function login(email: string, password: string): Promise<TokenResponse> {
  return apiClient
    .post<ApiResponse<TokenResponse>>('/auth/login', { email, password })
    .then(unwrap);
}

export function acceptInvitation(token: string, newPassword: string): Promise<TokenResponse> {
  return apiClient
    .post<ApiResponse<TokenResponse>>(`/auth/invitations/${token}/accept`, { newPassword })
    .then(unwrap);
}

export function logout(): Promise<void> {
  return apiClient.post<ApiResponse<null>>('/auth/logout').then(() => undefined);
}
