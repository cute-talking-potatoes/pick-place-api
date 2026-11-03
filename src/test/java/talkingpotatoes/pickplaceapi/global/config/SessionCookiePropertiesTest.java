package talkingpotatoes.pickplaceapi.global.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.Session;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * application.yml의 세션 쿠키 설정이 실제 server.servlet.session.* 키로 바인딩되는지 검증한다.
 * <pre>
 *  - DB 없이 실행할 수 있도록 스프링 컨텍스트를 띄우지 않고 application.yml만 읽어 바인딩한다.
 *  - 시스템 환경변수(SESSION_COOKIE_SECURE)의 영향을 받지 않도록 yml 프로퍼티 소스만 사용한다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("세션 쿠키 설정 바인딩 테스트")
class SessionCookiePropertiesTest {

    @Test
    void 세션쿠키설정이_server_servlet_session으로_바인딩된다() throws IOException {
        // Given
        Binder binder = createBinder(Map.of());

        // When
        ServerProperties serverProperties = binder.bind("server", Bindable.of(ServerProperties.class))
                .orElseGet(ServerProperties::new);
        Session session = serverProperties.getServlet().getSession();
        Cookie cookie = session.getCookie();

        // Then
        assertAll(
                () -> assertEquals(Duration.ofDays(7), session.getTimeout()),
                () -> assertEquals(Boolean.TRUE, cookie.getHttpOnly()),
                () -> assertEquals(Cookie.SameSite.LAX, cookie.getSameSite()),
                () -> assertEquals(Boolean.FALSE, cookie.getSecure())
        );
    }

    @Test
    void 환경변수로_세션쿠키_secure를_켤수있다() throws IOException {
        // Given
        Binder binder = createBinder(Map.of("SESSION_COOKIE_SECURE", "true"));

        // When
        ServerProperties serverProperties = binder.bind("server", Bindable.of(ServerProperties.class))
                .orElseGet(ServerProperties::new);

        // Then
        assertEquals(Boolean.TRUE, serverProperties.getServlet().getSession().getCookie().getSecure());
    }

    @Test
    void 세션설정이_spring하위에_잘못_들어가있지않다() throws IOException {
        // Given
        Binder binder = createBinder(Map.of());

        // When & Then
        assertFalse(binder.bind("spring.server", Bindable.mapOf(String.class, Object.class)).isBound());
    }

    private Binder createBinder(Map<String, Object> overrides) throws IOException {
        List<PropertySource<?>> yamlSources = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"));

        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test-overrides", overrides));
        yamlSources.forEach(propertySources::addLast);

        return new Binder(
                ConfigurationPropertySources.from(propertySources),
                new PropertySourcesPlaceholdersResolver(propertySources)
        );
    }
}
