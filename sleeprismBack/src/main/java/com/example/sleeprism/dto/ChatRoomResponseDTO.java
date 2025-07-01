package com.example.sleeprism.dto;

import com.example.sleeprism.entity.ChatRoom;
import com.example.sleeprism.entity.ChatRoomType;
import com.example.sleeprism.entity.User; // User 엔티티 임포트 추가
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ChatRoomResponseDTO {
  private Long id;
  private String name;
  private ChatRoomType type;

  // creator User 객체 필드 추가 (필요한 경우)
  private Long creatorId; // 기존 creatorId 유지 (DTO는 ID만 가질 수 있음)
  private String creatorNickname; // 기존 creatorNickname 유지
  // private User creator; // 만약 DTO가 User 엔티티 전체를 포함해야 한다면 주석 해제

  private boolean isDeleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private ChatMessageResponseDTO lastMessage; // 마지막 메시지 정보

  private List<ChatParticipantResponseDTO> participants; // 참가자 목록

  // ChatRoom 엔티티로부터 DTO를 생성하는 생성자
  public ChatRoomResponseDTO(ChatRoom chatRoom) {
    this.id = chatRoom.getId();
    this.name = chatRoom.getName();
    this.type = chatRoom.getType();

    // creator 필드에서 creatorId와 creatorNickname을 설정
    if (chatRoom.getCreator() != null) { // <-- getCreator() 사용
      this.creatorId = chatRoom.getCreator().getId(); // <-- getCreator().getId() 사용
      this.creatorNickname = chatRoom.getCreator().getNickname(); // <-- getCreator().getNickname() 사용
    }

    this.isDeleted = chatRoom.isDeleted();
    this.createdAt = chatRoom.getCreatedAt();
    this.updatedAt = chatRoom.getUpdatedAt();

    // 참가자 목록 DTO로 변환
    if (chatRoom.getParticipants() != null) {
      this.participants = chatRoom.getParticipants().stream()
          .map(ChatParticipantResponseDTO::new)
          .collect(Collectors.toList());
    }
  }
}
