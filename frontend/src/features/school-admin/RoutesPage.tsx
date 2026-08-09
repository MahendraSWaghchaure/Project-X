import { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createRoute, listRoutes, assignRoute, updateRouteZone } from '@/lib/api/routes';
import { listVehicles } from '@/lib/api/vehicles';
import { listDrivers } from '@/lib/api/drivers';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import type { Route } from '@/types/school';

const stopSchema = z.object({
  label: z.string().min(1),
  latitude: z.coerce.number(),
  longitude: z.coerce.number(),
  radiusMeters: z.coerce.number().optional(),
});
const schema = z.object({
  name: z.string().min(1),
  zoneMinRadiusMeters: z.coerce.number().min(0).optional(),
  zoneMaxRadiusMeters: z.coerce.number().min(0).optional(),
  stops: z.array(stopSchema).min(1, 'Add at least one stop'),
});
type FormValues = z.infer<typeof schema>;

function ZoneEditor({ schoolId, route }: { schoolId: number; route: Route }) {
  const queryClient = useQueryClient();
  const [min, setMin] = useState(route.zoneMinRadiusMeters ?? 0);
  const [max, setMax] = useState(route.zoneMaxRadiusMeters ?? 0);

  const zoneMutation = useMutation({
    mutationFn: () => updateRouteZone(schoolId, route.id, min, max),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes', schoolId] });
      toast.success('Zone updated');
    },
    onError: () => toast.error('Could not update zone'),
  });

  return (
    <div className="mt-2 flex flex-wrap items-end gap-2 border-t border-slate-100 pt-2">
      <Input
        id={`route-zone-min-${route.id}`}
        label="Zone min (m)"
        type="number"
        value={min}
        onChange={(e) => setMin(Number(e.target.value))}
        className="w-32"
      />
      <Input
        id={`route-zone-max-${route.id}`}
        label="Zone max (m)"
        type="number"
        value={max}
        onChange={(e) => setMax(Number(e.target.value))}
        className="w-32"
      />
      <Button type="button" variant="secondary" isLoading={zoneMutation.isPending} onClick={() => zoneMutation.mutate()}>
        Save zone
      </Button>
    </div>
  );
}

export function RoutesPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const queryClient = useQueryClient();

  const { data: routes, isLoading } = useQuery({
    queryKey: ['routes', schoolId],
    queryFn: () => listRoutes(schoolId!),
    enabled: schoolId != null,
  });
  const { data: vehicles } = useQuery({
    queryKey: ['vehicles', schoolId],
    queryFn: () => listVehicles(schoolId!),
    enabled: schoolId != null,
  });
  const { data: drivers } = useQuery({
    queryKey: ['drivers', schoolId],
    queryFn: () => listDrivers(schoolId!),
    enabled: schoolId != null,
  });

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', stops: [{ label: '', latitude: 0, longitude: 0, radiusMeters: 150 }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'stops' });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) =>
      createRoute(schoolId!, {
        name: values.name,
        zoneMinRadiusMeters: values.zoneMinRadiusMeters,
        zoneMaxRadiusMeters: values.zoneMaxRadiusMeters,
        stops: values.stops.map((stop, index) => ({ ...stop, sequenceNumber: index })),
      }),
    onSuccess: (route) => {
      queryClient.invalidateQueries({ queryKey: ['routes', schoolId] });
      reset();
      toast.success(`${route.name} created`);
    },
    onError: () => toast.error('Could not create route'),
  });

  const assignMutation = useMutation({
    mutationFn: ({ routeId, vehicleId, driverId }: { routeId: number; vehicleId: number; driverId: number }) =>
      assignRoute(schoolId!, routeId, vehicleId, driverId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes', schoolId] });
      toast.success('Vehicle and driver assigned');
    },
    onError: () => toast.error('Could not assign vehicle/driver'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Routes</h1>

      <Card>
        <CardHeader>
          <CardTitle>Create a route</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Set a pickup-radius zone (distance from the school) so children can be auto-assigned to the nearest
            covering route — e.g. Route A covers 0–5km, Route B covers 5–10km.
          </p>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit((values) => createMutation.mutate(values))} className="flex flex-col gap-4">
            <Input label="Route name" error={errors.name?.message} {...register('name')} />
            <div className="grid grid-cols-2 gap-4 sm:w-1/2">
              <Input label="Zone min (m)" type="number" {...register('zoneMinRadiusMeters')} />
              <Input label="Zone max (m)" type="number" {...register('zoneMaxRadiusMeters')} />
            </div>
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-slate-700">Stops (in order)</span>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => append({ label: '', latitude: 0, longitude: 0, radiusMeters: 150 })}
                >
                  Add stop
                </Button>
              </div>
              {fields.map((field, index) => (
                <div key={field.id} className="grid grid-cols-1 gap-3 rounded-md border border-slate-200 p-3 sm:grid-cols-4">
                  <Input id={`stop-label-${field.id}`} label="Label" {...register(`stops.${index}.label` as const)} />
                  <Input
                    id={`stop-latitude-${field.id}`}
                    label="Latitude"
                    type="number"
                    step="any"
                    {...register(`stops.${index}.latitude` as const)}
                  />
                  <Input
                    id={`stop-longitude-${field.id}`}
                    label="Longitude"
                    type="number"
                    step="any"
                    {...register(`stops.${index}.longitude` as const)}
                  />
                  <div className="flex items-end gap-2">
                    <Input
                      id={`stop-radius-${field.id}`}
                      label="Radius (m)"
                      type="number"
                      {...register(`stops.${index}.radiusMeters` as const)}
                    />
                    <Button type="button" variant="danger" onClick={() => remove(index)}>
                      Remove
                    </Button>
                  </div>
                </div>
              ))}
              {errors.stops?.message && <p className="text-sm text-red-600">{errors.stops.message}</p>}
            </div>
            {createMutation.isError && <p className="text-sm text-red-600">Could not create route.</p>}
            <Button type="submit" isLoading={isSubmitting} className="w-fit">
              Create route
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Routes</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col gap-4">
            {routes?.map((route) => (
              <li key={route.id} className="rounded-md border border-slate-200 p-3">
                <p className="font-medium text-slate-900">{route.name}</p>
                <p className="text-sm text-slate-500">
                  {route.stops.length} stops
                  {route.zoneMinRadiusMeters != null && route.zoneMaxRadiusMeters != null
                    ? ` · zone ${route.zoneMinRadiusMeters}–${route.zoneMaxRadiusMeters}m from school`
                    : ' · no zone configured'}
                </p>
                <form
                  className="mt-2 flex flex-wrap items-end gap-2"
                  onSubmit={(e) => {
                    e.preventDefault();
                    const form = e.currentTarget;
                    const vehicleId = Number((form.elements.namedItem('vehicleId') as HTMLSelectElement).value);
                    const driverId = Number((form.elements.namedItem('driverId') as HTMLSelectElement).value);
                    if (vehicleId && driverId) {
                      assignMutation.mutate({ routeId: route.id, vehicleId, driverId });
                    }
                  }}
                >
                  <select name="vehicleId" defaultValue={route.vehicleId ?? ''} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
                    <option value="">Vehicle&hellip;</option>
                    {vehicles?.map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.registrationNumber}
                      </option>
                    ))}
                  </select>
                  <select name="driverId" defaultValue={route.driverId ?? ''} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
                    <option value="">Driver&hellip;</option>
                    {drivers?.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.firstName} {d.lastName}
                      </option>
                    ))}
                  </select>
                  <Button type="submit" variant="secondary" isLoading={assignMutation.isPending}>
                    Assign
                  </Button>
                </form>
                <ZoneEditor schoolId={schoolId!} route={route} />
              </li>
            ))}
            {routes?.length === 0 && <p className="text-sm text-slate-500">No routes yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
