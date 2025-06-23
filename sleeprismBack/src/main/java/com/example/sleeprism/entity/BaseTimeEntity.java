package com.example.sleeprism.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 이 클래스를 상속받는 엔티티들은 아래 필드를 컬럼으로 인식함.
@EntityListeners(AuditingEntityListener.class) // JPA Auditing 기능 사용하여 생성/수정 시간을 자동으로 관리
public abstract class BaseTimeEntity {

  @CreatedDate // 엔티티가 생성될 때 시간이 자동 저장됩니다.
  @Column(updatable = false, nullable = false) // 생성 시간은 업데이트되지 않으며, null을 허용하지 않습니다.
  private LocalDateTime createdAt;

  @LastModifiedDate // 엔티티가 수정될 때 시간이 자동 저장됩니다.
  @Column(nullable = false) // 수정 시간은 null을 허용하지 않습니다.
  private LocalDateTime updatedAt;

}
