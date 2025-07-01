package com.example.sleeprism.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // DTO는 기본 생성자 필요
public class UserProfileUpdateRequestDTO {

  @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
  private String nickname;

  @Email(message = "유효한 이메일 주소를 입력해주세요.")
  private String email;

  // 프로필 이미지 제거 요청을 위한 필드 추가
  private boolean removeProfileImage; // <-- 이 필드와 Getter/Setter가 필요합니다.
}
