package talkingpotatoes.pickplaceapi.file.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 파일 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_file")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_srl")
    private Long fileSrl; // 파일 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (업로더)

    @Column(name = "file_nm")
    private String fileNm; // 파일 원본 이름

    @Column(name = "file_src")
    private String fileSrc; // 파일 경로

    @Column(name = "file_extension", length = 10)
    private String fileExtension; // 파일 확장자

    @Column(name = "uploaded_date", columnDefinition = "DATETIME")
    private LocalDateTime uploadedAt; // 업로드 일자

    @Column(name = "file_manage_srl", length = 50)
    private String fileManageSrl; // 파일 관리 번호 (UUID)
}
