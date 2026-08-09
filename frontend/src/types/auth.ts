export type Role = 'PLATFORM_ADMIN' | 'SCHOOL_ADMIN' | 'DRIVER' | 'PARENT';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  role: Role;
}

// Decoded from the JWT payload client-side for display purposes only - the server is always the
// source of truth for authorization, this is never trusted for access control decisions.
export interface DecodedAccessToken {
  sub: string;
  email: string;
  role: Role;
  schoolId?: number;
  exp: number;
}
