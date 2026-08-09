import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { CachedLocation } from '@/types/tracking';

export function getVehicleLocation(schoolId: number, vehicleId: number): Promise<CachedLocation> {
  return apiClient
    .get<ApiResponse<CachedLocation>>(`/schools/${schoolId}/vehicles/${vehicleId}/location`)
    .then(unwrap);
}

export function getMyChildVehicleLocation(childId: number): Promise<CachedLocation> {
  return apiClient
    .get<ApiResponse<CachedLocation>>(`/parents/me/children/${childId}/vehicle-location`)
    .then(unwrap);
}
