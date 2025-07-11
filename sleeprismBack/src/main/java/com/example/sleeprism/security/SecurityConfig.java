package com.example.sleeprism.security;

import com.example.sleeprism.jwt.JwtAuthenticationFilter;
import com.example.sleeprism.jwt.JwtTokenProvider;
import com.example.sleeprism.oauth2.CustomOAuth2UserService;
import com.example.sleeprism.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final CustomOAuth2UserService customOAuth2UserService;

  public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                        OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                        CustomOAuth2UserService customOAuth2UserService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
    this.customOAuth2UserService = customOAuth2UserService;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // ✅ 수정된 부분: 모든 Origin에서의 요청을 허용하도록 변경합니다.
    // 이렇게 하면 로컬에서 HTML 파일을 열 때 발생하는 'origin: null' 문제와
    // React 개발 서버(localhost:5173 등)에서의 요청을 모두 처리할 수 있습니다.
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public CorsFilter corsFilter() {
    return new CorsFilter(corsConfigurationSource());
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS 설정을 명시적으로 적용
        .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**", "/api/**"))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(authorize -> authorize
            // 웹소켓 연결 경로는 명시적으로 모두 허용합니다.
            .requestMatchers("/ws/**").permitAll()
            // 기존의 다른 허용 경로들
            .requestMatchers("/", "/error", "/api/auth/**", "/api/users/signup", "/api/users/signin",
                "/oauth2/**", "/login/**", "/api/files/**", "/api/posts/files/**", "/test.html", "/api/sounds/**"
            ).permitAll()
            .requestMatchers("/api/posts", "/api/posts/{postId}").permitAll()
            .requestMatchers("/files/profile-images/**", "/api/comments/post/**").permitAll()
            .requestMatchers("/files/**", "/images/**").permitAll()
            // 나머지 모든 요청은 인증을 요구합니다.
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2AuthenticationSuccessHandler)
        );

    return http.build();
  }
}