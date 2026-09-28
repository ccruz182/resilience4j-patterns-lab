package com.resilience4j.lab.controller;

import com.resilience4j.lab.fault.FaultSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to configure fault simulation at runtime via HTTP.
 * For development/testing purposes only.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/fault-simulator")
public class FaultSimulatorController {

    private final FaultSimulator faultSimulator;

    @PostMapping("/fail-first/{n}")
    public ResponseEntity<String> failFirstN(@PathVariable int n) {
        faultSimulator.failForFirstNCalls(n);
        return ResponseEntity.ok("Configured to fail for first " + n + " call(s)");
    }

    @PostMapping("/always-fail")
    public ResponseEntity<String> alwaysFail() {
        faultSimulator.alwaysFail();
        return ResponseEntity.ok("Configured to always fail");
    }

    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        faultSimulator.reset();
        return ResponseEntity.ok("Fault simulator reset");
    }
}