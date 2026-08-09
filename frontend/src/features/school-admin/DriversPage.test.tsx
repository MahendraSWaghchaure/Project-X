import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DriversPage } from './DriversPage';
import { useAuthStore } from '@/lib/authStore';
import type { Driver } from '@/types/people';

vi.mock('@/lib/api/drivers', () => ({
  listDrivers: vi.fn(),
  createDriver: vi.fn(),
  updateDriver: vi.fn(),
  terminateDriver: vi.fn(),
  claimDriver: vi.fn(),
  listIndependentDrivers: vi.fn(),
}));

const activeDriver: Driver = {
  id: 1,
  schoolId: 4,
  firstName: 'Ramesh',
  lastName: 'Patil',
  phone: '9876543210',
  licenseNumber: 'MH-1',
  licenseExpiry: null,
  insurancePolicyId: null,
  baseSalaryMonthly: null,
  vehicleId: null,
  employmentStatus: 'ACTIVE',
};

const independentDriver: Driver = {
  ...activeDriver,
  id: 2,
  schoolId: null,
  firstName: 'Suresh',
  lastName: 'Kumar',
  vehicleId: 9,
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <DriversPage />
    </QueryClientProvider>,
  );
}

describe('DriversPage', () => {
  beforeEach(async () => {
    useAuthStore.setState({ schoolId: 4 });
    const drivers = await import('@/lib/api/drivers');
    vi.mocked(drivers.listDrivers).mockResolvedValue([activeDriver]);
    vi.mocked(drivers.listIndependentDrivers).mockResolvedValue([independentDriver]);
  });

  it('lists the school roster and the independent driver pool', async () => {
    renderPage();

    expect(await screen.findByText('Ramesh Patil')).toBeInTheDocument();
    expect(await screen.findByText('Suresh Kumar')).toBeInTheDocument();
    expect(screen.getByText('has their own vehicle', { exact: false })).toBeInTheDocument();
  });

  it('terminates a driver and refreshes the roster', async () => {
    const drivers = await import('@/lib/api/drivers');
    vi.mocked(drivers.terminateDriver).mockResolvedValue(undefined);
    renderPage();
    await screen.findByText('Ramesh Patil');

    const rosterItem = screen.getByText('Ramesh Patil').closest('li')!;
    await userEvent.click(within(rosterItem).getByRole('button', { name: 'Terminate' }));

    await waitFor(() => expect(drivers.terminateDriver).toHaveBeenCalledWith(4, 1));
  });

  it('hires an independent driver, moving them out of the pool', async () => {
    const drivers = await import('@/lib/api/drivers');
    vi.mocked(drivers.claimDriver).mockResolvedValue({ ...independentDriver, schoolId: 4 });
    renderPage();
    await screen.findByText('Suresh Kumar');

    await userEvent.click(screen.getByRole('button', { name: 'Hire' }));

    await waitFor(() => expect(drivers.claimDriver).toHaveBeenCalledWith(4, 2));
  });

  it('opens an edit form pre-filled with the driver’s details', async () => {
    renderPage();
    const rosterItem = (await screen.findByText('Ramesh Patil')).closest('li')!;

    await userEvent.click(within(rosterItem).getByRole('button', { name: 'Edit' }));

    expect(screen.getByDisplayValue('Ramesh')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Patil')).toBeInTheDocument();
    expect(screen.getByDisplayValue('MH-1')).toBeInTheDocument();
  });
});
