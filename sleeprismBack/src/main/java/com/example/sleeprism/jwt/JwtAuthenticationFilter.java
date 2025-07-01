// src/main/java/com/example/sleeprism/jwt/JwtAuthenticationFilter.java
package com.example.sleeprism.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  // 이 필터가 JWT 토큰 검사를 수행하지 않을 (건너뛸) URI 패턴 목록입니다.
  // 이 경로는 Context Path가 제거된 상태로 매칭될 것입니다.
  private static final List<String> EXCLUDE_URLS = Arrays.asList(
      "/", // 루트 경로 (Context Path 없이)
      "/error",
      "/api/auth/**", // 인증 관련 API (로그인, 회원가입 등)
      "/api/users/signup", // 회원가입
      "/api/users/signin", // 로그인 (POST)
      "/oauth2/**", // OAuth2 로그인 관련 경로
      "/login/**",  // OAuth2 로그인 관련 경로
      "/api/files/**", // 일반 파일 접근
      "/api/posts/files/**", // 게시글 첨부 파일(이미지 등) 제공 URL
      "/test.html", // <-- test.html 파일 경로 추가 (Context Path 제외)
      "/ws/**",     // <-- 웹소켓 핸드셰이크 경로 추가 (Context Path 제외)
      "/raw-ws/**"  // <-- 일반 웹소켓 엔드포인트 추가 (Context Path 제외)
  );

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7); // "Bearer " 이후의 토큰만 추출
    }
    return null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    log.info("Processing JWT authentication for URI: {}", requestURI);

    try {
      String jwt = getJwtFromRequest(request);
      log.debug("Extracted JWT: {}", jwt != null ? jwt.substring(0, Math.min(jwt.length(), 30)) + "..." : "No JWT found");

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
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String requestUri = request.getRequestURI();
    String requestMethod = request.getMethod();

    // Context Path 제거: /sleeprism/api/posts -> /api/posts 로 변환
    String pathWithoutContext = requestUri.startsWith("/sleeprism") ?
        requestUri.substring("/sleeprism".length()) :
        requestUri;
    // Context Path 제거 후 빈 문자열이 되는 경우 (예: /sleeprism 만 요청한 경우) "/"로 설정
    if (pathWithoutContext.isEmpty()) {
      pathWithoutContext = "/";
    }

    log.debug("shouldNotFilter check for URI: {}, Method: {}, Path without context: {}", requestUri, requestMethod, pathWithoutContext);


    // OPTIONS 요청은 항상 필터를 건너뜝니다. (CORS Preflight)
    if (requestMethod.equals(HttpMethod.OPTIONS.name())) {
      log.info("Skipping JWT filter for OPTIONS request: {}", requestUri);
      return true;
    }

    // 게시글 목록 및 상세 조회 (GET 요청)는 JWT 필터를 건너뜝니다.
    if (requestMethod.equals(HttpMethod.GET.name())) {
      if (pathMatcher.match("/api/posts", pathWithoutContext) ||
          pathMatcher.match("/api/posts/*", pathWithoutContext) ||
          pathMatcher.match("/api/comments/files/comment/*", pathWithoutContext) ||
          pathMatcher.match("/api/sounds/*", pathWithoutContext) ||
          pathMatcher.match("/api/comments/post/*", pathWithoutContext)) {
        log.info("Skipping JWT filter for GET request to public posts URI (after context path removal): {}", pathWithoutContext);
        return true;
      }
    }

    // EXCLUDE_URLS에 명시된 다른 공개 경로들은 HTTP 메서드와 상관없이 필터를 건너뜁니다.
    for (String excludeUrlPattern : EXCLUDE_URLS) {
      log.debug("  Matching against pattern: {}", excludeUrlPattern);
      if (pathMatcher.match(excludeUrlPattern, pathWithoutContext)) {
        log.info("Skipping JWT filter for general public URI (after context path removal): {} matched by pattern {}", pathWithoutContext, excludeUrlPattern);
        return true;
      }
    }

    log.info("JWT filter WILL BE APPLIED for URI: {}", requestUri);
    return false; // 위에 해당하지 않는 모든 요청에 대해서는 필터를 실행합니다.
  }
}
