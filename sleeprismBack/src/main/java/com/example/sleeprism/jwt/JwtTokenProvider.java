package com.example.sleeprism.jwt;

import com.example.sleeprism.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

  private final Key secretKey;
  private final long validityInMilliseconds;
  private final UserDetailsService userDetailsService;

  public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKeyString,
                          @Value("${jwt.expiration-time}") long expirationTime,
                          UserDetailsService userDetailsService) {
    this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    this.validityInMilliseconds = expirationTime;
    this.userDetailsService = userDetailsService;
    log.info("JWT Secret Key initialized with size: {} bits", this.secretKey.getEncoded().length * 8);
  }

  public String generateToken(Long userId, String email, String nickname, Collection<? extends GrantedAuthority> authorities) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + validityInMilliseconds);

    String roles = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));

    log.debug("Generating token for userId: {}, email: {}, nickname: {}, roles: {}", userId, email, nickname, roles);
    log.debug("Token issued at: {}, expires at: {}", now, expiryDate);

    return Jwts.builder()
        .setSubject(String.valueOf(email))
        .claim("userId", userId)
        .claim("nickname", nickname)
        .claim("roles", roles)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Authentication getAuthentication(String token) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token)
          .getBody();

      log.debug("Claims extracted from token: {}", claims);

      String userEmail = claims.getSubject();
      UserDetails principal = userDetailsService.loadUserByUsername(userEmail);

      Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

      log.debug("Principal created: username={}, authorities={}", principal.getUsername(), principal.getAuthorities());

      return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    } catch (Exception e) {
      log.error("Error extracting authentication from token: {}", e.getMessage(), e);
      throw e;
    }
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token);
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

  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
    return claims.get("userId", Long.class);
  }

  public String getUserEmailFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
    return claims.getSubject(); // JWT의 subject 클레임에서 이메일 추출
  }

}
