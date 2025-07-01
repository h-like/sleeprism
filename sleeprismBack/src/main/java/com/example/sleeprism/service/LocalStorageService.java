package com.example.sleeprism.service;

import org.jsoup.Jsoup; // Jsoup 임포트
import org.jsoup.nodes.Document; // Jsoup Document 임포트
import org.jsoup.select.Elements; // Jsoup Elements 임포트
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption; // StandardCopyOption 임포트
import java.util.UUID;
import lombok.extern.slf4j.Slf4j; // Slf4j 임포트

// application.properties에서 설정한 파일 업로드 루트 디렉토리

@Service
@Slf4j
public class LocalStorageService implements FileStorageService {

  // application.properties에서 설정한 파일 업로드 루트 디렉토리
  @Value("${file.upload-dir}")
  private String uploadDir;

  /**
   * 파일을 로컬 스토리지에 업로드하고 저장된 파일의 상대 경로를 반환합니다.
   * (기존 메서드 유지: 게시글 본문 이미지, 프로필 이미지 등에 사용)
   *
   * @param file 업로드할 MultipartFile 객체
   * @param directory 파일을 저장할 하위 디렉토리 (예: "profile-images", "post-attachments")
   * @return 저장된 파일의 상대 경로 (예: /files/directory/storedFileName)
   * @throws IOException 파일 처리 중 오류 발생 시
   */
  @Override
  public String uploadFile(MultipartFile file, String directory) throws IOException {
    if (file.isEmpty()) {
      log.warn("LocalStorageService: Attempted to upload an empty file.");
      return null;
    }

    Path uploadPath = Paths.get(uploadDir, directory).toAbsolutePath().normalize();
    Files.createDirectories(uploadPath);

    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
    String fileExtension = "";
    if (originalFileName.contains(".")) {
      fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    String storedFileName = UUID.randomUUID().toString() + fileExtension;
    Path targetLocation = uploadPath.resolve(storedFileName);

    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

    log.info("LocalStorageService: File '{}' saved to '{}'.", originalFileName, targetLocation);
    // DB에 저장될 경로를 `/files/directory/storedFileName` 형태로 반환합니다.
    return "/files/" + Paths.get(directory, storedFileName).toString().replace("\\", "/");
  }


  /**
   * **[새로 추가/수정]**
   * 댓글 첨부 파일을 'comment' 하위 디렉토리에 업로드하고,
   * 'comments/파일명.png' 형태의 상대 경로를 반환합니다.
   *
   * @param file 업로드할 MultipartFile 객체
   * @return 저장된 파일의 상대 경로 (예: "comments/uuid.jpg")
   * @throws IOException 파일 처리 중 오류 발생 시
   */
  public String uploadCommentAttachment(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      log.warn("LocalStorageService: Attempted to upload a null or empty comment attachment.");
      return null;
    }

    // 1. 저장할 로컬 디렉토리 경로 정의 (C:uploads\comment)
    String commentDir = "comment";
    Path uploadPath = Paths.get(uploadDir, commentDir).toAbsolutePath().normalize();

    // 디렉토리가 없으면 생성합니다.
    Files.createDirectories(uploadPath);

    // 2. 파일명 생성 (충돌 방지를 위해 UUID와 원본 파일명 조합)
    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
    String fileExtension = "";
    if (originalFileName != null && originalFileName.contains(".")) {
      fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    String storedFileName = UUID.randomUUID().toString() + fileExtension;
    Path targetLocation = uploadPath.resolve(storedFileName);

    // 3. 파일을 지정된 경로에 복사하여 저장
    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

    log.info("LocalStorageService: Comment attachment '{}' saved to '{}'.", originalFileName, targetLocation);

    // 4. 데이터베이스에 저장할 URL 경로를 원하는 형식("comments/uuid.jpg")으로 반환
    // 이 경로는 WebConfig의 ResourceHandler와 매핑됩니다.
    return Paths.get(commentDir, storedFileName).toString().replace("\\", "/");
  }


  /**
   * 지정된 URL에 해당하는 로컬 파일을 삭제합니다.
   *
   * @param fileUrl 삭제할 파일의 URL (예: /files/profile-images/uuid.jpg 또는 comments/uuid.jpg)
   */
  @Override
  public void deleteFile(String fileUrl) {
    if (fileUrl == null || fileUrl.trim().isEmpty()) {
      log.warn("LocalStorageService: Attempted to delete a file with a null or empty URL.");
      return;
    }

    String relativePathInStorage = null;

    // 기존 URL 패턴 (/files/...) 처리 (프로필 이미지, 게시글 이미지)
    if (fileUrl.startsWith("/files/")) {
      relativePathInStorage = fileUrl.substring("/files/".length());
    }
    // **[새로 추가]** 댓글 첨부 파일 URL 패턴 (comments/...) 처리
    else if (fileUrl.startsWith("comments/")) {
      relativePathInStorage = fileUrl; // 'comments/' 자체도 하위 디렉토리이므로 그대로 사용
    }
    else {
      log.warn("LocalStorageService: File URL format not recognized for deletion: {}", fileUrl);
      return;
    }

    Path filePathToDelete = Paths.get(uploadDir, relativePathInStorage).normalize();

    try {
      if (Files.exists(filePathToDelete)) {
        Files.delete(filePathToDelete);
        log.info("LocalStorageService: File deleted from local storage: {}", filePathToDelete);
      } else {
        log.warn("LocalStorageService: File not found for deletion: {}", filePathToDelete);
      }
    } catch (IOException e) {
      log.error("LocalStorageService: Failed to delete file {}: {}", filePathToDelete, e.getMessage(), e);
    }
  }


  /**
   * HTML 콘텐츠에서 모든 <img> 태그의 src 속성을 파싱하여 해당 로컬 파일을 삭제합니다.
   * (기존 메서드 유지)
   */
  @Override
  public void deleteImagesFromHtmlContent(String htmlContent) {
    // 이 메서드는 기존 방식대로 게시글 본문 이미지 처리에만 사용됩니다.
    // 기존 로직과 URL 패턴을 그대로 유지합니다.
    if (htmlContent == null || htmlContent.trim().isEmpty()) {
      return;
    }

    Document doc = Jsoup.parse(htmlContent);
    Elements images = doc.select("img");

    final String apiPathSegment = "/api/posts/files/"; // 게시글 파일 API 경로

    for (org.jsoup.nodes.Element img : images) {
      String src = img.attr("src");
      if (src != null && !src.trim().isEmpty()) {
        String fileToDeletePath = null;

        // src가 /api/posts/files/ 로 시작하는 경우를 처리
        if (src.contains(apiPathSegment)) {
          // /api/posts/files/ 이후의 실제 상대 경로를 추출 (예: post-images/uuid.jpg)
          fileToDeletePath = src.substring(src.indexOf(apiPathSegment) + apiPathSegment.length());
          log.debug("Extracted file path (from HTML content): {}", fileToDeletePath);
        }

        if (fileToDeletePath != null) {
          // deleteFile 메서드는 "/files/..." 형태를 처리하므로, 그에 맞게 경로를 조정합니다.
          // deleteFile("/files/post-images/uuid.jpg") 형태로 호출됩니다.
          this.deleteFile("/files/" + fileToDeletePath);
        } else {
          log.warn("Image src not recognized for deletion from HTML content: {}", src);
        }
      }
    }
  }


  /**
   * 지정된 상대 경로의 파일을 Spring Resource 객체로 로드합니다.
   * (기존 메서드 유지)
   *
   * @param relativePath 로드할 파일의 상대 경로 (예: profile-images/uuid.jpg, comments/uuid.jpg)
   * @return 로드된 Resource 객체
   * @throws IOException 파일 로드 중 오류 발생 시 (파일을 찾을 수 없는 경우 포함)
   */
  @Override
  public Resource loadAsResource(String relativePath) throws IOException {
    try {
      // relativePath는 "profile-images/uuid.jpg" 또는 "post-images/uuid.jpg" 또는 "comments/uuid.jpg" 형태를 예상합니다.
      Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
      Resource resource = new UrlResource(filePath.toUri());

      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new IOException("File not found or not readable: " + relativePath);
      }
    } catch (MalformedURLException ex) {
      throw new IOException("Could not read file: " + relativePath, ex);
    }
  }
}