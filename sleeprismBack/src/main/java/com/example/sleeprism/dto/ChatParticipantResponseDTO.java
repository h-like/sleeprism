package com.example.sleeprism.dto;

import com.example.sleeprism.entity.ChatParticipant;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatParticipantResponseDTO {
  private Long id;
  private Long userId;
  private String userNickname;
  private Long chatRoomId;
  private LocalDateTime joinedAt;
  private boolean isLeft;

  public ChatParticipantResponseDTO(ChatParticipant participant) {
    this.id = participant.getId();
    this.userId = participant.getUser().getId();
    this.userNickname = participant.getUser().getNickname();
    this.chatRoomId = participant.getChatRoom().getId();
    this.joinedAt = participant.getJoinedAt();
    this.isLeft = participant.isLeft();
  }
}
