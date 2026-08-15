import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
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
    { label: 'Insurance', to: '/school-admin/insurance' },
    { label: 'Grievances', to: '/school-admin/grievances' },
    { label: 'Payments', to: '/school-admin/payments' },
  ],
  DRIVER: [
    { label: 'Dashboard', to: '/driver/dashboard' },
    { label: "Today's Trips", to: '/driver/trips' },
    { label: 'Grievances', to: '/driver/grievances' },
    { label: 'Payroll', to: '/driver/payroll' },
  ],
  PARENT: [
    { label: 'Dashboard', to: '/parent/dashboard' },
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

function useClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(id);
  }, []);
  return now;
}

const DATE_FORMAT = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
const TIME_FORMAT = new Intl.DateTimeFormat('en-IN', {
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: true,
});

export function AppShell() {
  const { role, email, clearSession } = useAuthStore();
  const navigate = useNavigate();
  const now = useClock();
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
          <div className="flex items-center gap-3">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              {role ? ROLE_LABEL[role] : ''}
            </span>
            <span className="hidden items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500 sm:flex">
              <span>{DATE_FORMAT.format(now)}</span>
              <span className="text-slate-300">&middot;</span>
              <span className="tabular-nums">{TIME_FORMAT.format(now)}</span>
            </span>
          </div>
          <div className="flex items-center gap-3">
            {role === 'PARENT' && (
              <button
                onClick={() => navigate('/parent/track')}
                title="Track bus live"
                className="flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
              >
                <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4">
                  <path d="M10 2a6 6 0 0 0-6 6c0 4.5 6 10 6 10s6-5.5 6-10a6 6 0 0 0-6-6Zm0 8.25A2.25 2.25 0 1 1 10 5.75a2.25 2.25 0 0 1 0 4.5Z" />
                </svg>
                Track bus
              </button>
            )}
            {email && <span className="hidden text-sm text-slate-600 md:inline">{email}</span>}
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
