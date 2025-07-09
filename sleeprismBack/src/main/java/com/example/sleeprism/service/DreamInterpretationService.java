package com.example.sleeprism.service;

//import com.example.sleeprism.config.WebClientConfig; // WebClient 설정을 위해 추가될 수 있음
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
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient; // WebClient로 변경

import java.io.IOException;
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
  private final ObjectMapper objectMapper;
  private final WebClient.Builder webClientBuilder; // WebClient.Builder 주입

  @Value("${gemini.api.key:}")
  private String geminiApiKey;

  // application.properties에서 관리하도록 변경
  @Value("${dream.interpretation.options-count:3}")
  private int numInterpretationOptions;


  @Transactional
  public DreamInterpretationResponseDTO interpretDream(DreamInterpretationRequestDTO requestDto, Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    Post post = postRepository.findByIdAndIsDeletedFalse(requestDto.getPostId())
        .orElseThrow(() -> new EntityNotFoundException("Post not found or deleted with ID: " + requestDto.getPostId()));

    // 1. 입력값 전처리 (HTML 태그 제거)
    String rawContent = post.getContent();
    String sanitizedContent = Jsoup.clean(rawContent, Safelist.none());

    // 2. 프롬프트 생성
    String prompt = String.format(
        "다음 꿈 내용을 해몽해주세요. 꿈의 상징들을 분석하고, 심리적인 의미, 가능한 미래 암시 등 다양한 관점에서 %d가지의 간결한 해석을 각각 JSON 형태로 반환해주세요. 각 해석은 'title'과 'content' 필드를 포함해야 합니다. 꿈 내용: \"%s\"",
        numInterpretationOptions, sanitizedContent
    );

    // 3. WebClient를 사용하여 Gemini API 호출
    String aiResponseJson;
    try {
      aiResponseJson = callGeminiApi(prompt);
    } catch (Exception e) {
      log.error("Failed to call Gemini API for post {}: {}", post.getId(), e.getMessage(), e);
      return DreamInterpretationResponseDTO.builder()
          .errorMessage("꿈 해몽 AI API 호출에 실패했습니다: " + e.getMessage())
          .build();
    }

    // 4. 엔티티 저장
    DreamInterpretation dreamInterpretation = DreamInterpretation.builder()
        .post(post)
        .user(user)
        .aiResponseContent(aiResponseJson)
        .build();
    DreamInterpretation savedInterpretation = dreamInterpretationRepository.save(dreamInterpretation);

    // 5. 최종 응답 DTO 생성 후 반환
    return buildResponseDTO(savedInterpretation);
  }

  // selectInterpretationOption, getDreamInterpretationById 등 다른 서비스 메서드는 변경 없음...
  @Transactional
  public DreamInterpretationResponseDTO selectInterpretationOption(
      Long interpretationId, DreamInterpretationSelectRequestDTO requestDto, Long userId) {
    DreamInterpretation dreamInterpretation = dreamInterpretationRepository.findById(interpretationId)
        .orElseThrow(() -> new EntityNotFoundException("Dream interpretation not found with ID: " + interpretationId));

    if (!dreamInterpretation.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to select an option for this interpretation.");
    }

    TarotCard selectedTarotCard = tarotCardRepository.findById(requestDto.getSelectedTarotCardId())
        .orElseThrow(() -> new EntityNotFoundException("Selected Tarot Card not found with ID: " + requestDto.getSelectedTarotCardId()));

    dreamInterpretation.selectInterpretation(requestDto.getSelectedOptionIndex(), selectedTarotCard);
    return buildResponseDTO(dreamInterpretation);
  }

  public DreamInterpretationResponseDTO getDreamInterpretationById(Long interpretationId, Long userId) {
    DreamInterpretation dreamInterpretation = dreamInterpretationRepository.findById(interpretationId)
        .orElseThrow(() -> new EntityNotFoundException("Dream interpretation not found with ID: " + interpretationId));

    if (!dreamInterpretation.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to view this interpretation.");
    }
    return buildResponseDTO(dreamInterpretation);
  }

  // DreamInterpretationService.java
  public List<DreamInterpretationResponseDTO> getMyDreamInterpretations(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    List<DreamInterpretation> interpretations = dreamInterpretationRepository.findByUserOrderByInterpretedAtDesc(user);

    // 각 해몽 기록을 DTO로 변환하여 리스트로 만듭니다.
    return interpretations.stream()
        .map(this::buildResponseDTO) // 이미 만들어둔 DTO 변환 헬퍼 메서드 재사용
        .collect(Collectors.toList());
  }


  // DTO 생성 헬퍼 메서드
  private DreamInterpretationResponseDTO buildResponseDTO(DreamInterpretation interpretation) {
    try {
      // 1. AI 응답 JSON 파싱
      Map<String, List<Map<String, String>>> aiResponseMap = objectMapper.readValue(
          interpretation.getAiResponseContent(),
          new TypeReference<>() {}
      );
      List<Map<String, String>> interpretationsData = aiResponseMap.getOrDefault("interpretations", Collections.emptyList());

      // 2. 타로카드 매칭 (최적화된 쿼리 사용)
      List<TarotCard> randomCards = tarotCardRepository.findRandomCards(numInterpretationOptions);

      List<InterpretationOptionDTO> options = new ArrayList<>();
      for (int i = 0; i < interpretationsData.size(); i++) {
        Map<String, String> data = interpretationsData.get(i);
        TarotCard matchedCard = (i < randomCards.size()) ? randomCards.get(i) : null;

        options.add(InterpretationOptionDTO.builder()
            .optionIndex(i)
            .title(data.get("title"))
            .content(data.get("content"))
            .tarotCardId(matchedCard != null ? matchedCard.getId() : null)
            .tarotCardName(matchedCard != null ? matchedCard.getName() : null)
            .tarotCardImageUrl(matchedCard != null ? matchedCard.getImageUrl() : null)
            .build());
      }

      // 3. 최종 DTO 빌드
      return DreamInterpretationResponseDTO.builder()
          .id(interpretation.getId())
          .postId(interpretation.getPost().getId())
          .userId(interpretation.getUser().getId())
          .userName(interpretation.getUser().getNickname())
          .interpretedAt(interpretation.getInterpretedAt())
          .interpretationOptions(options)
          .selectedOptionIndex(interpretation.getSelectedInterpretationIndex())
          .selectedTarotCardId(interpretation.getSelectedTarotCard() != null ? interpretation.getSelectedTarotCard().getId() : null)
          .selectedTarotCardName(interpretation.getSelectedTarotCard() != null ? interpretation.getSelectedTarotCard().getName() : null)
          .selectedTarotCardImageUrl(interpretation.getSelectedTarotCard() != null ? interpretation.getSelectedTarotCard().getImageUrl() : null)
          .build();

    } catch (JsonProcessingException e) {
      log.error("Failed to parse AI response content for DreamInterpretation ID {}: {}", interpretation.getId(), e.getMessage());
      return DreamInterpretationResponseDTO.builder()
          .id(interpretation.getId())
          .errorMessage("AI 응답을 처리하는 데 실패했습니다.")
          .build();
    }
  }

  // Gemini API 호출 메서드 (WebClient로 교체)
  private String callGeminiApi(String prompt) throws IOException {
    WebClient webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();

    // 요청 본문을 Map 객체로 안전하게 생성
    Map<String, Object> textPart = Map.of("text", prompt);
    Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(textPart));
    Map<String, Object> schemaProperty = Map.of("type", "STRING");
    Map<String, Object> interpretationsProperty = Map.of(
        "type", "ARRAY",
        "items", Map.of(
            "type", "OBJECT",
            "properties", Map.of("title", schemaProperty, "content", schemaProperty)
        )
    );
    Map<String, Object> responseSchema = Map.of(
        "type", "OBJECT",
        "properties", Map.of("interpretations", interpretationsProperty)
    );
    Map<String, Object> generationConfig = Map.of(
        "responseMimeType", "application/json",
        "responseSchema", responseSchema
    );
    Map<String, Object> requestBody = Map.of(
        "contents", List.of(userContent),
        "generationConfig", generationConfig
    );

    // WebClient를 사용하여 API 호출
    String rawResponse = webClient.post()
        .uri("/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .block(); // 동기식으로 결과를 기다림

    // Gemini 응답에서 실제 결과 JSON 추출
    Map<String, Object> geminiRawResponse = objectMapper.readValue(rawResponse, new TypeReference<>() {});
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
}
