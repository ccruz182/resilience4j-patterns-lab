package com.resilience4j.lab.client;

import com.resilience4j.lab.fault.FaultSimulator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class JsonPlaceholderClient {

    private final RestTemplate restTemplate;
    private final FaultSimulator faultSimulator;
    private final String baseUrl;

    public JsonPlaceholderClient(RestTemplate restTemplate,
                                 FaultSimulator faultSimulator,
                                 @Value("${external.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.faultSimulator = faultSimulator;
        this.baseUrl = baseUrl;
    }

    public String getPost(int postId) {
        faultSimulator.checkAndThrowIfNeeded("getPost");
        String url = baseUrl + "/posts/" + postId;
        log.debug("Calling external API: GET {}", url);
        return restTemplate.getForObject(url, String.class);
    }

    public String getComments(int postId) {
        faultSimulator.checkAndThrowIfNeeded("getComments");
        String url = baseUrl + "/posts/" + postId + "/comments";
        log.debug("Calling external API: GET {}", url);
        return restTemplate.getForObject(url, String.class);
    }

    public CompletableFuture<String> getPostAsync(int postId) {
        String url = baseUrl + "/posts/" + postId;
        log.debug("Calling external API async: GET {}", url);
        return CompletableFuture.supplyAsync(() -> {
            faultSimulator.checkAndThrowIfNeeded("getPostAsync");  // ← delay dentro del Future
            return restTemplate.getForObject(url, String.class);
        });
    }
}