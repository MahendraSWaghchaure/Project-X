import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { inviteParent } from '@/lib/api/parents';
import { listChildrenForSchool } from '@/lib/api/children';
import { useAuthStore } from '@/lib/authStore';
import { toast } from '@/lib/toastStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';

const schema = z.object({
  email: z.string().email(),
  phone: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

export function ChildrenPage() {
  const schoolId = useAuthStore((state) => state.schoolId);
  const [inviteLink, setInviteLink] = useState<string | null>(null);
  const { data: children, isLoading } = useQuery({
    queryKey: ['children', schoolId],
    queryFn: () => listChildrenForSchool(schoolId!),
    enabled: schoolId != null,
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const inviteMutation = useMutation({
    mutationFn: (values: FormValues) => inviteParent(schoolId!, values.email, values.phone),
    onSuccess: (response) => {
      setInviteLink(`${window.location.origin}/accept-invite/${response.inviteToken}`);
      reset();
      toast.success('Invite created');
    },
    onError: () => toast.error('Could not send invite'),
  });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold text-slate-900">Students</h1>

      <Card>
        <CardHeader>
          <CardTitle>Invite a parent</CardTitle>
          <p className="mt-1 text-sm text-slate-500">
            Parents register their own children once they accept their invite. SMS/email delivery isn't wired up with
            real credentials yet, so share the generated link with them directly for now.
          </p>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit((values) => inviteMutation.mutate(values))} className="flex flex-wrap items-end gap-3">
            <Input label="Parent email" type="email" error={errors.email?.message} {...register('email')} />
            <Input label="Parent phone (optional)" {...register('phone')} />
            <Button type="submit" isLoading={isSubmitting}>
              Send invite
            </Button>
          </form>
          {inviteLink && (
            <p className="mt-3 break-all rounded-md bg-slate-50 p-3 text-sm text-slate-700">
              Invite link: <span className="font-mono">{inviteLink}</span>
            </p>
          )}
          {inviteMutation.isError && <p className="mt-2 text-sm text-red-600">Could not send invite.</p>}
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Enrolled children</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading && <p className="text-sm text-slate-500">Loading&hellip;</p>}
          <ul className="divide-y divide-slate-100">
            {children?.map((child) => (
              <li key={child.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium text-slate-900">
                    {child.firstName} {child.lastName}
                  </p>
                  <p className="text-sm text-slate-500">{child.classSection ?? 'No class recorded'}</p>
                </div>
                <span className="text-sm text-slate-500">{child.routeId ? `Route #${child.routeId}` : 'No route yet'}</span>
              </li>
            ))}
            {children?.length === 0 && <p className="py-3 text-sm text-slate-500">No children enrolled yet.</p>}
          </ul>
        </CardBody>
      </Card>
    </div>
  );
}
