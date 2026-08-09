import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { useAuthStore } from '@/lib/authStore';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@/lib/api/auth', () => ({
  login: vi.fn(),
}));

function fakeJwt(payload: Record<string, unknown>): string {
  return `${btoa(JSON.stringify({ alg: 'none' }))}.${btoa(JSON.stringify(payload))}.sig`;
}

describe('LoginPage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    vi.clearAllMocks();
  });

  it('signs in successfully and routes the user to their role home', async () => {
    const { login } = await import('@/lib/api/auth');
    vi.mocked(login).mockResolvedValue({
      accessToken: fakeJwt({ sub: '5', email: 'sa@example.com', schoolId: 4 }),
      refreshToken: 'refresh-token',
      expiresInSeconds: 900,
      role: 'SCHOOL_ADMIN',
    });

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText('Email'), 'sa@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'password123');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/school-admin/dashboard'));
    expect(useAuthStore.getState().role).toBe('SCHOOL_ADMIN');
    expect(useAuthStore.getState().schoolId).toBe(4);
  });

  it('shows an error message and does not navigate on invalid credentials', async () => {
    const { login } = await import('@/lib/api/auth');
    vi.mocked(login).mockRejectedValue(new Error('Invalid credentials'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText('Email'), 'wrong@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'wrongpass');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
    expect(useAuthStore.getState().accessToken).toBeNull();
  });

  it('shows a validation error instead of calling the API when the email is malformed', async () => {
    const { login } = await import('@/lib/api/auth');

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText('Email'), 'not-an-email');
    await userEvent.type(screen.getByLabelText('Password'), 'password123');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Enter a valid email')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });
});
