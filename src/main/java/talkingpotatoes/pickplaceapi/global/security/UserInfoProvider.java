package talkingpotatoes.pickplaceapi.global.security;

import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.global.exception.ExceptionCode;
import talkingpotatoes.pickplaceapi.global.exception.FileException;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;

/**
 * 시큐리티에서 사용자 정보를 꺼내 제공해주는 클래스
 *
 * @author : 박지혁
 * @since : 2026/05/25
 */
@Component
@RequiredArgsConstructor
public class UserInfoProvider {

    private final UserRepository userRepository;

    public String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(ERR_AUTH_REQUIRED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SessionUserPrincipal sessionUserPrincipal)) {
            throw new AuthException(ERR_AUTH_INVALID_SESSION);
        }

        return sessionUserPrincipal.userId();
    }

    public User getUser() {
        return userRepository.findByUserId(getUserId())
                .orElseThrow(() -> new AuthException(ERR_AUTH_USER_NOT_FOUND));
    }

    public void checkUserAuthorization(String userId) {
        if (!getUserId().equals(userId)) {
            throw new AuthException(ERR_AUTH_ACCESS_DENIED);
        }
    }
}
