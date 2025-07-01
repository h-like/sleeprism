package com.example.sleeprism.service;

import com.example.sleeprism.event.ViewCountIncrementEvent;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException; // 이 임포트 추가
import org.springframework.scheduling.annotation.Async; // @Async 어노테이션 사용 시 필요
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 관리를 위해 추가

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountEventListener {

  private final PostRepository postRepository;

  @Async // 비동기로 이벤트 처리
  @EventListener
  @Transactional // 트랜잭션 내에서 실행
  public void handleViewCountIncrement(ViewCountIncrementEvent event) {
    Long postId = event.getPostId();
    int maxRetries = 3; // 최대 재시도 횟수
    long retryDelayMs = 100; // 재시도 간 지연 시간 (밀리초)

    for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
      try {
        // postId로 Post 엔티티를 조회하고, 조회수 증가
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));

        post.incrementViewCount(); // 조회수 증가
        postRepository.saveAndFlush(post); // 변경사항 즉시 DB 반영
        log.info("View count for post {} incremented successfully. (Retry: {})", postId, retryCount);
        return; // 성공 시 메서드 종료
      } catch (ObjectOptimisticLockingFailureException e) {
        // 옵티미스틱 락킹 실패 시 재시도
        log.warn("Optimistic locking failure while incrementing view count for post {}. Retrying... (Attempt {}/{})", postId, retryCount + 1, maxRetries);
        if (retryCount < maxRetries - 1) {
          try {
            Thread.sleep(retryDelayMs); // 잠시 대기 후 재시도
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during retry delay.", ie);
            break; // 인터럽트 발생 시 루프 종료
          }
        }
      } catch (Exception e) {
        log.error("An unexpected error occurred while incrementing view count for post {}: {}", postId, e.getMessage(), e);
        break; // 다른 유형의 오류 발생 시 재시도 없이 종료
      }
    }
    log.error("Failed to increment view count for post {} after {} retries.", postId, maxRetries);
  }
}
