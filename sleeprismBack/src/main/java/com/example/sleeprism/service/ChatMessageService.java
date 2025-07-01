package com.example.sleeprism.service;

import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.entity.ChatBlock;
import com.example.sleeprism.entity.ChatMessage;
import com.example.sleeprism.entity.ChatParticipant;
import com.example.sleeprism.entity.ChatRoom;
import com.example.sleeprism.entity.NotificationType;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.ChatBlockRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final UserRepository userRepository;
  private final ChatParticipantRepository chatParticipantRepository;
  private final ChatBlockRepository chatBlockRepository; // 차단 기능 추가
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
  public ChatMessageResponseDTO saveChatMessage(Long chatRoomId, Long senderId, String content) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(chatRoomId)
        .orElseThrow(() -> new EntityNotFoundException("Chat room not found or deleted with ID: " + chatRoomId));
    User sender = userRepository.findById(senderId)
        .orElseThrow(() -> new EntityNotFoundException("Sender user not found with ID: " + senderId));

    // 발신자가 채팅방의 활성 참가자인지 확인
    chatParticipantRepository.findByChatRoomAndUserAndIsLeft(chatRoom, sender, false)
        .orElseThrow(() -> new IllegalArgumentException("메시지를 보낼 수 있는 채팅방 참가자가 아닙니다."));

    // 1:1 채팅방의 경우, 차단 여부 확인
    if (chatRoom.getType() == com.example.sleeprism.entity.ChatRoomType.SINGLE) {
      List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomAndIsLeft(chatRoom, false);
      if (participants.size() == 2) { // 1:1 채팅방임을 다시 확인
        User recipient = participants.stream()
            .filter(p -> !p.getUser().getId().equals(senderId))
            .map(ChatParticipant::getUser)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("1:1 채팅방에서 상대방을 찾을 수 없습니다."));

        // 발신자가 수신자를 차단했는지 또는 수신자가 발신자를 차단했는지 확인
        if (chatBlockRepository.findByBlockerAndBlocked(sender, recipient).isPresent() ||
            chatBlockRepository.findByBlockerAndBlocked(recipient, sender).isPresent()) {
          throw new IllegalArgumentException("채팅이 차단된 사용자에게 메시지를 보낼 수 없습니다.");
        }
      }
    }

    ChatMessage chatMessage = ChatMessage.builder()
        .chatRoom(chatRoom)
        .sender(sender)
        .content(content)
        .build();

    ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
    log.info("Chat message saved for chat room {}: {}", chatRoomId, content);

    // --- 알림 생성 로직 (백그라운드에서 처리) ---
    // 메시지를 받은 모든 활성 참가자에게 알림 생성
    List<ChatParticipant> activeParticipants = chatParticipantRepository.findByChatRoomAndIsLeft(chatRoom, false);
    for (ChatParticipant participant : activeParticipants) {
      if (!participant.getUser().getId().equals(senderId)) { // 메시지 보낸 사람 제외
        String message = String.format("'%s'님이 새 메시지를 보냈습니다: '%s'",
            sender.getNickname(), savedMessage.getContent().substring(0, Math.min(savedMessage.getContent().length(), 50)) + "...");
        String redirectPath = String.format("/chatrooms/%d", chatRoom.getId()); // 채팅방으로 이동하는 경로

        // 차단된 사용자에게는 알림을 보내지 않음
        if (chatBlockRepository.findByBlockerAndBlocked(participant.getUser(), sender).isEmpty()) { // 수신자가 발신자를 차단하지 않았다면
          notificationService.createNotification(participant.getUser(), NotificationType.CHAT_MESSAGE, message,
              "ChatRoom", chatRoom.getId(), redirectPath);
          log.info("CHAT_MESSAGE notification sent to user {} for ChatRoom ID: {}", participant.getUser().getId(), chatRoom.getId());
        } else {
          log.info("CHAT_MESSAGE notification not sent to blocked user {} from sender {}.", participant.getUser().getId(), senderId);
        }
      }
    }
    // --- 알림 생성 로직 끝 ---

    return new ChatMessageResponseDTO(savedMessage);
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
  public List<ChatMessageResponseDTO> getChatHistory(Long chatRoomId, Long userId, int page, int size) {
    ChatRoom chatRoom = chatRoomRepository.findByIdAndIsDeletedFalse(chatRoomId)
        .orElseThrow(() -> new EntityNotFoundException("Chat room not found or deleted with ID: " + chatRoomId));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // 사용자가 채팅방의 참가자인지 확인
    chatParticipantRepository.findByChatRoomAndUserAndIsLeft(chatRoom, user, false)
        .orElseThrow(() -> new IllegalArgumentException("회원님은 이 채팅방의 참가자가 아닙니다."));

    Pageable pageable = PageRequest.of(page, size);
    List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable);

    // 메시지 읽음 처리 (선택 사항: 사용자가 메시지 내역을 조회하면 해당 방의 모든 메시지를 읽음 처리)
    // 이 로직은 UI에서 특정 메시지를 읽었을 때만 호출되도록 하거나, 별도의 API로 분리하는 것이 더 적절할 수 있습니다.
    // 여기서는 간단히 조회 시점에 최신 메시지를 읽은 것으로 처리합니다.
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
