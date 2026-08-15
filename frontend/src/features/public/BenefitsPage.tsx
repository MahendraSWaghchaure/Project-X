import { PublicLayout } from '@/components/layout/PublicLayout';
import { Card, CardBody } from '@/components/ui/Card';
import { ParentIcon, ChildIcon, SchoolIcon, DriverIcon } from '@/components/illustrations/RoleIcons';

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

export function BenefitsPage() {
  return (
    <PublicLayout>
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="text-center">
          <h1 className="text-sm font-semibold uppercase tracking-wide text-blue-600">Benefits</h1>
          <p className="mt-3 text-3xl font-bold text-slate-900 sm:text-4xl">Something for everyone in the trip</p>
          <p className="mx-auto mt-3 max-w-2xl text-slate-600">
            Four very different people rely on the same trip going well every day. Here&rsquo;s what Sunju does for
            each of them.
          </p>
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
    </PublicLayout>
  );
}
