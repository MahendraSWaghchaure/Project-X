import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createChildAsParent, listMyChildren } from '@/lib/api/children';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';

const schema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  dateOfBirth: z.string().optional(),
  classSection: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

export function ParentChildrenPage() {
  const queryClient = useQueryClient();
  const { data: children, isLoading } = useQuery({ queryKey: ['my-children'], queryFn: listMyChildren });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) => createChildAsParent(values),
    onSuccess: (child) => {
      queryClient.invalidateQueries({ queryKey: ['my-children'] });
      reset();
      toast.success(`${child.firstName} added`);
    },
    onError: () => toast.error('Could not add child'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">My Children</h1>

      <Card>
        <CardHeader>
          <CardTitle>Register a child</CardTitle>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={handleSubmit((values) => createMutation.mutate(values))}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <Input label="First name" error={errors.firstName?.message} {...register('firstName')} />
            <Input label="Last name" error={errors.lastName?.message} {...register('lastName')} />
            <Input label="Date of birth" type="date" {...register('dateOfBirth')} />
            <Input label="Class / section" {...register('classSection')} />
            {createMutation.isError && <p className="sm:col-span-2 text-sm text-red-600">Could not add child.</p>}
            <Button type="submit" isLoading={isSubmitting} className="sm:col-span-2 sm:w-fit">
              Add child
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Children</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {children?.map((child) => (
              <li key={child.id} className="py-3">
                <p className="font-medium text-slate-900">
                  {child.firstName} {child.lastName}
                </p>
                <p className="text-sm text-slate-500">
                  {child.routeId ? `Assigned to route #${child.routeId}` : 'Not yet assigned to a route by the school'}
                </p>
              </li>
            ))}
            {children?.length === 0 && <p className="py-3 text-sm text-slate-500">No children registered yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
