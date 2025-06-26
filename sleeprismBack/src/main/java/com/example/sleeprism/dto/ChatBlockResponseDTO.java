package com.example.sleeprism.dto;

import com.example.sleeprism.entity.ChatBlock;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatBlockResponseDTO {
  private Long id;
  private Long blockerId;
  private String blockerNickname;
  private Long blockedId;
  private String blockedNickname;
  private LocalDateTime createdAt;

  public ChatBlockResponseDTO(ChatBlock chatBlock) {
    this.id = chatBlock.getId();
    this.blockerId = chatBlock.getBlocker().getId();
    this.blockerNickname = chatBlock.getBlocker().getNickname();
    this.blockedId = chatBlock.getBlocked().getId();
    this.blockedNickname = chatBlock.getBlocked().getNickname();
    this.createdAt = chatBlock.getCreatedAt();
  }
}
