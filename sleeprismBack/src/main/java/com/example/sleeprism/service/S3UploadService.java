package com.example.sleeprism.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
// 실제 S3 SDK를 사용한다면 여기에 관련 클래스를 임포트해야 합니다.
// 예: import com.amazonaws.services.s3.AmazonS3Client;

/**
 * S3 또는 유사한 클라우드 스토리지에 파일을 업로드하는 서비스입니다.
 */
@Service // 이 클래스를 Spring Bean으로 등록합니다.
@Slf4j // 로깅을 위한 Lombok 어노테이션
public class S3UploadService {

  // 실제 S3 클라이언트를 주입받을 필드 (나중에 구현 시 필요)
  // private final AmazonS3Client amazonS3Client;
  // private final String bucketName; // S3 버킷 이름 (application.properties 등에서 설정)

  // 생성자 (필요하다면 S3Client 등을 주입받도록 수정)
  // public S3UploadService(AmazonS3Client amazonS3Client, @Value("${cloud.aws.s3.bucket}") String bucketName) {
  //     this.amazonS3Client = amazonS3Client;
  //     this.bucketName = bucketName;
  // }

  /**
   * 파일을 클라우드 스토리지에 업로드하고 URL을 반환합니다.
   * 이 메서드는 현재 임시로 더미 URL을 반환하며, 실제 S3 로직으로 대체해야 합니다.
   *
   * @param multipartFile 업로드할 파일
   * @param directoryPath 파일을 저장할 디렉토리 경로 (예: "profile-images", "post-attachments")
   * @return 업로드된 파일의 URL
   * @throws IOException 파일 처리 중 발생할 수 있는 예외
   */
  public String uploadFile(MultipartFile multipartFile, String directoryPath) throws IOException {
    if (multipartFile.isEmpty()) {
      log.warn("Attempted to upload an empty file to S3.");
      return null;
    }

    // 실제 S3 업로드 로직을 여기에 구현해야 합니다.
    // 예를 들어:
    // String fileName = directoryPath + "/" + UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
    // ObjectMetadata metadata = new ObjectMetadata();
    // metadata.setContentLength(multipartFile.getSize());
    // metadata.setContentType(multipartFile.getContentType());
    // amazonS3Client.putObject(bucketName, fileName, multipartFile.getInputStream(), metadata);
    // return amazonS3Client.getUrl(bucketName, fileName).toString();

    log.info("S3UploadService: File '{}' of type '{}' uploaded to directory '{}'. (Dummy URL returned)",
        multipartFile.getOriginalFilename(), multipartFile.getContentType(), directoryPath);

    // 임시 더미 URL 반환 (실제 구현 시 S3 URL로 교체)
    return "https://your-s3-bucket.com/" + directoryPath + "/" + multipartFile.getOriginalFilename();
  }

  /**
   * 클라우드 스토리지에서 파일을 삭제하는 메서드입니다.
   * (선택 사항: 파일 삭제 기능이 필요하다면 구현)
   * @param fileUrl 삭제할 파일의 URL
   */
  public void deleteFile(String fileUrl) {
    // 실제 S3 파일 삭제 로직 구현
    // 예: String fileName = extractFileNameFromUrl(fileUrl);
    // amazonS3Client.deleteObject(bucketName, fileName);
    log.info("S3UploadService: File with URL '{}' deleted. (Dummy deletion)", fileUrl);
  }

  // URL에서 파일 이름을 추출하는 헬퍼 메서드 (선택 사항)
  // private String extractFileNameFromUrl(String fileUrl) {
  //     return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
  // }
}
