package com.example.sleeprism.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor // @Component 어노테이션이 있으므로 final 필드에 대한 생성자가 자동 주입됩니다.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 모든 요청에 대해 이 필터가 실행됩니다.
   * shouldNotFilter 로직을 제거하여 SecurityConfig에서 경로 관리를 일원화합니다.
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // 1. 요청 헤더에서 JWT 토큰 추출
    String token = getJwtFromRequest(request);

    // 2. 토큰이 존재하고 유효한 경우에만 인증 정보 설정
    try {
      if (token != null && jwtTokenProvider.validateToken(token)) {
        // 토큰이 유효하면 인증 정보를 SecurityContext에 저장
        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("Authentication successful for user: '{}', URI: {}", authentication.getName(), request.getRequestURI());
      }
    } catch (Exception e) {
      // 토큰 유효성 검사 중 예외 발생 시 로그 기록 (연결을 끊지 않고 다음 필터로 계속 진행)
      log.warn("Invalid JWT Token: {} for URI: {}. Error: {}", token, request.getRequestURI(), e.getMessage());
    }

    // 3. 다음 필터로 요청 전달
    filterChain.doFilter(request, response);
  }

  /**
   * 요청 헤더에서 'Authorization' 필드의 Bearer 토큰을 추출하는 헬퍼 메소드
   */
  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  /**
   * shouldNotFilter 메소드를 제거하여 모든 요청이 이 필터를 거치도록 합니다.
   * 이제 특정 경로를 제외하는 로직은 SecurityConfig에서만 관리됩니다.
   */
}
