package com.example.sleeprism.entity;

/**
 * 알림의 종류를 정의하는 Enum 클래스입니다.
 */
public enum NotificationType {
  COMMENT,            // 게시글에 새로운 댓글이 달렸을 때
  REPLY_COMMENT,      // 내 댓글에 대댓글이 달렸을 때
  SALE_REQUEST,       // 게시글에 판매 요청이 들어왔을 때 (게시글 원본 작성자/현재 소유자에게)
  SALE_ACCEPTED,      // 내 판매 요청이 수락되었을 때 (구매 요청자에게)
  SALE_REJECTED,      // 내 판매 요청이 거절되었을 때 (구매 요청자에게)
  POST_PURCHASED,     // 내 게시물이 판매 완료되었을 때 (원본 작성자/이전 소유자에게)
  POST_LIKE,          // 내 게시물에 좋아요가 눌렸을 때
  CHAT_MESSAGE,       // 새로운 채팅 메시지가 도착했을 때 (추가)
  MESSAGE,            // 새로운 쪽지가 도착했을 때 등
}
