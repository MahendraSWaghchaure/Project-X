package com.projectx.common.trace;

/** Shared constants for request-tracing, so the header name and MDC key can't drift between TraceIdFilter, GlobalExceptionHandler, and anything else that reads/writes one. */
public final class TraceIds {

    /** Accepted from an incoming request (e.g. a caller replaying their own correlation id) and always echoed back on the response - a widely-recognized convention (GitHub, Heroku, AWS ALB all use this exact header name). */
    public static final String HEADER = "X-Request-Id";

    /** SLF4J MDC key - include %X{traceId} in a log pattern to see it on every line for a request, including lines logged from the async executors that copy MDC via MdcTaskDecorator. */
    public static final String MDC_KEY = "traceId";

    private TraceIds() {
    }
}
