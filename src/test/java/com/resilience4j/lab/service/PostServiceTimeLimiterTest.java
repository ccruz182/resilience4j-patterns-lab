package com.resilience4j.lab.service;

import com.resilience4j.lab.client.JsonPlaceholderClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
class PostServiceTimeLimiterTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private JsonPlaceholderClient jsonPlaceholderClient;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(1)
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .recordExceptions(Exception.class)
                .build();

        circuitBreakerRegistry.remove("fetchPostAsync");
        circuitBreakerRegistry.circuitBreaker("fetchPostAsync", config);
    }

    @Test
    @DisplayName("Should return response when call completes within timeout")
    void shouldReturnResponseWithinTimeout() throws ExecutionException, InterruptedException {
        when(jsonPlaceholderClient.getPostAsync(anyInt()))
                .thenReturn(CompletableFuture.completedFuture("{\"id\": 1}"));

        CompletableFuture<String> result = postService.fetchPostAsync(1);

        assertThat(result.get()).isEqualTo("{\"id\": 1}");
    }

    @Test
    @DisplayName("Should return fallback when call exceeds timeout")
    void shouldReturnFallbackWhenCallExceedsTimeout() throws ExecutionException, InterruptedException {
        // Simular llamada que tarda más que el timeout configurado (2s)
        when(jsonPlaceholderClient.getPostAsync(anyInt()))
                .thenReturn(CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "{\"id\": 1}";
                }));

        CompletableFuture<String> result = postService.fetchPostAsync(1);

        assertThat(result.get()).contains("Request timed out or unavailable");
        assertThat(result.get()).contains("TimeoutException");
    }

    @Test
    @DisplayName("Should return fallback when async call fails")
    void shouldReturnFallbackWhenAsyncCallFails() throws ExecutionException, InterruptedException {
        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Async failure"));

        when(jsonPlaceholderClient.getPostAsync(anyInt())).thenReturn(failedFuture);

        CompletableFuture<String> result = postService.fetchPostAsync(1);

        assertThat(result.get()).contains("Request timed out or unavailable");
    }
}