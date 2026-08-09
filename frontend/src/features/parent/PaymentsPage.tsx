import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createAdvancePaymentOrder,
  createPaymentOrder,
  createWaiverRequest,
  listMyFeeInvoices,
  listMyPayments,
  verifyPayment,
} from '@/lib/api/payments';
import { listMyChildren } from '@/lib/api/children';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { backendErrorMessage } from '@/lib/errorMessage';
import { openRazorpayCheckout } from '@/lib/razorpayCheckout';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { FeeInvoice } from '@/types/payment';

const DISCOUNT_TIERS: { months: number; percent: number }[] = [
  { months: 3, percent: 5 },
  { months: 6, percent: 8 },
  { months: 12, percent: 12 },
];

function discountPercentFor(months: number): number {
  return DISCOUNT_TIERS.filter((t) => months >= t.months).reduce((max, t) => Math.max(max, t.percent), 0);
}

function invoiceStatusTone(invoice: FeeInvoice): 'success' | 'warning' | 'danger' | 'neutral' {
  if (invoice.status === 'PAID') return 'success';
  if (invoice.status === 'OVERDUE') return 'danger';
  if (invoice.status === 'CANCELLED') return 'neutral';
  return 'warning';
}

function WaiverRequestForm({ invoice, onDone }: { invoice: FeeInvoice; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');

  const mutation = useMutation({
    mutationFn: () => createWaiverRequest(invoice.id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-waivers'] });
      toast.success('Waiver request submitted for review');
      onDone();
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not submit waiver request')),
  });

  return (
    <div className="mt-2 flex flex-col gap-2 rounded-md border border-amber-200 bg-amber-50/50 p-3">
      <label className="text-sm font-medium text-slate-700">
        Why was this payment late? (emergency reason for a late-fee waiver)
      </label>
      <textarea
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        rows={3}
        className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        placeholder="e.g. Family medical emergency, hospitalized from 3rd-6th..."
      />
      <div className="flex gap-2">
        <Button
          isLoading={mutation.isPending}
          disabled={reason.trim().length === 0}
          onClick={() => mutation.mutate()}
        >
          Submit for review
        </Button>
        <Button variant="secondary" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </div>
  );
}

export function ParentPaymentsPage() {
  const email = useAuthStore((state) => state.email);
  const queryClient = useQueryClient();
  const [payingInvoiceId, setPayingInvoiceId] = useState<number | null>(null);
  const [waiverInvoiceId, setWaiverInvoiceId] = useState<number | null>(null);
  const [advanceChildId, setAdvanceChildId] = useState<number | ''>('');
  const [advanceMonths, setAdvanceMonths] = useState(3);

  const { data: invoices, isLoading: invoicesLoading } = useQuery({ queryKey: ['my-fee-invoices'], queryFn: listMyFeeInvoices });
  const { data: payments, isLoading: paymentsLoading } = useQuery({ queryKey: ['my-payments'], queryFn: listMyPayments });
  const { data: children } = useQuery({ queryKey: ['my-children'], queryFn: listMyChildren });

  async function completeCheckout(order: { paymentId: number; razorpayOrderId: string; razorpayKeyId: string; amount: number }, description: string) {
    const result = await openRazorpayCheckout({
      keyId: order.razorpayKeyId,
      orderId: order.razorpayOrderId,
      amountInRupees: order.amount,
      description,
      prefillEmail: email ?? undefined,
    });
    await verifyPayment(order.paymentId, {
      razorpayOrderId: result.razorpay_order_id,
      razorpayPaymentId: result.razorpay_payment_id,
      razorpaySignature: result.razorpay_signature,
    });
    queryClient.invalidateQueries({ queryKey: ['my-fee-invoices'] });
    queryClient.invalidateQueries({ queryKey: ['my-payments'] });
    toast.success('Payment successful');
  }

  const payMutation = useMutation({
    mutationFn: async (invoice: FeeInvoice) => {
      const order = await createPaymentOrder([invoice.id]);
      await completeCheckout(order, `Fee for ${invoice.billingMonth}`);
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not start payment')),
    onSettled: () => setPayingInvoiceId(null),
  });

  const advancePayMutation = useMutation({
    mutationFn: async () => {
      if (advanceChildId === '') throw new Error('Select a child first');
      const order = await createAdvancePaymentOrder(advanceChildId, advanceMonths);
      await completeCheckout(order, `${advanceMonths} months advance payment`);
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not start advance payment')),
  });

  const discountPercent = discountPercentFor(advanceMonths);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Payments</h1>

      <Card>
        <CardHeader>
          <CardTitle>Your fees</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Due by the 7th of each month. A ₹50/day late fee applies after that until paid.
          </p>
        </CardHeader>
        <CardBody>
          {invoicesLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {invoices?.map((invoice) => (
              <li key={invoice.id} className="py-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-medium text-slate-900">{invoice.billingMonth}</p>
                    <p className="text-sm text-slate-500">
                      ₹{invoice.baseAmount.toFixed(2)}
                      {invoice.discountAmount > 0 && ` − ₹${invoice.discountAmount.toFixed(2)} discount`}
                      {invoice.lateFeeAmount > 0 && ` + ₹${invoice.lateFeeAmount.toFixed(2)} late fee`}
                      {' = '}
                      <span className="font-semibold">₹{invoice.totalAmount.toFixed(2)}</span>
                      {' · due '}
                      {invoice.dueDate}
                      {invoice.lateFeeWaived && ' · late fee waived'}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge tone={invoiceStatusTone(invoice)}>{invoice.status}</Badge>
                    {invoice.status !== 'PAID' && invoice.status !== 'CANCELLED' && (
                      <Button
                        isLoading={payMutation.isPending && payingInvoiceId === invoice.id}
                        onClick={() => {
                          setPayingInvoiceId(invoice.id);
                          payMutation.mutate(invoice);
                        }}
                      >
                        Pay ₹{invoice.totalAmount.toFixed(2)}
                      </Button>
                    )}
                    {invoice.lateFeeAmount > 0 && !invoice.lateFeeWaived && (
                      <Button
                        variant="secondary"
                        onClick={() => setWaiverInvoiceId(waiverInvoiceId === invoice.id ? null : invoice.id)}
                      >
                        Request waiver
                      </Button>
                    )}
                  </div>
                </div>
                {waiverInvoiceId === invoice.id && (
                  <WaiverRequestForm invoice={invoice} onDone={() => setWaiverInvoiceId(null)} />
                )}
              </li>
            ))}
            {invoices?.length === 0 && <p className="py-3 text-sm text-slate-500">No fees on file yet.</p>}
          </ul>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Pay in advance &amp; save</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Prepay several months at once for a discount: 3 months → 5% off, 6 months → 8% off, 12 months → 12% off.
          </p>
        </CardHeader>
        <CardBody>
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Child</label>
              <select
                value={advanceChildId}
                onChange={(e) => setAdvanceChildId(e.target.value ? Number(e.target.value) : '')}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Select&hellip;</option>
                {children?.map((child) => (
                  <option key={child.id} value={child.id}>
                    {child.firstName} {child.lastName}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Months</label>
              <select
                value={advanceMonths}
                onChange={(e) => setAdvanceMonths(Number(e.target.value))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                {[2, 3, 6, 12].map((m) => (
                  <option key={m} value={m}>
                    {m} months
                  </option>
                ))}
              </select>
            </div>
            <p className="text-sm text-slate-600">
              {discountPercent > 0 ? `${discountPercent}% off the total` : 'No discount below 3 months'}
            </p>
            <Button
              isLoading={advancePayMutation.isPending}
              disabled={advanceChildId === ''}
              onClick={() => advancePayMutation.mutate()}
            >
              Pay in advance
            </Button>
          </div>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Payment history &amp; receipts</CardTitle>
        </CardHeader>
        <CardBody>
          {paymentsLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {payments?.map((payment) => (
              <li key={payment.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className="font-medium text-slate-900">
                    ₹{payment.amount.toFixed(2)}
                    {payment.discountAmount > 0 && ` (saved ₹${payment.discountAmount.toFixed(2)})`}
                  </p>
                  <p className="text-sm text-slate-500">
                    {payment.receiptNumber ?? 'No receipt yet'} · {payment.mode ?? 'mode unknown'} ·{' '}
                    {new Date(payment.createdAt).toLocaleString()} · {payment.monthsCovered} month(s)
                  </p>
                </div>
                <Badge tone={payment.status === 'SUCCESS' ? 'success' : payment.status === 'FAILED' ? 'danger' : 'neutral'}>
                  {payment.status}
                </Badge>
              </li>
            ))}
            {payments?.length === 0 && <p className="py-3 text-sm text-slate-500">No payments yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
