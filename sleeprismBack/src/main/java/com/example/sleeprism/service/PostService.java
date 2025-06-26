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
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate; // TransactionTemplate 임포트 추가

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
    eventPublisher.publishEvent(new ViewCountIncrementEvent(this, postId)); // source를 'this'로 전달
    // 또는 eventPublisher.publishEvent(new ViewCountIncrementEvent(postId)); (source가 new Object()인 생성자 사용 시)

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

  // 모든 게시글 조회 (삭제되지 않은 게시글만) (변동 없음)
  public List<PostResponseDTO> getAllPosts() {
    return postRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
        .stream()
        .map(PostResponseDTO::new)
        .collect(Collectors.toList());
  }

  // 카테고리별 게시글 조회 (변동 없음)
  public List<PostResponseDTO> getPostsByCategory(PostCategory category) {
    return postRepository.findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(category)
        .stream()
        .map(PostResponseDTO::new)
        .collect(Collectors.toList());
  }
}