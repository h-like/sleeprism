package com.example.sleeprism.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageHistoryRequestDTO {
  private Long chatRoomId;
}