package com.example.sleeprism.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

  private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // CORS 허용
        .withSockJS();
  }

  @Override
  protected void customizeClientInboundChannel(ChannelRegistration registration) {
    // ✅ Interceptor 등록 순서도 중요할 수 있습니다. 인증 인터셉터를 먼저 실행합니다.
    registration.interceptors(webSocketAuthChannelInterceptor);
    log.info("STOMP Auth interceptor registered.");
  }

  @Override
  protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
    messages
        .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.HEARTBEAT, SimpMessageType.UNSUBSCRIBE, SimpMessageType.DISCONNECT).permitAll()
        .simpDestMatchers("/app/**").authenticated()
        // ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ 이 부분이 수정되었습니다 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
        // /user/** 경로에 대한 구독을 허용하는 규칙을 추가합니다.
        .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ 여기까지 수정되었습니다 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
        .anyMessage().denyAll();
    log.info("STOMP message security configured successfully.");
  }

  @Override
  protected boolean sameOriginDisabled() {
    return true;
  }
}
