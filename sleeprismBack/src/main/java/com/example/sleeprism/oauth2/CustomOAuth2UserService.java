package com.example.sleeprism.oauth2;

import com.example.sleeprism.entity.User;
import com.example.sleeprism.entity.UserRole; // UserRole 임포트 (최상위 enum)
import com.example.sleeprism.entity.SocialProvider; // SocialProvider 임포트 (최상위 enum)
import com.example.sleeprism.entity.UserStatus; // UserStatus 임포트 (최상위 enum)
import com.example.sleeprism.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google, naver, kakao
    String socialId = oAuth2User.getName(); // 소셜 서비스에서 제공하는 ID
    Map<String, Object> attributes = oAuth2User.getAttributes();

    String email = null;
    String nickname = null;
    String profileImageUrl = null;

    SocialProvider socialProvider = SocialProvider.valueOf(registrationId.toUpperCase());

    // 소셜 서비스별 사용자 정보 추출
    if ("google".equals(registrationId)) {
      email = (String) attributes.get("email");
      nickname = (String) attributes.get("name");
      profileImageUrl = (String) attributes.get("picture");
    } else if ("naver".equals(registrationId)) {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      email = (String) response.get("email");
      nickname = (String) response.get("nickname");
      profileImageUrl = (String) response.get("profile_image");
    } else if ("kakao".equals(registrationId)) {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      email = (String) kakaoAccount.get("email");
      Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
      nickname = (String) profile.get("nickname");
      profileImageUrl = (String) profile.get("profile_image_url");
    }

    log.info("OAuth2 로그인 시도 - provider: {}, email: {}, nickname: {}", socialProvider, email, nickname);

    // 사용자 정보 업데이트 또는 신규 등록
    Optional<User> userOptional = userRepository.findByEmail(email);
    User user;

    if (userOptional.isPresent()) {
      user = userOptional.get();
      // 기존 사용자의 정보 업데이트 (닉네임, 프로필 이미지 등)
      if (nickname != null && !nickname.equals(user.getNickname())) {
        user.updateNicknameAndEmail(nickname, user.getEmail()); // <-- 수정: 닉네임과 이메일 업데이트
      }
      if (profileImageUrl != null && !profileImageUrl.equals(user.getProfileImageUrl())) {
        user.updateProfileImageUrl(profileImageUrl); // <-- 수정: 프로필 이미지 업데이트
      }
      userRepository.save(user); // 변경사항 저장
    } else {
      // 새로운 사용자 등록
      user = User.builder()
          .email(email)
          .nickname(nickname != null ? nickname : "소셜유저") // 닉네임이 null이면 기본값 설정
          .password("social_login") // 소셜 로그인 사용자는 임시 비밀번호 설정
          .role(UserRole.USER) // 기본 역할 USER
          .socialId(socialId)
          .username(nickname)
          .socialProvider(socialProvider)
          .profileImageUrl(profileImageUrl)
          .status(UserStatus.ACTIVE) // 기본 상태 ACTIVE
          .isDeleted(false) // 기본 삭제 상태 false
          .build();
      userRepository.save(user);
      log.info("새로운 소셜 사용자 등록: email={}", email);
    }

    // CustomOAuth2User 생성자 인자 변경에 따라 수정
    return new CustomOAuth2User(user, oAuth2User.getAttributes(), socialId, socialProvider);
  }
}
