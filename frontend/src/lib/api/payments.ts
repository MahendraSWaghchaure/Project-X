import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import type {
  BankDetail,
  CreatePaymentOrder,
  FeeInvoice,
  FeeSchedule,
  Payment,
  PayrollRecord,
  WaiverRequest,
} from '@/types/payment';

// --- Fee schedule ---

export function upsertFeeSchedule(schoolId: number, monthlyAmount: number): Promise<FeeSchedule> {
  return apiClient.put<ApiResponse<FeeSchedule>>(`/schools/${schoolId}/fee-schedule`, { monthlyAmount }).then(unwrap);
}

export function getFeeSchedule(schoolId: number): Promise<FeeSchedule> {
  return apiClient.get<ApiResponse<FeeSchedule>>(`/schools/${schoolId}/fee-schedule`).then(unwrap);
}

// --- Fee invoices ---

export function listMyFeeInvoices(): Promise<FeeInvoice[]> {
  return apiClient.get<ApiResponse<FeeInvoice[]>>('/parents/me/fee-invoices').then(unwrap);
}

export function listFeeInvoicesForSchool(schoolId: number): Promise<FeeInvoice[]> {
  return apiClient.get<ApiResponse<FeeInvoice[]>>(`/schools/${schoolId}/fee-invoices`).then(unwrap);
}

// --- Payments / checkout ---

export function createPaymentOrder(feeInvoiceIds: number[]): Promise<CreatePaymentOrder> {
  return apiClient.post<ApiResponse<CreatePaymentOrder>>('/parents/me/payments/orders', { feeInvoiceIds }).then(unwrap);
}

export function createAdvancePaymentOrder(childId: number, months: number): Promise<CreatePaymentOrder> {
  return apiClient.post<ApiResponse<CreatePaymentOrder>>('/parents/me/payments/advance-orders', { childId, months }).then(unwrap);
}

export interface VerifyPaymentPayload {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

export function verifyPayment(paymentId: number, payload: VerifyPaymentPayload): Promise<Payment> {
  return apiClient.post<ApiResponse<Payment>>(`/parents/me/payments/${paymentId}/verify`, payload).then(unwrap);
}

export function listMyPayments(): Promise<Payment[]> {
  return apiClient.get<ApiResponse<Payment[]>>('/parents/me/payments').then(unwrap);
}

export function getMyReceipt(paymentId: number): Promise<Payment> {
  return apiClient.get<ApiResponse<Payment>>(`/parents/me/payments/${paymentId}`).then(unwrap);
}

export function listPaymentsForSchool(schoolId: number): Promise<Payment[]> {
  return apiClient.get<ApiResponse<Payment[]>>(`/schools/${schoolId}/payments`).then(unwrap);
}

// --- Late fee waivers ---

export function createWaiverRequest(feeInvoiceId: number, reason: string): Promise<WaiverRequest> {
  return apiClient.post<ApiResponse<WaiverRequest>>('/parents/me/late-fee-waivers', { feeInvoiceId, reason }).then(unwrap);
}

export function listMyWaiverRequests(): Promise<WaiverRequest[]> {
  return apiClient.get<ApiResponse<WaiverRequest[]>>('/parents/me/late-fee-waivers').then(unwrap);
}

export function listAllWaiverRequests(): Promise<WaiverRequest[]> {
  return apiClient.get<ApiResponse<WaiverRequest[]>>('/late-fee-waivers').then(unwrap);
}

export function reviewWaiverRequest(waiverId: number, status: 'APPROVED' | 'REJECTED', reviewNote?: string): Promise<WaiverRequest> {
  return apiClient.patch<ApiResponse<WaiverRequest>>(`/late-fee-waivers/${waiverId}/review`, { status, reviewNote }).then(unwrap);
}

// --- Driver bank details ---

export interface UpsertBankDetailsPayload {
  accountHolderName: string;
  accountNumber: string;
  ifscCode: string;
}

export function upsertMyBankDetails(payload: UpsertBankDetailsPayload): Promise<BankDetail> {
  return apiClient.put<ApiResponse<BankDetail>>('/drivers/me/bank-details', payload).then(unwrap);
}

export function getMyBankDetails(): Promise<BankDetail> {
  return apiClient.get<ApiResponse<BankDetail>>('/drivers/me/bank-details').then(unwrap);
}

// --- Payroll ---

export function listMyPayroll(): Promise<PayrollRecord[]> {
  return apiClient.get<ApiResponse<PayrollRecord[]>>('/drivers/me/payroll').then(unwrap);
}

export function listPayrollForSchool(schoolId: number): Promise<PayrollRecord[]> {
  return apiClient.get<ApiResponse<PayrollRecord[]>>(`/schools/${schoolId}/payroll`).then(unwrap);
}

export function setDriverIncentive(schoolId: number, driverId: number, incentiveAmount: number): Promise<PayrollRecord> {
  return apiClient
    .patch<ApiResponse<PayrollRecord>>(`/schools/${schoolId}/payroll/drivers/${driverId}/incentive`, { incentiveAmount })
    .then(unwrap);
}

// --- Billing admin (manual triggers) ---

export function triggerGenerateInvoices(): Promise<void> {
  return apiClient.post<ApiResponse<void>>('/billing/generate-invoices').then(unwrap);
}

export function triggerAccrueLateFees(): Promise<void> {
  return apiClient.post<ApiResponse<void>>('/billing/accrue-late-fees').then(unwrap);
}

export function triggerRunPayroll(): Promise<void> {
  return apiClient.post<ApiResponse<void>>('/billing/run-payroll').then(unwrap);
}
