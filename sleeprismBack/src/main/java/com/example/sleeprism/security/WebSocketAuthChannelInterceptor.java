package com.example.sleeprism.security;

import com.example.sleeprism.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor; // MessageHeaderAccessor 추가
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor; // SimpMessageHeaderAccessor 추가


import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 메시지 채널에 대한 인터셉터로, STOMP CONNECT 시 JWT 인증을 처리하고,
 * 이후 메시지(SUBSCRIBE, SEND)에 대해 인증 컨텍스트를 유지 관리합니다.
 */
@Component // 스프링 빈으로 등록
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  private final Map<String, Authentication> simpUserAuthenticationMap;

  // 생성자 주입
  public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService, Map<String, Authentication> simpUserAuthenticationMap) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userDetailsService = userDetailsService;
    this.simpUserAuthenticationMap = simpUserAuthenticationMap;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    // 기존 accessor를 복사하여 새로운 MessageHeaderAccessor를 생성합니다.
    // 이렇게 해야 메시지 헤더를 안전하게 수정할 수 있습니다.
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      System.err.println("ERROR: StompHeaderAccessor is null. Cannot process message.");
      return message; // 또는 null을 반환하여 메시지 처리 중단
    }

    String sessionId = accessor.getSessionId();

    System.out.println("DEBUG: Inbound Channel Interceptor - Command: " + accessor.getCommand());
    System.out.println("DEBUG: Inbound Channel Interceptor - Session ID: " + sessionId);

    // CONNECT 명령 처리 (초기 인증 및 맵에 저장)
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String token = accessor.getFirstNativeHeader("Authorization");
      System.out.println("DEBUG: CONNECT Command received. Authorization header: " + (token != null ? "Present" : "Missing"));

      if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
        System.out.println("DEBUG: Extracted JWT Token: " + token.substring(0, Math.min(token.length(), 20)) + "...");

        try {
          if (jwtTokenProvider.validateToken(token)) {
            System.out.println("DEBUG: JWT Token is valid.");
            String userIdentifier = jwtTokenProvider.getUserEmailFromToken(token);

            if (userIdentifier == null || userIdentifier.isEmpty()) {
              System.err.println("ERROR: User identifier from token is null or empty. Rejecting CONNECT.");
              return null;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userIdentifier);

            if (userDetails == null) {
              System.err.println("ERROR: UserDetails could not be loaded for: " + userIdentifier + ". Rejecting CONNECT.");
              return null;
            }

            System.out.println("DEBUG: UserDetails loaded for: " + userDetails.getUsername());

            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // STOMP 세션에 Principal 설정 (핵심)
            accessor.setUser(authentication);
            // SecurityContextHolder에도 인증 설정 (스레드 로컬 컨텍스트를 위해)
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 맵에 세션ID와 Authentication을 저장합니다.
            if (sessionId != null) {
              simpUserAuthenticationMap.put(sessionId, authentication);
              System.out.println("DEBUG: CONNECT -> Stored Authentication for session: " + sessionId + " User: " + authentication.getName());
            }
            System.out.println("DEBUG: Authentication set for STOMP session and SecurityContextHolder. User: " + authentication.getName());
          } else {
            System.out.println("DEBUG: JWT Token is invalid. Rejecting CONNECT.");
            return null;
          }
        } catch (Exception e) {
          System.err.println("ERROR: WebSocket Authentication failed during CONNECT: " + e.getClass().getSimpleName() + " - " + e.getMessage());
          e.printStackTrace();
          return null;
        }
      } else {
        System.out.println("DEBUG: Authorization header is missing or not 'Bearer ' type for CONNECT. Rejecting CONNECT.");
        return null;
      }
    }
    // DISCONNECT 명령 처리 (맵에서 제거)
    else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      if (sessionId != null) {
        simpUserAuthenticationMap.remove(sessionId);
        System.out.println("DEBUG: DISCONNECT -> Removed Authentication for session: " + sessionId);
        // DISCONNECT 시 SecurityContextHolder 클리어
        SecurityContextHolder.clearContext();
      }
    }
    // CONNECT 또는 DISCONNECT 외 다른 명령 (SUBSCRIBE, SEND 등) 처리
    else {
      Authentication authentication = simpUserAuthenticationMap.get(sessionId);
      if (authentication != null) {
        // STOMP 세션에 Principal 다시 설정 (가장 중요)
        accessor.setUser(authentication);

        // SecurityContextHolder에도 설정 (스레드 로컬 컨텍스트를 위해)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("DEBUG: " + accessor.getCommand() + " -> Retrieved and set Authentication for session: " + sessionId + " User: " + authentication.getName());
      } else {
        System.out.println("DEBUG: " + accessor.getCommand() + " -> No Authentication found in map for session: " + sessionId + ". This might indicate an unauthenticated or expired session.");
        // 인증 정보가 없으면, SecurityContextHolder를 클리어하여 인증되지 않은 상태로 만듭니다.
        SecurityContextHolder.clearContext();
      }
    }
    // 수정된 메시지를 반환합니다.
    return message;
  }
}
