import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/lib/authStore';
import type { Role } from '@/types/auth';

interface ProtectedRouteProps {
  allowedRoles: Role[];
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { accessToken, role } = useAuthStore();

  if (!accessToken || !role) {
    return <Navigate to="/login" replace />;
  }
  if (!allowedRoles.includes(role)) {
    return <Navigate to="/unauthorized" replace />;
  }
  return <Outlet />;
}
