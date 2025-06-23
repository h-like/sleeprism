package com.example.sleeprism.dto;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.PostCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자가 소유한 게시글 응답을 위한 데이터 전송 객체 (DTO)입니다.
 * 원본 작성자와 현재 소유자 정보를 모두 포함할 수 있습니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class OwnedPostResponseDTO {
  private Long id;
  private String title;
  private String content;
  private PostCategory category;
  private int viewCount;
  private String originalAuthorNickname; // 원본 작성자 닉네임
  private Long originalAuthorId;
  private String currentOwnerNickname; // 현재 소유자 닉네임 (나 자신)
  private Long currentOwnerId;
  private Integer soldPrice; // 이 게시글을 구매했을 때의 가격
  private LocalDateTime createdAt; // 게시글 생성일
  private LocalDateTime acquiredAt; // 소유권을 획득한 날짜 (옵션, 거래일과 유사)

  public OwnedPostResponseDTO(Post post) {
    this.id = post.getId();
    this.title = post.getTitle();
    this.content = post.getContent();
    this.category = post.getCategory();
    this.viewCount = post.getViewCount();
    this.originalAuthorNickname = post.getOriginalAuthor().getNickname(); // 또는 getUsername()
    this.originalAuthorId = post.getOriginalAuthor().getId();
    this.currentOwnerNickname = post.getCurrentOwner().getNickname(); // 현재 소유자 (나)
    this.currentOwnerId = post.getCurrentOwner().getId();
    this.soldPrice = post.getSoldPrice(); // 판매된 가격
    this.createdAt = post.getCreatedAt();
//     this.acquiredAt = post.getAcquiredAt(); // 만약 Post 엔티티에 소유권 획득 날짜 필드를 추가했다면
    // 현재는 Transaction 엔티티의 transactionDate를 통해 획득일을 유추할 수 있습니다.
  }
}
