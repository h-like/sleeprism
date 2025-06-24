package com.example.sleeprism.event; // 적절한 패키지명으로 변경해주세요 (예: com.example.sleeprism.event)

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ViewCountIncrementEvent extends ApplicationEvent {
  private final Long postId;

  public ViewCountIncrementEvent(Object source, Long postId) {
    super(source);
    this.postId = postId;
  }

  // 편의를 위해 source를 this로 전달하는 생성자도 추가
  public ViewCountIncrementEvent(Long postId) {
    this(new Object(), postId); // 'this'를 source로 사용해도 무방
  }
}