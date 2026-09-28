package com.resilience4j.lab.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CircuitBreakerEventLogger {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::attachListeners);

        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> attachListeners(event.getAddedEntry()));
    }

    private void attachListeners(CircuitBreaker cb) {
        cb.getEventPublisher()
                .onSuccess(e -> log.info("[CB] instance='{}' call succeeded | state={}",
                        e.getCircuitBreakerName(), cb.getState()))

                .onError(e -> log.warn("[CB] instance='{}' call failed | state={} | cause={}",
                        e.getCircuitBreakerName(), cb.getState(), e.getThrowable().getMessage()))

                .onStateTransition(e -> log.warn("[CB] instance='{}' STATE CHANGE: {} → {}",
                        e.getCircuitBreakerName(),
                        e.getStateTransition().getFromState(),
                        e.getStateTransition().getToState()))

                .onCallNotPermitted(e -> log.error("[CB] instance='{}' call REJECTED — circuit is OPEN",
                        e.getCircuitBreakerName()))

                .onSlowCallRateExceeded(e -> log.warn("[CB] instance='{}' slow call rate exceeded: {}%",
                        e.getCircuitBreakerName(), e.getSlowCallRate()));
    }
}