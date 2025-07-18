package com.example.sleeprism.controller;

import com.example.sleeprism.dto.PostCreateRequestDTO;
import com.example.sleeprism.dto.PostResponseDTO;
import com.example.sleeprism.dto.PostUpdateRequestDTO;
import com.example.sleeprism.entity.PostCategory;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.service.BookmarkService;
import com.example.sleeprism.service.FileStorageService;
import com.example.sleeprism.service.PostLikeService;
import com.example.sleeprism.service.PostService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController // RESTful API 컨트롤러
@RequestMapping("/api/posts") // 기본 URL 경로 설정
@RequiredArgsConstructor
@Slf4j
public class PostController {
  private final PostService postService;
  private final FileStorageService fileStorageService;
  private final PostLikeService postLikeService;
  private final BookmarkService bookmarkService;

  // UserDetails에서 userId를 추출하는 헬퍼 메서드
  private Long extractUserIdFromUserDetails(UserDetails userDetails) {
    if (userDetails instanceof User) {
      return ((User) userDetails).getId();
    }
    throw new IllegalStateException("AuthenticationPrincipal is not of type User.");
  }

  // 게시글 생성 (인증된 사용자만 가능하다고 가정)
  @PostMapping
  public ResponseEntity<PostResponseDTO> createPost(
      @Valid @RequestBody PostCreateRequestDTO requestDto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId;
    try {
      userId = extractUserIdFromUserDetails(userDetails);
      log.info("createPost method reached. User ID: {}", userId);
    } catch (IllegalStateException e) {
      log.error("Failed to extract user ID from UserDetails: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
    }

    // requestDto 내용 로깅 (파싱 성공 여부 확인)
    log.info("Received PostCreateRequestDTO: Title='{}', Category='{}', Content length={}",
        requestDto.getTitle(), requestDto.getCategory(), requestDto.getContent() != null ? requestDto.getContent().length() : 0);

    try {
      PostResponseDTO responseDto = postService.createPost(requestDto, userId);
      log.info("PostService.createPost succeeded. New Post ID: {}", responseDto.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(responseDto); // DTO로 변환하여 반환
    } catch (EntityNotFoundException e) {
      log.error("Error creating post: User not found with ID {}. Error: {}", userId, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found (사용자를 찾을 수 없을 때)
    } catch (IllegalArgumentException e) {
      log.error("Error creating post: Invalid argument. Error: {}", e.getMessage());
      return ResponseEntity.badRequest().build(); // 400 Bad Request
    } catch (Exception e) {
      log.error("An unexpected error occurred while creating post: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
    }
  }

  // 특정 게시글 조회
  @GetMapping("/{postId}")
  public ResponseEntity<PostResponseDTO> getPost(@PathVariable Long postId) {
    PostResponseDTO responseDto = postService.getPostById(postId);
    return ResponseEntity.ok(responseDto);
  }

  // 게시글 수정 (작성자만 가능하다고 가정)
  @PutMapping("/{postId}")
  public ResponseEntity<PostResponseDTO> updatePost(
      @PathVariable Long postId,
      @Valid @RequestBody PostUpdateRequestDTO requestDto,
      @AuthenticationPrincipal UserDetails userDetails // Spring Security를 통해 현재 로그인한 사용자 정보 주입
  ) {
    Long userId;
    if (userDetails instanceof User) {
      userId = ((User) userDetails).getId();
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    PostResponseDTO responseDTO = postService.updatePost(postId, requestDto, userId);
    return ResponseEntity.ok(responseDTO);
  }

  // 게시글 삭제 (작성자만 가능하다고 가정)
  @DeleteMapping("/{postId}")
  public ResponseEntity<Void> deletePost(
      @PathVariable Long postId,
      @AuthenticationPrincipal UserDetails userDetails // Spring Security를 통해 현재 로그인한 사용자 정보 주입
  ) {
    Long userId;
    if (userDetails instanceof User) {
      userId = ((User) userDetails).getId();
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    postService.deletePost(postId, userId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  // 모든 게시글 또는 카테고리별 게시글 조회 통합 엔드포인트
  @GetMapping
  public ResponseEntity<List<PostResponseDTO>> getPosts(
      @RequestParam(required = false) List<PostCategory> category) { // List<PostCategory>로 변경
    List<PostResponseDTO> posts = postService.getPosts(category); // 서비스 호출도 List로 변경
    return ResponseEntity.ok(posts);
  }

  // 게시글 좋아요/좋아요 취소 엔드포인트
  @PostMapping("/{postId}/like")
  public ResponseEntity<Map<String, Boolean>> toggleLike(
      @PathVariable Long postId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId;
    try {
      userId = extractUserIdFromUserDetails(userDetails);
    } catch (IllegalStateException e) {
      log.error("Failed to extract user ID from UserDetails for like toggle: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
    }

    try {
      boolean liked = postLikeService.togglePostLike(postId, userId);
      return ResponseEntity.ok(Map.of("liked", liked)); // 좋아요 상태 반환
    } catch (EntityNotFoundException e) {
      log.error("게시글 또는 사용자를 찾을 수 없어 좋아요 토글 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("좋아요 토글 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // 특정 게시글의 좋아요 상태 확인 엔드포인트
  @GetMapping("/{postId}/like/status")
  public ResponseEntity<Map<String, Boolean>> getLikeStatus(
      @PathVariable Long postId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId;
    try {
      userId = extractUserIdFromUserDetails(userDetails);
    } catch (IllegalStateException e) {
      // 로그인하지 않은 사용자는 좋아요 상태를 알 수 없으므로 false 반환 또는 401
      // 여기서는 401로 처리하여 로그인 필요함을 알림
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      boolean isLiked = postLikeService.isPostLikedByUser(postId, userId);
      return ResponseEntity.ok(Map.of("isLiked", isLiked));
    } catch (EntityNotFoundException e) {
      log.error("게시글 또는 사용자를 찾을 수 없어 좋아요 상태 확인 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("좋아요 상태 확인 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // 게시글 북마크/북마크 취소 엔드포인트 추가
  @PostMapping("/{postId}/bookmark")
  public ResponseEntity<Map<String, Boolean>> toggleBookmark(
      @PathVariable Long postId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId;
    try {
      userId = extractUserIdFromUserDetails(userDetails);
    } catch (IllegalStateException e) {
      log.error("Failed to extract user ID from UserDetails for bookmark toggle: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
    }

    try {
      boolean bookmarked = bookmarkService.togglePostBookmark(postId, userId);
      return ResponseEntity.ok(Map.of("bookmarked", bookmarked)); // 북마크 상태 반환
    } catch (EntityNotFoundException e) {
      log.error("게시글 또는 사용자를 찾을 수 없어 북마크 토글 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("북마크 토글 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // 특정 게시글의 북마크 상태 확인 엔드포인트 추가
  @GetMapping("/{postId}/bookmark/status")
  public ResponseEntity<Map<String, Boolean>> getBookmarkStatus(
      @PathVariable Long postId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long userId;
    try {
      userId = extractUserIdFromUserDetails(userDetails);
    } catch (IllegalStateException e) {
      // 로그인하지 않은 사용자는 북마크 상태를 알 수 없으므로 false 반환 또는 401
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      boolean isBookmarked = bookmarkService.isPostBookmarkedByUser(postId, userId);
      return ResponseEntity.ok(Map.of("isBookmarked", isBookmarked));
    } catch (EntityNotFoundException e) {
      log.error("게시글 또는 사용자를 찾을 수 없어 북마크 상태 확인 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("북마크 상태 확인 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // 인기 게시글 조회 엔드포인트 (기간 필터링 추가)
  @GetMapping("/popular")
  public ResponseEntity<List<PostResponseDTO>> getPopularPosts(
      @RequestParam(required = false, defaultValue = "all_time") String period) { // 기간 파라미터 추가
    try {
      List<PostResponseDTO> popularPosts = postService.getPopularPosts(period); // 서비스 호출에 period 전달
      return ResponseEntity.ok(popularPosts);
    } catch (IllegalArgumentException e) {
      log.error("유효하지 않은 기간 파라미터: {}", e.getMessage());
      return ResponseEntity.badRequest().build(); // 400 Bad Request
    } catch (Exception e) {
      log.error("인기 게시글 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // ==== Quill.js 이미지 업로드 및 파일 제공 엔드포인트 추가 ====

  /**
   * Quill.js 에디터에서 이미지를 업로드하기 위한 엔드포인트입니다.
   * 이미지를 서버에 저장하고, 저장된 이미지의 URL을 반환합니다.
   *
   * @param file 업로드할 이미지 파일 (Quill.js는 'image'라는 필드명으로 전송)
   * @return 업로드된 이미지의 URL을 포함하는 JSON 응답
   */
  @PostMapping("/upload-image")
  public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
    try {
      // "post-images" 디렉토리에 이미지 파일을 저장
      String relativePath = fileStorageService.uploadFile(file, "post-images");

      // 수정 제안: relativePath가 이미 '/files/'로 시작하는지 확인하여 중복 방지
      String imageUrl;
      if (relativePath.startsWith("/files/")) {
        imageUrl = "/api/posts" + relativePath; // 이미 /files/로 시작하면 /api/posts만 붙임
      } else {
        imageUrl = "/api/posts/files/" + relativePath; // 아니면 기존처럼 /api/posts/files/를 붙임
      }

      // Quill.js가 기대하는 형식으로 URL 반환 (JSON 객체 {"url": "..."})
      return ResponseEntity.ok(Map.of("url", imageUrl));
    } catch (IOException e) {
      // 파일 업로드 실패 시 로깅 및 에러 응답
      System.err.println("Failed to upload image: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "이미지 업로드에 실패했습니다."));
    }
  }

  /**
   * 서버에 저장된 파일을 웹 브라우저에 제공하기 위한 엔드포인트입니다.
   * 예: http://localhost:8080/api/posts/files/post-images/uuid.jpg
   *
   * @param directory 파일이 저장된 하위 디렉토리 (예: "post-images")
   * @param filename  파일의 고유 이름 (예: "uuid.jpg")
   * @return 파일 Resource 및 HTTP 헤더
   */
  @GetMapping("/files/{directory}/{filename:.+}") // {filename:.+}는 파일명에 점(.)이 포함되어도 모두 매칭
  public ResponseEntity<Resource> serveFile(@PathVariable String directory, @PathVariable String filename) {
    try {
      String relativePath = directory + "/" + filename;
      Resource file = fileStorageService.loadAsResource(relativePath);

      // 파일의 MIME 타입 결정 (Optional: 필요시 MediaTypeFactory 등을 사용하여 동적으로 결정)
      String contentType = "application/octet-stream"; // 기본값
      try {
        contentType = file.getURL().openConnection().getContentType();
      } catch (IOException ex) {
        // Fallback to default if content type cannot be determined
      }

      return ResponseEntity.ok()
          .contentType(org.springframework.http.MediaType.parseMediaType(contentType)) // MIME 타입 설정
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"") // 브라우저에서 인라인으로 표시
          .body(file);
    } catch (IOException e) {
      System.err.println("Error serving file: " + directory + "/" + filename + " - " + e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  // TODO: 검색, 인기 게시글 등에 대한 엔드포인트 추가
}
