import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Route } from '@/types/school';

export interface RouteStopRequest {
  sequenceNumber: number;
  label: string;
  latitude: number;
  longitude: number;
  radiusMeters?: number;
}

export interface CreateRouteRequest {
  name: string;
  stops: RouteStopRequest[];
  zoneMinRadiusMeters?: number;
  zoneMaxRadiusMeters?: number;
}

export function listRoutes(schoolId: number): Promise<Route[]> {
  return apiClient.get<ApiResponse<Route[]>>(`/schools/${schoolId}/routes`).then(unwrap);
}

export function createRoute(schoolId: number, request: CreateRouteRequest): Promise<Route> {
  return apiClient.post<ApiResponse<Route>>(`/schools/${schoolId}/routes`, request).then(unwrap);
}

export function assignRoute(schoolId: number, routeId: number, vehicleId: number, driverId: number): Promise<Route> {
  return apiClient
    .put<ApiResponse<Route>>(`/schools/${schoolId}/routes/${routeId}/assignment`, { vehicleId, driverId })
    .then(unwrap);
}

export function updateRouteZone(
  schoolId: number,
  routeId: number,
  zoneMinRadiusMeters: number,
  zoneMaxRadiusMeters: number,
): Promise<Route> {
  return apiClient
    .put<ApiResponse<Route>>(`/schools/${schoolId}/routes/${routeId}/zone`, { zoneMinRadiusMeters, zoneMaxRadiusMeters })
    .then(unwrap);
}
