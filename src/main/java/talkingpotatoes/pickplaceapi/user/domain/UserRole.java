package talkingpotatoes.pickplaceapi.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원 권한
 * @author : 박지혁
 * @since : 2026/03/08
 */
@Getter
@AllArgsConstructor
public enum UserRole {
    USER("일반회원"),
    ADMIN("관리자"),
    ;

    private final String korRole;
}
