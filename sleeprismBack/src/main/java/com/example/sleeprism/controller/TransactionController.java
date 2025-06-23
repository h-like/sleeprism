package com.example.sleeprism.controller;

import com.example.sleeprism.dto.TransactionResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  // UserDetails에서 userId를 추출하는 헬퍼 메서드
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalStateException("AuthenticationPrincipal is not of type User.");
  }

  /**
   * 현재 로그인한 사용자의 모든 거래 내역을 조회합니다 (판매 및 구매).
   *
   * @param userDetails 현재 로그인한 사용자 정보
   * @return 거래 내역 목록 DTO
   */
  @GetMapping
  public ResponseEntity<List<TransactionResponseDTO>> getMyTransactions(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId = extractUserIdFromUserDetails(userDetails);
    List<TransactionResponseDTO> transactions = transactionService.getMyTransactions(userId);
    return ResponseEntity.ok(transactions);
  }

  /**
   * 특정 거래 ID로 거래 내역을 조회합니다.
   *
   * @param transactionId 거래 ID
   * @param userDetails 현재 로그인한 사용자 정보 (조회 권한 검증용)
   * @return 거래 내역 DTO
   */
  @GetMapping("/{transactionId}")
  public ResponseEntity<TransactionResponseDTO> getTransactionById(
      @PathVariable Long transactionId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    try {
      Long userId = extractUserIdFromUserDetails(userDetails);
      TransactionResponseDTO transaction = transactionService.getTransactionById(transactionId, userId);
      return ResponseEntity.ok(transaction);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden (권한 없음)
    }
  }
}
