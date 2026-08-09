import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Driver } from '@/types/people';
import type { VehicleType } from '@/types/school';

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

export interface UpdateDriverRequest {
  firstName: string;
  lastName: string;
  phone: string;
  licenseNumber: string;
  licenseExpiry?: string;
  baseSalaryMonthly?: number;
}

export function updateDriver(schoolId: number, driverId: number, request: UpdateDriverRequest): Promise<Driver> {
  return apiClient.patch<ApiResponse<Driver>>(`/schools/${schoolId}/drivers/${driverId}`, request).then(unwrap);
}

export function terminateDriver(schoolId: number, driverId: number): Promise<void> {
  return apiClient.delete<ApiResponse<void>>(`/schools/${schoolId}/drivers/${driverId}`).then(unwrap);
}

export function claimDriver(schoolId: number, driverId: number): Promise<Driver> {
  return apiClient.post<ApiResponse<Driver>>(`/schools/${schoolId}/drivers/${driverId}/claim`, {}).then(unwrap);
}

export interface DriverSelfRegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone: string;
  licenseNumber: string;
  licenseExpiry?: string;
  vehicleRegistrationNumber: string;
  vehicleType: VehicleType;
  vehicleSeatCapacity: number;
}

/** Public - a privately-owned-vehicle driver signing up on their own, before any school has engaged them. */
export function selfRegisterDriver(request: DriverSelfRegisterRequest): Promise<Driver> {
  return apiClient.post<ApiResponse<Driver>>('/drivers/register', request).then(unwrap);
}

/** The pool of self-registered drivers no school has claimed yet. */
export function listIndependentDrivers(): Promise<Driver[]> {
  return apiClient.get<ApiResponse<Driver[]>>('/drivers/independent').then(unwrap);
}
