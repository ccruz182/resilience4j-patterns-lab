package com.resilience4j.lab.controller;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes Bulkhead runtime metrics for debugging and monitoring.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/bulkheads")
public class BulkheadStatusController {

    private final BulkheadRegistry bulkheadRegistry;

    @GetMapping("/{name}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String name) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(name);
        Bulkhead.Metrics metrics = bulkhead.getMetrics();

        return ResponseEntity.ok(Map.of(
                "name", name,
                "maxConcurrentCalls", metrics.getMaxAllowedConcurrentCalls(),
                "availableConcurrentCalls", metrics.getAvailableConcurrentCalls()
        ));
    }
}