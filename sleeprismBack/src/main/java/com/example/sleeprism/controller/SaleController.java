package com.example.sleeprism.controller;

import com.example.sleeprism.dto.OwnedPostResponseDTO;
import com.example.sleeprism.dto.SaleRequestCreateRequestDTO;
import com.example.sleeprism.dto.SaleRequestResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sale-requests")
@RequiredArgsConstructor
public class SaleController {

  private final SaleService saleService;

  // UserDetails에서 userId를 추출하는 헬퍼 메서드
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalStateException("AuthenticationPrincipal is not of type User.");
  }

  /**
   * 구매자가 특정 게시글에 대한 판매 요청을 생성합니다.
   *
   * @param requestDto 판매 요청 생성 DTO (postId, proposedPrice 포함)
   * @param userDetails 현재 로그인한 구매자 정보
   * @return 생성된 판매 요청 정보 DTO
   */
  @PostMapping
  public ResponseEntity<SaleRequestResponseDTO> createSaleRequest(
      @Valid @RequestBody SaleRequestCreateRequestDTO requestDto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long buyerId = extractUserIdFromUserDetails(userDetails);
      SaleRequestResponseDTO responseDTO = saleService.createSaleRequest(buyerId, requestDto);
      return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404 Not Found
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(null); // 400 Bad Request (자기 게시물에 요청 등)
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 409 Conflict (이미 판매 완료 또는 중복 요청)
    }
  }

  /**
   * 원본 작성자(판매자)에게 들어온 모든 판매 요청을 조회합니다.
   *
   * @param userDetails 현재 로그인한 판매자 정보
   * @return 판매 요청 목록 DTO
   */
  @GetMapping("/for-seller")
  public ResponseEntity<List<SaleRequestResponseDTO>> getSaleRequestsForSeller(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long sellerId = extractUserIdFromUserDetails(userDetails);
    List<SaleRequestResponseDTO> requests = saleService.getSaleRequestsForSeller(sellerId);
    return ResponseEntity.ok(requests);
  }

  /**
   * 구매자가 자신이 보낸 모든 판매 요청을 조회합니다.
   *
   * @param userDetails 현재 로그인한 구매자 정보
   * @return 판매 요청 목록 DTO
   */
  @GetMapping("/for-buyer")
  public ResponseEntity<List<SaleRequestResponseDTO>> getSaleRequestsForBuyer(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long buyerId = extractUserIdFromUserDetails(userDetails);
    List<SaleRequestResponseDTO> requests = saleService.getSaleRequestsForBuyer(buyerId);
    return ResponseEntity.ok(requests);
  }

  /**
   * 판매자가 판매 요청을 수락합니다.
   *
   * @param requestId 판매 요청 ID
   * @param userDetails 현재 로그인한 판매자 정보
   * @return 수락된 판매 요청 정보 DTO
   */
  @PostMapping("/{requestId}/accept")
  public ResponseEntity<SaleRequestResponseDTO> acceptSaleRequest(
      @PathVariable Long requestId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long sellerId = extractUserIdFromUserDetails(userDetails);
      SaleRequestResponseDTO responseDTO = saleService.acceptSaleRequest(requestId, sellerId);
      return ResponseEntity.ok(responseDTO);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 Forbidden (권한 없음)
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400 Bad Request (이미 처리된 요청 등)
    }
  }

  /**
   * 판매자가 판매 요청을 거절합니다.
   *
   * @param requestId 판매 요청 ID
   * @param userDetails 현재 로그인한 판매자 정보
   * @return 거절된 판매 요청 정보 DTO
   */
  @PostMapping("/{requestId}/reject")
  public ResponseEntity<SaleRequestResponseDTO> rejectSaleRequest(
      @PathVariable Long requestId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long sellerId = extractUserIdFromUserDetails(userDetails);
      SaleRequestResponseDTO responseDTO = saleService.rejectSaleRequest(requestId, sellerId);
      return ResponseEntity.ok(responseDTO);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 Forbidden (권한 없음)
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400 Bad Request
    }
  }

  /**
   * 구매자가 자신이 보낸 PENDING 상태의 판매 요청을 취소합니다.
   *
   * @param requestId 판매 요청 ID
   * @param userDetails 현재 로그인한 구매자 정보
   * @return 취소된 판매 요청 정보 DTO
   */
  @PostMapping("/{requestId}/cancel")
  public ResponseEntity<SaleRequestResponseDTO> cancelSaleRequest(
      @PathVariable Long requestId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long buyerId = extractUserIdFromUserDetails(userDetails);
      SaleRequestResponseDTO responseDTO = saleService.cancelSaleRequest(requestId, buyerId);
      return ResponseEntity.ok(responseDTO);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 Forbidden (권한 없음)
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400 Bad Request
    }
  }

  /**
   * 특정 사용자가 소유한 게시글 목록을 조회합니다. (마이페이지 등에서 사용)
   *
   * @param userId 조회할 사용자 ID (경로 변수)
   * @return 소유한 게시글 목록 DTO
   */
  @GetMapping("/users/{userId}/owned-posts")
  public ResponseEntity<List<OwnedPostResponseDTO>> getOwnedPosts(@PathVariable Long userId) {
    try {
      List<OwnedPostResponseDTO> ownedPosts = saleService.getOwnedPosts(userId);
      return ResponseEntity.ok(ownedPosts);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    }
  }
}
