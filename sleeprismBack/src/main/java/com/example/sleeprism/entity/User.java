package com.example.sleeprism.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder; // Lombok @Builder 임포트
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder; // @SuperBuilder 임포트 추가
import jakarta.persistence.*; // JPA 관련 어노테이션 임포트
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 인터페이스 임포트

import java.time.LocalDateTime;
import java.util.ArrayList; // ArrayList 임포트 추가
import java.util.Collection;
import java.util.Collections; // Collections 임포트
import java.util.List; // List 임포트 추가

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder // <-- @Builder 대신 @SuperBuilder 사용 (BaseTimeEntity 상속 고려)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // AllArgsConstructor는 SuperBuilder와 함께 사용하는 경우 주의 (명시적 생성자가 없어야 함)
public class User extends BaseTimeEntity implements UserDetails { // <-- BaseTimeEntity 상속 명시

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long id;

  @Column(unique = true, nullable = false, length = 100)
  private String email;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(length = 50)
  private String username;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role; // <-- 최상위 UserRole enum 사용

  @Column(name = "social_id", length = 255)
  private String socialId;

  @Enumerated(EnumType.STRING)
  @Column(name = "social_provider", length = 20)
  private SocialProvider socialProvider; // <-- 최상위 SocialProvider enum 사용

  @Enumerated(EnumType.STRING) // <-- String -> Enum 타입으로 변경
  @Column(nullable = false, length = 20)
  private UserStatus status; // <-- 최상위 UserStatus enum 사용

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default // <-- @Builder.Default 추가
  private boolean isDeleted = false;

  // --- 관계 매핑 (Post, Comment, SaleRequest 등과 User 간의 관계) ---
  // User가 작성한 게시글 목록
  @OneToMany(mappedBy = "originalAuthor", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default // @Builder 사용 시 초기화 표현식 무시 경고 방지
  private List<Post> posts = new ArrayList<>();

  // User가 현재 소유한 게시글 목록 (판매를 통해 소유권이 바뀐 경우)
  @OneToMany(mappedBy = "currentOwner", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default // @Builder 사용 시 초기화 표현식 무시 경고 방지
  private List<Post> ownedPosts = new ArrayList<>(); // SaleService의 getOwnedPosts()를 위해 추가

  // User가 작성한 댓글 목록
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default // @Builder 사용 시 초기화 표현식 무시 경고 방지
  private List<Comment> comments = new ArrayList<>();

  // User가 참여한 채팅방 목록
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ChatParticipant> chatParticipants = new ArrayList<>();


  // --- 사용자 정보 업데이트를 위한 커스텀 메서드 ---
  public void updateNicknameAndEmail(String nickname, String email) {
    this.nickname = nickname;
    this.email = email;
  }

  public void updateProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }


  // --- UserDetails 인터페이스 구현 ---
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
  }

  @Override
  public String getUsername() {
    return this.email; // username은 email로 사용
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return !this.isDeleted && this.status == UserStatus.ACTIVE; // 계정 삭제 여부와 상태를 함께 확인
  }
}
