package com.example.sleeprism.config;

import com.example.sleeprism.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
      log.debug("WebSocket CONNECT attempt with Authorization header: {}", (authorizationHeader != null ? "Present" : "Not Present"));

      if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
        String token = authorizationHeader.substring(7);
        try {
          if (jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            // ✅ 중요: 인증된 사용자를 WebSocket 세션과 연결합니다.
            // 이 코드가 없으면 후속 STOMP 메시지에서 인증 정보를 잃게 됩니다.
            accessor.setUser(authentication);
            // SecurityContextHolder에도 설정하여 현재 스레드에서 즉시 사용 가능하게 합니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("User '{}' authenticated successfully for WebSocket session.", authentication.getName());
          }
        } catch (Exception e) {
          log.error("WebSocket CONNECT Authentication failed: {}", e.getMessage());
          // 인증 실패 시 연결을 거부할 수 있으나, 여기서는 그냥 통과시키고
          // 다음 단계의 보안 규칙에서 차단되도록 둡니다.
        }
      }
    }
    return message;
  }
}
