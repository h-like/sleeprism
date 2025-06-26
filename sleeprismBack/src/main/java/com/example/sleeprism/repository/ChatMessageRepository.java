package com.example.sleeprism.repository;

import com.example.sleeprism.entity.ChatMessage;
import com.example.sleeprism.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /**
   * 특정 채팅방의 메시지를 최신순으로 조회합니다.
   *
   * @param chatRoom 메시지가 속한 채팅방 엔티티
   * @return 해당 채팅방의 메시지 목록
   */
  List<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);

  /**
   * 특정 채팅방의 최신 N개 메시지를 조회합니다. (페이징 처리)
   *
   * @param chatRoom 메시지가 속한 채팅방 엔티티
   * @param pageable 페이징 정보 (예: PageRequest.of(0, 50))
   * @return 해당 채팅방의 최신 메시지 목록
   */
  List<ChatMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);

  /**
   * 특정 메시지를 조회합니다.
   * @param id 메시지 ID
   * @return 메시지 (Optional)
   */
  Optional<ChatMessage> findById(Long id);
}
