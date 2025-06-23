// src/main/java/com/example/sleeprism/util/HtmlSanitizer.java
package com.example.sleeprism.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * HTML Sanitization을 위한 유틸리티 클래스입니다.
 * Jsoup 라이브러리를 사용하여 사용자 입력 HTML에서 잠재적으로 위험한 요소를 제거합니다.
 */
public class HtmlSanitizer {

  /**
   * Quill.js가 생성하는 HTML을 안전하게 정화하는 Safelist를 반환합니다.
   * Jsoup의 relaxed Safelist를 기반으로 하며, Quill.js에서 필요한 추가 태그와 속성을 명시적으로 허용합니다.
   *
   * @return HTML 정화에 사용될 Safelist 객체
   */
  private static Safelist getQuillSafelist() {
    // Jsoup의 기본 relaxed Safelist는 대부분의 텍스트 포맷팅 태그를 허용합니다.
    // (p, b, i, em, strong, a, img, br, ul, ol, li, blockquote, pre, h1-h6 등)
    Safelist safelist = Safelist.relaxed();

    // Quill.js에서 자주 사용하는 태그들 추가
    safelist.addTags("span", "div");

    // 모든 태그에 class 속성 허용 (Quill의 스타일 클래스 유지를 위함)
    safelist.addAttributes(":all", "class");

    // 링크에 target="_blank", rel="noopener noreferrer" 등 허용
    safelist.addAttributes("a", "target", "rel");

    // 이미지 태그의 필수 속성 허용 및 프로토콜 허용
    safelist.addAttributes("img", "src", "alt", "width", "height");
    safelist.addProtocols("img", "src", "http", "https");

    // ====================================================================
    // 핵심 변경: 'style' 속성 자체를 모든 태그에 허용.
    // Jsoup은 기본적으로 style 속성을 제거합니다. 이를 명시적으로 허용해야 합니다.
    // 이렇게 하면 style="color: blue; text-align: center;" 와 같은 인라인 스타일이 유지됩니다.
    // Jsoup의 Safelist는 일반적으로 CSS 속성 자체를 '화이트리스트'하는 기능은
    // 매우 제한적이거나 특정 버전에만 존재하며, 대부분은 style 속성을 '허용할지 말지'에 중점을 둡니다.
    // Quill.js와 같은 에디터의 복잡한 스타일을 완벽히 유지하려면 style 속성 자체를 허용하는 것이 가장 현실적입니다.
    // 이로 인해 잠재적인 위험이 약간 증가할 수 있지만, 일반적인 에디터 사용에서는 필요한 트레이드오프입니다.
    safelist.addAttributes(":all", "style"); // 모든 태그에 style 속성 허용

    // 추가적으로 Quill이 생성할 수 있는 특정 CSS 속성을 더 강력하게 제어하고 싶다면
    // Jsoup의 기능만으로는 한계가 있을 수 있으며, DOMPurify (JavaScript 라이브러리)를
    // 프론트엔드에서 사용하거나, 더 정교한 서버 사이드 HTML 파서/라이브러리를 고려해야 합니다.
    // 하지만 대부분의 경우 addAttributes(":all", "style")로 충분합니다.
    // ====================================================================

    // XSS 공격 방지를 위해 스크립트 관련 태그는 기본적으로 허용하지 않습니다.
    // 예를 들어, <script>, onerror, onload 등은 Jsoup의 Safelist에서 자동으로 제거됩니다.
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
    // Jsoup.clean(html, baseUri, safelist, OutputSettings) 형태로 사용 가능
    // baseUri는 상대 경로를 절대 경로로 변환할 때 사용되지만, 여기서는 필요 없으므로 빈 문자열
    return Jsoup.clean(html, "", getQuillSafelist());
  }
}