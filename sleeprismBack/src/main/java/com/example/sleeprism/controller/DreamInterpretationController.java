package com.example.sleeprism.controller;

import com.example.sleeprism.dto.DreamInterpretationRequestDTO;
import com.example.sleeprism.dto.DreamInterpretationResponseDTO;
import com.example.sleeprism.dto.DreamInterpretationSelectRequestDTO;
import com.example.sleeprism.entity.User; // User 엔티티 임포트
import com.example.sleeprism.service.DreamInterpretationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dream-interpretations")
@RequiredArgsConstructor
public class DreamInterpretationController {

  private final DreamInterpretationService dreamInterpretationService;

  // UserDetails에서 userId를 추출하는 헬퍼 메서드 (중복 코드를 줄이기 위해 별도의 유틸리티 클래스로 분리 권장)
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalStateException("AuthenticationPrincipal is not of type User or User ID cannot be extracted.");
  }

  /**
   * AI를 사용하여 특정 꿈 게시글을 해몽하도록 요청합니다.
   * @param requestDto 해몽할 꿈 게시글 ID를 포함하는 DTO
   * @param userDetails 현재 로그인한 사용자 정보
   * @return AI가 생성한 여러 해몽 옵션이 포함된 DTO
   */
  @PostMapping("/interpret")
  public ResponseEntity<DreamInterpretationResponseDTO> interpretDream(
      @Valid @RequestBody DreamInterpretationRequestDTO requestDto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      DreamInterpretationResponseDTO response = dreamInterpretationService.interpretDream(requestDto, userId);
      if (response.getErrorMessage() != null) {
        // 서비스에서 API 호출 실패 등으로 에러 메시지를 포함하여 반환한 경우
        log.error("Dream interpretation failed with error: {}", response.getErrorMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
      log.info("Dream interpretation requested successfully for post ID: {} by user ID: {}", requestDto.getPostId(), userId);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (EntityNotFoundException e) {
      log.error("Entity not found during dream interpretation: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found (게시글 또는 사용자 없음)
    } catch (IllegalArgumentException e) {
      log.error("Invalid argument for dream interpretation: {}", e.getMessage());
      return ResponseEntity.badRequest().body(null); // 400 Bad Request
    } catch (Exception e) {
      log.error("An unexpected error occurred during dream interpretation: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 사용자가 AI가 제공한 해몽 옵션 중 하나를 최종적으로 선택하고 저장합니다.
   * @param interpretationId 해몽 기록 ID
   * @param requestDto 선택된 옵션 정보 DTO (선택된 인덱스 및 타로 카드 ID 포함)
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 업데이트된 해몽 결과 DTO
   */
  @PostMapping("/{interpretationId}/select")
  public ResponseEntity<DreamInterpretationResponseDTO> selectInterpretationOption(
      @PathVariable Long interpretationId,
      @Valid @RequestBody DreamInterpretationSelectRequestDTO requestDto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      DreamInterpretationResponseDTO response = dreamInterpretationService.selectInterpretationOption(interpretationId, requestDto, userId);
      log.info("Interpretation option selected successfully for interpretation ID: {} by user ID: {}", interpretationId, userId);
      return ResponseEntity.ok(response);
    } catch (EntityNotFoundException e) {
      log.error("Entity not found during interpretation option selection: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      log.error("Invalid argument or permission denied for interpretation option selection: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 Forbidden (권한 없음)
    } catch (Exception e) {
      log.error("An unexpected error occurred during interpretation option selection: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 특정 해몽 기록을 ID로 조회합니다.
   * @param interpretationId 해몽 기록 ID
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 해몽 결과 DTO
   */
  @GetMapping("/{interpretationId}")
  public ResponseEntity<DreamInterpretationResponseDTO> getDreamInterpretationById(
      @PathVariable Long interpretationId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      DreamInterpretationResponseDTO response = dreamInterpretationService.getDreamInterpretationById(interpretationId, userId);
      log.info("Dream interpretation retrieved successfully for ID: {} by user ID: {}", interpretationId, userId);
      return ResponseEntity.ok(response);
    } catch (EntityNotFoundException e) {
      log.error("Dream interpretation not found for ID: {}. {}", interpretationId, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      log.error("Permission denied to view dream interpretation ID: {}. {}", interpretationId, e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
    } catch (Exception e) {
      log.error("An unexpected error occurred while retrieving dream interpretation ID: {}: {}", interpretationId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  /**
   * 특정 사용자가 요청한 모든 해몽 기록을 조회합니다. (마이페이지 등)
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 해몽 결과 목록 DTO
   */
  @GetMapping("/my-interpretations")
  public ResponseEntity<List<DreamInterpretationResponseDTO>> getMyDreamInterpretations(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      List<DreamInterpretationResponseDTO> interpretations = dreamInterpretationService.getMyDreamInterpretations(userId);
      log.info("Retrieved {} dream interpretations for user ID: {}", interpretations.size(), userId);
      return ResponseEntity.ok(interpretations);
    } catch (EntityNotFoundException e) {
      log.error("User not found for retrieving interpretations: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (Exception e) {
      log.error("An unexpected error occurred while retrieving user's interpretations: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }
}
