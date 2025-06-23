package com.example.sleeprism.dto;

import com.example.sleeprism.entity.DreamInterpretation;
import com.example.sleeprism.entity.TarotCard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 꿈 해몽 결과를 클라이언트에게 응답하기 위한 DTO.
 * AI가 생성한 여러 해몽 옵션과 사용자가 최종 선택한 정보를 포함.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder // <-- 다시 @Builder 어노테이션 추가
@Slf4j
public class DreamInterpretationResponseDTO {
  private Long id; // DreamInterpretation 엔티티의 ID
  private Long postId; // 해몽 대상 꿈 게시글 ID
  private Long userId; // 해몽 요청자 ID
  private String userName; // 해몽 요청자 닉네임

  // AI가 제안하는 여러 해몽 옵션들
  private List<InterpretationOptionDTO> interpretationOptions;

  // 사용자가 최종 선택한 해몽 옵션 정보
  private Integer selectedOptionIndex;
  private Long selectedTarotCardId;
  private String selectedTarotCardName;
  private String selectedTarotCardImageUrl;
  private LocalDateTime interpretedAt;

  private String errorMessage;

  /**
   * DreamInterpretation 엔티티를 기반으로 DTO를 생성하는 생성자.
   * AI 응답 내용을 파싱하여 interpretationOptions를 채웁니다.
   * 이 생성자는 DreamInterpretationService에서만 호출될 것입니다.
   * (외부에서 직접 DTO를 생성할 때는 빌더 패턴을 사용합니다.)
   *
   * @param dreamInterpretation 변환할 DreamInterpretation 엔티티 객체
   * @param objectMapper ObjectMapper 인스턴스 (Service에서 주입받아 사용)
   * @param tarotCards Map<Long, TarotCard> 타로 카드 ID와 객체의 매핑 (각 옵션에 매칭된 타로 카드 정보 채우기 위함)
   */
  public DreamInterpretationResponseDTO(DreamInterpretation dreamInterpretation, ObjectMapper objectMapper, Map<Long, TarotCard> tarotCards) {
    this.id = dreamInterpretation.getId();
    this.postId = dreamInterpretation.getPost().getId();
    this.userId = dreamInterpretation.getUser().getId();
    this.userName = dreamInterpretation.getUser().getNickname();

    this.interpretedAt = dreamInterpretation.getInterpretedAt();
    this.selectedOptionIndex = dreamInterpretation.getSelectedInterpretationIndex();
    if (dreamInterpretation.getSelectedTarotCard() != null) {
      this.selectedTarotCardId = dreamInterpretation.getSelectedTarotCard().getId();
      this.selectedTarotCardName = dreamInterpretation.getSelectedTarotCard().getName();
      this.selectedTarotCardImageUrl = dreamInterpretation.getSelectedTarotCard().getImageUrl();
    }

    this.interpretationOptions = new ArrayList<>();
    try {
      Map<String, List<Map<String, Object>>> aiResponseMap = objectMapper.readValue(
          dreamInterpretation.getAiResponseContent(),
          new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<Map<String, Object>>>>() {}
      );

      List<Map<String, Object>> interpretations = aiResponseMap.get("interpretations");
      if (interpretations != null) {
        for (int i = 0; i < interpretations.size(); i++) {
          Map<String, Object> interpretationData = interpretations.get(i);
          // AI 응답 JSON 내에 tarotCardId가 있다면 사용하고, 없다면 null 처리
          Long optionTarotCardId = interpretationData.get("tarotCardId") != null ? ((Number) interpretationData.get("tarotCardId")).longValue() : null;
          TarotCard matchedTarotCard = null;
          if (optionTarotCardId != null && tarotCards.containsKey(optionTarotCardId)) {
            matchedTarotCard = tarotCards.get(optionTarotCardId);
          }

          this.interpretationOptions.add(InterpretationOptionDTO.builder()
              .optionIndex(i)
              .title((String) interpretationData.get("title"))
              .content((String) interpretationData.get("content"))
              .tarotCardId(matchedTarotCard != null ? matchedTarotCard.getId() : null)
              .tarotCardName(matchedTarotCard != null ? matchedTarotCard.getName() : null)
              .tarotCardImageUrl(matchedTarotCard != null ? matchedTarotCard.getImageUrl() : null)
              .build());
        }
      }

    } catch (JsonProcessingException e) {
      log.error("Failed to parse AI response content for DreamInterpretation ID {}: {}", dreamInterpretation.getId(), e.getMessage());
      this.errorMessage = "AI 응답 내용을 파싱하는 데 실패했습니다.";
    } catch (Exception e) {
      log.error("An unexpected error occurred during DTO creation for DreamInterpretation ID {}: {}", dreamInterpretation.getId(), e.getMessage(), e);
      this.errorMessage = "해몽 데이터를 처리하는 중 오류가 발생했습니다.";
    }
  }
}
