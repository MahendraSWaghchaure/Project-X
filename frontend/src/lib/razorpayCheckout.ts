// Razorpay Standard Checkout ships as a CDN script, not an npm package - it renders its own modal
// (with UPI/QR/card/netbanking/wallet tabs) outside React's tree, so this is a thin imperative
// wrapper rather than a component. See PaymentController/PaymentService on the backend for the
// order-creation + signature-verification half of this flow.

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => { open(): void };
  }
}

interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  order_id: string;
  name: string;
  description?: string;
  prefill?: { name?: string; email?: string; contact?: string };
  theme?: { color?: string };
  handler: (response: RazorpaySuccessResponse) => void;
  modal?: { ondismiss?: () => void };
}

export interface RazorpaySuccessResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

let scriptLoadPromise: Promise<void> | null = null;

function loadCheckoutScript(): Promise<void> {
  if (window.Razorpay) {
    return Promise.resolve();
  }
  scriptLoadPromise ??= new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Could not load the Razorpay checkout script'));
    document.body.appendChild(script);
  });
  return scriptLoadPromise;
}

export async function openRazorpayCheckout(params: {
  keyId: string;
  orderId: string;
  amountInRupees: number;
  description: string;
  prefillName?: string;
  prefillEmail?: string;
  prefillContact?: string;
}): Promise<RazorpaySuccessResponse> {
  await loadCheckoutScript();
  if (!window.Razorpay) {
    throw new Error('Razorpay checkout did not load');
  }

  return new Promise((resolve, reject) => {
    const razorpay = new window.Razorpay!({
      key: params.keyId,
      amount: Math.round(params.amountInRupees * 100),
      currency: 'INR',
      order_id: params.orderId,
      name: 'Sunju School Bus Service',
      description: params.description,
      prefill: { name: params.prefillName, email: params.prefillEmail, contact: params.prefillContact },
      theme: { color: '#2563eb' },
      handler: (response) => resolve(response),
      modal: { ondismiss: () => reject(new Error('Payment window closed before completing')) },
    });
    razorpay.open();
  });
}
