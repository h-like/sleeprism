package com.example.sleeprism.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignInRequestDTO {
  @NotBlank(message = "email cannot be blank.")
  private String email; // 사용자 이름 또는 이메일로 로그인 가능

  @NotBlank(message = "Password cannot be blank.")
  private String password;
}
