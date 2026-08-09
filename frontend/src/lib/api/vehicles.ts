import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Vehicle, VehicleStatus, VehicleType } from '@/types/school';

export interface CreateVehicleRequest {
  registrationNumber: string;
  type: VehicleType;
  seatCapacity: number;
  gpsDeviceId?: string;
}

export function listVehicles(schoolId: number): Promise<Vehicle[]> {
  return apiClient.get<ApiResponse<Vehicle[]>>(`/schools/${schoolId}/vehicles`).then(unwrap);
}

export function createVehicle(schoolId: number, request: CreateVehicleRequest): Promise<Vehicle> {
  return apiClient.post<ApiResponse<Vehicle>>(`/schools/${schoolId}/vehicles`, request).then(unwrap);
}

export function updateVehicleStatus(schoolId: number, vehicleId: number, status: VehicleStatus): Promise<Vehicle> {
  return apiClient
    .patch<ApiResponse<Vehicle>>(`/schools/${schoolId}/vehicles/${vehicleId}/status`, { status })
    .then(unwrap);
}

export interface UpdateVehicleRequest {
  registrationNumber: string;
  type: VehicleType;
  seatCapacity: number;
}

export function updateVehicle(schoolId: number, vehicleId: number, request: UpdateVehicleRequest): Promise<Vehicle> {
  return apiClient.patch<ApiResponse<Vehicle>>(`/schools/${schoolId}/vehicles/${vehicleId}`, request).then(unwrap);
}

export function retireVehicle(schoolId: number, vehicleId: number): Promise<void> {
  return apiClient.delete<ApiResponse<void>>(`/schools/${schoolId}/vehicles/${vehicleId}`).then(unwrap);
}
