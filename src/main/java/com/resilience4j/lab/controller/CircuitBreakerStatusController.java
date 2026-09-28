package com.resilience4j.lab.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes Circuit Breaker runtime state for debugging and monitoring.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/circuit-breakers")
public class CircuitBreakerStatusController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/{name}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        return ResponseEntity.ok(Map.of(
                "name", name,
                "state", cb.getState(),
                "failureRate", metrics.getFailureRate() + "%",
                "slowCallRate", metrics.getSlowCallRate() + "%",
                "bufferedCalls", metrics.getNumberOfBufferedCalls(),
                "failedCalls", metrics.getNumberOfFailedCalls(),
                "successfulCalls", metrics.getNumberOfSuccessfulCalls(),
                "notPermittedCalls", metrics.getNumberOfNotPermittedCalls()
        ));
    }
}