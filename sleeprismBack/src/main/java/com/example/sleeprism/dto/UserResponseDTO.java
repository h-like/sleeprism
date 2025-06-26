package com.example.sleeprism.dto;

import com.example.sleeprism.entity.User;
import com.example.sleeprism.entity.UserRole; // 최상위 UserRole 임포트
import com.example.sleeprism.entity.UserStatus; // 최상위 UserStatus 임포트
import com.example.sleeprism.entity.SocialProvider; // 최상위 SocialProvider 임포트
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor // DTO는 기본 생성자 필요
public class UserResponseDTO {
  private Long id;
  private String email;
  private String nickname;
  private String profileImageUrl;
  private UserRole role; // <-- 최상위 UserRole 타입으로 변경
  private UserStatus status; // <-- 최상위 UserStatus 타입으로 변경
  private SocialProvider socialProvider; // <-- 최상위 SocialProvider 타입으로 변경
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean isDeleted; // isDeleted 필드 추가

  // User 엔티티로부터 DTO 생성 (생성자)
  public UserResponseDTO(User user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.nickname = user.getNickname();
    this.profileImageUrl = user.getProfileImageUrl();
    this.role = user.getRole(); // <-- 이제 타입 일치
    this.status = user.getStatus(); // <-- 이제 타입 일치
    this.socialProvider = user.getSocialProvider(); // <-- 이제 타입 일치
    this.createdAt = user.getCreatedAt();
    this.updatedAt = user.getUpdatedAt();
    this.isDeleted = user.isDeleted(); // isDeleted 필드 매핑
  }
}
