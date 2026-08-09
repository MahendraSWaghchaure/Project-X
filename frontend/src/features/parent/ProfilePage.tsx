import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { completeMyParentProfile, getMyParentProfile } from '@/lib/api/parents';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { LocationPicker } from '@/components/ui/LocationPicker';

const schema = z.object({
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  phone: z.string().min(1),
  address: z.string().min(1),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});
type FormValues = z.infer<typeof schema>;

export function ParentProfilePage() {
  const { data: profile, isLoading } = useQuery({ queryKey: ['my-parent-profile'], queryFn: getMyParentProfile });
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (profile) {
      reset({
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
        phone: profile.phone ?? '',
        address: profile.address ?? '',
        latitude: profile.homeLocation?.latitude,
        longitude: profile.homeLocation?.longitude,
      });
    }
  }, [profile, reset]);

  const latitude = watch('latitude');
  const longitude = watch('longitude');

  const mutation = useMutation({
    mutationFn: (values: FormValues) => completeMyParentProfile(values),
    onSuccess: () => toast.success('Profile updated'),
    onError: () => toast.error('Could not update your profile'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">My Profile</h1>

      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Your details</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Your home location is used to alert you when the bus is nearly there at drop-off.
          </p>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
            <Input label="First name" error={errors.firstName?.message} {...register('firstName')} />
            <Input label="Last name" error={errors.lastName?.message} {...register('lastName')} />
            <Input label="Phone" error={errors.phone?.message} {...register('phone')} />
            <Input label="Address" error={errors.address?.message} {...register('address')} />
            <LocationPicker
              label="Home location"
              latitude={latitude}
              longitude={longitude}
              onChange={(point) => {
                setValue('latitude', point.latitude);
                setValue('longitude', point.longitude);
              }}
            />
            {mutation.isError && <p className="text-sm text-red-600">Could not save your profile.</p>}
            <Button type="submit" isLoading={isSubmitting} className="w-fit">
              Save changes
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
