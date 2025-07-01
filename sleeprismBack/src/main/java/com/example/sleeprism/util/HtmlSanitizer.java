package com.example.sleeprism.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTML Sanitization을 위한 유틸리티 클래스입니다.
 * Jsoup 라이브러리를 사용하여 사용자 입력 HTML에서 잠재적으로 위험한 요소를 제거합니다.
 */
public class HtmlSanitizer {
  private static final Logger log = LoggerFactory.getLogger(HtmlSanitizer.class);

  /**
   * Quill.js가 생성하는 HTML을 안전하게 정화하는 Safelist를 반환합니다.
   * Jsoup의 `basicWithImages()` Safelist를 기반으로 하며,
   * Quill.js/Tiptap에서 필요한 추가 태그와 속성을 명시적으로 허용합니다.
   *
   * @return HTML 정화에 사용될 Safelist 객체
   */
  private static Safelist getQuillSafelist() {
    // 이미지를 안전하게 처리하는 기본 Safelist인 basicWithImages()를 시작점으로 사용합니다.
    // 이 Safelist는 <img> 태그와 그 src, alt, height, width 속성을 기본적으로 허용합니다.
    Safelist safelist = Safelist.basicWithImages();

    // Quill.js/Tiptap에서 자주 사용하는 태그들 추가
    safelist.addTags("span", "div");

    // 모든 태그에 class 속성 허용 (Tailwind CSS, 에디터 자체 스타일 클래스 유지를 위함)
    safelist.addAttributes(":all", "class");

    // 링크에 target="_blank", rel="noopener noreferrer" 등 허용
    safelist.addAttributes("a", "target", "rel");

    // 'style' 속성 자체를 모든 태그에 허용.
    safelist.addAttributes(":all", "style");

    return safelist;
  }

  /**
   * 사용자 입력 HTML 문자열을 안전하게 정화합니다.
   *
   * @param html 정화할 원본 HTML 문자열
   * @return 정화된 (Sanitized) HTML 문자열
   */
  public static String sanitize(String html) {
    if (html == null || html.trim().isEmpty()) {
      return "";
    }
    log.info("Sanitizer input HTML (first 200 chars): {}", html.substring(0, Math.min(html.length(), 200)));

    // FIX: Jsoup.clean()에 `baseUri`를 추가합니다.
    // 백엔드 URL의 도메인과 포트를 `baseUri`로 설정하여 Jsoup이 상대 경로를
    // 유효한 URL로 인식하도록 돕습니다.
    // 여기서는 http://localhost:8080/sleeprism/ 와 같이 설정합니다.
    // 실제 배포 환경에서는 서버의 실제 도메인과 컨텍스트 경로로 변경해야 합니다.
    String baseUri = "http://localhost:8080/sleeprism/"; // 사용자 환경에 맞게 수정 필요 (배포 시)

    String sanitizedHtml = Jsoup.clean(html, baseUri, getQuillSafelist()); // baseUri 인자 추가
    log.info("Sanitizer output HTML (first 200 chars): {}", sanitizedHtml.substring(0, Math.min(sanitizedHtml.length(), 200)));
    return sanitizedHtml;
  }
}
