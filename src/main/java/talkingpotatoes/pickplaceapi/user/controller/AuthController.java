package talkingpotatoes.pickplaceapi.user.controller;

import static talkingpotatoes.pickplaceapi.global.api.SuccessCode.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import talkingpotatoes.pickplaceapi.global.api.ApiResponse;
import talkingpotatoes.pickplaceapi.user.dto.AuthRequest;
import talkingpotatoes.pickplaceapi.user.dto.AuthResponse;
import talkingpotatoes.pickplaceapi.user.dto.SignUpRequest;
import talkingpotatoes.pickplaceapi.user.service.AuthService;

import java.util.Map;

/**
 * 로그인, 회원가입 컨트롤러
 * @author : 이나영
 * @since : 2026/05/19
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // CSRF 토큰 발급
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> csrf(CsrfToken csrfToken) {
        // 프론트는 이 토큰을 읽어 이후 POST 요청의 헤더에 실어 보낸다.
        Map<String, String> data = Map.of(
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken()
        );
        return ApiResponse.ok(SUC_AUTH_CSRF_TOKEN_ISSUED, data);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthResponse authResponse = authService.login(request.getUserId(), request.getPassword(), httpServletRequest);
        return ApiResponse.ok(SUC_AUTH_LOGIN, authResponse);
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignUpRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthResponse authResponse = authService.register(request, httpServletRequest);
        return ApiResponse.ok(SUC_AUTH_SIGNUP, authResponse);
    }

    // 현재 로그인 사용자 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(HttpServletRequest request) {
        return ApiResponse.ok(SUC_AUTH_CURRENT_USER_FOUND, authService.me(request));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(request, response);
        return ApiResponse.ok(SUC_AUTH_LOGOUT);
    }
}
