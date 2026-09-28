package com.resilience4j.lab.fault;

/**
 * Marker exception used exclusively by FaultSimulator.
 * Treated as a retryable server-side error in resilience configuration.
 */
public class SimulatedException extends RuntimeException {

    public SimulatedException(String message) {
        super(message);
    }
}