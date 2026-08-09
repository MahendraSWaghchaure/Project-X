import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Driver } from '@/types/people';

export interface CreateDriverRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone: string;
  licenseNumber: string;
  licenseExpiry?: string;
  baseSalaryMonthly?: number;
}

export function listDrivers(schoolId: number): Promise<Driver[]> {
  return apiClient.get<ApiResponse<Driver[]>>(`/schools/${schoolId}/drivers`).then(unwrap);
}

export function createDriver(schoolId: number, request: CreateDriverRequest): Promise<Driver> {
  return apiClient.post<ApiResponse<Driver>>(`/schools/${schoolId}/drivers`, request).then(unwrap);
}
