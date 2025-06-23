package com.example.sleeprism.dto;

import com.example.sleeprism.entity.Transaction;
import com.example.sleeprism.entity.TransactionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 거래 응답을 위한 데이터 전송 객체 (DTO)입니다.
 * 거래 정보와 관련된 게시글, 판매자, 구매자 정보를 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransactionResponseDTO {
  private Long id;
  private Long saleRequestId; // 이 거래를 유발한 SaleRequest ID
  private Long postId;
  private String postTitle; // 거래된 게시글의 제목
  private Long sellerId;
  private String sellerNickname; // 판매자 닉네임
  private Long buyerId;
  private String buyerNickname; // 구매자 닉네임
  private Integer amount; // 거래 금액
  private TransactionStatus status;
  private LocalDateTime transactionDate;
  private String externalTransactionId; // 외부 결제 시스템 거래 ID

  public TransactionResponseDTO(Transaction transaction) {
    this.id = transaction.getId();
    this.saleRequestId = transaction.getSaleRequest().getId();
    this.postId = transaction.getPost().getId();
    this.postTitle = transaction.getPost().getTitle();
    this.sellerId = transaction.getSeller().getId();
    this.sellerNickname = transaction.getSeller().getNickname();
    this.buyerId = transaction.getBuyer().getId();
    this.buyerNickname = transaction.getBuyer().getNickname();
    this.amount = transaction.getAmount();
    this.status = transaction.getStatus();
    this.transactionDate = transaction.getTransactionDate();
    this.externalTransactionId = transaction.getExternalTransactionId();
  }

}
