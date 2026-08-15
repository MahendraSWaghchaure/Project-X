import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listMyTripsAsDriver } from '@/lib/api/trips';
import { getMyBankDetails, listMyPayroll } from '@/lib/api/payments';
import { listMyGrievances } from '@/lib/api/grievances';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/cn';
import type { TripStatus } from '@/types/tracking';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

const STATUS_TONE: Record<TripStatus, 'neutral' | 'success' | 'warning' | 'danger' | 'info'> = {
  SCHEDULED: 'neutral',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  DELAYED: 'warning',
  CANCELLED: 'danger',
};

interface StatCardProps {
  label: string;
  value: number | string;
  to: string;
  hint?: string;
  danger?: boolean;
}

function StatCard({ label, value, to, hint, danger }: StatCardProps) {
  return (
    <Link to={to}>
      <Card className="transition-shadow hover:shadow-md">
        <CardBody>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className={cn('mt-1 text-3xl font-bold', danger ? 'text-red-600' : 'text-slate-900')}>{value}</p>
          {hint && <p className={cn('mt-1 text-xs', danger ? 'text-red-500' : 'text-slate-400')}>{hint}</p>}
        </CardBody>
      </Card>
    </Link>
  );
}

export function DriverDashboardPage() {
  const { data: trips } = useQuery({ queryKey: ['my-trips', today()], queryFn: () => listMyTripsAsDriver(today()) });
  const { data: payroll } = useQuery({ queryKey: ['my-payroll'], queryFn: listMyPayroll });
  const { data: bankDetails, isFetched: bankFetched } = useQuery({
    queryKey: ['my-bank-details'],
    queryFn: getMyBankDetails,
    retry: false,
  });
  const { data: grievances } = useQuery({ queryKey: ['my-grievances'], queryFn: listMyGrievances });

  const pendingTrips = (trips ?? []).filter((t) => t.status === 'SCHEDULED' || t.status === 'IN_PROGRESS').length;
  const openGrievances = (grievances ?? []).filter((g) => g.status === 'OPEN' || g.status === 'IN_PROGRESS').length;
  const latestPayroll = [...(payroll ?? [])].sort((a, b) => b.periodStart.localeCompare(a.periodStart))[0];
  const bankMissing = bankFetched && !bankDetails;

  return (
    <div className="flex flex-col gap-6">
      <div className="rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 px-6 py-5 text-white shadow-sm">
        <p className="text-sm text-emerald-100">{greeting()}</p>
        <h1 className="mt-1 text-2xl font-bold">
          {pendingTrips > 0
            ? `You have ${pendingTrips} trip${pendingTrips > 1 ? 's' : ''} to run today`
            : 'No trips left to run today'}
        </h1>
      </div>

      {bankMissing && (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          You haven't added your bank details yet — add them now so you don't miss the next payroll run on the 10th.{' '}
          <Link to="/driver/payroll" className="font-semibold underline">
            Add bank details
          </Link>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Trips today" value={trips?.length ?? '—'} hint={`${pendingTrips} remaining`} to="/driver/trips" />
        <StatCard
          label="Last payroll"
          value={latestPayroll ? `₹${(latestPayroll.totalPaid ?? latestPayroll.baseSalary).toFixed(0)}` : '—'}
          hint={latestPayroll?.status ?? 'no history yet'}
          danger={latestPayroll?.status === 'FAILED'}
          to="/driver/payroll"
        />
        <StatCard label="Open grievances" value={openGrievances} to="/driver/grievances" />
        <StatCard label="Bank details" value={bankMissing ? 'Missing' : 'On file'} danger={bankMissing} to="/driver/payroll" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle>Today&rsquo;s trips</CardTitle>
            <Link to="/driver/trips" className="text-sm font-medium text-blue-600 hover:underline">
              Manage &rarr;
            </Link>
          </CardHeader>
          <CardBody>
            <ul className="divide-y divide-slate-100">
              {trips?.map((trip) => (
                <li key={trip.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium text-slate-900">
                      {trip.direction === 'MORNING_PICKUP' ? 'Morning pickup' : 'Afternoon drop'}
                    </p>
                    <p className="text-sm text-slate-500">Route #{trip.routeId}</p>
                  </div>
                  <Badge tone={STATUS_TONE[trip.status]}>{trip.status}</Badge>
                </li>
              ))}
              {trips?.length === 0 && <p className="py-3 text-sm text-slate-500">No trips scheduled for today.</p>}
            </ul>
          </CardBody>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick actions</CardTitle>
          </CardHeader>
          <CardBody className="flex flex-col gap-3">
            <Link to="/driver/trips">
              <Button variant="secondary" className="w-full justify-start">
                Start / mark today&rsquo;s trips
              </Button>
            </Link>
            <Link to="/driver/payroll">
              <Button variant="secondary" className="w-full justify-start">
                View salary history &amp; bank details
              </Button>
            </Link>
            <Link to="/driver/grievances">
              <Button variant="secondary" className="w-full justify-start">
                Raise a grievance
              </Button>
            </Link>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
