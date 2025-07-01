package com.example.sleeprism.security;

import com.example.sleeprism.jwt.JwtAuthenticationFilter;
import com.example.sleeprism.jwt.JwtTokenProvider;
import com.example.sleeprism.oauth2.CustomOAuth2UserService;
import com.example.sleeprism.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSecurity
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class SecurityConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtTokenProvider jwtTokenProvider;
  // UserDetailsService는 WebSocketAuthChannelInterceptor의 생성자로 직접 주입합니다.
  private final UserDetailsService userDetailsService;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

  // STOMP 세션 ID를 Key로, Authentication 객체를 Value로 저장하는 맵 빈을 정의합니다.
  // 이 맵은 웹소켓 세션 간에 인증 정보를 유지하는 데 사용됩니다.
  @Bean
  public Map<String, Authentication> simpUserAuthenticationMap() {
    return new ConcurrentHashMap<>();
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
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter(jwtTokenProvider);
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring()
        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
        .requestMatchers("/test.html", "/sleeprism/test.html")
        .requestMatchers("/test_websocket.html", "/sleeprism/test_websocket.html")
        .requestMatchers("/ws/**", "/sleeprism/ws/**");
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(form -> form.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/*", "/api/files/**", "/api/comments/post/*", "/api/comments/files/**", "/api/comments/files/comments/**", "/api/sounds/**", "/api/sounds/internal/**").permitAll()
            .requestMatchers("/", "/error", "/api/auth/**", "/api/users/signup", "/api/users/signin", "/oauth2/**", "/login/**", "/api/posts/files/**").permitAll()
            .requestMatchers("/files/**", "/api/freesound-search/**").permitAll()
            .requestMatchers("/api/users/profile").authenticated()
            .requestMatchers("/api/chats/**").authenticated()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .exceptionHandling(exception -> exception.authenticationEntryPoint(new CustomAuthenticationEntryPoint()))
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2AuthenticationSuccessHandler)
        );

    return http.build();
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
    registry.addEndpoint("/raw-ws")
        .setAllowedOriginPatterns("*");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // 이제 WebSocketAuthChannelInterceptor를 직접 생성하여 맵을 주입합니다.
    registration.interceptors(
        new WebSocketAuthChannelInterceptor(jwtTokenProvider, userDetailsService, simpUserAuthenticationMap())
    );
  }

  @Bean
  public AuthenticationPrincipalArgumentResolver authenticationPrincipalArgumentResolver() {
    return new AuthenticationPrincipalArgumentResolver();
  }

}
