// src/main/java/com/example/sleeprism/entity/User.java
package com.example.sleeprism.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // isDeleted 필드의 setter를 위해 추가

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 사용자 정보를 담는 엔티티 클래스입니다.
 * Spring Security의 UserDetails 인터페이스를 구현하여 인증 및 권한 관리에 사용됩니다.
 */
@Getter // Lombok을 사용하여 모든 필드에 대한 getter 메서드를 자동으로 생성합니다.
@Setter // Lombok을 사용하여 모든 필드에 대한 setter 메서드를 자동으로 생성합니다. (isDeleted 필드의 setDeleted를 위해)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok을 사용하여 인자 없는 protected 생성자를 자동으로 생성합니다.
@Entity // 이 클래스가 JPA 엔티티임을 나타냅니다.
@Table(name = "users") // 데이터베이스 테이블 이름을 'users'로 지정합니다.
public class User extends BaseTimeEntity implements UserDetails { // BaseTimeEntity를 상속받아 생성 및 수정 시간을 자동으로 관리합니다.
  @Id // 기본 키(Primary Key)임을 나타냅니다.
  @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 생성을 데이터베이스에 위임합니다. (Auto-increment)
  @Column(name = "user_id") // 데이터베이스 컬럼 이름을 'user_id'로 지정합니다.
  private Long id;

  @Column(nullable = false, unique = true, length = 100) // null을 허용하지 않고, 유니크하며, 최대 길이 100인 컬럼입니다.
  private String email; // 로그인 ID로 사용될 수 있는 이메일 주소입니다.

  @Column(nullable = false, unique = true, length = 100) // null을 허용하지 않고, 유니크하며, 최대 길이 100인 컬럼입니다.
  private String username; // 사용자 이름 (고유한 식별자, 변경 불가능할 수 있음)

  @Column(nullable = false) // null을 허용하지 않는 컬럼입니다.
  private String password; // 암호화된 비밀번호입니다.

  @Column(nullable = false, unique = true, length = 100) // null을 허용하지 않고, 유니크하며, 최대 길이 100인 컬럼입니다.
  private String nickname; // 사용자 닉네임 (표시 이름, 변경 가능)

  @Column(length = 255) // 프로필 이미지 URL (nullable)
  private String profileImageUrl;

  @Enumerated(EnumType.STRING) // Enum 타입을 문자열로 데이터베이스에 저장합니다.
  @Column(nullable = false) // null을 허용하지 않는 컬럼입니다.
  private UserRole role; // 사용자 권한 (UserRole enum)

  @Enumerated(EnumType.STRING) // Enum 타입을 문자열로 데이터베이스에 저장합니다.
  @Column(nullable = false) // null을 허용하지 않는 컬럼입니다.
  private UserStatus status; // 사용자 계정 상태 (UserStatus enum)

  @Column(length = 50) // 소셜 로그인 제공자 (예: "kakao", "google")
  private String socialProvider;

  @Column(unique = true, length = 255) // 소셜 로그인 고유 ID (각 소셜 서비스별로 고유해야 함)
  private String socialId; // <-- 이 필드를 추가합니다.

  @Column(name = "is_deleted", nullable = false) // 'is_deleted' 컬럼 이름을 사용하고 null을 허용하지 않습니다.
  private boolean isDeleted = false; // 사용자 삭제 여부를 나타냅니다. 기본값은 false입니다.

  // 연관관계 매핑: User와 Post는 1:N 관계입니다.
  @OneToMany(mappedBy = "originalAuthor", cascade = CascadeType.ALL, orphanRemoval = true) // Post 엔티티의 필드명 변경에 맞춰 'user' -> 'originalAuthor'로 변경
  private List<Post> posts = new ArrayList<>();

  // **추가: 구매/소유한 게시물 목록**
  @OneToMany(mappedBy = "currentOwner", cascade = CascadeType.ALL) // 소유권은 삭제해도 원본 포스트는 유지되어야 하므로 orphanRemoval = true는 적절치 않음.
  private List<Post> ownedPosts = new ArrayList<>(); // 현재 사용자가 소유한 게시물 목록


  // 연관관계 매핑: User와 Comment는 1:N 관계입니다.
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments = new ArrayList<>();

  // 연관관계 매핑: User와 PostLike는 1:N 관계입니다.
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PostLike> likes = new ArrayList<>();

  // 연관관계 매핑: User와 Bookmark는 1:N 관계입니다.
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Bookmark> bookmarks = new ArrayList<>();

  /**
   * Lombok의 @Builder를 사용하여 사용자 엔티티를 생성하는 생성자입니다.
   *
   * @param email 사용자 이메일
   * @param username 사용자 이름 (고유 식별자)
   * @param password 사용자 비밀번호 (암호화된 상태)
   * @param nickname 사용자 닉네임 (표시 이름)
   * @param profileImageUrl 프로필 이미지 URL
   * @param role 사용자 권한 (UserRole enum)
   * @param status 사용자 계정 상태 (UserStatus enum)
   * @param socialProvider 소셜 로그인 제공자 (예: "kakao", "google")
   * @param socialId 소셜 로그인 고유 ID
   */
  @Builder
  public User(String email, String username, String password, String nickname,
              String profileImageUrl, UserRole role, UserStatus status,
              String socialProvider, String socialId) { // <-- socialId 인자를 추가합니다.
    this.email = email;
    this.username = username;
    this.password = password;
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
    this.role = role;
    this.status = status;
    this.socialProvider = socialProvider;
    this.socialId = socialId; // <-- socialId 필드를 초기화합니다.
    this.isDeleted = false; // 기본값 설정
  }

  /**
   * 사용자 닉네임과 이메일을 업데이트합니다.
   *
   * @param nickname 새로운 닉네임
   * @param email 새로운 이메일
   */
  public void updateNicknameAndEmail(String nickname, String email) {
    this.nickname = nickname;
    this.email = email;
  }

  /**
   * 사용자 프로필 이미지 URL을 업데이트합니다.
   *
   * @param profileImageUrl 새로운 프로필 이미지 URL
   */
  public void updateProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }


  // --- UserDetails 인터페이스 구현 메소드 ---

  /**
   * 사용자의 권한 목록을 반환합니다.
   * Spring Security에서 사용자의 권한을 확인하는 데 사용됩니다.
   *
   * @return 권한 목록 (예: ROLE_USER, ROLE_ADMIN)
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // 사용자의 UserRole을 "ROLE_" 접두사와 함께 SimpleGrantedAuthority로 변환하여 반환합니다.
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  /**
   * Spring Security에서 사용되는 사용자 이름을 반환합니다.
   * 여기서는 이메일을 사용자 이름으로 사용합니다.
   *
   * @return 사용자 이름 (이메일)
   */
  @Override
  public String getUsername() {
    return email; // Spring Security에서 사용되는 username은 실제 로그인 ID를 의미합니다.
  }

  /**
   * 사용자의 비밀번호를 반환합니다.
   *
   * @return 비밀번호 (암호화된 상태)
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * 계정 만료 여부를 반환합니다.
   * (true = 만료되지 않음)
   *
   * @return 항상 true (만료되지 않음)
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 계정 잠금 여부를 반환합니다.
   * (true = 잠금되지 않음)
   *
   * @return 항상 true (잠금되지 않음)
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * 비밀번호 만료 여부를 반환합니다.
   * (true = 만료되지 않음)
   *
   * @return 항상 true (만료되지 않음)
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 계정 활성화 여부를 반환합니다.
   * (true = 활성화됨)
   *
   * @return isDeleted 필드에 따라 활성화 여부 반환 (삭제되지 않았다면 true)
   */
  @Override
  public boolean isEnabled() {
    return !isDeleted; // 삭제된 사용자는 비활성화됩니다.
  }
}
