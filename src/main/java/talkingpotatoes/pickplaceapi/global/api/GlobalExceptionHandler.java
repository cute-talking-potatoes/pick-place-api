package talkingpotatoes.pickplaceapi.global.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증 중심 전역 예외 처리
 * @author : 이나영
 * @since : 2026/05/19
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        // DTO 검증 실패는 필드 메시지를 그대로 내려 프론트가 즉시 표시할 수 있게 한다.
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";
        return ResponseEntity.badRequest().body(ApiResponse.error(message, null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        // 서비스에서 의도적으로 지정한 HTTP 상태/메시지를 유지한다.
        String message = exception.getReason() != null ? exception.getReason() : "요청 처리 중 오류가 발생했습니다.";
        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiResponse.error(message, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        // 엔티티 생성 규칙 위반 같은 잘못된 입력 계열 예외는 400 으로 처리한다.
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(exception.getMessage(), null));
    }
}
