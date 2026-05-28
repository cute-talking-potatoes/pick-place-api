package talkingpotatoes.pickplaceapi.global.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import org.springframework.http.ResponseEntity;

/**
 * 공통 API 응답
 * @author : 이나영
 * @since : 2026/05/19
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // 프론트가 성공/실패를 공통 방식으로 판단할 수 있게 하는 플래그
    private final boolean success;
    // 프론트가 실패 원인을 안정적으로 분기할 수 있게 하는 코드
    private final String code;
    // 사용자에게 보여줄 처리 결과 메시지
    private final String message;
    // 실제 응답 데이터
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        // 성공 응답 생성 방식을 한곳에 모아 컨트롤러 응답 형식을 통일한다.
        return new ApiResponse<>(true, null, message, data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(success(message, data));
    }

    public static ResponseEntity<ApiResponse<Void>> ok(String message) {
        return ok(message, null);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(SuccessCode code, T data) {
        return ok(code.getMessage(), data);
    }

    public static ResponseEntity<ApiResponse<Void>> ok(SuccessCode code) {
        return ok(code.getMessage());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        // 실패 응답도 같은 형식을 사용해 프론트 분기 처리를 단순화한다.
        return error(null, message, data);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}
