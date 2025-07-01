package com.example.sleeprism.dto;

import com.example.sleeprism.entity.ChatRoomType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatRoomCreateRequestDTO {
  private ChatRoomType type; // SINGLE 또는 GROUP
  private String name; // GROUP 채팅방일 경우 이름 (1:1은 선택 사항)
  private List<Long> participantUserIds; // 1:1 채팅은 1명, 그룹 채팅은 여러 명 (생성자 제외)
}
