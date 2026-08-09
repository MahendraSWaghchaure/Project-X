import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { Child } from '@/types/people';

export interface CreateChildRequest {
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  classSection?: string;
  primaryParentId?: number;
}

export function listChildrenForSchool(schoolId: number): Promise<Child[]> {
  return apiClient.get<ApiResponse<Child[]>>(`/schools/${schoolId}/children`).then(unwrap);
}

export function listChildrenForRoute(schoolId: number, routeId: number): Promise<Child[]> {
  return apiClient.get<ApiResponse<Child[]>>(`/schools/${schoolId}/routes/${routeId}/children`).then(unwrap);
}

export function createChildAsSchoolAdmin(schoolId: number, request: CreateChildRequest): Promise<Child> {
  return apiClient.post<ApiResponse<Child>>(`/schools/${schoolId}/children`, request).then(unwrap);
}

export function listMyChildren(): Promise<Child[]> {
  return apiClient.get<ApiResponse<Child[]>>('/parents/me/children').then(unwrap);
}

export function createChildAsParent(request: CreateChildRequest): Promise<Child> {
  return apiClient.post<ApiResponse<Child>>('/parents/me/children', request).then(unwrap);
}

export function assignChildRouteStop(
  schoolId: number,
  childId: number,
  routeId: number,
  routeStopId: number,
): Promise<Child> {
  return apiClient
    .put<ApiResponse<Child>>(`/schools/${schoolId}/children/${childId}/route-stop`, { routeId, routeStopId })
    .then(unwrap);
}

export interface UpdateChildRequest {
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  classSection?: string;
}

export function updateChildAsSchoolAdmin(schoolId: number, childId: number, request: UpdateChildRequest): Promise<Child> {
  return apiClient.patch<ApiResponse<Child>>(`/schools/${schoolId}/children/${childId}`, request).then(unwrap);
}

export function deactivateChild(schoolId: number, childId: number): Promise<void> {
  return apiClient.delete<ApiResponse<void>>(`/schools/${schoolId}/children/${childId}`).then(unwrap);
}

export function updateChildAsParent(childId: number, request: UpdateChildRequest): Promise<Child> {
  return apiClient.patch<ApiResponse<Child>>(`/parents/me/children/${childId}`, request).then(unwrap);
}

/** Zone-based auto-allocation: picks the route/stop nearest the child's home among routes whose radius band covers it. */
export function autoAssignRoute(schoolId: number, childId: number): Promise<Child> {
  return apiClient.post<ApiResponse<Child>>(`/schools/${schoolId}/children/${childId}/auto-assign-route`, {}).then(unwrap);
}
