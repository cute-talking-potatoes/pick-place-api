package talkingpotatoes.pickplaceapi.global.api;

import lombok.Getter;

/**
 * 성공 응답 코드
 *
 * @author : 이나영
 * @since : 2026/05/27
 */
@Getter
public enum SuccessCode {
    SUC_AUTH_CSRF_TOKEN_ISSUED("CSRF 토큰을 발급했습니다."),
    SUC_AUTH_LOGIN("로그인 성공"),
    SUC_AUTH_SIGNUP("회원가입이 완료되었습니다."),
    SUC_AUTH_CURRENT_USER_FOUND("현재 사용자 조회 성공"),
    SUC_AUTH_LOGOUT("로그아웃 성공"),
    SUC_PLACE_FIND("장소 조회 성공"),
    SUC_PLACE_CREATE("장소 등록 성공"),
    SUC_PLACE_UPDATE("장소 수정 성공"),
    SUC_PLACE_DELETE("장소 삭제 성공"),
    ;

    private final String message;

    SuccessCode(String message) {
        this.message = message;
    }
}
