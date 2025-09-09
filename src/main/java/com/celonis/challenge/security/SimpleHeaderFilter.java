package com.celonis.challenge.security;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import java.io.IOException;

@Component
public class SimpleHeaderFilter extends OncePerRequestFilter {

    private final String HEADER_NAME = "Celonis-Auth";
    private final String headerValue;

    public SimpleHeaderFilter(@Value("${security.header.value:totally_secret}") String headerValue) {
        this.headerValue = headerValue;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // OPTIONS should always work
        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow actuator endpoints without auth for health/readiness/metrics
        String uri = request.getRequestURI();
        if (uri != null && (uri.equals("/actuator/health") || uri.startsWith("/actuator"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = java.util.UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);
        String val = request.getHeader(HEADER_NAME);
        if (val == null || !val.equals(headerValue)) {
            response.setStatus(401);
            response.getWriter().append("Not authorized");
            MDC.clear();
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
