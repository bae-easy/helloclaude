package com.example.board.global.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page&lt;T&gt;를 직접 직렬화하지 않고 안정적인 페이징 응답 형식으로 변환한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
