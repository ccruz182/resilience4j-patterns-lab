package com.resilience4j.lab.service;

import com.resilience4j.lab.client.JsonPlaceholderClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService {

    private static final String INSTANCE_NAME = "fetchPost";
    private static final String ASYNC_INSTANCE_NAME = "fetchPostAsync";

    private final JsonPlaceholderClient jsonPlaceholderClient;

    @CircuitBreaker(name = INSTANCE_NAME, fallbackMethod = "fetchPostCbFallback")
    @Retry(name = INSTANCE_NAME)
    public String fetchPost(int postId) {
        log.debug("Attempting to fetch post with id: {}", postId);
        return jsonPlaceholderClient.getPost(postId);
    }

    @Bulkhead(name = "fetchComments", fallbackMethod = "fetchCommentsBulkheadFallback")
    public String fetchComments(int postId) {
        log.debug("Attempting to fetch comments for postId: {}", postId);
        return jsonPlaceholderClient.getComments(postId);
    }

    @TimeLimiter(name = ASYNC_INSTANCE_NAME, fallbackMethod = "fetchPostAsyncFallback")
    @CircuitBreaker(name = ASYNC_INSTANCE_NAME, fallbackMethod = "fetchPostAsyncFallback")
    public CompletableFuture<String> fetchPostAsync(int postId) {
        log.debug("Attempting async fetch for postId: {}", postId);
        return jsonPlaceholderClient.getPostAsync(postId);
    }

    private CompletableFuture<String> fetchPostAsyncFallback(int postId, Throwable ex) {
        log.warn("[ASYNC-FALLBACK] postId={}. Cause: {}", postId, ex.getMessage());
        return CompletableFuture.completedFuture(
                String.format("{\"error\": \"Request timed out or unavailable\", " +
                                "\"postId\": %d, \"exception\": \"%s\"}",
                        postId, ex.getClass().getSimpleName()));
    }

    private String fetchCommentsBulkheadFallback(int postId, Throwable ex) {
        log.warn("[BULKHEAD-FALLBACK] Too many concurrent calls for comments postId={}. Cause: {}",
                postId, ex.getMessage());
        return String.format(
                "{\"error\": \"Too many concurrent requests\", \"postId\": %d, \"exception\": \"%s\"}",
                postId, ex.getClass().getSimpleName());
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