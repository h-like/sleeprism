package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.SaleRequest;
import com.example.sleeprism.entity.SaleRequestStatus;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRequestRepositroy extends JpaRepository<SaleRequest, Long> {
  // 특정 게시글(Post)에 대한 모든 판매 요청 조회 (최신순)
  List<SaleRequest> findByPostOrderByCreatedAtDesc(Post post); // <-- requestedAt -> CreatedAt으로 변경

  // 특정 게시글(Post) ID에 대한 모든 판매 요청 조회 (최신순)
  List<SaleRequest> findByPost_IdOrderByCreatedAtDesc(Long postId); // <-- requestedAt -> CreatedAt으로 변경

  // 특정 구매자가 보낸 모든 판매 요청 조회 (최신순)
  List<SaleRequest> findByBuyerOrderByCreatedAtDesc(User buyer); // <-- requestedAt -> CreatedAt으로 변경

  // 특정 구매자가 보낸 특정 상태의 판매 요청 조회 (예: PENDING 상태의 요청)
  List<SaleRequest> findByBuyerAndStatusOrderByCreatedAtDesc(User buyer, SaleRequestStatus status); // <-- requestedAt -> CreatedAt으로 변경

  // 특정 게시글의 원본 작성자(판매자)에게 들어온 모든 판매 요청 조회 (최신순)
  List<SaleRequest> findByPost_OriginalAuthorOrderByCreatedAtDesc(User originalAuthor); // <-- requestedAt -> CreatedAt으로 변경

  // 특정 게시글의 원본 작성자(판매자)에게 들어온 특정 상태의 판매 요청 조회
  List<SaleRequest> findByPost_OriginalAuthorAndStatusOrderByCreatedAtDesc(User originalAuthor, SaleRequestStatus status); // <-- requestedAt -> CreatedAt으로 변경

  // 특정 게시글에 대해 특정 구매자가 보낸 PENDING 상태의 판매 요청이 있는지 확인 (중복 요청 방지 등)
  Optional<SaleRequest> findByPostAndBuyerAndStatus(Post post, User buyer, SaleRequestStatus status);

  // 에스크로 트랜잭션 ID로 판매 요청 조회 (PG사 연동 시 유용)
  Optional<SaleRequest> findByEscrowTransactionId(String escrowTransactionId);
}
