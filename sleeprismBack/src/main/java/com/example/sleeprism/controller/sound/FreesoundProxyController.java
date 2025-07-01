// src/main/java/com/example/sleeprism/controller/sound/FreesoundProxyController.java
package com.example.sleeprism.controller.sound;

import com.example.sleeprism.dto.sound.SoundInfo;
import com.example.sleeprism.service.Sound.FreesoundService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource; // InputStreamResource 추가
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FreesoundProxyController {

  private final FreesoundService freesoundService;

  @Value("${audio.file.base.path:audio-files}")
  private String audioFileBasePath;

  public FreesoundProxyController(FreesoundService freesoundService) {
    this.freesoundService = freesoundService;
  }

  // Freesound 검색 프록시 엔드포인트 (기존 코드 유지)
  @GetMapping("/freesound-search")
  public ResponseEntity<String> searchFreesound(@RequestParam String query) {
    System.out.println("Freesound search requested for query: " + query);
    if (query == null || query.trim().isEmpty()) {
      return new ResponseEntity<>("{\"error\": \"검색어가 필요합니다.\"}", HttpStatus.BAD_REQUEST);
    }
    try {
      String freesoundResponse = freesoundService.searchSounds(query);
      return new ResponseEntity<>(freesoundResponse, HttpStatus.OK);
    } catch (IllegalStateException e) {
      System.err.println("Freesound API Key not configured: " + e.getMessage());
      return new ResponseEntity<>("{\"error\": \"" + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      System.err.println("Error during Freesound search: " + e.getMessage());
      return new ResponseEntity<>("{\"error\": \"Freesound 검색 중 서버 오류가 발생했습니다.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 특정 사운드 파일을 스트리밍하는 엔드포인트입니다.
   * HTTP Range 헤더를 지원하여 부분 콘텐츠 요청을 처리합니다.
   * @param soundName 파일 이름 (확장자 제외)
   * @param rangeHeader Range 헤더 (선택 사항)
   * @return 오디오 파일 스트림
   */
  @GetMapping(value = "/sounds/{soundName}.mp3", produces = "audio/mpeg") // produces 속성 명시 유지
  public ResponseEntity<Resource> getAudioFile(@PathVariable String soundName,
                                               @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
    System.out.println("Received request for audio file: " + soundName + ".mp3");
    Path filePath = Paths.get(audioFileBasePath, soundName + ".mp3").normalize();
    System.out.println("Attempting to load audio file from: " + filePath.toAbsolutePath());

    try {
      if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
        System.err.println("Audio file not found or not readable: " + filePath.toAbsolutePath());
        return ResponseEntity.notFound().build();
      }

      long contentLength = Files.size(filePath);
      InputStream inputStream = Files.newInputStream(filePath);
      InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
      headers.setCacheControl("no-cache, no-store, must-revalidate");
      headers.setPragma("no-cache");
      headers.setExpires(0);
      headers.set(HttpHeaders.ACCEPT_RANGES, "bytes"); // Range 요청 지원 명시

      // Range 헤더 처리 (부분 콘텐츠 스트리밍)
      if (rangeHeader != null && !rangeHeader.isEmpty()) {
        long rangeStart = 0;
        long rangeEnd = contentLength - 1;
        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        if (ranges.length == 2) {
          rangeStart = Long.parseLong(ranges[0]);
          if (!ranges[1].isEmpty()) {
            rangeEnd = Long.parseLong(ranges[1]);
          }
        } else if (ranges.length == 1) {
          rangeStart = Long.parseLong(ranges[0]);
        }

        // 유효한 범위인지 확인
        if (rangeStart < 0 || rangeStart >= contentLength || rangeEnd < rangeStart) {
          System.err.println("Invalid Range header: " + rangeHeader);
          return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
              .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
              .build();
        }

        long actualRangeEnd = Math.min(rangeEnd, contentLength - 1);
        long rangeLength = actualRangeEnd - rangeStart + 1;

        // 스트림을 요청된 시작 위치로 건너뛰기
        inputStream.skip(rangeStart);

        headers.setContentLength(rangeLength);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + actualRangeEnd + "/" + contentLength);

        System.out.println("Serving partial content: " + rangeHeader + ", Content-Range: " + headers.get(HttpHeaders.CONTENT_RANGE));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT) // 206 Partial Content
            .headers(headers)
            .body(inputStreamResource); // InputStreamResource 반환

      } else {
        // 전체 콘텐츠 요청
        headers.setContentLength(contentLength);
        System.out.println("Serving full content.");
        return ResponseEntity.ok() // 200 OK
            .headers(headers)
            .body(inputStreamResource); // InputStreamResource 반환
      }

    } catch (IOException e) {
      System.err.println("Error reading audio file " + filePath.toAbsolutePath() + ": " + e.getMessage());
      // 파일 읽기 오류 발생 시, 500 Internal Server Error를 직접 반환
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (NumberFormatException e) {
      System.err.println("Invalid Range header format: " + rangeHeader + ". Error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build(); // 416 Range Not Satisfiable
    } catch (Exception e) {
      System.err.println("An unexpected error occurred while serving audio file: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 프론트엔드에 제공할 기본 사운드 목록을 반환하는 엔드포인트입니다.
   * 이 목록은 백엔드에 직접 호스팅된 사운드 파일의 URL을 포함합니다.
   * @return SoundInfo 객체 목록
   */
  @GetMapping("/sounds/internal")
  public ResponseEntity<List<SoundInfo>> getInternalSounds() {
    System.out.println("Received request for internal sounds list.");
    List<SoundInfo> internalSounds = Arrays.asList(
        new SoundInfo("preset-wind", "바람소리", "/api/sounds/wind.mp3", 0.5),
        new SoundInfo("preset-rain", "빗소리", "/api/sounds/rain.mp3", 0.5),
        new SoundInfo("preset-fire", "장작 타는 소리", "/api/sounds/fire.mp3", 0.5),
        new SoundInfo("preset-bird", "새소리", "/api/sounds/bird.mp3", 0.5),
        new SoundInfo("preset-ocean", "파도 소리", "/api/sounds/ocean.mp3", 0.5),
        new SoundInfo("preset-river", "개울물 소리", "/api/sounds/river.mp3", 0.5),
        new SoundInfo("preset-bar", "카페 소리", "/api/sounds/bar.mp3", 0.5),
        new SoundInfo("preset-underwater", "물속의 소리", "/api/sounds/underwater.mp3", 0.5),
        new SoundInfo("preset-nature", "여름 밤의 소리", "/api/sounds/nature.mp3", 0.5)
    );
    return ResponseEntity.ok(internalSounds);
  }
}
