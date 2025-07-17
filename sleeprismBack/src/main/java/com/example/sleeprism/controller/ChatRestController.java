package com.example.sleeprism.controller;

import com.example.sleeprism.dto.ChatRoomCreateRequestDTO;
import com.example.sleeprism.dto.ChatRoomResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.ChatRoomService;
import com.example.sleeprism.service.LocalStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
// ▼▼▼ 요청하신 대로 /api/chat -> /api/chats 로 수정합니다. ▼▼▼
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatRestController {

  private final ChatRoomService chatRoomService;
  private final LocalStorageService fileStorageService;

  @GetMapping("/rooms")
  public ResponseEntity<List<ChatRoomResponseDTO>> getUserChatRooms(
      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      return ResponseEntity.status(401).build();
    }
    List<ChatRoomResponseDTO> chatRooms = chatRoomService.getUserChatRooms(currentUser.getId());
    return ResponseEntity.ok(chatRooms);
  }

  @PostMapping("/files/upload")
  public ResponseEntity<?> uploadChatFile(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증이 필요합니다."));
    }
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("message", "업로드할 파일이 비어있습니다."));
    }

    try {
      // 저장할 하위 디렉토리를 "chat-image"로 설정합니다.
      String fileUrl = fileStorageService.uploadFile(file, "chat-image");
      log.info("Chat file uploaded by user {}. URL: {}", currentUser.getEmail(), fileUrl);

      return ResponseEntity.ok(Map.of("fileUrl", fileUrl));

    } catch (IOException e) {
      log.error("Chat file upload failed for user {}", currentUser.getEmail(), e);
      // 클라이언트가 오류 메시지를 쉽게 파싱할 수 있도록 JSON 형태로 반환합니다.
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "파일 업로드 중 서버 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  @PostMapping("/single")
  public ResponseEntity<?> createSingleChatRoom(
      @RequestBody ChatRoomCreateRequestDTO request,
      @AuthenticationPrincipal User currentUser) {

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증이 필요합니다."));
    }

    // 요청 유효성 검사: 1:1 채팅이므로 참가자 ID는 반드시 1개여야 합니다.
    if (request.getParticipantUserIds() == null || request.getParticipantUserIds().size() != 1) {
      return ResponseEntity.badRequest().body(Map.of("message", "1:1 채팅을 시작할 상대방 정보가 올바르지 않습니다."));
    }

    Long otherUserId = request.getParticipantUserIds().get(0);
    log.info("User {} is requesting a single chat with user {}", currentUser.getId(), otherUserId);

    try {
      // 이미 만들어진 완벽한 서비스 로직을 호출합니다.
      ChatRoomResponseDTO chatRoom = chatRoomService.createOrGetSingleChatRoom(
          currentUser.getId(),
          otherUserId
      );
      return ResponseEntity.ok(chatRoom);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to create single chat room: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

}
