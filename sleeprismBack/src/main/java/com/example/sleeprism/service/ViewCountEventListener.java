package com.example.sleeprism.service;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.event.ViewCountIncrementEvent;
import com.example.sleeprism.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountEventListener {

  private final PostRepository postRepository;

  /**
   * 게시글 조회수 증가 이벤트를 비동기적으로 처리합니다.
   * @Async: 이 메소드를 별도의 스레드에서 실행합니다.
   * @Transactional(propagation = Propagation.REQUIRES_NEW):
   * - 이 메소드가 항상 새로운 트랜잭션에서 실행되도록 보장합니다.
   * - 이를 통해 게시글을 조회하는 원래 트랜잭션과의 충돌을 방지하고,
   * 'OptimisticLockingFailureException'을 해결합니다.
   */
  @Async
  @EventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleViewCountIncrement(ViewCountIncrementEvent event) {
    try {
      log.info("조회수 증가 이벤트 수신: postId={}", event.getPostId());
      Post post = postRepository.findById(event.getPostId())
          .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + event.getPostId()));

      // 엔티티의 상태를 직접 변경하는 대신, 엔티티가 제공하는 비즈니스 메소드를 호출합니다.
      post.incrementViewCount();

      postRepository.save(post);
      log.info("게시글 ID {}의 조회수가 성공적으로 증가했습니다.", post.getId());
    } catch (OptimisticLockingFailureException e) {
      log.warn("조회수 증가 중 동시성 충돌 발생 (postId: {}). 재시도하지 않음: {}", event.getPostId(), e.getMessage());
    } catch (Exception e) {
      log.error("조회수 증가 처리 중 예외 발생 (postId: {}): {}", event.getPostId(), e.getMessage());
    }
  }
}
