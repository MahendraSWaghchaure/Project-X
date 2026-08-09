import { useQuery } from '@tanstack/react-query';
import { listMyNotifications } from '@/lib/api/notifications';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

const CHANNEL_LABEL: Record<string, string> = { SMS: 'SMS', EMAIL: 'Email', PUSH: 'Push' };

export function NotificationsPage() {
  const { data: notifications, isLoading } = useQuery({ queryKey: ['my-notifications'], queryFn: listMyNotifications });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Notifications</h1>
      <Card>
        <CardHeader>
          <CardTitle>History</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {notifications?.map((n) => (
              <li key={n.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium text-slate-900">{n.templateKey}</p>
                  <p className="text-sm text-slate-500">
                    {CHANNEL_LABEL[n.channel]} &middot; {new Date(n.createdAt).toLocaleString()}
                  </p>
                </div>
                <Badge tone={n.status === 'SENT' ? 'success' : n.status === 'FAILED' ? 'danger' : 'neutral'}>
                  {n.status.replace(/_/g, ' ').toLowerCase()}
                </Badge>
              </li>
            ))}
            {notifications?.length === 0 && <p className="py-3 text-sm text-slate-500">No notifications yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
