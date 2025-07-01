package com.example.sleeprism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;

/**
 * 댓글 생성을 위한 요청 DTO (최상위 댓글 또는 대댓글)
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentCreateRequestDTO {
  @NotBlank(message = "댓글 내용은 필수입니다.")
  @Size(max = 1000, message = "댓글 내용은 1000자를 초과할 수 없습니다.")
  private String content;

  private Long postId; // 댓글이 달릴 게시글 ID
  private Long parentCommentId; // 대댓글인 경우 부모 댓글 ID (선택적)

  // 첨부 파일 정보 (단일 파일)
  private String attachmentUrl; // 업로드된 파일의 URL
  private String attachmentType; // 파일의 MIME 타입

  // **[새로 추가]** 프론트엔드에서 직접 전송하는 MultipartFile
  private File attachmentFile;
}
