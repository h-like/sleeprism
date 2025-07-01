package com.example.sleeprism.service;

import com.example.sleeprism.dto.DreamInterpretationRequestDTO;
import com.example.sleeprism.dto.DreamInterpretationResponseDTO;
import com.example.sleeprism.dto.DreamInterpretationSelectRequestDTO;
import com.example.sleeprism.dto.InterpretationOptionDTO;
import com.example.sleeprism.entity.DreamInterpretation;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.TarotCard;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.DreamInterpretationRepository;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.repository.TarotCardRepository;
import com.example.sleeprism.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate; // API 호출을 위해 RestTemplate 또는 WebClient 사용 (여기서는 RestTemplate 예시)

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DreamInterpretationService {

  private final DreamInterpretationRepository dreamInterpretationRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final TarotCardRepository tarotCardRepository;
  private final ObjectMapper objectMapper; // JSON 파싱을 위해 주입
  private final RestTemplate restTemplate = new RestTemplate(); // API 호출을 위한 RestTemplate (WebClient 권장)

  // Gemini API 키는 application.properties에 정의하고 @Value로 주입받습니다.
  // NOTE: Canvas 환경에서는 apiKey가 빈 문자열이면 런타임에 자동으로 제공됩니다.
  @Value("${gemini.api.key:}") // 기본값으로 빈 문자열 설정
  private String geminiApiKey;

  // AI에게 요청할 해몽 옵션의 개수 (예: 3가지 타로 카드와 매칭)
  private static final int NUM_INTERPRETATION_OPTIONS = 3;

  /**
   * AI (Google Gemini)를 사용하여 꿈 내용을 해몽하고, 결과를 저장하며, 타로 카드를 매칭합니다.
   *
   * @param requestDto 해몽 요청 DTO (꿈 게시글 ID 포함)
   * @param userId     해몽을 요청한 사용자 ID
   * @return 생성된 해몽 결과 DTO (여러 해몽 옵션 포함)
   */
  @Transactional
  public DreamInterpretationResponseDTO interpretDream(DreamInterpretationRequestDTO requestDto, Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    Post post = postRepository.findByIdAndIsDeletedFalse(requestDto.getPostId())
        .orElseThrow(() -> new EntityNotFoundException("Post not found or deleted with ID: " + requestDto.getPostId()));

    // 이미 해당 포스트와 사용자에 대한 해몽이 있다면 가져와서 재사용 또는 업데이트 (정책에 따라)
    // 여기서는 새로운 해몽을 요청할 때마다 새 기록을 생성하도록 합니다.
    // Optional<DreamInterpretation> existingInterpretation = dreamInterpretationRepository.findByPostAndUserOrderByInterpretedAtDesc(post, user);
    // if (existingInterpretation.isPresent()) { /* 정책에 따른 처리 */ }

    // 1. 꿈 내용을 기반으로 Gemini API 프롬프트 생성
    String dreamContent = post.getContent();
    // AI가 JSON 형태로 여러 해몽을 제공하도록 프롬프트 구성 (responseSchema와 연동)
    String prompt = String.format(
        "다음 꿈 내용을 해몽해주세요. 꿈의 상징들을 분석하고, 심리적인 의미, 가능한 미래 암시 등 다양한 관점에서 %d가지의 간결한 해석을 각각 JSON 형태로 반환해주세요. 각 해석은 'title'과 'content' 필드를 포함해야 합니다. 꿈 내용: \"%s\"",
        NUM_INTERPRETATION_OPTIONS, dreamContent
    );

    String aiResponseJson;
    try {
      // 2. Gemini API 호출
      aiResponseJson = callGeminiApi(prompt);
      log.info("Gemini API raw response: {}", aiResponseJson);
    } catch (Exception e) {
      log.error("Failed to call Gemini API for post {}: {}", post.getId(), e.getMessage(), e);
      // API 호출 실패 시 에러 응답 DTO 반환
      return DreamInterpretationResponseDTO.builder()
          .postId(post.getId())
          .userId(user.getId())
          .userName(user.getNickname())
          .errorMessage("꿈 해몽 AI API 호출에 실패했습니다: " + e.getMessage())
          .build();
    }

    // 3. AI 응답 파싱 및 타로 카드 매칭
    List<InterpretationOptionDTO> interpretationOptions = new ArrayList<>();
    Map<Long, TarotCard> allTarotCardsMap = tarotCardRepository.findAll().stream()
        .collect(Collectors.toMap(TarotCard::getId, card -> card)); // 모든 타로 카드를 맵으로 로드

    try {
      // Gemini API 응답 JSON 파싱. "interpretations" 키 아래에 해몽 목록이 있다고 가정.
      // example: {"interpretations": [{"title": "...", "content": "..."}, {"title": "...", "content": "..."}, ...]}
      Map<String, List<Map<String, String>>> aiResponseMap = objectMapper.readValue(
          aiResponseJson,
          new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<Map<String, String>>>>() {
          }
      );

      List<Map<String, String>> interpretationsData = aiResponseMap.get("interpretations");
      if (interpretationsData == null || interpretationsData.isEmpty()) {
        throw new IllegalStateException("AI 응답에 유효한 해석 목록이 포함되어 있지 않습니다.");
      }

      // 타로 카드 무작위 선택 (NUM_INTERPRETATION_OPTIONS 만큼)
      List<TarotCard> allTarotCards = new ArrayList<>(allTarotCardsMap.values());
      Collections.shuffle(allTarotCards); // 카드 목록 섞기

      for (int i = 0; i < Math.min(interpretationsData.size(), NUM_INTERPRETATION_OPTIONS); i++) {
        Map<String, String> interpretationData = interpretationsData.get(i);
        TarotCard matchedCard = null;
        if (i < allTarotCards.size()) { // 충분한 타로 카드가 있는지 확인
          matchedCard = allTarotCards.get(i); // 섞인 목록에서 순서대로 매칭
        } else if (!allTarotCards.isEmpty()) {
          matchedCard = allTarotCards.get(0); // 카드가 부족하면 첫 번째 카드 반복
        }

        interpretationOptions.add(InterpretationOptionDTO.builder()
            .optionIndex(i)
            .title(interpretationData.getOrDefault("title", "제목 없음"))
            .content(interpretationData.getOrDefault("content", "해석 내용 없음"))
            .tarotCardId(matchedCard != null ? matchedCard.getId() : null)
            .tarotCardName(matchedCard != null ? matchedCard.getName() : null)
            .tarotCardImageUrl(matchedCard != null ? matchedCard.getImageUrl() : null)
            .build());
      }

    } catch (JsonProcessingException e) {
      log.error("Failed to parse Gemini API response for post {}: {}", post.getId(), e.getMessage(), e);
      return DreamInterpretationResponseDTO.builder()
          .postId(post.getId())
          .userId(user.getId())
          .userName(user.getNickname())
          .errorMessage("AI 응답을 파싱하는 데 실패했습니다. (응답 형식 오류)")
          .build();
    } catch (Exception e) {
      log.error("Error processing AI interpretation or matching tarot cards for post {}: {}", post.getId(), e.getMessage(), e);
      return DreamInterpretationResponseDTO.builder()
          .postId(post.getId())
          .userId(user.getId())
          .userName(user.getNickname())
          .errorMessage("해몽 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage())
          .build();
    }

    // 4. DreamInterpretation 엔티티 저장 (처음에는 선택된 해몽 없음)
    DreamInterpretation dreamInterpretation = DreamInterpretation.builder()
        .post(post)
        .user(user)
        .aiResponseContent(aiResponseJson) // AI의 원본 응답 JSON 저장
        .selectedInterpretationIndex(null) // 처음에는 선택되지 않음
        .selectedTarotCard(null) // 처음에는 선택되지 않음
        .build();

    DreamInterpretation savedInterpretation = dreamInterpretationRepository.save(dreamInterpretation);

    // 저장된 엔티티와 파싱된 옵션들로 DTO 구성하여 반환
    return DreamInterpretationResponseDTO.builder()
        .id(savedInterpretation.getId())
        .postId(savedInterpretation.getPost().getId())
        .userId(savedInterpretation.getUser().getId())
        .userName(savedInterpretation.getUser().getNickname())
        .interpretedAt(savedInterpretation.getInterpretedAt())
        .interpretationOptions(interpretationOptions)
        // selectedOptionIndex, selectedTarotCard 등은 처음에는 null
        .build();
  }


  /**
   * 사용자가 AI가 제공한 해몽 옵션 중 하나를 최종적으로 선택하고 저장합니다.
   *
   * @param interpretationId 해몽 기록 ID
   * @param requestDto       선택된 옵션 정보 DTO
   * @param userId           선택을 요청한 사용자 ID
   * @return 업데이트된 해몽 결과 DTO
   */
  @Transactional
  public DreamInterpretationResponseDTO selectInterpretationOption(
      Long interpretationId, DreamInterpretationSelectRequestDTO requestDto, Long userId) {
    DreamInterpretation dreamInterpretation = dreamInterpretationRepository.findById(interpretationId)
        .orElseThrow(() -> new EntityNotFoundException("Dream interpretation not found with ID: " + interpretationId));

    // 해당 해몽 기록의 요청자만 선택할 수 있도록 검증
    if (!dreamInterpretation.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to select an option for this interpretation.");
    }

    TarotCard selectedTarotCard = tarotCardRepository.findById(requestDto.getSelectedTarotCardId())
        .orElseThrow(() -> new EntityNotFoundException("Selected Tarot Card not found with ID: " + requestDto.getSelectedTarotCardId()));

    // 해몽 엔티티 업데이트
    dreamInterpretation.selectInterpretation(requestDto.getSelectedOptionIndex(), selectedTarotCard);
    // dreamInterpretationRepository.save(dreamInterpretation); // @Transactional에 의해 변경 감지되어 자동 저장

    // 업데이트된 엔티티를 DTO로 변환하여 반환
    // 이때, DreamInterpretationResponseDTO 생성자에 필요한 ObjectMapper와 TarotCard 맵을 전달
    Map<Long, TarotCard> allTarotCardsMap = tarotCardRepository.findAll().stream()
        .collect(Collectors.toMap(TarotCard::getId, card -> card));

    return new DreamInterpretationResponseDTO(dreamInterpretation, objectMapper, allTarotCardsMap);
  }

  /**
   * 특정 해몽 기록을 ID로 조회합니다.
   *
   * @param interpretationId 해몽 기록 ID
   * @param userId           조회 요청 사용자 ID
   * @return 해몽 결과 DTO
   */
  public DreamInterpretationResponseDTO getDreamInterpretationById(Long interpretationId, Long userId) {
    DreamInterpretation dreamInterpretation = dreamInterpretationRepository.findById(interpretationId)
        .orElseThrow(() -> new EntityNotFoundException("Dream interpretation not found with ID: " + interpretationId));

    // 해몽 기록의 요청자만 조회할 수 있도록 (정책에 따라 변경 가능)
    if (!dreamInterpretation.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to view this interpretation.");
    }

    // DTO 변환 시 필요한 ObjectMapper와 TarotCard 맵을 전달
    Map<Long, TarotCard> allTarotCardsMap = tarotCardRepository.findAll().stream()
        .collect(Collectors.toMap(TarotCard::getId, card -> card));

    return new DreamInterpretationResponseDTO(dreamInterpretation, objectMapper, allTarotCardsMap);
  }

  /**
   * 특정 사용자가 요청한 모든 해몽 기록을 조회합니다.
   *
   * @param userId 사용자 ID
   * @return 해몽 결과 목록 DTO
   */
  public List<DreamInterpretationResponseDTO> getMyDreamInterpretations(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    List<DreamInterpretation> interpretations = dreamInterpretationRepository.findByUserOrderByInterpretedAtDesc(user);

    Map<Long, TarotCard> allTarotCardsMap = tarotCardRepository.findAll().stream()
        .collect(Collectors.toMap(TarotCard::getId, card -> card));

    return interpretations.stream()
        .map(di -> new DreamInterpretationResponseDTO(di, objectMapper, allTarotCardsMap))
        .collect(Collectors.toList());
  }

  // ======================================================================
  // PRIVATE HELPER METHOD for Gemini API Call (Using HttpURLConnection)
  // ======================================================================
  private String callGeminiApi(String prompt) throws IOException {
    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;
    URL url = new URL(apiUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true); // Enable writing to the output stream

    // Request Body (Payload) for Gemini API
    String requestBody = String.format(
        "{\"contents\": [{\"role\": \"user\", \"parts\": [{\"text\": \"%s\"}]}], \"generationConfig\": {\"responseMimeType\": \"application/json\", \"responseSchema\": {\"type\": \"OBJECT\", \"properties\": {\"interpretations\": {\"type\": \"ARRAY\", \"items\": {\"type\": \"OBJECT\", \"properties\": {\"title\": {\"type\": \"STRING\"}, \"content\": {\"type\": \"STRING\"}}}}}, \"propertyOrdering\": [\"interpretations\"]}}}",
        prompt.replace("\"", "\\\"") // JSON 내부에 따옴표가 있을 경우 이스케이프 처리
    );

    log.debug("Gemini API Request Body: {}", requestBody);

    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = requestBody.getBytes("utf-8");
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    log.info("Gemini API Response Code: {}", responseCode);

    if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
      try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
          response.append(responseLine.trim());
        }
        // Gemini 응답에서 "text" 필드만 추출 (generationConfig의 responseSchema를 통해 JSON 객체로 받아옴)
        // "text" 필드 안에 우리가 요청한 JSON 문자열이 담겨 있습니다.
        // 따라서 이 부분을 다시 파싱해야 합니다.
        Map<String, Object> geminiRawResponse = objectMapper.readValue(response.toString(), new TypeReference<Map<String, Object>>() {
        });
        if (geminiRawResponse.containsKey("candidates")) {
          List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiRawResponse.get("candidates");
          if (!candidates.isEmpty() && candidates.get(0).containsKey("content")) {
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content.containsKey("parts")) {
              List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
              if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                return (String) parts.get(0).get("text"); // AI가 생성한 JSON 문자열 반환
              }
            }
          }
        }
        throw new IOException("Gemini API response does not contain expected 'text' part.");
      }
    } else {
      // 에러 스트림 읽기
      try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
        StringBuilder errorResponse = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
          errorResponse.append(responseLine.trim());
        }
        log.error("Gemini API Error Response: {}", errorResponse.toString());
        throw new IOException("Gemini API call failed with status code " + responseCode + ": " + errorResponse.toString());
      }
    }
  }

  // ======================================================================
  // PRIVATE HELPER METHOD for Tarot Card Initialization (for testing/demo)
  // ======================================================================

  /**
   * 애플리케이션 시작 시 타로 카드 데이터를 미리 DB에 추가하는 메서드 (개발/테스트용).
   * 실제 서비스에서는 관리자 페이지나 데이터 로딩 스크립트를 통해 데이터를 관리합니다.
   */
  @Transactional
  public void initializeTarotCards() {
    if (tarotCardRepository.count() == 0) { // 데이터가 없을 때만 초기화
      log.info("Initializing Tarot Card data...");
      // 메이저 아르카나 22장 예시 (간단히 일부만)
      List<TarotCard> initialCards = List.of(
          TarotCard.builder().name("The Fool").imageUrl("/images/tarot/0_the_fool.png").meaningUpright("새로운 시작, 순수, 잠재력").meaningReversed("무모함, 부주의, 어리석음").keywords("시작, 자유").build(),
          TarotCard.builder().name("The Magician").imageUrl("/images/tarot/1_the_magician.png").meaningUpright("힘, 기술, 집중, 행동").meaningReversed("미숙함, 우유부단, 조작").keywords("창조, 능력").build(),
          TarotCard.builder().name("The High Priestess").imageUrl("/images/tarot/2_the_high_priestess.png").meaningUpright("직관, 신비, 잠재의식").meaningReversed("비밀, 직감 부족, 침묵").keywords("지혜, 비밀").build(),
          TarotCard.builder().name("The Empress").imageUrl("/images/tarot/3_the_empress.png").meaningUpright("풍요, 다산, 자연, 모성").meaningReversed("의존, 질식, 공허").keywords("창조성, 성장").build(),
          TarotCard.builder().name("The Emperor").imageUrl("/images/tarot/4_the_emperor.png").meaningUpright("권위, 구조, 통제, 안정").meaningReversed("지배, 경직성, 냉혹함").keywords("리더십, 질서").build()
          // ... 나머지 카드들도 추가 (78장)
      );
      tarotCardRepository.saveAll(initialCards);
      log.info("Tarot Card data initialized with {} cards.", initialCards.size());
    }
  }
}
