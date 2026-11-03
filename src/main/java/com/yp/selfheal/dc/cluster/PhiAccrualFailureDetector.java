package com.yp.selfheal.dc.cluster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptive Failure Detector based on the Hayashibara et al. Phi Accrual algorithm.
 * Suspicion level phi = -log10(P_later(t - t_last))
 */
@Component
public class PhiAccrualFailureDetector {

    private final int windowSize;
    private final double phiThreshold;

    private static class HeartbeatHistory {
        final LinkedList<Long> intervals = new LinkedList<>();
        long lastTimestamp = -1;

        synchronized void recordHeartbeat(long now) {
            if (lastTimestamp > 0) {
                long interval = now - lastTimestamp;
                intervals.addLast(interval);
            }
            lastTimestamp = now;
        }

        synchronized void prune(int maxWindow) {
            while (intervals.size() > maxWindow) {
                intervals.removeFirst();
            }
        }

        synchronized double mean() {
            if (intervals.isEmpty()) return 1000.0; // fallback default 1s
            long sum = 0;
            for (long i : intervals) sum += i;
            return (double) sum / intervals.size();
        }

        synchronized double variance(double mean) {
            if (intervals.size() <= 1) return 0.0;
            double sumSquares = 0.0;
            for (long i : intervals) {
                sumSquares += Math.pow(i - mean, 2);
            }
            return sumSquares / (intervals.size() - 1);
        }
    }

    private final Map<String, HeartbeatHistory> nodeHistories = new ConcurrentHashMap<>();

    public PhiAccrualFailureDetector(
            @Value("${dc.cluster.phi.window-size:50}") int windowSize,
            @Value("${dc.cluster.phi.threshold:8.0}") double phiThreshold) {
        this.windowSize = windowSize;
        this.phiThreshold = phiThreshold;
    }

    public void heartbeat(String nodeId) {
        long now = System.currentTimeMillis();
        nodeHistories.computeIfAbsent(nodeId, k -> new HeartbeatHistory()).recordHeartbeat(now);
        nodeHistories.get(nodeId).prune(windowSize);
    }

    public double phi(String nodeId) {
        HeartbeatHistory history = nodeHistories.get(nodeId);
        if (history == null || history.lastTimestamp <= 0) {
            return 0.0;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - history.lastTimestamp;
        double mean = history.mean();

        // Using exponential distribution approximation: P_later(t) = exp(-t / mean)
        // phi = -log10(P_later(t)) = (t / mean) * log10(e)
        double pLater = Math.exp(-((double) elapsed / Math.max(mean, 1.0)));
        if (pLater <= 0.0) {
            return 16.0; // max capped suspicion
        }
        return -Math.log10(Math.max(pLater, 1e-16));
    }

    public boolean isSuspect(String nodeId) {
        return phi(nodeId) >= phiThreshold;
    }

    public void removeNode(String nodeId) {
        nodeHistories.remove(nodeId);
    }
}
