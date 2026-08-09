import type { GeoPoint } from './school';

export type DriverEmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED';

export interface Driver {
  id: number;
  schoolId: number;
  firstName: string;
  lastName: string;
  phone: string;
  licenseNumber: string;
  licenseExpiry: string | null;
  insurancePolicyId: number | null;
  baseSalaryMonthly: string | null;
  employmentStatus: DriverEmploymentStatus;
}

export interface Parent {
  id: number;
  invitedBySchoolId: number;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  address: string | null;
  homeLocation: GeoPoint | null;
}

export type ChildStatus = 'ACTIVE' | 'INACTIVE';

export interface Child {
  id: number;
  schoolId: number;
  primaryParentId: number;
  secondaryParentId: number | null;
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;
  classSection: string | null;
  routeId: number | null;
  routeStopId: number | null;
  insurancePolicyId: number | null;
  status: ChildStatus;
}
