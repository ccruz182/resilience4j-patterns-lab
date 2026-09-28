package com.resilience4j.lab.service;

import com.resilience4j.lab.client.JsonPlaceholderClient;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService {

    private static final String FETCH_POST_INSTANCE = "fetchPost";

    private final JsonPlaceholderClient jsonPlaceholderClient;

    @Retry(name = FETCH_POST_INSTANCE, fallbackMethod = "fetchPostFallback")
    public String fetchPost(int postId) {
        log.debug("Attempting to fetch post with id: {}", postId);
        return jsonPlaceholderClient.getPost(postId);
    }

    // Fallback: misma firma + el Throwable al final
    private String fetchPostFallback(int postId, Throwable ex) {
        log.warn("All retry attempts exhausted for postId={}. Reason: {}", postId, ex.getMessage());
        return "{\"error\": \"Service temporarily unavailable\", \"postId\": " + postId + "}";
    }
}