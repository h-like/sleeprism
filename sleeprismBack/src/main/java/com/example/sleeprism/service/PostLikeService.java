package com.example.sleeprism.service;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.PostLike;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.PostLikeRepository;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostLikeService {

  private final PostLikeRepository postLikeRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * 게시글에 좋아요를 추가합니다.
   * 이미 좋아요를 누른 경우, 좋아요를 취소합니다 (토글 기능).
   *
   * @param postId 좋아요를 누를 게시글의 ID
   * @param userId 좋아요를 누른 사용자의 ID
   * @return 좋아요 추가/취소 성공 여부 (true: 좋아요 추가, false: 좋아요 취소)
   */
  @Transactional
  public boolean togglePostLike(Long postId, Long userId) {
    // 사용자 및 게시글 엔티티를 찾습니다.
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    // 해당 사용자가 이 게시글에 이미 좋아요를 눌렀는지 확인합니다.
    Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);

    if (existingLike.isPresent()) {
      // 이미 좋아요를 눌렀다면 좋아요 취소 (삭제)
      postLikeRepository.delete(existingLike.get());
      post.decrementLikeCount(); // Post 엔티티의 좋아요 수 감소
      log.info("게시글 ID {}에 대한 사용자 ID {}의 좋아요를 취소했습니다. 현재 좋아요 수: {}", postId, userId, post.getLikeCount());
      return false; // 좋아요 취소됨
    } else {
      // 좋아요를 누르지 않았다면 새로 추가
      PostLike newLike = PostLike.builder()
          .user(user)
          .post(post)
          .build();
      postLikeRepository.save(newLike);
      post.incrementLikeCount(); // Post 엔티티의 좋아요 수 증가
      log.info("게시글 ID {}에 대한 사용자 ID {}의 좋아요를 추가했습니다. 현재 좋아요 수: {}", postId, userId, post.getLikeCount());
      return true; // 좋아요 추가됨
    }
  }

  /**
   * 특정 게시글에 대한 현재 사용자의 좋아요 상태를 확인합니다.
   *
   * @param postId 확인할 게시글의 ID
   * @param userId 확인할 사용자의 ID
   * @return 사용자가 해당 게시글에 좋아요를 눌렀는지 여부
   */
  @Transactional(readOnly = true)
  public boolean isPostLikedByUser(Long postId, Long userId) {
    return postLikeRepository.findByUserIdAndPostId(userId, postId).isPresent();
  }
}
