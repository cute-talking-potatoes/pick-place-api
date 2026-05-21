package talkingpotatoes.pickplaceapi.global.config;

import talkingpotatoes.pickplaceapi.user.domain.UserRole;

/**
 * 세션 기반 인증 사용자 정보
 * @author : 이나영
 * @since : 2026/05/19
 */
public record SessionUserPrincipal(
        // 내부 식별자. 현재 로그인 사용자를 다시 찾을 때 기준이 된다.
        Long userSrl,
        // 화면/로그에서 다루기 쉬운 로그인 아이디
        String userId,
        // Spring Security 권한 매핑의 기준 역할
        UserRole userRole
) {
}
