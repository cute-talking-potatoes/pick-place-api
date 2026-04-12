package talkingpotatoes.pickplaceapi.global.exception;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public enum ExceptionCode {
    ;
    private final String korErrorMessage;
}
