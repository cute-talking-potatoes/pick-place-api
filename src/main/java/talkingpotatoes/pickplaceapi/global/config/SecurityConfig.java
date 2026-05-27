package talkingpotatoes.pickplaceapi.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import talkingpotatoes.pickplaceapi.global.security.SessionAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.time.Clock;

/**
 * 보안 설정
 * @author : 이나영
 * @since : 2026/05/19
 */
@Configuration
public class SecurityConfig {

    // 세션 인증을 Spring Security 인증 객체로 복원하는 커스텀 필터
    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // 브라우저 CSRF 쿠키는 보내되, 크로스 사이트 전송 범위는 완화된 수준으로 제한한다.
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        http
                // 세션 기반 앱이므로 CSRF 토큰은 쿠키로 내려 프론트가 헤더로 다시 보낼 수 있게 한다.
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .authorizeHttpRequests(authorize -> authorize
                        // 로그인 전 접근이 필요한 인증 엔드포인트만 공개한다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 나머지 인증 관련 조회/변경 API 는 로그인 세션이 필요하다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .anyRequest().authenticated()
                )
                // 필요할 때만 세션을 만들고, 세션이 존재하면 그 인증 상태를 사용한다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exception -> exception
                        // 브라우저 리다이렉트 대신 401 을 내려 SPA/클라이언트가 직접 처리하게 한다.
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // 직접 구현한 세션 로그인 방식을 사용하므로 기본 로그인 기능은 비활성화한다.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // UsernamePasswordAuthenticationFilter 전에 세션 인증을 복원한다.
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호는 해시값으로만 저장/비교한다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock clock() {
        // 시간 생성을 빈으로 분리해 테스트에서 고정 시간을 주입할 수 있게 한다.
        return Clock.systemDefaultZone();
    }
}
