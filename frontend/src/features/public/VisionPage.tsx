import { PublicLayout } from '@/components/layout/PublicLayout';

const PILLARS = [
  {
    title: 'Trackable by default',
    body: 'Not an add-on feature you have to ask for - every vehicle on the platform is live-tracked from day one, the same way a food delivery rider is.',
  },
  {
    title: 'Insured by default',
    body: 'Children, drivers, and vehicles all carry real insurance coverage, tracked alongside everything else instead of living in a filing cabinet.',
  },
  {
    title: 'Accountable to everyone in the trip',
    body: 'Parents, schools, and drivers all see the same facts - who boarded, when, and whether the drop-off ran on time - not three different stories.',
  },
  {
    title: 'Open to every operator, not just large fleets',
    body: 'An independent driver with their own single vehicle can register directly and get matched to a school, the same as a large fleet operator can.',
  },
];

export function VisionPage() {
  return (
    <PublicLayout>
      <section className="bg-slate-900 px-4 py-20 text-center sm:px-6">
        <h1 className="text-sm font-semibold uppercase tracking-wide text-blue-400">Our vision</h1>
        <p className="mx-auto mt-3 max-w-2xl text-3xl font-bold text-white sm:text-4xl">
          A country where no parent has to wonder if their child's ride home is safe
        </p>
        <p className="mx-auto mt-4 max-w-2xl text-slate-300">
          We want every school van and bus in India &mdash; not just fleets owned by large operators &mdash; to be
          trackable, insured, and accountable by default, and every driver behind the wheel to be treated fairly.
        </p>
      </section>

      <section className="mx-auto max-w-5xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="grid gap-6 sm:grid-cols-2">
          {PILLARS.map((pillar) => (
            <div key={pillar.title} className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
              <p className="text-lg font-bold text-slate-900">{pillar.title}</p>
              <p className="mt-2 text-sm text-slate-600">{pillar.body}</p>
            </div>
          ))}
        </div>
      </section>
    </PublicLayout>
  );
}
