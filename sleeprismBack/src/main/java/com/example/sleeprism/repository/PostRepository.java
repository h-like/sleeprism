package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.PostCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
  // JpaRepository<엔티티 타입, 엔티티의 ID 타입>

  // 1. 특정 사용자가 작성한 게시글을 최신순으로 조회 (삭제되지 않은 게시글만)
  List<Post> findByOriginalAuthor_IdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId); // <-- 이 부분을 수정!

  // 2. 제목 또는 내용으로 게시글 검색 (삭제되지 않은 게시글만)
  // 네이티브 쿼리를 사용하여 데이터베이스 SQL 함수를 직접 호출
  @Query(value = "SELECT * FROM post p " + // 실제 DB 테이블 이름 사용 (post)
      "WHERE p.is_deleted = false AND " + // is_deleted 컬럼 이름 사용
      "(LOWER(p.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')) OR " +
      "LOWER(p.content) LIKE LOWER(CONCAT('%', :contentKeyword, '%'))) " +
      "ORDER BY p.created_at DESC", // created_at 컬럼 이름 사용
      nativeQuery = true)
  // <-- 네이티브 쿼리임을 명시
  List<Post> searchByTitleOrContentAndNotDeleted(
      @Param("titleKeyword") String titleKeyword,
      @Param("contentKeyword") String contentKeyword
  );

  // 3. 카테고리별 게시글 조회 (삭제되지 않은 게시글만)
  List<Post> findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(PostCategory category);

  // 4. 특정 게시글 ID로 조회 (삭제되지 않은 게시글만)
  // Optional을 사용하여 게시글이 존재하지 않을 경우를 안전하게 처리
  Optional<Post> findByIdAndIsDeletedFalse(Long id);

  // 5. 조회수 상위 N개 게시글 조회 (삭제되지 않은 게시글만)
  // JPQL을 사용하여 직접 쿼리 작성. LIMIT은 JPA Pageable로 처리하는 것이 더 일반적
  @Query(value = "SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY p.viewCount DESC, p.createdAt DESC")
  List<Post> findTopNByViewCount(org.springframework.data.domain.Pageable pageable);

  // 6. 좋아요 개수 상위 N개 게시글 조회 (삭제되지 않은 게시글만) - PostLike 엔티티의 likes 관계를 활용
  @Query(value = "SELECT p FROM Post p LEFT JOIN p.likes l WHERE p.isDeleted = false GROUP BY p ORDER BY COUNT(l) DESC, p.createdAt DESC")
  List<Post> findTopNByLikes(org.springframework.data.domain.Pageable pageable);

  // 7. 특정 유저가 작성한, 삭제되지 않은 게시글의 개수
  long countByOriginalAuthor_IdAndIsDeletedFalse(Long userId); // <-- 이 부분을 수정!

  // 8. 특정 카테고리의 삭제되지 않은 게시글 개수
  long countByCategoryAndIsDeletedFalse(PostCategory category);

  // 9. 모든 삭제되지 않은 게시글을 최신순으로 조회
  List<Post> findByIsDeletedFalseOrderByCreatedAtDesc();

  // 10. 현재 소유자()로 게시글 조회 메서드 현재 소유자(currentOwner)로 게시글 조회 메서드
  List<Post> findByCurrentOwner_IdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId); // <-- 이 부분을 추가!

  // FIX: 비관적 잠금을 적용한 findById 메서드 추가
  @Lock(LockModeType.PESSIMISTIC_WRITE) // 쓰기 잠금
  @Query("select p from Post p where p.id = :id and p.isDeleted = false")
  // isDeleted 조건 추가
  Optional<Post> findById(@Param("id") Long id, LockModeType lockModeType);

  // 방법 2a: comments만 fetch하고 likes는 Lazy 로딩
  @Query("SELECT p FROM Post p LEFT JOIN FETCH p.comments WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
  List<Post> findByIsDeletedFalseOrderByCreatedAtDescFetchComments();

  // 방법 2b: Likes만 fetch하고 comments는 Lazy 로딩 (필요에 따라)
  @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
  List<Post> findByIsDeletedFalseOrderByCreatedAtDescFetchLikes();

  // 새롭게 추가: 여러 카테고리 목록으로 게시글 조회 (IN 쿼리)
  List<Post> findByCategoryInAndIsDeletedFalseOrderByCreatedAtDesc(Collection<PostCategory> categories);

  // 새로 추가: 인기 게시글 조회 (좋아요 수, 북마크 수, 조회수 내림차순)
  // isDeleted = FALSE인 게시글만 가져오도록 명시적으로 조건 추가
  List<Post> findByIsDeletedFalseOrderByLikeCountDescBookmarkCountDescViewCountDesc();

  // 새로 추가: 특정 기간 내의 인기 게시글 조회
  // isDeleted = FALSE이고 createdAt이 startDate와 endDate 사이인 게시글을
  // 좋아요 수, 북마크 수, 조회수 순으로 내림차순 정렬
  List<Post> findByIsDeletedFalseAndCreatedAtBetweenOrderByLikeCountDescBookmarkCountDescViewCountDesc(LocalDateTime startDate, LocalDateTime endDate);
}