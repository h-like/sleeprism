package com.example.sleeprism.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder // 생성 로직이 Service로 옮겨갔으므로, Service에서 DTO를 편하게 만들기 위해 Builder를 사용합니다.
@NoArgsConstructor
@AllArgsConstructor
public class DreamInterpretationResponseDTO {
  private Long id;
  private Long postId;
  private Long userId;
  private String userName;

  // AI가 제안하는 여러 해몽 옵션들
  private List<InterpretationOptionDTO> interpretationOptions;

  // 사용자가 최종 선택한 해몽 옵션 정보
  private Integer selectedOptionIndex;
  private Long selectedTarotCardId;
  private String selectedTarotCardName;
  private String selectedTarotCardImageUrl;
  private LocalDateTime interpretedAt;

  private String errorMessage; // 에러 발생 시 메시지를 담기 위한 필드
}
