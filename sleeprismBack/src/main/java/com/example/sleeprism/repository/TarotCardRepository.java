package com.example.sleeprism.repository;

import com.example.sleeprism.entity.TarotCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TarotCardRepository extends JpaRepository<TarotCard, Long> {
  // 카드 이름으로 타로 카드 조회
  Optional<TarotCard> findByName(String name);
}
