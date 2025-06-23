package com.example.sleeprism.dto;

import com.example.sleeprism.entity.Attachment;
import com.example.sleeprism.entity.AttachmentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AttachmentResponseDTO {
  private Long id;
  private Long postId;
  private String originalFileName;
  private String fileUrl;
  private String fileType;
  private Long fileSize;
  private AttachmentType attachmentType;
  private LocalDateTime createdAt;

  public AttachmentResponseDTO(Attachment attachment) {
    this.id = attachment.getId();
    this.postId = attachment.getPost().getId();
    this.originalFileName = attachment.getOriginalFileName();
    this.fileUrl = attachment.getFileUrl();
    this.fileType = attachment.getFileType();
    this.fileSize = attachment.getFileSize();
    this.attachmentType = attachment.getAttachmentType();
    this.createdAt = attachment.getCreatedAt();
  }
}
