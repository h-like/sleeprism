package com.example.sleeprism.controller;

import com.example.sleeprism.dto.*;
import com.example.sleeprism.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.sleeprism.entity.User; // User 엔티티 경로 확인

@RestController
@RequestMapping("/api/auth") // 경로를 /api/auth로 통일
@RequiredArgsConstructor
public class AuthController { // AuthController로 변경 (UserController와 분리)

  private final UserService userService; // UserService 주입

  // 회원가입
  @PostMapping("/signup")
  public ResponseEntity<UserResponseDTO> signUp(@Valid @RequestBody UserSignUpRequestDTO requestDto) {
    UserResponseDTO responseDto = userService.signUp(requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  // 일반 로그인 (Spring Security와 연동)
  @PostMapping("/signin")
  public ResponseEntity<AuthResponseDTO> signIn(
      @Valid @RequestBody UserSignInRequestDTO requestDto,
      HttpServletRequest request
  ) {
    String ipAddress = request.getRemoteAddr();
    AuthResponseDTO responseDto = userService.signIn(requestDto, ipAddress);
    return ResponseEntity.ok(responseDto);
  }

  // OAuth2 로그인 성공 후 리디렉션 처리 (프론트엔드에서 처리)
  // 이 컨트롤러는 주로 일반 로그인/회원가입 API를 제공합니다.
  // OAuth2 관련 로직은 CustomOAuth2UserService와 OAuth2AuthenticationSuccessHandler에서 처리됩니다.

  // 임시: 사용자 정보 확인용 (개발/테스트 용도)
  @GetMapping("/me")
  public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails instanceof User) {
      User currentUser = (User) userDetails;
      return ResponseEntity.ok(new UserResponseDTO(currentUser));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
