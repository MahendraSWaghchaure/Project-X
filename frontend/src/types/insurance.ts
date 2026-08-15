export type InsuredSubjectType = 'CHILD' | 'DRIVER' | 'VEHICLE';
export type InsurancePolicyStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export interface InsurancePolicy {
  id: number;
  schoolId: number;
  subjectType: InsuredSubjectType;
  subjectId: number;
  provider: string;
  policyNumber: string;
  coverageAmount: number;
  startDate: string;
  endDate: string;
  status: InsurancePolicyStatus;
}
