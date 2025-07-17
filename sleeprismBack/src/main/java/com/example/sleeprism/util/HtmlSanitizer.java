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
    // FIX: Safelist를 더욱 명시적으로 정의하여 src 속성 제거 방지
    Safelist safelist = new Safelist();

    // 허용할 태그들 추가
    safelist.addTags("a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em", "i", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong", "sub", "sup", "u", "ul", "h1", "h2", "h3", "hr", "div");

    // 이미지 태그 및 속성 명시적 허용
    safelist.addTags("img");
    // src, alt, title, width, height, class, style 속성 허용
    safelist.addAttributes("img", "src", "alt", "title", "width", "height", "class", "style");
    // 이미지 src에 허용할 프로토콜 명시
    safelist.addProtocols("img", "src", "http", "https", "data"); // data URI 스킴도 허용 (필요시)

    // 모든 태그에 class, style 속성 허용
    safelist.addAttributes(":all", "class", "style");

    // 링크에 특정 속성 허용
    safelist.addAttributes("a", "href", "target", "rel"); // rel="noopener noreferrer"는 보안 권장 사항

    // 텍스트 정렬을 위한 align 속성 (CSS style 사용을 권장하지만, 필요한 경우 추가)
    // safelist.addAttributes(":all", "align");

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
    // FIX: Full HTML content logging for debugging
    log.info("Sanitizer input HTML: {}", html);

    // FIX: baseUri를 다시 명시적으로 설정하여 Jsoup이 상대 경로를 올바르게 해석하도록 합니다.
    // 프론트엔드에서 /api/posts/files/... 와 같은 상대 경로를 보내므로,
    // Jsoup이 이를 올바른 절대 경로로 변환할 수 있도록 백엔드 URL의 루트를 baseUri로 설정합니다.
    // 이 URL은 실제 배포 환경에 따라 변경되어야 합니다.
    String baseUri = "http://localhost:8080/";
    String sanitizedHtml = Jsoup.clean(html, baseUri, getQuillSafelist());
    log.info("Sanitizer output HTML: {}", sanitizedHtml);
    return sanitizedHtml;
  }
}
