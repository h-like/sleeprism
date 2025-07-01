package com.example.sleeprism.service;

import com.example.sleeprism.dto.NotificationResponseDTO;
import com.example.sleeprism.entity.Notification;
import com.example.sleeprism.entity.NotificationType;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.NotificationRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 설정
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  /**
   * 새로운 알림을 생성하고 저장합니다.
   *
   * @param recipient 알림을 받을 사용자 엔티티
   * @param type 알림의 종류 (예: COMMENT, SALE_REQUEST)
   * @param message 사용자에게 표시될 알림 메시지
   * @param targetEntityType 알림과 관련된 엔티티의 타입 (예: "Post", "SaleRequest")
   * @param targetEntityId 알림과 관련된 엔티티의 ID
   * @param redirectPath 알림 클릭 시 이동할 프론트엔드 경로 (URL)
   * @return 생성된 알림의 응답 DTO
   */
  @Transactional
  public NotificationResponseDTO createNotification(User recipient, NotificationType type, String message,
                                                    String targetEntityType, Long targetEntityId, String redirectPath) {
    if (recipient == null) {
      log.warn("Attempted to create notification for a null recipient. Skipping.");
      return null; // 수신자가 null이면 알림 생성 건너뛰기
    }
    Notification notification = Notification.builder()
        .user(recipient)
        .type(type)
        .message(message)
        .targetEntityType(targetEntityType)
        .targetEntityId(targetEntityId)
        .redirectPath(redirectPath)
        .build();
    Notification savedNotification = notificationRepository.save(notification);
    log.info("Notification created for user {}: {}", recipient.getId(), message);
    return new NotificationResponseDTO(savedNotification);
  }

  /**
   * 특정 사용자의 모든 알림을 조회합니다.
   *
   * @param userId 알림을 조회할 사용자 ID
   * @return 해당 사용자의 알림 목록 DTO
   */
  public List<NotificationResponseDTO> getNotificationsForUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
        .map(NotificationResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 특정 사용자의 읽지 않은 알림을 조회합니다.
   *
   * @param userId 알림을 조회할 사용자 ID
   * @return 해당 사용자의 읽지 않은 알림 목록 DTO
   */
  public List<NotificationResponseDTO> getUnreadNotificationsForUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    return notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false).stream()
        .map(NotificationResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 특정 알림을 읽음 상태로 변경합니다.
   *
   * @param notificationId 읽음 처리할 알림 ID
   * @param userId 알림을 소유한 사용자 ID (권한 검증용)
   * @return 업데이트된 알림의 응답 DTO
   */
  @Transactional
  public NotificationResponseDTO markNotificationAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

    if (!notification.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to modify this notification.");
    }

    if (!notification.isRead()) { // 이미 읽음 상태가 아니라면
      notification.markAsRead();
      // notificationRepository.save(notification); // 변경 감지로 자동 저장
      log.info("Notification {} marked as read for user {}", notificationId, userId);
    }
    return new NotificationResponseDTO(notification);
  }

  /**
   * 특정 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.
   *
   * @param userId 모든 알림을 읽음 처리할 사용자 ID
   */
  @Transactional
  public void markAllNotificationsAsRead(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
    for (Notification notification : unreadNotifications) {
      notification.markAsRead();
    }
    notificationRepository.saveAll(unreadNotifications); // 변경된 모든 알림을 한 번에 저장
    log.info("All unread notifications marked as read for user {}", userId);
  }

  /**
   * 특정 알림을 삭제합니다. (하드 삭제)
   * @param notificationId 삭제할 알림 ID
   * @param userId 알림을 소유한 사용자 ID (권한 검증용)
   */
  @Transactional
  public void deleteNotification(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

    if (!notification.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to delete this notification.");
    }
    notificationRepository.delete(notification);
    log.info("Notification {} deleted for user {}", notificationId, userId);
  }
}
