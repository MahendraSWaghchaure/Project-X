package com.projectx.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns every request a trace id - reused from the caller's own {@value TraceIds#HEADER} header
 * if they sent one (so a request that already carries a correlation id from an upstream system,
 * e.g. a future API gateway, keeps it end to end), otherwise a fresh random one. Stored in SLF4J's
 * MDC for the lifetime of the request so every log line written while handling it - including the
 * ones GlobalExceptionHandler's catch-all writes for a 500 - carries the same id, and echoed back
 * on the response header so a client (or a person reporting a bug) can quote it.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} on a plain {@code @Component} filter runs this before
 * Spring Security's whole filter chain (including RateLimitFilter, wired in further down inside
 * SecurityConfig) - even a 401/403/429 response gets a trace id, which matters most exactly when
 * something is going wrong.
 *
 * <p>MDC is thread-bound and Tomcat reuses worker threads across requests, so the {@code finally}
 * block clearing it is load-bearing, not defensive style - without it a later request on the same
 * pooled thread would inherit a stale trace id if it never got as far as this filter setting its
 * own (it always does, but the cost of being wrong here is silently mislabeled logs, so clear
 * unconditionally rather than trust that).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** A client-supplied id is only ever reused if it looks like this - otherwise it's silently replaced with a fresh one rather than trusted verbatim into logs (log-line forging via embedded CR/LF) and a response header. */
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[a-zA-Z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TraceIds.HEADER);
        if (traceId == null || !SAFE_TRACE_ID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TraceIds.MDC_KEY, traceId);
        response.setHeader(TraceIds.HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.MDC_KEY);
        }
    }
}
