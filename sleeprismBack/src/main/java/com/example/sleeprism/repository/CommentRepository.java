package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
  // 특정 포스트의 최상위 댓글(부모가 없는 댓글)을 최신순으로 조회 (삭제되지 않은 댓글만)
  // 대댓글은 parent 관계를 통해 자동으로 fetch될 수 있습니다.
  List<Comment> findByPost_IdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(Long postId);

  // 특정 유저가 작성한, 삭제되지 않은 댓글 조회 (최신순)
  List<Comment> findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

  // 특정 ID의 댓글 조회 (삭제되지 않은 댓글만)
  Optional<Comment> findByIdAndIsDeletedFalse(Long commentId);

  // 특정 부모 댓글의 자식 댓글 조회 (대댓글 조회) - (선택적: 보통 상위 쿼리에서 EAGER 로딩 또는 Batch Fetching으로 처리)
  List<Comment> findByParent_IdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentCommentId);

  @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.children WHERE c.post.id = :postId AND c.parent IS NULL AND c.isDeleted = false ORDER BY c.createdAt DESC")
  List<Comment> findTopLevelCommentsWithChildrenByPostId(@Param("postId") Long postId);
}