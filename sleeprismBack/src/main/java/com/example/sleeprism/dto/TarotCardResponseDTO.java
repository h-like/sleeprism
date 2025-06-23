package com.example.sleeprism.dto;

import com.example.sleeprism.entity.TarotCard;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 타로 카드 정보를 클라이언트에게 응답하기 위한 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class TarotCardResponseDTO {
  private Long id;
  private String name;
  private String imageUrl;
  private String meaningUpright;
  private String meaningReversed;
  private String keywords;

  public TarotCardResponseDTO(TarotCard tarotCard) {
    this.id = tarotCard.getId();
    this.name = tarotCard.getName();
    this.imageUrl = tarotCard.getImageUrl();
    this.meaningUpright = tarotCard.getMeaningUpright();
    this.meaningReversed = tarotCard.getMeaningReversed();
    this.keywords = tarotCard.getKeywords();
  }
}
