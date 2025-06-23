package com.example.sleeprism.service;

import com.example.sleeprism.dto.AuthResponseDTO;
import com.example.sleeprism.dto.UserProfileUpdateRequestDTO;
import com.example.sleeprism.dto.UserResponseDTO;
import com.example.sleeprism.dto.UserSignInRequestDTO;
import com.example.sleeprism.dto.UserSignUpRequestDTO;

import com.example.sleeprism.entity.LoginLog;
import com.example.sleeprism.entity.LoginType;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.entity.UserRole;
import com.example.sleeprism.jwt.JwtTokenProvider;
import com.example.sleeprism.repository.LoginLogRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final LoginLogRepository loginLogRepository;
  private final PasswordEncoder passwordEncoder;
  private final FileStorageService fileStorageService;
  private final JwtTokenProvider jwtTokenProvider;

  // 회원가입
  @Transactional
  public UserResponseDTO signUp(UserSignUpRequestDTO requestDto) {
    if (userRepository.existsByUsername(requestDto.getUsername())) {
      throw new EntityExistsException("Username already exists.");
    }
    if (userRepository.existsByNickname(requestDto.getNickname())) {
      throw new EntityExistsException("Nickname already exists.");
    }
    if (userRepository.existsByEmail(requestDto.getEmail())) {
      throw new EntityExistsException("Email already exists.");
    }

    User newUser = User.builder()
        .username(requestDto.getUsername())
        .password(passwordEncoder.encode(requestDto.getPassword()))
        .nickname(requestDto.getNickname())
        .email(requestDto.getEmail())
        .profileImageUrl(null)
        .role(UserRole.USER)
        .status(com.example.sleeprism.entity.UserStatus.ACTIVE)
        .build();

    User savedUser = userRepository.save(newUser);
    return new UserResponseDTO(savedUser);
  }

  // 일반 로그인
  @Transactional
  public AuthResponseDTO signIn(UserSignInRequestDTO requestDto, String ipAddress) {
    Optional<User> userOptional = userRepository.findByEmail(requestDto.getEmail());

    boolean successStatus = false;
    User user = null;
    try {
      user = userOptional.orElseThrow(() -> new EntityNotFoundException("User not found or invalid credentials."));

      if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
        throw new IllegalArgumentException("Invalid password.");
      }
      successStatus = true;

      // JWT 토큰 생성
      String accessToken = jwtTokenProvider.generateToken(
          user.getId(), // userId
          user.getEmail(), // email
          user.getUsername(), // username (User 엔티티에 username 필드 있음)
          Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())) // Collection<GrantedAuthority> 형태로 변환
      );
      // AuthResponseDTO를 빌더 패턴으로 생성하여 반환
      return AuthResponseDTO.builder()
          .accessToken(accessToken)
          .userId(user.getId())
          .email(user.getEmail())
          .username(user.getUsername())
          .role(user.getRole().name())
          .build();

    } finally {
      LoginLog loginLog = LoginLog.builder()
          .user(userOptional.orElse(null))
          .ipAddress(ipAddress)
          .loginType(LoginType.NORMAL)
          .successStatus(successStatus)
          .build();
      loginLogRepository.save(loginLog);
    }
  }

  // 사용자 정보 조회
  public UserResponseDTO getUserProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    return new UserResponseDTO(user);
  }

  // 사용자 프로필 업데이트
  @Transactional
  public UserResponseDTO updateProfile(Long userId, UserProfileUpdateRequestDTO requestDto, MultipartFile profileImageFile) throws IOException {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    if (!user.getNickname().equals(requestDto.getNickname()) && userRepository.existsByNickname(requestDto.getNickname())) {
      throw new EntityExistsException("Nickname already exists.");
    }
    if (!user.getEmail().equals(requestDto.getEmail()) && userRepository.existsByEmail(requestDto.getEmail())) {
      throw new EntityExistsException("Email already exists.");
    }

    user.updateNicknameAndEmail(requestDto.getNickname(), requestDto.getEmail());

    if (profileImageFile != null && !profileImageFile.isEmpty()) {
      String imageUrl = fileStorageService.uploadFile(profileImageFile, "profile-images");
      user.updateProfileImageUrl(imageUrl);
    } else if (profileImageFile != null && profileImageFile.isEmpty() && user.getProfileImageUrl() != null) {
      fileStorageService.deleteFile(user.getProfileImageUrl());
      user.updateProfileImageUrl(null);
    }

    return new UserResponseDTO(user);
  }

  // 사용자 삭제 (탈퇴)
  @Transactional
  public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    // NOTE: 만약 '소프트 삭제'를 원한다면 아래 라인처럼 isDeleted 필드를 true로 설정해야 합니다.
    // user.setDeleted(true);
    // 그렇지 않고 실제 DB에서 삭제를 원한다면 userRepository.delete(user);를 사용합니다.
    userRepository.delete(user); // 현재는 실제 삭제 (Hard Delete)
  }
}
