package com.example.sleeprism.controller;

import com.example.sleeprism.dto.ChatMessageHistoryRequestDTO;
import com.example.sleeprism.dto.ChatMessageRequestDTO;
import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.ChatMessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

  private final SimpMessageSendingOperations messagingTemplate;
  private final ChatMessageService chatMessageService;

  @MessageMapping("/chat.sendMessage")
  public ChatMessageResponseDTO sendMessage(@Payload ChatMessageRequestDTO chatMessageRequest,
                                            @AuthenticationPrincipal User currentUser) {
    // 1. 사용자 인증 확인
    if (currentUser == null) {
      log.error("AuthenticationPrincipal User is null. User not authenticated for WebSocket message send.");
      throw new AccessDeniedException("Authentication required to send messages.");
    }

    // 여기서 currentUser의 ID를 명확히 로깅하여 확인합니다.
    log.info("ChatController: Debugging currentUser ID: {}", currentUser.getId());

    Long senderId = currentUser.getId();
    String senderNickname = currentUser.getNickname();

    log.info("ChatController: Received message from user ID {}: chatRoomId={}, content='{}'", senderId, chatMessageRequest.getChatRoomId(), chatMessageRequest.getContent());

    ChatMessageResponseDTO savedMessage;
    try {
      savedMessage = chatMessageService.saveChatMessage(
          chatMessageRequest.getChatRoomId(),
          currentUser.getId(),
          chatMessageRequest.getContent(),
          chatMessageRequest.getMessageType() // messageType을 서비스로 전달
      );

      if (savedMessage.getSenderNickname() == null) {
        savedMessage.setSenderNickname(senderNickname);
      }

      String destination = "/topic/chat/room/" + chatMessageRequest.getChatRoomId();
      messagingTemplate.convertAndSend(destination, savedMessage);
      log.info("ChatController: Message successfully sent to topic {}: {}", destination, savedMessage.getContent());

    } catch (Exception e) {
      log.error("ChatController: Failed to send message for user {}: {}", senderId, e.getMessage(), e);
      messagingTemplate.convertAndSendToUser(
          currentUser.getId().toString(),
          "/queue/errors",
          "메시지 전송 실패: " + e.getMessage()
      );
      return null;
    }
    return savedMessage;
  }

  @MessageMapping("/chat.addUser")
  public void addUser(@Payload ChatMessageRequestDTO chatMessageRequest,
                      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("AuthenticationPrincipal User is null when trying to add user to chat.");
      return;
    }
    Long userId = currentUser.getId();
    String userNickname = currentUser.getNickname();

    log.info("ChatController: User ID {} ({}) is attempting to join chat room {}", userId, userNickname, chatMessageRequest.getChatRoomId());

    ChatMessageResponseDTO joinMessage = new ChatMessageResponseDTO();
    joinMessage.setChatRoomId(chatMessageRequest.getChatRoomId());
    joinMessage.setContent(String.format("%s님이 입장했습니다.", userNickname));
    joinMessage.setSenderId(0L);
    joinMessage.setSenderNickname("System");

    // ↓↓↓ 이 두 줄을 추가하여 완전한 메시지 객체를 만듭니다. ↓↓↓
    joinMessage.setSentAt(java.time.LocalDateTime.now()); // 현재 시간 추가
    joinMessage.setId(System.currentTimeMillis()); // 임시 고유 ID 추가

    String destination = "/topic/chat/room/" + chatMessageRequest.getChatRoomId();
    messagingTemplate.convertAndSend(destination, joinMessage);
    log.info("ChatController: System join message sent to topic {}: {}", destination, joinMessage.getContent());
  }

  /**
   * 클라이언트의 과거 메시지 내역 요청을 처리합니다.
   * @param request 클라이언트가 보낸 요청 (chatRoomId 포함)
   * @param currentUser 현재 인증된 사용자 정보
   */
  @MessageMapping("/chat.history")
  public void getChatHistory(@Payload ChatMessageHistoryRequestDTO request,
                             @AuthenticationPrincipal User currentUser) {

    if (currentUser == null) {
      log.error("AuthenticationPrincipal User is null. Cannot fetch chat history.");
      return;
    }

    Long roomId = request.getChatRoomId();
    log.info("User '{}' requested chat history for room {}.", currentUser.getEmail(), roomId);

    try {
      // 이미 만들어둔 완벽한 서비스 메소드를 호출합니다.
      List<ChatMessageResponseDTO> history = chatMessageService.getChatHistory(roomId, currentUser.getId(), 0, 100); // 최근 100개

      // 결과를 요청한 사용자에게만 개인적으로 보냅니다.
      messagingTemplate.convertAndSendToUser(
          currentUser.getEmail(), // Spring Security의 UserDetails.getUsername()과 동일
          "/queue/chat/history/" + roomId,
          history
      );

      log.info("Successfully sent {} history messages for room {} to user '{}'.", history.size(), roomId, currentUser.getEmail());

    } catch (Exception e) {
      log.error("Failed to fetch or send chat history for room {} to user '{}': {}", roomId, currentUser.getEmail(), e.getMessage(), e);
      // 에러 발생 시 사용자에게 에러 메시지를 보낼 수도 있습니다.
      messagingTemplate.convertAndSendToUser(
          currentUser.getEmail(),
          "/queue/errors",
          "채팅 내역을 불러오는 데 실패했습니다: " + e.getMessage()
      );
    }
  }

  /*
  @SubscribeMapping("/room/{roomId}")
  @Transactional(readOnly = true) // DTO 변환 중 예외 방지를 위해 반드시 트랜잭션으로 묶습니다.
  public List<ChatMessageResponseDTO> handleSubscription(
      @DestinationVariable Long roomId,
      @AuthenticationPrincipal User currentUser) {

    log.info("User {} subscribed to /app/room/{}. Sending chat history.", currentUser.getUsername(), roomId);

    // 이미 만들어둔 안전한 서비스 메소드를 호출하여 데이터를 반환합니다.
    // 이 메소드의 반환값은 자동으로 해당 구독자에게만 전송됩니다.
    return chatMessageService.getChatHistory(roomId, currentUser.getId(), 0, 100); // 최근 100개
  }
*/
}
