package com.example.sleeprism.service;

import org.jsoup.Jsoup; // Jsoup 임포트
import org.jsoup.nodes.Document; // Jsoup Document 임포트
import org.jsoup.select.Elements; // Jsoup Elements 임포트
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j; // Slf4j 임포트

/**
 * 로컬 파일 시스템에 파일을 저장하고 관리하는 서비스입니다.
 * FileStorageService 인터페이스를 구현합니다.
 */
@Service
@Slf4j // 로깅 활성화
public class LocalStorageService implements FileStorageService {

  @Value("${file.upload-dir}")
  private String uploadDir;

  /**
   * 파일을 로컬 스토리지에 업로드하고 저장된 파일의 상대 경로를 반환합니다.
   * (예: "profile-images/uuid.jpg")
   *
   * @param file 업로드할 MultipartFile 객체
   * @param directory 파일을 저장할 하위 디렉토리 (예: "profile-images", "post-attachments")
   * @return 저장된 파일의 상대 경로 (예: directory/storedFileName)
   * @throws IOException 파일 처리 중 오류 발생 시
   */
  @Override
  public String uploadFile(MultipartFile file, String directory) throws IOException {
    Path uploadPath = Paths.get(uploadDir, directory).toAbsolutePath().normalize();
    Files.createDirectories(uploadPath);

    String originalFileName = file.getOriginalFilename();
    String fileExtension = "";
    if (originalFileName != null && originalFileName.contains(".")) {
      fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    String storedFileName = UUID.randomUUID().toString() + fileExtension;
    Path targetLocation = uploadPath.resolve(storedFileName);

    Files.copy(file.getInputStream(), targetLocation);

    // 저장된 파일의 상대 경로를 반환할 때 항상 웹 친화적인 슬래시(/)를 사용하도록 합니다.
    return Paths.get(directory, storedFileName).toString().replace("\\", "/");
  }

  /**
   * 지정된 URL에 해당하는 로컬 파일을 삭제합니다.
   *
   * @param fileUrl 삭제할 파일의 URL (예: /api/posts/files/post-images/uuid.jpg)
   * 또는 /files/post-images/uuid.jpg (deleteImagesFromHtmlContent에서 변환될 경로)
   */
  @Override
  public void deleteFile(String fileUrl) {
    if (fileUrl == null || fileUrl.trim().isEmpty()) {
      log.warn("Attempted to delete a file with a null or empty URL.");
      return;
    }

    // 파일 URL에서 실제 파일 시스템 경로를 유추합니다.
    String relativePathInStorage = null;
    final String apiPrefix = "/api/posts/files/";
    final String filesPrefix = "/files/";

    // URL이 백엔드 API 경로로 시작하는 경우 (예: /api/posts/files/...)
    if (fileUrl.startsWith(apiPrefix)) {
      relativePathInStorage = fileUrl.substring(apiPrefix.length());
    }
    // URL이 '/files/'로 시작하는 경우 (deleteImagesFromHtmlContent에서 변환된 경우)
    else if (fileUrl.startsWith(filesPrefix)) {
      relativePathInStorage = fileUrl.substring(filesPrefix.length());
    } else {
      log.warn("File URL format not recognized for deletion: {}", fileUrl);
      return;
    }

    // 저장 디렉토리와 상대 경로를 결합하여 삭제할 파일의 실제 경로를 구성합니다.
    Path filePathToDelete = Paths.get(uploadDir, relativePathInStorage).normalize();

    try {
      if (Files.exists(filePathToDelete)) {
        Files.delete(filePathToDelete);
        log.info("File deleted from local storage: {}", filePathToDelete);
      } else {
        log.warn("File not found for deletion: {}", filePathToDelete);
      }
    } catch (IOException e) {
      log.error("Failed to delete file {}: {}", filePathToDelete, e.getMessage(), e);
    }
  }

  /**
   * HTML 콘텐츠에서 모든 <img> 태그의 src 속성을 파싱하여 해당 로컬 파일을 삭제합니다.
   *
   * @param htmlContent 게시글의 HTML 내용
   */
  @Override
  public void deleteImagesFromHtmlContent(String htmlContent) {
    if (htmlContent == null || htmlContent.trim().isEmpty()) {
      return;
    }

    Document doc = Jsoup.parse(htmlContent);
    Elements images = doc.select("img");

    final String backendBaseUrlWithContext = "http://localhost:8080/sleeprism"; // 백엔드 컨텍스트 경로 포함
    final String backendBaseUrlNoContext = "http://localhost:8080"; // 컨텍스트 경로 없는 백엔드 URL
    final String apiPathSegment = "/api/posts/files/"; // 파일 API 경로 세그먼트

    for (org.jsoup.nodes.Element img : images) {
      String src = img.attr("src");
      if (src != null && !src.trim().isEmpty()) {
        String fileToDeletePath = null;

        // 1. http://localhost:8080/sleeprism/api/posts/files/... (프론트엔드 에디터에서 사용하는 절대 경로)
        if (src.startsWith(backendBaseUrlWithContext + apiPathSegment)) {
          fileToDeletePath = src.substring((backendBaseUrlWithContext + apiPathSegment).length());
          log.debug("Extracted file path (with context): {}", fileToDeletePath);
        }
        // 2. http://localhost:8080/api/posts/files/... (HtmlSanitizer 처리 후 DB에 저장된 절대 경로)
        else if (src.startsWith(backendBaseUrlNoContext + apiPathSegment)) {
          fileToDeletePath = src.substring((backendBaseUrlNoContext + apiPathSegment).length());
          log.debug("Extracted file path (no context): {}", fileToDeletePath);
        }
        // 3. /api/posts/files/... (백엔드 uploadFile 반환 경로, 또는 과거 Sanitizer에서 보존된 상대경로)
        else if (src.startsWith(apiPathSegment)) {
          fileToDeletePath = src.substring(apiPathSegment.length());
          log.debug("Extracted file path (relative): {}", fileToDeletePath);
        }

        if (fileToDeletePath != null) {
          // deleteFile 메서드는 "post-images/uuid.jpg" 형식의 상대 경로를 기대하므로
          // 직접 경로를 넘겨주거나, deleteFile 내부에서 한번 더 변환 로직을 갖도록 할 수 있습니다.
          // 현재 deleteFile은 '/files/' 접두사를 처리하므로, 그에 맞게 경로를 조정합니다.
          // 여기서 fileToDeletePath는 이미 "post-images/uuid.jpg" 형태가 됩니다.
          // deleteFile은 이 형태의 경로도 처리할 수 있도록 수정되어야 합니다.
          // (혹은 여기서 /files/post-images/uuid.jpg 형태로 만들어서 넘겨줍니다)
          this.deleteFile(apiPathSegment.replace("/api/posts", "") + fileToDeletePath); // ex: /files/post-images/uuid.jpg
        } else {
          log.warn("Image src not recognized for deletion: {}", src);
        }
      }
    }
  }


  /**
   * 지정된 상대 경로의 파일을 Spring Resource 객체로 로드합니다.
   *
   * @param relativePath 로드할 파일의 상대 경로 (예: profile-images/uuid.jpg)
   * @return 로드된 Resource 객체
   * @throws IOException 파일 로드 중 오류 발생 시 (파일을 찾을 수 없는 경우 포함)
   */
  @Override
  public Resource loadAsResource(String relativePath) throws IOException {
    try {
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
