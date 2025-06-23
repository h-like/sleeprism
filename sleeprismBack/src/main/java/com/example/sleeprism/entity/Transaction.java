package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "transaction")
public class Transaction extends BaseTimeEntity{
  @Id // <-- 이 어노테이션이 있어야 합니다.
  @GeneratedValue(strategy = GenerationType.IDENTITY) // <-- ID 자동 생성을 위해 이 어노테이션도 있어야 합니다.
  @Column(name = "transaction_id") // 컬럼 이름 명시 (선택적이지만 명확하게)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_request_id", unique = true, nullable = false) // SaleRequest와 1:1 매핑
  private SaleRequest saleRequest; // 어떤 판매 요청에 의해 발생한 거래인지

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post; // 거래된 게시글

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "seller_id", nullable = false)
  private User seller; // 판매자 (원본 작성자)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id", nullable = false)
  private User buyer; // 구매자

  @Column(nullable = false)
  private Integer amount; // 거래 금액

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransactionStatus status; // 거래 상태 (PENDING, COMPLETED, FAILED, REFUNDED)

  @Column(name = "transaction_date", nullable = false)
  private LocalDateTime transactionDate; // 거래 발생 시간

  // 에스크로 시스템의 실제 거래 ID (PG사 연동 시 필수)
  @Column(name = "external_transaction_id", unique = true, length = 255)
  private String externalTransactionId;

  @Builder
  public Transaction(SaleRequest saleRequest, Post post, User seller, User buyer, Integer amount, String externalTransactionId) {
    this.saleRequest = saleRequest;
    this.post = post;
    this.seller = seller;
    this.buyer = buyer;
    this.amount = amount;
    this.status = TransactionStatus.PENDING; // 초기 상태
    this.transactionDate = LocalDateTime.now();
    this.externalTransactionId = externalTransactionId;
  }

  // 거래 상태 변경 메서드
  public void complete() {
    if (this.status != TransactionStatus.PENDING) {
      throw new IllegalStateException("이미 완료 또는 실패한 거래입니다.");
    }
    this.status = TransactionStatus.COMPLETED;
  }

  public void fail() {
    if (this.status != TransactionStatus.PENDING) {
      throw new IllegalStateException("이미 완료 또는 실패한 거래입니다.");
    }
    this.status = TransactionStatus.FAILED;
  }

  public void refund() {
    if (this.status != TransactionStatus.COMPLETED && this.status != TransactionStatus.PENDING) { // 완료 전이나 완료 후 환불 가능
      throw new IllegalStateException("환불할 수 없는 상태의 거래입니다.");
    }
    this.status = TransactionStatus.REFUNDED;
  }
}
