package com.example.sleeprism.dto.sound;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FreesoundSearchResponse {
  private String next; // 다음 페이지 URL (페이징 시 사용)
  private List<FreesoundSound> results; // 검색 결과 사운드 목록

  @Data
  public static class FreesoundSound {
    private String id; // Freesound의 사운드 고유 ID
    private String name; // 사운드 이름
    private String description; // 사운드 설명
    private Map<String, String> previews; // 오디오 미리보기 URL (small, med, large)
    private List<String> tags; // 태그 목록
    // 필요하다면 다른 필드도 추가 가능 (예: username, duration 등)
  }
}
