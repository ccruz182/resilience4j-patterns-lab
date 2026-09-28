package com.resilience4j.lab.controller;

import com.resilience4j.lab.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @GetMapping("/{id}")
    public ResponseEntity<String> getPost(@PathVariable int id) {
        return ResponseEntity.ok(postService.fetchPost(id));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<String> getComments(@PathVariable int id) {
        return ResponseEntity.ok(postService.fetchComments(id));
    }

    @GetMapping("/{id}/async")
    public CompletableFuture<ResponseEntity<String>> getPostAsync(@PathVariable int id) {
        return postService.fetchPostAsync(id)
                .thenApply(ResponseEntity::ok);
    }
}