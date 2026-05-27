package talkingpotatoes.pickplaceapi.global.exception;

/**
 * 인증 예외
 *
 * @author : 이나영
 * @since : 2026/05/27
 */
public class AuthException extends PickPlaceException {

    public AuthException(ExceptionCode code) {
        super(code);
    }

    public AuthException(ExceptionCode code, String extraMessage) {
        super(code, extraMessage);
    }
}
