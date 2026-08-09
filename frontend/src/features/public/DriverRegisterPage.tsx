import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { selfRegisterDriver } from '@/lib/api/drivers';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Logo } from '@/components/ui/Logo';

const schema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(8),
  phone: z.string().min(1),
  licenseNumber: z.string().min(1),
  licenseExpiry: z.string().optional(),
  vehicleRegistrationNumber: z.string().min(1),
  vehicleType: z.enum(['VAN', 'BUS']),
  vehicleSeatCapacity: z.coerce.number().int().min(1),
});
type FormValues = z.infer<typeof schema>;

export function DriverRegisterPage() {
  const [done, setDone] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { vehicleType: 'VAN' } });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => selfRegisterDriver(values),
    onSuccess: () => setDone(true),
  });

  if (done) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-slate-50 px-4">
        <Logo size="lg" />
        <Card className="w-full max-w-md text-center">
          <CardBody className="flex flex-col gap-4 py-8">
            <h1 className="text-xl font-semibold text-slate-900">You're registered</h1>
            <p className="text-sm text-slate-600">
              Your driver and vehicle details are saved. A school can now find and hire you on Sunju — once they do,
              you'll be able to sign in and see your assigned route.
            </p>
            <Link to="/login">
              <Button className="w-full">Go to sign in</Button>
            </Link>
          </CardBody>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-slate-50 px-4 py-12">
      <Logo size="lg" />
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle>Register as an independent driver</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Run your own van or bus? Register here with your details and your vehicle's details. Schools on Sunju can
            then find and hire you — we hand your verified profile straight to them.
          </p>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={handleSubmit((values) => mutation.mutate(values))}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <Input label="First name" error={errors.firstName?.message} {...register('firstName')} />
            <Input label="Last name" error={errors.lastName?.message} {...register('lastName')} />
            <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
            <Input label="Password" type="password" error={errors.password?.message} {...register('password')} />
            <Input label="Phone" error={errors.phone?.message} {...register('phone')} />
            <Input label="License number" error={errors.licenseNumber?.message} {...register('licenseNumber')} />
            <Input label="License expiry" type="date" {...register('licenseExpiry')} />
            <div />

            <div className="sm:col-span-2 border-t border-slate-100 pt-4 text-sm font-medium text-slate-500">
              Your vehicle
            </div>
            <Input
              label="Registration number"
              error={errors.vehicleRegistrationNumber?.message}
              {...register('vehicleRegistrationNumber')}
            />
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Type</label>
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" {...register('vehicleType')}>
                <option value="VAN">Van</option>
                <option value="BUS">Bus</option>
              </select>
            </div>
            <Input
              label="Seat capacity"
              type="number"
              error={errors.vehicleSeatCapacity?.message}
              {...register('vehicleSeatCapacity')}
            />

            {mutation.isError && (
              <p className="sm:col-span-2 text-sm text-red-600">
                Could not register. Check your details — the email, license number, or vehicle registration number
                may already be in use.
              </p>
            )}
            <Button type="submit" isLoading={isSubmitting} className="sm:col-span-2">
              Register
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
