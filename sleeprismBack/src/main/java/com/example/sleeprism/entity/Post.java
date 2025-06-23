package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post")
public class Post extends BaseTimeEntity{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "post_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY) // N:1 관계, 지연 로딩이 기본
  @JoinColumn(name = "original_author_id", nullable = false) // FK 컬럼 지정: 원본 작성자
  private User originalAuthor; // 'user' 필드를 'originalAuthor'로 변경하거나 추가

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_owner_id", nullable = false) // FK 컬럼 지정: 현재 소유자
  private User currentOwner; // 현재 소유자 (판매되면 바뀜)

  @Column(nullable = false, length = 255)
  private String title;

  @Lob // 대용량 텍스트를 위한 어노테이션
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private PostCategory category;

  @Column(name = "view_count", nullable = false)
  private int viewCount = 0;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted = false;

  // 추가: 판매 요청을 받을 수 있는지 여부 (기본 false, 구매자가 요청하면 고려)
  @Column(name = "is_sellable", nullable = false)
  private boolean isSellable = true; // 모든 게시물이 판매 요청을 받을 수 있도록 기본값 true로 설정.
  // 필요에 따라 이 필드를 활용하여 판매 불가 게시물 설정 가능.

  // 추가: 포스트가 판매 완료되었는지 여부
  @Column(name = "is_sold", nullable = false)
  private boolean isSold = false;

  // 추가: 판매된 가격 (판매 완료 시 기록)
  @Column(name = "sold_price")
  private Integer soldPrice; // Optional: 판매된 가격을 기록


  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PostLike> likes = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Bookmark> bookmarks = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Attachment> attachments = new ArrayList<>();


  // 연관관계 편의 메소드 (기존 'setUser' 대신 'setOriginalAuthor'와 'setCurrentOwner'로 분리)
  public void setOriginalAuthor(User originalAuthor) {
    this.originalAuthor = originalAuthor;
    if (originalAuthor != null && !originalAuthor.getPosts().contains(this)) {
      originalAuthor.getPosts().add(this);
    }
  }

  public void setCurrentOwner(User currentOwner) {
    this.currentOwner = currentOwner;
    // 필요하다면 User 엔티티에 ownedPosts 같은 목록을 추가하고 여기에 추가 로직 구현
  }


  @Builder
  public Post(User user, String title, String content, PostCategory category) {
    this.originalAuthor = user; // 초기 작성자가 원본 작성자이자 현재 소유자
    this.currentOwner = user;
    this.title = title;
    this.content = content;
    this.category = category;
    this.viewCount = 0;
    this.isDeleted = false;
  }

  // 조회수 증가 메소드
  public void incrementViewCount() {
    this.viewCount++;
  }

  // 삭제 처리 (Soft Delete)
  public void delete() {
    this.isDeleted = true;
  }

  // 수정 메소드
  public void update(String title, String content, PostCategory category) {
    if (this.isSold) throw new IllegalStateException("판매된 게시물은 수정할 수 없습니다.");
    this.title = title;
    this.content = content;
    this.category = category;
  }

  // Attachment 연관관계 편의 메소드 유지
  public void addAttachment(Attachment attachment) {
    this.attachments.add(attachment);
    if (attachment.getPost() != this) {
      attachment.setPost(this);
    }
  }

  // **추가: 포스트 판매 완료 처리 메소드**
  public void markAsSold(User buyer, Integer price) {
    if (this.isSold) {
      throw new IllegalStateException("이미 판매된 게시물입니다.");
    }
    this.isSold = true;
    this.soldPrice = price;
    this.currentOwner = buyer; // 소유자 변경
  }

  // **추가: 포스트 수정/삭제 가능 여부 확인 메소드 (권한 로직)**
  public boolean isModifiableBy(User user) {
    // 원본 작성자이고, 아직 판매되지 않았으며, 삭제되지 않은 경우에만 수정 가능
    return this.originalAuthor.equals(user) && !this.isSold && !this.isDeleted;
  }

  public boolean isDeletableBy(User user) {
    // 원본 작성자이고, 아직 판매되지 않았으며, 삭제되지 않은 경우에만 삭제 가능
    return this.originalAuthor.equals(user) && !this.isSold && !this.isDeleted;
  }
}




