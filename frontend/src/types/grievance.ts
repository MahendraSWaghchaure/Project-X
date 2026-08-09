import type { Role } from './auth';

export type GrievanceCategory = 'SAFETY' | 'VEHICLE_CONDITION' | 'DRIVER_CONDUCT' | 'PAYMENT' | 'DELAY' | 'OTHER';
export type GrievanceStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';

export interface Grievance {
  id: number;
  schoolId: number | null;
  raisedByUserId: number;
  raisedByRole: Role;
  raisedByEmail: string;
  category: GrievanceCategory;
  subject: string;
  description: string;
  status: GrievanceStatus;
  resolutionNote: string | null;
  resolvedByUserId: number | null;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
