package com.resilience4j.lab.config;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TimeLimiterEventLogger {

    private final TimeLimiterRegistry timeLimiterRegistry;

    @PostConstruct
    public void registerListeners() {
        timeLimiterRegistry.getAllTimeLimiters().forEach(this::attachListeners);

        timeLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> attachListeners(event.getAddedEntry()));
    }

    private void attachListeners(TimeLimiter timeLimiter) {
        timeLimiter.getEventPublisher()
                .onSuccess(e -> log.info("[TIME-LIMITER] instance='{}' call completed within timeout",
                        e.getTimeLimiterName()))

                .onTimeout(e -> log.warn("[TIME-LIMITER] instance='{}' call TIMED OUT",
                        e.getTimeLimiterName()))

                .onError(e -> log.error("[TIME-LIMITER] instance='{}' call failed. Cause: {}",
                        e.getTimeLimiterName(), e.getThrowable().getMessage()));
    }
}