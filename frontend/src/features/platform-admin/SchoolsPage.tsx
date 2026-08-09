import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createSchool, listSchools, type CreateSchoolRequest } from '@/lib/api/schools';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

const schema = z.object({
  name: z.string().min(1),
  address: z.string().min(1),
  city: z.string().min(1),
  contactPhone: z.string().optional(),
  contactEmail: z.string().email().optional().or(z.literal('')),
  adminFirstName: z.string().min(1),
  adminLastName: z.string().min(1),
  adminEmail: z.string().email(),
  adminPassword: z.string().min(8),
});
type FormValues = z.infer<typeof schema>;

export function SchoolsPage() {
  const queryClient = useQueryClient();
  const { data: schools, isLoading } = useQuery({ queryKey: ['schools'], queryFn: listSchools });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const createMutation = useMutation({
    mutationFn: (values: CreateSchoolRequest) => createSchool(values),
    onSuccess: (school) => {
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      reset();
      toast.success(`${school.name} created`);
    },
    onError: () => toast.error('Could not create school'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Schools</h1>

      <Card>
        <CardHeader>
          <CardTitle>Onboard a new school</CardTitle>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={handleSubmit((values) => createMutation.mutate(values))}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <Input label="School name" error={errors.name?.message} {...register('name')} />
            <Input label="City" error={errors.city?.message} {...register('city')} />
            <Input label="Address" className="sm:col-span-2" error={errors.address?.message} {...register('address')} />
            <Input label="Contact phone" {...register('contactPhone')} />
            <Input label="Contact email" type="email" {...register('contactEmail')} />
            <div className="sm:col-span-2 border-t border-slate-100 pt-4 text-sm font-medium text-slate-500">
              First school admin login
            </div>
            <Input label="Admin first name" error={errors.adminFirstName?.message} {...register('adminFirstName')} />
            <Input label="Admin last name" error={errors.adminLastName?.message} {...register('adminLastName')} />
            <Input label="Admin email" type="email" error={errors.adminEmail?.message} {...register('adminEmail')} />
            <Input
              label="Admin temporary password"
              type="password"
              error={errors.adminPassword?.message}
              {...register('adminPassword')}
            />
            {createMutation.isError && (
              <p className="sm:col-span-2 text-sm text-red-600">Could not create school. Check the fields and try again.</p>
            )}
            <Button type="submit" isLoading={isSubmitting} className="sm:col-span-2 sm:w-fit">
              Create school
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>All schools</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {schools?.map((school) => (
              <li key={school.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium text-slate-900">{school.name}</p>
                  <p className="text-sm text-slate-500">
                    {school.city}
                    {school.address ? ` — ${school.address}` : ''}
                  </p>
                </div>
                <Badge tone={school.status === 'ACTIVE' ? 'success' : 'warning'}>{school.status}</Badge>
              </li>
            ))}
            {schools?.length === 0 && <p className="py-3 text-sm text-slate-500">No schools yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
