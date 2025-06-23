package com.example.sleeprism.jwt;

import com.example.sleeprism.entity.User; // <-- 중요: com.example.sleeprism.entity.User 임포트
import com.example.sleeprism.repository.UserRepository; // <-- UserRepository 임포트

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.userdetails.User; // <-- 이 줄은 반드시 제거해야 합니다! (중요)
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long validityInSeconds;
  private final UserDetailsService userDetailsService;

  // 생성자 주입: @Value 값들과 UserRepository를 모두 주입받습니다.
  public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKeyString,
                          @Value("${jwt.token-validity-in-seconds}") long validityInSeconds,
                          UserDetailsService userDetailsService) { // <-- 생성자에 UserDetailsService 추가
    this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    this.validityInSeconds = validityInSeconds;
    this.userDetailsService = userDetailsService; // <-- 주입받은 UserDetailsService 할당
    log.info("JWT Secret Key initialized with size: {} bits", this.secretKey.getEncoded().length * 8);
  }

  // JWT 토큰 생성 (여기서는 UserDetails를 인자로 받지 않으므로 변경 없음)
  public String generateToken(Long userId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + (validityInSeconds * 1000));

    String roles = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));

    log.debug("Generating token for userId: {}, email: {}, username: {}, roles: {}", userId, email, username, roles);
    log.debug("Token issued at: {}, expires at: {}", now, expiryDate);

    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("email", email)
        .claim("username", username)
        .claim("roles", roles)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  // JWT 토큰에서 인증 정보 조회
  public Authentication getAuthentication(String token) {
    try {
      Claims claims = Jwts.parser()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token)
          .getBody();

      log.debug("Claims extracted from token: {}", claims);

      // 토큰의 Subject (userId)를 사용하여 UserDetailsService에서 UserDetails 객체를 로드합니다.
      // 여기서 로드되는 UserDetails는 com.example.sleeprism.entity.User (UserDetails 구현체)가 됩니다.
      UserDetails principal = userDetailsService.loadUserByUsername(claims.get("email", String.class)); // 이메일로 로드

      Collection<? extends GrantedAuthority> authorities = principal.getAuthorities(); // UserDetails에서 권한 가져오기

      log.debug("Principal created: username={}, authorities={}", principal.getUsername(), principal.getAuthorities());

      return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    } catch (Exception e) {
      log.error("Error extracting authentication from token: {}", e.getMessage(), e);
      throw e;
    }
  }



  // JWT 토큰 유효성 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token);
      log.debug("Token is valid.");
      return true;
    } catch (SignatureException ex) {
      log.error("Invalid JWT signature: {}", ex.getMessage());
    } catch (MalformedJwtException ex) {
      log.error("Invalid JWT token: {}", ex.getMessage());
    } catch (ExpiredJwtException ex) {
      log.error("Expired JWT token: {}", ex.getMessage());
    } catch (UnsupportedJwtException ex) {
      log.error("Unsupported JWT token: {}", ex.getMessage());
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string is empty: {}", ex.getMessage());
    } catch (Exception ex) {
      log.error("Unexpected error during JWT token validation: {}", ex.getMessage(), ex);
    }
    return false;
  }

  // 토큰에서 사용자 ID 추출
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
    return Long.parseLong(claims.getSubject());
  }

  // 토큰에서 사용자 이메일 추출
  public String getUserEmailFromToken(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
    return claims.get("email", String.class);
  }
}
