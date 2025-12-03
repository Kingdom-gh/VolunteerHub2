package com.volunteerhub.backend.service;
import com.volunteerhub.backend.entity.LatencyRecorder;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
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
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            HttpServletRequest req = (HttpServletRequest) request;
            String uri = req.getRequestURI();
            if (!uri.contains("healthz") && !uri.contains("readyz")) {
                latencyRecorder.record(System.currentTimeMillis() - start);
            }
        }
    }
}