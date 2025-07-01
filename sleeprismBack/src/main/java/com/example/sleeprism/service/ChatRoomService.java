package com.example.sleeprism.service;

import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.dto.ChatRoomCreateRequestDTO;
import com.example.sleeprism.dto.ChatRoomResponseDTO;
import com.example.sleeprism.dto.ChatParticipantResponseDTO;
import com.example.sleeprism.entity.ChatParticipant;
import com.example.sleeprism.entity.ChatRoom;
import com.example.sleeprism.entity.ChatRoomType;
import com.example.sleeprism.entity.ChatMessage;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.ChatMessageRepository;
import com.example.sleeprism.repository.ChatParticipantRepository;
import com.example.sleeprism.repository.ChatRoomRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;
  private final ChatMessageRepository chatMessageRepository; // 마지막 메시지 조회를 위해 추가
  private final UserRepository userRepository;

  /**
   * 1대1 채팅방을 생성하거나 기존 채팅방을 반환합니다.
   *
   * @param creatorId 채팅방을 생성하려는 사용자 ID
   * @param otherUserId 상대방 사용자 ID
   * @return 생성되거나 조회된 1대1 채팅방 응답 DTO
   */
  @Transactional
  public ChatRoomResponseDTO createOrGetSingleChatRoom(Long creatorId, Long otherUserId) {
    if (creatorId.equals(otherUserId)) {
      throw new IllegalArgumentException("자신과 1대1 채팅을 할 수 없습니다.");
    }

    User creator = userRepository.findById(creatorId)
        .orElseThrow(() -> new EntityNotFoundException("Creator user not found with ID: " + creatorId));
    User otherUser = userRepository.findById(otherUserId)
        .orElseThrow(() -> new EntityNotFoundException("Other user not found with ID: " + otherUserId));

    // 기존 1:1 채팅방이 있는지 확인 (두 사용자 모두 참여 중이고, 삭제되지 않은 채팅방)
    Optional<ChatRoom> existingRoom = chatRoomRepository.findSingleChatRoomBetweenUsers(creatorId, otherUserId);

    if (existingRoom.isPresent()) {
      ChatRoom room = existingRoom.get();
      // 만약 채팅방이 소프트 삭제된 상태였다면 다시 활성화
      if (room.isDeleted()) {
        room.activate(); // <-- activate() 메서드 사용
        log.info("Existing single chat room found and activated between user {} and {}", creatorId, otherUserId);
      } else {
        log.info("Existing single chat room found and reused between user {} and {}", creatorId, otherUserId);
      }

      // 참가자가 나갔다가 다시 들어오는 경우 처리 (rejoin 메서드 사용)
      chatParticipantRepository.findByChatRoomAndUser(room, creator).ifPresent(p -> {
        if (p.isLeft()) { p.rejoin(); } // <-- rejoin() 메서드 사용
      });
      chatParticipantRepository.findByChatRoomAndUser(room, otherUser).ifPresent(p -> {
        if (p.isLeft()) { p.rejoin(); } // <-- rejoin() 메서드 사용
      });
      // participantRepository.saveAll()를 통해 변경사항 저장 필요
      chatParticipantRepository.saveAll(room.getParticipants().stream()
          .filter(p -> p.isLeft() != chatParticipantRepository.findById(p.getId()).orElseThrow().isLeft()) // isLeft 변경된 것만 저장
          .collect(Collectors.toList()));


      return mapChatRoomToDto(room);
    } else {
      // 새로운 1대1 채팅방 생성
      ChatRoom newRoom = ChatRoom.builder()
          .type(ChatRoomType.SINGLE)
          .name(String.format("%s, %s의 1:1 채팅방", creator.getNickname(), otherUser.getNickname())) // 1:1 채팅방 이름 자동 생성
          .creator(null) // 1:1 채팅은 방장 없음 (ChatRoom 엔티티에 creator 필드 추가했음을 가정)
          .build();
      newRoom = chatRoomRepository.save(newRoom);

      // 참가자 추가
      ChatParticipant creatorParticipant = ChatParticipant.builder().user(creator).chatRoom(newRoom).build();
      ChatParticipant otherUserParticipant = ChatParticipant.builder().user(otherUser).chatRoom(newRoom).build();
      chatParticipantRepository.save(creatorParticipant);
      chatParticipantRepository.save(otherUserParticipant);

      log.info("New single chat room created (ID: {}) between user {} and {}", newRoom.getId(), creatorId, otherUserId);
      return mapChatRoomToDto(newRoom);
    }
  }

  /**
   * 그룹 채팅방을 생성합니다.
   *
   * @param creatorId 채팅방을 생성하려는 사용자 ID (방장)
   * @param requestDto 그룹 채팅방 생성 요청 DTO (채팅방 이름, 참가자 ID 목록)
   * @return 생성된 그룹 채팅방 응답 DTO
   */
  @Transactional
  public ChatRoomResponseDTO createGroupChatRoom(Long creatorId, ChatRoomCreateRequestDTO requestDto) {
    if (requestDto.getType() != ChatRoomType.GROUP) {
      throw new IllegalArgumentException("그룹 채팅방 생성 요청에는 type이 GROUP이어야 합니다.");
    }
    if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
      throw new IllegalArgumentException("그룹 채팅방 이름은 필수입니다.");
    }

    User creator = userRepository.findById(creatorId)
        .orElseThrow(() -> new EntityNotFoundException("Creator user not found with ID: " + creatorId));

    ChatRoom newRoom = ChatRoom.builder()
        .type(ChatRoomType.GROUP)
        .name(requestDto.getName())
        .creator(creator) // <-- creator 빌더 메서드 사용 (ChatRoom 엔티티에 creator 필드 추가했음을 가정)
        .build();
    newRoom = chatRoomRepository.save(newRoom);

    // 방장 먼저 참가자로 추가
    ChatParticipant creatorParticipant = ChatParticipant.builder().user(creator).chatRoom(newRoom).build();
    chatParticipantRepository.save(creatorParticipant);

    // 다른 참가자들 추가
    if (requestDto.getParticipantUserIds() != null) {
      for (Long participantId : requestDto.getParticipantUserIds()) {
        if (!participantId.equals(creatorId)) { // 방장 제외
          User participantUser = userRepository.findById(participantId)
              .orElseThrow(() -> new EntityNotFoundException("Participant user not found with ID: " + participantId));
          ChatParticipant participant = ChatParticipant.builder().user(participantUser).chatRoom(newRoom).build();
          chatParticipantRepository.save(participant);
        }
      }
    }
    log.info("New group chat room created (ID: {}) by user {}", newRoom.getId(), creatorId);
    return mapChatRoomToDto(newRoom);
  }

  /**
   * 특정 채팅방 상세 정보를 조회합니다.
   *
   * @param chatRoomId 채팅방 ID
   * @param userId 요청한 사용자 ID (권한 검증용)
   * @return 채팅방 응답 DTO
   */
  public ChatRoomResponseDTO getChatRoomDetails(Long chatRoomId, Long userId) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(chatRoomId)
        .orElseThrow(() -> new EntityNotFoundException("Chat room not found or deleted with ID: " + chatRoomId));

    // 요청한 사용자가 채팅방에 참여하고 있는지 확인
    boolean isParticipant = chatRoom.getParticipants().stream()
        .anyMatch(p -> p.getUser().getId().equals(userId) && !p.isLeft());
    if (!isParticipant) {
      throw new IllegalArgumentException("회원님은 이 채팅방의 참가자가 아닙니다.");
    }

    return mapChatRoomToDto(chatRoom);
  }

  /**
   * 특정 사용자가 참여하고 있는 모든 채팅방 목록을 조회합니다.
   *
   * @param userId 사용자 ID
   * @return 채팅방 목록 DTO
   */
  public List<ChatRoomResponseDTO> getUserChatRooms(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByUserId(userId);
    return chatRooms.stream()
        .map(this::mapChatRoomToDto)
        .collect(Collectors.toList());
  }

  /**
   * 그룹 채팅방에 새로운 참가자를 추가합니다.
   *
   * @param chatRoomId 그룹 채팅방 ID
   * @param creatorId 요청한 사용자 ID (방장만 가능)
   * @param newParticipantId 새로 추가할 사용자 ID
   * @return 업데이트된 채팅방 응답 DTO
   */
  @Transactional
  public ChatRoomResponseDTO addParticipantToGroupChat(Long chatRoomId, Long creatorId, Long newParticipantId) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndTypeAndIsDeletedFalse(chatRoomId, ChatRoomType.GROUP)
        .orElseThrow(() -> new EntityNotFoundException("그룹 채팅방을 찾을 수 없거나 삭제되었습니다. ID: " + chatRoomId));

    // chatRoom.getCreator() 오류 해결: ChatRoom 엔티티에 creator 필드가 있다고 가정하고 getter 호출
    if (chatRoom.getCreator() == null || !chatRoom.getCreator().getId().equals(creatorId)) { // <-- getCreator() 사용
      throw new IllegalArgumentException("그룹 채팅방의 방장만 참가자를 추가할 수 있습니다.");
    }

    User newParticipantUser = userRepository.findById(newParticipantId)
        .orElseThrow(() -> new EntityNotFoundException("새로 추가할 사용자를 찾을 수 없습니다. ID: " + newParticipantId));

    // 이미 참여 중인지 확인 (소프트 삭제된 상태 포함)
    Optional<ChatParticipant> existingParticipant = chatParticipantRepository.findByChatRoomAndUser(chatRoom, newParticipantUser);

    if (existingParticipant.isPresent()) {
      ChatParticipant participant = existingParticipant.get();
      if (!participant.isLeft()) { // 이미 참여 중이고 나가지 않은 상태
        throw new IllegalStateException("해당 사용자는 이미 채팅방에 참여 중입니다.");
      } else { // 이전에 나갔던 사용자라면 다시 참여하도록 상태 변경
        participant.rejoin(); // <-- rejoin() 메서드 사용
        log.info("User {} rejoined group chat room {}", newParticipantId, chatRoomId);
      }
    } else { // 새로 추가
      ChatParticipant participant = ChatParticipant.builder().user(newParticipantUser).chatRoom(chatRoom).build();
      chatParticipantRepository.save(participant);
      log.info("User {} added to group chat room {}", newParticipantId, chatRoomId);
    }
    return mapChatRoomToDto(chatRoom);
  }

  /**
   * 그룹 채팅방에서 참가자를 내보냅니다. (방장만 가능)
   *
   * @param chatRoomId 그룹 채팅방 ID
   * @param creatorId 요청한 사용자 ID (방장)
   * @param participantToRemoveId 내보낼 참가자 ID
   */
  @Transactional
  public void removeParticipantFromGroupChat(Long chatRoomId, Long creatorId, Long participantToRemoveId) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndTypeAndIsDeletedFalse(chatRoomId, ChatRoomType.GROUP)
        .orElseThrow(() -> new EntityNotFoundException("그룹 채팅방을 찾을 수 없거나 삭제되었습니다. ID: " + chatRoomId));

    if (chatRoom.getCreator() == null || !chatRoom.getCreator().getId().equals(creatorId)) { // <-- getCreator() 사용
      throw new IllegalArgumentException("그룹 채팅방의 방장만 참가자를 내보낼 수 있습니다.");
    }
    if (creatorId.equals(participantToRemoveId)) {
      throw new IllegalArgumentException("방장은 스스로를 내보낼 수 없습니다. 채팅방을 삭제해야 합니다.");
    }

    User participantUser = userRepository.findById(participantToRemoveId)
        .orElseThrow(() -> new EntityNotFoundException("내보낼 사용자를 찾을 수 없습니다. ID: " + participantToRemoveId));

    ChatParticipant participant = chatParticipantRepository.findByChatRoomAndUserAndIsLeft(chatRoom, participantUser, false)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자는 이 채팅방의 활성 참가자가 아닙니다."));

    participant.leave(); // <-- leave() 메서드 사용 (소프트 삭제)
    log.info("User {} removed from group chat room {}", participantToRemoveId, chatRoomId);
  }

  /**
   * 사용자가 그룹 채팅방을 나갑니다.
   *
   * @param chatRoomId 그룹 채팅방 ID
   * @param userId 나가는 사용자 ID
   */
  @Transactional
  public void leaveGroupChatRoom(Long chatRoomId, Long userId) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndTypeAndIsDeletedFalse(chatRoomId, ChatRoomType.GROUP)
        .orElseThrow(() -> new EntityNotFoundException("그룹 채팅방을 찾을 수 없거나 삭제되었습니다. ID: " + chatRoomId));

    ChatParticipant participant = chatParticipantRepository.findByChatRoomAndUserAndIsLeft(chatRoom, userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId)), false)
        .orElseThrow(() -> new IllegalArgumentException("회원님은 이 채팅방의 활성 참가자가 아닙니다."));

    // 방장이 나갈 경우, 새로운 방장을 위임하거나 채팅방을 비활성화하는 로직 필요
    if (chatRoom.getCreator() != null && chatRoom.getCreator().getId().equals(userId)) { // <-- getCreator() 사용
      throw new IllegalArgumentException("방장은 채팅방을 나갈 수 없습니다. 채팅방을 삭제하거나 방장 권한을 위임해주세요.");
    }

    participant.leave(); // <-- leave() 메서드 사용 (소프트 삭제)
    log.info("User {} left group chat room {}", userId, chatRoomId);

    // 만약 나간 후 참가자가 0명이 된다면 채팅방을 삭제 (선택 사항)
    long activeParticipantsCount = chatRoom.getParticipants().stream().filter(p -> !p.isLeft()).count();
    if (activeParticipantsCount <= 1) { // 마지막 남은 한 명이 나갈 경우 (혹은 0명이 되는 경우)
      chatRoom.delete(); // 채팅방 소프트 삭제
      log.info("Group chat room {} deleted as no active participants remain.", chatRoomId);
    }
  }

  /**
   * 채팅방을 소프트 삭제합니다. (방장 또는 1:1 채팅방의 경우 양측 모두)
   *
   * @param chatRoomId 삭제할 채팅방 ID
   * @param userId 요청한 사용자 ID
   */
  @Transactional
  public void deleteChatRoom(Long chatRoomId, Long userId) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(chatRoomId)
        .orElseThrow(() -> new EntityNotFoundException("Chat room not found or deleted with ID: " + chatRoomId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    if (chatRoom.getType() == ChatRoomType.GROUP) {
      if (chatRoom.getCreator() == null || !chatRoom.getCreator().getId().equals(userId)) { // <-- getCreator() 사용
        throw new IllegalArgumentException("그룹 채팅방은 방장만 삭제할 수 있습니다.");
      }
    } else { // SINGLE chat room
      // 1:1 채팅방은 양쪽 모두 나간 상태여야 완전히 삭제 (사용자 입장에서는 나가기 기능)
      // 여기서는 '삭제' 기능을 '완전한 퇴장'으로 간주
      ChatParticipant participant = chatParticipantRepository.findByChatRoomAndUserAndIsLeft(chatRoom, user, false)
          .orElseThrow(() -> new IllegalArgumentException("회원님은 이 채팅방의 활성 참가자가 아닙니다."));
      participant.leave(); // <-- leave() 메서드 사용 (isLeft를 true로 변경)

      long activeParticipantsCount = chatRoom.getParticipants().stream().filter(p -> !p.isLeft()).count();
      if (activeParticipantsCount == 0) { // 양측 모두 나갔다면
        chatRoom.delete(); // 채팅방 소프트 삭제
        log.info("Single chat room {} deleted as both participants left.", chatRoomId);
      } else {
        log.info("User {} left single chat room {}. Room not fully deleted yet.", userId, chatRoomId);
      }
      return; // 1:1 채팅방 삭제 로직은 여기서 종료
    }

    chatRoom.delete(); // 그룹 채팅방 소프트 삭제
    log.info("Chat room {} (type: {}) deleted by user {}", chatRoomId, chatRoom.getType(), userId);
  }


  /**
   * ChatRoom 엔티티를 ChatRoomResponseDTO로 매핑하고, 마지막 메시지를 추가합니다.
   *
   * @param chatRoom 매핑할 ChatRoom 엔티티
   * @return 매핑된 ChatRoomResponseDTO
   */
  private ChatRoomResponseDTO mapChatRoomToDto(ChatRoom chatRoom) {
    ChatRoomResponseDTO dto = new ChatRoomResponseDTO(chatRoom);

    // 마지막 메시지 조회 및 설정
    Pageable pageable = PageRequest.of(0, 1); // 최신 메시지 1개만 가져옴
    List<ChatMessage> lastMessages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable);
    if (!lastMessages.isEmpty()) {
      dto.setLastMessage(new ChatMessageResponseDTO(lastMessages.get(0)));
    }
    return dto;
  }
}
