package com.example.sleeprism.controller;

import com.example.sleeprism.dto.*;
import com.example.sleeprism.entity.User;
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

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  // 회원가입
  @PostMapping("/signup")
  public ResponseEntity<UserResponseDTO> signUp(@Valid @RequestBody UserSignUpRequestDTO requestDto) {
    UserResponseDTO responseDto = userService.signUp(requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  // 일반 로그인 (Spring Security와 연동)
  // 실제로는 Spring Security의 UsernamePasswordAuthenticationFilter 등이 이 역할을 대신합니다.
  // 여기서는 간단한 테스트용으로만 사용하거나, 로그인 로그를 남기는 용도로 활용됩니다.
  @PostMapping("/signin") // GET -> POST로 변경
  public ResponseEntity<AuthResponseDTO> signIn( // UserResponseDTO -> AuthResponseDTO로 반환 타입 변경
                                                 @Valid @RequestBody UserSignInRequestDTO requestDto,
                                                 HttpServletRequest request
  ) {
    String ipAddress = request.getRemoteAddr();
    AuthResponseDTO responseDto = userService.signIn(requestDto, ipAddress); // AuthResponseDTO 반환
    return ResponseEntity.ok(responseDto);
  }


  // 사용자 프로필 조회 (인증된 사용자만 접근 가능)
  @GetMapping("/profile")
  public ResponseEntity<UserResponseDTO> getUserProfile(
      @AuthenticationPrincipal UserDetails userDetails // 현재 로그인한 사용자 정보
  ) {
    // userDetails에서 userId를 추출
    Long userId = extractUserIdFromUserDetails(userDetails); // TODO: 실제 UserDetails 구현에 따라 추출 방식 변경
    UserResponseDTO responseDto = userService.getUserProfile(userId);
    return ResponseEntity.ok(responseDto);
  }

  // 사용자 프로필 업데이트 (인증된 사용자만 가능)
  // MultipartFile을 받기 위해 @RequestPart 사용
  @PutMapping("/profile")
  public ResponseEntity<UserResponseDTO> updateProfile(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestPart("request") UserProfileUpdateRequestDTO requestDto,
      @RequestPart(value = "profileImageFile", required = false) MultipartFile profileImageFile // 선택적 파일 업로드
  ) throws IOException {
    Long userId = extractUserIdFromUserDetails(userDetails); // TODO: 실제 UserDetails 구현에 따라 추출 방식 변경
    UserResponseDTO responseDto = userService.updateProfile(userId, requestDto, profileImageFile);
    return ResponseEntity.ok(responseDto);
  }

  // 사용자 계정 삭제 (탈퇴) (인증된 사용자만 가능)
  @DeleteMapping
  public ResponseEntity<Void> deleteUser(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId = extractUserIdFromUserDetails(userDetails); // TODO: 실제 UserDetails 구현에 따라 추출 방식 변경
    userService.deleteUser(userId);
    return ResponseEntity.noContent().build();
  }

  // UserDetails에서 userId를 추출하는 헬퍼 메서드 (실제 구현에 따라 달라짐)
  // UserDetails에서 userId를 추출하는 헬퍼 메서드 (실제 구현에 따라 달라짐)
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    // 실제 UserDetails 구현이 com.example.sleeprism.model.User라고 가정합니다.
    // User 엔티티에 getId() 메서드가 있어서 사용자 ID를 반환한다고 가정합니다.
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    // TODO: 만약 UserDetails가 User 엔티티가 아니라 CustomUserDetails와 같은 다른 클래스라면
    // 해당 클래스로 캐스팅하고, ID를 가져오는 적절한 메서드를 호출해야 합니다.
    // 예: ((CustomUserDetails) userDetails).getUserId();
    throw new IllegalArgumentException("사용자 정보를 가져올 수 없습니다. UserDetails 구현을 확인하세요.");
  }
}
