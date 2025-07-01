package com.example.sleeprism.controller;

import com.example.sleeprism.dto.CommentCreateRequestDTO;
import com.example.sleeprism.dto.CommentResponseDTO;
import com.example.sleeprism.dto.CommentUpdateRequestDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.CommentService;
import com.example.sleeprism.service.FileStorageService;
import io.jsonwebtoken.io.IOException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // 첨부 파일 업로드용

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;
  private final FileStorageService fileStorageService;

  // UserDetails에서 userId를 추출하는 헬퍼 메서드 (중복 코드를 줄이기 위해 별도의 유틸리티 클래스로 분리 권장)
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalStateException("AuthenticationPrincipal is not of type User or User ID cannot be extracted.");
  }

  /**
   * 새로운 댓글을 생성합니다. (최상위 댓글 또는 대댓글, 첨부 파일 포함 가능)
   *
   * @param requestDto 댓글 생성 요청 DTO (postId, parentCommentId, content)
   * @param attachmentFile 댓글에 첨부할 MultipartFile (선택 사항)
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 생성된 댓글 정보 DTO
   */
  @PostMapping(consumes = {"multipart/form-data"}) // <-- consumes 타입 변경
  public ResponseEntity<CommentResponseDTO> createComment(
      @RequestPart("requestDto") @Valid CommentCreateRequestDTO requestDto, // <-- @RequestPart로 변경
      @RequestPart(value = "attachmentFile", required = false) MultipartFile attachmentFile, // <-- 파일 파트 추가
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);

      String uploadedAttachmentUrl = null;
      String uploadedAttachmentType = null;

      // 파일 업로드가 있는 경우 LocalStorageService의 전용 메서드를 호출
      if (attachmentFile != null && !attachmentFile.isEmpty()) {
        uploadedAttachmentUrl = fileStorageService.uploadCommentAttachment(attachmentFile); // <-- 이 메서드 사용
        uploadedAttachmentType = attachmentFile.getContentType() != null && attachmentFile.getContentType().startsWith("image/") ? "IMAGE" : "FILE"; // 간단한 타입 설정

        log.info("댓글 첨부 파일 업로드 완료. URL: {}", uploadedAttachmentUrl);
      }

      // DTO에 업로드된 파일 정보 설정 (service로 전달하기 위함)
      requestDto.setAttachmentUrl(uploadedAttachmentUrl);
      requestDto.setAttachmentType(uploadedAttachmentType);

      CommentResponseDTO responseDTO = commentService.createComment(requestDto, userId);
      log.info("Comment created successfully. Comment ID: {}", responseDTO.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    } catch (EntityNotFoundException e) {
      log.error("Error creating comment: Parent entity not found. {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found (게시글 또는 부모 댓글 없음)
    } catch (IllegalArgumentException e) {
      log.error("Error creating comment: Invalid argument. {}", e.getMessage());
      return ResponseEntity.badRequest().body(null); // 400 Bad Request (2단계 대댓글 시도 등)
    } catch (IOException e) { // 파일 업로드 시 발생할 수 있는 IOException 처리
      log.error("Failed to upload comment attachment: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    } catch (Exception e) {
      log.error("An unexpected error occurred while creating comment: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 특정 게시글의 모든 댓글을 조회합니다. (최상위 댓글 및 대댓글 포함)
   *
   * @param postId 게시글 ID
   * @return 댓글 목록 DTO
   */
  @GetMapping("/post/{postId}")
  public ResponseEntity<List<CommentResponseDTO>> getCommentsByPostId(@PathVariable Long postId) {
    try {
      List<CommentResponseDTO> comments = commentService.getCommentsByPostId(postId);
      return ResponseEntity.ok(comments);
    } catch (EntityNotFoundException e) {
      log.error("Error getting comments: Post not found with ID {}. {}", postId, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (Exception e) {
      log.error("An unexpected error occurred while getting comments for post {}: {}", postId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 특정 댓글을 수정합니다.
   *
   * @param commentId 수정할 댓글 ID
   * @param requestDto 댓글 수정 요청 DTO
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 수정된 댓글 정보 DTO
   */
  @PutMapping("/{commentId}")
  public ResponseEntity<CommentResponseDTO> updateComment(
      @PathVariable Long commentId,
      @Valid @RequestBody CommentUpdateRequestDTO requestDto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      CommentResponseDTO responseDTO = commentService.updateComment(commentId, requestDto, userId);
      log.info("Comment updated successfully. Comment ID: {}", responseDTO.getId());
      return ResponseEntity.ok(responseDTO);
    } catch (EntityNotFoundException e) {
      log.error("Error updating comment: Comment not found with ID {}. {}", commentId, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      log.error("Error updating comment: Permission denied or invalid argument. {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 Forbidden (권한 없음)
    } catch (Exception e) {
      log.error("An unexpected error occurred while updating comment {}: {}", commentId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 특정 댓글을 삭제합니다. (소프트 삭제)
   *
   * @param commentId 삭제할 댓글 ID
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 응답 없음 (204 No Content)
   */
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable Long commentId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      commentService.deleteComment(commentId, userId);
      log.info("Comment deleted successfully. Comment ID: {}", commentId);
      return ResponseEntity.noContent().build(); // 204 No Content
    } catch (EntityNotFoundException e) {
      log.error("Error deleting comment: Comment not found with ID {}. {}", commentId, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      log.error("Error deleting comment: Permission denied. {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
    } catch (Exception e) {
      log.error("An unexpected error occurred while deleting comment {}: {}", commentId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  // (옵션) 댓글에 첨부 파일을 직접 업로드하는 엔드포인트
  // 이 경우 CommentCreateRequestDTO에 attachmentUrl과 attachmentType을 직접 받지 않고,
  // 이 엔드포인트에서 파일 업로드 후 반환된 URL을 댓글 생성 요청 시 포함시키거나,
  // 별도의 DTO로 처리하는 방식 등을 고려해야 합니다.
  // 현재는 CommentCreateRequestDTO에 URL이 직접 포함되는 것을 전제합니다.
    /*
    @PostMapping("/{commentId}/upload-attachment")
    public ResponseEntity<Map<String, String>> uploadCommentAttachment(
            @PathVariable Long commentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            Long userId = extractUserIdFromUserDetails(userDetails);
            // AttachmentService를 활용하여 파일 업로드 후, 해당 URL을 Comment 엔티티에 업데이트하는 로직 필요
            // 이 로직은 CommentService.updateComment 메서드와 통합되거나,
            // 별도로 댓글 생성/수정 전에 파일 업로드를 먼저 수행하는 흐름으로 가야 합니다.
            String fileUrl = fileStorageService.uploadFile(file, "comment-attachments/" + commentId);
            // 여기서는 임시로 URL만 반환. 실제로는 Comment 엔티티와 연결하고 CommentResponseDTO를 반환
            return ResponseEntity.ok(Map.of("url", "/api/files/" + fileUrl)); // /api/files/downloadFile/ 형태로 변경
        } catch (IOException e) {
            log.error("Failed to upload comment attachment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "댓글 첨부 파일 업로드에 실패했습니다."));
        }
    }
    */
}
