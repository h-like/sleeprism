package com.example.sleeprism.dto;

import com.example.sleeprism.entity.Comment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;



// --- CommentResponseDTO ---
/**
 * 댓글 응답을 위한 DTO (계층 구조 포함)
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentResponseDTO {
  private Long id;
  private String content;
  private Long postId;
  private Long authorId;
  private String authorNickname; // 작성자 닉네임
  private String authorProfileImageUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long parentCommentId; // 부모 댓글 ID
  private boolean isDeleted; // 삭제 여부

  // 첨부 파일 정보
  private String attachmentUrl;
  private String attachmentType;


  private List<CommentResponseDTO> children; // 대댓글 목록

  public CommentResponseDTO(Comment comment) {
    this.id = comment.getId();
    // 삭제된 댓글의 경우 내용 등을 비워줄 수 있습니다.
    this.content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
    this.postId = comment.getPost().getId();
    this.authorId = comment.getUser().getId();
    this.authorNickname = comment.getUser().getNickname(); // User 엔티티에 getNickname() 있다고 가정
    this.authorProfileImageUrl = comment.getUser().getProfileImageUrl();
    this.createdAt = comment.getCreatedAt();
    this.updatedAt = comment.getUpdatedAt();
    this.parentCommentId = (comment.getParent() != null) ? comment.getParent().getId() : null;
    this.isDeleted = comment.isDeleted();

    this.attachmentUrl = comment.getAttachmentUrl();
    this.attachmentType = comment.getAttachmentType();

    // 대댓글 재귀적으로 DTO로 변환
    this.children = comment.getChildren().stream()
        .map(CommentResponseDTO::new)
        .collect(Collectors.toList());
  }

  // 삭제된 댓글은 내용만 "삭제된 댓글입니다"로 표시하고 자식 댓글은 그대로 보여주는 경우
  public CommentResponseDTO(Comment comment, boolean showDeletedContent) {
    this.id = comment.getId();
    this.content = showDeletedContent || !comment.isDeleted() ? comment.getContent() : "삭제된 댓글입니다.";
    this.postId = comment.getPost().getId();
    this.authorId = comment.getUser().getId();
    this.authorNickname = comment.getUser().getNickname();
    this.authorProfileImageUrl = comment.getUser().getProfileImageUrl();
    this.createdAt = comment.getCreatedAt();
    this.updatedAt = comment.getUpdatedAt();
    this.parentCommentId = (comment.getParent() != null) ? comment.getParent().getId() : null;
    this.isDeleted = comment.isDeleted();

    this.attachmentUrl = comment.getAttachmentUrl();
    this.attachmentType = comment.getAttachmentType();

    this.children = comment.getChildren().stream()
        .map(child -> new CommentResponseDTO(child, showDeletedContent)) // 재귀 호출
        .collect(Collectors.toList());
  }
}
