package com.example.sleeprism.service;

import com.example.sleeprism.dto.TransactionResponseDTO;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.TransactionRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;

  /**
   * 특정 사용자의 모든 거래 내역을 조회합니다. (판매 또는 구매)
   * @param userId 사용자 ID
   * @return 거래 내역 목록 DTO
   */
  public List<TransactionResponseDTO> getMyTransactions(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // 해당 사용자가 판매자 또는 구매자인 모든 거래를 조회
    List<com.example.sleeprism.entity.Transaction> transactions = transactionRepository.findByBuyerOrderByTransactionDateDesc(user);
    transactions.addAll(transactionRepository.findBySellerOrderByTransactionDateDesc(user)); // 판매자로서의 거래도 추가

    // 중복 제거 및 시간순 정렬 (필요하다면)
    // 현재는 Repository 메서드가 Desc로 정렬하므로, 두 리스트를 합친 후 다시 정렬하는 로직이 필요할 수 있습니다.
    // 예를 들어, Stream API의 distinct()와 sorted()를 사용
    return transactions.stream()
        .distinct() // 중복 제거 (동일한 트랜잭션이 판매자와 구매자 양쪽에서 조회될 수 있으므로)
        .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate())) // 최신순 정렬
        .map(TransactionResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 특정 거래 ID로 거래 내역을 조회합니다.
   * @param transactionId 거래 ID
   * @param userId 조회 요청 사용자 ID (권한 검증용)
   * @return 거래 내역 DTO
   */
  public TransactionResponseDTO getTransactionById(Long transactionId, Long userId) {
    com.example.sleeprism.entity.Transaction transaction = transactionRepository.findById(transactionId)
        .orElseThrow(() -> new EntityNotFoundException("Transaction not found with ID: " + transactionId));

    // 해당 거래의 판매자 또는 구매자만 조회 가능하도록 검증
    if (!transaction.getBuyer().getId().equals(userId) && !transaction.getSeller().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to view this transaction.");
    }

    return new TransactionResponseDTO(transaction);
  }
}