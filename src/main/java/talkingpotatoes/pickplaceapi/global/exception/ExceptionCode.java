package talkingpotatoes.pickplaceapi.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 예외 코드
 * <pre>
 *  - 한글 예외 메시지는 최대한 상세히 작성해주세요.
 *  - ERR_도메인_메시지 형태로 작성해주세요.
 * </pre>
 * @author : 박지혁
 * @since : 2026/04/12
 */
@Getter
public enum ExceptionCode {
    ERR_FILE_CREATE_DIRECTORY("파일 폴더 생성에 실패했습니다."),
    ERR_FILE_SAVE("파일 저장에 실패했습니다."),
    ERR_FILE_UPDATE("파일 수정에 실패했습니다."),
    ERR_FILE_DELETE("파일 삭제에 실패했습니다."),
    ERR_FILE_EXTENSION_NOT_MATCH("업로드할 수 없는 파일 확장자입니다."),
    ERR_FILE_CANNOT_DOWNLOAD("다운로드 할 수 없는 파일입니다."),
    ERR_FILE_NOT_EXIST("존재하지 않는 파일입니다."),
    ERR_FILE_FAIL_DOWNLOAD("파일 다운로드에 실패했습니다."),
    ERR_FILE_SAVE_INCORRECT_TRANSACTION("파일 저장은 트랜잭션 안에서만 수행되어야 합니다."),

    ERR_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ERR_AUTH_INVALID_SESSION(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다."),
    ERR_AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "현재 사용자 정보를 찾을 수 없습니다."),
    ERR_AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ERR_AUTH_DUPLICATED_USER_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    ERR_AUTH_DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ERR_AUTH_PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    ERR_AUTH_PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상으로 입력해 주세요."),
    ERR_AUTH_PASSWORD_RULE_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호는 영문과 숫자를 모두 포함해 주세요."),
    ERR_AUTH_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "아이디를 입력해 주세요."),
    ERR_AUTH_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일을 입력해 주세요."),
    ERR_AUTH_NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "닉네임을 입력해 주세요."),
    ERR_AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 작업에 대한 권한이 없습니다."),

    ERR_PLACE_INVALID_TITLE(HttpStatus.BAD_REQUEST, "장소명이 유효하지 않습니다."),
    ERR_PLACE_INVALID_MAP(HttpStatus.BAD_REQUEST, "장소의 좌표가 유효하지 않습니다."),
    ERR_PLACE_INVALID_LATITUDE(HttpStatus.BAD_REQUEST, "위도가 유효한 범위가 아닙니다."),
    ERR_PLACE_INVALID_LONGITUDE(HttpStatus.BAD_REQUEST, "경도가 유효한 범위가 아닙니다."),
    ERR_PLACE_NOT_FOUND(HttpStatus.BAD_REQUEST, "요청한 장소를 찾을 수 없습니다."),
    ;
    private final HttpStatus status;
    private final String korErrorMessage;

    ExceptionCode(String korErrorMessage) {
        this(HttpStatus.BAD_REQUEST, korErrorMessage);
    }

    ExceptionCode(HttpStatus status, String korErrorMessage) {
        this.status = status;
        this.korErrorMessage = korErrorMessage;
    }
}
