import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listGrievancesForSchool, updateGrievanceStatus } from '@/lib/api/grievances';
import { useAuthStore } from '@/lib/authStore';
import { useLiveTopic } from '@/hooks/useLiveTopic';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { GrievanceStatusBadge } from '@/components/grievance/GrievanceStatusBadge';
import type { Grievance, GrievanceStatus } from '@/types/grievance';

function ResolveRow({ grievance, schoolId }: { grievance: Grievance; schoolId: number }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<GrievanceStatus>(grievance.status === 'OPEN' ? 'IN_PROGRESS' : grievance.status);
  const [note, setNote] = useState(grievance.resolutionNote ?? '');

  const mutation = useMutation({
    mutationFn: () => updateGrievanceStatus(grievance.id, status, note || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['grievances', schoolId] });
      toast.success('Grievance updated');
    },
    onError: () => toast.error('Could not update grievance'),
  });

  return (
    <div className="mt-2 flex flex-col gap-2 border-t border-slate-100 pt-2 sm:flex-row sm:items-end">
      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-slate-600">Status</label>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as GrievanceStatus)}
          className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
        >
          <option value="OPEN">Open</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="RESOLVED">Resolved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>
      <div className="flex flex-1 flex-col gap-1">
        <label className="text-xs font-medium text-slate-600">Response note</label>
        <input
          value={note}
          onChange={(e) => setNote(e.target.value)}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
          placeholder="What did you do about it?"
        />
      </div>
      <Button variant="secondary" isLoading={mutation.isPending} onClick={() => mutation.mutate()}>
        Save
      </Button>
    </div>
  );
}

export function GrievancesPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const queryClient = useQueryClient();
  const { data: grievances, isLoading } = useQuery({
    queryKey: ['grievances', schoolId],
    queryFn: () => listGrievancesForSchool(schoolId!),
    enabled: schoolId != null,
  });

  useLiveTopic([schoolId ? `/topic/schools/${schoolId}/grievances` : null], () => {
    queryClient.invalidateQueries({ queryKey: ['grievances', schoolId] });
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Grievances</h1>

      <Card>
        <CardHeader>
          <CardTitle>Raised by parents and drivers</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="flex flex-col divide-y divide-slate-100">
            {grievances?.map((g) => (
              <li key={g.id} className="py-3">
                <div className="flex items-center justify-between gap-4">
                  <p className="font-medium text-slate-900">{g.subject}</p>
                  <GrievanceStatusBadge status={g.status} />
                </div>
                <p className="text-sm text-slate-500">
                  {g.raisedByRole} &middot; {g.raisedByEmail} &middot; {g.category.replace('_', ' ')} &middot;{' '}
                  {new Date(g.createdAt).toLocaleString()}
                </p>
                <p className="mt-1 text-sm text-slate-600">{g.description}</p>
                <ResolveRow grievance={g} schoolId={schoolId!} />
              </li>
            ))}
            {grievances?.length === 0 && <p className="py-3 text-sm text-slate-500">No grievances raised yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
