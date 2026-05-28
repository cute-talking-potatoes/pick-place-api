package talkingpotatoes.pickplaceapi.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;
import talkingpotatoes.pickplaceapi.user.dto.AuthResponse;
import talkingpotatoes.pickplaceapi.user.dto.SignUpRequest;

@DisplayName("인증 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void 존재하지않는_아이디로_로그인하면_공통인증예외가_발생한다() {
        // Given
        AuthService authService = new AuthService(userRepository, passwordEncoder, FIXED_CLOCK);
        given(userRepository.findByUserId("tester")).willReturn(Optional.empty());

        // When
        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.login(" tester ", "password1", new MockHttpServletRequest())
        );

        // Then
        assertEquals(ERR_AUTH_INVALID_CREDENTIALS.name(), exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void 비밀번호가_일치하지않으면_공통인증예외가_발생한다() {
        // Given
        AuthService authService = new AuthService(userRepository, passwordEncoder, FIXED_CLOCK);
        User user = createUser(1L, "tester", "encoded-password");
        given(userRepository.findByUserId("tester")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        // When
        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.login("tester", "wrong-password", new MockHttpServletRequest())
        );

        // Then
        assertEquals(ERR_AUTH_INVALID_CREDENTIALS.name(), exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void 로그인에_성공하면_세션에_사용자식별자를_저장한다() {
        // Given
        AuthService authService = new AuthService(userRepository, passwordEncoder, FIXED_CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
        User user = createUser(1L, "tester", "encoded-password");
        given(userRepository.findByUserId("tester")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1", "encoded-password")).willReturn(true);

        // When
        AuthResponse response = authService.login("tester", "password1", request);

        // Then
        assertEquals("tester", response.getUserId());
        HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertEquals(1L, session.getAttribute(AuthService.AUTH_SESSION_USER_SRL));
        assertEquals(7 * 24 * 60 * 60, session.getMaxInactiveInterval());
    }

    @Test
    void 중복된_아이디로_회원가입하면_공통인증예외가_발생한다() {
        // Given
        AuthService authService = new AuthService(userRepository, passwordEncoder, FIXED_CLOCK);
        SignUpRequest request = createSignUpRequest("tester", "tester@example.com", "tester", "password1", "password1");
        given(userRepository.findByUserId("tester")).willReturn(Optional.of(createUser(1L, "tester", "encoded")));

        // When
        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.register(request, new MockHttpServletRequest())
        );

        // Then
        assertEquals(ERR_AUTH_DUPLICATED_USER_ID.name(), exception.getCode());
        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void 비밀번호확인이_다르면_공통인증예외가_발생한다() {
        // Given
        AuthService authService = new AuthService(userRepository, passwordEncoder, FIXED_CLOCK);
        SignUpRequest request = createSignUpRequest("tester", "tester@example.com", "tester", "password1", "password2");

        // When
        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.register(request, new MockHttpServletRequest())
        );

        // Then
        assertEquals(ERR_AUTH_PASSWORD_CONFIRM_MISMATCH.name(), exception.getCode());
        assertEquals(400, exception.getStatus().value());
        then(userRepository).should(never()).findByUserId(any());
    }

    private User createUser(Long userSrl, String userId, String encodedPassword) {
        User user = User.createLocalUser(
                userId,
                userId + "@example.com",
                encodedPassword,
                "테스터",
                LocalDateTime.now(FIXED_CLOCK)
        );
        ReflectionTestUtils.setField(user, "userSrl", userSrl);
        return user;
    }

    private SignUpRequest createSignUpRequest(
            String userId,
            String email,
            String nickname,
            String password,
            String passwordConfirm
    ) {
        SignUpRequest request = new SignUpRequest();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "passwordConfirm", passwordConfirm);
        return request;
    }
}
