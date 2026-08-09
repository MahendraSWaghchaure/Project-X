import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listSchools } from '@/lib/api/schools';
import { Card, CardBody } from '@/components/ui/Card';

function StatCard({ label, value, hint }: { label: string; value: number | string; hint?: string }) {
  return (
    <Card>
      <CardBody>
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <p className="mt-1 text-3xl font-bold text-slate-900">{value}</p>
        {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
      </CardBody>
    </Card>
  );
}

export function PlatformAdminDashboardPage() {
  const { data: schools, isLoading } = useQuery({ queryKey: ['schools'], queryFn: listSchools });
  const active = schools?.filter((s) => s.status === 'ACTIVE').length ?? 0;
  const suspended = schools?.filter((s) => s.status === 'SUSPENDED').length ?? 0;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Platform overview</h1>
        <Link to="/platform-admin/schools" className="text-sm font-medium text-blue-600 hover:underline">
          Manage schools &rarr;
        </Link>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <StatCard label="Schools onboarded" value={isLoading ? '—' : (schools?.length ?? 0)} />
        <StatCard label="Active" value={isLoading ? '—' : active} />
        <StatCard label="Suspended" value={isLoading ? '—' : suspended} />
      </div>

      <Card>
        <CardBody>
          <p className="text-sm text-slate-600">
            You're the platform operator - the only role that spans every school. From here you onboard new schools
            (which creates their first school admin login automatically); each school then manages its own
            vehicles, drivers, routes, and students independently.
          </p>
        </CardBody>
      </Card>
    </div>
  );
}
