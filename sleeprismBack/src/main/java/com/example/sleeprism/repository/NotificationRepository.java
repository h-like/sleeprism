package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Notification;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  /**
   * 특정 사용자에게 할당된 모든 알림을 최신순으로 조회합니다.
   *
   * @param user 알림을 받을 사용자 엔티티
   * @return 해당 사용자의 알림 목록
   */
  List<Notification> findByUserOrderByCreatedAtDesc(User user);

  /**
   * 특정 사용자에게 할당된 읽지 않은 알림을 최신순으로 조회합니다.
   *
   * @param user 알림을 받을 사용자 엔티티
   * @param isRead 읽음 여부 (false일 경우 읽지 않은 알림)
   * @return 해당 사용자의 읽지 않은 알림 목록
   */
  List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean isRead);

  /**
   * 특정 사용자의 모든 알림을 읽음 상태로 업데이트합니다. (벌크 업데이트를 위해 @Modifying과 @Query를 사용할 수 있으나,
   * 여기서는 간단히 List를 받아 각 항목을 업데이트하는 방식으로 간주합니다. 대량의 알림 처리 시 고려 필요)
   *
   * @param user 알림을 받을 사용자 엔티티
   * @param isRead 업데이트할 읽음 상태 (true)
   * @return 업데이트된 알림 목록
   */
  // @Modifying // bulk update 시 필요
  // @Query("UPDATE Notification n SET n.isRead = :isRead WHERE n.user = :user") // 벌크 업데이트 쿼리 예시
  // int markAllAsReadForUser(@Param("user") User user, @Param("isRead") boolean isRead);

  // Note: Spring Data JPA의 saveAll()을 사용하여 여러 Notification을 한 번에 업데이트할 수 있습니다.
  // 이 메서드는 직접적인 사용보다는 서비스 계층에서 활용됩니다.
}
