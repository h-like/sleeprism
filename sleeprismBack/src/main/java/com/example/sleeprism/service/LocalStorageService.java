package com.example.sleeprism.service;

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

/**
 * 로컬 파일 시스템에 파일을 저장하고 관리하는 서비스입니다.
 * FileStorageService 인터페이스를 구현합니다.
 */
@Service
public class LocalStorageService implements FileStorageService {

  // application.properties에서 file.upload-dir 값을 주입받습니다.
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
    // 업로드 경로를 구성하고 정규화합니다.
    Path uploadPath = Paths.get(uploadDir, directory).toAbsolutePath().normalize();
    // 디렉토리가 없으면 생성합니다.
    Files.createDirectories(uploadPath);

    // 원본 파일 이름에서 확장자를 추출합니다.
    String originalFileName = file.getOriginalFilename();
    String fileExtension = "";
    if (originalFileName != null && originalFileName.contains(".")) {
      fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    // UUID를 사용하여 고유한 파일 이름을 생성합니다.
    String storedFileName = UUID.randomUUID().toString() + fileExtension;
    // 파일을 저장할 최종 경로를 결정합니다.
    Path targetLocation = uploadPath.resolve(storedFileName);

    // 파일을 대상 위치로 복사합니다.
    Files.copy(file.getInputStream(), targetLocation);

    // FIX: 저장된 파일의 상대 경로를 반환할 때 항상 웹 친화적인 슬래시(/)를 사용하도록 합니다.
    return Paths.get(directory, storedFileName).toString().replace("\\", "/");
  }

  /**
   * 지정된 URL에 해당하는 로컬 파일을 삭제합니다.
   *
   * @param fileUrl 삭제할 파일의 URL (예: /files/profile-images/uuid.jpg)
   */
  @Override
  public void deleteFile(String fileUrl) {
    // fileUrl에서 실제 파일 시스템 경로를 유추하여 삭제 로직을 구현합니다.
    if (fileUrl != null && fileUrl.startsWith("/files/")) {
      // "/files/" 접두사를 제거하여 상대 경로를 얻습니다.
      String relativePath = fileUrl.substring("/files/".length());
      // 업로드 디렉토리와 상대 경로를 결합하여 삭제할 파일의 실제 경로를 구성합니다.
      Path filePathToDelete = Paths.get(uploadDir, relativePath).normalize();
      try {
        // 파일이 존재하는지 확인 후 삭제합니다.
        if (Files.exists(filePathToDelete)) {
          Files.delete(filePathToDelete);
          System.out.println("File deleted from local storage: " + filePathToDelete);
        } else {
          System.out.println("File not found for deletion: " + filePathToDelete);
        }
      } catch (IOException e) {
        System.err.println("Failed to delete file: " + filePathToDelete + " - " + e.getMessage());
        // 실제 서비스에서는 로깅 프레임워크를 사용하여 오류를 기록해야 합니다.
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
      // 업로드 디렉토리와 상대 경로를 결합하여 파일의 실제 경로를 구성합니다.
      Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
      Resource resource = new UrlResource(filePath.toUri());

      // 리소스가 존재하고 읽을 수 있는지 확인합니다.
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
