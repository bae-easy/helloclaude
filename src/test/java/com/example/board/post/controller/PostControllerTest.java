package com.example.board.post.controller;

import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
import com.example.board.post.dto.PostCreateRequest;
import com.example.board.post.dto.PostResponse;
import com.example.board.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PostService postService;

    @Test
    void create_201과_Location_헤더를_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        given(postService.create(new PostCreateRequest("제목", "내용", "작성자")))
                .willReturn(new PostResponse(1L, "제목", "내용", "작성자", now, now));

        MvcTestResult result = mvc.post().uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "제목", "content": "내용", "author": "작성자"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/v1/posts/1");
        assertThat(result).bodyJson().extractingPath("$.id").asNumber().isEqualTo(1);
    }

    @Test
    void create_검증_실패시_400과_필드_에러를_반환한다() {
        MvcTestResult result = mvc.post().uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "", "content": "내용", "author": "작성자"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT_VALUE");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");
    }

    @Test
    void get_존재하지_않으면_404_ProblemDetail을_반환한다() {
        given(postService.get(99L)).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        MvcTestResult result = mvc.get().uri("/api/v1/posts/99").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("POST_NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(404);
    }
}
