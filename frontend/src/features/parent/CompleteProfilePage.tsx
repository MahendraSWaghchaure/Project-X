import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { completeMyParentProfile } from '@/lib/api/parents';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';

const schema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  phone: z.string().min(1),
  address: z.string().min(1),
  latitude: z.coerce.number().optional(),
  longitude: z.coerce.number().optional(),
});
type FormValues = z.infer<typeof schema>;

export function CompleteProfilePage() {
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => completeMyParentProfile(values),
    onSuccess: () => navigate('/parent/track'),
  });

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Tell us a bit about you</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Your home location is used to alert you when the bus is nearly there at drop-off.
          </p>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
            <Input label="First name" error={errors.firstName?.message} {...register('firstName')} />
            <Input label="Last name" error={errors.lastName?.message} {...register('lastName')} />
            <Input label="Phone" error={errors.phone?.message} {...register('phone')} />
            <Input label="Address" error={errors.address?.message} {...register('address')} />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Home latitude" type="number" step="any" {...register('latitude')} />
              <Input label="Home longitude" type="number" step="any" {...register('longitude')} />
            </div>
            {mutation.isError && <p className="text-sm text-red-600">Could not save your profile.</p>}
            <Button type="submit" isLoading={isSubmitting} className="w-full">
              Continue
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
