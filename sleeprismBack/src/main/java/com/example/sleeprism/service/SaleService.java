package com.example.sleeprism.service;

import com.example.sleeprism.dto.OwnedPostResponseDTO;
import com.example.sleeprism.dto.SaleRequestCreateRequestDTO;
import com.example.sleeprism.dto.SaleRequestResponseDTO;
import com.example.sleeprism.entity.NotificationType; // NotificationType import 추가
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.entity.SaleRequest;
import com.example.sleeprism.entity.SaleRequestStatus;
import com.example.sleeprism.entity.Transaction;
import com.example.sleeprism.entity.TransactionStatus;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.PostRepository;
import com.example.sleeprism.repository.SaleRequestRepositroy;
import com.example.sleeprism.repository.TransactionRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Slf4j import 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j // 로그 추가
public class SaleService {

  private final SaleRequestRepositroy saleRequestRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final TransactionRepository transactionRepository;
  private final NotificationService notificationService; // NotificationService 주입

  /**
   * 구매자가 특정 게시글에 대한 판매 요청을 생성합니다.
   * 판매 요청 생성 시 게시글 원본 작성자(판매자)에게 알림을 생성합니다.
   * @param buyerId 구매자 사용자 ID
   * @param requestDto 판매 요청 생성 DTO
   * @return 생성된 판매 요청 정보 DTO
   */
  @Transactional
  public SaleRequestResponseDTO createSaleRequest(Long buyerId, SaleRequestCreateRequestDTO requestDto) {
    User buyer = userRepository.findById(buyerId)
        .orElseThrow(() -> new EntityNotFoundException("Buyer user not found with ID: " + buyerId));
    Post post = postRepository.findByIdAndIsDeletedFalse(requestDto.getPostId())
        .orElseThrow(() -> new EntityNotFoundException("Post not found or deleted with ID: " + requestDto.getPostId()));

    // 게시글의 원본 작성자가 자기 자신에게 구매 요청을 할 수 없음
    if (post.getOriginalAuthor().getId().equals(buyerId)) {
      throw new IllegalArgumentException("You cannot send a purchase request to your own post.");
    }
    // 게시글이 이미 판매 완료된 경우
    if (post.isSold()) {
      throw new IllegalStateException("This post has already been sold.");
    }
    // 이미 PENDING 상태의 판매 요청이 있는지 확인 (중복 요청 방지)
    Optional<SaleRequest> existingPendingRequest = saleRequestRepository.findByPostAndBuyerAndStatus(post, buyer, SaleRequestStatus.PENDING);
    if (existingPendingRequest.isPresent()) {
      throw new IllegalStateException("You already have a pending purchase request for this post.");
    }

    // 에스크로 시스템에 결제 대금을 임시로 묶는다고 가정 (실제 연동 시 이곳에 PG사 API 호출)
    // 여기서는 임시 external_transaction_id 생성
    String escrowTransactionId = "ESCROW_" + UUID.randomUUID().toString();
    // TODO: 실제 PG사 에스크로 API 호출 및 금액 묶는 로직 구현 (가상 결제 또는 테스트 결제 연동)

    SaleRequest saleRequest = SaleRequest.builder()
        .post(post)
        .buyer(buyer)
        .proposedPrice(requestDto.getProposedPrice())
        .escrowTransactionId(escrowTransactionId)
        .build();

    SaleRequest savedSaleRequest = saleRequestRepository.save(saleRequest);

    // --- 알림 생성 로직 추가 ---
    User seller = post.getOriginalAuthor(); // 게시글 원본 작성자 = 판매자
    String message = String.format("'%s'님이 회원님의 게시글 '%s'에 판매 요청을 보냈습니다. (제시 가격: %d원)",
        buyer.getNickname(), post.getTitle(), requestDto.getProposedPrice());
    String redirectPath = String.format("/sale-requests/%d", savedSaleRequest.getId()); // 판매 요청 상세 페이지 경로
    notificationService.createNotification(seller, NotificationType.SALE_REQUEST, message,
        "SaleRequest", savedSaleRequest.getId(), redirectPath);
    log.info("SALE_REQUEST notification sent to seller (User ID: {}) for SaleRequest ID: {}", seller.getId(), savedSaleRequest.getId());
    // --- 알림 생성 로직 끝 ---

    return new SaleRequestResponseDTO(savedSaleRequest);
  }

  /**
   * 원본 작성자(판매자)에게 들어온 모든 판매 요청을 조회합니다.
   * @param sellerId 판매자 사용자 ID
   * @return 판매 요청 목록 DTO
   */
  public List<SaleRequestResponseDTO> getSaleRequestsForSeller(Long sellerId) {
    User seller = userRepository.findById(sellerId)
        .orElseThrow(() -> new EntityNotFoundException("Seller user not found with ID: " + sellerId));

    return saleRequestRepository.findByPost_OriginalAuthorOrderByCreatedAtDesc(seller).stream()
        .map(SaleRequestResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 구매자가 자신이 보낸 모든 판매 요청을 조회합니다.
   * @param buyerId 구매자 사용자 ID
   * @return 판매 요청 목록 DTO
   */
  public List<SaleRequestResponseDTO> getSaleRequestsForBuyer(Long buyerId) {
    User buyer = userRepository.findById(buyerId)
        .orElseThrow(() -> new EntityNotFoundException("Buyer user not found with ID: " + buyerId));

    return saleRequestRepository.findByBuyerOrderByCreatedAtDesc(buyer).stream()
        .map(SaleRequestResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 판매자가 판매 요청을 수락합니다.
   * 요청 수락 시 구매자에게 알림을 생성합니다.
   * @param requestId 수락할 판매 요청 ID
   * @param sellerId 판매자 사용자 ID (권한 검증용)
   * @return 수락된 판매 요청 정보 DTO
   */
  @Transactional
  public SaleRequestResponseDTO acceptSaleRequest(Long requestId, Long sellerId) {
    SaleRequest saleRequest = saleRequestRepository.findById(requestId)
        .orElseThrow(() -> new EntityNotFoundException("Sale request not found with ID: " + requestId));

    // 요청을 수락하는 사용자가 해당 게시글의 원본 작성자인지 확인
    if (!saleRequest.getPost().getOriginalAuthor().getId().equals(sellerId)) {
      throw new IllegalArgumentException("You do not have permission to accept this sale request.");
    }
    // 요청 상태가 PENDING인지 확인
    if (saleRequest.getStatus() != SaleRequestStatus.PENDING) {
      throw new IllegalStateException("This sale request is not in a pending state.");
    }
    // 게시글이 이미 판매 완료된 경우
    if (saleRequest.getPost().isSold()) {
      throw new IllegalStateException("The post associated with this request has already been sold.");
    }

    // 판매 요청 상태를 ACCEPTED로 변경
    saleRequest.accept();

    // 게시글의 소유권 변경 및 판매 상태 업데이트
    Post post = saleRequest.getPost();
    User buyer = saleRequest.getBuyer();
    Integer agreedPrice = saleRequest.getProposedPrice();

    post.markAsSold(buyer, agreedPrice); // Post 엔티티의 markAsSold 메서드 호출

    // 에스크로 시스템에서 판매자에게 대금 지급 (실제 PG사 API 호출)
    String externalTransactionId = saleRequest.getEscrowTransactionId();
    // TODO: 실제 PG사 API를 통해 에스크로 대금 지급 로직 구현

    // 거래 기록 생성
    Transaction transaction = Transaction.builder()
        .saleRequest(saleRequest)
        .post(post)
        .seller(post.getOriginalAuthor())
        .buyer(buyer)
        .amount(agreedPrice)
        .externalTransactionId(externalTransactionId)
        .build();
    transaction.complete(); // 거래 완료 상태로 설정

    transactionRepository.save(transaction);
    postRepository.save(post);

    // --- 알림 생성 로직 추가 ---
    // 구매자에게 판매 요청 수락 알림
    String messageToBuyer = String.format("회원님의 게시글 '%s' 구매 요청이 판매자에 의해 수락되었습니다!", post.getTitle());
    String redirectPathToBuyer = String.format("/sale-requests/%d", saleRequest.getId());
    notificationService.createNotification(buyer, NotificationType.SALE_ACCEPTED, messageToBuyer,
        "SaleRequest", saleRequest.getId(), redirectPathToBuyer);
    log.info("SALE_ACCEPTED notification sent to buyer (User ID: {}) for SaleRequest ID: {}", buyer.getId(), saleRequest.getId());

    // (선택 사항) 판매자 본인에게 게시글 판매 완료 알림 (선택 사항, 직접 수행했으니 굳이 알림은 필요 없을 수 있음)
    // String messageToSeller = String.format("회원님의 게시글 '%s'가 '%s'님에게 판매 완료되었습니다.", post.getTitle(), buyer.getNickname());
    // String redirectPathToSeller = String.format("/posts/%d", post.getId());
    // notificationService.createNotification(seller, NotificationType.POST_PURCHASED, messageToSeller,
    //     "Post", post.getId(), redirectPathToSeller);
    // log.info("POST_PURCHASED notification sent to seller (User ID: {}) for Post ID: {}", seller.getId(), post.getId());
    // --- 알림 생성 로직 끝 ---

    return new SaleRequestResponseDTO(saleRequest);
  }

  /**
   * 판매자가 판매 요청을 거절합니다.
   * 요청 거절 시 구매자에게 알림을 생성합니다.
   * @param requestId 거절할 판매 요청 ID
   * @param sellerId 판매자 사용자 ID (권한 검증용)
   * @return 거절된 판매 요청 정보 DTO
   */
  @Transactional
  public SaleRequestResponseDTO rejectSaleRequest(Long requestId, Long sellerId) {
    SaleRequest saleRequest = saleRequestRepository.findById(requestId)
        .orElseThrow(() -> new EntityNotFoundException("Sale request not found with ID: " + requestId));

    if (!saleRequest.getPost().getOriginalAuthor().getId().equals(sellerId)) {
      throw new IllegalArgumentException("You do not have permission to reject this sale request.");
    }
    if (saleRequest.getStatus() != SaleRequestStatus.PENDING) {
      throw new IllegalStateException("This sale request is not in a pending state.");
    }

    saleRequest.reject(); // 판매 요청 상태를 REJECTED로 변경

    // 에스크로 시스템에 묶여있던 대금을 구매자에게 환불 (실제 PG사 API 호출)
    // TODO: 실제 PG사 API를 통해 에스크로 환불 로직 구현

    // 거래 기록 생성 (옵션: 실패한 거래도 기록할 경우)
    Transaction transaction = Transaction.builder()
        .saleRequest(saleRequest)
        .post(saleRequest.getPost())
        .seller(saleRequest.getPost().getOriginalAuthor())
        .buyer(saleRequest.getBuyer())
        .amount(saleRequest.getProposedPrice())
        .externalTransactionId(saleRequest.getEscrowTransactionId())
        .build();
    transaction.fail(); // 거래 실패 상태로 설정
    transactionRepository.save(transaction);

    // --- 알림 생성 로직 추가 ---
    // 구매자에게 판매 요청 거절 알림
    User buyer = saleRequest.getBuyer();
    String message = String.format("회원님의 게시글 '%s' 구매 요청이 판매자에 의해 거절되었습니다.", saleRequest.getPost().getTitle());
    String redirectPath = String.format("/sale-requests/%d", saleRequest.getId());
    notificationService.createNotification(buyer, NotificationType.SALE_REJECTED, message,
        "SaleRequest", saleRequest.getId(), redirectPath);
    log.info("SALE_REJECTED notification sent to buyer (User ID: {}) for SaleRequest ID: {}", buyer.getId(), saleRequest.getId());
    // --- 알림 생성 로직 끝 ---

    return new SaleRequestResponseDTO(saleRequest);
  }

  /**
   * 구매자가 자신이 보낸 PENDING 상태의 판매 요청을 취소합니다.
   * 요청 취소 시 판매자에게 알림을 생성합니다.
   * @param requestId 취소할 판매 요청 ID
   * @param buyerId 구매자 사용자 ID (권한 검증용)
   * @return 취소된 판매 요청 정보 DTO
   */
  @Transactional
  public SaleRequestResponseDTO cancelSaleRequest(Long requestId, Long buyerId) {
    SaleRequest saleRequest = saleRequestRepository.findById(requestId)
        .orElseThrow(() -> new EntityNotFoundException("Sale request not found with ID: " + requestId));

    if (!saleRequest.getBuyer().getId().equals(buyerId)) {
      throw new IllegalArgumentException("You do not have permission to cancel this sale request.");
    }
    if (saleRequest.getStatus() != SaleRequestStatus.PENDING) {
      throw new IllegalStateException("This sale request is not in a pending state and cannot be canceled.");
    }

    saleRequest.cancel(); // 판매 요청 상태를 CANCELED로 변경

    // 에스크로 시스템에 묶여있던 대금을 구매자에게 환불 (실제 PG사 API 호출)
    // TODO: 실제 PG사 API를 통해 에스크로 환불 로직 구현

    // 거래 기록 생성 (옵션: 실패한 거래도 기록할 경우)
    Transaction transaction = Transaction.builder()
        .saleRequest(saleRequest)
        .post(saleRequest.getPost())
        .seller(saleRequest.getPost().getOriginalAuthor())
        .buyer(saleRequest.getBuyer())
        .amount(saleRequest.getProposedPrice())
        .externalTransactionId(saleRequest.getEscrowTransactionId())
        .build();
    transaction.fail(); // 거래 실패 상태로 설정 (취소도 실패의 일종)
    transactionRepository.save(transaction);

    // --- 알림 생성 로직 추가 ---
    // 판매자에게 판매 요청 취소 알림
    User seller = saleRequest.getPost().getOriginalAuthor();
    String message = String.format("'%s'님이 회원님의 게시글 '%s'에 대한 판매 요청을 취소했습니다.",
        saleRequest.getBuyer().getNickname(), saleRequest.getPost().getTitle());
    String redirectPath = String.format("/sale-requests/%d", saleRequest.getId());
    notificationService.createNotification(seller, NotificationType.SALE_REJECTED, message, // 취소도 거절과 유사하게 처리 (원한다면 NotificationType.SALE_CANCELED 추가 가능)
        "SaleRequest", saleRequest.getId(), redirectPath);
    log.info("SALE_REJECTED (CANCELED) notification sent to seller (User ID: {}) for SaleRequest ID: {}", seller.getId(), saleRequest.getId());
    // --- 알림 생성 로직 끝 ---

    return new SaleRequestResponseDTO(saleRequest);
  }

  /**
   * 특정 사용자가 소유한 게시글 목록을 조회합니다.
   * @param userId 사용자 ID (currentOwner 기준)
   * @return 소유한 게시글 목록 DTO
   */
  public List<OwnedPostResponseDTO> getOwnedPosts(Long userId) {
    User owner = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

    // User 엔티티의 ownedPosts 리스트를 활용
    return owner.getOwnedPosts().stream()
        .map(OwnedPostResponseDTO::new)
        .collect(Collectors.toList());
  }
}
