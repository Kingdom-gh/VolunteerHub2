package com.example.backend.controller;
import com.example.backend.entity.LatencyRecorder;
import java.net.InetAddress;
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

    private static final long SLA_LIMIT_MS = 1400; // 2x SLA (700ms)
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

            if (details.containsKey("db")) {
                Status dbStatus = details.get("db").getStatus();
                if (Status.DOWN.equals(dbStatus) || Status.OUT_OF_SERVICE.equals(dbStatus)) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("CRITICAL: MySQL is DOWN. Cannot serve traffic.");
                }
            }
            if (details.containsKey("redis")) {
                Status redisStatus = details.get("redis").getStatus();
                if (Status.DOWN.equals(redisStatus)) {
                    System.out.println("WARNING: Redis is DOWN. System running in DEGRADED mode (Database only).");
                }
            }
//        // 2. Check SLA Degradation
//        long p95 = latencyRecorder.getP95Latency();
//        if (p95 > SLA_LIMIT_MS) {
//            if (consecutiveFailures.incrementAndGet() >= 3) {
//                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body("Degraded: P95 latency " + p95 + "ms");
//            }
//        } else {
//            consecutiveFailures.set(0);
//        }
//
        return ResponseEntity.ok("READY");
    }
    @GetMapping("/whoami")
    public ResponseEntity<String> whoAmI() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return ResponseEntity.ok("Hello! I am container: " + hostName);
        } catch (Exception e) {
            return ResponseEntity.ok("Unknown Container");
        }
    }
}