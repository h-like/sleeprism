package com.example.sleeprism.controller;

import com.example.sleeprism.dto.AttachmentResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.AttachmentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentService attachmentService;

  // UserDetails에서 userId를 추출하는 헬퍼 메서드
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalStateException("AuthenticationPrincipal is not of type User.");
  }

  /**
   * 특정 게시글에 첨부 파일을 업로드합니다.
   * 파일은 'file'이라는 이름으로 MultipartFile 형태로 전송되어야 합니다.
   *
   * @param postId 게시글 ID
   * @param file 업로드할 파일
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 업로드된 첨부 파일 정보 DTO
   */
  @PostMapping("/upload/{postId}")
  public ResponseEntity<AttachmentResponseDTO> uploadAttachment(
      @PathVariable Long postId,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      // userId는 현재 로그인한 사용자를 식별하기 위함 (여기서는 권한 검증에 사용되지 않지만, 다른 로직에서 필요할 수 있음)
      // Long userId = extractUserIdFromUserDetails(userDetails);
      AttachmentResponseDTO responseDTO = attachmentService.uploadAttachment(postId, file);
      return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(null); // 400 Bad Request
    } catch (IOException e) {
      System.err.println("파일 업로드 중 오류 발생: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 특정 게시글에 속한 모든 첨부 파일을 조회합니다.
   *
   * @param postId 게시글 ID
   * @return 첨부 파일 목록 DTO
   */
  @GetMapping("/post/{postId}")
  public ResponseEntity<List<AttachmentResponseDTO>> getAttachmentsByPostId(@PathVariable Long postId) {
    try {
      List<AttachmentResponseDTO> attachments = attachmentService.getAttachmentsByPostId(postId);
      return ResponseEntity.ok(attachments);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    }
  }

  /**
   * 특정 첨부 파일을 ID로 조회합니다.
   *
   * @param attachmentId 첨부 파일 ID
   * @return 첨부 파일 정보 DTO
   */
  @GetMapping("/{attachmentId}")
  public ResponseEntity<AttachmentResponseDTO> getAttachmentById(@PathVariable Long attachmentId) {
    try {
      AttachmentResponseDTO attachment = attachmentService.getAttachmentById(attachmentId);
      return ResponseEntity.ok(attachment);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    }
  }

  /**
   * 특정 첨부 파일을 삭제합니다.
   *
   * @param attachmentId 삭제할 첨부 파일의 ID
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 응답 없음 (204 No Content)
   */
  @DeleteMapping("/{attachmentId}")
  public ResponseEntity<Void> deleteAttachment(
      @PathVariable Long attachmentId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      attachmentService.deleteAttachment(attachmentId, userId);
      return ResponseEntity.noContent().build(); // 204 No Content
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden (권한 없음)
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // 400 Bad Request (판매된 게시물 등 상태 오류)
    }
  }
}
