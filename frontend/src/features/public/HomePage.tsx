import { Link } from 'react-router-dom';
import { PublicHeader } from '@/components/layout/PublicHeader';
import { PublicFooter } from '@/components/layout/PublicFooter';
import { Button } from '@/components/ui/Button';
import { Card, CardBody } from '@/components/ui/Card';
import { ParentIcon, ChildIcon, SchoolIcon, DriverIcon } from '@/components/illustrations/RoleIcons';

// Real photos, not stock illustrations - a mix of Pexels (free license, no attribution
// required) and Wikimedia Commons (CC BY-SA, attribution required) - courtesy credit for all
// of them is in the footer anyway, see PublicFooter.
const CHILDREN_PHOTO_URL = 'https://images.pexels.com/photos/5909898/pexels-photo-5909898.jpeg?auto=compress&cs=tinysrgb&w=500';

interface FleetPhoto {
  src: string;
  alt: string;
  caption: string;
}

// Real, currently-in-service school buses, photographed in three different Indian cities - not
// one hero shot pretending to represent the whole country. Every bus here already shows the
// kind of safety signage (STOP arm, "SCHOOL BUS", school name + phone number on the body) this
// product's insurance and verified-driver features back up.
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

const BENEFITS = [
  {
    role: 'Parents',
    title: 'Know exactly where your child is',
    Icon: ParentIcon,
    points: [
      'Live GPS location of the van/bus, just like tracking a food delivery',
      'Alerts when the vehicle is approaching your stop, and when it arrives',
      'You confirm your child reached home safely - the record is yours',
      'Delay alerts if pickup or drop-off is running late',
    ],
  },
  {
    role: 'Children',
    title: 'Safer trips, every day',
    Icon: ChildIcon,
    points: [
      'Every child is covered by insurance for the ride',
      'Boarding and drop-off are logged by the driver in real time',
      'Only verified, background-checked drivers are assigned routes',
      'A clear digital record exists for every trip taken',
    ],
  },
  {
    role: 'Schools',
    title: 'Oversight without the overhead',
    Icon: SchoolIcon,
    points: [
      'One dashboard for every vehicle, driver, and route serving your students',
      'Automatic delay escalation if a drop-off runs past dismissal time',
      'Enrollment and attendance data stays organized and auditable',
      'Onboard your own admin team and manage everything independently',
    ],
  },
  {
    role: 'Drivers',
    title: 'Fair pay, real support',
    Icon: DriverIcon,
    points: [
      'Monthly salary plus incentives for covering another route in a pinch',
      'Vehicle and personal insurance coverage included',
      'A clear, simple app for today’s route - no guesswork',
      'Independent drivers can register directly and get matched to routes',
    ],
  },
];

export function HomePage() {
  return (
    <div className="flex min-h-screen flex-col">
      <PublicHeader />

      <main className="flex-1">
        {/* Hero */}
        <section id="home" className="relative overflow-hidden bg-gradient-to-b from-blue-50 to-white px-4 py-16 sm:px-6 sm:py-20">
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
          <div className="group flex w-max animate-marquee">
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

        {/* About */}
        <section id="about" className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
          <div className="grid gap-10 md:grid-cols-2 md:items-center">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-blue-600">About Sunju</h2>
              <p className="mt-3 text-3xl font-bold text-slate-900">Built for a real, everyday problem</p>
              <p className="mt-4 text-slate-600">
                50&ndash;60% of Indian parents put their children on privately-run school vans and buses that carry
                almost no accountability &mdash; no live location, no verified driver record, no insurance
                transparency, and no way to know if today's drop-off is running late. Sunju replaces that blind spot
                with a single connected system for parents, schools, drivers, and the vehicles themselves.
              </p>
            </div>
            <div className="flex flex-col items-center gap-6">
              <img
                src={CHILDREN_PHOTO_URL}
                alt="Schoolgirls in uniform walking near Hampi, India"
                className="h-56 w-56 rounded-full object-cover shadow-lg"
                loading="lazy"
              />
              <div className="grid w-full grid-cols-2 gap-4">
                {[
                  ['Live tracking', 'Every vehicle, every trip'],
                  ['Insurance', 'Child, driver & vehicle'],
                  ['Verified drivers', 'Onboarded and accountable'],
                  ['Instant alerts', 'Arrival & delay notifications'],
                ].map(([title, desc]) => (
                  <Card key={title}>
                    <CardBody>
                      <p className="font-semibold text-slate-900">{title}</p>
                      <p className="mt-1 text-sm text-slate-500">{desc}</p>
                    </CardBody>
                  </Card>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Vision */}
        <section id="vision" className="bg-slate-900 px-4 py-20 text-center sm:px-6">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-blue-400">Our vision</h2>
          <p className="mx-auto mt-3 max-w-2xl text-3xl font-bold text-white">
            A country where no parent has to wonder if their child's ride home is safe
          </p>
          <p className="mx-auto mt-4 max-w-2xl text-slate-300">
            We want every school van and bus in India &mdash; not just fleets owned by large operators &mdash; to be
            trackable, insured, and accountable by default, and every driver behind the wheel to be treated fairly.
          </p>
        </section>

        {/* Benefits */}
        <section id="benefits" className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
          <div className="text-center">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-blue-600">Benefits</h2>
            <p className="mt-3 text-3xl font-bold text-slate-900">Something for everyone in the trip</p>
          </div>
          <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {BENEFITS.map((b) => (
              <Card key={b.role} className="flex flex-col">
                <CardBody className="flex flex-1 flex-col">
                  <b.Icon className="flex h-11 w-11 items-center justify-center rounded-full bg-blue-50 text-blue-600" />
                  <p className="mt-3 text-xs font-semibold uppercase tracking-wide text-blue-600">{b.role}</p>
                  <p className="mt-1 text-lg font-bold text-slate-900">{b.title}</p>
                  <ul className="mt-4 flex flex-1 flex-col gap-2 text-sm text-slate-600">
                    {b.points.map((point) => (
                      <li key={point} className="flex gap-2">
                        <span className="mt-1 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-blue-500" />
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                </CardBody>
              </Card>
            ))}
          </div>
        </section>

        {/* CTA */}
        <section className="bg-blue-600 px-4 py-16 text-center sm:px-6">
          <p className="text-2xl font-bold text-white">Ready to make your school's transport accountable?</p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <Link to="/login">
              <Button variant="secondary" className="px-6 py-2.5 text-base">
                Sign in
              </Button>
            </Link>
          </div>
        </section>
      </main>

      <PublicFooter />
    </div>
  );
}
