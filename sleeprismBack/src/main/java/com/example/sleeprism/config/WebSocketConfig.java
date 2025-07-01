package com.example.sleeprism.config;

import com.example.sleeprism.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal; // Principal import 추가
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
    System.out.println("백엔드: STOMP 엔드포인트 '/ws' 등록됨.");

    registry.addEndpoint("/raw-ws")
        .setAllowedOriginPatterns("*");
    System.out.println("백엔드: STOMP 엔드포인트 '/raw-ws' 등록됨 (일반 WebSocket).");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        System.out.println("DEBUG: Inbound Channel Interceptor - Command: " + accessor.getCommand());
        System.out.println("DEBUG: Inbound Channel Interceptor - Session ID: " + accessor.getSessionId());

        // CONNECT 명령 처리 (초기 인증)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          String token = accessor.getFirstNativeHeader("Authorization");
          System.out.println("DEBUG: CONNECT Command received. Authorization header: " + (token != null ? "Present" : "Missing"));

          if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            System.out.println("DEBUG: Extracted JWT Token: " + token.substring(0, Math.min(token.length(), 20)) + "..."); // 토큰 앞부분만 로깅

            try {
              if (jwtTokenProvider.validateToken(token)) {
                System.out.println("DEBUG: JWT Token is valid.");
                String userIdentifier = jwtTokenProvider.getUserEmailFromToken(token); // 또는 getUsernameFromJWT, getUserIdFromJWT 등
                System.out.println("DEBUG: User Identifier from token: " + userIdentifier);

                UserDetails userDetails = userDetailsService.loadUserByUsername(userIdentifier);
                System.out.println("DEBUG: UserDetails loaded for: " + userDetails.getUsername());

                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContextHolder에 인증 설정
                accessor.setUser(authentication); // STOMP 세션에 인증 정보 설정 (이후 메시지에 사용될 Principal)
                System.out.println("DEBUG: Authentication set for STOMP session and SecurityContextHolder. User: " + authentication.getName());
              } else {
                System.out.println("DEBUG: JWT Token is invalid. Rejecting CONNECT.");
                // accessor.setLeaveMutable(true); // 제거
                // accessor.setErrorDetail("Invalid JWT token"); // 제거
                return null; // 메시지 처리를 중단하여 연결 거부
              }
            } catch (Exception e) {
              System.err.println("ERROR: WebSocket Authentication failed during CONNECT: " + e.getClass().getSimpleName() + " - " + e.getMessage());
              e.printStackTrace();
              // accessor.setLeaveMutable(true); // 제거
              // accessor.setErrorDetail("Authentication failed: " + e.getMessage()); // 제거
              return null; // 메시지 처리를 중단
            }
          } else {
            System.out.println("DEBUG: Authorization header is missing or not 'Bearer ' type for CONNECT. Rejecting CONNECT.");
            // accessor.setLeaveMutable(true); // 제거
            // accessor.setErrorDetail("Authorization token required"); // 제거
            return null; // 메시지 처리를 중단
          }
        }
        // CONNECT 외 다른 명령 (SEND, SUBSCRIBE, UNSUBSCRIBE 등) 처리
        // 이 시점에서는 이미 STOMP 세션에 Principal이 설정되어 있어야 합니다.
        // 해당 Principal을 SecurityContextHolder에 다시 설정하여 @AuthenticationPrincipal이 작동하도록 합니다.
        else {
          Principal principal = accessor.getUser(); // STOMP 세션의 Principal 가져오기
          if (principal instanceof Authentication) { // Principal이 Authentication 객체인 경우 (CONNECT 시 설정했으므로)
            SecurityContextHolder.getContext().setAuthentication((Authentication) principal);
            System.out.println("DEBUG: SecurityContextHolder set for command " + accessor.getCommand() + ". User: " + principal.getName());
          } else {
            // Principal이 없거나 Authentication 타입이 아닌 경우 (인증되지 않은 상태)
            System.out.println("DEBUG: No Authentication found in Principal for command " + accessor.getCommand() + ". Principal: " + principal);
            // 필요하다면 인증되지 않은 메시지 전송을 거부할 수 있습니다.
            // accessor.setLeaveMutable(true);
            // accessor.setErrorDetail("Authentication required for this operation.");
            // return null;
          }
        }

        return message;
      }
    });
  }
//  @Configuration
//  public class WebSocketSessionManager {
    // STOMP 세션 ID를 Key로, Authentication 객체를 Value로 저장합니다.
//    @Bean
//    public Map<String, Authentication> simpUserAuthenticationMap() {
//      return new ConcurrentHashMap<>();
//    }
//  }
}
