package com.example.sleeprism.oauth2;

import com.example.sleeprism.entity.User;
import com.example.sleeprism.entity.SocialProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OAuth2User 인터페이스를 구현하는 커스텀 사용자 클래스입니다.
 * 우리의 User 엔티티와 OAuth2 사용자 정보를 통합하여 Spring Security에서 사용할 수 있도록 합니다.
 */
public class CustomOAuth2User implements OAuth2User {

  private User user; // 우리의 User 엔티티
  private Map<String, Object> attributes; // OAuth2 제공자로부터 받은 사용자 속성
  private String socialId;
  private SocialProvider socialProvider;

  // 생성자 수정: User 객체를 직접 받도록 변경
  public CustomOAuth2User(User user, Map<String, Object> attributes, String socialId, SocialProvider socialProvider) {
    this.user = user;
    this.attributes = attributes;
    this.socialId = socialId;
    this.socialProvider = socialProvider;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return user.getAuthorities(); // User 엔티티의 역할을 기반으로 권한을 반환
  }

  @Override
  public String getName() {
    // OAuth2User의 'name' 속성을 반환합니다. 일반적으로 sub (소셜 ID) 또는 email
    return socialId; // 소셜 ID 반환
  }

  // UserDetails의 getUsername()과 같은 역할을 하는 메서드 추가 (필요한 경우)
  public String getUsername() {
    return user.getEmail(); // User 엔티티의 이메일을 반환
  }

  // User 엔티티에 접근할 수 있는 추가적인 getter
  public User getUser() {
    return user;
  }

  public String getEmail() {
    return user.getEmail();
  }

  public String getNickname() {
    return user.getNickname();
  }

  public Long getId() {
    return user.getId();
  }
}
    