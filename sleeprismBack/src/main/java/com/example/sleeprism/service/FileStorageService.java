package com.example.sleeprism.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 파일 저장 서비스를 위한 인터페이스입니다.
 * 로컬 스토리지, S3 등 다양한 파일 저장 방식을 추상화합니다.
 */
public interface FileStorageService {

  /**
   * 파일을 업로드하고 저장된 파일의 상대 경로를 반환합니다.
   * (예: "profile-images/uuid.jpg")
   *
   * @param file 업로드할 MultipartFile 객체
   * @param directory 파일을 저장할 하위 디렉토리 (예: "profile-images", "post-attachments")
   * @return 저장된 파일의 상대 경로 (예: directory/storedFileName)
   * @throws IOException 파일 처리 중 오류 발생 시
   */
  String uploadFile(MultipartFile file, String directory) throws IOException;

  /**
   * 지정된 파일 URL에 해당하는 파일을 삭제합니다.
   *
   * @param fileUrl 삭제할 파일의 URL (예: /files/profile-images/uuid.jpg)
   */
  void deleteFile(String fileUrl);

  /**
   * HTML 내용에서 모든 <img> 태그의 src 속성을 파싱하여 해당 로컬 파일을 삭제합니다.
   * 이 메서드는 특정 URL 패턴에 따라 파일을 삭제합니다.
   *
   * @param htmlContent 게시글의 HTML 내용
   */
  void deleteImagesFromHtmlContent(String htmlContent);

  /**
   * 지정된 상대 경로의 파일을 Spring Resource 객체로 로드합니다.
   *
   * @param relativePath 로드할 파일의 상대 경로 (예: profile-images/uuid.jpg)
   * @return 로드된 Resource 객체
   * @throws IOException 파일 로드 중 오류 발생 시 (파일을 찾을 수 없는 경우 포함)
   */
  Resource loadAsResource(String relativePath) throws IOException;

  /**
   * 댓글 첨부 파일을 업로드하고 저장된 파일의 상대 경로를 반환합니다.
   * (예: "comments/uuid.jpg")
   *
   * @param attachmentFile 업로드할 MultipartFile 객체
   * @return 저장된 파일의 상대 경로 (예: "comments/storedFileName")
   * @throws IOException 파일 처리 중 오류 발생 시
   */
  String uploadCommentAttachment(MultipartFile attachmentFile) throws IOException; // <-- throws IOException 추가
}
