import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createSchool,
  listSchools,
  setSchoolStatus,
  updateSchool,
  type CreateSchoolRequest,
  type UpdateSchoolRequest,
} from '@/lib/api/schools';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { LocationPicker } from '@/components/ui/LocationPicker';
import type { School } from '@/types/school';

const schema = z.object({
  name: z.string().min(1),
  address: z.string().min(1),
  city: z.string().min(1),
  contactPhone: z.string().optional(),
  contactEmail: z.string().email().optional().or(z.literal('')),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  adminFirstName: z.string().min(1),
  adminLastName: z.string().min(1),
  adminEmail: z.string().email(),
  adminPassword: z.string().min(8),
});
type FormValues = z.infer<typeof schema>;

const editSchema = z.object({
  name: z.string().min(1),
  address: z.string().min(1),
  city: z.string().min(1),
  state: z.string().optional(),
  pincode: z.string().optional(),
  contactPhone: z.string().optional(),
  contactEmail: z.string().email().optional().or(z.literal('')),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});
type EditFormValues = z.infer<typeof editSchema>;

function EditSchoolRow({ school, onDone }: { school: School; onDone: () => void }) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      name: school.name,
      address: school.address,
      city: school.city,
      state: school.state ?? '',
      pincode: school.pincode ?? '',
      contactPhone: school.contactPhone ?? '',
      contactEmail: school.contactEmail ?? '',
      latitude: school.location?.latitude,
      longitude: school.location?.longitude,
    },
  });
  const latitude = watch('latitude');
  const longitude = watch('longitude');

  const updateMutation = useMutation({
    mutationFn: (values: UpdateSchoolRequest) => updateSchool(school.id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      toast.success('School updated');
      onDone();
    },
    onError: () => toast.error('Could not update school'),
  });

  return (
    <form
      onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
      className="grid grid-cols-1 gap-3 rounded-md border border-blue-200 bg-blue-50/40 p-3 sm:grid-cols-2"
    >
      <Input id={`school-name-${school.id}`} label="School name" error={errors.name?.message} {...register('name')} />
      <Input id={`school-city-${school.id}`} label="City" error={errors.city?.message} {...register('city')} />
      <Input
        id={`school-address-${school.id}`}
        label="Address"
        className="sm:col-span-2"
        error={errors.address?.message}
        {...register('address')}
      />
      <Input id={`school-state-${school.id}`} label="State" {...register('state')} />
      <Input id={`school-pincode-${school.id}`} label="Pincode" {...register('pincode')} />
      <Input id={`school-contact-phone-${school.id}`} label="Contact phone" {...register('contactPhone')} />
      <Input id={`school-contact-email-${school.id}`} label="Contact email" type="email" {...register('contactEmail')} />
      <LocationPicker
        label="School location"
        latitude={latitude}
        longitude={longitude}
        onChange={(point) => {
          setValue('latitude', point.latitude);
          setValue('longitude', point.longitude);
        }}
        height={220}
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

export function SchoolsPage() {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const { data: schools, isLoading } = useQuery({ queryKey: ['schools'], queryFn: listSchools });
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });
  const newSchoolLatitude = watch('latitude');
  const newSchoolLongitude = watch('longitude');

  const createMutation = useMutation({
    mutationFn: (values: CreateSchoolRequest) => createSchool(values),
    onSuccess: (school) => {
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      reset();
      toast.success(`${school.name} created`);
    },
    onError: () => toast.error('Could not create school'),
  });

  const statusMutation = useMutation({
    mutationFn: ({ schoolId, status }: { schoolId: number; status: 'ACTIVE' | 'SUSPENDED' }) =>
      setSchoolStatus(schoolId, status),
    onSuccess: (school) => {
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      toast.success(`${school.name} is now ${school.status.toLowerCase()}`);
    },
    onError: () => toast.error('Could not change school status'),
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
            <LocationPicker
              label="School location"
              latitude={newSchoolLatitude}
              longitude={newSchoolLongitude}
              onChange={(point) => {
                setValue('latitude', point.latitude);
                setValue('longitude', point.longitude);
              }}
              height={220}
            />
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
          <ul className="flex flex-col divide-y divide-slate-100">
            {schools?.map((school) =>
              editingId === school.id ? (
                <li key={school.id} className="py-3">
                  <EditSchoolRow school={school} onDone={() => setEditingId(null)} />
                </li>
              ) : (
                <li key={school.id} className="flex items-center justify-between gap-4 py-3">
                  <div>
                    <p className="font-medium text-slate-900">{school.name}</p>
                    <p className="text-sm text-slate-500">
                      {school.city}
                      {school.address ? ` — ${school.address}` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge tone={school.status === 'ACTIVE' ? 'success' : 'warning'}>{school.status}</Badge>
                    <Button variant="secondary" onClick={() => setEditingId(school.id)}>
                      Edit
                    </Button>
                    {school.status === 'ACTIVE' ? (
                      <Button
                        variant="danger"
                        isLoading={statusMutation.isPending && statusMutation.variables?.schoolId === school.id}
                        onClick={() => statusMutation.mutate({ schoolId: school.id, status: 'SUSPENDED' })}
                      >
                        Suspend
                      </Button>
                    ) : (
                      <Button
                        variant="secondary"
                        isLoading={statusMutation.isPending && statusMutation.variables?.schoolId === school.id}
                        onClick={() => statusMutation.mutate({ schoolId: school.id, status: 'ACTIVE' })}
                      >
                        Reactivate
                      </Button>
                    )}
                  </div>
                </li>
              ),
            )}
            {schools?.length === 0 && <p className="py-3 text-sm text-slate-500">No schools yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
