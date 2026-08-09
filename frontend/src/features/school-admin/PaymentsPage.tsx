import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getFeeSchedule,
  listFeeInvoicesForSchool,
  listPayrollForSchool,
  listPaymentsForSchool,
  setDriverIncentive,
  upsertFeeSchedule,
} from '@/lib/api/payments';
import { listDrivers } from '@/lib/api/drivers';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { backendErrorMessage } from '@/lib/errorMessage';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { PayrollRecord } from '@/types/payment';

function IncentiveEditor({ schoolId, driverId, record }: { schoolId: number; driverId: number; record?: PayrollRecord }) {
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState(String(record?.incentiveAmount ?? 0));

  const mutation = useMutation({
    mutationFn: () => setDriverIncentive(schoolId, driverId, Number(amount)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['school-payroll', schoolId] });
      toast.success('Incentive saved');
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not save incentive')),
  });

  return (
    <div className="flex items-center gap-2">
      <input
        type="number"
        min="0"
        step="0.01"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        className="w-28 rounded-md border border-slate-300 px-2 py-1 text-sm"
      />
      <Button variant="secondary" isLoading={mutation.isPending} onClick={() => mutation.mutate()}>
        Set incentive
      </Button>
    </div>
  );
}

export function PaymentsPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const queryClient = useQueryClient();
  const [monthlyAmount, setMonthlyAmount] = useState('');

  const { data: schedule } = useQuery({
    queryKey: ['fee-schedule', schoolId],
    queryFn: () => getFeeSchedule(schoolId!),
    enabled: schoolId != null,
    retry: false,
  });
  const { data: invoices, isLoading: invoicesLoading } = useQuery({
    queryKey: ['school-fee-invoices', schoolId],
    queryFn: () => listFeeInvoicesForSchool(schoolId!),
    enabled: schoolId != null,
  });
  const { data: payments, isLoading: paymentsLoading } = useQuery({
    queryKey: ['school-payments', schoolId],
    queryFn: () => listPaymentsForSchool(schoolId!),
    enabled: schoolId != null,
  });
  const { data: payroll, isLoading: payrollLoading } = useQuery({
    queryKey: ['school-payroll', schoolId],
    queryFn: () => listPayrollForSchool(schoolId!),
    enabled: schoolId != null,
  });
  const { data: drivers } = useQuery({
    queryKey: ['drivers', schoolId],
    queryFn: () => listDrivers(schoolId!),
    enabled: schoolId != null,
  });

  useEffect(() => {
    if (schedule) {
      setMonthlyAmount(String(schedule.monthlyAmount));
    }
  }, [schedule]);

  const scheduleMutation = useMutation({
    mutationFn: () => upsertFeeSchedule(schoolId!, Number(monthlyAmount)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-schedule', schoolId] });
      toast.success('Fee schedule saved');
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not save fee schedule')),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Payments</h1>

      <Card>
        <CardHeader>
          <CardTitle>Monthly fee</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Applied to every enrolled child. Invoices are generated on the 1st, due the 7th, with a ₹50/day late fee after that.
          </p>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              scheduleMutation.mutate();
            }}
            className="flex flex-wrap items-end gap-3"
          >
            <Input
              label="Monthly amount (₹)"
              type="number"
              min="1"
              step="0.01"
              value={monthlyAmount}
              onChange={(e) => setMonthlyAmount(e.target.value)}
              className="w-48"
            />
            <Button type="submit" isLoading={scheduleMutation.isPending}>
              Save
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Fee invoices</CardTitle>
        </CardHeader>
        <CardBody>
          {invoicesLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {invoices?.map((invoice) => (
              <li key={invoice.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className="font-medium text-slate-900">Child #{invoice.childId} &middot; {invoice.billingMonth}</p>
                  <p className="text-sm text-slate-500">
                    ₹{invoice.totalAmount.toFixed(2)} total (₹{invoice.baseAmount.toFixed(2)} base
                    {invoice.lateFeeAmount > 0 && ` + ₹${invoice.lateFeeAmount.toFixed(2)} late fee`}
                    {invoice.discountAmount > 0 && ` − ₹${invoice.discountAmount.toFixed(2)} discount`}) · due {invoice.dueDate}
                  </p>
                </div>
                <Badge
                  tone={
                    invoice.status === 'PAID'
                      ? 'success'
                      : invoice.status === 'OVERDUE'
                        ? 'danger'
                        : invoice.status === 'CANCELLED'
                          ? 'neutral'
                          : 'warning'
                  }
                >
                  {invoice.status}
                </Badge>
              </li>
            ))}
            {invoices?.length === 0 && <p className="py-3 text-sm text-slate-500">No invoices generated yet.</p>}
          </ul>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Payments received</CardTitle>
        </CardHeader>
        <CardBody>
          {paymentsLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {payments?.map((payment) => (
              <li key={payment.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className="font-medium text-slate-900">₹{payment.amount.toFixed(2)}</p>
                  <p className="text-sm text-slate-500">
                    {payment.receiptNumber ?? 'Pending'} · {payment.mode ?? 'mode unknown'} ·{' '}
                    {new Date(payment.createdAt).toLocaleString()}
                  </p>
                </div>
                <Badge tone={payment.status === 'SUCCESS' ? 'success' : payment.status === 'FAILED' ? 'danger' : 'neutral'}>
                  {payment.status}
                </Badge>
              </li>
            ))}
            {payments?.length === 0 && <p className="py-3 text-sm text-slate-500">No payments received yet.</p>}
          </ul>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Driver payroll</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Salaries disburse on the 10th of each month. Set an incentive (e.g. for covering another driver's route)
            before the run.
          </p>
        </CardHeader>
        <CardBody>
          {payrollLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {drivers?.map((driver) => {
              const record = payroll?.find((p) => p.driverId === driver.id);
              return (
                <li key={driver.id} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-medium text-slate-900">
                      {driver.firstName} {driver.lastName}
                    </p>
                    <p className="text-sm text-slate-500">
                      {record
                        ? `${record.periodStart} · base ₹${record.baseSalary.toFixed(2)} + incentive ₹${record.incentiveAmount.toFixed(2)}`
                        : 'No payroll record for this period yet'}
                      {record?.transactionReference && ` · txn ${record.transactionReference}`}
                    </p>
                    {record?.failureReason && <p className="text-xs text-red-600">{record.failureReason}</p>}
                  </div>
                  <div className="flex items-center gap-2">
                    {record && (
                      <Badge
                        tone={
                          record.status === 'PAID' ? 'success' : record.status === 'FAILED' ? 'danger' : 'neutral'
                        }
                      >
                        {record.status}
                      </Badge>
                    )}
                    <IncentiveEditor schoolId={schoolId!} driverId={driver.id} record={record} />
                  </div>
                </li>
              );
            })}
            {drivers?.length === 0 && <p className="py-3 text-sm text-slate-500">No drivers yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
