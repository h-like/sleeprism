// src/main/java/com/example/sleeprism/security/WebConfig.java
package com.example.sleeprism.security; // 패키지 경로 확인

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration; // @Configuration 어노테이션 추가
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 이 클래스를 Spring 설정 빈으로 만듭니다.
public class WebConfig implements WebMvcConfigurer { // WebMvcConfigurer 인터페이스 구현 확인

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // 모든 경로에 대해 CORS 적용 (가장 포괄적)
    // allowedOrigins에 정확한 프론트엔드 오리진(들)을 명시합니다.
    // * (모든 오리진)을 사용하지 않습니다.
    registry.addMapping("/**") // <-- 모든 경로에 CORS를 적용합니다. Context Path는 여기서 고려할 필요 없습니다.
        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // 프론트엔드 오리진 명확히 지정
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 모든 관련 메서드 포함
        .allowedHeaders("*") // 모든 헤더 허용
        .allowCredentials(true) // 중요: 자격 증명(쿠키, 인증 헤더 등) 허용
        .maxAge(3600);
  }

  @Value("${file.upload-dir}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // /files/** 경로로 요청이 오면 C:\\upload 디렉토리에서 파일을 찾도록 설정
    // 이 설정도 WebMvcConfigurer에서 하는 것이 맞습니다.
    registry.addResourceHandler("/files/**") // 웹에서 접근할 경로
        .addResourceLocations("file:" + uploadDir + "/"); // 실제 파일 시스템 경로
  }
}
