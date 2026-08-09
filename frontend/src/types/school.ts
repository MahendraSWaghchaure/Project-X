export interface GeoPoint {
  latitude: number;
  longitude: number;
}

export interface School {
  id: number;
  name: string;
  address: string;
  city: string;
  state: string | null;
  pincode: string | null;
  contactPhone: string | null;
  contactEmail: string | null;
  location: GeoPoint | null;
  geofenceRadiusMeters: number;
  dismissalTime: string | null; // "HH:mm:ss"
  status: 'ACTIVE' | 'SUSPENDED';
}

export type VehicleType = 'VAN' | 'BUS';
export type VehicleStatus = 'ACTIVE' | 'IN_MAINTENANCE' | 'RETIRED';

export interface Vehicle {
  id: number;
  schoolId: number;
  registrationNumber: string;
  type: VehicleType;
  seatCapacity: number;
  gpsDeviceId: string | null;
  insurancePolicyId: number | null;
  status: VehicleStatus;
}

export interface RouteStop {
  id: number;
  sequenceNumber: number;
  label: string;
  location: GeoPoint;
  radiusMeters: number;
}

export interface Route {
  id: number;
  schoolId: number;
  name: string;
  vehicleId: number | null;
  driverId: number | null;
  active: boolean;
  stops: RouteStop[];
}
