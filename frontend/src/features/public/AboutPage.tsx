import { PublicLayout } from '@/components/layout/PublicLayout';
import { Card, CardBody } from '@/components/ui/Card';

const CHILDREN_PHOTO_URL = 'https://images.pexels.com/photos/5909898/pexels-photo-5909898.jpeg?auto=compress&cs=tinysrgb&w=500';

const STATS = [
  ['Live tracking', 'Every vehicle, every trip'],
  ['Insurance', 'Child, driver & vehicle'],
  ['Verified drivers', 'Onboarded and accountable'],
  ['Instant alerts', 'Arrival & delay notifications'],
];

export function AboutPage() {
  return (
    <PublicLayout>
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="grid gap-10 md:grid-cols-2 md:items-center">
          <div>
            <h1 className="text-sm font-semibold uppercase tracking-wide text-blue-600">About Sunju</h1>
            <p className="mt-3 text-3xl font-bold text-slate-900 sm:text-4xl">Built for a real, everyday problem</p>
            <p className="mt-4 text-slate-600">
              50&ndash;60% of Indian parents put their children on privately-run school vans and buses that carry
              almost no accountability &mdash; no live location, no verified driver record, no insurance
              transparency, and no way to know if today's drop-off is running late.
            </p>
            <p className="mt-4 text-slate-600">
              Sunju replaces that blind spot with a single connected system for parents, schools, drivers, and the
              vehicles themselves. A school admin onboards their own fleet and staff; parents get a live view of
              every trip their child takes; drivers get fair, transparent pay; and every vehicle, driver, and child
              on the platform carries real insurance coverage.
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
              {STATS.map(([title, desc]) => (
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

      <section className="border-t border-slate-100 bg-slate-50 px-4 py-16 sm:px-6">
        <div className="mx-auto max-w-4xl">
          <h2 className="text-center text-2xl font-bold text-slate-900">How Sunju fits together</h2>
          <div className="mt-8 grid gap-6 sm:grid-cols-3">
            {[
              {
                step: '1',
                title: 'The platform onboards a school',
                body: 'A new school gets its own admin login, which manages that school’s vehicles, drivers, and routes independently from every other school.',
              },
              {
                step: '2',
                title: 'Parents and drivers join',
                body: 'School admins invite parents (who then register their own children) and hire drivers, including independent drivers who bring their own vehicle.',
              },
              {
                step: '3',
                title: 'Every trip is tracked and paid for, transparently',
                body: 'Live GPS, boarding/drop-off logs, grievances, fee collection, and driver payroll all run through one system, not a phone call and a WhatsApp group.',
              },
            ].map((item) => (
              <div key={item.step} className="rounded-lg border border-slate-200 bg-white p-5">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">
                  {item.step}
                </span>
                <p className="mt-3 font-semibold text-slate-900">{item.title}</p>
                <p className="mt-1 text-sm text-slate-600">{item.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </PublicLayout>
  );
}
