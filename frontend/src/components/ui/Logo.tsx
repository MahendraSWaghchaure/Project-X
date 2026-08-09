import { cn } from '@/lib/cn';

const SIZE_CLASSES = {
  sm: { brand: 'text-lg', tag: 'text-[9px]' },
  md: { brand: 'text-2xl', tag: 'text-[10px]' },
  lg: { brand: 'text-4xl sm:text-5xl', tag: 'text-xs sm:text-sm' },
} as const;

interface LogoProps {
  size?: keyof typeof SIZE_CLASSES;
  className?: string;
  tone?: 'light' | 'dark';
}

/** The brand mark: "Sunju" bold and big, "School Bus Service" small underneath - used in the public header, sidebar, and auth pages so it stays consistent everywhere. */
export function Logo({ size = 'md', className, tone = 'dark' }: LogoProps) {
  const { brand, tag } = SIZE_CLASSES[size];
  return (
    <span className={cn('inline-flex flex-col leading-none', className)}>
      <span className={cn('font-extrabold tracking-tight', brand, tone === 'dark' ? 'text-blue-600' : 'text-white')}>
        Sunju
      </span>
      <span
        className={cn(
          'mt-0.5 font-semibold uppercase tracking-wider',
          tag,
          tone === 'dark' ? 'text-slate-500' : 'text-blue-100',
        )}
      >
        School Bus Service
      </span>
    </span>
  );
}
