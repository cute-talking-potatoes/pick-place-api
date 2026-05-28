package talkingpotatoes.pickplaceapi.global.api;

import static org.junit.jupiter.api.Assertions.*;
import static talkingpotatoes.pickplaceapi.global.api.SuccessCode.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

@DisplayName("공통 API 응답 테스트")
class ApiResponseTest {

    @Test
    void 성공코드와_데이터로_200_성공응답을_생성한다() {
        // Given
        String data = "token";

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponse.ok(SUC_AUTH_CSRF_TOKEN_ISSUED, data);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getCode());
        assertEquals("CSRF 토큰을 발급했습니다.", response.getBody().getMessage());
        assertEquals(data, response.getBody().getData());
    }

    @Test
    void 성공코드만으로_데이터없는_200_성공응답을_생성한다() {
        // When
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.ok(SUC_AUTH_LOGOUT);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getCode());
        assertEquals("로그아웃 성공", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void null_필드는_JSON_응답에서_제외한다() {
        // When
        JsonInclude include = ApiResponse.class.getAnnotation(JsonInclude.class);

        // Then
        assertNotNull(include);
        assertEquals(JsonInclude.Include.NON_NULL, include.value());
    }
}
