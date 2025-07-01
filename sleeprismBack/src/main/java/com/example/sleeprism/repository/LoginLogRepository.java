package com.example.sleeprism.repository;

import com.example.sleeprism.entity.LoginLog;
import com.example.sleeprism.entity.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
  // user로 로그인 로그 조회 (특정 사용자의 로그인 기록 확인)

  List<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId);

  // 성공한 로그인 로그만 조회
//  List<LoginLog> findBySuccessStatusTrue();

  // 특정 로그인 타입의 로그 조회
 // List<LoginLog> findByLoginType(LoginType loginType); // LoginType enum을 사용
}
