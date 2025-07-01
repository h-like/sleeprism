package com.example.sleeprism.repository;

import com.example.sleeprism.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository  extends JpaRepository<PostLike, Long> {

  // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
  Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

  // 특정 게시글의 좋아요 개수를 세는 메서드 (Post 엔티티의 likeCount 업데이트를 위해 필요)
  long countByPostId(Long postId);

  // 특정 사용자가 특정 게시글에 누른 좋아요를 삭제하는 메서드
  void deleteByUserIdAndPostId(Long userId, Long postId);

  Optional<PostLike> findByUser_IdAndPost_Id(Long userId, Long postId); // 특정 유저가 특정 포스트에 좋아요를 눌렀는지
  List<PostLike> findByPost_Id(Long postId); // 특정 포스트의 좋아요 목록
  List<PostLike> findByUser_Id(Long userId); // 특정 유저가 누른 좋아요 목록
  boolean existsByUser_IdAndPost_Id(Long userId, Long postId); // 특정 유저가 특정 포스트에 좋아요를 눌렀는지 확인
  long countByPost_Id(Long postId); // 특정 포스트의 좋아요 개수
}
