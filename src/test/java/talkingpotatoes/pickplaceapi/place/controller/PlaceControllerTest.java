package talkingpotatoes.pickplaceapi.place.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.ERR_PLACE_NOT_FOUND;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import talkingpotatoes.pickplaceapi.global.config.SecurityConfig;
import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.global.security.SessionUserPrincipal;
import talkingpotatoes.pickplaceapi.global.security.SpringSecurityAuditorAware;
import talkingpotatoes.pickplaceapi.place.dto.PlaceDetailResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceMarkerResponse.PlacePhotoResponse;
import talkingpotatoes.pickplaceapi.place.service.PlaceService;
import talkingpotatoes.pickplaceapi.user.domain.UserRole;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;

/**
 * 장소 컨트롤러 API 계약 테스트 (MockMvc)
 * <pre>
 *  - 실제 SecurityConfig를 적용해 인증 여부에 따른 응답을 검증한다.
 *  - 서비스는 Mock으로 대체하고 HTTP 상태·공통 응답 형식·JSON 필드를 검증한다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("장소 컨트롤러 API 계약 테스트")
@WebMvcTest(PlaceController.class)
@Import({SecurityConfig.class, PlaceControllerTest.AuditingConfig.class})
class PlaceControllerTest {

    private static final String WRITER_ID = "writer-login-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;
    @MockitoBean
    private UserRepository userRepository; // SessionAuthenticationFilter 의존성
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext; // @EnableJpaAuditing 의존성

    @Test
    void 로그인사용자가_장소상세를_조회하면_공통성공응답과_camelCase_필드를_반환한다() throws Exception {
        // Given
        PlaceDetailResponse response = new PlaceDetailResponse(
                1L, "경복궁", "조선 법궁", "서울 종로구 사직로 161", "광화문", "03045",
                37.5786111111, 126.9772222222,
                List.of(
                        new PlacePhotoResponse(11L, 101L, "gyeongbokgung-1.jpg", ".jpg", "/api/place/photos/11"),
                        new PlacePhotoResponse(12L, 102L, "gyeongbokgung-2.png", ".png", "/api/place/photos/12")
                ),
                LocalDateTime.of(2026, 11, 1, 10, 0, 0),
                LocalDateTime.of(2026, 11, 2, 11, 30, 0),
                true
        );
        given(placeService.findPlaceDetail(1L)).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/place/{plSrl}", 1L).with(loginAs(WRITER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("장소 조회 성공"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.plSrl").value(1))
                .andExpect(jsonPath("$.data.plNm").value("경복궁"))
                .andExpect(jsonPath("$.data.plDesc").value("조선 법궁"))
                .andExpect(jsonPath("$.data.plAddr1").value("서울 종로구 사직로 161"))
                .andExpect(jsonPath("$.data.plAddr2").value("광화문"))
                .andExpect(jsonPath("$.data.plCd").value("03045"))
                .andExpect(jsonPath("$.data.plLat").value(37.5786111111))
                .andExpect(jsonPath("$.data.plLng").value(126.9772222222))
                .andExpect(jsonPath("$.data.isOwner").value(true))
                .andExpect(jsonPath("$.data.owner").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").value(org.hamcrest.Matchers.startsWith("2026-11-01T10:00")))
                .andExpect(jsonPath("$.data.updatedAt").value(org.hamcrest.Matchers.startsWith("2026-11-02T11:30")))
                .andExpect(jsonPath("$.data.photos", hasSize(2)))
                .andExpect(jsonPath("$.data.photos[0].photoFileSrl").value(11))
                .andExpect(jsonPath("$.data.photos[0].fileSrl").value(101))
                .andExpect(jsonPath("$.data.photos[0].fileName").value("gyeongbokgung-1.jpg"))
                .andExpect(jsonPath("$.data.photos[0].fileExtension").value(".jpg"))
                .andExpect(jsonPath("$.data.photos[0].imageUrl").value("/api/place/photos/11"))
                .andExpect(jsonPath("$.data.photos[1].photoFileSrl").value(12))
                .andExpect(jsonPath("$.data.photos[1].imageUrl").value("/api/place/photos/12"))
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedBy").doesNotExist())
                .andExpect(content().string(not(containsString(WRITER_ID))));
    }

    @Test
    void 존재하지않는_장소상세를_조회하면_공통에러응답을_반환한다() throws Exception {
        // Given
        given(placeService.findPlaceDetail(999L)).willThrow(new PlaceException(ERR_PLACE_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/place/{plSrl}", 999L).with(loginAs(WRITER_ID)))
                .andExpect(status().is(ERR_PLACE_NOT_FOUND.getStatus().value()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ERR_PLACE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 장소를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 로그인하지않은_사용자가_장소상세를_조회하면_401을_반환한다() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/place/{plSrl}", 1L))
                .andExpect(status().isUnauthorized());
        then(placeService).should(never()).findPlaceDetail(any());
    }

    private RequestPostProcessor loginAs(String userId) {
        SessionUserPrincipal principal = new SessionUserPrincipal(1L, userId, UserRole.USER);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    /**
     * 애플리케이션의 @EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")가 찾는 이름으로 등록한다.
     */
    @TestConfiguration
    static class AuditingConfig {

        @Bean(name = "springSecurityAuditorAware")
        SpringSecurityAuditorAware springSecurityAuditorAware() {
            return new SpringSecurityAuditorAware();
        }
    }
}
