package com.example.sleeprism.controller;

import com.example.sleeprism.dto.ChatMessageRequestDTO;
import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.ChatMessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.security.Principal; // Principal import 다시 추가

/**
 * WebSocket을 통해 실시간 채팅 메시지를 처리하는 컨트롤러입니다.
 * STOMP 메시징 프로토콜을 사용합니다.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

  private final SimpMessageSendingOperations messagingTemplate;
  private final ChatMessageService chatMessageService;

  /**
   * 클라이언트로부터 채팅 메시지를 받아 처리하고, 해당 채팅방에 메시지를 브로드캐스트합니다.
   * 클라이언트는 "/app/chat.sendMessage"로 메시지를 전송합니다.
   *
   * @param chatMessageRequest 메시지 요청 DTO (채팅방 ID, 내용)
   * @param principal 현재 인증된 사용자 정보 (Principal 인터페이스, Spring이 주입)
   * @return 전송된 메시지 응답 DTO (클라이언트로 다시 전송될 수 있음)
   */
  @MessageMapping("/chat.sendMessage")
  public ChatMessageResponseDTO sendMessage(@Payload ChatMessageRequestDTO chatMessageRequest,
                                            Principal principal) { // Principal 인자 다시 받음
    User currentUser = null;

    // Principal 객체 확인 및 User 추출
    if (principal instanceof Authentication) {
      Authentication authentication = (Authentication) principal;
      if (authentication.getPrincipal() instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof User) {
          currentUser = (User) userDetails;
          log.info("Successfully retrieved current user from Principal. User: {}", currentUser.getUsername());
        } else {
          log.error("Principal is UserDetails but not an instance of com.example.sleeprism.entity.User. Type: {}", userDetails.getClass().getName());
        }
      } else {
        log.error("Principal is an Authentication but its principal is not an instance of UserDetails. Type: {}", authentication.getPrincipal().getClass().getName());
      }
    } else if (principal != null) {
      log.error("Provided Principal is not an instance of Authentication. Type: {}", principal.getClass().getName());
    } else {
      log.error("Provided Principal is null. Authentication context not available.");
    }

    // 1. 사용자 인증 확인
    if (currentUser == null) {
      log.error("AuthenticationPrincipal User is null. User not authenticated for WebSocket message send.");
      throw new AccessDeniedException("Authentication required to send messages.");
    }

    Long senderId = currentUser.getId();
    String senderNickname = currentUser.getNickname();

    log.info("Received message from user ID {}: chatRoomId={}, content='{}'", senderId, chatMessageRequest.getChatRoomId(), chatMessageRequest.getContent());

    ChatMessageResponseDTO savedMessage;
    try {
      savedMessage = chatMessageService.saveChatMessage(
          chatMessageRequest.getChatRoomId(),
          senderId,
          chatMessageRequest.getContent()
      );

      if (savedMessage.getSenderNickname() == null) {
        savedMessage.setSenderNickname(senderNickname);
      }

      String destination = "/topic/chat/room/" + chatMessageRequest.getChatRoomId();
      messagingTemplate.convertAndSend(destination, savedMessage);
      log.info("Message successfully sent to topic {}: {}", destination, savedMessage.getContent());

    } catch (IllegalArgumentException | IllegalStateException | EntityNotFoundException e) {
      log.error("Failed to send message for user {}: {}", senderId, e.getMessage());
      messagingTemplate.convertAndSendToUser(
          currentUser.getId().toString(),
          "/queue/errors",
          "메시지 전송 실패: " + e.getMessage()
      );
      return null;
    } catch (AccessDeniedException e) {
      log.warn("Access denied for user {} when sending message: {}", senderId, e.getMessage());
      messagingTemplate.convertAndSendToUser(
          currentUser.getId().toString(),
          "/queue/errors",
          "메시지 전송 권한이 없습니다: " + e.getMessage()
      );
      return null;
    }
    catch (Exception e) {
      log.error("An unexpected error occurred while sending message for user {}: {}", senderId, e.getMessage(), e);
      messagingTemplate.convertAndSendToUser(
          currentUser.getId().toString(),
          "/queue/errors",
          "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
      );
      return null;
    }
    return savedMessage;
  }

  /**
   * (선택 사항) 새로운 사용자가 채팅방에 입장했음을 알리는 메시지.
   * 클라이언트가 "/app/chat.addUser"로 메시지를 전송할 때 사용될 수 있습니다.
   * 주로 그룹 채팅방 입장/퇴장 알림용으로 활용됩니다.
   *
   * @param chatMessageRequest 메시지 요청 DTO (채팅방 ID, 입장 메시지 내용)
   * @param principal 현재 인증된 사용자 정보
   */
  @MessageMapping("/chat.addUser")
  public void addUser(@Payload ChatMessageRequestDTO chatMessageRequest,
                      Principal principal) { // Principal 인자 다시 받음
    User currentUser = null;
    if (principal instanceof Authentication) {
      Authentication authentication = (Authentication) principal;
      if (authentication.getPrincipal() instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof User) {
          currentUser = (User) userDetails;
        }
      }
    }

    if (currentUser == null) {
      log.warn("AuthenticationPrincipal User is null when trying to add user to chat.");
      return;
    }
    Long userId = currentUser.getId();
    String userNickname = currentUser.getNickname();

    log.info("User ID {} ({}) is attempting to join chat room {}", userId, userNickname, chatMessageRequest.getChatRoomId());

    ChatMessageResponseDTO joinMessage = new ChatMessageResponseDTO();
    joinMessage.setChatRoomId(chatMessageRequest.getChatRoomId());
    joinMessage.setContent(String.format("%s님이 입장했습니다.", userNickname));
    joinMessage.setSenderId(0L);
    joinMessage.setSenderNickname("System");

    String destination = "/topic/chat/room/" + chatMessageRequest.getChatRoomId();
    messagingTemplate.convertAndSend(destination, joinMessage);
    log.info("System join message sent to topic {}: {}", destination, joinMessage.getContent());
  }
}
