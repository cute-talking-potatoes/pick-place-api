package talkingpotatoes.pickplaceapi.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;
import talkingpotatoes.pickplaceapi.user.service.AuthService;

import java.io.IOException;
import java.util.List;

/**
 * HttpSession 기반 인증 복원 필터
 * @author : 이나영
 * @since : 2026/05/19
 */
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    // 세션에 들어 있는 사용자 식별자로 실제 사용자를 다시 조회한다.
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 이미 인증된 요청은 건드리지 않고, 세션에 저장된 사용자만 복원한다.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                // 세션에는 최소 식별자만 저장하고, 현재 권한/상태는 DB 기준으로 다시 읽는다.
                Object userSrlValue = session.getAttribute(AuthService.AUTH_SESSION_USER_SRL);
                if (userSrlValue instanceof Long userSrl) {
                    userRepository.findById(userSrl)
                            .ifPresent(user -> authenticate(user));
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user) {
        // 세션 사용자 정보를 SecurityContext 에 올려 이후 인가 로직에서 재사용한다.
        SessionUserPrincipal principal = new SessionUserPrincipal(
                user.getUserSrl(),
                user.getUserId(),
                user.getUserRole()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()))
                );

        // 이후 컨트롤러, 인가 규칙, 후속 필터가 모두 같은 인증 컨텍스트를 공유한다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
