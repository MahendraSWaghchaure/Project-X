import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createDriver, listDrivers } from '@/lib/api/drivers';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

const schema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(8),
  phone: z.string().min(1),
  licenseNumber: z.string().min(1),
  licenseExpiry: z.string().optional(),
  baseSalaryMonthly: z.coerce.number().optional(),
});
type FormValues = z.infer<typeof schema>;

export function DriversPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const queryClient = useQueryClient();
  const { data: drivers, isLoading } = useQuery({
    queryKey: ['drivers', schoolId],
    queryFn: () => listDrivers(schoolId!),
    enabled: schoolId != null,
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) => createDriver(schoolId!, values),
    onSuccess: (driver) => {
      queryClient.invalidateQueries({ queryKey: ['drivers', schoolId] });
      reset();
      toast.success(`${driver.firstName} ${driver.lastName} added`);
    },
    onError: () => toast.error('Could not add driver'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Drivers</h1>

      <Card>
        <CardHeader>
          <CardTitle>Add a driver</CardTitle>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={handleSubmit((values) => createMutation.mutate(values))}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <Input label="First name" error={errors.firstName?.message} {...register('firstName')} />
            <Input label="Last name" error={errors.lastName?.message} {...register('lastName')} />
            <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
            <Input label="Temporary password" type="password" error={errors.password?.message} {...register('password')} />
            <Input label="Phone" error={errors.phone?.message} {...register('phone')} />
            <Input label="License number" error={errors.licenseNumber?.message} {...register('licenseNumber')} />
            <Input label="License expiry" type="date" {...register('licenseExpiry')} />
            <Input label="Base monthly salary" type="number" {...register('baseSalaryMonthly')} />
            {createMutation.isError && <p className="sm:col-span-2 text-sm text-red-600">Could not add driver.</p>}
            <Button type="submit" isLoading={isSubmitting} className="sm:col-span-2 sm:w-fit">
              Add driver
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Roster</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {drivers?.map((driver) => (
              <li key={driver.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium text-slate-900">
                    {driver.firstName} {driver.lastName}
                  </p>
                  <p className="text-sm text-slate-500">
                    {driver.phone} &middot; License {driver.licenseNumber}
                  </p>
                </div>
                <Badge tone={driver.employmentStatus === 'ACTIVE' ? 'success' : 'warning'}>{driver.employmentStatus}</Badge>
              </li>
            ))}
            {drivers?.length === 0 && <p className="py-3 text-sm text-slate-500">No drivers yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
