import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  claimDriver,
  createDriver,
  listDrivers,
  listIndependentDrivers,
  terminateDriver,
  updateDriver,
  type UpdateDriverRequest,
} from '@/lib/api/drivers';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { Driver } from '@/types/people';

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

const editSchema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  phone: z.string().min(1),
  licenseNumber: z.string().min(1),
  licenseExpiry: z.string().optional(),
  baseSalaryMonthly: z.coerce.number().optional(),
});
type EditFormValues = z.infer<typeof editSchema>;

function EditDriverRow({ schoolId, driver, onDone }: { schoolId: number; driver: Driver; onDone: () => void }) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      firstName: driver.firstName,
      lastName: driver.lastName,
      phone: driver.phone,
      licenseNumber: driver.licenseNumber,
      licenseExpiry: driver.licenseExpiry ?? undefined,
      baseSalaryMonthly: driver.baseSalaryMonthly ? Number(driver.baseSalaryMonthly) : undefined,
    },
  });

  const updateMutation = useMutation({
    mutationFn: (values: UpdateDriverRequest) => updateDriver(schoolId, driver.id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['drivers', schoolId] });
      toast.success('Driver updated');
      onDone();
    },
    onError: () => toast.error('Could not update driver'),
  });

  return (
    <form
      onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
      className="grid grid-cols-1 gap-3 rounded-md border border-blue-200 bg-blue-50/40 p-3 sm:grid-cols-2"
    >
      <Input id={`driver-first-name-${driver.id}`} label="First name" error={errors.firstName?.message} {...register('firstName')} />
      <Input id={`driver-last-name-${driver.id}`} label="Last name" error={errors.lastName?.message} {...register('lastName')} />
      <Input id={`driver-phone-${driver.id}`} label="Phone" error={errors.phone?.message} {...register('phone')} />
      <Input
        id={`driver-license-number-${driver.id}`}
        label="License number"
        error={errors.licenseNumber?.message}
        {...register('licenseNumber')}
      />
      <Input id={`driver-license-expiry-${driver.id}`} label="License expiry" type="date" {...register('licenseExpiry')} />
      <Input
        id={`driver-base-salary-${driver.id}`}
        label="Base monthly salary"
        type="number"
        {...register('baseSalaryMonthly')}
      />
      <div className="flex gap-2 sm:col-span-2">
        <Button type="submit" isLoading={isSubmitting}>
          Save
        </Button>
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

function IndependentDriversCard({ schoolId }: { schoolId: number }) {
  const queryClient = useQueryClient();
  const { data: independentDrivers, isLoading } = useQuery({
    queryKey: ['drivers-independent'],
    queryFn: listIndependentDrivers,
  });

  const claimMutation = useMutation({
    mutationFn: (driverId: number) => claimDriver(schoolId, driverId),
    onSuccess: (driver) => {
      queryClient.invalidateQueries({ queryKey: ['drivers-independent'] });
      queryClient.invalidateQueries({ queryKey: ['drivers', schoolId] });
      toast.success(`${driver.firstName} ${driver.lastName} hired`);
    },
    onError: () => toast.error('Could not hire this driver'),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Independent drivers available to hire</CardTitle>
        <p className="mt-1 text-sm text-slate-500">
          These privately-owned-vehicle drivers registered themselves on Sunju directly. Hiring one moves them, their
          login, and their vehicle into your school.
        </p>
      </CardHeader>
      <CardBody>
        {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
        <ul className="divide-y divide-slate-100">
          {independentDrivers?.map((driver) => (
            <li key={driver.id} className="flex items-center justify-between py-3">
              <div>
                <p className="font-medium text-slate-900">
                  {driver.firstName} {driver.lastName}
                </p>
                <p className="text-sm text-slate-500">
                  {driver.phone} &middot; License {driver.licenseNumber}
                  {driver.vehicleId ? ` · has their own vehicle` : ''}
                </p>
              </div>
              <Button
                variant="secondary"
                isLoading={claimMutation.isPending && claimMutation.variables === driver.id}
                onClick={() => claimMutation.mutate(driver.id)}
              >
                Hire
              </Button>
            </li>
          ))}
          {independentDrivers?.length === 0 && (
            <p className="py-3 text-sm text-slate-500">No independent drivers are waiting to be hired right now.</p>
          )}
        </ul>
      </CardBody>
    </Card>
  );
}

export function DriversPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
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

  const terminateMutation = useMutation({
    mutationFn: (driverId: number) => terminateDriver(schoolId!, driverId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['drivers', schoolId] });
      toast.success('Driver terminated');
    },
    onError: () => toast.error('Could not terminate driver'),
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
          <ul className="flex flex-col divide-y divide-slate-100">
            {drivers?.map((driver) =>
              editingId === driver.id ? (
                <li key={driver.id} className="py-3">
                  <EditDriverRow schoolId={schoolId!} driver={driver} onDone={() => setEditingId(null)} />
                </li>
              ) : (
                <li key={driver.id} className="flex items-center justify-between gap-4 py-3">
                  <div>
                    <p className="font-medium text-slate-900">
                      {driver.firstName} {driver.lastName}
                    </p>
                    <p className="text-sm text-slate-500">
                      {driver.phone} &middot; License {driver.licenseNumber}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge tone={driver.employmentStatus === 'ACTIVE' ? 'success' : 'warning'}>{driver.employmentStatus}</Badge>
                    <Button variant="secondary" onClick={() => setEditingId(driver.id)}>
                      Edit
                    </Button>
                    {driver.employmentStatus !== 'TERMINATED' && (
                      <Button
                        variant="danger"
                        isLoading={terminateMutation.isPending && terminateMutation.variables === driver.id}
                        onClick={() => terminateMutation.mutate(driver.id)}
                      >
                        Terminate
                      </Button>
                    )}
                  </div>
                </li>
              ),
            )}
            {drivers?.length === 0 && <p className="py-3 text-sm text-slate-500">No drivers yet.</p>}
          </ul>
        </CardBody>
      </Card>

      {schoolId != null && <IndependentDriversCard schoolId={schoolId} />}
    </div>
  );
}
