package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 타로 카드 한 장의 정보를 담는 엔티티입니다.
 * 앱 내에서 정적인 마스터 데이터로 사용됩니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tarot_card")
public class TarotCard extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "card_id")
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String name; // 카드 이름 (예: "The Fool", "The Magician")

  @Column(nullable = false, length = 255)
  private String imageUrl; // 카드 이미지 URL

  @Column(nullable = false, columnDefinition = "TEXT")
  private String meaningUpright; // 정방향 의미

  @Column(nullable = false, columnDefinition = "TEXT")
  private String meaningReversed; // 역방향 의미

  @Column(length = 255)
  private String keywords; // 관련 키워드 (콤마로 구분, 예: "시작, 순수, 모험")

  @Builder
  public TarotCard(String name, String imageUrl, String meaningUpright, String meaningReversed, String keywords) {
    this.name = name;
    this.imageUrl = imageUrl;
    this.meaningUpright = meaningUpright;
    this.meaningReversed = meaningReversed;
    this.keywords = keywords;
  }

  // 카드 정보 업데이트 메서드 (관리자용)
  public void update(String name, String imageUrl, String meaningUpright, String meaningReversed, String keywords) {
    this.name = name;
    this.imageUrl = imageUrl;
    this.meaningUpright = meaningUpright;
    this.meaningReversed = meaningReversed;
    this.keywords = keywords;
  }
}
