import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listMyChildren } from '@/lib/api/children';
import { getMyParentProfile } from '@/lib/api/parents';
import { listMyFeeInvoices, listMyWaiverRequests } from '@/lib/api/payments';
import { listMyGrievances } from '@/lib/api/grievances';
import { listMyNotifications } from '@/lib/api/notifications';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/cn';

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

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

export function ParentDashboardPage() {
  const { data: profile } = useQuery({ queryKey: ['my-parent-profile'], queryFn: getMyParentProfile });
  const { data: children } = useQuery({ queryKey: ['my-children'], queryFn: listMyChildren });
  const { data: invoices } = useQuery({ queryKey: ['my-fee-invoices'], queryFn: listMyFeeInvoices });
  const { data: grievances } = useQuery({ queryKey: ['my-grievances'], queryFn: listMyGrievances });
  const { data: notifications } = useQuery({ queryKey: ['my-notifications'], queryFn: listMyNotifications });
  const { data: waivers } = useQuery({ queryKey: ['my-waivers'], queryFn: listMyWaiverRequests });

  const activeChildren = (children ?? []).filter((c) => c.status === 'ACTIVE');
  const unpaidInvoices = (invoices ?? []).filter((i) => i.status === 'PENDING' || i.status === 'OVERDUE');
  const totalDue = unpaidInvoices.reduce((sum, i) => sum + i.totalAmount, 0);
  const overdueCount = unpaidInvoices.filter((i) => i.status === 'OVERDUE').length;
  const nextDue = [...unpaidInvoices].sort((a, b) => a.dueDate.localeCompare(b.dueDate))[0];
  const openGrievances = (grievances ?? []).filter((g) => g.status === 'OPEN' || g.status === 'IN_PROGRESS').length;
  const pendingWaivers = (waivers ?? []).filter((w) => w.status === 'PENDING').length;
  const recentNotifications = [...(notifications ?? [])]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, 4);

  const heroSubtitle =
    activeChildren.length === 0
      ? 'Once your child is registered, you will see their ride status here.'
      : activeChildren.length === 1
        ? `Here's what's happening with ${activeChildren[0].firstName}'s ride today`
        : `Here's what's happening with your children's rides today`;

  return (
    <div className="flex flex-col gap-6">
      <div className="rounded-xl bg-gradient-to-r from-blue-600 to-blue-500 px-6 py-5 text-white shadow-sm">
        <p className="text-sm text-blue-100">
          {greeting()}
          {profile?.firstName ? `, ${profile.firstName}` : ''}
        </p>
        <h1 className="mt-1 text-2xl font-bold">{heroSubtitle}</h1>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Children" value={activeChildren.length} to="/parent/children" />
        <StatCard
          label="Fee due"
          value={`₹${totalDue.toFixed(0)}`}
          hint={overdueCount > 0 ? `${overdueCount} overdue` : nextDue ? `due ${nextDue.dueDate}` : 'all clear'}
          danger={overdueCount > 0}
          to="/parent/payments"
        />
        <StatCard label="Open grievances" value={openGrievances} to="/parent/grievances" />
        <StatCard label="Notifications" value={notifications?.length ?? 0} to="/parent/notifications" />
      </div>

      {overdueCount > 0 && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
          You have {overdueCount} overdue invoice{overdueCount > 1 ? 's' : ''} totalling &#8377;{totalDue.toFixed(0)}. Late
          fees of &#8377;50/day are accruing.{' '}
          <Link to="/parent/payments" className="font-semibold underline">
            Pay now
          </Link>{' '}
          or request a waiver if this is due to an emergency.
          {pendingWaivers > 0 && <span className="ml-1">({pendingWaivers} waiver request pending review)</span>}
        </div>
      )}
      {overdueCount === 0 && nextDue && (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Next payment of &#8377;{nextDue.totalAmount.toFixed(0)} due {nextDue.dueDate}.{' '}
          <Link to="/parent/payments" className="font-semibold underline">
            Pay now, or pay several months in advance for a discount
          </Link>
          .
        </div>
      )}
      {overdueCount === 0 && !nextDue && activeChildren.length > 0 && (
        <div className="rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          You're all paid up. Nothing due right now.
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle>My children</CardTitle>
            <Link to="/parent/children" className="text-sm font-medium text-blue-600 hover:underline">
              Manage &rarr;
            </Link>
          </CardHeader>
          <CardBody>
            {activeChildren.length === 0 && <p className="text-sm text-slate-500">No children registered yet.</p>}
            <ul className="divide-y divide-slate-100">
              {activeChildren.map((child) => (
                <li key={child.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium text-slate-900">
                      {child.firstName} {child.lastName}
                    </p>
                    <p className="text-sm text-slate-500">{child.classSection ?? 'No class set'}</p>
                  </div>
                  <div className="flex gap-2">
                    <Badge tone={child.routeId ? 'success' : 'neutral'}>{child.routeId ? 'Route assigned' : 'No route'}</Badge>
                    <Badge tone={child.insurancePolicyId ? 'info' : 'neutral'}>
                      {child.insurancePolicyId ? 'Insured' : 'Not insured'}
                    </Badge>
                  </div>
                </li>
              ))}
            </ul>
          </CardBody>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick actions</CardTitle>
          </CardHeader>
          <CardBody className="flex flex-col gap-3">
            <Link to="/parent/track">
              <Button variant="secondary" className="w-full justify-start">
                Track the bus live
              </Button>
            </Link>
            <Link to="/parent/payments">
              <Button variant="secondary" className="w-full justify-start">
                Pay fees / view receipts
              </Button>
            </Link>
            <Link to="/parent/grievances">
              <Button variant="secondary" className="w-full justify-start">
                Raise a grievance
              </Button>
            </Link>
            <Link to="/parent/children">
              <Button variant="secondary" className="w-full justify-start">
                Manage children
              </Button>
            </Link>
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex items-center justify-between">
          <CardTitle>Recent notifications</CardTitle>
          <Link to="/parent/notifications" className="text-sm font-medium text-blue-600 hover:underline">
            View all &rarr;
          </Link>
        </CardHeader>
        <CardBody>
          {recentNotifications.length === 0 && <p className="text-sm text-slate-500">No notifications yet.</p>}
          <ul className="divide-y divide-slate-100">
            {recentNotifications.map((n) => (
              <li key={n.id} className="flex items-center justify-between py-2 text-sm">
                <span className="text-slate-700 capitalize">{n.templateKey.replaceAll('_', ' ').toLowerCase()}</span>
                <span className="text-slate-400">{new Date(n.createdAt).toLocaleString()}</span>
              </li>
            ))}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
