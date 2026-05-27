package talkingpotatoes.pickplaceapi.global.exception;

/**
 * 파일 예외
 *
 * @author : 박지혁
 * @since : 2026/04/12
 */
public class FileException extends PickPlaceException {

    public FileException(ExceptionCode code) {
        super(code);
    }

    public FileException(ExceptionCode code, String extraMessage) {
        super(code, extraMessage);
    }
}
