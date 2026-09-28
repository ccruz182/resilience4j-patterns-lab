package com.resilience4j.lab.service;

import com.resilience4j.lab.client.JsonPlaceholderClient;
import com.resilience4j.lab.fault.SimulatedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private JsonPlaceholderClient jsonPlaceholderClient;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig testConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(1)
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .recordExceptions(SimulatedException.class, RuntimeException.class)
                .build();

        circuitBreakerRegistry.remove("fetchPost");
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("fetchPost", testConfig);
    }


    @Test
    @DisplayName("Should succeed on first attempt without retrying")
    void shouldSucceedOnFirstAttempt() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenReturn("{\"id\": 1}");

        String result = postService.fetchPost(1);

        assertThat(result).isEqualTo("{\"id\": 1}");
        verify(jsonPlaceholderClient, times(1)).getPost(1);
    }

    @Test
    @DisplayName("Should retry and succeed on second attempt")
    void shouldRetryAndSucceedOnSecondAttempt() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail #1"))
                .thenReturn("{\"id\": 1}");

        String result = postService.fetchPost(1);

        assertThat(result).isEqualTo("{\"id\": 1}");
        verify(jsonPlaceholderClient, times(2)).getPost(1);
    }

    @Test
    @DisplayName("Should return CB fallback after exhausting all retry attempts")
    void shouldReturnFallbackAfterExhaustingRetries() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail"));

        String result = postService.fetchPost(1);

        assertThat(result).contains("Service temporarily unavailable");
        assertThat(result).contains("\"postId\": 1");
        assertThat(result).contains("SimulatedException");
        verify(jsonPlaceholderClient, times(3)).getPost(1);
    }

    @Test
    @DisplayName("Should count one failure per request not per retry attempt")
    void shouldCountOneFailurePerRequestNotPerRetryAttempt() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail"));

        postService.fetchPost(1);

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                .isEqualTo(1);
        verify(jsonPlaceholderClient, times(3)).getPost(1);
    }

    // ─── Circuit Breaker tests ──────────────────────────────────────────

    @Test
    @DisplayName("Should open circuit breaker after failure threshold is reached")
    void shouldOpenCircuitBreakerAfterFailureThreshold() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail"));

        for (int i = 0; i < 5; i++) {
            postService.fetchPost(1);
        }

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should return fallback immediately when circuit breaker is open")
    void shouldReturnFallbackImmediatelyWhenCircuitIsOpen() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail"));

        for (int i = 0; i < 5; i++) {
            postService.fetchPost(1);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        System.out.println("State before call: " + circuitBreaker.getState());
        reset(jsonPlaceholderClient);

        String result = postService.fetchPost(1);
        System.out.println("Result: " + result);

        assertThat(result).contains("Service temporarily unavailable");
        assertThat(result).contains("CallNotPermittedException");
        verify(jsonPlaceholderClient, never()).getPost(anyInt());
    }

    @Test
    @DisplayName("Should transition to HALF_OPEN and close after successful calls")
    void shouldCloseAfterSuccessfulCallsInHalfOpen() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail"));

        for (int i = 0; i < 5; i++) {
            postService.fetchPost(1);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        circuitBreaker.transitionToHalfOpenState();

        // Reset limpio del mock antes de reconfigurar comportamiento
        reset(jsonPlaceholderClient);
        when(jsonPlaceholderClient.getPost(anyInt())).thenReturn("{\"id\": 1}");

        postService.fetchPost(1);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Should stay OPEN if failures continue in HALF_OPEN state")
    void shouldStayOpenIfFailuresContinueInHalfOpen() {
        when(jsonPlaceholderClient.getPost(anyInt()))
                .thenThrow(new SimulatedException("Fail"));

        for (int i = 0; i < 5; i++) {
            postService.fetchPost(1);
        }

        circuitBreaker.transitionToHalfOpenState();

        // Con permitted=1, una llamada (= 3 reintentos internos) es suficiente
        // para que el CB evalúe y vuelva a OPEN
        postService.fetchPost(1);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}