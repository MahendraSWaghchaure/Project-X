import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from './authStore';

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'none' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.signature`;
}

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
  });

  it('decodes userId, schoolId, and email from the JWT payload on setSession', () => {
    const token = fakeJwt({ sub: '42', schoolId: 7, email: 'admin@example.com' });

    useAuthStore.getState().setSession({ accessToken: token, refreshToken: 'refresh', role: 'SCHOOL_ADMIN' });

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe(token);
    expect(state.role).toBe('SCHOOL_ADMIN');
    expect(state.userId).toBe(42);
    expect(state.schoolId).toBe(7);
    expect(state.email).toBe('admin@example.com');
  });

  it('leaves schoolId null when the claim is absent (platform admin / parent tokens)', () => {
    const token = fakeJwt({ sub: '1', email: 'parent@example.com' });

    useAuthStore.getState().setSession({ accessToken: token, refreshToken: 'refresh', role: 'PARENT' });

    expect(useAuthStore.getState().schoolId).toBeNull();
  });

  it('falls back to nulls for a malformed token instead of throwing', () => {
    useAuthStore.getState().setSession({ accessToken: 'not-a-jwt', refreshToken: 'refresh', role: 'DRIVER' });

    const state = useAuthStore.getState();
    expect(state.userId).toBeNull();
    expect(state.schoolId).toBeNull();
    expect(state.email).toBeNull();
  });

  it('clearSession resets everything back to null', () => {
    const token = fakeJwt({ sub: '1', email: 'x@example.com' });
    useAuthStore.getState().setSession({ accessToken: token, refreshToken: 'refresh', role: 'PARENT' });

    useAuthStore.getState().clearSession();

    const state = useAuthStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(state.role).toBeNull();
    expect(state.userId).toBeNull();
    expect(state.schoolId).toBeNull();
    expect(state.email).toBeNull();
  });
});
