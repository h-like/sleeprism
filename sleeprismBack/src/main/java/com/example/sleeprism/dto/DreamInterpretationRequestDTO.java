package com.example.sleeprism.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 꿈 해몽 요청을 위한 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class DreamInterpretationRequestDTO {
  @NotNull(message = "해몽할 꿈 게시글 ID는 필수입니다.")
  private Long postId; // 해몽을 요청할 꿈 게시글의 ID
}
