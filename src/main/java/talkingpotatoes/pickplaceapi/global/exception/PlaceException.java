package talkingpotatoes.pickplaceapi.global.exception;

/**
 * 장소 예외
 *
 * @author : 박지혁
 * @since : 2026/06/21
 */
public class PlaceException extends PickPlaceException {

    public PlaceException(ExceptionCode code) {
        super(code);
    }

    public PlaceException(ExceptionCode code, String extraMessage) {
        super(code, extraMessage);
    }
}
