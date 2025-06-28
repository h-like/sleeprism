package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class LoginLog extends BaseTimeEntity { // BaseTimeEntity 상속

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id")
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "login_timestamp", nullable = false)
  private LocalDateTime loginTime;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  @Enumerated(EnumType.STRING)
  @Column(name = "login_type", nullable = false, length = 20)
  private LoginType loginType; // <-- 외부에서 분리된 LoginType enum 사용

  @Column(name = "success_status", nullable = false) // <-- 이 필드를 추가합니다.
  private boolean successStatus; // 로그인 성공 여부

  // LoginType enum이 이제 독립적인 파일로 분리되었으므로, 이 내부 enum은 제거합니다.
  /*
  public enum LoginType {
    NORMAL, GOOGLE, KAKAO, NAVER
  }
  */
}
