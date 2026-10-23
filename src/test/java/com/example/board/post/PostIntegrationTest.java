package com.example.board.post;

import com.example.board.post.repository.PostRepository;
import com.example.board.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 PostgreSQL(Testcontainers) + Flyway 스키마로 게시글 CRUD 흐름을 검증한다.
 */
class PostIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
    }

    @Test
    void 게시글_생성_조회_목록_수정_삭제() {
        // 생성
        MvcTestResult created = mvc.post().uri("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "첫 글", "content": "본문", "author": "홍길동"}
                        """)
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.createdAt").isNotNull();
        String location = created.getResponse().getHeader("Location");
        String path = location.substring(location.indexOf("/api/v1/posts"));

        // 단건 조회
        assertThat(mvc.get().uri(path))
                .hasStatusOk()
                .bodyJson().extractingPath("$.title").isEqualTo("첫 글");

        // 목록 조회 (본문 미포함)
        MvcTestResult list = mvc.get().uri("/api/v1/posts").exchange();
        assertThat(list).hasStatusOk();
        assertThat(list).bodyJson().extractingPath("$.totalElements").asNumber().isEqualTo(1);
        assertThat(list).bodyJson().extractingPath("$.content[0].title").isEqualTo("첫 글");
        assertThat(list).bodyJson().extractingPath("$.content[0].createdAt").isNotNull();
        assertThat(list).bodyJson().doesNotHavePath("$.content[0].content");

        // 허용되지 않은 정렬 필드
        assertThat(mvc.get().uri("/api/v1/posts?sort=content,asc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_SORT_PROPERTY");

        // 수정
        assertThat(mvc.put().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "수정된 글", "content": "수정된 본문"}
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.title").isEqualTo("수정된 글");

        // 삭제
        assertThat(mvc.delete().uri(path)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(path)).hasStatus(HttpStatus.NOT_FOUND);
    }
}
