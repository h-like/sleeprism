package com.example.sleeprism.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // 가장 높은 우선순위로 설정
public class StompChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null) {
      // STOMP Command 로깅
      StompCommand command = accessor.getCommand();
      String sessionId = accessor.getSessionId();
      Principal principal = accessor.getUser(); // STOMP 세션에 연결된 사용자 (인증 후)

      String userIdentifier = Optional.ofNullable(principal)
          .map(Principal::getName)
          .orElse("UNAUTHENTICATED");

      log.debug("STOMP Interceptor - preSend: Command: {}, Session ID: {}, User: {}, Destination: {}",
          command, sessionId, userIdentifier, accessor.getDestination()); // Destination 추가 로깅

      // CONNECT 명령 처리 시 JWT 토큰 확인
      if (StompCommand.CONNECT.equals(command)) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        log.debug("STOMP Interceptor - CONNECT Command received. Authorization header: {}",
            authorizationHeader != null ? "Present" : "Not Present");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
          String token = authorizationHeader.substring(7);
          log.debug("STOMP Interceptor - Extracted JWT Token (first 30 chars): {}", token.substring(0, Math.min(token.length(), 30)) + "...");
        }
      }
      // SUBSCRIBE 명령 처리 시 구독 대상 로깅
      else if (StompCommand.SUBSCRIBE.equals(command)) {
        log.debug("STOMP Interceptor - SUBSCRIBE Destination: {}", accessor.getDestination());
        log.debug("STOMP Interceptor - SUBSCRIBE ID: {}", accessor.getSubscriptionId()); // 구독 ID 로깅
      }
      // SEND 명령 처리 시 목적지 및 페이로드 일부 로깅
      else if (StompCommand.SEND.equals(command)) {
        log.debug("STOMP Interceptor - SEND Destination: {}, Payload size: {}",
            accessor.getDestination(), message.getPayload().toString().length());
        // String payloadContent = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8); // 실제 페이로드 내용 로깅 (주의: 너무 길면 성능 저하)
        // log.debug("STOMP Interceptor - SEND Payload: {}", payloadContent);
      }
      // DISCONNECT 명령 처리
      else if (StompCommand.DISCONNECT.equals(command)) {
        log.debug("STOMP Interceptor - DISCONNECT Command received.");
      }

      // SecurityContextHolder에 Principal 설정 (필요한 경우)
      // Spring Security WebSocket 설정에 따라 이미 처리될 수 있음
      // if (principal != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      //     SecurityContextHolder.getContext().setAuthentication((Authentication) principal);
      //     log.debug("STOMP Interceptor - SecurityContextHolder set for command {}. User: {}", command, userIdentifier);
      // }
    }
    return message;
  }

  @Override
  public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null && accessor.getCommand() != null) {
      // DISCONNECT 명령 이후에는 Principal이 null이 될 수 있음
      Principal principal = accessor.getUser();
      String userIdentifier = Optional.ofNullable(principal)
          .map(Principal::getName)
          .orElse("UNAUTHENTICATED/DISCONNECTED");
      log.debug("STOMP Interceptor - PostSend Command: {}, Session ID: {}, User: {}, Sent: {}",
          accessor.getCommand(), accessor.getSessionId(), userIdentifier, sent);
    }
  }

  @Override
  public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null && accessor.getCommand() != null) {
      Principal principal = accessor.getUser();
      String userIdentifier = Optional.ofNullable(principal)
          .map(Principal::getName)
          .orElse("UNAUTHENTICATED/DISCONNECTED");
      if (ex != null) {
        log.error("STOMP Interceptor - AfterSendCompletion Command: {}, Session ID: {}, User: {}, Exception: {}",
            accessor.getCommand(), accessor.getSessionId(), userIdentifier, ex.getMessage(), ex);
      } else {
        log.debug("STOMP Interceptor - AfterSendCompletion Command: {}, Session ID: {}, User: {}, Sent: {}",
            accessor.getCommand(), accessor.getSessionId(), userIdentifier, sent);
      }
    }
  }

  // 세션 종료 시 호출
  @Override
  public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null && accessor.getCommand() != null) {
      if (StompCommand.DISCONNECT.equals(accessor.getCommand()) || StompCommand.ERROR.equals(accessor.getCommand())) {
        log.info("STOMP Interceptor - Session completion for DISCONNECT/ERROR. Session ID: {}", accessor.getSessionId());
      }
    }
    if (ex != null) {
      log.error("STOMP Interceptor - Error during message receive completion. Session ID: {}, Exception: {}", accessor.getSessionId(), ex.getMessage(), ex);
    }
  }
}