// src/main/java/com/example/sleeprism/security/WebConfig.java
package com.example.sleeprism.security; // 패키지 경로 확인

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration // 이 클래스를 Spring 설정 빈으로 만듭니다.
public class WebConfig implements WebMvcConfigurer { // WebMvcConfigurer 인터페이스 구현 확인

//  @Override
//  public void addCorsMappings(CorsRegistry registry) {
//    // 모든 경로에 대해 CORS 적용 (가장 포괄적)
//    // allowedOrigins에 정확한 프론트엔드 오리진(들)을 명시합니다.
//    // * (모든 오리진)을 사용하지 않습니다.
//    registry.addMapping("/**") // <-- 모든 경로에 CORS를 적용합니다. Context Path는 여기서 고려할 필요 없습니다.
//        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // 프론트엔드 오리진 명확히 지정
//        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 모든 관련 메서드 포함
//        .allowedHeaders("*") // 모든 헤더 허용
//        .allowCredentials(true) // 중요: 자격 증명(쿠키, 인증 헤더 등) 허용
//        .maxAge(3600);
//  }

  @Value("${file.upload-dir}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 기존 게시글 및 프로필 이미지 핸들러
    // uploadDir이 "C:/uploads"이든 "C:/uploads/"이든 항상 올바른 경로를 생성합니다.
    registry.addResourceHandler("/files/**")
        .addResourceLocations("file:" + Paths.get(uploadDir).toAbsolutePath().normalize().toString() + "/");

    // 댓글 첨부 파일 핸들러: 'comment' 폴더를 올바르게 매핑
    // 요청 URL 경로 패턴을 "/api/comments/files/comment/**"로 수정합니다.
    // 이는 프론트엔드에서 BACKEND_BASE_URL + "/api/comments/files/" + comment.attachmentUrl (예: "comment/uuid.jpg") 로 요청했을 때
    // 최종 URL이 "/api/comments/files/comment/uuid.jpg"가 되도록 일치시킵니다.
    registry.addResourceHandler("/api/comments/files/comment/**") // <-- 이 부분을 수정합니다: 'comments' -> 'comment'
        // uploadDir 아래의 "comment" 폴더를 매핑합니다.
        // Paths.get을 사용하여 OS에 독립적인 경로를 안전하게 구성합니다.
        .addResourceLocations("file:" + Paths.get(uploadDir, "comment").toAbsolutePath().normalize().toString() + "/");

    // 참고: 만약 게시글 파일이 '/api/posts/files/post-images/**' 같은 경로로 서빙되어야 한다면
    // 아래와 같이 명시적으로 추가할 수 있습니다.
    // registry.addResourceHandler("/api/posts/files/**")
    //         .addResourceLocations("file:" + Paths.get(uploadDir, "post-images").toAbsolutePath().normalize().toString() + "/");
    // dgg22
  }


}
