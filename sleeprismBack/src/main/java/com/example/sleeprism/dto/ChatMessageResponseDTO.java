package com.example.sleeprism.dto;

import com.example.sleeprism.entity.ChatMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageResponseDTO {
  private Long id;
  private Long chatRoomId;
  private Long senderId;
  private String senderNickname;
  private String content;
  private LocalDateTime sentAt;
  private boolean isRead;

  public ChatMessageResponseDTO(ChatMessage message) {
    this.id = message.getId();
    this.chatRoomId = message.getChatRoom().getId();
    this.senderId = message.getSender().getId();
    this.senderNickname = message.getSender().getNickname();
    this.content = message.getContent();
    this.sentAt = message.getCreatedAt(); // BaseTimeEntity의 createdAt 사용
    this.isRead = message.isRead();
  }
}
