import { Link } from 'react-router-dom';
import { PublicHeader } from '@/components/layout/PublicHeader';
import { PublicFooter } from '@/components/layout/PublicFooter';
import { Button } from '@/components/ui/Button';
import { Card, CardBody } from '@/components/ui/Card';

const BENEFITS = [
  {
    role: 'Parents',
    title: 'Know exactly where your child is',
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
        <section id="home" className="bg-gradient-to-b from-blue-50 to-white px-4 py-20 text-center sm:px-6">
          <h1 className="mx-auto max-w-3xl text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
            Privately-run school transport, <span className="text-blue-600">finally accountable</span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg text-slate-600">
            Sunju brings live GPS tracking, arrival alerts, insurance coverage, and verified drivers to school vans
            and buses across India &mdash; so every parent knows where their child is, in real time.
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link to="/login">
              <Button className="px-6 py-2.5 text-base">Sign in</Button>
            </Link>
            <Link to="/driver/register">
              <Button variant="secondary" className="px-6 py-2.5 text-base">
                Register as a driver
              </Button>
            </Link>
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
            <div className="grid grid-cols-2 gap-4">
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
                  <p className="text-xs font-semibold uppercase tracking-wide text-blue-600">{b.role}</p>
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
