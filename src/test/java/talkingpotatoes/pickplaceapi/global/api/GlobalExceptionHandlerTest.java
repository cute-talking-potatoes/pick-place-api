package talkingpotatoes.pickplaceapi.global.api;

import static org.junit.jupiter.api.Assertions.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

@DisplayName("전역 예외 처리 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 인증_공통예외는_지정된_HTTP상태와_공통응답으로_변환한다() {
        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handlePickPlace(new AuthException(ERR_AUTH_REQUIRED));

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("로그인이 필요합니다.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void 파일_공통예외는_기본_BAD_REQUEST와_공통응답으로_변환한다() {
        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handlePickPlace(new FileException(ERR_FILE_NOT_EXIST));

        // Then
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("존재하지 않는 파일입니다.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
