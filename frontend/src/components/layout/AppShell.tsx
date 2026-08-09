import { NavLink, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/lib/authStore';
import { cn } from '@/lib/cn';
import { Logo } from '@/components/ui/Logo';
import type { Role } from '@/types/auth';

interface NavItem {
  label: string;
  to: string;
}

const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  PLATFORM_ADMIN: [
    { label: 'Dashboard', to: '/platform-admin/dashboard' },
    { label: 'Schools', to: '/platform-admin/schools' },
    { label: 'Grievances', to: '/platform-admin/grievances' },
    { label: 'Payments', to: '/platform-admin/payments' },
  ],
  SCHOOL_ADMIN: [
    { label: 'Dashboard', to: '/school-admin/dashboard' },
    { label: 'Vehicles', to: '/school-admin/vehicles' },
    { label: 'Drivers', to: '/school-admin/drivers' },
    { label: 'Routes', to: '/school-admin/routes' },
    { label: 'Students & Parents', to: '/school-admin/children' },
    { label: 'Trips', to: '/school-admin/trips' },
    { label: 'Grievances', to: '/school-admin/grievances' },
    { label: 'Payments', to: '/school-admin/payments' },
  ],
  DRIVER: [
    { label: "Today's Trips", to: '/driver/trips' },
    { label: 'Grievances', to: '/driver/grievances' },
    { label: 'Payroll', to: '/driver/payroll' },
  ],
  PARENT: [
    { label: 'Track Bus', to: '/parent/track' },
    { label: 'My Children', to: '/parent/children' },
    { label: 'Notifications', to: '/parent/notifications' },
    { label: 'My Profile', to: '/parent/profile' },
    { label: 'Grievances', to: '/parent/grievances' },
    { label: 'Payments', to: '/parent/payments' },
  ],
};

const ROLE_LABEL: Record<Role, string> = {
  PLATFORM_ADMIN: 'Platform Admin',
  SCHOOL_ADMIN: 'School Admin',
  DRIVER: 'Driver',
  PARENT: 'Parent',
};

export function AppShell() {
  const { role, email, clearSession } = useAuthStore();
  const navItems = role ? NAV_BY_ROLE[role] : [];

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 flex-col border-r border-slate-200 bg-white">
        <div className="px-5 py-5">
          <Logo size="sm" />
        </div>
        <nav className="flex flex-1 flex-col gap-1 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'rounded-md px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100',
                  isActive && 'bg-blue-50 text-blue-700',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
          <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {role ? ROLE_LABEL[role] : ''}
          </span>
          <div className="flex items-center gap-3">
            {email && <span className="text-sm text-slate-600">{email}</span>}
            <button
              onClick={() => {
                clearSession();
                window.location.assign('/login');
              }}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-500 hover:bg-slate-100"
            >
              Sign out
            </button>
          </div>
        </header>
        <main className="flex-1 overflow-y-auto bg-slate-50 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
