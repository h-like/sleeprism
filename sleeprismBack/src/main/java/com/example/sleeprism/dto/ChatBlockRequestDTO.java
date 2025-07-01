package com.example.sleeprism.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatBlockRequestDTO {
  private Long blockedUserId; // 차단할 사용자 ID
}
