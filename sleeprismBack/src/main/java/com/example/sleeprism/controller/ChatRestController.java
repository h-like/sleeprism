package com.example.sleeprism.controller;

import com.example.sleeprism.dto.ChatBlockRequestDTO;
import com.example.sleeprism.dto.ChatBlockResponseDTO;
import com.example.sleeprism.dto.ChatRoomCreateRequestDTO;
import com.example.sleeprism.dto.ChatRoomResponseDTO;
import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.entity.ChatRoomType;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.ChatBlockService;
import com.example.sleeprism.service.ChatMessageService;
import com.example.sleeprism.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅방 생성, 조회, 메시지 내역 조회, 사용자 차단 등 채팅 관련 REST API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/chats") // 기본 경로 설정
@RequiredArgsConstructor
@Slf4j
public class ChatRestController {

  private final ChatRoomService chatRoomService;
  private final ChatMessageService chatMessageService;
  private final ChatBlockService chatBlockService;

  /**
   * 1대1 채팅방을 생성하거나 기존 채팅방을 조회합니다.
   * 프론트엔드의 `createOrGetSingleChatRoom` 함수가 이 API를 호출합니다.
   *
   * @param requestDto 채팅방 생성 요청 DTO (participantUserIds에 상대방 ID 포함)
   * @param currentUser 현재 로그인한 사용자 정보
   * @return 생성되거나 조회된 1대1 채팅방 응답
   */
  @PostMapping("/single")
  public ResponseEntity<ChatRoomResponseDTO> createOrGetSingleChatRoom(
      @Valid @RequestBody ChatRoomCreateRequestDTO requestDto,
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to create or get single chat room.");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (requestDto.getParticipantUserIds() == null || requestDto.getParticipantUserIds().size() != 1) {
      log.warn("Invalid participantUserIds for single chat room creation: {}", requestDto.getParticipantUserIds());
      return ResponseEntity.badRequest().body(null);
    }

    Long otherUserId = requestDto.getParticipantUserIds().get(0);
    log.info("Request to create or get single chat room between user {} and {}", currentUser.getId(), otherUserId);
    try {
      ChatRoomResponseDTO chatRoom = chatRoomService.createOrGetSingleChatRoom(currentUser.getId(), otherUserId);
      return ResponseEntity.status(HttpStatus.CREATED).body(chatRoom);
    } catch (IllegalArgumentException e) {
      log.error("Failed to create or get single chat room: {}", e.getMessage());
      return ResponseEntity.badRequest().body(null); // 에러 메시지를 포함하는 DTO 반환 고려
    } catch (Exception e) {
      log.error("Error creating or getting single chat room", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 그룹 채팅방을 생성합니다.
   *
   * @param requestDto 그룹 채팅방 생성 요청 DTO (채팅방 이름, 참가자 ID 목록)
   * @param currentUser 현재 로그인한 사용자 정보 (방장이 됨)
   * @return 생성된 그룹 채팅방 응답
   */
  @PostMapping("/group")
  public ResponseEntity<ChatRoomResponseDTO> createGroupChatRoom(
      @Valid @RequestBody ChatRoomCreateRequestDTO requestDto,
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to create group chat room.");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (requestDto.getType() != ChatRoomType.GROUP) {
      log.warn("Invalid chat room type for group creation: {}", requestDto.getType());
      return ResponseEntity.badRequest().body(null);
    }
    if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
      log.warn("Group chat room name is empty.");
      return ResponseEntity.badRequest().body(null);
    }

    log.info("Request to create group chat room '{}' by user {}", requestDto.getName(), currentUser.getId());
    try {
      ChatRoomResponseDTO chatRoom = chatRoomService.createGroupChatRoom(currentUser.getId(), requestDto);
      return ResponseEntity.status(HttpStatus.CREATED).body(chatRoom);
    } catch (IllegalArgumentException e) {
      log.error("Failed to create group chat room: {}", e.getMessage());
      return ResponseEntity.badRequest().body(null);
    } catch (Exception e) {
      log.error("Error creating group chat room", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 현재 로그인한 사용자가 참여하고 있는 모든 채팅방 목록을 조회합니다.
   *
   * @param currentUser 현재 로그인한 사용자 정보
   * @return 채팅방 목록 응답
   */
  @GetMapping
  public ResponseEntity<List<ChatRoomResponseDTO>> getUserChatRooms(
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to get user chat rooms.");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Request to get chat rooms for user {}", currentUser.getId());
    try {
      List<ChatRoomResponseDTO> chatRooms = chatRoomService.getUserChatRooms(currentUser.getId());
      return ResponseEntity.ok(chatRooms);
    } catch (Exception e) {
      log.error("Error getting user chat rooms", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 특정 채팅방의 메시지 내역을 조회합니다.
   *
   * @param chatRoomId 채팅방 ID
   * @param page 조회할 페이지 번호 (기본값: 0)
   * @param size 페이지당 메시지 수 (기본값: 50)
   * @param currentUser 현재 로그인한 사용자 정보
   * @return 메시지 목록 응답
   */
  @GetMapping("/{chatRoomId}/messages")
  public ResponseEntity<List<ChatMessageResponseDTO>> getChatHistory(
      @PathVariable Long chatRoomId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to get chat history for room {}.", chatRoomId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Request to get chat history for room {} by user {}", chatRoomId, currentUser.getId());
    try {
      List<ChatMessageResponseDTO> messages = chatMessageService.getChatHistory(chatRoomId, currentUser.getId(), page, size);
      return ResponseEntity.ok(messages);
    } catch (IllegalArgumentException e) {
      log.error("Failed to get chat history: {}", e.getMessage());
      return ResponseEntity.badRequest().body(null);
    } catch (Exception e) {
      log.error("Error getting chat history for room {}", chatRoomId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * (선택 사항) 특정 알림을 읽음 상태로 업데이트합니다. (채팅방 메시지 읽음 처리용)
   *
   * @param messageId 읽음 처리할 메시지 ID
   * @param currentUser 현재 로그인한 사용자 정보
   * @return 업데이트된 메시지 응답
   */
  @PatchMapping("/messages/{messageId}/read")
  public ResponseEntity<ChatMessageResponseDTO> markMessageAsRead(
      @PathVariable Long messageId,
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to mark message {} as read.", messageId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Request to mark message {} as read by user {}", messageId, currentUser.getId());
    try {
      ChatMessageResponseDTO updatedMessage = chatMessageService.markMessageAsRead(messageId, currentUser.getId());
      return ResponseEntity.ok(updatedMessage);
    } catch (IllegalArgumentException e) {
      log.error("Failed to mark message as read: {}", e.getMessage());
      return ResponseEntity.badRequest().body(null);
    } catch (Exception e) {
      log.error("Error marking message {} as read", messageId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // --- 사용자 차단/해제 관련 API ---
  // @RequestMapping("/api/chat-blocks") 경로로 분리될 수 있습니다.

  /**
   * 사용자를 차단합니다.
   *
   * @param requestDto 차단할 사용자 ID DTO
   * @param currentUser 현재 로그인한 사용자 정보 (차단자)
   * @return 생성된 차단 정보 응답
   */
  @PostMapping("/blocks") // /api/chats/blocks 또는 /api/chat-blocks로 분리 가능
  public ResponseEntity<ChatBlockResponseDTO> blockUser(
      @Valid @RequestBody ChatBlockRequestDTO requestDto,
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to block user.");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Request to block user {} by user {}", requestDto.getBlockedUserId(), currentUser.getId());
    try {
      ChatBlockResponseDTO chatBlock = chatBlockService.blockUser(currentUser.getId(), requestDto.getBlockedUserId());
      return ResponseEntity.status(HttpStatus.CREATED).body(chatBlock);
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.error("Failed to block user: {}", e.getMessage());
      return ResponseEntity.badRequest().body(null);
    } catch (Exception e) {
      log.error("Error blocking user {}", requestDto.getBlockedUserId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 사용자의 차단을 해제합니다.
   *
   * @param blockedUserId 차단 해제할 사용자 ID
   * @param currentUser 현재 로그인한 사용자 정보 (차단 해제자)
   * @return 응답 없음 (No Content)
   */
  @DeleteMapping("/blocks/{blockedUserId}") // /api/chats/blocks/{blockedUserId} 또는 /api/chat-blocks/{blockedUserId}
  public ResponseEntity<Void> unblockUser(
      @PathVariable Long blockedUserId,
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to unblock user {}.", blockedUserId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Request to unblock user {} by user {}", blockedUserId, currentUser.getId());
    try {
      chatBlockService.unblockUser(currentUser.getId(), blockedUserId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.error("Failed to unblock user: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error unblocking user {}", blockedUserId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 현재 로그인한 사용자가 차단한 모든 사용자 목록을 조회합니다.
   *
   * @param currentUser 현재 로그인한 사용자 정보
   * @return 차단 목록 응답
   */
  @GetMapping("/blocks") // /api/chats/blocks 또는 /api/chat-blocks
  public ResponseEntity<List<ChatBlockResponseDTO>> getBlockedUsers(
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      log.warn("Unauthorized attempt to get blocked users list.");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    log.info("Request to get blocked users list for user {}", currentUser.getId());
    try {
      List<ChatBlockResponseDTO> blockedUsers = chatBlockService.getBlockedUsers(currentUser.getId());
      return ResponseEntity.ok(blockedUsers);
    } catch (Exception e) {
      log.error("Error getting blocked users for user {}", currentUser.getId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
