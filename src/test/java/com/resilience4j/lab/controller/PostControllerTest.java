package com.resilience4j.lab.controller;

import com.resilience4j.lab.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("Should return post response with status 200")
    void shouldReturnPostWithStatus200() throws Exception {
        when(postService.fetchPost(anyInt())).thenReturn("{\"id\": 1}");

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"id\": 1}"));

        verify(postService).fetchPost(1);
    }

    @Test
    @DisplayName("Should return fallback response when service returns error")
    void shouldReturnFallbackResponse() throws Exception {
        when(postService.fetchPost(anyInt()))
                .thenReturn("{\"error\": \"Service temporarily unavailable\", \"postId\": 1}");

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "{\"error\": \"Service temporarily unavailable\", \"postId\": 1}"));
    }

    @Test
    @DisplayName("Should return comments response with status 200")
    void shouldReturnCommentsWithStatus200() throws Exception {
        when(postService.fetchComments(anyInt())).thenReturn("[{\"id\": 1}]");

        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"id\": 1}]"));

        verify(postService).fetchComments(1);
    }

    @Test
    @DisplayName("Should return 400 when post id is not a number")
    void shouldReturn400WhenPostIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/posts/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return async post response with status 200")
    void shouldReturnAsyncPostWithStatus200() throws Exception {
        when(postService.fetchPostAsync(anyInt()))
                .thenReturn(CompletableFuture.completedFuture("{\"id\": 1}"));

        mockMvc.perform(asyncDispatch(
                        mockMvc.perform(get("/api/posts/1/async"))
                                .andExpect(request().asyncStarted())
                                .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"id\": 1}"));

        verify(postService).fetchPostAsync(1);
    }

    @Test
    @DisplayName("Should return fallback response when async call times out")
    void shouldReturnFallbackWhenAsyncTimesOut() throws Exception {
        when(postService.fetchPostAsync(anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        "{\"error\": \"Request timed out or unavailable\", \"postId\": 1, \"exception\": \"TimeoutException\"}"));

        mockMvc.perform(asyncDispatch(
                        mockMvc.perform(get("/api/posts/1/async"))
                                .andExpect(request().asyncStarted())
                                .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("timed out")));

        verify(postService).fetchPostAsync(1);
    }
}