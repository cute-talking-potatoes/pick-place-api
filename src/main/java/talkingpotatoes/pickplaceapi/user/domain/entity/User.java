package talkingpotatoes.pickplaceapi.user.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import talkingpotatoes.pickplaceapi.global.domain.entity.BaseEntity;
import talkingpotatoes.pickplaceapi.user.domain.UserRole;

/**
 * 회원 엔티티
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Table(name = "tb_user")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_srl")
    private Long userSrl; // 회원 NO (PK)

    @Column(name = "user_id", length = 50)
    private String userId; // 회원 ID

    @Column(name = "email")
    private String email; // 회원 이메일

    @Column(name = "password")
    private String password; // 회원 비밀번호

    @Column(name = "nickname", length = 50)
    private String nickname; // 회원 닉네임

    @Column(name = "user_role")
    @Enumerated(value = EnumType.STRING)
    private UserRole userRole; // 회원 권한

    @Column(name = "last_login_at", columnDefinition = "DATETIME")
    private LocalDateTime lastLoginAt; // 최종 로그인 일시
}
