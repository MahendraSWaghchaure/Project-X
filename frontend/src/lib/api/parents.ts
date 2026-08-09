import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Parent } from '@/types/people';

export interface InviteParentResponse {
  parentProfileId: number;
  email: string;
  inviteToken: string;
}

export function inviteParent(schoolId: number, email: string, phone?: string): Promise<InviteParentResponse> {
  return apiClient
    .post<ApiResponse<InviteParentResponse>>(`/schools/${schoolId}/parents/invitations`, { email, phone })
    .then(unwrap);
}

export function getMyParentProfile(): Promise<Parent> {
  return apiClient.get<ApiResponse<Parent>>('/parents/me').then(unwrap);
}

export interface CompleteParentProfileRequest {
  firstName: string;
  lastName: string;
  phone: string;
  address: string;
  latitude?: number;
  longitude?: number;
}

export function completeMyParentProfile(request: CompleteParentProfileRequest): Promise<Parent> {
  return apiClient.patch<ApiResponse<Parent>>('/parents/me', request).then(unwrap);
}
