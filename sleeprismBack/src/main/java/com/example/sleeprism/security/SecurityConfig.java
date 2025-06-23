package com.example.sleeprism.security;

import com.example.sleeprism.jwt.JwtAuthenticationFilter;
import com.example.sleeprism.jwt.JwtTokenProvider;
import com.example.sleeprism.oauth2.CustomOAuth2UserService;
import com.example.sleeprism.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // HttpMethod 임포트 확인
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter(jwtTokenProvider);
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring()
        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico");
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(form -> form.disable())
        .httpBasic(httpBasic -> httpBasic.disable())

        // CORS 설정을 활성화합니다.
        // 이 부분이 있어야 CorsConfig에서 정의한 규칙이 적용됩니다.
        .cors(cors -> {
        })

        .authorizeHttpRequests(authorize -> authorize
                // 1. 모든 OPTIONS 요청을 인증 없이 허용 (가장 상단)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 2. 게시글 조회 (GET) 허용
                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/*").permitAll()
                // 3. 댓글 조회 (GET) 허용
                .requestMatchers(HttpMethod.GET, "/api/comments/post/*").permitAll()

                // 3. 다른 일반적인 permitAll() 경로들
                .requestMatchers(
                    "/",
                    "/error",
                    "/api/auth/**",
                    "/api/users/signup",
                    "/api/users/signin",
                    "/oauth2/**",
                    "/login/**",
                    "/api/files/**", // 일반 파일 (예: Postman에서 테스트용)
                    "/api/posts/files/**" // 게시글 첨부 이미지 등
                ).permitAll()

                // 3. 관리자 경로 (인증 및 역할 필요)
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 4. 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
        )
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
        )
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2AuthenticationSuccessHandler)
        );

    return http.build();
  }
}
