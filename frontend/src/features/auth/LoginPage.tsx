import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { login } from '@/lib/api/auth';
import { useAuthStore } from '@/lib/authStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import type { Role } from '@/types/auth';

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});
type FormValues = z.infer<typeof schema>;

const HOME_BY_ROLE: Record<Role, string> = {
  PLATFORM_ADMIN: '/platform-admin/dashboard',
  SCHOOL_ADMIN: '/school-admin/dashboard',
  DRIVER: '/driver/trips',
  PARENT: '/parent/track',
};

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    setServerError(null);
    try {
      const tokens = await login(values.email, values.password);
      setSession(tokens);
      navigate(HOME_BY_ROLE[tokens.role]);
    } catch {
      setServerError('Invalid email or password');
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Sign in to ProjectX</CardTitle>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <Input label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
            <Input
              label="Password"
              type="password"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />
            {serverError && <p className="text-sm text-red-600">{serverError}</p>}
            <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
              Sign in
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
