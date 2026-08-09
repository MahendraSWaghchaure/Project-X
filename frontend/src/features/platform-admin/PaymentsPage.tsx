import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  listAllWaiverRequests,
  reviewWaiverRequest,
  triggerAccrueLateFees,
  triggerGenerateInvoices,
  triggerRunPayroll,
} from '@/lib/api/payments';
import { toast } from '@/lib/toastStore';
import { backendErrorMessage } from '@/lib/errorMessage';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { WaiverRequest } from '@/types/payment';

function WaiverReviewRow({ waiver }: { waiver: WaiverRequest }) {
  const queryClient = useQueryClient();
  const [note, setNote] = useState('');

  const mutation = useMutation({
    mutationFn: (status: 'APPROVED' | 'REJECTED') => reviewWaiverRequest(waiver.id, status, note || undefined),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['all-waivers'] });
      toast.success(`Waiver ${updated.status.toLowerCase()}`);
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not review waiver')),
  });

  return (
    <li className="py-3">
      <div className="flex items-center justify-between gap-4">
        <p className="font-medium text-slate-900">
          Invoice #{waiver.feeInvoiceId} &middot; Parent #{waiver.parentId} &middot; School #{waiver.schoolId}
        </p>
        <Badge tone={waiver.status === 'PENDING' ? 'warning' : waiver.status === 'APPROVED' ? 'success' : 'danger'}>
          {waiver.status}
        </Badge>
      </div>
      <p className="mt-1 text-sm text-slate-600">{waiver.reason}</p>
      {waiver.status === 'PENDING' ? (
        <div className="mt-2 flex flex-wrap items-end gap-2 border-t border-slate-100 pt-2">
          <input
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Review note (optional)"
            className="flex-1 rounded-md border border-slate-300 px-3 py-1.5 text-sm"
          />
          <Button isLoading={mutation.isPending} onClick={() => mutation.mutate('APPROVED')}>
            Approve
          </Button>
          <Button variant="danger" isLoading={mutation.isPending} onClick={() => mutation.mutate('REJECTED')}>
            Reject
          </Button>
        </div>
      ) : (
        waiver.reviewNote && <p className="mt-1 text-sm text-slate-500">Review note: {waiver.reviewNote}</p>
      )}
    </li>
  );
}

export function PlatformAdminPaymentsPage() {
  const { data: waivers, isLoading } = useQuery({ queryKey: ['all-waivers'], queryFn: listAllWaiverRequests });

  const generateInvoicesMutation = useMutation({
    mutationFn: triggerGenerateInvoices,
    onSuccess: () => toast.success('Monthly invoice generation run'),
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not run invoice generation')),
  });
  const accrueLateFeesMutation = useMutation({
    mutationFn: triggerAccrueLateFees,
    onSuccess: () => toast.success('Late fee accrual run'),
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not run late fee accrual')),
  });
  const runPayrollMutation = useMutation({
    mutationFn: triggerRunPayroll,
    onSuccess: () => toast.success('Payroll run triggered'),
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not run payroll')),
  });

  const pendingWaivers = waivers?.filter((w) => w.status === 'PENDING') ?? [];
  const reviewedWaivers = waivers?.filter((w) => w.status !== 'PENDING') ?? [];

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Payments</h1>

      <Card>
        <CardHeader>
          <CardTitle>Billing jobs</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            These normally run on a schedule (invoices on the 1st, late fees daily, payroll on the 10th) - trigger them
            manually here if needed.
          </p>
        </CardHeader>
        <CardBody className="flex flex-wrap gap-3">
          <Button variant="secondary" isLoading={generateInvoicesMutation.isPending} onClick={() => generateInvoicesMutation.mutate()}>
            Generate this month's invoices
          </Button>
          <Button variant="secondary" isLoading={accrueLateFeesMutation.isPending} onClick={() => accrueLateFeesMutation.mutate()}>
            Run late fee accrual
          </Button>
          <Button variant="secondary" isLoading={runPayrollMutation.isPending} onClick={() => runPayrollMutation.mutate()}>
            Run driver payroll
          </Button>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Late-fee waiver requests</CardTitle>
          <p className="mt-1 text-sm text-slate-500">Reviewed by the platform admin, not individual schools.</p>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {pendingWaivers.map((waiver) => (
              <WaiverReviewRow key={waiver.id} waiver={waiver} />
            ))}
            {pendingWaivers.length === 0 && <p className="py-3 text-sm text-slate-500">No pending waiver requests.</p>}
          </ul>
          {reviewedWaivers.length > 0 && (
            <>
              <p className="mt-4 mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">Previously reviewed</p>
              <ul className="flex flex-col divide-y divide-slate-100">
                {reviewedWaivers.map((waiver) => (
                  <WaiverReviewRow key={waiver.id} waiver={waiver} />
                ))}
              </ul>
            </>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
