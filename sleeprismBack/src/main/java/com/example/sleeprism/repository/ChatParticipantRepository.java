package com.example.sleeprism.repository;

import com.example.sleeprism.entity.ChatParticipant;
import com.example.sleeprism.entity.ChatRoom;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  /**
   * 특정 채팅방의 모든 활성 참가자를 조회합니다.
   *
   * @param chatRoom 채팅방 엔티티
   * @param isLeft 나갔는지 여부 (false일 경우 활성 참가자)
   * @return 해당 채팅방의 참가자 목록
   */
  List<ChatParticipant> findByChatRoomAndIsLeft(ChatRoom chatRoom, boolean isLeft);

  /**
   * 특정 채팅방에서 특정 사용자의 참가 정보를 조회합니다.
   *
   * @param chatRoom 채팅방 엔티티
   * @param user 사용자 엔티티
   * @return 해당 채팅방에서의 참가 정보 (Optional)
   */
  Optional<ChatParticipant> findByChatRoomAndUser(ChatRoom chatRoom, User user);

  /**
   * 특정 채팅방에서 특정 사용자가 현재 참여 중인지 확인합니다.
   * @param chatRoom 채팅방 엔티티
   * @param user 사용자 엔티티
   * @param isLeft 나갔는지 여부 (false)
   * @return 참여 중인 참가 정보 (Optional)
   */
  Optional<ChatParticipant> findByChatRoomAndUserAndIsLeft(ChatRoom chatRoom, User user, boolean isLeft);
}
