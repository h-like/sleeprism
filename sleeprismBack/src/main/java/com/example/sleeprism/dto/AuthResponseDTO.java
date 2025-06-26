package com.example.sleeprism.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // <-- 이 어노테이션이 (String accessToken, UserResponseDTO user) 생성자를 자동 생성합니다.
public class AuthResponseDTO {
  private String accessToken;
  // private String refreshToken; // 만약 Refresh Token을 사용한다면 추가

  private UserResponseDTO user; // UserResponseDTO 객체

  // 이 생성자는 @AllArgsConstructor와 중복되므로 제거합니다.
  // public AuthResponseDTO(String accessToken, UserResponseDTO userResponseDTO) {
  //     this.accessToken = accessToken;
  //     this.user = userResponseDTO;
  // }
}
