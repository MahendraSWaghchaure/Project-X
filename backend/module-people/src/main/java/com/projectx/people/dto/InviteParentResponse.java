package com.projectx.people.dto;

/**
 * inviteToken is only returned here because module-notification's email/SMS delivery isn't wired
 * with real credentials yet (see backend README) - a school admin currently has to hand-deliver
 * the accept-invite link. Once Twilio/SMTP config lands this response should stop carrying the
 * raw token.
 */
public record InviteParentResponse(Long parentProfileId, String email, String inviteToken) {
}
