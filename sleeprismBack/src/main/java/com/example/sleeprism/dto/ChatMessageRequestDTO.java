package com.example.sleeprism.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequestDTO {
  private Long chatRoomId;
  private String content;
  // private Long senderId; // Spring Security에서 가져올 예정이므로 DTO에서는 불필요
}
