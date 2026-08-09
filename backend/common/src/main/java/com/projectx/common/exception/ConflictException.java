package com.projectx.common.exception;

/** A request would violate a uniqueness or state invariant (duplicate email, double check-in, etc). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
