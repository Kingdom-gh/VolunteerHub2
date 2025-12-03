package com.volunteerhub.backend.controller;
import com.volunteerhub.backend.entity.LatencyRecorder;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class HealthCheckController {
    private final HealthEndpoint healthEndpoint;
    private final LatencyRecorder latencyRecorder;

    private static final long SLA_LIMIT_MS = 1400; // 2x SLA (700ms)
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public HealthCheckController(HealthEndpoint healthEndpoint, LatencyRecorder latencyRecorder) {
        this.healthEndpoint = healthEndpoint;
        this.latencyRecorder = latencyRecorder;
    }

    // Liveness Probe: Dùng cho Docker restart container nếu app treo cứng
    @GetMapping("/healthz")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("ALIVE");
    }

    // Readiness Probe: Dùng cho Traefik routing
    @GetMapping("/readyz")
    public ResponseEntity<?> readiness() {
        // 1. Check Infrastructure (MySQL, Redis)
        if (healthEndpoint.health().getStatus() != Status.UP) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Infrastructure Down");
        }

        // 2. Check SLA Degradation
        long p95 = latencyRecorder.getP95Latency();
        if (p95 > SLA_LIMIT_MS) {
            if (consecutiveFailures.incrementAndGet() >= 3) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Degraded: P95 latency " + p95 + "ms");
            }
        } else {
            consecutiveFailures.set(0);
        }

        return ResponseEntity.ok("READY");
    }
}