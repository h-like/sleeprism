package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder; // Lombok Builder 임포트 유지
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder; // SuperBuilder 임포트 추가

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post")
@SuperBuilder // <-- @Builder 대신 @SuperBuilder 사용 (BaseTimeEntity 상속 고려)
public class Post extends BaseTimeEntity { // BaseTimeEntity 상속

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "post_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "original_author_id", nullable = false)
  private User originalAuthor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_owner_id", nullable = false)
  private User currentOwner;

  @Column(nullable = false, length = 255)
  private String title;

  @Lob
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private PostCategory category;

  @Column(name = "view_count", nullable = false)
  @Builder.Default // @Builder.Default 추가
  private Long viewCount = 0L;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default // @Builder.Default 추가
  private boolean isDeleted = false;

  @Column(name = "is_sellable", nullable = false)
  @Builder.Default // @Builder.Default 추가
  private boolean isSellable = true;

  @Column(name = "is_sold", nullable = false)
  @Builder.Default // @Builder.Default 추가
  private boolean isSold = false;

  @Column(name = "sold_price")
  private Integer soldPrice;

  // 좋아요 수
  @Column(name = "like_count", nullable = false)
  @Builder.Default
  private int likeCount = 0;

  //  nullable = false 와 @Builder.Default 명시
  @Column(name = "bookmark_count", nullable = false)
  @Builder.Default
  private int bookmarkCount = 0;


  @Version
  @Builder.Default // @Builder.Default 추가
  private Long version = 0L;

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Comment> comments = new HashSet<>(); // FIX: List 대신 Set 사용

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<PostLike> likes = new HashSet<>(); // FIX: List 대신 Set 사용

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default // @Builder.Default 추가
  private List<Bookmark> bookmarks = new ArrayList<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default // @Builder.Default 추가
  private List<Attachment> attachments = new ArrayList<>();

  // setOriginalAuthor, setCurrentOwner 메서드는 User 엔티티의 컬렉션 관리를 위해 필요했지만,
  // User 엔티티에서 @OneToMany(mappedBy="originalAuthor") 등으로 관계를 관리하므로 Post 측에서는 직접 List를 수정할 필요가 없습니다.
  // User 엔티티가 @OneToMany 컬렉션을 가지고 있으면 Post 저장 시 자동으로 관계가 설정됩니다.
  // 따라서 이 편의 메서드들 내부의 `user.getPosts().add(this)`와 같은 로직은 제거합니다.
  public void setOriginalAuthor(User originalAuthor) {
    this.originalAuthor = originalAuthor;
  }

  public void setCurrentOwner(User currentOwner) {
    this.currentOwner = currentOwner;
  }

  @PrePersist
  public void prePersist() {
    // @Builder.Default가 있으므로 여기서 널 체크 및 초기화 로직은 필요 없습니다.
    // boolean 타입은 primitive type이라 null이 될 수 없으므로, null 체크를 제거합니다.
    if (this.viewCount == null) {
      this.viewCount = 0L;
    }
    if (this.version == null) {
      this.version = 0L;
    }
    // isDeleted, isSellable, isSold는 boolean primitive이므로 null이 될 수 없음.
    // 따라서 이들에 대한 null 체크는 제거합니다.
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

  // 포스트 판매 완료 처리 메소드
  public void markAsSold(User buyer, Integer price) {
    if (this.isSold) {
      throw new IllegalStateException("이미 판매된 게시물입니다.");
    }
    this.isSold = true;
    this.soldPrice = price;
    this.currentOwner = buyer;
  }

  // 포스트 수정/삭제 가능 여부 확인 메소드 (권한 로직)
  public boolean isModifiableBy(User user) {
    return this.originalAuthor.equals(user) && !this.isSold && !this.isDeleted;
  }

  public boolean isDeletableBy(User user) {
    return this.originalAuthor.equals(user) && !this.isSold && !this.isDeleted;
  }

  // 좋아요 수를 증가시키는 메서드
  public void incrementLikeCount() {
    this.likeCount++;
  }

  // 좋아요 수를 감소시키는 메서드
  public void decrementLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  // 북마크 수를 증가시키는 메서드
  public void incrementBookmarkCount() {
    this.likeCount++;
  }

  // 좋아요 수를 감소시키는 메서드
  public void decrementBookmarkCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }
}
