package com.projectx.notification.sender;

/** Thrown by a sender when its channel isn't configured yet (e.g. no Twilio creds) - distinct from an actual send failure so the audit log can tell "skipped" apart from "broke". */
public class ChannelNotConfiguredException extends RuntimeException {

    public ChannelNotConfiguredException(String message) {
        super(message);
    }
}
