package com.example.sleeprism.repository;

import com.example.sleeprism.entity.ChatBlock;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatBlockRepository extends JpaRepository<ChatBlock, Long> {

  /**
   * 특정 사용자가 차단한 모든 사용자 목록을 조회합니다.
   *
   * @param blocker 차단한 사용자 엔티티
   * @return 차단 목록
   */
  List<ChatBlock> findByBlocker(User blocker);

  /**
   * 특정 사용자를 차단한 모든 사용자 목록을 조회합니다.
   *
   * @param blocked 차단당한 사용자 엔티티
   * @return 역 차단 목록
   */
  List<ChatBlock> findByBlocked(User blocked);


  /**
   * 특정 사용자가 다른 특정 사용자를 차단했는지 여부를 확인합니다.
   *
   * @param blocker 차단한 사용자 엔티티
   * @param blocked 차단당한 사용자 엔티티
   * @return 차단 정보 (Optional)
   */
  Optional<ChatBlock> findByBlockerAndBlocked(User blocker, User blocked);

  /**
   * 특정 차단 정보를 삭제합니다.
   * @param blocker 차단한 사용자 엔티티
   * @param blocked 차단당한 사용자 엔티티
   */
  void deleteByBlockerAndBlocked(User blocker, User blocked);
}
