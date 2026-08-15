import { useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createInsurancePolicy,
  linkChildInsurancePolicy,
  linkDriverInsurancePolicy,
  linkVehicleInsurancePolicy,
  listInsurancePolicies,
} from '@/lib/api/insurance';
import { listChildrenForSchool } from '@/lib/api/children';
import { listDrivers } from '@/lib/api/drivers';
import { listVehicles } from '@/lib/api/vehicles';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { backendErrorMessage } from '@/lib/errorMessage';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { InsuredSubjectType } from '@/types/insurance';

const schema = z.object({
  subjectType: z.enum(['CHILD', 'DRIVER', 'VEHICLE']),
  subjectId: z.string().min(1, 'Select who this policy covers'),
  provider: z.string().min(1, 'Provider is required'),
  policyNumber: z.string().min(1, 'Policy number is required'),
  coverageAmount: z.coerce.number().positive('Must be greater than 0'),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().min(1, 'End date is required'),
});
type FormValues = z.infer<typeof schema>;

function daysUntil(dateStr: string): number {
  const ms = new Date(dateStr).getTime() - new Date().setHours(0, 0, 0, 0);
  return Math.round(ms / (1000 * 60 * 60 * 24));
}

export function InsurancePage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const enabled = schoolId != null;
  const queryClient = useQueryClient();

  const { data: policies, isLoading } = useQuery({
    queryKey: ['insurance-policies', schoolId],
    queryFn: () => listInsurancePolicies(schoolId!),
    enabled,
  });
  const { data: children } = useQuery({ queryKey: ['children', schoolId], queryFn: () => listChildrenForSchool(schoolId!), enabled });
  const { data: drivers } = useQuery({ queryKey: ['drivers', schoolId], queryFn: () => listDrivers(schoolId!), enabled });
  const { data: vehicles } = useQuery({ queryKey: ['vehicles', schoolId], queryFn: () => listVehicles(schoolId!), enabled });

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { subjectType: 'CHILD' } });
  const subjectType = watch('subjectType');

  const subjectOptions = useMemo(() => {
    if (subjectType === 'CHILD') return (children ?? []).map((c) => ({ id: c.id, label: `${c.firstName} ${c.lastName}` }));
    if (subjectType === 'DRIVER') return (drivers ?? []).map((d) => ({ id: d.id, label: `${d.firstName} ${d.lastName}` }));
    return (vehicles ?? []).map((v) => ({ id: v.id, label: v.registrationNumber }));
  }, [subjectType, children, drivers, vehicles]);

  function subjectLabel(type: InsuredSubjectType, id: number): string {
    if (type === 'CHILD') {
      const c = children?.find((x) => x.id === id);
      return c ? `${c.firstName} ${c.lastName}` : `Child #${id}`;
    }
    if (type === 'DRIVER') {
      const d = drivers?.find((x) => x.id === id);
      return d ? `${d.firstName} ${d.lastName}` : `Driver #${id}`;
    }
    const v = vehicles?.find((x) => x.id === id);
    return v ? v.registrationNumber : `Vehicle #${id}`;
  }

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const policy = await createInsurancePolicy(schoolId!, {
        subjectType: values.subjectType,
        subjectId: Number(values.subjectId),
        provider: values.provider,
        policyNumber: values.policyNumber,
        coverageAmount: values.coverageAmount,
        startDate: values.startDate,
        endDate: values.endDate,
      });
      const subjectId = Number(values.subjectId);
      if (values.subjectType === 'CHILD') await linkChildInsurancePolicy(schoolId!, subjectId, policy.id);
      else if (values.subjectType === 'DRIVER') await linkDriverInsurancePolicy(schoolId!, subjectId, policy.id);
      else await linkVehicleInsurancePolicy(schoolId!, subjectId, policy.id);
      return policy;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['insurance-policies', schoolId] });
      queryClient.invalidateQueries({ queryKey: ['children', schoolId] });
      queryClient.invalidateQueries({ queryKey: ['drivers', schoolId] });
      queryClient.invalidateQueries({ queryKey: ['vehicles', schoolId] });
      toast.success('Insurance policy added and linked');
      reset();
    },
    onError: (error) => toast.error(backendErrorMessage(error, 'Could not save the insurance policy')),
  });

  const sortedPolicies = [...(policies ?? [])].sort((a, b) => a.endDate.localeCompare(b.endDate));
  const expiringSoon = sortedPolicies.filter((p) => p.status === 'ACTIVE' && daysUntil(p.endDate) <= 30);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Insurance</h1>

      {expiringSoon.length > 0 && (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {expiringSoon.length} polic{expiringSoon.length > 1 ? 'ies are' : 'y is'} expiring within 30 days:{' '}
          {expiringSoon.map((p) => subjectLabel(p.subjectType, p.subjectId)).join(', ')}.
        </div>
      )}

      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle>Add a policy</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Covers a child, driver, or vehicle. Saving both creates the policy record and links it in one step.
          </p>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-4">
              <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
                Covers
                <select
                  className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                  {...register('subjectType')}
                >
                  <option value="CHILD">Child</option>
                  <option value="DRIVER">Driver</option>
                  <option value="VEHICLE">Vehicle</option>
                </select>
              </label>
              <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
                Who
                <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" {...register('subjectId')}>
                  <option value="">Select&hellip;</option>
                  {subjectOptions.map((opt) => (
                    <option key={opt.id} value={opt.id}>
                      {opt.label}
                    </option>
                  ))}
                </select>
                {errors.subjectId && <span className="text-xs text-red-600">{errors.subjectId.message}</span>}
              </label>
            </div>
            <Input label="Insurance provider" error={errors.provider?.message} {...register('provider')} />
            <Input label="Policy number" error={errors.policyNumber?.message} {...register('policyNumber')} />
            <Input
              label="Coverage amount (₹)"
              type="number"
              step="0.01"
              error={errors.coverageAmount?.message}
              {...register('coverageAmount')}
            />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Start date" type="date" error={errors.startDate?.message} {...register('startDate')} />
              <Input label="End date" type="date" error={errors.endDate?.message} {...register('endDate')} />
            </div>
            {mutation.isError && (
              <p className="text-sm text-red-600">{backendErrorMessage(mutation.error, 'Could not save the insurance policy')}</p>
            )}
            <Button type="submit" isLoading={isSubmitting} className="w-fit">
              Add policy
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>All policies</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {sortedPolicies.map((policy) => {
              const remaining = daysUntil(policy.endDate);
              const expired = remaining < 0 || policy.status === 'EXPIRED';
              const tone = policy.status === 'CANCELLED' ? 'neutral' : expired ? 'danger' : remaining <= 30 ? 'warning' : 'success';
              return (
                <li key={policy.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium text-slate-900">
                      {subjectLabel(policy.subjectType, policy.subjectId)}{' '}
                      <span className="font-normal text-slate-400">
                        &middot; {policy.subjectType.charAt(0) + policy.subjectType.slice(1).toLowerCase()}
                      </span>
                    </p>
                    <p className="text-sm text-slate-500">
                      {policy.provider} &middot; {policy.policyNumber} &middot; &#8377;{policy.coverageAmount.toLocaleString('en-IN')}{' '}
                      cover
                    </p>
                    <p className="text-xs text-slate-400">
                      {policy.startDate} &rarr; {policy.endDate}
                    </p>
                  </div>
                  <Badge tone={tone}>{expired ? 'Expired' : remaining <= 30 ? `${remaining}d left` : policy.status}</Badge>
                </li>
              );
            })}
            {sortedPolicies.length === 0 && !isLoading && (
              <p className="py-3 text-sm text-slate-500">No insurance policies recorded yet.</p>
            )}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
