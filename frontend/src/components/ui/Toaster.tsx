import { useToastStore, type ToastTone } from '@/lib/toastStore';
import { cn } from '@/lib/cn';

const toneClasses: Record<ToastTone, string> = {
  success: 'bg-green-600',
  error: 'bg-red-600',
  info: 'bg-slate-800',
};

export function Toaster() {
  const { toasts, dismiss } = useToastStore();

  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          role="status"
          className={cn(
            'pointer-events-auto flex items-center gap-3 rounded-md px-4 py-2.5 text-sm font-medium text-white shadow-lg',
            toneClasses[t.tone],
          )}
        >
          <span>{t.message}</span>
          <button onClick={() => dismiss(t.id)} className="text-white/70 hover:text-white" aria-label="Dismiss">
            &times;
          </button>
        </div>
      ))}
    </div>
  );
}
