package com.example.sleeprism.repository;

import com.example.sleeprism.entity.ChatRoom;
import com.example.sleeprism.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  /**
   * 특정 사용자가 참여하고 있는 모든 채팅방을 조회합니다.
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 채팅방 목록
   */
  @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants cp WHERE cp.user.id = :userId AND cr.isDeleted = false AND cp.isLeft = false ORDER BY cr.createdAt DESC")
  List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

  /**
   * 두 사용자 간의 1대1 채팅방을 조회합니다.
   *
   * @param user1Id 첫 번째 사용자 ID
   * @param user2Id 두 번째 사용자 ID
   * @return 두 사용자 간의 1대1 채팅방 (Optional)
   */
  @Query("SELECT cr FROM ChatRoom cr " +
      "JOIN ChatParticipant cp1 ON cr.id = cp1.chatRoom.id " +
      "JOIN ChatParticipant cp2 ON cr.id = cp2.chatRoom.id " +
      "WHERE cr.type = 'SINGLE' AND cr.isDeleted = false " +
      "AND cp1.user.id = :user1Id AND cp2.user.id = :user2Id " +
      "AND cp1.isLeft = false AND cp2.isLeft = false")
  Optional<ChatRoom> findSingleChatRoomBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

  /**
   * 특정 ID의 채팅방을 삭제되지 않은 상태로 조회합니다.
   * @param id 채팅방 ID
   * @return 채팅방 (Optional)
   */
  Optional<ChatRoom> findByIdAndIsDeletedFalse(Long id);

  /**
   * 특정 ID의 그룹 채팅방을 조회합니다.
   * @param id 채팅방 ID
   * @param type 채팅방 유형 (GROUP)
   * @return 그룹 채팅방 (Optional)
   */
  Optional<ChatRoom> findByIdAndTypeAndIsDeletedFalse(Long id, ChatRoomType type);

}
