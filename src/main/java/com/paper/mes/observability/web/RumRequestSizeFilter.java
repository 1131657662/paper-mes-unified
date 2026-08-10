package com.paper.mes.observability.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Reject oversized telemetry bodies before Jackson allocates a request object. */
@Component
public class RumRequestSizeFilter extends OncePerRequestFilter {

    private static final long MAX_BODY_BYTES = 4096;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("/api/rum".equals(request.getRequestURI())
                && request.getContentLengthLong() > MAX_BODY_BYTES) {
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Telemetry payload too large");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
