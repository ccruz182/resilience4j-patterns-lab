package com.resilience4j.lab.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BulkheadEventLogger {

    private final BulkheadRegistry bulkheadRegistry;

    @PostConstruct
    public void registerListeners() {
        bulkheadRegistry.getAllBulkheads().forEach(this::attachListeners);

        bulkheadRegistry.getEventPublisher()
                .onEntryAdded(event -> attachListeners(event.getAddedEntry()));
    }

    private void attachListeners(Bulkhead bulkhead) {
        bulkhead.getEventPublisher()
                .onCallPermitted(e -> log.debug("[BULKHEAD] instance='{}' call permitted | available={}",
                        e.getBulkheadName(),
                        bulkhead.getMetrics().getAvailableConcurrentCalls()))

                .onCallRejected(e -> log.warn("[BULKHEAD] instance='{}' call REJECTED — too many concurrent calls | available={}",
                        e.getBulkheadName(),
                        bulkhead.getMetrics().getAvailableConcurrentCalls()))

                .onCallFinished(e -> log.debug("[BULKHEAD] instance='{}' call finished | available={}",
                        e.getBulkheadName(),
                        bulkhead.getMetrics().getAvailableConcurrentCalls()));
    }
}