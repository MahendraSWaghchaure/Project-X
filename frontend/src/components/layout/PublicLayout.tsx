import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { PublicHeader } from './PublicHeader';
import { PublicFooter } from './PublicFooter';
import { Button } from '@/components/ui/Button';

/** Shared chrome for every public marketing page (Home/About/Vision/Benefits) - header, a
 * closing sign-in CTA, and the footer, so each page only has to own its own content. */
export function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <PublicHeader />
      <main className="flex-1">{children}</main>
      <section className="bg-blue-600 px-4 py-16 text-center sm:px-6">
        <p className="text-2xl font-bold text-white">Ready to make your school&rsquo;s transport accountable?</p>
        <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
          <Link to="/login">
            <Button variant="secondary" className="px-6 py-2.5 text-base">
              Sign in
            </Button>
          </Link>
        </div>
      </section>
      <PublicFooter />
    </div>
  );
}
