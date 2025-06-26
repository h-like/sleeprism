// src/main/java/com/example/sleeprism/entity/Notification.java
package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자에게 전송될 알림 정보를 담는 엔티티 클래스입니다.
 * 댓글, 판매 요청 등 다양한 이벤트에 대한 알림을 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications") // 데이터베이스 테이블 이름을 'notifications'로 지정합니다.
public class Notification extends BaseTimeEntity { // 생성 및 수정 시간을 자동으로 관리하기 위해 BaseTimeEntity 상속

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false) // 알림을 받을 사용자 (대상 사용자)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private NotificationType type; // 알림의 종류 (예: COMMENT, SALE_REQUEST, POST_LIKE 등)

  @Column(nullable = false, length = 500) // 알림 메시지 내용
  private String message;

  @Column(name = "target_entity_type", nullable = false, length = 50)
  private String targetEntityType; // 알림과 관련된 엔티티의 타입 (예: "Post", "SaleRequest", "Comment")

  @Column(name = "target_entity_id", nullable = false)
  private Long targetEntityId; // 알림과 관련된 엔티티의 ID (예: 댓글이 달린 게시물 ID, 판매 요청 ID)

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false; // 사용자가 알림을 읽었는지 여부 (기본값: false)

  // 알림 클릭 시 이동할 경로 (URL 또는 프론트엔드 라우트 경로)
  @Column(name = "redirect_path", length = 255)
  private String redirectPath;

  @Builder
  public Notification(User user, NotificationType type, String message,
                      String targetEntityType, Long targetEntityId, String redirectPath) {
    this.user = user;
    this.type = type;
    this.message = message;
    this.targetEntityType = targetEntityType;
    this.targetEntityId = targetEntityId;
    this.redirectPath = redirectPath;
    this.isRead = false; // 새로 생성된 알림은 기본적으로 읽지 않은 상태
  }

  /**
   * 알림을 읽음 상태로 변경하는 메서드
   */
  public void markAsRead() {
    this.isRead = true;
  }

  /**
   * 알림 메시지를 업데이트하는 메서드 (필요시 사용)
   */
  public void updateMessage(String newMessage) {
    this.message = newMessage;
  }
}