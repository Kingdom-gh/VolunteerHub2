package com.example.backend.security;

import com.example.backend.entity.LatencyRecorder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LatencyFilter implements Filter {
    private final LatencyRecorder latencyRecorder;

    public LatencyFilter(LatencyRecorder latencyRecorder) {
        this.latencyRecorder = latencyRecorder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        if (uri.equals("/healthz") || uri.equals("/readyz")) {
            chain.doFilter(request, response);
            return;
        }
        long start = System.currentTimeMillis();
        latencyRecorder.startRequest();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            latencyRecorder.endRequest(duration);
        }
    }
}