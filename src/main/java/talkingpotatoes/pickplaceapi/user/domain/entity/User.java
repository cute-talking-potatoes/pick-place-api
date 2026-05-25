package talkingpotatoes.pickplaceapi.user.domain.entity;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

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

    private static final int USER_ID_MAX_LENGTH = 50;
    private static final int NICKNAME_MAX_LENGTH = 50;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z0-9]+$");

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

    private User(
            String userId,
            String email,
            String password,
            String nickname,
            UserRole userRole,
            LocalDateTime lastLoginAt
    ) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.userRole = userRole;
        this.lastLoginAt = lastLoginAt;
    }

    public static User createLocalUser(
            String userId,
            String email,
            String encodedPassword,
            String nickname,
            LocalDateTime lastLoginAt
    ) {
        // DTO 검증을 통과하지 않는 생성 경로도 있을 수 있어, 엔티티가 최소 도메인 규칙을 한 번 더 보장한다.
        validateUserId(userId);
        validateNickname(nickname);

        return new User(
                userId,
                email,
                encodedPassword,
                nickname,
                UserRole.USER,
                lastLoginAt
        );
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    private static void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }
        if (userId.length() > USER_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("아이디는 50자 이하로 입력해 주세요.");
        }
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException("아이디는 영문 소문자와 숫자만 사용할 수 있습니다.");
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 50자 이하로 입력해 주세요.");
        }
    }
}
