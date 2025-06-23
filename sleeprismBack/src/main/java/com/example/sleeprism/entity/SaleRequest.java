package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sale_request")
public class SaleRequest extends BaseTimeEntity{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "sale_request_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post; // 어떤 게시글에 대한 요청인지

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id", nullable = false)
  private User buyer; // 구매를 요청한 사용자

  @Column(nullable = false)
  private Integer proposedPrice; // 구매자가 제시한 가격

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SaleRequestStatus status; // 요청 상태 (PENDING, ACCEPTED, REJECTED, CANCELED)

  @Column(name = "responded_at")
  private LocalDateTime respondedAt; // 요청에 응답한 시간

  // 에스크로 거래 ID (외부 결제 시스템과 연동 시 필요)
  @Column(name = "escrow_transaction_id", unique = true, length = 255)
  private String escrowTransactionId;

  @Builder
  public SaleRequest(Post post, User buyer, Integer proposedPrice, String escrowTransactionId) {
    this.post = post;
    this.buyer = buyer;
    this.proposedPrice = proposedPrice;
    this.status = SaleRequestStatus.PENDING; // 초기 상태는 '대기 중'
    this.escrowTransactionId = escrowTransactionId;
  }

  // 요청 상태 변경 메서드
  public void accept() {
    if (this.status != SaleRequestStatus.PENDING) {
      throw new IllegalStateException("이미 처리된 판매 요청입니다.");
    }
    this.status = SaleRequestStatus.ACCEPTED;
    this.respondedAt = LocalDateTime.now();
  }

  public void reject() {
    if (this.status != SaleRequestStatus.PENDING) {
      throw new IllegalStateException("이미 처리된 판매 요청입니다.");
    }
    this.status = SaleRequestStatus.REJECTED;
    this.respondedAt = LocalDateTime.now();
  }

  public void cancel() {
    if (this.status != SaleRequestStatus.PENDING) {
      throw new IllegalStateException("이미 처리된 판매 요청입니다.");
    }
    this.status = SaleRequestStatus.CANCELED;
    this.respondedAt = LocalDateTime.now();
  }
}
