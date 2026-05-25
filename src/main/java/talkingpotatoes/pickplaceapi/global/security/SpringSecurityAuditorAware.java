package talkingpotatoes.pickplaceapi.global.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring Security 인증 정보 기반 JPA 감사자 제공자
 * @author : 이나영
 * @since : 2026/05/21
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SessionUserPrincipal sessionUserPrincipal) {
            return Optional.ofNullable(sessionUserPrincipal.userId());
        }

        return Optional.empty();
    }
}
