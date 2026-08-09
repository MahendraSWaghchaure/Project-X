import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { toast, useToastStore } from './toastStore';

describe('toastStore', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    useToastStore.setState({ toasts: [] });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('push adds a toast with the given tone and message', () => {
    toast.success('Saved');
    const { toasts } = useToastStore.getState();
    expect(toasts).toHaveLength(1);
    expect(toasts[0]).toMatchObject({ tone: 'success', message: 'Saved' });
  });

  it('assigns unique ids across multiple toasts', () => {
    toast.success('First');
    toast.error('Second');
    const { toasts } = useToastStore.getState();
    expect(toasts).toHaveLength(2);
    expect(toasts[0].id).not.toBe(toasts[1].id);
  });

  it('auto-dismisses a toast after 4 seconds', () => {
    toast.info('Temporary');
    expect(useToastStore.getState().toasts).toHaveLength(1);

    vi.advanceTimersByTime(4000);
    expect(useToastStore.getState().toasts).toHaveLength(0);
  });

  it('dismiss removes only the targeted toast', () => {
    toast.success('Keep me');
    toast.error('Remove me');
    const [, second] = useToastStore.getState().toasts;

    useToastStore.getState().dismiss(second.id);

    const remaining = useToastStore.getState().toasts;
    expect(remaining).toHaveLength(1);
    expect(remaining[0].message).toBe('Keep me');
  });
});
