package com.example.backend.entity;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LatencyRecorder {
    private final ConcurrentLinkedDeque<Long> latencies = new ConcurrentLinkedDeque<>();
    private static final int WINDOW_SIZE = 50;

    private final AtomicInteger activeRequests = new AtomicInteger(0);

    private volatile long lastUpdatedTime = System.currentTimeMillis();

    private static final long STALE_THRESHOLD_MS = 10000;

    public void startRequest() {
        activeRequests.incrementAndGet();
    }

    public void endRequest(long durationMs) {
        activeRequests.decrementAndGet();

        lastUpdatedTime = System.currentTimeMillis();
        latencies.add(durationMs);
        if (latencies.size() > WINDOW_SIZE) {
            latencies.pollFirst();
        }
    }

    public long getP95Latency() {
        long idleTime = System.currentTimeMillis() - lastUpdatedTime;
        if (idleTime > STALE_THRESHOLD_MS) {
            latencies.clear();
            return 0;
        }

        if (latencies.isEmpty()) return 0;

        List<Long> snapshot = new ArrayList<>(latencies);
        Collections.sort(snapshot);
        int index = (int) Math.ceil(0.95 * snapshot.size()) - 1;
        return snapshot.get(Math.max(0, index));
    }

    public int getCurrentActiveRequests() {
        return activeRequests.get();
    }
}