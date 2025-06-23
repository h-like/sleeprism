package com.example.sleeprism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 댓글 수정을 위한 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentUpdateRequestDTO {
  @NotBlank(message = "댓글 내용은 필수입니다.")
  @Size(max = 1000, message = "댓글 내용은 1000자를 초과할 수 없습니다.")
  private String content;

  // 첨부 파일 정보 (단일 파일) - 수정 시 변경되거나 제거될 수 있음
  private String attachmentUrl;
  private String attachmentType;
  private boolean removeAttachment; // 기존 첨부 파일을 제거할지 여부
}
