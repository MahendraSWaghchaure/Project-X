import { Link } from 'react-router-dom';
import { Logo } from '@/components/ui/Logo';

export function PublicFooter() {
  return (
    <footer className="border-t border-slate-200 bg-slate-900 text-slate-300">
      <div className="mx-auto grid max-w-6xl grid-cols-2 gap-8 px-4 py-12 sm:px-6 md:grid-cols-4">
        <div className="col-span-2 md:col-span-1">
          <Logo size="sm" tone="light" />
          <p className="mt-3 text-sm text-slate-400">
            Making privately-run school transport accountable, one tracked trip at a time.
          </p>
        </div>
        <div>
          <p className="text-sm font-semibold text-white">Company</p>
          <ul className="mt-3 flex flex-col gap-2 text-sm">
            <li><a href="#about" className="hover:text-white">About</a></li>
            <li><a href="#vision" className="hover:text-white">Vision</a></li>
            <li><a href="#benefits" className="hover:text-white">Benefits</a></li>
          </ul>
        </div>
        <div>
          <p className="text-sm font-semibold text-white">Get started</p>
          <ul className="mt-3 flex flex-col gap-2 text-sm">
            <li><Link to="/login" className="hover:text-white">Sign in</Link></li>
            <li><Link to="/driver/register" className="hover:text-white">Register as a driver</Link></li>
          </ul>
        </div>
        <div>
          <p className="text-sm font-semibold text-white">Contact</p>
          <ul className="mt-3 flex flex-col gap-2 text-sm">
            <li>support@sunju.example</li>
            <li>+91 00000 00000</li>
          </ul>
        </div>
      </div>
      <div className="border-t border-slate-800 px-4 py-3 text-center text-xs text-slate-500 sm:px-6">
        Photos courtesy of{' '}
        <a href="https://www.pexels.com/photo/group-of-local-schoolgirls-near-rickshaw-5909898/" target="_blank" rel="noreferrer" className="underline hover:text-white">
          Plato Terentev
        </a>{' '}
        and{' '}
        <a href="https://www.pexels.com/photo/school-bus-of-indian-schools-the-travel-soul-sandeep-kr-singh-28203486/" target="_blank" rel="noreferrer" className="underline hover:text-white">
          Sandeep Singh
        </a>{' '}
        via Pexels; &ldquo;School bus, Indore&rdquo; by Bernard Gagnon (
        <a href="https://commons.wikimedia.org/wiki/File:School_bus,_Indore.jpg" target="_blank" rel="noreferrer" className="underline hover:text-white">
          CC BY-SA 3.0
        </a>
        ) and &ldquo;School bus kerala&rdquo; by Pulari Studio Padapparamba (
        <a href="https://commons.wikimedia.org/wiki/File:School_bus_kerala.JPG" target="_blank" rel="noreferrer" className="underline hover:text-white">
          CC BY-SA 4.0
        </a>
        ) via Wikimedia Commons.
      </div>
      <div className="border-t border-slate-800 px-4 py-4 text-center text-xs text-slate-500 sm:px-6">
        &copy; {new Date().getFullYear()} Sunju School Bus Service. All rights reserved.
      </div>
    </footer>
  );
}
