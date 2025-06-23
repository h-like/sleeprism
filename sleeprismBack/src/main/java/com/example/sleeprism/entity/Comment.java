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
@Table(name = "comments")
public class Comment extends BaseTimeEntity{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Lob
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id") // 대댓글을 위한 부모 댓글 ID
  private Comment parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> children = new ArrayList<>(); // 자식 댓글들 (대댓글)

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted = false;

  // **추가: 댓글에 첨부되는 단일 이미지/GIF 관련 필드**
  @Column(name = "attachment_url", length = 255)
  private String attachmentUrl; // 첨부 파일의 URL (S3, CDN 등)

  @Column(name = "attachment_type", length = 50)
  private String attachmentType; // 첨부 파일의 MIME 타입 (image/jpeg, image/gif 등)

  // 연관관계 편의 메소드
  public void setPost(Post post) {
    this.post = post;
    if (!post.getComments().contains(this)) {
      post.getComments().add(this);
    }
  }

  public void setUser(User user) {
    this.user = user;
    if (!user.getComments().contains(this)) {
      user.getComments().add(this);
    }
  }

  public void setParent(Comment parent) {
    this.parent = parent;
    if (parent != null && !parent.getChildren().contains(this)) {
      parent.getChildren().add(this);
    }
  }

  @Builder
  public Comment(Post post, User user, String content, Comment parent, String attachmentUrl, String attachmentType) {
    this.post = post;
    this.user = user;
    this.content = content;
    this.parent = parent;
    this.isDeleted = false;
    this.attachmentUrl = attachmentUrl; // 추가
    this.attachmentType = attachmentType; // 추가
  }

  public void delete() {
    this.isDeleted = true;
  }

  public void update(String content, String attachmentUrl, String attachmentType) {
    this.content = content;
    this.attachmentUrl = attachmentUrl; // 추가
    this.attachmentType = attachmentType; // 추가
  }

  // **추가: 첨부 파일 제거 메서드 (옵션)**
  public void removeAttachment() {
    this.attachmentUrl = null;
    this.attachmentType = null;
  }
}
