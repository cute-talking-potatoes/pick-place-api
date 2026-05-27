package talkingpotatoes.pickplaceapi.global.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.user.domain.UserRole;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;

@DisplayName("사용자 인증 정보 제공 테스트")
@ExtendWith(MockitoExtension.class)
class UserInfoProviderTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증정보가_없으면_인증필요_예외가_발생한다() {
        // Given
        UserInfoProvider provider = new UserInfoProvider(userRepository);

        // When
        AuthException exception = assertThrows(AuthException.class, provider::getUserId);

        // Then
        assertEquals(ERR_AUTH_REQUIRED.name(), exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void 세션_사용자_주체가_아니면_잘못된_인증정보_예외가_발생한다() {
        // Given
        UserInfoProvider provider = new UserInfoProvider(userRepository);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "anonymousUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        AuthException exception = assertThrows(AuthException.class, provider::getUserId);

        // Then
        assertEquals(ERR_AUTH_INVALID_SESSION.name(), exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void 세션_사용자_주체에서_사용자아이디를_꺼낸다() {
        // Given
        UserInfoProvider provider = new UserInfoProvider(userRepository);
        setAuthentication(new SessionUserPrincipal(1L, "tester", UserRole.USER));

        // When
        String userId = provider.getUserId();

        // Then
        assertEquals("tester", userId);
    }

    @Test
    void 인증된_사용자가_DB에_없으면_사용자없음_예외가_발생한다() {
        // Given
        UserInfoProvider provider = new UserInfoProvider(userRepository);
        setAuthentication(new SessionUserPrincipal(1L, "tester", UserRole.USER));
        given(userRepository.findByUserId("tester")).willReturn(Optional.empty());

        // When
        AuthException exception = assertThrows(AuthException.class, provider::getUser);

        // Then
        assertEquals(ERR_AUTH_USER_NOT_FOUND.name(), exception.getCode());
        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void 인증된_사용자를_DB에서_조회한다() {
        // Given
        UserInfoProvider provider = new UserInfoProvider(userRepository);
        User user = mock(User.class);
        setAuthentication(new SessionUserPrincipal(1L, "tester", UserRole.USER));
        given(userRepository.findByUserId("tester")).willReturn(Optional.of(user));

        // When
        User result = provider.getUser();

        // Then
        assertSame(user, result);
    }

    private void setAuthentication(SessionUserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.userRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
