package com.example.sleeprism.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자가 AI 해몽 옵션 중 하나를 선택할 때 사용하는 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class DreamInterpretationSelectRequestDTO {
  @NotNull(message = "선택할 해몽 옵션의 인덱스는 필수입니다.")
  @Min(value = 0, message = "해몽 옵션 인덱스는 0 이상이어야 합니다.")
  private Integer selectedOptionIndex; // 선택된 해몽 옵션의 인덱스 (AI 응답 JSON 내)

  @NotNull(message = "선택된 타로 카드 ID는 필수입니다.")
  private Long selectedTarotCardId; // 선택된 타로 카드의 ID
}
