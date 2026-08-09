import { Link } from 'react-router-dom';

export function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 text-center">
      <h1 className="text-2xl font-bold text-slate-900">Unauthorized</h1>
      <p className="text-slate-600">Your account doesn't have access to this page.</p>
      <Link to="/login" className="text-blue-600 hover:underline">
        Back to sign in
      </Link>
    </div>
  );
}
