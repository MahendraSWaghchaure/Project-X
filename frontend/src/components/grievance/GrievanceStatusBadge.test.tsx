import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { GrievanceStatusBadge } from './GrievanceStatusBadge';

describe('GrievanceStatusBadge', () => {
  it('renders the status with underscores replaced by spaces', () => {
    render(<GrievanceStatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('IN PROGRESS')).toBeInTheDocument();
  });

  it.each([
    ['OPEN', 'bg-amber-100'],
    ['IN_PROGRESS', 'bg-blue-100'],
    ['RESOLVED', 'bg-green-100'],
    ['REJECTED', 'bg-red-100'],
  ] as const)('gives %s the expected tone color', (status, expectedClass) => {
    render(<GrievanceStatusBadge status={status} />);
    expect(screen.getByText(status.replace('_', ' '))).toHaveClass(expectedClass);
  });
});
