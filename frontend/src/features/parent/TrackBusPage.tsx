import { useMemo, useState } from 'react';
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet';
import { useMutation, useQuery } from '@tanstack/react-query';
import { listMyChildren } from '@/lib/api/children';
import { getMyChildVehicleLocation } from '@/lib/api/tracking';
import { getTodaysTripForMyChild, recordParentHomeArrival } from '@/lib/api/trips';
import { useLiveVehicleLocation } from '@/hooks/useLiveVehicleLocation';
import { vehicleIcon } from '@/lib/leafletIcons';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';

const DEFAULT_CENTER: [number, number] = [20.5937, 78.9629]; // India, used only before any position is known

export function TrackBusPage() {
  const { data: children } = useQuery({ queryKey: ['my-children'], queryFn: listMyChildren });
  const [selectedChildId, setSelectedChildId] = useState<number | null>(null);
  const childId = selectedChildId ?? children?.[0]?.id ?? null;

  const { data: restLocation } = useQuery({
    queryKey: ['child-vehicle-location', childId],
    queryFn: () => getMyChildVehicleLocation(childId!),
    enabled: childId != null,
    retry: false,
  });

  const { location: liveLocation, connected } = useLiveVehicleLocation(restLocation?.vehicleId ?? null);
  const location = liveLocation ?? restLocation;

  const homeArrivalMutation = useMutation({
    mutationFn: async () => {
      if (childId == null) throw new Error('No child selected');
      const trip = await getTodaysTripForMyChild(childId, 'AFTERNOON_DROP');
      return recordParentHomeArrival(trip.id, childId);
    },
  });

  const center = useMemo<[number, number]>(
    () => (location ? [location.latitude, location.longitude] : DEFAULT_CENTER),
    [location],
  );

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Track Bus</h1>
        {children && children.length > 1 && (
          <select
            value={childId ?? ''}
            onChange={(e) => setSelectedChildId(Number(e.target.value))}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            {children.map((child) => (
              <option key={child.id} value={child.id}>
                {child.firstName} {child.lastName}
              </option>
            ))}
          </select>
        )}
      </div>

      <Card className="overflow-hidden">
        <div className="h-[420px] w-full">
          <MapContainer center={center} zoom={13} className="h-full w-full">
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {location && (
              <Marker position={[location.latitude, location.longitude]} icon={vehicleIcon}>
                <Popup>
                  {location.speedKmh != null ? `${Math.round(location.speedKmh)} km/h` : 'Speed unknown'}
                  <br />
                  Last update: {new Date(location.recordedAt).toLocaleTimeString()}
                </Popup>
              </Marker>
            )}
          </MapContainer>
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Status</CardTitle>
        </CardHeader>
        <CardBody className="flex flex-col gap-3">
          <p className="text-sm text-slate-600">
            Live connection: {connected ? <span className="text-green-600">connected</span> : <span className="text-slate-400">connecting&hellip;</span>}
          </p>
          {!location && <p className="text-sm text-slate-500">No live position yet for this child's bus.</p>}
          <Button
            variant="secondary"
            isLoading={homeArrivalMutation.isPending}
            disabled={childId == null}
            onClick={() => homeArrivalMutation.mutate()}
          >
            Confirm my child reached home safely
          </Button>
          {homeArrivalMutation.isSuccess && <p className="text-sm text-green-600">Thanks, marked as confirmed.</p>}
          {homeArrivalMutation.isError && (
            <p className="text-sm text-red-600">Could not confirm right now &mdash; there may be no drop-off trip today yet.</p>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
