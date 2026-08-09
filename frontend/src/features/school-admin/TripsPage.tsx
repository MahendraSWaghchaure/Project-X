import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listRoutes } from '@/lib/api/routes';
import { listTripsForSchool, scheduleTrip } from '@/lib/api/trips';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { TripDirection, TripStatus } from '@/types/tracking';

const STATUS_TONE: Record<TripStatus, 'neutral' | 'success' | 'warning' | 'danger' | 'info'> = {
  SCHEDULED: 'neutral',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  DELAYED: 'warning',
  CANCELLED: 'danger',
};

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function TripsPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const [date, setDate] = useState(today());
  const [routeId, setRouteId] = useState<number | ''>('');
  const [direction, setDirection] = useState<TripDirection>('MORNING_PICKUP');
  const queryClient = useQueryClient();

  const { data: routes } = useQuery({
    queryKey: ['routes', schoolId],
    queryFn: () => listRoutes(schoolId!),
    enabled: schoolId != null,
  });
  const { data: trips, isLoading } = useQuery({
    queryKey: ['trips', schoolId, date],
    queryFn: () => listTripsForSchool(schoolId!, date),
    enabled: schoolId != null,
  });

  const scheduleMutation = useMutation({
    mutationFn: () => scheduleTrip(schoolId!, Number(routeId), direction, date),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trips', schoolId, date] });
      toast.success('Trip scheduled');
    },
    onError: () => toast.error('Could not schedule trip'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Trips</h1>

      <Card>
        <CardHeader>
          <CardTitle>Schedule a trip</CardTitle>
        </CardHeader>
        <CardBody>
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Date</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Route</label>
              <select
                value={routeId}
                onChange={(e) => setRouteId(e.target.value ? Number(e.target.value) : '')}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Choose a route&hellip;</option>
                {routes?.map((route) => (
                  <option key={route.id} value={route.id}>
                    {route.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-slate-700">Direction</label>
              <select
                value={direction}
                onChange={(e) => setDirection(e.target.value as TripDirection)}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="MORNING_PICKUP">Morning pickup</option>
                <option value="AFTERNOON_DROP">Afternoon drop</option>
              </select>
            </div>
            <Button disabled={!routeId} isLoading={scheduleMutation.isPending} onClick={() => scheduleMutation.mutate()}>
              Schedule trip
            </Button>
          </div>
          {scheduleMutation.isError && <p className="mt-2 text-sm text-red-600">Could not schedule trip.</p>}
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Trips on {date}</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {trips?.map((trip) => (
              <li key={trip.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium text-slate-900">
                    Route #{trip.routeId} &middot; {trip.direction === 'MORNING_PICKUP' ? 'Morning pickup' : 'Afternoon drop'}
                  </p>
                  <p className="text-sm text-slate-500">Vehicle #{trip.vehicleId} &middot; Driver #{trip.driverId}</p>
                </div>
                <Badge tone={STATUS_TONE[trip.status]}>{trip.status}</Badge>
              </li>
            ))}
            {trips?.length === 0 && <p className="py-3 text-sm text-slate-500">No trips scheduled for this date.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
