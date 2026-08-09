import { Badge } from '@/components/ui/Badge';
import type { GrievanceStatus } from '@/types/grievance';

const TONE: Record<GrievanceStatus, 'warning' | 'info' | 'success' | 'danger'> = {
  OPEN: 'warning',
  IN_PROGRESS: 'info',
  RESOLVED: 'success',
  REJECTED: 'danger',
};

export function GrievanceStatusBadge({ status }: { status: GrievanceStatus }) {
  return <Badge tone={TONE[status]}>{status.replace('_', ' ')}</Badge>;
}
