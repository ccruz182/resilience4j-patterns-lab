package com.resilience4j.lab.client;

import com.resilience4j.lab.fault.FaultSimulator;
import com.resilience4j.lab.fault.SimulatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonPlaceholderClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FaultSimulator faultSimulator;

    private JsonPlaceholderClient client;

    @BeforeEach
    void setUp() {
        client = new JsonPlaceholderClient(restTemplate, faultSimulator,
                "https://jsonplaceholder.typicode.com");
    }

    @Test
    @DisplayName("Should call correct URL and return response")
    void shouldCallCorrectUrlAndReturnResponse() {
        when(restTemplate.getForObject(
                "https://jsonplaceholder.typicode.com/posts/1", String.class))
                .thenReturn("{\"id\": 1}");

        String result = client.getPost(1);

        assertThat(result).isEqualTo("{\"id\": 1}");
        verify(restTemplate).getForObject(
                "https://jsonplaceholder.typicode.com/posts/1", String.class);
    }

    @Test
    @DisplayName("Should throw SimulatedException when fault simulator is active")
    void shouldThrowWhenFaultSimulatorIsActive() {
        doThrow(new SimulatedException("Simulated failure"))
                .when(faultSimulator).checkAndThrowIfNeeded(anyString());

        assertThatThrownBy(() -> client.getPost(1))
                .isInstanceOf(SimulatedException.class)
                .hasMessageContaining("Simulated failure");

        verify(restTemplate, never()).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Should not call external API when fault is simulated")
    void shouldNotCallExternalApiWhenFaultIsSimulated() {
        doThrow(new SimulatedException("Forced fail"))
                .when(faultSimulator).checkAndThrowIfNeeded(anyString());

        try {
            client.getPost(5);
        } catch (SimulatedException ignored) {}

        verifyNoInteractions(restTemplate);
    }
}