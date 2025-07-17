package com.example.sleeprism.service;

import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.entity.*;
import com.example.sleeprism.repository.ChatMessageRepository;
import com.example.sleeprism.repository.ChatParticipantRepository;
import com.example.sleeprism.repository.ChatRoomRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;
  private final UserRepository userRepository;
  // private final ChatBlockRepository chatBlockRepository; // 차단 기능 (필요시 주석 해제)
  private final NotificationService notificationService; // 알림 서비스 주입

  /**
   * 새로운 채팅 메시지를 저장합니다.
   * 메시지 전송 전에 발신자-수신자 간 차단 여부를 확인합니다.
   *
   * @param chatRoomId 메시지가 전송될 채팅방 ID
   * @param senderId 메시지 발신자 ID
   * @param content 메시지 내용
   * @return 저장된 메시지의 응답 DTO
   */
  @Transactional
  public ChatMessageResponseDTO saveChatMessage(Long chatRoomId, Long senderId, String content, MessageType messageType) {
    log.info("Saving message. RoomId: {}, SenderId: {}, Type: {}, Content: '{}'", chatRoomId, senderId, messageType, content);

    // 1. 채팅방 유효성 검사
    ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
        .orElseThrow(() -> {
          log.error("ChatMessageService: ChatRoom not found with ID: {}", chatRoomId);
          return new EntityNotFoundException("채팅방을 찾을 수 없습니다.");
        });
    log.info("ChatMessageService: ChatRoom found: {}", chatRoom.getName());

    // 2. 발신자 User 엔티티 조회
    User sender = userRepository.findById(senderId)
        .orElseThrow(() -> {
          log.error("ChatMessageService: Sender User not found with ID: {}", senderId);
          return new EntityNotFoundException("발신자 사용자를 찾을 수 없습니다.");
        });
    log.info("ChatMessageService: Sender User found: {}", sender.getNickname());


    // 3. 메시지 엔티티 생성 (sender 필드에 User 객체 직접 할당)
    ChatMessage chatMessage = ChatMessage.builder()
        .chatRoom(chatRoom)
        .sender(sender) // <-- sender User 객체 할당
        .content(content)
        .messageType(messageType)
        .isRead(false)
        .build();
    log.info("ChatMessageService: ChatMessage entity created. Sender User ID: {}", chatMessage.getSender().getId());

    // 4. 메시지 저장
    ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
    log.info("ChatMessageService: ChatMessage saved with ID: {}", savedMessage.getId());

    // 5. 채팅방 참여자들의 마지막 읽은 메시지 ID 업데이트 및 알림 전송
    List<ChatParticipant> activeParticipants = chatParticipantRepository.findByChatRoomAndIsLeft(chatRoom, false); // 활성 참여자만
    log.info("ChatMessageService: Found {} active participants for chat room {}", activeParticipants.size(), chatRoomId);

    for (ChatParticipant participant : activeParticipants) {
      if (participant.getUser().getId().equals(senderId)) { // 메시지 보낸 사람
        participant.updateLastReadMessageId(savedMessage.getId()); // 편의 메서드 사용
        log.info("ChatMessageService: User {} (sender) last read message ID updated to {}", participant.getUser().getId(), savedMessage.getId());
      } else { // 메시지 받은 사람
        String notificationMessage = String.format("'%s'님이 새 메시지를 보냈습니다: '%s'",
            sender.getNickname(), savedMessage.getContent().substring(0, Math.min(savedMessage.getContent().length(), 50)) + "...");
        String redirectPath = String.format("/chatrooms/%d", chatRoom.getId());

        // (선택 사항) 차단된 사용자에게는 알림을 보내지 않음 (필요시 주석 해제)
        // if (chatBlockRepository.findByBlockerAndBlocked(participant.getUser(), sender).isEmpty()) {
        notificationService.createNotification(
            participant.getUser(),
            NotificationType.CHAT_MESSAGE, // NotificationType enum 사용
            notificationMessage,
            "ChatRoom", // targetEntityType
            chatRoom.getId(), // targetEntityId
            redirectPath
        );
        log.info("ChatMessageService: CHAT_MESSAGE notification sent to user {} for ChatRoom ID: {}", participant.getUser().getId(), chatRoomId);
        // } else {
        //     log.info("ChatMessageService: CHAT_MESSAGE notification not sent to blocked user {} from sender {}.", participant.getUser().getId(), senderId);
        // }
      }
      chatParticipantRepository.save(participant); // 변경사항 저장
    }

    // 6. 응답 DTO 생성
    return new ChatMessageResponseDTO(savedMessage); // ChatMessageResponseDTO 생성자에 savedMessage 전달
  }

  /**
   * 특정 채팅방의 메시지 내역을 조회합니다. (최신순)
   *
   * @param chatRoomId 채팅방 ID
   * @param userId 메시지 내역을 조회하는 사용자 ID (권한 검증용)
   * @param page 조회할 페이지 번호 (0부터 시작)
   * @param size 페이지당 메시지 수
   * @return 메시지 목록 DTO
   */
  @Transactional(readOnly = true)
  public List<ChatMessageResponseDTO> getChatHistory(Long chatRoomId, Long userId, int page, int size) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
        .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + chatRoomId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // 사용자가 채팅방의 참가자인지 확인
    chatParticipantRepository.findByChatRoomAndUserAndIsLeft(chatRoom, user, false)
        .orElseThrow(() -> new IllegalArgumentException("회원님은 이 채팅방의 참가자가 아닙니다."));

    List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom);

    // 메시지 읽음 처리 (선택 사항: 사용자가 메시지 내역을 조회하면 해당 방의 모든 메시지를 읽음 처리)
    chatParticipantRepository.findByChatRoomAndUser(chatRoom, user)
        .ifPresent(p -> {
          if (!messages.isEmpty()) {
            p.updateLastReadMessageId(messages.get(0).getId()); // 가장 최신 메시지의 ID로 업데이트
            chatParticipantRepository.save(p);
            log.info("User {} last read message ID updated to {} for chat room {}", userId, messages.get(0).getId(), chatRoomId);
          }
        });

    return messages.stream()
        .map(ChatMessageResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 특정 메시지를 읽음 상태로 업데이트합니다.
   * @param messageId 메시지 ID
   * @param userId 메시지를 읽은 사용자 ID
   * @return 업데이트된 메시지 DTO
   */
  @Transactional
  public ChatMessageResponseDTO markMessageAsRead(Long messageId, Long userId) {
    ChatMessage message = chatMessageRepository.findById(messageId)
        .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // 사용자가 해당 메시지가 속한 채팅방의 참가자인지 확인 (나갔다면 읽음 처리 불가)
    chatParticipantRepository.findByChatRoomAndUserAndIsLeft(message.getChatRoom(), user, false)
        .orElseThrow(() -> new IllegalArgumentException("메시지를 읽을 권한이 없습니다 (채팅방 참가자 아님)."));

    if (!message.isRead()) {
      message.markAsRead();
      log.info("Message {} marked as read by user {}", messageId, userId);

      // 참가자의 마지막 읽은 메시지 ID 업데이트 (선택 사항)
      chatParticipantRepository.findByChatRoomAndUser(message.getChatRoom(), user)
          .ifPresent(p -> {
            if (p.getLastReadMessageId() == null || message.getId() > p.getLastReadMessageId()) {
              p.updateLastReadMessageId(message.getId());
              chatParticipantRepository.save(p);
              log.info("User {} last read message ID updated to {} for chat room {}", userId, message.getId(), message.getChatRoom().getId());
            }
          });
    }
    return new ChatMessageResponseDTO(message);
  }
}
