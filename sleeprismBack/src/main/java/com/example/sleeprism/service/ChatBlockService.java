package com.example.sleeprism.service;

import com.example.sleeprism.dto.ChatBlockResponseDTO;
import com.example.sleeprism.entity.ChatBlock;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.ChatBlockRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatBlockService {

  private final ChatBlockRepository chatBlockRepository;
  private final UserRepository userRepository;

  /**
   * 사용자를 차단합니다.
   *
   * @param blockerId 차단하려는 사용자 ID
   * @param blockedUserId 차단당할 사용자 ID
   * @return 생성된 차단 정보 DTO
   */
  @Transactional
  public ChatBlockResponseDTO blockUser(Long blockerId, Long blockedUserId) {
    if (blockerId.equals(blockedUserId)) {
      throw new IllegalArgumentException("자신을 차단할 수 없습니다.");
    }

    User blocker = userRepository.findById(blockerId)
        .orElseThrow(() -> new EntityNotFoundException("Blocker user not found with ID: " + blockerId));
    User blocked = userRepository.findById(blockedUserId)
        .orElseThrow(() -> new EntityNotFoundException("Blocked user not found with ID: " + blockedUserId));

    // 이미 차단되어 있는지 확인
    if (chatBlockRepository.findByBlockerAndBlocked(blocker, blocked).isPresent()) {
      throw new IllegalStateException("이미 차단된 사용자입니다.");
    }

    ChatBlock chatBlock = ChatBlock.builder()
        .blocker(blocker)
        .blocked(blocked)
        .build();

    ChatBlock savedChatBlock = chatBlockRepository.save(chatBlock);
    log.info("User {} blocked user {}", blockerId, blockedUserId);
    return new ChatBlockResponseDTO(savedChatBlock);
  }

  /**
   * 사용자의 차단을 해제합니다.
   *
   * @param blockerId 차단을 해제하려는 사용자 ID
   * @param blockedUserId 차단 해제할 사용자 ID
   */
  @Transactional
  public void unblockUser(Long blockerId, Long blockedUserId) {
    User blocker = userRepository.findById(blockerId)
        .orElseThrow(() -> new EntityNotFoundException("Blocker user not found with ID: " + blockerId));
    User blocked = userRepository.findById(blockedUserId)
        .orElseThrow(() -> new EntityNotFoundException("Blocked user not found with ID: " + blockedUserId));

    Optional<ChatBlock> existingBlock = chatBlockRepository.findByBlockerAndBlocked(blocker, blocked);
    if (existingBlock.isEmpty()) {
      throw new IllegalStateException("해당 사용자는 차단되어 있지 않습니다.");
    }

    chatBlockRepository.deleteByBlockerAndBlocked(blocker, blocked);
    log.info("User {} unblocked user {}", blockerId, blockedUserId);
  }

  /**
   * 특정 사용자가 차단한 모든 사용자 목록을 조회합니다.
   *
   * @param blockerId 차단한 사용자 ID
   * @return 차단 목록 DTO
   */
  public List<ChatBlockResponseDTO> getBlockedUsers(Long blockerId) {
    User blocker = userRepository.findById(blockerId)
        .orElseThrow(() -> new EntityNotFoundException("Blocker user not found with ID: " + blockerId));
    return chatBlockRepository.findByBlocker(blocker).stream()
        .map(ChatBlockResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 두 사용자 간의 차단 관계가 존재하는지 확인합니다.
   * @param user1Id 사용자 1 ID
   * @param user2Id 사용자 2 ID
   * @return true (차단 관계 존재), false (차단 관계 없음)
   */
  public boolean areUsersBlocked(Long user1Id, Long user2Id) {
    User user1 = userRepository.findById(user1Id)
        .orElseThrow(() -> new EntityNotFoundException("User 1 not found with ID: " + user1Id));
    User user2 = userRepository.findById(user2Id)
        .orElseThrow(() -> new EntityNotFoundException("User 2 not found with ID: " + user2Id));

    // user1이 user2를 차단했는지 또는 user2가 user1을 차단했는지 확인
    return chatBlockRepository.findByBlockerAndBlocked(user1, user2).isPresent() ||
        chatBlockRepository.findByBlockerAndBlocked(user2, user1).isPresent();
  }
}
