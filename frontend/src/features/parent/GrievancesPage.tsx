import { useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listMyChildren } from '@/lib/api/children';
import { listMyGrievances, raiseGrievance } from '@/lib/api/grievances';
import { useAuthStore } from '@/lib/authStore';
import { useLiveTopic } from '@/hooks/useLiveTopic';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { GrievanceStatusBadge } from '@/components/grievance/GrievanceStatusBadge';

const CATEGORIES = ['SAFETY', 'VEHICLE_CONDITION', 'DRIVER_CONDUCT', 'PAYMENT', 'DELAY', 'OTHER'] as const;

const schema = z.object({
  schoolId: z.coerce.number({ required_error: 'Choose which school this is about' }),
  category: z.enum(CATEGORIES),
  subject: z.string().min(1),
  description: z.string().min(1),
});
type FormValues = z.infer<typeof schema>;

export function ParentGrievancesPage() {
  const userId = useAuthStore((state) => state.userId);
  const queryClient = useQueryClient();
  const { data: children } = useQuery({ queryKey: ['my-children'], queryFn: listMyChildren });
  const { data: grievances, isLoading } = useQuery({ queryKey: ['my-grievances'], queryFn: listMyGrievances });

  const schoolOptions = useMemo(() => {
    const ids = new Set<number>();
    children?.forEach((c) => ids.add(c.schoolId));
    return Array.from(ids);
  }, [children]);

  useLiveTopic([userId ? `/topic/users/${userId}/grievances` : null], () => {
    queryClient.invalidateQueries({ queryKey: ['my-grievances'] });
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { category: 'OTHER' } });

  const raiseMutation = useMutation({
    mutationFn: (values: FormValues) => raiseGrievance(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-grievances'] });
      reset();
      toast.success('Grievance submitted');
    },
    onError: () => toast.error('Could not submit grievance'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Grievances</h1>

      <Card>
        <CardHeader>
          <CardTitle>Raise a grievance</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Something wrong with a trip, the vehicle, or a driver's conduct? Tell your child's school directly.
          </p>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={handleSubmit((values) => raiseMutation.mutate(values))}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">School</label>
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" {...register('schoolId')}>
                <option value="">Select&hellip;</option>
                {schoolOptions.map((id) => (
                  <option key={id} value={id}>
                    School #{id}
                  </option>
                ))}
              </select>
              {errors.schoolId && <span className="text-xs text-red-600">{errors.schoolId.message}</span>}
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Category</label>
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" {...register('category')}>
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </div>
            <Input label="Subject" className="sm:col-span-2" error={errors.subject?.message} {...register('subject')} />
            <div className="flex flex-col gap-1 sm:col-span-2">
              <label className="text-sm font-medium text-slate-700">Description</label>
              <textarea
                rows={4}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                {...register('description')}
              />
              {errors.description && <span className="text-xs text-red-600">{errors.description.message}</span>}
            </div>
            {raiseMutation.isError && <p className="sm:col-span-2 text-sm text-red-600">Could not submit grievance.</p>}
            <Button type="submit" isLoading={isSubmitting} className="sm:col-span-2 sm:w-fit">
              Submit
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Your grievances</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {grievances?.map((g) => (
              <li key={g.id} className="flex flex-col gap-1 py-3">
                <div className="flex items-center justify-between gap-4">
                  <p className="font-medium text-slate-900">{g.subject}</p>
                  <GrievanceStatusBadge status={g.status} />
                </div>
                <p className="text-sm text-slate-500">
                  {g.category.replace('_', ' ')} &middot; {new Date(g.createdAt).toLocaleString()}
                </p>
                <p className="text-sm text-slate-600">{g.description}</p>
                {g.resolutionNote && (
                  <p className="mt-1 rounded-md bg-slate-50 p-2 text-sm text-slate-700">
                    <span className="font-medium">School's response: </span>
                    {g.resolutionNote}
                  </p>
                )}
              </li>
            ))}
            {grievances?.length === 0 && <p className="py-3 text-sm text-slate-500">No grievances raised yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
