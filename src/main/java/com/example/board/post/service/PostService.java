package com.example.board.post.service;

import com.example.board.global.common.PageResponse;
import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
import com.example.board.post.domain.Post;
import com.example.board.post.dto.PostCreateRequest;
import com.example.board.post.dto.PostResponse;
import com.example.board.post.dto.PostSummaryResponse;
import com.example.board.post.dto.PostUpdateRequest;
import com.example.board.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "createdAt");

    private final PostRepository postRepository;

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        Post post = Post.create(request.title(), request.content(), request.author());
        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse get(Long postId) {
        return PostResponse.from(findPost(postId));
    }

    public PageResponse<PostSummaryResponse> getList(Pageable pageable) {
        validateSort(pageable.getSort());
        return PageResponse.from(postRepository.findAll(pageable).map(PostSummaryResponse::from));
    }

    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest request) {
        Post post = findPost(postId);
        post.update(request.title(), request.content());
        // 응답에 갱신된 updatedAt(Auditing)을 담기 위해 커밋 전에 flush한다.
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long postId) {
        postRepository.delete(findPost(postId));
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateSort(Sort sort) {
        sort.stream()
                .map(Sort.Order::getProperty)
                .filter(property -> !SORTABLE_PROPERTIES.contains(property))
                .findFirst()
                .ifPresent(property -> {
                    throw new BusinessException(ErrorCode.INVALID_SORT_PROPERTY,
                            "정렬할 수 없는 필드입니다: " + property);
                });
    }
}
