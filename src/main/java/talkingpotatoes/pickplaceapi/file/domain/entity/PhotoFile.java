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
 * 사진첩 파일 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_photo_file")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_file_srl")
    private Long photoFileSrl; // 사진첩 파일 NO (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_srl")
    private File file; // 파일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_srl")
    private User user; // 회원 (업로더)
}
