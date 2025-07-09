package com.example.sleeprism.repository;

import com.example.sleeprism.entity.TarotCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarotCardRepository extends JpaRepository<TarotCard, Long> {
  // 카드 이름으로 타로 카드 조회
  Optional<TarotCard> findByName(String name);

  // MySQL, MariaDB, H2는 RAND() / PostgreSQL은 RANDOM()
  @Query(value = "SELECT * FROM tarot_card ORDER BY RAND() LIMIT :count", nativeQuery = true)
  List<TarotCard> findRandomCards(@Param("count") int count);
}
