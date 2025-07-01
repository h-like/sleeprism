package com.example.sleeprism.service;

import com.example.sleeprism.entity.Bookmark;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.BookmarkRepository;
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
public class BookmarkService {

  private final BookmarkRepository bookmarkRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * 게시글에 북마크를 추가하거나 취소합니다 (토글 기능).
   *
   * @param postId 북마크할 게시글의 ID
   * @param userId 북마크를 누른 사용자의 ID
   * @return 북마크 추가/취소 성공 여부 (true: 북마크 추가, false: 북마크 취소)
   */
  @Transactional
  public boolean togglePostBookmark(Long postId, Long userId) {
    // 사용자 및 게시글 엔티티를 찾습니다.
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    // 해당 사용자가 이 게시글에 이미 북마크를 눌렀는지 확인합니다.
    Optional<Bookmark> existingBookmark = bookmarkRepository.findByUser_IdAndPost_Id(userId, postId);

    if (existingBookmark.isPresent()) {
      // 이미 북마크를 눌렀다면 북마크 취소 (삭제)
      bookmarkRepository.delete(existingBookmark.get());
      post.decrementBookmarkCount(); // Post 엔티티의 북마크 수 감소
      log.info("게시글 ID {}에 대한 사용자 ID {}의 북마크를 취소했습니다. 현재 북마크 수: {}", postId, userId, post.getBookmarkCount());
      return false; // 북마크 취소됨
    } else {
      // 북마크를 누르지 않았다면 새로 추가
      Bookmark newBookmark = Bookmark.builder()
          .user(user)
          .post(post)
          .build();
      bookmarkRepository.save(newBookmark);
      post.incrementBookmarkCount(); // Post 엔티티의 북마크 수 증가
      log.info("게시글 ID {}에 대한 사용자 ID {}의 북마크를 추가했습니다. 현재 북마크 수: {}", postId, userId, post.getBookmarkCount());
      return true; // 북마크 추가됨
    }
  }

  /**
   * 특정 게시글에 대한 현재 사용자의 북마크 상태를 확인합니다.
   *
   * @param postId 확인할 게시글의 ID
   * @param userId 확인할 사용자의 ID
   * @return 사용자가 해당 게시글에 북마크를 눌렀는지 여부
   */
  @Transactional(readOnly = true)
  public boolean isPostBookmarkedByUser(Long postId, Long userId) {
    return bookmarkRepository.existsByUser_IdAndPost_Id(userId, postId);
  }
}
