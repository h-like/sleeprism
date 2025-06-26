package com.example.sleeprism.service;

import com.example.sleeprism.dto.AuthResponseDTO;
import com.example.sleeprism.dto.UserProfileUpdateRequestDTO;
import com.example.sleeprism.dto.UserResponseDTO;
import com.example.sleeprism.dto.UserSignInRequestDTO;
import com.example.sleeprism.dto.UserSignUpRequestDTO;
import com.example.sleeprism.entity.*;
import com.example.sleeprism.jwt.JwtTokenProvider;
import com.example.sleeprism.repository.LoginLogRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  private final LoginLogRepository loginLogRepository;
  private final S3UploadService s3UploadService; // S3 서비스 주입

  @Transactional
  public UserResponseDTO signUp(UserSignUpRequestDTO requestDto) {
    if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
      throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
    }

    User user = User.builder()
        .email(requestDto.getEmail())
        .password(passwordEncoder.encode(requestDto.getPassword()))
        .nickname(requestDto.getNickname())
        .role(UserRole.USER) // 기본 역할 USER
        .socialProvider(SocialProvider.NONE) // 일반 회원가입은 NONE
        .status(UserStatus.ACTIVE) // 기본 상태 ACTIVE
        .isDeleted(false) // 기본 삭제 상태 false
        .build();

    User savedUser = userRepository.save(user);
    return new UserResponseDTO(savedUser);
  }

  @Transactional
  public AuthResponseDTO signIn(UserSignInRequestDTO requestDto, String ipAddress) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      User user = (User) authentication.getPrincipal(); // UserDetails에서 User 엔티티 추출
      // generateToken 호출 인자 수정: userId, userEmail, userNickname, authorities
      String jwt = jwtTokenProvider.generateToken(
          user.getId(),
          user.getEmail(),
          user.getNickname(),
          user.getAuthorities() // UserDetails가 제공하는 권한 목록
      );

      // 로그인 로그 저장
      LoginLog loginLog = LoginLog.builder()
          .userId(user.getId()) // userId 빌더 메서드 사용
          .loginTime(LocalDateTime.now())
          .ipAddress(ipAddress)
          .loginType(LoginType.NORMAL) // LoginLog.LoginType 사용 (내부 enum)
          .build();
      loginLogRepository.save(loginLog);
      log.info("User {} logged in successfully from IP: {}", user.getEmail(), ipAddress);

      // AuthResponseDTO 생성자 수정 (UserResponseDTO를 넘기도록)
      return new AuthResponseDTO(jwt, new UserResponseDTO(user));
    } catch (Exception e) {
      log.error("로그인 실패: {}", e.getMessage());
      throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  }

  public UserResponseDTO getUserProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    return new UserResponseDTO(user);
  }

  @Transactional
  public UserResponseDTO updateProfile(Long userId, UserProfileUpdateRequestDTO requestDto, MultipartFile profileImageFile) throws IOException {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // 닉네임과 이메일 업데이트 (null 체크 추가)
    if (requestDto.getNickname() != null || requestDto.getEmail() != null) {
      user.updateNicknameAndEmail(requestDto.getNickname(), requestDto.getEmail());
    }

    // 프로필 이미지 업데이트
    if (profileImageFile != null && !profileImageFile.isEmpty()) {
      String imageUrl = s3UploadService.uploadFile(profileImageFile, "profile-images");
      user.updateProfileImageUrl(imageUrl);
    } else if (requestDto.isRemoveProfileImage()) { // isRemoveProfileImage() 메서드 사용
      // 이미지 제거 요청이 있으면 null로 설정
      user.updateProfileImageUrl(null);
    }

    User updatedUser = userRepository.save(user);
    return new UserResponseDTO(updatedUser);
  }


  @Transactional
  public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    user.setDeleted(true); // 소프트 삭제
    user.setStatus(UserStatus.DELETED); // 상태도 DELETED로 변경
    userRepository.save(user);
    log.info("User {} (ID: {}) has been soft-deleted.", user.getEmail(), userId);
  }
}
