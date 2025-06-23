package com.example.sleeprism.repository;

import com.example.sleeprism.entity.DreamInterpretation;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DreamInterpretationRepository extends JpaRepository<DreamInterpretation, Long> {

  // 특정 꿈 게시글(Post)에 대한 사용자의 해몽 기록 조회 (최신순)
  // 한 게시글에 대해 한 사용자는 하나의 해몽 기록만 갖는다고 가정하거나, 여러 번 요청 시 각 기록이 남는다고 가정
  Optional<DreamInterpretation> findByPostAndUserOrderByInterpretedAtDesc(Post post, User user);

  // 특정 꿈 게시글(Post)에 대한 모든 해몽 기록 조회 (어떤 사용자가 요청했든)
  List<DreamInterpretation> findByPostOrderByInterpretedAtDesc(Post post);

  // 특정 사용자가 요청한 모든 해몽 기록 조회 (최신순)
  List<DreamInterpretation> findByUserOrderByInterpretedAtDesc(User user);
}
