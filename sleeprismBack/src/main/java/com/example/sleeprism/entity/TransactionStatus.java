package com.example.sleeprism.entity;

public enum TransactionStatus {
  PENDING,    // 대기 중 (에스크로 결제 대기, 판매자 수락 후)
  COMPLETED,  // 완료됨 (결제 및 소유권 이전 완료)
  FAILED,     // 실패함 (결제 실패, 기타 오류)
  REFUNDED    // 환불됨 (거래 취소 등으로 인해 환불 발생)
}
