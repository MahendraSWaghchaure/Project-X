import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { School } from '@/types/school';

export interface CreateSchoolRequest {
  name: string;
  address: string;
  city: string;
  state?: string;
  pincode?: string;
  contactPhone?: string;
  contactEmail?: string;
  latitude?: number;
  longitude?: number;
  geofenceRadiusMeters?: number;
  dismissalTime?: string; // "HH:mm:ss"
  adminFirstName: string;
  adminLastName: string;
  adminEmail: string;
  adminPassword: string;
}

export function listSchools(): Promise<School[]> {
  return apiClient.get<ApiResponse<School[]>>('/schools').then(unwrap);
}

export function getSchool(schoolId: number): Promise<School> {
  return apiClient.get<ApiResponse<School>>(`/schools/${schoolId}`).then(unwrap);
}

export function createSchool(request: CreateSchoolRequest): Promise<School> {
  return apiClient.post<ApiResponse<School>>('/schools', request).then(unwrap);
}
