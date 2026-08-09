package com.projectx.common.exception;

/** Caller is authenticated but not allowed to act on this resource (e.g. a different school's data). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
