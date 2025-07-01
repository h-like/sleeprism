// src/main/java/com/example/sleeprism/dto/PostResponseDTO.java
package com.example.sleeprism.dto;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.PostCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 응답을 위한 데이터 전송 객체 (DTO)입니다.
 * 게시글 정보와 작성자 닉네임을 포함합니다.
 */
@Getter // Lombok을 사용하여 모든 필드에 대한 getter 메서드를 자동으로 생성합니다.
@Setter // Lombok을 사용하여 모든 필드에 대한 setter 메서드를 자동으로 생성합니다.
@NoArgsConstructor // Lombok을 사용하여 인자 없는 생성자를 자동으로 생성합니다.
public class PostResponseDTO {
  private Long id;
  private String title;
  private String content;
  private PostCategory category;
  private Long viewCount;
  private boolean isDeleted;
  private String authorNickname; // 작성자 닉네임 (User 엔티티의 username 필드와 매핑)
  private Long originalAuthorId;
  private String authorProfileImageUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean isSellable;
  private boolean isSold;

  private int likeCount;
  private int commentCount;
  private int bookmarkCount;

  /**
   * Post 엔티티를 기반으로 PostResponseDTO 객체를 생성하는 생성자입니다.
   *
   * @param post 변환할 Post 엔티티 객체
   */
  public PostResponseDTO(Post post) {
    this.id = post.getId();
    this.title = post.getTitle();
    this.content = post.getContent();
    this.category = post.getCategory();
    this.viewCount = post.getViewCount();
    this.isDeleted = post.isDeleted();
    this.authorNickname = post.getOriginalAuthor().getUsername();
    this.originalAuthorId = post.getOriginalAuthor().getId();
    this.authorProfileImageUrl = post.getOriginalAuthor().getProfileImageUrl();
    this.createdAt = post.getCreatedAt();
    this.updatedAt = post.getUpdatedAt();
    this.isSellable = post.isSellable();
    this.isSold = post.isSold();

    this.likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
    this.commentCount = post.getComments() != null ? post.getComments().size() : 0;
  }
}
