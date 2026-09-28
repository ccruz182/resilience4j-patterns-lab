package com.resilience4j.lab.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CircuitBreakerStatusController.class)
class CircuitBreakerStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("Should return circuit breaker status")
    void shouldReturnCircuitBreakerStatus() throws Exception {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);

        when(circuitBreakerRegistry.circuitBreaker("fetchPost")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(cb.getMetrics()).thenReturn(metrics);
        when(metrics.getFailureRate()).thenReturn(0.0f);
        when(metrics.getSlowCallRate()).thenReturn(0.0f);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(0);
        when(metrics.getNumberOfFailedCalls()).thenReturn(0);
        when(metrics.getNumberOfSuccessfulCalls()).thenReturn(5);
        when(metrics.getNumberOfNotPermittedCalls()).thenReturn(0L);

        mockMvc.perform(get("/circuit-breakers/fetchPost/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("fetchPost"))
                .andExpect(jsonPath("$.state").value("CLOSED"))
                .andExpect(jsonPath("$.successfulCalls").value(5));
    }

    @Test
    @DisplayName("Should return OPEN state when circuit breaker is open")
    void shouldReturnOpenState() throws Exception {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);

        when(circuitBreakerRegistry.circuitBreaker("fetchPost")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.OPEN);
        when(cb.getMetrics()).thenReturn(metrics);
        when(metrics.getFailureRate()).thenReturn(100.0f);
        when(metrics.getSlowCallRate()).thenReturn(0.0f);
        when(metrics.getNumberOfBufferedCalls()).thenReturn(5);
        when(metrics.getNumberOfFailedCalls()).thenReturn(5);
        when(metrics.getNumberOfSuccessfulCalls()).thenReturn(0);
        when(metrics.getNumberOfNotPermittedCalls()).thenReturn(0L);

        mockMvc.perform(get("/circuit-breakers/fetchPost/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPEN"))
                .andExpect(jsonPath("$.failureRate").value("100.0%"));
    }
}