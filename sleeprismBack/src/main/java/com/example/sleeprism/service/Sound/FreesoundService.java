// src/main/java/com/example/soundmixer/service/FreesoundService.java (예시 경로)

package com.example.sleeprism.service.Sound;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FreesoundService {

  @Value("${freesound.api.key}") // application.properties에서 API 키 주입
  private String freesoundApiKey;

  private final RestTemplate restTemplate;

  public FreesoundService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate; // RestTemplate은 Spring에서 HTTP 요청을 보내는 데 사용
  }

  public String searchSounds(String query) {
    if (freesoundApiKey == null || freesoundApiKey.isEmpty() || freesoundApiKey.equals("YOUR_FREESOUND_API_KEY_HERE")) {
      throw new IllegalStateException("Freesound API 키가 설정되지 않았습니다.");
    }

    String url = UriComponentsBuilder.fromHttpUrl("https://freesound.org/apiv2/search/text/")
        .queryParam("query", query)
        .queryParam("token", freesoundApiKey)
        .queryParam("fields", "id,name,previews") // 필요한 필드만 요청
        .toUriString();

    // Freesound API에 GET 요청을 보내고 응답을 String으로 받습니다.
    // 실제 프로젝트에서는 응답을 JSON 객체로 매핑하여 처리하는 것이 일반적입니다.
    return restTemplate.getForObject(url, String.class);
  }
}