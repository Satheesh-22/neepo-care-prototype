package com.neepo.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = validOrGeneratedTraceId(request.getHeader(TRACE_ID_HEADER));
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        filterChain.doFilter(request, response);
    }

    public static String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TRACE_ID_ATTRIBUTE);
        return value instanceof String traceId ? traceId : UUID.randomUUID().toString();
    }

    private String validOrGeneratedTraceId(String candidate) {
        if (candidate != null) {
            try {
                return UUID.fromString(candidate).toString();
            } catch (IllegalArgumentException ignored) {
                // Invalid external trace IDs are replaced, not trusted.
            }
        }
        return UUID.randomUUID().toString();
    }
}
