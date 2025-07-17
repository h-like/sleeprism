package com.example.sleeprism;

import com.example.sleeprism.service.DreamInterpretationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 예를 들어, SleeprismApplication 클래스나 별도의 설정 클래스에 추가
@Component
@RequiredArgsConstructor
public class DataLoader {
  private final DreamInterpretationService dreamInterpretationService;

  @EventListener(ApplicationReadyEvent.class)
  public void loadData() {
//    dreamInterpretationService.initializeTarotCards();
  }
}