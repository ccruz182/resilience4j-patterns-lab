package com.resilience4j.lab.controller;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
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

@WebMvcTest(BulkheadStatusController.class)
class BulkheadStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BulkheadRegistry bulkheadRegistry;

    @Test
    @DisplayName("Should return bulkhead status with available slots")
    void shouldReturnBulkheadStatusWithAvailableSlots() throws Exception {
        Bulkhead bulkhead = mock(Bulkhead.class);
        Bulkhead.Metrics metrics = mock(Bulkhead.Metrics.class);

        when(bulkheadRegistry.bulkhead("fetchComments")).thenReturn(bulkhead);
        when(bulkhead.getMetrics()).thenReturn(metrics);
        when(metrics.getMaxAllowedConcurrentCalls()).thenReturn(5);
        when(metrics.getAvailableConcurrentCalls()).thenReturn(5);

        mockMvc.perform(get("/bulkheads/fetchComments/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("fetchComments"))
                .andExpect(jsonPath("$.maxConcurrentCalls").value(5))
                .andExpect(jsonPath("$.availableConcurrentCalls").value(5));
    }

    @Test
    @DisplayName("Should return reduced available slots when bulkhead is busy")
    void shouldReturnReducedAvailableSlotsWhenBusy() throws Exception {
        Bulkhead bulkhead = mock(Bulkhead.class);
        Bulkhead.Metrics metrics = mock(Bulkhead.Metrics.class);

        when(bulkheadRegistry.bulkhead("fetchComments")).thenReturn(bulkhead);
        when(bulkhead.getMetrics()).thenReturn(metrics);
        when(metrics.getMaxAllowedConcurrentCalls()).thenReturn(5);
        when(metrics.getAvailableConcurrentCalls()).thenReturn(2);

        mockMvc.perform(get("/bulkheads/fetchComments/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableConcurrentCalls").value(2));
    }
}