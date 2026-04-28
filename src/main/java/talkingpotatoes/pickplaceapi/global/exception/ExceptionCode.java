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
    ERR_FILE_CREATE_DIRECTORY("파일 폴더 생성에 실패했습니다."),
    ERR_FILE_SAVE("파일 저장에 실패했습니다."),
    ERR_FILE_DELETE("파일 삭제에 실패했습니다."),
    ERR_FILE_EXTENSION_NOT_MATCH("업로드할 수 없는 파일 확장자입니다."),
    ERR_FILE_CANNOT_DOWNLOAD("다운로드 할 수 없는 파일입니다."),
    ERR_FILE_NOT_EXIST("존재하지 않는 파일입니다."),
    ERR_FILE_FAIL_DOWNLOAD("파일 다운로드에 실패했습니다."),
    ;
    private final String korErrorMessage;
}
