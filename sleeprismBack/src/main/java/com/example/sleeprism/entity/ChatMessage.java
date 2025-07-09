package com.example.sleeprism.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder; // @Builder 임포트
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder; // @SuperBuilder 임포트

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id") // <-- 추가
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder // <-- @Builder 대신 @SuperBuilder 사용
public class ChatMessage extends BaseTimeEntity { // BaseTimeEntity 상속

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_message_id")
  private Long id;

  @JsonBackReference("room-messages")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_user_id", nullable = false)
  private User sender;

  @Column(nullable = false, length = 1000)
  private String content;

  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  @Column(name = "is_read", nullable = false)
  @Builder.Default // <-- @Builder.Default 추가
  private boolean isRead = false; // 메시지 읽음 여부

  // isRead 상태를 변경하는 편의 메서드 (getter/setter 사용보다 명확)
  public void markAsRead() {
    this.isRead = true;
  }



  // ChatMessageResponseDTO에서 필요하다면 senderNickname 등을 위한 추가 메서드 또는 DTO 변환 로직 필요
}
