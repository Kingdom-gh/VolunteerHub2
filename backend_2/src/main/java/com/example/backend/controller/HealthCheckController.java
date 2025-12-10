package com.example.backend.controller;

import com.example.backend.entity.LatencyRecorder;
import org.springframework.boot.actuate.health.HealthComponent;
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

    private static final long SLA_LIMIT_MS = 3000;
    private static final int MAX_THREADS_THRESHOLD = 180;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public HealthCheckController(HealthEndpoint healthEndpoint, LatencyRecorder latencyRecorder) {
        this.healthEndpoint = healthEndpoint;
        this.latencyRecorder = latencyRecorder;
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("ALIVE");
    }

    @GetMapping("/readyz")
    public ResponseEntity<?> readiness() {
        var health = (HealthComponent) healthEndpoint.health();
        var details = ((org.springframework.boot.actuate.health.CompositeHealth) health).getComponents();

        if (details.containsKey("db") && Status.DOWN.equals(details.get("db").getStatus())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("CRITICAL: DB DOWN");
        }

        int activeReqs = latencyRecorder.getCurrentActiveRequests();
        if (activeReqs > MAX_THREADS_THRESHOLD) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("CRITICAL: Thread Pool Exhausted (" + activeReqs + " active requests)");
        }

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