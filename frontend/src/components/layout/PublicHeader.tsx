import { Link, NavLink } from 'react-router-dom';
import { Logo } from '@/components/ui/Logo';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/cn';

const NAV_LINKS = [
  { label: 'Home', to: '/' },
  { label: 'About', to: '/about' },
  { label: 'Vision', to: '/vision' },
  { label: 'Benefits', to: '/benefits' },
];

export function PublicHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
        <Link to="/">
          <Logo size="sm" />
        </Link>
        <nav className="hidden items-center gap-6 md:flex">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) =>
                cn('text-sm font-medium text-slate-600 hover:text-blue-600', isActive && 'text-blue-600')
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          <Link to="/driver/register">
            <Button variant="secondary" className="hidden sm:inline-flex">
              Register as a driver
            </Button>
          </Link>
          <Link to="/login">
            <Button>Sign in</Button>
          </Link>
        </div>
      </div>
    </header>
  );
}
