package com.projectx.tracking.ws;

/** Envelope carried over the Redis pub/sub relay channel - the STOMP destination plus the already-serialized JSON body to forward verbatim. */
record StompRelayMessage(String destination, String payloadJson) {
}
