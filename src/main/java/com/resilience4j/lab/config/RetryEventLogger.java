package com.resilience4j.lab.config;

import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RetryEventLogger {

    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerListeners() {
        retryRegistry.getAllRetries().forEach(this::attachListeners);

        // Cubre instancias registradas después del arranque
        retryRegistry.getEventPublisher()
                .onEntryAdded(event -> attachListeners(event.getAddedEntry()));
    }

    private void attachListeners(io.github.resilience4j.retry.Retry retry) {
        retry.getEventPublisher()
                .onRetry(e -> log.warn("[RETRY] instance='{}' attempt={} cause={}",
                        e.getName(),
                        e.getNumberOfRetryAttempts(),
                        e.getLastThrowable().getMessage()))

                .onSuccess(e -> log.info("[RETRY] instance='{}' succeeded after {} attempt(s)",
                        e.getName(),
                        e.getNumberOfRetryAttempts()))

                .onError(e -> log.error("[RETRY] instance='{}' FAILED after {} attempt(s). Final cause={}",
                        e.getName(),
                        e.getNumberOfRetryAttempts(),
                        e.getLastThrowable().getMessage()));
    }
}