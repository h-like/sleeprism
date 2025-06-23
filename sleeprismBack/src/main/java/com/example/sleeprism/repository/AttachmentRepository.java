package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Attachment;
import com.example.sleeprism.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {


  // 특정 게시글(Post)에 속한 모든 첨부 파일을 조회
  List<Attachment> findByPost(Post post);

  // 특정 게시글(Post) ID에 속한 모든 첨부 파일을 조회
  List<Attachment> findByPost_Id(Long postId);

  // 특정 첨부 파일 ID와 해당 게시글(Post) ID로 첨부 파일 조회
  Optional<Attachment> findByIdAndPost_Id(Long id, Long postId);

  // 특정 게시글에 속한 첨부 파일 개수 조회
  long countByPost(Post post);

  // 특정 게시글에 속한 첨부 파일들을 삭제
  void deleteByPost(Post post); // @Transactional과 함께 사용해야 함
}
