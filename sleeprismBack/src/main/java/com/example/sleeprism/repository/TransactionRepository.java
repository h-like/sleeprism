package com.example.sleeprism.repository;

import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.Transaction;
import com.example.sleeprism.entity.TransactionStatus;
import com.example.sleeprism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  // 특정 게시글(Post)과 관련된 모든 거래 내역 조회 (최신순)
  List<Transaction> findByPostOrderByTransactionDateDesc(Post post);

  // 특정 구매자가 관련된 모든 거래 내역 조회 (최신순)
  List<Transaction> findByBuyerOrderByTransactionDateDesc(User buyer);

  // 특정 판매자가 관련된 모든 거래 내역 조회 (최신순)
  List<Transaction> findBySellerOrderByTransactionDateDesc(User seller);

  // 특정 판매 요청(SaleRequest)에 의해 발생한 거래 조회 (1:1 관계)
  Optional<Transaction> findBySaleRequestId(Long saleRequestId);

  // 특정 상태의 거래 내역 조회 (예: COMPLETED 상태의 거래만)
  List<Transaction> findByStatusOrderByTransactionDateDesc(TransactionStatus status);

  // 외부 트랜잭션 ID로 거래 조회 (에스크로 PG사 연동 시 필수)
  Optional<Transaction> findByExternalTransactionId(String externalTransactionId);
}
