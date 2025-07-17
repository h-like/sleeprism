package com.example.sleeprism.dto;

import com.example.sleeprism.entity.ChatMessage;
import com.example.sleeprism.entity.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageResponseDTO {
  private Long id;
  private Long chatRoomId; // ChatRoom 객체 대신 ID만 있어야 합니다.
  private Long senderId;
  private String senderNickname;
  private String content;
  private LocalDateTime sentAt;
  private boolean isRead;
  private MessageType messageType;

  // 생성자에서 ID만 가져와서 할당하는지 다시 확인해주세요.
  public ChatMessageResponseDTO(ChatMessage message) {
    this.id = message.getId();
    this.chatRoomId = message.getChatRoom().getId();
    this.senderId = message.getSender().getId();
    this.senderNickname = message.getSender().getNickname();
    this.content = message.getContent();
    this.sentAt = message.getCreatedAt();
    this.isRead = message.isRead();
    this.messageType = message.getMessageType();
  }
}