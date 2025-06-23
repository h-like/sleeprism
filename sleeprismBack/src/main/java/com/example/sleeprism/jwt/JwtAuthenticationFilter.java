package com.example.sleeprism.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    log.info("Processing JWT authentication for URI: {}", requestURI);

    try {
      String jwt = getJwtFromRequest(request); // HTTP 요청에서 JWT 토큰 추출
      log.debug("Extracted JWT: {}", jwt != null ? jwt.substring(0, Math.min(jwt.length(), 30)) + "..." : "No JWT found"); // 토큰의 일부만 로깅

      // JWT 토큰이 존재하고 유효한 경우
      if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
        log.info("JWT token is present and valid. Attempting to set authentication.");
        Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("Authentication set successfully for user: {}", authentication.getName());
      } else if (jwt != null) {
        log.warn("JWT token found but validation failed for URI: {}", requestURI);
      } else {
        log.info("No JWT token found in request for URI: {}", requestURI);
      }
    } catch (Exception ex) {
      log.error("Could not set user authentication in security context for URI: {}. Error: {}", requestURI, ex.getMessage(), ex);
      // 이 예외가 발생하면 인증이 설정되지 않으므로 401이 반환될 수 있습니다.
      // CustomAuthenticationEntryPoint가 잡아서 처리할 것입니다.
    }

    filterChain.doFilter(request, response);
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      log.debug("Authorization header found: {}", bearerToken);
      return bearerToken.substring(7);
    }
    log.debug("No 'Bearer ' token found in Authorization header.");
    return null;
  }
}
