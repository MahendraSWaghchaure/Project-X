import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type { ChildTripEvent, ChildTripEventType, Trip, TripDirection } from '@/types/tracking';

export function listTripsForSchool(schoolId: number, date: string): Promise<Trip[]> {
  return apiClient.get<ApiResponse<Trip[]>>(`/schools/${schoolId}/trips`, { params: { date } }).then(unwrap);
}

export function scheduleTrip(schoolId: number, routeId: number, direction: TripDirection, tripDate: string): Promise<Trip> {
  return apiClient
    .post<ApiResponse<Trip>>(`/schools/${schoolId}/trips`, { routeId, direction, tripDate })
    .then(unwrap);
}

export function listMyTripsAsDriver(date: string): Promise<Trip[]> {
  return apiClient.get<ApiResponse<Trip[]>>('/drivers/me/trips', { params: { date } }).then(unwrap);
}

export function getTrip(tripId: number): Promise<Trip> {
  return apiClient.get<ApiResponse<Trip>>(`/trips/${tripId}`).then(unwrap);
}

export function startTrip(tripId: number): Promise<Trip> {
  return apiClient.post<ApiResponse<Trip>>(`/trips/${tripId}/start`).then(unwrap);
}

export function completeTrip(tripId: number): Promise<Trip> {
  return apiClient.post<ApiResponse<Trip>>(`/trips/${tripId}/complete`).then(unwrap);
}

export function cancelTrip(tripId: number): Promise<Trip> {
  return apiClient.post<ApiResponse<Trip>>(`/trips/${tripId}/cancel`).then(unwrap);
}

export function listChildTripEvents(tripId: number): Promise<ChildTripEvent[]> {
  return apiClient.get<ApiResponse<ChildTripEvent[]>>(`/trips/${tripId}/child-events`).then(unwrap);
}

export function recordDriverMarkedEvent(tripId: number, childId: number, eventType: ChildTripEventType): Promise<ChildTripEvent> {
  return apiClient
    .post<ApiResponse<ChildTripEvent>>(`/trips/${tripId}/child-events`, { childId, eventType })
    .then(unwrap);
}

export function recordParentHomeArrival(tripId: number, childId: number): Promise<ChildTripEvent> {
  return apiClient
    .post<ApiResponse<ChildTripEvent>>(`/trips/${tripId}/children/${childId}/home-arrival`)
    .then(unwrap);
}

export function getTodaysTripForMyChild(childId: number, direction: TripDirection): Promise<Trip> {
  return apiClient
    .get<ApiResponse<Trip>>(`/parents/me/children/${childId}/trips/today`, { params: { direction } })
    .then(unwrap);
}
