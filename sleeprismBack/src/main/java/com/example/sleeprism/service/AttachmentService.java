package com.example.sleeprism.service;

import com.example.sleeprism.dto.AttachmentResponseDTO;
import com.example.sleeprism.entity.Attachment;
import com.example.sleeprism.entity.AttachmentType;
import com.example.sleeprism.entity.Post;
import com.example.sleeprism.repository.AttachmentRepository;
import com.example.sleeprism.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션, 쓰기 메서드에서 @Transactional 재선언
public class AttachmentService {
  private final AttachmentRepository attachmentRepository;
  private final PostRepository postRepository; // Post 엔티티와 연결하기 위해 필요
  private final FileStorageService fileStorageService; // 실제 파일 저장/삭제를 처리하는 서비스

  /**
   * 게시글에 첨부 파일을 업로드하고 DB에 기록합니다.
   * @param postId 파일을 첨부할 게시글의 ID
   * @param file 업로드할 MultipartFile 객체
   * @return 저장된 첨부 파일 정보 DTO
   * @throws IOException 파일 처리 중 발생할 수 있는 예외
   */
  @Transactional // 쓰기 작업
  public AttachmentResponseDTO uploadAttachment(Long postId, MultipartFile file) throws IOException {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));

    if (file.isEmpty()) {
      throw new IllegalArgumentException("첨부할 파일이 비어있습니다.");
    }

    String originalFileName = file.getOriginalFilename();
    String directoryPath = "post-attachments/" + postId; // 게시글 ID별 경로

    // FileStorageService를 통해 실제 파일을 저장하고, 저장된 파일의 상대 경로를 반환받습니다.
    // LocalStorageService는 이 메서드 내부에서 UUID를 사용하여 고유 파일명을 생성합니다.
    String relativeFilePath = fileStorageService.uploadFile(file, directoryPath);

    // 데이터베이스에 저장할 fileUrl (클라이언트가 접근할 수 있는 URL) 생성
    // 여기서는 예시로 "/files/" 접두사를 붙여 URL을 구성합니다.
    // 실제로는 환경 설정 또는 FileStorageService가 이 완전한 URL을 반환하도록 할 수 있습니다.
    String fileUrl = "/files/" + relativeFilePath;

    // 파일 타입 결정 (간단한 예시, 실제로는 더 정교한 로직 필요)
    String contentType = file.getContentType();
    AttachmentType attachmentType;
    if (contentType != null && contentType.startsWith("image")) {
      attachmentType = AttachmentType.IMAGE;
    } else if (contentType != null && contentType.startsWith("video")) {
      attachmentType = AttachmentType.VIDEO;
    } else {
      attachmentType = AttachmentType.DOCUMENT;
    }

    // DB에 저장할 storedFileName은 relativeFilePath에서 파일 이름 부분만 추출하여 사용하거나
    // 별도로 관리하지 않아도 됩니다 (fileUrl, originalFileName, fileType 등으로 충분).
    // 만약 storedFileName을 DB에 저장해야 한다면, relativeFilePath에서 마지막 경로 부분을 파싱하여 사용합니다.
    String storedFileName = Paths.get(relativeFilePath).getFileName().toString();


    Attachment attachment = Attachment.builder()
        .post(post)
        .originalFileName(originalFileName)
        .storedFileName(storedFileName) // LocalStorageService가 생성한 고유 파일명 저장
        .fileUrl(fileUrl)             // 클라이언트 접근용 URL
        .fileType(contentType)
        .fileSize(file.getSize())
        .attachmentType(attachmentType)
        .build();

    // 게시글 엔티티의 첨부 파일 리스트에 추가 (양방향 연관관계 편의 메서드)
    post.addAttachment(attachment);
    Attachment savedAttachment = attachmentRepository.save(attachment);

    return new AttachmentResponseDTO(savedAttachment);
  }

  /**
   * 특정 게시글에 속한 모든 첨부 파일을 조회합니다.
   * @param postId 게시글 ID
   * @return 해당 게시글의 첨부 파일 목록 DTO
   */
  public List<AttachmentResponseDTO> getAttachmentsByPostId(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + postId));
    return attachmentRepository.findByPost(post).stream()
        .map(AttachmentResponseDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * 특정 첨부 파일을 ID로 조회합니다.
   * @param attachmentId 첨부 파일 ID
   * @return 첨부 파일 정보 DTO
   */
  public AttachmentResponseDTO getAttachmentById(Long attachmentId) {
    Attachment attachment = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new EntityNotFoundException("Attachment not found with ID: " + attachmentId));
    return new AttachmentResponseDTO(attachment);
  }

  /**
   * 특정 첨부 파일을 삭제합니다. (실제 파일도 삭제)
   * @param attachmentId 삭제할 첨부 파일의 ID
   * @param userId 삭제를 요청한 사용자 ID (권한 검증용)
   */
  @Transactional
  public void deleteAttachment(Long attachmentId, Long userId) {
    Attachment attachment = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new EntityNotFoundException("Attachment not found with ID: " + attachmentId));

    // 해당 첨부 파일이 속한 게시글의 원본 작성자만 삭제 가능하도록 검증
    if (!attachment.getPost().getOriginalAuthor().getId().equals(userId)) {
      throw new IllegalArgumentException("You do not have permission to delete this attachment.");
    }
    // 게시글이 이미 판매된 경우 첨부 파일 삭제 불가 (콘텐츠 무결성)
    if (attachment.getPost().isSold()) {
      throw new IllegalStateException("Sold posts' attachments cannot be deleted.");
    }

    // 실제 파일 스토리지에서 파일 삭제
    fileStorageService.deleteFile(attachment.getFileUrl());

    // DB에서 Attachment 엔티티 삭제
    attachmentRepository.delete(attachment);
  }
}
