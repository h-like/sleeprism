package com.example.sleeprism.service;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.repository.PostRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountEventListener {
  private final PostRepository postRepository;
  private final TransactionTemplate transactionTemplate;

  @EventListener
  @Async // 비동기로 처리 (Spring Boot @EnableAsync 필요)
  public void handleViewCountIncrement(ViewCountIncrementEvent event) {
    Long postId = event.getPostId();
    final int MAX_RETRIES = 20; // 비동기 로직에서도 재시도는 필요
    for (int i = 0; i < MAX_RETRIES; i++) {
      try {
        transactionTemplate.execute(status -> {
          Post post = postRepository.findById(postId) // isDeletedFalse는 이 컨텍스트에서 다시 체크
              .orElse(null); // 없을 수도 있음

          if (post != null && !post.isDeleted()) {
            post.incrementViewCount();
            postRepository.saveAndFlush(post);
          }
          return null; // void 처리
        });
        break; // 성공하면 루프 탈출
      } catch (OptimisticLockingFailureException | OptimisticLockException e) {
        // 재시도 로직
        try { Thread.sleep(50L * (long)Math.pow(2, i)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
      } catch (Exception e) {
        // 로깅 (비동기 처리이므로 사용자에게 직접 에러를 보낼 수 없음)
        log.error("Failed to increment view count for post {}: {}", postId, e.getMessage(), e);
        break; // 다른 예상치 못한 에러는 재시도하지 않음
      }
    }
  }
}
