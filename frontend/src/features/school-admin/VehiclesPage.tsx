import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createVehicle, listVehicles, retireVehicle, updateVehicle, type UpdateVehicleRequest } from '@/lib/api/vehicles';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { Vehicle } from '@/types/school';

const schema = z.object({
  registrationNumber: z.string().min(1),
  type: z.enum(['VAN', 'BUS']),
  seatCapacity: z.coerce.number().int().min(1),
  gpsDeviceId: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

const editSchema = z.object({
  registrationNumber: z.string().min(1),
  type: z.enum(['VAN', 'BUS']),
  seatCapacity: z.coerce.number().int().min(1),
});
type EditFormValues = z.infer<typeof editSchema>;

function EditVehicleRow({ schoolId, vehicle, onDone }: { schoolId: number; vehicle: Vehicle; onDone: () => void }) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    defaultValues: { registrationNumber: vehicle.registrationNumber, type: vehicle.type, seatCapacity: vehicle.seatCapacity },
  });

  const updateMutation = useMutation({
    mutationFn: (values: UpdateVehicleRequest) => updateVehicle(schoolId, vehicle.id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles', schoolId] });
      toast.success('Vehicle updated');
      onDone();
    },
    onError: () => toast.error('Could not update vehicle'),
  });

  return (
    <form
      onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
      className="grid grid-cols-1 gap-3 rounded-md border border-blue-200 bg-blue-50/40 p-3 sm:grid-cols-3"
    >
      <Input
        id={`vehicle-registration-number-${vehicle.id}`}
        label="Registration number"
        error={errors.registrationNumber?.message}
        {...register('registrationNumber')}
      />
      <div className="flex flex-col gap-1">
        <label className="text-sm font-medium text-slate-700">Type</label>
        <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" {...register('type')}>
          <option value="VAN">Van</option>
          <option value="BUS">Bus</option>
        </select>
      </div>
      <Input
        id={`vehicle-seat-capacity-${vehicle.id}`}
        label="Seat capacity"
        type="number"
        error={errors.seatCapacity?.message}
        {...register('seatCapacity')}
      />
      <div className="flex gap-2 sm:col-span-3">
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

export function VehiclesPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const { data: vehicles, isLoading } = useQuery({
    queryKey: ['vehicles', schoolId],
    queryFn: () => listVehicles(schoolId!),
    enabled: schoolId != null,
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { type: 'VAN' } });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) => createVehicle(schoolId!, values),
    onSuccess: (vehicle) => {
      queryClient.invalidateQueries({ queryKey: ['vehicles', schoolId] });
      reset();
      toast.success(`${vehicle.registrationNumber} added`);
    },
    onError: () => toast.error('Could not add vehicle'),
  });

  const retireMutation = useMutation({
    mutationFn: (vehicleId: number) => retireVehicle(schoolId!, vehicleId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles', schoolId] });
      toast.success('Vehicle retired');
    },
    onError: () => toast.error('Could not retire vehicle'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Vehicles</h1>

      <Card>
        <CardHeader>
          <CardTitle>Add a vehicle</CardTitle>
        </CardHeader>
        <CardBody>
          <form
            onSubmit={handleSubmit((values) => createMutation.mutate(values))}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            <Input label="Registration number" error={errors.registrationNumber?.message} {...register('registrationNumber')} />
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Type</label>
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" {...register('type')}>
                <option value="VAN">Van</option>
                <option value="BUS">Bus</option>
              </select>
            </div>
            <Input label="Seat capacity" type="number" error={errors.seatCapacity?.message} {...register('seatCapacity')} />
            <Input label="GPS device id (Traccar uniqueId)" {...register('gpsDeviceId')} />
            {createMutation.isError && <p className="sm:col-span-2 text-sm text-red-600">Could not add vehicle.</p>}
            <Button type="submit" isLoading={isSubmitting} className="sm:col-span-2 sm:w-fit">
              Add vehicle
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Fleet</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {vehicles?.map((vehicle) =>
              editingId === vehicle.id ? (
                <li key={vehicle.id} className="py-3">
                  <EditVehicleRow schoolId={schoolId!} vehicle={vehicle} onDone={() => setEditingId(null)} />
                </li>
              ) : (
                <li key={vehicle.id} className="flex items-center justify-between gap-4 py-3">
                  <div>
                    <p className="font-medium text-slate-900">{vehicle.registrationNumber}</p>
                    <p className="text-sm text-slate-500">
                      {vehicle.type} &middot; {vehicle.seatCapacity} seats
                      {vehicle.gpsDeviceId ? ` · GPS ${vehicle.gpsDeviceId}` : ' · no GPS device linked'}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge tone={vehicle.status === 'ACTIVE' ? 'success' : 'warning'}>{vehicle.status}</Badge>
                    {vehicle.insurancePolicyId ? (
                      <Badge tone="info">Insured</Badge>
                    ) : (
                      <Link to="/school-admin/insurance">
                        <Badge tone="neutral" className="cursor-pointer hover:bg-slate-200">
                          Not insured
                        </Badge>
                      </Link>
                    )}
                    <Button variant="secondary" onClick={() => setEditingId(vehicle.id)}>
                      Edit
                    </Button>
                    {vehicle.status !== 'RETIRED' && (
                      <Button
                        variant="danger"
                        isLoading={retireMutation.isPending && retireMutation.variables === vehicle.id}
                        onClick={() => retireMutation.mutate(vehicle.id)}
                      >
                        Retire
                      </Button>
                    )}
                  </div>
                </li>
              ),
            )}
            {vehicles?.length === 0 && <p className="py-3 text-sm text-slate-500">No vehicles yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
