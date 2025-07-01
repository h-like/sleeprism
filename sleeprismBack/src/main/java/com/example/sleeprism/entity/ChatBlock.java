package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 사용자 간 채팅 차단 정보를 담는 엔티티 클래스입니다.
 * blockeeUser는 blockerUser의 메시지를 받지 않으며, blockerUser에게 메시지를 보낼 수 없습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC) // <-- protected -> PUBLIC으로 변경
@Entity
@Table(name = "chat_blocks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_user_id", "blocked_user_id"})) // 중복 차단 방지
@SuperBuilder // <-- @Builder 대신 @SuperBuilder 사용 (상속된 필드를 빌더에 포함)
public class ChatBlock extends BaseTimeEntity { // BaseTimeEntity 상속

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_block_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "blocker_user_id", nullable = false)
  private User blocker; // 차단한 사용자

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "blocked_user_id", nullable = false)
  private User blocked; // 차단당한 사용자

  // @Builder // 이 특정 생성자에 붙은 @Builder 어노테이션은 제거합니다.
  // @SuperBuilder가 클래스 레벨에 있으므로 이 생성자를 기반으로 빌더를 생성할 필요가 없습니다.
  // 이 생성자는 필요하다면 유지할 수 있지만, 빌더를 생성하지는 않습니다.
  public ChatBlock(User blocker, User blocked) {
    this.blocker = blocker;
    this.blocked = blocked;
  }
}
