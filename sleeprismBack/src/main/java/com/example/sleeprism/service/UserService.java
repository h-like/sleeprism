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
import org.springframework.security.core.userdetails.UserDetails;
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
  private final FileStorageService fileStorageService; // S3 서비스 대신 로컬 파일 저장 서비스 주입

  @Transactional
  public UserResponseDTO signUp(UserSignUpRequestDTO requestDto) {
    if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
      throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
    }

    User user = User.builder()
        .email(requestDto.getEmail())
        .password(passwordEncoder.encode(requestDto.getPassword()))
        .nickname(requestDto.getNickname())
        .username(requestDto.getUsername())
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

    // 닉네임 업데이트: requestDto에 닉네임이 존재할 경우에만 업데이트
    if (requestDto.getNickname() != null) {
      user.setNickname(requestDto.getNickname()); // User 엔티티에 setNickname() 메서드가 필요합니다.
    }

    // 이메일 업데이트: requestDto에 이메일이 존재하고 (null이 아니며), 기존 이메일과 다를 경우에만 업데이트
    // DB의 email 컬럼은 NOT NULL이므로, null 값으로 업데이트 시도 방지
    if (requestDto.getEmail() != null && !requestDto.getEmail().equals(user.getEmail())) {
      // (선택 사항) 이메일 변경 시 중복 체크를 추가할 수 있습니다.
      if (userRepository.existsByEmail(requestDto.getEmail())) {
        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
      }
      user.setEmail(requestDto.getEmail()); // User 엔티티에 setEmail() 메서드가 필요합니다.
    }

    // 프로필 이미지 업데이트 로직 변경: S3 대신 로컬 파일 시스템 사용
    if (profileImageFile != null && !profileImageFile.isEmpty()) {
      // 기존 프로필 이미지가 있다면 삭제
      if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
        fileStorageService.deleteFile(user.getProfileImageUrl());
      }
      String imageUrl = fileStorageService.uploadFile(profileImageFile, "profile-images"); // saveFile 대신 uploadFile 사용
      user.setProfileImageUrl(imageUrl); // User 엔티티에 setProfileImageUrl() 메서드가 필요합니다.
    } else if (requestDto.isRemoveProfileImage()) { // 이미지 제거 요청이 있으면
      // 기존 프로필 이미지가 있다면 삭제
      if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
        fileStorageService.deleteFile(user.getProfileImageUrl());
      }
      user.setProfileImageUrl(null); // DB에서 URL 제거
    }

    // 업데이트 시간 기록 (User 엔티티에 setUpdatedAt() 메서드가 필요하거나 @UpdateTimestamp 사용)
//    user.setUpdatedAt(LocalDateTime.now());

    User updatedUser = userRepository.save(user); // 변경된 내용 저장
    return new UserResponseDTO(updatedUser);
  }


  @Transactional
  public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // 사용자를 소프트 삭제하기 전에, 프로필 이미지가 있다면 로컬에서 삭제
    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
      fileStorageService.deleteFile(user.getProfileImageUrl());
    }

    user.setDeleted(true); // 소프트 삭제
    user.setStatus(UserStatus.DELETED); // 상태도 DELETED로 변경
    userRepository.save(user);
    log.info("User {} (ID: {}) has been soft-deleted.", user.getEmail(), userId);
  }

  // UserDetails에서 userId를 추출하는 헬퍼 메서드 (실제 구현에 따라 달라짐)
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalArgumentException("사용자 정보를 가져올 수 없습니다. UserDetails 구현을 확인하세요.");
  }
}
