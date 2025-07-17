package com.example.sleeprism.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id") // <-- 추가
@Entity
@Table(name = "chat_rooms")
@Getter
@Setter // Setter 추가 (creator 필드 등을 설정할 때 필요)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ChatRoom extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_room_id")
  private Long id;

  @Column(length = 100)
  private String name; // 그룹 채팅방 이름 (1대1은 null)

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChatRoomType type; // 채팅방 유형 (SINGLE, GROUP)

  // ChatRoomService에서 creator를 User 타입으로 사용하고 있으므로 관계 매핑으로 변경
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_user_id") // DB 컬럼명과 일치
  private User creator; // 그룹 채팅방 방장 (1:1 채팅방은 null)

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false; // 채팅방 삭제 여부 (소프트 삭제)

  @JsonManagedReference("room-participants")
  @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ChatParticipant> participants = new ArrayList<>(); // 채팅방 참가자 목록

  @JsonManagedReference("room-messages")
  @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ChatMessage> messages = new ArrayList<>(); // 채팅방 메시지 목록

  // 생성자 (필요하다면 @Builder.Default와 함께 사용될 수 있도록 고려)
  public ChatRoom(ChatRoomType type, User creator) { // creator를 User 타입으로 받도록 변경
    this.type = type;
    this.creator = creator; // 방장 설정
    // 기타 필드는 @Builder.Default나 Lombok의 Setter로 설정
  }

  // 채팅방 이름 설정 (그룹 채팅방용)
  public void setName(String name) {
    if (this.type == ChatRoomType.SINGLE) {
      throw new IllegalStateException("1대1 채팅방은 이름을 가질 수 없습니다.");
    }
    this.name = name;
  }

  // 채팅방 삭제 (Soft Delete)
  public void delete() { // isDeleted를 true로 변경하는 기존 메서드
    this.isDeleted = true;
  }

  // 채팅방 활성화 (isDeleted를 false로 변경)
  public void activate() { // <-- 이 메서드를 추가합니다.
    this.isDeleted = false;
  }
}
    