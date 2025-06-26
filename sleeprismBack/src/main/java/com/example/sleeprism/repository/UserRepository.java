package com.example.sleeprism.repository;

import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  // JpaRepository<엔티티 타입, 엔티티의 ID 타입>

  // email로 사용자 조회 (CustomUserDetailsService에서 사용)
  Optional<User> findByEmail(String email);

  // nickname으로 사용자 조회
  Optional<User> findByNickname(String nickname);

  // socialProvider와 socialId로 사용자 조회 (소셜 로그인)
  Optional<User> findBySocialProviderAndSocialId(String socialProvider, String socialId);

  // --- 아래 메서드들은 User 엔티티에 'username' 필드가 없을 경우 컴파일/실행 오류를 유발하므로 제거합니다. ---
  // Optional<User> findByUsername(String username);
  // Optional<User> findByUsernameOrEmail(String username, String email);
  // boolean existsByUsername(String username);

  // 특정 필드의 존재 여부 확인 (중복 체크 등에 활용)
  boolean existsByEmail(String email);
  boolean existsByNickname(String nickname);
}
