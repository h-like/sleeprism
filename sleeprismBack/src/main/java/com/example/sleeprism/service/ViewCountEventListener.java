package com.example.sleeprism.service;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.event.ViewCountIncrementEvent;
import jakarta.persistence.EntityManager; // EntityManager 임포트
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.Propagation;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountEventListener {
  private final PostRepository postRepository;
  private final EntityManager entityManager; // FIX: EntityManager 주입

  /**
   * 게시글 조회수 증가 이벤트를 처리합니다.
   * - @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT):
   * 이벤트 발행 트랜잭션이 성공적으로 커밋된 후에만 이 리스너가 실행되도록 보장합니다.
   * - @Async: 별도의 스레드에서 비동기적으로 실행됩니다. (Spring Boot 애플리케이션에 @EnableAsync 필요)
   * - @Transactional(propagation = Propagation.REQUIRES_NEW):
   * 이 리스너 메서드 자체를 항상 새로운 독립적인 트랜잭션으로 묶습니다.
   * 이는 기존 트랜잭션과의 충돌을 방지하면서 조회수 업데이트를 안전하게 처리합니다.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleViewCountIncrement(ViewCountIncrementEvent event) {
    Long postId = event.getPostId();
    final int MAX_RETRIES = 20;
    for (int i = 0; i < MAX_RETRIES; i++) {
      try {
        Post post = postRepository.findById(postId)
            .orElse(null);

        if (post != null && !post.isDeleted()) {
          // FIX: 엔티티 매니저를 통해 엔티티를 강제로 새로고침하여 최신 상태를 보장합니다.
          // 이는 낙관적 잠금 충돌을 줄이는 데 매우 효과적입니다.
          entityManager.refresh(post);

          post.incrementViewCount();
          postRepository.saveAndFlush(post);
          log.info("View count for post {} incremented successfully.", postId);
        } else if (post == null) {
          log.warn("Post with ID {} not found for view count increment.", postId);
        } else { // post.isDeleted() == true
          log.warn("View count not incremented for deleted post with ID {}.", postId);
        }
        break;
      } catch (OptimisticLockingFailureException | OptimisticLockException e) {
        log.warn("Optimistic lock conflict for post {}. Retrying (attempt {}/{})", postId, i + 1, MAX_RETRIES);
        try { Thread.sleep(50L * (long)Math.pow(2, i)); } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.error("View count increment retry interrupted for post {}.", postId);
          break;
        }
      } catch (Exception e) {
        log.error("An unexpected error occurred while incrementing view count for post {}: {}", postId, e.getMessage(), e);
        break;
      }
    }
  }
}
