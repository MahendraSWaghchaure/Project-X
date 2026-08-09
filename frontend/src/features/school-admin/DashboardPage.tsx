import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listVehicles } from '@/lib/api/vehicles';
import { listDrivers } from '@/lib/api/drivers';
import { listRoutes } from '@/lib/api/routes';
import { listChildrenForSchool } from '@/lib/api/children';
import { listTripsForSchool } from '@/lib/api/trips';
import { getSchool } from '@/lib/api/schools';
import { useAuthStore } from '@/lib/authStore';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import type { TripStatus } from '@/types/tracking';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

interface StatCardProps {
  label: string;
  value: number | string;
  to: string;
  hint?: string;
}

function StatCard({ label, value, to, hint }: StatCardProps) {
  return (
    <Link to={to}>
      <Card className="transition-shadow hover:shadow-md">
        <CardBody>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-1 text-3xl font-bold text-slate-900">{value}</p>
          {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
        </CardBody>
      </Card>
    </Link>
  );
}

const STATUS_TONE: Record<TripStatus, 'neutral' | 'success' | 'warning' | 'danger' | 'info'> = {
  SCHEDULED: 'neutral',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  DELAYED: 'warning',
  CANCELLED: 'danger',
};

export function SchoolAdminDashboardPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const enabled = schoolId != null;

  const { data: school } = useQuery({ queryKey: ['school', schoolId], queryFn: () => getSchool(schoolId!), enabled });
  const { data: vehicles } = useQuery({ queryKey: ['vehicles', schoolId], queryFn: () => listVehicles(schoolId!), enabled });
  const { data: drivers } = useQuery({ queryKey: ['drivers', schoolId], queryFn: () => listDrivers(schoolId!), enabled });
  const { data: routes } = useQuery({ queryKey: ['routes', schoolId], queryFn: () => listRoutes(schoolId!), enabled });
  const { data: children } = useQuery({
    queryKey: ['children', schoolId],
    queryFn: () => listChildrenForSchool(schoolId!),
    enabled,
  });
  const { data: trips } = useQuery({
    queryKey: ['trips', schoolId, today()],
    queryFn: () => listTripsForSchool(schoolId!, today()),
    enabled,
  });

  const activeVehicles = vehicles?.filter((v) => v.status === 'ACTIVE').length ?? 0;
  const activeTrips = trips?.filter((t) => t.status === 'IN_PROGRESS' || t.status === 'DELAYED').length ?? 0;
  const delayedTrips = trips?.filter((t) => t.status === 'DELAYED').length ?? 0;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{school?.name ?? 'Dashboard'}</h1>
        <p className="text-sm text-slate-500">{school?.city}</p>
      </div>

      {delayedTrips > 0 && (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {delayedTrips} trip{delayedTrips > 1 ? 's are' : ' is'} currently running past dismissal time - see the
          Trips tab.
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        <StatCard label="Vehicles" value={vehicles?.length ?? '—'} hint={`${activeVehicles} active`} to="/school-admin/vehicles" />
        <StatCard label="Drivers" value={drivers?.length ?? '—'} to="/school-admin/drivers" />
        <StatCard label="Routes" value={routes?.length ?? '—'} to="/school-admin/routes" />
        <StatCard label="Students" value={children?.length ?? '—'} to="/school-admin/children" />
        <StatCard label="Trips today" value={trips?.length ?? '—'} hint={`${activeTrips} in progress`} to="/school-admin/trips" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Today&rsquo;s trips</CardTitle>
        </CardHeader>
        <CardBody>
          <ul className="divide-y divide-slate-100">
            {trips?.map((trip) => (
              <li key={trip.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium text-slate-900">
                    Route #{trip.routeId} &middot; {trip.direction === 'MORNING_PICKUP' ? 'Morning pickup' : 'Afternoon drop'}
                  </p>
                  <p className="text-sm text-slate-500">
                    Vehicle #{trip.vehicleId} &middot; Driver #{trip.driverId}
                  </p>
                </div>
                <Badge tone={STATUS_TONE[trip.status]}>{trip.status}</Badge>
              </li>
            ))}
            {trips?.length === 0 && <p className="py-3 text-sm text-slate-500">No trips scheduled for today yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
