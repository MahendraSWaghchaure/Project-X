import { AxiosError } from 'axios';

/** Pulls the backend's ApiError.message out of a failed axios call, for the cases where it's worth showing verbatim (e.g. "Razorpay is not configured"). */
export function backendErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof AxiosError) {
    const message = error.response?.data?.error?.message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
  }
  return fallback;
}
