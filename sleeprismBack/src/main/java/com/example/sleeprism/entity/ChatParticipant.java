package com.example.sleeprism.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id") // <-- 추가
@Entity
@Table(name = "chat_participants")
@Getter
@Setter // Setter 추가 (특히 joinedAt, leftAt 등 변경 가능 필드에 유용)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ChatParticipant extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_participant_id")
  private Long id;

  @JsonBackReference("room-participants")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "joined_at", nullable = false)
  @Builder.Default
  private LocalDateTime joinedAt = LocalDateTime.now(); // 기본값 설정

  @Column(name = "left_at")
  private LocalDateTime leftAt; // 그룹 채팅방에서 나간 시간

  @Column(name = "is_left", nullable = false)
  @Builder.Default
  private boolean isLeft = false; // 그룹 채팅방에서 나갔는지 여부 (소프트 삭제 개념)

  @Column(name = "last_read_message_id")
  private Long lastReadMessageId; // 사용자가 마지막으로 읽은 메시지 ID

  // --- 편의 메서드 ---
  // 참가자가 채팅방을 나갔음을 표시
  public void leave() {
    this.isLeft = true;
    this.leftAt = LocalDateTime.now();
  }

  // 참가자가 채팅방에 다시 참여했음을 표시
  public void rejoin() {
    this.isLeft = false;
    this.joinedAt = LocalDateTime.now(); // 재참여 시간 업데이트
    this.leftAt = null; // 나간 시간 초기화
  }

  // 마지막 읽은 메시지 ID 업데이트
  public void updateLastReadMessageId(Long messageId) {
    this.lastReadMessageId = messageId;
  }
}
    