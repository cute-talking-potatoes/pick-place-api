package talkingpotatoes.pickplaceapi.user.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import talkingpotatoes.pickplaceapi.user.dto.AuthResponse;
import talkingpotatoes.pickplaceapi.user.dto.SignUpRequest;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Auth 서비스
 * @author : 이나영
 * @since : 2026/05/19
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    // HttpSession 에 저장할 로그인 사용자 식별 키
    public static final String AUTH_SESSION_USER_SRL = "AUTH_SESSION_USER_SRL";

    // 서버 세션 최대 유지 시간
    private static final Duration SESSION_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // 회원가입
    @Transactional
    public AuthResponse register(SignUpRequest request, HttpServletRequest servletRequest) {
        // 비교/저장 기준을 통일하기 위해 사용자 입력을 먼저 정규화한다.
        String normalizedUserId = normalizeUserId(request.getUserId());
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedNickname = normalizeNickname(request.getNickname());
        String rawPassword = request.getPassword();

        // 회원가입 폼 전용 검증은 서비스에서 한 번 더 확인해 명확한 400 응답으로 연결한다.
        validatePasswordConfirmation(rawPassword, request.getPasswordConfirm());
        validatePasswordRules(rawPassword);
        // 중복 검사는 저장 직전에 수행해야 최신 DB 상태를 기준으로 막을 수 있다.
        validateSignUpUniqueness(normalizedUserId, normalizedEmail);

        LocalDateTime now = now();
        // 비밀번호는 엔티티에 넣기 전에 반드시 인코딩하고, 생성 규칙은 엔티티 팩토리에 위임한다.
        User user = userRepository.save(User.createLocalUser(
                normalizedUserId,
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                normalizedNickname,
                now
        ));

        // 가입 직후 바로 로그인된 상태로 전환하기 위해 세션을 생성한다.
        establishSession(servletRequest, user.getUserSrl());
        return AuthResponse.from(user);
    }

    // 로그인
    @Transactional
    public AuthResponse login(String userId, String password, HttpServletRequest servletRequest) {
        // 로그인 비교도 회원가입과 같은 규칙으로 정규화한 userId 기준으로 조회한다.
        User user = userRepository.findByUserId(normalizeUserId(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 불일치 여부는 사용자 존재 여부와 같은 메시지로 응답해 계정 추측을 어렵게 한다.
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 로그인 성공 시점만 마지막 로그인 시간으로 기록한다.
        user.updateLastLoginAt(now());
        // 기존 세션이 있더라도 새 세션으로 교체해 현재 로그인 사용자 기준을 명확히 맞춘다.
        establishSession(servletRequest, user.getUserSrl());
        return AuthResponse.from(user);
    }

    // 현재 로그인 사용자 조회
    public AuthResponse me(HttpServletRequest request) {
        return AuthResponse.from(getCurrentUser(request));
    }

    // 로그아웃
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        clearSessionCookie(request, response);
    }

    private void validateSignUpUniqueness(String normalizedUserId, String normalizedEmail) {
        // userId 와 email 을 각각 분리 검사해 클라이언트가 어떤 값이 충돌했는지 바로 알 수 있게 한다.
        if (userRepository.findByUserId(normalizedUserId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        // 세션이 없으면 인증 상태가 없다는 뜻이므로 바로 401 로 처리한다.
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 세션에는 최소 식별자만 저장하고, 실제 사용자 정보는 매 요청마다 DB 기준으로 다시 확인한다.
        Object userSrlValue = session.getAttribute(AUTH_SESSION_USER_SRL);
        if (!(userSrlValue instanceof Long userSrl)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return userRepository.findById(userSrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 사용자 정보를 찾을 수 없습니다."));
    }

    private void establishSession(HttpServletRequest request, Long userSrl) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            // 다른 사용자 정보가 남아 있을 수 있으므로 로그인 전에 기존 세션을 비운다.
            existingSession.invalidate();
        }

        // 로그인 직후 새 세션을 만들어 세션 고정 공격 가능성을 줄인다.
        HttpSession session = request.getSession(true);
        // 세션에는 화면에 필요한 전체 사용자 객체 대신 최소 식별자만 저장한다.
        session.setMaxInactiveInterval((int) SESSION_TTL.getSeconds());
        session.setAttribute(AUTH_SESSION_USER_SRL, userSrl);
        request.changeSessionId();
    }

    private void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        // 브라우저에 만료된 JSESSIONID 를 내려 세션 쿠키를 제거한다.
        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());
    }

    // 비밀번호 확인
    private void validatePasswordConfirmation(String password, String passwordConfirm) {
        if (!StringUtils.hasText(password) || !Objects.equals(password, passwordConfirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다.");
        }
    }

    // 비밀번호 규칙
    private void validatePasswordRules(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상으로 입력해 주세요.");
        }

        // 최소 규칙만 서비스에서 강제하고, 더 복잡한 정책이 생기면 이 메서드에서 확장한다.
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 영문과 숫자를 모두 포함해 주세요.");
        }
    }

    private String normalizeUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디를 입력해 주세요.");
        }
        return userId.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 입력해 주세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임을 입력해 주세요.");
        }
        return nickname.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
