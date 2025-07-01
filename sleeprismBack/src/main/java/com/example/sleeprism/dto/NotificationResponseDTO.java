package com.example.sleeprism.dto;

import com.example.sleeprism.entity.Notification;
import com.example.sleeprism.entity.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDTO {
  private Long id;
  private Long userId; // 알림을 받은 사용자 ID
  private NotificationType type;
  private String message;
  private String targetEntityType;
  private Long targetEntityId;
  private boolean isRead;
  private String redirectPath;
  private LocalDateTime createdAt;

  public NotificationResponseDTO(Notification notification) {
    this.id = notification.getId();
    this.userId = notification.getUser().getId();
    this.type = notification.getType();
    this.message = notification.getMessage();
    this.targetEntityType = notification.getTargetEntityType();
    this.targetEntityId = notification.getTargetEntityId();
    this.isRead = notification.isRead();
    this.redirectPath = notification.getRedirectPath();
    this.createdAt = notification.getCreatedAt();
  }
}
