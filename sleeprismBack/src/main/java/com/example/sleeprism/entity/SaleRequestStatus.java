package com.example.sleeprism.entity;

public enum SaleRequestStatus {
  PENDING,    // 대기 중 (구매자가 요청하고 판매자가 응답하기 전)
  ACCEPTED,   // 수락됨 (판매자가 요청을 수락함)
  REJECTED,   // 거절됨 (판매자가 요청을 거절함)
  CANCELED    // 취소됨 (구매자가 요청을 취소함)
}
