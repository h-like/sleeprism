package com.example.sleeprism.service;

import com.example.sleeprism.dto.CommentCreateRequestDTO;
import com.example.sleeprism.dto.CommentResponseDTO;
import com.example.sleeprism.dto.CommentUpdateRequestDTO;
import com.example.sleeprism.entity.Comment;
import com.example.sleeprism.entity.NotificationType; // NotificationType import 추가
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.CommentRepository;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Slf4j import 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j // 로그 추가
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final FileStorageService fileStorageService; // 첨부 파일 관리를 위해 추가
  private final NotificationService notificationService; // NotificationService 주입

  /**
   * 댓글을 생성합니다. (최상위 댓글 또는 대댓글)
   * 댓글 생성 시, 게시글 작성자 또는 부모 댓글 작성자에게 알림을 생성합니다.
   * @param requestDto 댓글 생성 요청 DTO
   * @param userId 댓글 작성자 ID
   * @return 생성된 댓글 응답 DTO
   */
  @Transactional
  public CommentResponseDTO createComment(CommentCreateRequestDTO requestDto, Long userId) {
    User commentAuthor = userRepository.findById(userId) // 댓글 작성자
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    Post post = postRepository.findByIdAndIsDeletedFalse(requestDto.getPostId())
        .orElseThrow(() -> new EntityNotFoundException("Post not found or deleted with ID: " + requestDto.getPostId()));

    Comment parentComment = null;
    if (requestDto.getParentCommentId() != null) {
      parentComment = commentRepository.findByIdAndIsDeletedFalse(requestDto.getParentCommentId())
          .orElseThrow(() -> new EntityNotFoundException("Parent comment not found or deleted with ID: " + requestDto.getParentCommentId()));

      // 대댓글 1단계만 허용: 부모 댓글이 이미 부모를 가지고 있다면 (즉, 2단계 대댓글) 생성 불가
      if (parentComment.getParent() != null) {
        throw new IllegalArgumentException("댓글은 1단계 대댓글까지만 허용됩니다.");
      }
    }

    Comment comment = Comment.builder()
        .post(post)
        .user(commentAuthor) // 댓글 작성자 설정
        .content(requestDto.getContent())
        .parent(parentComment)
        .attachmentUrl(requestDto.getAttachmentUrl()) // 첨부 파일 URL
        .attachmentType(requestDto.getAttachmentType()) // 첨부 파일 타입
        .build();

    // 양방향 관계 설정 (Post, User, ParentComment)
    comment.setPost(post);
    comment.setUser(commentAuthor);
    if (parentComment != null) {
      comment.setParent(parentComment);
    }

    Comment savedComment = commentRepository.save(comment);

    // --- 알림 생성 로직 추가 ---
    // 1. 게시글 작성자에게 새 댓글 알림
    User postAuthor = post.getOriginalAuthor();
    if (!postAuthor.getId().equals(commentAuthor.getId())) { // 댓글 작성자와 게시글 작성자가 다를 경우에만 알림
      String message = String.format("'%s'님이 회원님의 게시글 '%s'에 새 댓글을 남겼습니다.",
          commentAuthor.getNickname(), post.getTitle());
      String redirectPath = String.format("/posts/%d#comment-%d", post.getId(), savedComment.getId());
      notificationService.createNotification(postAuthor, NotificationType.COMMENT, message,
          "Post", post.getId(), redirectPath);
      log.info("COMMENT notification sent to post author (User ID: {}) for Post ID: {}", postAuthor.getId(), post.getId());
    }

    // 2. 부모 댓글 작성자에게 대댓글 알림 (대댓글인 경우에만)
    if (parentComment != null && !parentComment.getUser().getId().equals(commentAuthor.getId())) { // 대댓글이고 작성자가 부모 댓글 작성자와 다를 경우에만 알림
      String message = String.format("'%s'님이 회원님의 댓글에 대댓글을 남겼습니다: '%s'",
          commentAuthor.getNickname(), savedComment.getContent().substring(0, Math.min(savedComment.getContent().length(), 20)) + "..."); // 메시지 길이 제한
      String redirectPath = String.format("/posts/%d#comment-%d", post.getId(), savedComment.getId());
      notificationService.createNotification(parentComment.getUser(), NotificationType.REPLY_COMMENT, message,
          "Comment", parentComment.getId(), redirectPath);
      log.info("REPLY_COMMENT notification sent to parent comment author (User ID: {}) for Comment ID: {}", parentComment.getUser().getId(), parentComment.getId());
    }
    // --- 알림 생성 로직 끝 ---

    return new CommentResponseDTO(savedComment);
  }

  /**
   * 특정 게시글의 모든 댓글(최상위 및 대댓글 포함)을 조회합니다.
   * @param postId 게시글 ID
   * @return 댓글 목록 DTO
   */
  public List<CommentResponseDTO> getCommentsByPostId(Long postId) {
    List<Comment> topLevelComments = commentRepository.findByPost_IdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(postId);
    return topLevelComments.stream()
        .map(comment -> new CommentResponseDTO(comment, false)) // 삭제된 댓글 내용은 숨김
        .collect(Collectors.toList());
  }

  /**
   * 특정 댓글을 수정합니다.
   * @param commentId 수정할 댓글 ID
   * @param requestDto 댓글 수정 요청 DTO
   * @param userId 댓글 작성자 ID (권한 검증용)
   * @return 수정된 댓글 응답 DTO
   */
  @Transactional
  public CommentResponseDTO updateComment(Long commentId, CommentUpdateRequestDTO requestDto, Long userId) {
    Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
        .orElseThrow(() -> new EntityNotFoundException("Comment not found or deleted with ID: " + commentId));

    // 작성자만 수정 가능
    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to update this comment.");
    }

    String updatedAttachmentUrl = requestDto.getAttachmentUrl();
    String updatedAttachmentType = requestDto.getAttachmentType();

    // 기존 첨부 파일 제거 요청이 있을 경우
    if (requestDto.isRemoveAttachment() && comment.getAttachmentUrl() != null) {
      fileStorageService.deleteFile(comment.getAttachmentUrl()); // 실제 파일 삭제
      updatedAttachmentUrl = null;
      updatedAttachmentType = null;
    }
    // 새 첨부 파일이 있고 기존 파일 제거 요청이 없다면 (혹은 기존 파일이 없다면)
    else if (requestDto.getAttachmentUrl() != null && !requestDto.getAttachmentUrl().isEmpty()) {
      // 기존 파일이 있다면 삭제 (새 파일로 교체)
      if (comment.getAttachmentUrl() != null && !comment.getAttachmentUrl().equals(requestDto.getAttachmentUrl())) {
        fileStorageService.deleteFile(comment.getAttachmentUrl());
      }
      // attachmentUrl은 이미 FileStorageService에서 생성된 URL이라고 가정
    }
    // 새 첨부 파일이 없고 기존 파일 제거 요청도 없다면 기존 URL 유지
    else {
      updatedAttachmentUrl = comment.getAttachmentUrl();
      updatedAttachmentType = comment.getAttachmentType();
    }

    comment.update(requestDto.getContent(), updatedAttachmentUrl, updatedAttachmentType);
    return new CommentResponseDTO(comment);
  }

  /**
   * 특정 댓글을 소프트 삭제합니다. 대댓글은 부모 댓글이 삭제되어도 유지됩니다.
   * @param commentId 삭제할 댓글 ID
   * @param userId 댓글 작성자 ID (권한 검증용)
   */
  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
        .orElseThrow(() -> new EntityNotFoundException("Comment not found or deleted with ID: " + commentId));

    // 작성자만 삭제 가능
    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to delete this comment.");
    }

    // 첨부 파일이 있다면 실제 파일도 삭제
    if (comment.getAttachmentUrl() != null) {
      fileStorageService.deleteFile(comment.getAttachmentUrl());
    }

    comment.delete(); // isDeleted를 true로 변경
  }

  /**
   * (옵션) 특정 유저가 작성한 모든 댓글을 조회합니다.
   * @param userId 사용자 ID
   * @return 댓글 목록 DTO
   */
  public List<CommentResponseDTO> getCommentsByUserId(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    return commentRepository.findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(userId).stream()
        .map(comment -> new CommentResponseDTO(comment, true)) // 내 댓글은 삭제된 내용도 볼 수 있게
        .collect(Collectors.toList());
  }
}
