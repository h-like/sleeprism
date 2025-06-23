package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "attachment")
public class Attachment extends BaseTimeEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post; // 어떤 게시글에 속하는지

  @Column(nullable = false, length = 255)
  private String originalFileName; // 원본 파일명

  @Column(nullable = false, length = 255)
  private String storedFileName; // 서버에 저장된 파일명 (UUID 등)

  @Column(nullable = false, length = 255)
  private String fileUrl; // 파일에 접근할 수 있는 URL (S3, CDN 등)

  @Column(length = 50)
  private String fileType; // 파일 종류 (image/jpeg, video/mp4, application/pdf 등 MIME 타입)

  private Long fileSize; // 파일 크기 (바이트)

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private AttachmentType attachmentType; // 이미지, 동영상, 일반 파일 구분

  @Builder
  public Attachment(Post post, String originalFileName, String storedFileName, String fileUrl, String fileType, Long fileSize, AttachmentType attachmentType) {
    this.post = post;
    this.originalFileName = originalFileName;
    this.storedFileName = storedFileName;
    this.fileUrl = fileUrl;
    this.fileType = fileType;
    this.fileSize = fileSize;
    this.attachmentType = attachmentType;
    // Builder에서 Post를 설정하면, Post 엔티티의 addAttachment를 호출하여 양방향 관계를 설정하도록 강제할 수 있습니다.
    // 즉, Attachment 생성 시점에 바로 Post와 연결하는 것이 아니라, Post에서 Attachment를 추가하는 방식으로 유도합니다.
    // 이 방식이 더 안전하고 일반적입니다. (아래 setPost 메소드와 연결)
  }

  // 연관관계 편의 메서드 (양방향 관계 시 필요).
  // 이 메서드는 외부에서 직접 호출하기보다 Post 엔티티의 addAttachment 내부에서 호출되도록 유도하는 것이 좋습니다.
  // 따라서 접근 제어자를 protected 또는 private으로 설정하는 것을 고려할 수 있습니다.
  // Post에서 addAttachment(attachment) 호출 -> attachment.setPost(this) 호출
//  @Setter(AccessLevel.PROTECTED) // 또는 PRIVATE

  public void setPost(Post post) {
    this.post = post;
    // 양방향 관계 시 무한 루프 방지 및 일관성 유지를 위해 if 조건문 사용
    // 이 로직은 Post 엔티티의 addAttachment 메소드 내에서 처리되는 것이 더 자연스럽습니다.
    // Post의 addAttachment가 호출되면, 이미 Post의 attachments 리스트에 이 Attachment가 추가되어 있을 것이기 때문입니다.
    // 따라서 이 부분은 Post.addAttachment()의 역할을 보조하는 것으로 생각하면 됩니다.
    // 만약 Post 엔티티의 addAttachment() 메소드가 이미 List에 추가하고 setPost()를 호출한다면 이 조건은 불필요할 수 있습니다.
    // 하지만 안전하게 양방향 연결을 보장하려면 아래와 같이 확인하는 것이 좋습니다.
    if (post != null && !post.getAttachments().contains(this)) {
      post.getAttachments().add(this);
    }
  }
}