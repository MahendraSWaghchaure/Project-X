export type FeeInvoiceStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';
export type PaymentStatus = 'CREATED' | 'SUCCESS' | 'FAILED';
export type PaymentMode = 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET' | 'OTHER';
export type WaiverStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type PayrollStatus = 'PENDING' | 'PROCESSING' | 'PAID' | 'FAILED';

export interface FeeSchedule {
  id: number;
  schoolId: number;
  monthlyAmount: number;
}

export interface FeeInvoice {
  id: number;
  childId: number;
  parentId: number;
  schoolId: number;
  billingMonth: string;
  baseAmount: number;
  discountAmount: number;
  lateFeeAmount: number;
  totalAmount: number;
  dueDate: string;
  status: FeeInvoiceStatus;
  lateFeeWaived: boolean;
  paymentId: number | null;
}

export interface CreatePaymentOrder {
  paymentId: number;
  razorpayOrderId: string;
  razorpayKeyId: string;
  amount: number;
  discountAmount: number;
  monthsCovered: number;
}

export interface Payment {
  id: number;
  parentId: number;
  schoolId: number;
  amount: number;
  discountAmount: number;
  monthsCovered: number;
  mode: PaymentMode | null;
  status: PaymentStatus;
  receiptNumber: string | null;
  invoiceIds: number[];
  createdAt: string;
}

export interface WaiverRequest {
  id: number;
  feeInvoiceId: number;
  parentId: number;
  schoolId: number;
  reason: string;
  status: WaiverStatus;
  reviewedByUserId: number | null;
  reviewedAt: string | null;
  reviewNote: string | null;
  createdAt: string;
}

export interface BankDetail {
  id: number;
  driverId: number;
  accountHolderName: string;
  maskedAccountNumber: string;
  ifscCode: string;
}

export interface PayrollRecord {
  id: number;
  driverId: number;
  periodStart: string;
  baseSalary: number;
  overtimeHours: number;
  incentiveAmount: number;
  totalPaid: number | null;
  status: PayrollStatus;
  transactionReference: string | null;
  disbursedAt: string | null;
  failureReason: string | null;
}
