interface IllustrationProps {
  className?: string;
}

/** Flat-style parent-and-child silhouette (holding hands) - drawn shapes, not a photo of a real person. */
export function FamilyIllustration({ className }: IllustrationProps) {
  return (
    <svg viewBox="0 0 260 260" className={className} fill="none" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Parent and child">
      <circle cx="130" cy="130" r="120" fill="#EFF6FF" />

      {/* Connecting hands */}
      <path d="M108 165 Q130 180 158 150" stroke="#F59E0B" strokeWidth="8" strokeLinecap="round" fill="none" />

      {/* Parent */}
      <circle cx="158" cy="90" r="26" fill="#2563EB" />
      <path d="M120 210 Q158 130 196 210 Z" fill="#2563EB" />

      {/* Child */}
      <circle cx="95" cy="120" r="18" fill="#F59E0B" />
      <path d="M68 210 Q95 160 122 210 Z" fill="#F59E0B" />

      {/* Backpack accent on child */}
      <rect x="82" y="150" width="26" height="30" rx="6" fill="#EA580C" />
    </svg>
  );
}
