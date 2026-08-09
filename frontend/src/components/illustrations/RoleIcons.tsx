import type { ReactNode } from 'react';

interface IconProps {
  className?: string;
}

const wrap = (children: ReactNode) => (
  <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    {children}
  </svg>
);

export function ParentIcon({ className }: IconProps) {
  return (
    <div className={className}>
      {wrap(
        <>
          <circle cx="9" cy="6" r="2.5" />
          <path d="M4.5 19c0-3 2-5 4.5-5s4.5 2 4.5 5" />
          <circle cx="17" cy="9" r="1.8" />
          <path d="M13.5 19c0-2.3 1.5-3.8 3.5-3.8s3.5 1.5 3.5 3.8" />
        </>,
      )}
    </div>
  );
}

export function ChildIcon({ className }: IconProps) {
  return (
    <div className={className}>
      {wrap(
        <>
          <circle cx="12" cy="6" r="3" />
          <path d="M6 20c0-4 2.5-7 6-7s6 3 6 7" />
          <path d="M9 20l1-4M15 20l-1-4" />
        </>,
      )}
    </div>
  );
}

export function SchoolIcon({ className }: IconProps) {
  return (
    <div className={className}>
      {wrap(
        <>
          <path d="M3 10l9-5 9 5-9 5-9-5z" />
          <path d="M6 12v5c0 1.5 3 3 6 3s6-1.5 6-3v-5" />
          <path d="M21 10v6" />
        </>,
      )}
    </div>
  );
}

export function DriverIcon({ className }: IconProps) {
  return (
    <div className={className}>
      {wrap(
        <>
          <circle cx="12" cy="12" r="8" />
          <circle cx="12" cy="12" r="2.5" />
          <path d="M12 4v3M12 17v3M4 12h3M17 12h3M6.3 6.3l2 2M15.7 15.7l2 2M6.3 17.7l2-2M15.7 8.3l2-2" />
        </>,
      )}
    </div>
  );
}
