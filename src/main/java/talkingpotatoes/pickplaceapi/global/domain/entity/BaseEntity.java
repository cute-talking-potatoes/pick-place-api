package talkingpotatoes.pickplaceapi.global.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 엔티티 추적용 공통 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @Column(name = "created_by", updatable = false)
    private String createdBy; // 생성자 (단순 기록용, FK X)

    @Column(name = "updated_by")
    private String updatedBy; // 수정자 (단순 기록용, FK X)

    @CreatedDate
    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt; // 등록일시

    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt; // 수정일시
}
