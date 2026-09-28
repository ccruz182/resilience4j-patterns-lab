package com.resilience4j.lab.service;

import com.resilience4j.lab.client.JsonPlaceholderClient;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
class PostServiceBulkheadTest {

    @Autowired
    private PostService postService;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @MockitoBean
    private JsonPlaceholderClient jsonPlaceholderClient;

    @BeforeEach
    void setUp() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(5)
                .maxWaitDuration(Duration.ofMillis(0))
                .build();

        bulkheadRegistry.remove("fetchPost");
        bulkheadRegistry.bulkhead("fetchPost", config);
    }

    @Test
    @DisplayName("Should allow up to max concurrent calls")
    void shouldAllowUpToMaxConcurrentCalls() throws InterruptedException {
        when(jsonPlaceholderClient.getComments(anyInt())).thenAnswer(invocation -> {
            Thread.sleep(300);
            return "[{\"id\": 1}]";
        });

        int totalThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> postService.fetchComments(1)));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long successful = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return "error";
                    }
                })
                .filter(result -> result.contains("id"))
                .count();

        assertThat(successful).isEqualTo(5);
    }

    @Test
    @DisplayName("Should reject calls exceeding max concurrent calls")
    void shouldRejectCallsExceedingMaxConcurrentCalls() throws InterruptedException {
        when(jsonPlaceholderClient.getComments(anyInt())).thenAnswer(invocation -> {
            Thread.sleep(300);
            return "[{\"id\": 1}]";
        });

        int totalThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> postService.fetchComments(1)));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long rejected = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return "error";
                    }
                })
                .filter(result -> result.contains("Too many concurrent requests"))
                .count();

        assertThat(rejected).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return bulkhead fallback response when rejected")
    void shouldReturnBulkheadFallbackWhenRejected() throws InterruptedException {
        when(jsonPlaceholderClient.getComments(anyInt())).thenAnswer(invocation -> {
            Thread.sleep(300);
            return "[{\"id\": 1}]";
        });

        int totalThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> postService.fetchComments(1)));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        boolean anyRejected = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return "error";
                    }
                })
                .anyMatch(result -> result.contains("BulkheadFullException"));

        assertThat(anyRejected).isTrue();
    }
}