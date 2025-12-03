package com.example.backend.entity;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class LatencyRecorder {
    private final ConcurrentLinkedDeque<Long> latencies = new ConcurrentLinkedDeque<>();
    private static final int WINDOW_SIZE = 50;

    public void record(long durationMs) {
        latencies.add(durationMs);
        if (latencies.size() > WINDOW_SIZE) {
            latencies.pollFirst();
        }
    }

    public long getP95Latency() {
        if (latencies.isEmpty()) return 0;
        List<Long> snapshot = new ArrayList<>(latencies);
        Collections.sort(snapshot);
        int index = (int) Math.ceil(0.95 * snapshot.size()) - 1;
        return snapshot.get(Math.max(0, index));
    }
}