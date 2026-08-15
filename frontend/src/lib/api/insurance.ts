import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { InsurancePolicy, InsuredSubjectType } from '@/types/insurance';
import type { Child } from '@/types/people';
import type { Driver } from '@/types/people';
import type { Vehicle } from '@/types/school';

export interface CreateInsurancePolicyRequest {
  subjectType: InsuredSubjectType;
  subjectId: number;
  provider: string;
  policyNumber: string;
  coverageAmount: number;
  startDate: string;
  endDate: string;
}

export function listInsurancePolicies(schoolId: number): Promise<InsurancePolicy[]> {
  return apiClient.get<ApiResponse<InsurancePolicy[]>>(`/schools/${schoolId}/insurance-policies`).then(unwrap);
}

export function createInsurancePolicy(schoolId: number, request: CreateInsurancePolicyRequest): Promise<InsurancePolicy> {
  return apiClient.post<ApiResponse<InsurancePolicy>>(`/schools/${schoolId}/insurance-policies`, request).then(unwrap);
}

export function linkChildInsurancePolicy(schoolId: number, childId: number, insurancePolicyId: number): Promise<Child> {
  return apiClient
    .put<ApiResponse<Child>>(`/schools/${schoolId}/children/${childId}/insurance-policy`, { insurancePolicyId })
    .then(unwrap);
}

export function linkDriverInsurancePolicy(schoolId: number, driverId: number, insurancePolicyId: number): Promise<Driver> {
  return apiClient
    .patch<ApiResponse<Driver>>(`/schools/${schoolId}/drivers/${driverId}/insurance-policy`, { insurancePolicyId })
    .then(unwrap);
}

export function linkVehicleInsurancePolicy(schoolId: number, vehicleId: number, insurancePolicyId: number): Promise<Vehicle> {
  return apiClient
    .patch<ApiResponse<Vehicle>>(`/schools/${schoolId}/vehicles/${vehicleId}/insurance-policy`, { insurancePolicyId })
    .then(unwrap);
}
