import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';

export function HomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-slate-50 px-4 text-center">
      <h1 className="text-4xl font-bold text-slate-900">ProjectX</h1>
      <p className="max-w-md text-slate-600">
        Live GPS tracking, arrival alerts, and accountability for privately-run school transport &mdash;
        so every parent knows where their child's bus is, in real time.
      </p>
      <Link to="/login">
        <Button>Sign in</Button>
      </Link>
    </div>
  );
}
