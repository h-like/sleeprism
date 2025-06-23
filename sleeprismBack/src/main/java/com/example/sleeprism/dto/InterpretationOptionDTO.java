package com.example.sleeprism.dto;

import lombok.AllArgsConstructor; // AllArgsConstructor 임포트 추가
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI가 제공하는 단일 해몽 옵션 및 연결된 타로 카드 정보를 담는 DTO.
 */
@Getter
@Setter
@NoArgsConstructor // Lombok을 통해 기본 생성자 생성
@AllArgsConstructor // Lombok을 통해 모든 필드를 인자로 받는 생성자 생성 (Builder와 함께 사용 권장)
@Builder // Lombok을 통해 빌더 패턴 생성
public class InterpretationOptionDTO {
  private Integer optionIndex; // AI 응답 내에서 이 옵션의 순서 (0부터 시작)
  private String title; // 해몽 옵션의 요약 제목 (AI 생성)
  private String content; // 해당 옵션의 해몽 내용 (AI 생성)

  // 매칭된 타로 카드 정보
  private Long tarotCardId;
  private String tarotCardName;
  private String tarotCardImageUrl;

  // 이전에 있던 사용자 정의 생성자는 제거합니다.
  // DTO 생성이 필요한 곳에서는 이 클래스의 Builder()를 사용하거나
  // @AllArgsConstructor로 생성된 생성자를 직접 사용합니다.
}
