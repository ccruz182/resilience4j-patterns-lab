package com.resilience4j.lab.controller;

import com.resilience4j.lab.fault.FaultSimulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FaultSimulatorController.class)
class FaultSimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FaultSimulator faultSimulator;

    @Test
    @DisplayName("Should configure fail for first N calls")
    void shouldConfigureFailForFirstNCalls() throws Exception {
        mockMvc.perform(post("/fault-simulator/fail-first/3"))
                .andExpect(status().isOk())
                .andExpect(content().string("Configured to fail for first 3 call(s)"));

        verify(faultSimulator).failForFirstNCalls(3);
    }

    @Test
    @DisplayName("Should configure always fail")
    void shouldConfigureAlwaysFail() throws Exception {
        mockMvc.perform(post("/fault-simulator/always-fail"))
                .andExpect(status().isOk())
                .andExpect(content().string("Configured to always fail"));

        verify(faultSimulator).alwaysFail();
    }

    @Test
    @DisplayName("Should reset fault simulator")
    void shouldResetFaultSimulator() throws Exception {
        mockMvc.perform(post("/fault-simulator/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("Fault simulator reset"));

        verify(faultSimulator).reset();
    }
}