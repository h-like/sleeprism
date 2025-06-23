package com.example.sleeprism.service;

import com.example.sleeprism.dto.PostCreateRequestDTO;
import com.example.sleeprism.dto.PostResponseDTO;
import com.example.sleeprism.dto.PostUpdateRequestDTO;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.PostCategory;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.repository.UserRepository;
import com.example.sleeprism.util.HtmlSanitizer; // 이 유틸리티 클래스가 존재한다고 가정
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Spring의 @Transactional 사용

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션, 쓰기 메서드에서 @Transactional 재선언
public class PostService {
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  // 게시글 생성
  @Transactional // 쓰기 작업이므로 트랜잭션 필요
  public PostResponseDTO createPost(PostCreateRequestDTO requestDto, Long userId) {
    // User 엔티티를 찾아와서 Post와 연결
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // HTML Sanitization 적용
    String sanitizedContent = HtmlSanitizer.sanitize(requestDto.getContent());

    Post post = Post.builder()
        .user(user) // Post 엔티티의 @Builder에서 user는 originalAuthor와 currentOwner 모두를 설정합니다.
        .title(requestDto.getTitle())
        .content(sanitizedContent)
        .category(requestDto.getCategory())
        .build();

    Post savedPost = postRepository.save(post);
    return new PostResponseDTO(savedPost); // DTO로 변환하여 반환
  }

  // 특정 게시글 조회 (조회수 증가)
  @Transactional // 조회수 증가가 쓰기 작업이므로 트랜잭션 필요
  public PostResponseDTO getPostById(Long postId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));
    post.incrementViewCount(); // 조회수 증가
    return new PostResponseDTO(post);
  }

  // 게시글 수정
  @Transactional
  public PostResponseDTO updatePost(Long postId, PostUpdateRequestDTO requestDto, Long userId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    // 게시글 수정 권한 검증: 원본 작성자만 수정 가능하며, 이미 판매된 게시글은 수정 불가
    if (!post.getOriginalAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to update this post.");
    }
    if (post.isSold()) {
      throw new IllegalStateException("Sold posts cannot be updated.");
    }

    // HTML Sanitization 적용
    String sanitizedContent = HtmlSanitizer.sanitize(requestDto.getContent());

    post.update(requestDto.getTitle(), sanitizedContent, requestDto.getCategory());
    // save를 명시적으로 호출하지 않아도 @Transactional에 의해 변경 감지(Dirty Checking) 후 업데이트됨
    return new PostResponseDTO(post);
  }

  // 게시글 삭제 (소프트 삭제)
  @Transactional
  public void deletePost(Long postId, Long userId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    // 게시글 삭제 권한 검증: 원본 작성자만 삭제 가능하며, 이미 판매된 게시글은 삭제 불가
    if (!post.getOriginalAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to delete this post.");
    }
    if (post.isSold()) {
      throw new IllegalStateException("Sold posts cannot be deleted.");
    }

    post.delete(); // isDeleted를 true로 변경
    // postRepository.save(post); // Dirty Checking으로 자동 저장될 수 있으나, 명시적으로 호출해도 무방
  }

  // 모든 게시글 조회 (삭제되지 않은 게시글만)
  public List<PostResponseDTO> getAllPosts() {
    return postRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
        .stream()
        .map(PostResponseDTO::new)
        .collect(Collectors.toList());
  }

  // 카테고리별 게시글 조회
  public List<PostResponseDTO> getPostsByCategory(PostCategory category) {
    return postRepository.findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(category)
        .stream()
        .map(PostResponseDTO::new)
        .collect(Collectors.toList());
  }

  // TODO: 검색, 인기 게시글 등의 추가 메서드 구현 (기존 PostService 로직과 동일하게 유지)

}