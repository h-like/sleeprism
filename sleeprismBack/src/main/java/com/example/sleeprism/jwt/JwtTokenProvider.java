package com.example.sleeprism.jwt;

import com.example.sleeprism.entity.User; // User 엔티티 임포트
import com.example.sleeprism.repository.UserRepository; // UserRepository 임포트 제거 (필요 없음)

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException; // SignatureException 임포트
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // UserDetailsService 임포트
import org.springframework.stereotype.Component;

import java.security.Key; // javax.crypto.SecretKey 대신 java.security.Key 사용
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

  private final Key secretKey; // SecretKey 대신 Key 사용
  private final long validityInMilliseconds; // validityInSeconds 대신 validityInMilliseconds
  private final UserDetailsService userDetailsService; // UserDetailsService 필드

  // 생성자 주입: @Value 값들과 UserDetailsService를 모두 주입받습니다.
  public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKeyString,
                          @Value("${jwt.expiration-time}") long expirationTime, // properties와 일치
                          UserDetailsService userDetailsService) { // <-- 생성자에 UserDetailsService 추가
    this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    this.validityInMilliseconds = expirationTime; // properties와 일치
    this.userDetailsService = userDetailsService; // <-- 주입받은 UserDetailsService 할당
    log.info("JWT Secret Key initialized with size: {} bits", this.secretKey.getEncoded().length * 8);
  }

  // JWT 토큰 생성
  public String generateToken(Long userId, String email, String nickname, Collection<? extends GrantedAuthority> authorities) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + validityInMilliseconds); // 밀리초 사용

    String roles = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));

    log.debug("Generating token for userId: {}, email: {}, nickname: {}, roles: {}", userId, email, nickname, roles);
    log.debug("Token issued at: {}, expires at: {}", now, expiryDate);

    // builder().subject() 대신 .setSubject() 사용
    return Jwts.builder()
        .setSubject(String.valueOf(email)) // Subject는 email로 설정하는 것이 일반적입니다.
        .claim("userId", userId) // 사용자 ID를 별도의 클레임으로 추가
        .claim("nickname", nickname) // 닉네임 클레임 추가
        .claim("roles", roles)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  // JWT 토큰에서 인증 정보 조회 (최신 JJWT 문법 적용)
  public Authentication getAuthentication(String token) {
    try {
      Claims claims = Jwts.parserBuilder() // <-- Jwts.parserBuilder() 사용
          .setSigningKey(secretKey)
          .build() // <-- .build() 호출 추가
          .parseClaimsJws(token)
          .getBody();

      log.debug("Claims extracted from token: {}", claims);

      // 토큰의 email 클레임을 사용하여 UserDetailsService에서 UserDetails 객체를 로드합니다.
      String userEmail = claims.getSubject(); // subject를 email로 설정했으므로 getSubject() 사용
      // claims.get("email", String.class); 로 명시적으로 email 클레임을 가져올 수도 있습니다.
      UserDetails principal = userDetailsService.loadUserByUsername(userEmail);

      Collection<? extends GrantedAuthority> authorities = principal.getAuthorities(); // UserDetails에서 권한 가져오기

      log.debug("Principal created: username={}, authorities={}", principal.getUsername(), principal.getAuthorities());

      return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    } catch (Exception e) {
      log.error("Error extracting authentication from token: {}", e.getMessage(), e);
      throw e;
    }
  }

  // JWT 토큰 유효성 검증 (최신 JJWT 문법 적용)
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder() // <-- Jwts.parserBuilder() 사용
          .setSigningKey(secretKey)
          .build() // <-- .build() 호출 추가
          .parseClaimsJws(token);
      log.debug("Token is valid.");
      return true;
    } catch (SignatureException ex) { // SignatureException 사용
      log.error("Invalid JWT signature: {}", ex.getMessage());
    } catch (MalformedJwtException ex) {
      log.error("Invalid JWT token: {}", ex.getMessage());
    } catch (ExpiredJwtException ex) {
      log.error("Expired JWT token: {}", ex.getMessage());
    } catch (UnsupportedJwtException ex) {
      log.error("Unsupported JWT token: {}", ex.getMessage());
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string is empty: {}", ex.getMessage());
    } catch (Exception ex) { // 예상치 못한 다른 예외 처리
      log.error("Unexpected error during JWT token validation: {}", ex.getMessage(), ex);
    }
    return false;
  }

  // 토큰에서 사용자 ID 추출 (최신 JJWT 문법 적용)
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parserBuilder() // <-- Jwts.parserBuilder() 사용
        .setSigningKey(secretKey)
        .build() // <-- .build() 호출 추가
        .parseClaimsJws(token)
        .getBody();
    return claims.get("userId", Long.class); // "userId" 클레임에서 가져옴
  }

  // 토큰에서 사용자 이메일 추출 (최신 JJWT 문법 적용)
  public String getUserEmailFromToken(String token) {
    Claims claims = Jwts.parserBuilder() // <-- Jwts.parserBuilder() 사용
        .setSigningKey(secretKey)
        .build() // <-- .build() 호출 추가
        .parseClaimsJws(token)
        .getBody();
    return claims.getSubject(); // Subject가 email이므로 getSubject() 사용
  }
}
