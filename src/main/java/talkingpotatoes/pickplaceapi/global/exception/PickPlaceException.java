package talkingpotatoes.pickplaceapi.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 의도적으로 발생시키는 모든 예외의 최상위 예외
 * <pre>
 *  - 의도적으로 발생시키는 예외는 모두 이 클래스를 상속받아주세요.
 *  - 기본적으로는 기본 생성자, 추가 메시지를 원하는 경우에는 추가 메시지 생성자를 사용해주세요.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/04/12
 */
@Getter
public class PickPlaceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String message;

    // 기본 예외 생성자
    public PickPlaceException(ExceptionCode code) {
        super(code.getKorErrorMessage());
        this.status = code.getStatus();
        this.code = code.name();
        this.message = code.getKorErrorMessage();
    }

    // 추가 메시지를 포함한 예외 생성자
    public PickPlaceException(ExceptionCode code, String extraMessage) {
        super(code.getKorErrorMessage() + "\n📝 추가 메시지: " + extraMessage);
        this.status = code.getStatus();
        this.code = code.name();
        this.message = code.getKorErrorMessage() + "\n📝 추가 메시지: " + extraMessage;
    }
}
