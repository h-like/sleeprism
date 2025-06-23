package com.example.sleeprism.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignUpRequestDTO{
  @NotBlank(message = "Username cannot be blank.")
  @Size(min = 1, max = 50, message = "Username must be between 4 and 50 characters.")
  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username can only contain letters and numbers.")
  private String username;

  @NotBlank(message = "Password cannot be blank.")
  @Size(min = 1, max = 20, message = "Password must be between 8 and 20 characters.")
  // 복잡성 규칙 (예: 영문, 숫자, 특수문자 포함)은 여기에 추가하거나 서비스 계층에서 검증
  private String password;

  @NotBlank(message = "Nickname cannot be blank.")
  @Size(min = 1, max = 50, message = "Nickname must be between 2 and 50 characters.")
  private String nickname;

  @NotBlank(message = "Email cannot be blank.")
  @Email(message = "Invalid email format.")
  @Size(max = 100, message = "Email cannot exceed 100 characters.")
  private String email;
}
