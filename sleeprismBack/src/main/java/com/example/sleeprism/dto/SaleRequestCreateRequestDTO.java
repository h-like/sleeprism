package com.example.sleeprism.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 판매 요청 생성을 위한 데이터 전송 객체 (DTO)입니다.
 * 구매자가 제시하는 가격을 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SaleRequestCreateRequestDTO {
  @NotNull(message = "게시글 ID는 필수입니다.")
  @Min(value = 1, message = "게시글 ID는 1 이상이어야 합니다.")
  private Long postId; // 어떤 게시글에 대한 요청인지

  @NotNull(message = "제시 가격은 필수입니다.")
  @Min(value = 100, message = "제시 가격은 최소 100원 이상이어야 합니다.") // 최소 가격 정책에 따라 조정 가능
  private Integer proposedPrice; // 구매자가 제시하는 가격
}
