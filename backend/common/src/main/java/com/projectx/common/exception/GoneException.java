package com.projectx.common.exception;

/** The resource existed (or may have) but is no longer available - used for expired / already-used single-use links. Maps to HTTP 410. */
public class GoneException extends RuntimeException {

    public GoneException(String message) {
        super(message);
    }
}
