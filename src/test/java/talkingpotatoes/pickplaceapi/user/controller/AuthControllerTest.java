package talkingpotatoes.pickplaceapi.user.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.CsrfToken;

import talkingpotatoes.pickplaceapi.global.api.ApiResponse;
import talkingpotatoes.pickplaceapi.user.dto.AuthRequest;
import talkingpotatoes.pickplaceapi.user.dto.AuthResponse;
import talkingpotatoes.pickplaceapi.user.service.AuthService;

@DisplayName("인증 컨트롤러 테스트")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Test
    void CSRF토큰_발급시_성공코드_응답을_반환한다() {
        // Given
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value");

        // When
        ResponseEntity<ApiResponse<java.util.Map<String, String>>> response = authController.csrf(csrfToken);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("CSRF 토큰을 발급했습니다.", response.getBody().getMessage());
        assertEquals("token-value", response.getBody().getData().get("token"));
    }

    @Test
    void 로그인_성공시_성공코드_응답을_반환한다() {
        // Given
        AuthRequest request = mock(AuthRequest.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        AuthResponse authResponse = AuthResponse.builder()
                .userId("tester")
                .nickname("테스터")
                .email("tester@example.com")
                .build();
        given(request.getUserId()).willReturn("tester");
        given(request.getPassword()).willReturn("password1");
        given(authService.login("tester", "password1", servletRequest)).willReturn(authResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(request, servletRequest);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("로그인 성공", response.getBody().getMessage());
        assertSame(authResponse, response.getBody().getData());
    }

    @Test
    void 로그아웃_성공시_데이터없는_성공코드_응답을_반환한다() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // When
        ResponseEntity<ApiResponse<Void>> result = authController.logout(request, response);

        // Then
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isSuccess());
        assertEquals("로그아웃 성공", result.getBody().getMessage());
        assertNull(result.getBody().getData());
        then(authService).should().logout(request, response);
    }
}
