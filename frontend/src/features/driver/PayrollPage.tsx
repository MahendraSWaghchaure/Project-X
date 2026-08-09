import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getMyBankDetails, listMyPayroll, upsertMyBankDetails } from '@/lib/api/payments';
import { toast } from '@/lib/toastStore';
import { backendErrorMessage } from '@/lib/errorMessage';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

const schema = z.object({
  accountHolderName: z.string().min(1),
  accountNumber: z.string().regex(/^\d{6,20}$/, 'must be 6-20 digits'),
  ifscCode: z
    .string()
    .regex(/^[A-Z]{4}0[A-Z0-9]{6}$/, 'must be a valid IFSC code, e.g. HDFC0001234')
    .transform((v) => v.toUpperCase()),
});
type FormValues = z.infer<typeof schema>;

export function DriverPayrollPage() {
  const queryClient = useQueryClient();
  const { data: bankDetails, isLoading: bankLoading } = useQuery({
    queryKey: ['my-bank-details'],
    queryFn: getMyBankDetails,
    retry: false,
  });
  const { data: payroll, isLoading: payrollLoading } = useQuery({ queryKey: ['my-payroll'], queryFn: listMyPayroll });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (bankDetails) {
      reset({ accountHolderName: bankDetails.accountHolderName, accountNumber: '', ifscCode: bankDetails.ifscCode });
    }
  }, [bankDetails, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => upsertMyBankDetails(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-bank-details'] });
      toast.success('Bank details saved');
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not save bank details')),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Payroll</h1>

      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Bank details</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Required to receive your monthly salary, disbursed on the 10th of each month.
          </p>
        </CardHeader>
        <CardBody>
          {bankLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          {bankDetails && (
            <p className="mb-3 text-sm text-slate-600">
              Currently on file: {bankDetails.accountHolderName}, account ending {bankDetails.maskedAccountNumber.slice(-4)},{' '}
              {bankDetails.ifscCode}
            </p>
          )}
          <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
            <Input
              label="Account holder name"
              error={errors.accountHolderName?.message}
              {...register('accountHolderName')}
            />
            <Input
              label="Account number"
              error={errors.accountNumber?.message}
              placeholder={bankDetails ? 're-enter to change' : undefined}
              {...register('accountNumber')}
            />
            <Input label="IFSC code" error={errors.ifscCode?.message} placeholder="HDFC0001234" {...register('ifscCode')} />
            {mutation.isError && (
              <p className="text-sm text-red-600">{backendErrorMessage(mutation.error, 'Could not save bank details')}</p>
            )}
            <Button type="submit" isLoading={isSubmitting} className="w-fit">
              Save bank details
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Salary history</CardTitle>
        </CardHeader>
        <CardBody>
          {payrollLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {payroll?.map((record) => (
              <li key={record.id} className="py-3">
                <div className="flex items-center justify-between gap-4">
                  <p className="font-medium text-slate-900">{record.periodStart}</p>
                  <Badge tone={record.status === 'PAID' ? 'success' : record.status === 'FAILED' ? 'danger' : 'neutral'}>
                    {record.status}
                  </Badge>
                </div>
                <p className="text-sm text-slate-500">
                  Base ₹{record.baseSalary.toFixed(2)}
                  {record.incentiveAmount > 0 && ` + incentive ₹${record.incentiveAmount.toFixed(2)}`}
                  {record.totalPaid != null && ` = ₹${record.totalPaid.toFixed(2)} paid`}
                </p>
                {record.transactionReference && (
                  <p className="text-sm text-slate-500">
                    Transaction #{record.transactionReference}
                    {record.disbursedAt && ` · ${new Date(record.disbursedAt).toLocaleString()}`}
                  </p>
                )}
                {record.failureReason && <p className="text-sm text-red-600">{record.failureReason}</p>}
              </li>
            ))}
            {payroll?.length === 0 && <p className="py-3 text-sm text-slate-500">No payroll history yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
