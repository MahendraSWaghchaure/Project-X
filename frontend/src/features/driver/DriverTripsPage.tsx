import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listMyTripsAsDriver, startTrip, completeTrip, recordDriverMarkedEvent } from '@/lib/api/trips';
import { listChildrenForRoute } from '@/lib/api/children';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { ChildTripEventType, Trip, TripStatus } from '@/types/tracking';

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

function eventButtons(direction: Trip['direction']): { label: string; type: ChildTripEventType }[] {
  return direction === 'MORNING_PICKUP'
    ? [
        { label: 'Boarded', type: 'BOARDED' },
        { label: 'Dropped at school', type: 'DROPPED_AT_SCHOOL' },
        { label: 'Absent', type: 'ABSENT' },
      ]
    : [
        { label: 'Picked from school', type: 'PICKED_FROM_SCHOOL' },
        { label: 'Absent', type: 'ABSENT' },
      ];
}

export function DriverTripsPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const [expandedTripId, setExpandedTripId] = useState<number | null>(null);
  const queryClient = useQueryClient();

  const { data: trips, isLoading } = useQuery({
    queryKey: ['driver-trips', today()],
    queryFn: () => listMyTripsAsDriver(today()),
  });

  const startMutation = useMutation({
    mutationFn: (tripId: number) => startTrip(tripId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-trips', today()] });
      toast.success('Trip started');
    },
    onError: () => toast.error('Could not start trip'),
  });
  const completeMutation = useMutation({
    mutationFn: (tripId: number) => completeTrip(tripId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['driver-trips', today()] });
      toast.success('Trip completed');
    },
    onError: () => toast.error('Could not complete trip'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Today's Trips</h1>
      {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
      {trips?.length === 0 && <p className="text-sm text-slate-500">No trips scheduled for you today.</p>}

      {trips?.map((trip) => (
        <Card key={trip.id}>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>
              {trip.direction === 'MORNING_PICKUP' ? 'Morning pickup' : 'Afternoon drop'} &middot; Route #{trip.routeId}
            </CardTitle>
            <Badge tone={STATUS_TONE[trip.status]}>{trip.status}</Badge>
          </CardHeader>
          <CardBody className="flex flex-col gap-3">
            <div className="flex gap-2">
              {trip.status === 'SCHEDULED' && (
                <Button isLoading={startMutation.isPending} onClick={() => startMutation.mutate(trip.id)}>
                  Start trip
                </Button>
              )}
              {(trip.status === 'IN_PROGRESS' || trip.status === 'DELAYED') && (
                <>
                  <Button variant="secondary" onClick={() => setExpandedTripId(expandedTripId === trip.id ? null : trip.id)}>
                    {expandedTripId === trip.id ? 'Hide children' : 'Mark children'}
                  </Button>
                  <Button isLoading={completeMutation.isPending} onClick={() => completeMutation.mutate(trip.id)}>
                    Complete trip
                  </Button>
                </>
              )}
            </div>
            {expandedTripId === trip.id && schoolId != null && <TripChildren trip={trip} schoolId={schoolId} />}
          </CardBody>
        </Card>
      ))}
    </div>
  );
}

function TripChildren({ trip, schoolId }: { trip: Trip; schoolId: number }) {
  const queryClient = useQueryClient();
  const { data: children } = useQuery({
    queryKey: ['route-children', trip.routeId],
    queryFn: () => listChildrenForRoute(schoolId, trip.routeId),
  });

  const markMutation = useMutation({
    mutationFn: ({ childId, type }: { childId: number; type: ChildTripEventType }) =>
      recordDriverMarkedEvent(trip.id, childId, type),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['child-trip-events', trip.id] });
      toast.success(`Marked ${variables.type.replace(/_/g, ' ').toLowerCase()}`);
    },
    onError: () => toast.error('Could not record that'),
  });

  return (
    <ul className="divide-y divide-slate-100 rounded-md border border-slate-200">
      {children?.map((child) => (
        <li key={child.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
          <span className="font-medium text-slate-900">
            {child.firstName} {child.lastName}
          </span>
          <div className="flex gap-1.5">
            {eventButtons(trip.direction).map((btn) => (
              <Button
                key={btn.type}
                variant="secondary"
                className="text-xs"
                isLoading={markMutation.isPending && markMutation.variables?.childId === child.id}
                onClick={() => markMutation.mutate({ childId: child.id, type: btn.type })}
              >
                {btn.label}
              </Button>
            ))}
          </div>
        </li>
      ))}
      {children?.length === 0 && <li className="px-3 py-2 text-sm text-slate-500">No children assigned to this route.</li>}
    </ul>
  );
}
