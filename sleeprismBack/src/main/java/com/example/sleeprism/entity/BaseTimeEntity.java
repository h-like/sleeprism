package com.example.sleeprism.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor; // 추가: @SuperBuilder 사용 시 함께 필요할 수 있음
import lombok.experimental.SuperBuilder; // 변경: @Builder -> @SuperBuilder
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor // @SuperBuilder 사용 시 함께 필요할 수 있음
@SuperBuilder // <-- @Builder 대신 @SuperBuilder 사용
public abstract class BaseTimeEntity {

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

}
