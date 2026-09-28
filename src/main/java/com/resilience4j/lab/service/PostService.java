package com.resilience4j.lab.service;

import com.resilience4j.lab.client.JsonPlaceholderClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService {

    private static final String INSTANCE_NAME = "fetchPost";

    private final JsonPlaceholderClient jsonPlaceholderClient;

    @CircuitBreaker(name = INSTANCE_NAME, fallbackMethod = "fetchPostCbFallback")
    @Retry(name = INSTANCE_NAME)
    public String fetchPost(int postId) {
        log.debug("Attempting to fetch post with id: {}", postId);
        return jsonPlaceholderClient.getPost(postId);
    }

    // Retry fallback — se activa cuando se agotan los reintentos
    private String fetchPostRetryFallback(int postId, Throwable ex) {
        log.warn("[RETRY-FALLBACK] All retry attempts exhausted for postId={}. Cause: {}",
                postId, ex.getMessage());
        // Re-lanza para que el Circuit Breaker lo registre como fallo
        throw new RuntimeException(ex);
    }

    // Circuit Breaker fallback — se activa cuando el CB está OPEN
    private String fetchPostCbFallback(int postId, Throwable ex) {
        String exceptionName = ex.getClass().getSimpleName();

        if (ex instanceof CallNotPermittedException) {
            log.warn("[CB-FALLBACK] Circuit is OPEN — call rejected for postId={}", postId);
        } else {
            log.warn("[CB-FALLBACK] Retries exhausted for postId={}. Cause: {}", postId, ex.getMessage());
        }

        return String.format(
                "{\"error\": \"Service temporarily unavailable\", \"postId\": %d, \"exception\": \"%s\"}",
                postId, exceptionName
        );
    }
}