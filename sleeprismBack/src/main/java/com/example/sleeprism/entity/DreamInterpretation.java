package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자의 꿈에 대한 AI 해몽 결과를 담는 엔티티입니다.
 * 한 꿈에 대해 여러 사용자가 각자 해몽을 요청하고 저장할 수 있습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "dream_interpretation")
public class DreamInterpretation extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "interpretation_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post; // 해몽 대상이 되는 꿈 게시글

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user; // 해몽을 요청한 사용자

  // AI가 생성한 해몽 내용 (JSON 형태로 저장하거나, 여러 옵션을 별도 테이블로 관리)
  // 여기서는 AI가 반환한 원본 JSON 문자열을 저장하는 것을 고려합니다.
  // 또는 AI가 반환한 여러 해몽 옵션을 이 필드에 직렬화하여 저장할 수도 있습니다.
  // JSON 직렬화/역직렬화는 서비스 계층에서 처리
  @Lob
  @Column(nullable = false, columnDefinition = "JSON") // MySQL 5.7+ 또는 PostgreSQL의 JSON 타입 활용
  private String aiResponseContent; // AI가 반환한 원본 해몽 내용 (JSON 문자열)

  // 사용자가 최종적으로 선택한 해몽 옵션 관련 필드
  // InterpretationOption 엔티티를 별도로 두어 One-to-Many 관계를 맺는다면 이 필드는 제거됩니다.
  // 현재는 DreamInterpretation 안에 해몽 옵션이 포함되어 있거나,
  // AI 응답 JSON에서 어떤 옵션이 선택되었는지를 기록하는 형태로 설계합니다.
  // 예시에서는 AI 응답 JSON을 통째로 저장하고, 그 안에서 선택된 내용을 기록
  @Column(name = "selected_interpretation_index")
  private Integer selectedInterpretationIndex; // AI 응답 JSON 내에서 선택된 해몽의 인덱스 (0, 1, 2 등)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "selected_tarot_card_id") // 사용자가 선택한 타로 카드
  private TarotCard selectedTarotCard;

  @Column(name = "interpreted_at", nullable = false)
  private LocalDateTime interpretedAt; // 해몽이 요청되어 생성된 시간

  @Builder
  public DreamInterpretation(Post post, User user, String aiResponseContent, Integer selectedInterpretationIndex, TarotCard selectedTarotCard) {
    this.post = post;
    this.user = user;
    this.aiResponseContent = aiResponseContent;
    this.selectedInterpretationIndex = selectedInterpretationIndex;
    this.selectedTarotCard = selectedTarotCard;
    this.interpretedAt = LocalDateTime.now();
  }

  // 사용자가 최종 해몽을 선택했을 때 호출되는 메서드
  public void selectInterpretation(Integer selectedInterpretationIndex, TarotCard selectedTarotCard) {
    this.selectedInterpretationIndex = selectedInterpretationIndex;
    this.selectedTarotCard = selectedTarotCard;
    // interpretedAt은 최초 해몽 생성 시에만 설정되므로 여기서는 변경하지 않습니다.
  }
}
