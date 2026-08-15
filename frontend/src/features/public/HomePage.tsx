import { Link } from 'react-router-dom';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { Button } from '@/components/ui/Button';

interface FleetPhoto {
  src: string;
  alt: string;
  caption: string;
}

// Real, currently-in-service school buses, photographed in three different Indian cities - not
// one hero shot pretending to represent the whole country. Every bus here already shows the
// kind of safety signage (STOP arm, "SCHOOL BUS", school name + phone number on the body) this
// product's insurance and verified-driver features back up. Free-license photos (Pexels/
// Wikimedia Commons), courtesy credit in the footer - see PublicFooter.
const FLEET_PHOTOS: FleetPhoto[] = [
  {
    src: 'https://images.pexels.com/photos/28203486/pexels-photo-28203486.jpeg?auto=compress&cs=tinysrgb&w=700',
    alt: 'A fleet of yellow school buses with STOP-arm signage in Talegaon Dabhade, Maharashtra',
    caption: 'Talegaon, Maharashtra',
  },
  {
    src: 'https://upload.wikimedia.org/wikipedia/commons/thumb/0/03/School_bus%2C_Indore.jpg/960px-School_bus%2C_Indore.jpg',
    alt: 'A school bus with the school name and phone number printed on the side, Indore',
    caption: 'Indore, Madhya Pradesh',
  },
  {
    src: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/School_bus_kerala.JPG/960px-School_bus_kerala.JPG',
    alt: 'A government school bus with a STOP sign on the door, Kerala',
    caption: 'Kerala',
  },
];

const EXPLORE_LINKS = [
  {
    to: '/about',
    label: 'About Sunju',
    title: 'The problem we’re solving',
    body: '50–60% of Indian parents put their kids on transport with zero accountability. Here’s the everyday problem Sunju replaces.',
  },
  {
    to: '/vision',
    label: 'Our vision',
    title: 'Where we’re headed',
    body: 'Every school van and bus in India, trackable and insured by default — not just the fleets large operators already run well.',
  },
  {
    to: '/benefits',
    label: 'Benefits',
    title: 'What’s in it for you',
    body: 'Parents, children, schools, and drivers each get something different out of Sunju. See what it means for your role.',
  },
];

export function HomePage() {
  return (
    <PublicLayout>
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-b from-blue-50 to-white px-4 py-16 sm:px-6 sm:py-20">
        <div
          aria-hidden
          className="pointer-events-none absolute -top-24 right-0 h-96 w-96 rounded-full bg-blue-200/40 blur-3xl"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute bottom-0 left-0 h-72 w-72 rounded-full bg-amber-200/30 blur-3xl"
        />
        <div className="relative mx-auto grid max-w-6xl items-center gap-10 md:grid-cols-2">
          <div className="text-center md:text-left">
            <h1 className="mx-auto max-w-xl text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl md:mx-0">
              Privately-run school transport, <span className="text-blue-600">finally accountable</span>
            </h1>
            <p className="mx-auto mt-5 max-w-xl text-lg text-slate-600 md:mx-0">
              Sunju brings live GPS tracking, arrival alerts, insurance coverage, and verified drivers to school
              vans and buses across India &mdash; so every parent knows where their child is, in real time.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3 md:justify-start">
              <Link to="/login">
                <Button className="px-6 py-2.5 text-base">Sign in</Button>
              </Link>
              <Link to="/driver/register">
                <Button variant="secondary" className="px-6 py-2.5 text-base">
                  Register as a driver
                </Button>
              </Link>
            </div>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 md:justify-start">
              {['Live GPS tracking', 'Verified drivers', 'Insurance covered', 'Instant alerts'].map((label) => (
                <span key={label} className="flex items-center gap-1.5 text-sm font-medium text-slate-500">
                  <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4 text-blue-500">
                    <path
                      fillRule="evenodd"
                      d="M16.704 5.29a1 1 0 0 1 0 1.415l-7.5 7.5a1 1 0 0 1-1.415 0l-3.5-3.5a1 1 0 1 1 1.415-1.414L8.5 12.086l6.79-6.796a1 1 0 0 1 1.414 0Z"
                      clipRule="evenodd"
                    />
                  </svg>
                  {label}
                </span>
              ))}
            </div>
          </div>

          {/* Live-tracking mockup - visualizes what the product actually does rather than a
              generic street photo standing in for it. */}
          <div className="relative mx-auto w-full max-w-md">
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
              <div className="relative h-64 bg-gradient-to-br from-blue-50 via-slate-50 to-amber-50">
                <svg viewBox="0 0 400 260" className="absolute inset-0 h-full w-full" aria-hidden>
                  <path d="M-20 210 C 100 160, 180 250, 300 180 S 420 60, 460 90" stroke="#cbd5e1" strokeWidth="10" fill="none" />
                  <path
                    d="M-20 210 C 100 160, 180 250, 300 180 S 420 60, 460 90"
                    stroke="#f8fafc"
                    strokeWidth="2"
                    strokeDasharray="8 8"
                    fill="none"
                  />
                </svg>
                <div className="absolute left-4 top-4 max-w-[13rem] rounded-lg bg-white/95 px-3 py-2 shadow-md">
                  <p className="text-xs font-semibold text-slate-900">Route 12 &middot; MH-14-LL-2747</p>
                  <p className="text-xs font-medium text-blue-600">Arriving in 4 min</p>
                </div>
                <div className="absolute left-[58%] top-[42%] -translate-x-1/2 -translate-y-1/2">
                  <span className="absolute inline-flex h-9 w-9 animate-ping rounded-full bg-blue-400 opacity-60" />
                  <span className="relative flex h-9 w-9 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg">
                    <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5">
                      <path d="M3 6a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v7a1 1 0 0 1-1 1h-1a2 2 0 1 1-4 0H9a2 2 0 1 1-4 0H4a1 1 0 0 1-1-1V6Zm2 6.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 0 0-3 0Zm8 0a1.5 1.5 0 1 0 3 0 1.5 1.5 0 0 0-3 0ZM5 5.5v3h10v-3H5Z" />
                    </svg>
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-3 border-t border-slate-100 px-4 py-3">
                <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-green-100 text-green-600">
                  <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5">
                    <path
                      fillRule="evenodd"
                      d="M16.704 5.29a1 1 0 0 1 0 1.415l-7.5 7.5a1 1 0 0 1-1.415 0l-3.5-3.5a1 1 0 1 1 1.415-1.414L8.5 12.086l6.79-6.796a1 1 0 0 1 1.414 0Z"
                      clipRule="evenodd"
                    />
                  </svg>
                </span>
                <div>
                  <p className="text-sm font-medium text-slate-900">Aarav confirmed home safely</p>
                  <p className="text-xs text-slate-400">3:45 PM &middot; by parent</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Fleet - auto-scrolling strip of real, currently-in-service buses from different cities */}
      <section aria-label="Our fleet, across India" className="overflow-hidden border-y border-slate-100 bg-white py-10">
        <p className="mb-6 text-center text-sm font-semibold uppercase tracking-wide text-slate-400">
          Real buses, real safety signage &mdash; every city we run in
        </p>
        <div className="flex w-max animate-marquee">
          {[...FLEET_PHOTOS, ...FLEET_PHOTOS].map((photo, i) => (
            <figure key={`${photo.caption}-${i}`} className="mx-3 w-72 flex-shrink-0">
              <img
                src={photo.src}
                alt={photo.alt}
                className="h-48 w-72 rounded-xl object-cover shadow-md"
                loading={i < FLEET_PHOTOS.length ? 'eager' : 'lazy'}
              />
              <figcaption className="mt-2 text-center text-sm font-medium text-slate-500">{photo.caption}</figcaption>
            </figure>
          ))}
        </div>
      </section>

      {/* Explore - each topic now lives on its own page instead of one long scroll */}
      <section className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
        <div className="text-center">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-blue-600">Learn more</h2>
          <p className="mt-3 text-3xl font-bold text-slate-900">Everything behind the tracking map</p>
        </div>
        <div className="mt-10 grid gap-6 sm:grid-cols-3">
          {EXPLORE_LINKS.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className="group flex flex-col rounded-lg border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md"
            >
              <p className="text-xs font-semibold uppercase tracking-wide text-blue-600">{item.label}</p>
              <p className="mt-2 text-lg font-bold text-slate-900">{item.title}</p>
              <p className="mt-2 flex-1 text-sm text-slate-600">{item.body}</p>
              <span className="mt-4 text-sm font-semibold text-blue-600 group-hover:underline">Read more &rarr;</span>
            </Link>
          ))}
        </div>
      </section>
    </PublicLayout>
  );
}
