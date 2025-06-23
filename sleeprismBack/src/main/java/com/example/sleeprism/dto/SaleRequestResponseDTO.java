package com.example.sleeprism.dto;

import com.example.sleeprism.entity.SaleRequest;
import com.example.sleeprism.entity.SaleRequestStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 판매 요청 응답을 위한 데이터 전송 객체 (DTO)입니다.
 * 판매 요청 정보와 관련된 사용자, 게시글 정보를 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SaleRequestResponseDTO {
  private Long id;
  private Long postId;
  private String postTitle; // 요청된 게시글의 제목
  private Long buyerId;
  private String buyerNickname; // 요청을 보낸 구매자 닉네임
  private Long sellerId; // 게시글의 원본 작성자 (판매자) ID
  private String sellerNickname; // 게시글의 원본 작성자 (판매자) 닉네임
  private Integer proposedPrice;
  private SaleRequestStatus status;
  private LocalDateTime requestedAt;
  private LocalDateTime respondedAt;

  public SaleRequestResponseDTO(SaleRequest saleRequest) {
    this.id = saleRequest.getId();
    this.postId = saleRequest.getPost().getId();
    this.postTitle = saleRequest.getPost().getTitle();
    this.buyerId = saleRequest.getBuyer().getId();
    this.buyerNickname = saleRequest.getBuyer().getNickname(); // User 엔티티에 getNickname()이 있다고 가정
    this.sellerId = saleRequest.getPost().getOriginalAuthor().getId();
    this.sellerNickname = saleRequest.getPost().getOriginalAuthor().getNickname(); // User 엔티티에 getNickname()이 있다고 가정
    this.proposedPrice = saleRequest.getProposedPrice();
    this.status = saleRequest.getStatus();
    this.requestedAt = saleRequest.getCreatedAt(); // BaseTimeEntity 상속으로 createdAt 사용
    this.respondedAt = saleRequest.getRespondedAt();
  }

}
