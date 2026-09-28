package com.resilience4j.lab.fault;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility component to simulate controlled failures during development/testing.
 * Must NOT be used in production profiles.
 */
@Slf4j
@Component
public class FaultSimulator {

    private final AtomicInteger callCount = new AtomicInteger(0);

    private int failForFirstNCalls = 0;
    private boolean alwaysFail = false;
    private long delayMillis = 0;


    /**
     * Configures the simulator to fail for the first N calls, then succeed.
     */
    public void failForFirstNCalls(int n) {
        this.failForFirstNCalls = n;
        this.alwaysFail = false;
        this.callCount.set(0);
        log.info("[FAULT-SIM] Configured to fail for first {} call(s)", n);
    }

    /**
     * Configures the simulator to always fail.
     */
    public void alwaysFail() {
        this.alwaysFail = true;
        log.info("[FAULT-SIM] Configured to always fail");
    }

    /**
     * Resets the simulator to normal (no failures).
     */
    public void reset() {
        this.alwaysFail = false;
        this.failForFirstNCalls = 0;
        this.delayMillis = 0;
        this.callCount.set(0);
        log.info("[FAULT-SIM] Reset — no failures will be simulated");
    }

    /**
     * Call this at the beginning of any method you want to protect.
     * Throws RuntimeException if the simulator is configured to fail.
     */
    public void checkAndThrowIfNeeded(String context) {
        // delay simulation
        if (delayMillis > 0) {
            try {
                log.warn("[FAULT-SIM] Adding delay of {}ms on context='{}'", delayMillis, context);
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (alwaysFail) {
            log.warn("[FAULT-SIM] Forcing failure on context='{}'", context);
            throw new SimulatedException("Simulated failure on: " + context);
        }

        int current = callCount.incrementAndGet();
        if (current <= failForFirstNCalls) {
            log.warn("[FAULT-SIM] Forcing failure #{} of {} on context='{}'",
                    current, failForFirstNCalls, context);
            throw new SimulatedException("Simulated failure #" + current + " on: " + context);
        }
    }

    public void addDelay(long millis) {
        this.delayMillis = millis;
        log.info("[FAULT-SIM] Configured delay of {}ms", millis);
    }
}