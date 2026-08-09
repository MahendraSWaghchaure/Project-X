import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Grievance, GrievanceCategory, GrievanceStatus } from '@/types/grievance';

export interface CreateGrievanceRequest {
  schoolId?: number;
  category: GrievanceCategory;
  subject: string;
  description: string;
}

export function raiseGrievance(request: CreateGrievanceRequest): Promise<Grievance> {
  return apiClient.post<ApiResponse<Grievance>>('/grievances', request).then(unwrap);
}

export function listMyGrievances(): Promise<Grievance[]> {
  return apiClient.get<ApiResponse<Grievance[]>>('/grievances/mine').then(unwrap);
}

export function listGrievancesForSchool(schoolId: number): Promise<Grievance[]> {
  return apiClient.get<ApiResponse<Grievance[]>>(`/schools/${schoolId}/grievances`).then(unwrap);
}

export function listAllGrievances(): Promise<Grievance[]> {
  return apiClient.get<ApiResponse<Grievance[]>>('/grievances').then(unwrap);
}

export function updateGrievanceStatus(
  grievanceId: number,
  status: GrievanceStatus,
  resolutionNote?: string,
): Promise<Grievance> {
  return apiClient
    .patch<ApiResponse<Grievance>>(`/grievances/${grievanceId}/status`, { status, resolutionNote })
    .then(unwrap);
}
