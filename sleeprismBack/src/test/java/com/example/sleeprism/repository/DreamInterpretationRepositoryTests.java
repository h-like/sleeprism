package com.example.sleeprism.repository;

import com.example.sleeprism.entity.DreamInterpretation;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DreamInterpretationRepositoryTests {

  /*
  @Autowired
  private DreamInterpretationRepository dreamInterpretationRepository;

  @Autowired
  private TestEntityManager entityManager; // DB 상태를 직접 제어하기 위해 사용

  private User testUser1;
  private User testUser2;
  private Post testPost1;
  private Post testPost2;
  private DreamInterpretation di1;
  private DreamInterpretation di2;
  private DreamInterpretation di3;

  @BeforeEach
  void setUp() {
    // 테스트를 위한 더미 데이터 초기화 및 저장
    // 각 테스트 메서드가 독립적으로 실행될 수 있도록 BeforeEach에서 초기화
    testUser1 = User.builder().nickname("테스트유저1").email("user1@example.com").build();
    testUser2 = User.builder().nickname("테스트유저2").email("user2@example.com").build();
    entityManager.persist(testUser1);
    entityManager.persist(testUser2);

    testPost1 = Post.builder().title("꿈1").content("이상한 꿈을 꾸었다.").finalize(testUser1).build(); // user 필드명을 originalAuthor로 변경했으니 이곳도 변경
    testPost2 = Post.builder().title("꿈2").content("행복한 꿈을 꾸었다.").finalize(testUser2).build(); // user 필드명을 originalAuthor로 변경했으니 이곳도 변경
    entityManager.persist(testPost1); // 이 시점에 ID가 할당됩니다.
    entityManager.persist(testPost2);

    di1 = DreamInterpretation.builder()
        .post(testPost1)
        .user(testUser1)
        .aiResponseContent("{\"interpretations\": [{\"title\": \"해석1\", \"content\": \"내용1\"}]}")
//        .interpretedAt(LocalDateTime.now().minusDays(1)) // 어제
        .build();

    di2 = DreamInterpretation.builder()
        .post(testPost1)
        .user(testUser1)
        .aiResponseContent("{\"interpretations\": [{\"title\": \"해석2\", \"content\": \"내용2\"}]}")
//        .interpretedAt(LocalDateTime.now()) // 오늘 (최신)
        .build();

    di3 = DreamInterpretation.builder()
        .post(testPost2)
        .user(testUser2)
        .aiResponseContent("{\"interpretations\": [{\"title\": \"해석3\", \"content\": \"내용3\"}]}")
//        .interpretedAt(LocalDateTime.now().minusHours(2))
        .build();

    entityManager.persist(di1);
    entityManager.persist(di2);
    entityManager.persist(di3);
    entityManager.flush(); // 변경사항을 DB에 즉시 반영
    entityManager.clear(); // 영속성 컨텍스트 초기화 (깨끗한 상태에서 조회하기 위함)
  }

  @Test
  @DisplayName("꿈 해몽 기록을 저장한다")
  void saveDreamInterpretation() {
    DreamInterpretation newDI = DreamInterpretation.builder()
        .post(testPost1)
        .user(testUser1)
        .aiResponseContent("{\"interpretations\": [{\"title\": \"새로운 해석\", \"content\": \"새로운 내용\"}]}")
        .build();

    DreamInterpretation savedDI = dreamInterpretationRepository.save(newDI);

    assertThat(savedDI).isNotNull();
    assertThat(savedDI.getId()).isNotNull();
    assertThat(savedDI.getPost().getId()).isEqualTo(testPost1.getId());
    assertThat(savedDI.getUser().getId()).isEqualTo(testUser1.getId());
  }

  @Test
  @DisplayName("특정 게시글과 사용자에 대한 최신 해몽 기록을 조회한다")
  void findByPostAndUserOrderByInterpretedAtDesc() {
    Optional<DreamInterpretation> found = dreamInterpretationRepository.findByPostAndUserOrderByInterpretedAtDesc(testPost1, testUser1);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(di2.getId()); // di2가 가장 최신이므로
    assertThat(found.get().getInterpretedAt()).isAfterOrEqualTo(di1.getInterpretedAt());
  }

  @Test
  @DisplayName("특정 게시글과 사용자에 대한 해몽 기록이 없을 때 Optional.empty()를 반환한다")
  void findByPostAndUserOrderByInterpretedAtDesc_NotFound() {
    // 존재하지 않는 게시글과 사용자 조합
    Post nonExistentPost = Post.builder().id(999L).title("없음").content("없음").user(testUser1).build(); // ID만 설정 (DB에는 없음)
    Optional<DreamInterpretation> found = dreamInterpretationRepository.findByPostAndUserOrderByInterpretedAtDesc(nonExistentPost, testUser1);

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("특정 게시글에 대한 모든 해몽 기록을 최신순으로 조회한다")
  void findByPostOrderByInterpretedAtDesc() {
    List<DreamInterpretation> interpretations = dreamInterpretationRepository.findByPostOrderByInterpretedAtDesc(testPost1);

    assertThat(interpretations).hasSize(2);
    assertThat(interpretations.get(0).getId()).isEqualTo(di2.getId()); // 최신순
    assertThat(interpretations.get(1).getId()).isEqualTo(di1.getId());
  }

  @Test
  @DisplayName("특정 사용자가 요청한 모든 해몽 기록을 최신순으로 조회한다")
  void findByUserOrderByInterpretedAtDesc() {
    List<DreamInterpretation> interpretations = dreamInterpretationRepository.findByUserOrderByInterpretedAtDesc(testUser1);

    assertThat(interpretations).hasSize(2);
    assertThat(interpretations.get(0).getId()).isEqualTo(di2.getId()); // 최신순
    assertThat(interpretations.get(1).getId()).isEqualTo(di1.getId());

    List<DreamInterpretation> interpretations2 = dreamInterpretationRepository.findByUserOrderByInterpretedAtDesc(testUser2);
    assertThat(interpretations2).hasSize(1);
    assertThat(interpretations2.get(0).getId()).isEqualTo(di3.getId());
  }

  @Test
  @DisplayName("특정 사용자가 요청한 해몽 기록이 없을 때 빈 리스트를 반환한다")
  void findByUserOrderByInterpretedAtDesc_NoInterpretations() {
    User userWithNoInterpretations = User.builder().nickname("새로운유저").email("newuser@example.com").build();
    entityManager.persist(userWithNoInterpretations);
    entityManager.flush();

    List<DreamInterpretation> interpretations = dreamInterpretationRepository.findByUserOrderByInterpretedAtDesc(userWithNoInterpretations);

    assertThat(interpretations).isEmpty();
  }
  */
}