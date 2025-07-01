package com.example.sleeprism.service;

import com.example.sleeprism.dto.PostCreateRequestDTO;
import com.example.sleeprism.dto.PostResponseDTO;
import com.example.sleeprism.dto.PostUpdateRequestDTO;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.PostCategory;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.event.ViewCountIncrementEvent;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.repository.UserRepository;
import com.example.sleeprism.util.HtmlSanitizer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate; // TransactionTemplate 임포트 추가

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final TransactionTemplate transactionTemplate; // TransactionTemplate 주입

  @Autowired // ApplicationEventPublisher 주입
  private ApplicationEventPublisher eventPublisher;

  // 게시글 생성 (변동 없음)
  @Transactional
  public PostResponseDTO createPost(PostCreateRequestDTO requestDto, Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    String sanitizedContent = HtmlSanitizer.sanitize(requestDto.getContent());
    Post post = Post.builder()
        .originalAuthor(user)
        .currentOwner(user)
        .title(requestDto.getTitle())
        .content(sanitizedContent)
        .category(requestDto.getCategory())
        .build();
    Post savedPost = postRepository.save(post);
    log.info("게시글 생성 완료: ID={}", savedPost.getId());
    return new PostResponseDTO(savedPost);
  }

  public PostResponseDTO getPostById(Long postId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    // 조회수 증가는 이벤트 발행으로 분리
    eventPublisher.publishEvent(new ViewCountIncrementEvent(this, postId));

    // 게시글 데이터만 반환 (조회수는 즉시 반영되지 않음)
    return new PostResponseDTO(post);
  }

  // 게시글 수정 (변동 없음)
  @Transactional
  public PostResponseDTO updatePost(Long postId, PostUpdateRequestDTO requestDto, Long userId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    if (!post.getOriginalAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to update this post.");
    }
    if (post.isSold()) {
      throw new IllegalStateException("Sold posts cannot be updated.");
    }

    String sanitizedContent = HtmlSanitizer.sanitize(requestDto.getContent());
    post.update(requestDto.getTitle(), sanitizedContent, requestDto.getCategory());
    return new PostResponseDTO(post);
  }

  // 게시글 삭제 (소프트 삭제) (변동 없음)
  @Transactional
  public void deletePost(Long postId, Long userId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    if (!post.getOriginalAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to delete this post.");
    }
    if (post.isSold()) {
      throw new IllegalStateException("Sold posts cannot be deleted.");
    }

    post.delete();
  }

  // 모든 게시글 또는 카테고리별 게시글을 조회하는 통합 메서드
  // 파라미터 타입을 List<PostCategory>로 변경합니다.
  public List<PostResponseDTO> getPosts(List<PostCategory> categories) {
    List<Post> posts;
    // categories 리스트가 null이 아니거나 비어있지 않으면 카테고리 필터링
    if (categories != null && !categories.isEmpty()) {
      // findByCategoryInAndIsDeletedFalseOrderByCreatedAtDesc는 Collection을 인자로 받으므로 List를 그대로 전달 가능
      posts = postRepository.findByCategoryInAndIsDeletedFalseOrderByCreatedAtDesc(categories);
      log.info("{} 카테고리 게시글 조회 완료. 총 {}개의 게시글.", categories, posts.size());
    } else {
      // categories 리스트가 null이거나 비어있으면 모든 게시글 조회
      posts = postRepository.findByIsDeletedFalseOrderByCreatedAtDescFetchComments();
      log.info("모든 게시글 조회 완료. 총 {}개의 게시글.", posts.size());
    }
    return posts.stream()
        .map(PostResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 인기 게시글을 조회합니다. (좋아요 수, 북마크 수, 조회수 순으로 정렬)
   * 선택적 기간 필터링을 지원합니다.
   *
   * @param period 조회 기간 ("today", "week", "month", "all_time")
   * @return 인기 게시글 목록
   * @throws IllegalArgumentException 유효하지 않은 기간이 지정될 경우
   */
  public List<PostResponseDTO> getPopularPosts(String period) {
    List<Post> popularPosts;
    LocalDateTime startDate = null; // 시작 날짜
    LocalDateTime endDate = LocalDateTime.now(); // 현재 시간 (종료 날짜는 항상 현재)

    switch (period.toLowerCase()) {
      case "today":
        startDate = LocalDate.now().atStartOfDay(); // 오늘 자정
        break;
      case "week":
        startDate = LocalDate.now().minusWeeks(1).atStartOfDay(); // 7일 전 자정
        break;
      case "month":
        startDate = LocalDate.now().minusMonths(1).atStartOfDay(); // 한 달 전 자정
        break;
      case "all_time":
        // startDate를 null로 유지하여 모든 기간 포함
        break;
      default:
        throw new IllegalArgumentException("유효하지 않은 기간 파라미터입니다: " + period + ". 'today', 'week', 'month', 'all_time' 중 하나를 선택해주세요.");
    }

    if (startDate != null) {
      // 특정 기간 내의 게시글을 필터링하여 조회 (좋아요, 북마크, 조회수 순 정렬)
      popularPosts = postRepository.findByIsDeletedFalseAndCreatedAtBetweenOrderByLikeCountDescBookmarkCountDescViewCountDesc(startDate, endDate);
      log.info("기간 '{}' 동안의 인기 게시글 조회 완료. 총 {}개의 게시글.", period, popularPosts.size());
    } else {
      // 모든 기간의 인기 게시글 조회
      popularPosts = postRepository.findByIsDeletedFalseOrderByLikeCountDescBookmarkCountDescViewCountDesc();
      log.info("모든 기간의 인기 게시글 조회 완료. 총 {}개의 게시글.", popularPosts.size());
    }

    return popularPosts.stream()
        .map(PostResponseDTO::new)
        .collect(Collectors.toList());
  }
}
