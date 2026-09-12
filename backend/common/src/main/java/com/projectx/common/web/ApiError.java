package com.projectx.common.web;

import com.projectx.common.trace.TraceIds;
import java.util.List;
import org.slf4j.MDC;

public record ApiError(String code, String message, List<FieldError> fieldErrors, String traceId) {

    public record FieldError(String field, String message) {}

    /** traceId is read from MDC automatically (see TraceIdFilter) so every call site gets one for free - quote it in a bug report and it's the exact grep target across every log line for that request. */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), MDC.get(TraceIds.MDC_KEY));
    }
}
