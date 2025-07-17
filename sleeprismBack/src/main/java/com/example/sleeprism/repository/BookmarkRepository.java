package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Bookmark;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Book;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

  // 기존: public abstract boolean com.example.sleeprism.repository.BookmarkRepository.existByUser_IdAndPost_Id(java.lang.Long,java.lang.Long);
  // 문제의 메서드: Spring Data JPA 명명 규칙에 맞게 변경 SAVE_MY_WORK
  boolean existsByUser_IdAndPost_Id(Long userId, Long postId); // <-- user 엔티티의 id, post 엔티티의 id를 의미.

  // 혹은 다음과 같이 existsBy와 매개변수를 직접 참조하는 방법 (이 방법이 더 가독성 높고 권장됨)
  // Bookmark 엔티티에 User user; 와 Post post; 필드가 있다고 가정
  boolean existsByUserAndPost(User user, Post post); // User와 Post 객체 자체로 검색

  // 만약 User와 Post 객체가 아닌 ID만으로 검색하고 싶다면, User 엔티티와 Post 엔티티의 id 필드 이름을 따른다.
  // Bookmark 엔티티의 user 필드에 접근하여 그 user의 id 필드에 접근한다는 의미: user.id
  // Bookmark 엔티티의 post 필드에 접근하여 그 post의 id 필드에 접근한다는 의미: post.id
  // 그래서 _ (언더스코어)로 연결하여 사용한다.
  // existsBy[엔티티 필드명]_[엔티티 ID 필드명]And[다른 엔티티 필드명]_[다른 엔티티 ID 필드명]
  // 예를 들어, Bookmark 엔티티에 private User user; 와 private Post post; 가 있다면,
  // existsByUser_IdAndPost_Id(Long userId, Long postId); 가 올바른 형태입니다.

  // 추가로, 특정 북마크를 찾을 때 Optional<Bookmark>를 반환하도록 하는 것이 좋습니다.
  Optional<Bookmark> findByUser_IdAndPost_Id(Long userId, Long postId);

  // 북마크 삭제 시
  void deleteByUser_IdAndPost_Id(Long userId, Long postId); // ID로 직접 삭제

  // 특정 유저의 북마크 개수
  long countByUser_Id(Long userId);

  // 특정 게시글의 북마크 개수
  long countByPost_Id(Long postId);

  // 특정 유저의 북마크 리스트 (최신순)
  List<Bookmark> findByUser_IdOrderByCreatedAtDesc(Long userId);

  // 특정 게시글의 북마크 리스트 (최신순)
  List<Bookmark> findByPost_IdOrderByCreatedAtDesc(Long postId);
}
