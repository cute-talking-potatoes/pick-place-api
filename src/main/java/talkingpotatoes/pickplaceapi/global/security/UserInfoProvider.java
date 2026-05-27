package talkingpotatoes.pickplaceapi.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
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
        if (authentication == null) {
            throw new RuntimeException(); // TODO: 2026/05/25 회원처리에 대한 공통 예외가 추가되면 해당 예외를 던지도록 수정하기
        }
        SessionUserPrincipal principal = (SessionUserPrincipal)authentication.getPrincipal();
        if (principal == null) {
            throw new RuntimeException(); // TODO: 2026/05/25 회원처리에 대한 공통 예외가 추가되면 해당 예외를 던지도록 수정하기
        }
        return principal.userId();
    }

    public User getUser() {
        return userRepository.findByUserId(getUserId()).orElseThrow(RuntimeException::new); // TODO: 2026/05/25 회원처리에 대한 공통 예외가 추가되면 해당 예외를 던지도록 수정하기
    }
}
