import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { acceptInvitation } from '@/lib/api/auth';
import { useAuthStore } from '@/lib/authStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';

const schema = z
  .object({
    newPassword: z.string().min(8, 'At least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });
type FormValues = z.infer<typeof schema>;

export function AcceptInvitePage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    if (!token) return;
    setServerError(null);
    try {
      const tokens = await acceptInvitation(token, values.newPassword);
      setSession(tokens);
      navigate('/parent/complete-profile');
    } catch {
      setServerError('This invite link is invalid or has expired');
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Welcome to ProjectX</CardTitle>
          <p className="mt-1 text-sm text-slate-500">Choose a password to activate your account.</p>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <Input label="New password" type="password" error={errors.newPassword?.message} {...register('newPassword')} />
            <Input
              label="Confirm password"
              type="password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            {serverError && <p className="text-sm text-red-600">{serverError}</p>}
            <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
              Activate account
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
