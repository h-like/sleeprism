package com.example.sleeprism.security;

import com.example.sleeprism.jwt.JwtAuthenticationFilter;
import com.example.sleeprism.jwt.JwtTokenProvider;
import com.example.sleeprism.oauth2.CustomOAuth2UserService;
import com.example.sleeprism.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정을 정의하는 클래스입니다.
 * JWT 기반 인증 및 OAuth2 로그인을 구성합니다.
 */
@Configuration // 스프링 설정 클래스임을 나타냅니다.
@EnableWebSecurity // Spring Security 활성화
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성합니다.
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

  /**
   * 비밀번호 암호화를 위한 PasswordEncoder Bean을 등록합니다.
   * BCrypt 해싱 알고리즘을 사용합니다.
   *
   * @return BCryptPasswordEncoder 인스턴스
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * AuthenticationManager Bean을 등록합니다.
   * Spring Security의 인증 처리를 담당합니다.
   *
   * @param authenticationConfiguration AuthenticationConfiguration 객체
   * @return AuthenticationManager 인스턴스
   * @throws Exception 인증 관리자 획득 중 예외 발생 시
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * JwtAuthenticationFilter Bean을 등록합니다.
   * 이 필터는 HTTP 요청 헤더에서 JWT 토큰을 추출하고 유효성을 검사하여 인증을 처리합니다.
   *
   * @return JwtAuthenticationFilter 인스턴스
   */
  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    // JwtTokenProvider를 주입받는 생성자를 사용하여 필터 인스턴스를 생성합니다.
    return new JwtAuthenticationFilter(jwtTokenProvider);
  }

  /**
   * Spring Security 필터 체인을 완전히 우회할 경로를 설정합니다.
   * 주로 정적 자원(CSS, JS, 이미지 등)에 사용됩니다.
   *
   * @return WebSecurityCustomizer 인스턴스
   */
  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring()
        // 순수한 정적 자원들만 필터 체인 우회 대상으로 남깁니다.
        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico");
  }

  /**
   * Spring Security 필터 체인을 구성합니다.
   *
   * @param http HttpSecurity 객체
   * @return 구성된 SecurityFilterChain
   * @throws Exception 보안 설정 중 예외 발생 시
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF (Cross-Site Request Forgery) 보호를 비활성화합니다.
        // JWT와 같은 토큰 기반 인증에서는 세션을 사용하지 않으므로 CSRF 보호가 필요 없습니다.
        .csrf(csrf -> csrf.disable())
        // 세션 관리를 설정합니다.
        // JWT를 사용하므로 세션을 생성하거나 사용하지 않는 STATELESS 정책을 사용합니다.
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 폼 로그인 기능을 비활성화합니다. (REST API이므로)
        .formLogin(form -> form.disable())
        // HTTP Basic 인증을 비활성화합니다. (REST API이므로)
        .httpBasic(httpBasic -> httpBasic.disable())
        // HTTP 요청에 대한 권한 부여 규칙을 정의합니다.
        .authorizeHttpRequests(authorize -> authorize
            // 다음 경로들은 인증 없이도 접근을 허용합니다.
            .requestMatchers(
                "/",
                "/sleeprism/**", // 컨텍스트 경로를 포함한 모든 하위 경로
                "/error",
                "/api/auth/**", // 인증 관련 API (로그인, 회원가입)
                "/api/files/**", // 일반 파일 업로드/다운로드 API (Postman의 FileController)
                "/api/users/signup", // 회원가입
                "/api/users/signin", // 로그인 (POST)
                "/oauth2/**", // OAuth2 로그인 관련 경로
                "/login/**",  // OAuth2 로그인 관련 경로
                // 게시글 첨부 파일 제공 URL은 인증 없이 접근 허용 (이미지 로딩 등)
                "/api/posts/files/**" // <-- 오타 수정 및 public으로 변경
            ).permitAll()
            // "/admin/**" 경로에는 "ADMIN" 역할을 가진 사용자만 접근을 허용합니다.
            .requestMatchers("/admin/**").hasRole("ADMIN")
            // 나머지 모든 요청은 인증된 사용자만 접근을 허용합니다.
            // 여기에는 게시글 생성/수정/삭제 (/api/posts, /api/posts/{postId} PUT/DELETE)
            // 게시글 조회 (/api/posts, /api/posts/{postId} GET, /api/posts/category/{category} GET) 등
            // 인증이 필요한 API가 포함됩니다.
            .anyRequest().authenticated()
        )
        // 예외 처리를 설정합니다.
        .exceptionHandling(exception -> exception
                // 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출될 AuthenticationEntryPoint를 설정합니다.
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
            // .accessDeniedHandler(new CustomAccessDeniedHandler()) // 권한이 없는 사용자가 접근할 때 (필요시 추가)
        )
        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가합니다.
        // 이렇게 함으로써 JWT 토큰을 먼저 검사하고, 유효한 경우 인증을 완료합니다.
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        // OAuth2 로그인 설정을 구성합니다.
        .oauth2Login(oauth2 -> oauth2
                // 사용자 정보 엔드포인트 설정을 시작합니다.
                .userInfoEndpoint(userInfo -> userInfo
                    // CustomOAuth2UserService를 등록하여 소셜 로그인 사용자 정보를 처리합니다.
                    .userService(customOAuth2UserService)
                )
                // OAuth2 인증 성공 시 호출될 핸들러를 설정합니다.
                .successHandler(oAuth2AuthenticationSuccessHandler)
            // .failureHandler(oAuth2AuthenticationFailureHandler) // OAuth2 인증 실패 핸들러 (필요시 추가)
        );

    return http.build(); // 구성된 HttpSecurity 객체를 빌드하여 SecurityFilterChain을 반환합니다.
  }
}
