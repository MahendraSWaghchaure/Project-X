interface IllustrationProps {
  className?: string;
}

/** Hand-drawn flat-style school bus - no external image assets or third-party stock photos needed. */
export function BusIllustration({ className }: IllustrationProps) {
  return (
    <svg viewBox="0 0 400 260" className={className} fill="none" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="School bus">
      <ellipse cx="200" cy="235" rx="170" ry="14" fill="#0f172a" opacity="0.06" />

      {/* Body */}
      <rect x="40" y="70" width="300" height="120" rx="20" fill="#F59E0B" />
      <rect x="40" y="70" width="300" height="120" rx="20" stroke="#B45309" strokeWidth="3" />

      {/* Roof curve */}
      <path d="M60 70 Q200 30 320 70" stroke="#B45309" strokeWidth="3" fill="none" />

      {/* Blue side stripe */}
      <rect x="40" y="150" width="300" height="16" fill="#2563EB" />

      {/* Windows */}
      {[80, 135, 190, 245].map((x) => (
        <rect key={x} x={x} y="90" width="42" height="40" rx="6" fill="#BFDBFE" stroke="#1D4ED8" strokeWidth="2" />
      ))}
      <rect x="300" y="90" width="30" height="40" rx="6" fill="#BFDBFE" stroke="#1D4ED8" strokeWidth="2" />

      {/* Door */}
      <rect x="46" y="120" width="20" height="65" rx="4" fill="#FCD34D" stroke="#B45309" strokeWidth="2" />

      {/* Headlight */}
      <circle cx="332" cy="175" r="7" fill="#FEF3C7" stroke="#B45309" strokeWidth="2" />

      {/* Wheels */}
      <circle cx="110" cy="192" r="26" fill="#111827" />
      <circle cx="110" cy="192" r="10" fill="#9CA3AF" />
      <circle cx="290" cy="192" r="26" fill="#111827" />
      <circle cx="290" cy="192" r="10" fill="#9CA3AF" />

      {/* Front bumper */}
      <rect x="335" y="150" width="10" height="35" rx="3" fill="#9CA3AF" />
    </svg>
  );
}
