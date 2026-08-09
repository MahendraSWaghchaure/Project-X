import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role } from '@/types/auth';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  role: Role | null;
  userId: number | null;
  schoolId: number | null;
  email: string | null;
  setSession: (params: { accessToken: string; refreshToken: string; role: Role }) => void;
  clearSession: () => void;
}

interface DecodedClaims {
  userId: number | null;
  schoolId: number | null;
  email: string | null;
}

function decodeClaims(accessToken: string): DecodedClaims {
  try {
    const payload = JSON.parse(atob(accessToken.split('.')[1]));
    return {
      userId: Number(payload.sub),
      schoolId: typeof payload.schoolId === 'number' ? payload.schoolId : null,
      email: typeof payload.email === 'string' ? payload.email : null,
    };
  } catch {
    return { userId: null, schoolId: null, email: null };
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      role: null,
      userId: null,
      schoolId: null,
      email: null,
      setSession: ({ accessToken, refreshToken, role }) =>
        set({ accessToken, refreshToken, role, ...decodeClaims(accessToken) }),
      clearSession: () =>
        set({ accessToken: null, refreshToken: null, role: null, userId: null, schoolId: null, email: null }),
    }),
    { name: 'projectx-auth' },
  ),
);
