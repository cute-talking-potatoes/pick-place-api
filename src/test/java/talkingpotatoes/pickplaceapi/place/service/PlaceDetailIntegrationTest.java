package talkingpotatoes.pickplaceapi.place.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.file.service.FileDownloadService;
import talkingpotatoes.pickplaceapi.file.service.FileMetadataService;
import talkingpotatoes.pickplaceapi.file.service.FileService;
import talkingpotatoes.pickplaceapi.file.service.LocalFileStorageService;
import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
import talkingpotatoes.pickplaceapi.global.security.SessionUserPrincipal;
import talkingpotatoes.pickplaceapi.global.security.SpringSecurityAuditorAware;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.PlaceDetailResponse;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.place.repository.PlaceRepository;
import talkingpotatoes.pickplaceapi.user.domain.UserRole;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;

/**
 * 장소 상세 조회 실DB 통합 테스트 (H2, MySQL 호환 모드)
 * <pre>
 *  - 실제 PlaceService 빈과 JPA 리포지토리로 장소 상세 조회 흐름을 검증한다.
 *  - 서비스의 읽기 전용 트랜잭션만으로 동작하는지 보기 위해 테스트 메서드 트랜잭션을 끈다. (NOT_SUPPORTED)
 *  - 데이터소스 설정은 place-delete-h2.properties를 재사용하되, 다른 테스트와 DB를 공유하지 않도록 URL만 바꾼다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("장소 상세 조회 실DB 통합 테스트")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
        locations = "classpath:place-delete-h2.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:place-detail-it;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=500"
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    PlaceService.class,
    FileService.class,
    FileMetadataService.class,
    LocalFileStorageService.class,
    FileDownloadService.class,
    UserInfoProvider.class,
    FileProp.class,
    PlaceDetailIntegrationTest.AuditingConfig.class
})
class PlaceDetailIntegrationTest {

    private static final String WRITER_ID = "writer";
    private static final String OTHER_ID = "other";

    @Autowired
    private PlaceService placeService;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private PhotoFileRepository photoFileRepository;
    @Autowired
    private UserFileRepository userFileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        loginAs(WRITER_ID);
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            photoFileRepository.deleteAllInBatch();
            userFileRepository.deleteAllInBatch();
            fileRepository.deleteAllInBatch();
            placeRepository.deleteAllInBatch();
            userRepository.deleteAllInBatch();
        });
        SecurityContextHolder.clearContext();
    }

    @Test
    void 사진2장인_장소상세를_조회하면_사진이_오름차순이고_필드가_채워지며_사진조회는_1회다() {
        // Given
        Long plSrl = savePlaceWithPhotos(2);
        List<Long> savedPhotoFileSrls = photoFileRepository.findAll().stream()
                .map(PhotoFile::getPhotoFileSrl)
                .sorted()
                .toList();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        // When
        PlaceDetailResponse result = placeService.findPlaceDetail(plSrl);

        // Then
        assertAll(
                () -> assertEquals(plSrl, result.plSrl()),
                () -> assertEquals("경복궁", result.plNm()),
                () -> assertEquals("설명", result.plDesc()),
                () -> assertEquals("서울 종로구 사직로 161 경복궁", result.plAddr1()),
                () -> assertEquals("광화문", result.plAddr2()),
                () -> assertEquals("03045", result.plCd()),
                () -> assertEquals(37.5786111111, result.plLat()),
                () -> assertEquals(126.9772222222, result.plLng()),
                () -> assertNotNull(result.createdAt()),
                () -> assertNotNull(result.updatedAt()),
                () -> assertTrue(result.isOwner()),
                () -> assertEquals(2, result.photos().size()),
                () -> assertEquals(savedPhotoFileSrls.get(0), result.photos().get(0).photoFileSrl()),
                () -> assertEquals(savedPhotoFileSrls.get(1), result.photos().get(1).photoFileSrl()),
                () -> assertTrue(result.photos().get(0).photoFileSrl() < result.photos().get(1).photoFileSrl()),
                () -> assertEquals("photo-1.jpg", result.photos().get(0).fileName()),
                () -> assertEquals("photo-2.jpg", result.photos().get(1).fileName()),
                () -> assertEquals(".jpg", result.photos().get(0).fileExtension()),
                () -> assertNotNull(result.photos().get(0).fileSrl()),
                () -> assertEquals("/api/place/photos/" + savedPhotoFileSrls.get(0), result.photos().get(0).imageUrl()),
                () -> assertEquals(1, executionCountOf(statistics, "pf.place = :place"), "사진은 JOIN FETCH 1회로 조회한다"),
                () -> assertEquals(0, statistics.getEntityFetchCount(), "지연 로딩 추가 조회(N+1)가 없어야 한다")
        );
    }

    @Test
    void 다른사용자가_장소상세를_조회하면_isOwner가_false다() {
        // Given
        Long plSrl = savePlaceWithPhotos(0);
        loginAs(OTHER_ID);

        // When
        PlaceDetailResponse result = placeService.findPlaceDetail(plSrl);

        // Then
        assertFalse(result.isOwner());
        assertTrue(result.photos().isEmpty());
    }

    @Test
    void 존재하지않는_장소상세를_조회하면_예외가발생한다() {
        // When & Then
        assertThrows(PlaceException.class, () -> placeService.findPlaceDetail(Long.MAX_VALUE));
    }

    private Long savePlaceWithPhotos(int photoCount) {
        Long plSrl = transactionTemplate.execute(status -> {
            User writer = userRepository.save(
                    User.createLocalUser(WRITER_ID, "writer@example.com", "dummy-hash", "작성자", LocalDateTime.now())
            );
            Place place = placeRepository.save(new Place(new PlaceRequest(
                    "경복궁", "설명", "서울 종로구 사직로 161 경복궁", "광화문", "03045", 37.5786111111, 126.9772222222
            )));
            String uuid = UUID.randomUUID().toString();

            for (int i = 1; i <= photoCount; i++) {
                File file = fileRepository.save(File.builder()
                        .user(writer)
                        .fileNm("photo-" + i + ".jpg")
                        .fileSrc("/not-used/photo-" + i)
                        .fileExtension(".jpg")
                        .uploadedAt(LocalDateTime.now())
                        .fileManageSrl(uuid)
                        .fileSeq((long)i)
                        .build());
                photoFileRepository.save(PhotoFile.builder()
                        .file(file)
                        .user(writer)
                        .place(place)
                        .build());
            }
            return place.getPlSrl();
        });

        assertEquals(WRITER_ID, placeRepository.findById(plSrl).orElseThrow().getCreatedBy()); // 감사 필드로 작성자 기록
        return plSrl;
    }

    private void loginAs(String userId) {
        SessionUserPrincipal principal = new SessionUserPrincipal(1L, userId, UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private long executionCountOf(Statistics statistics, String queryFragment) {
        return Arrays.stream(statistics.getQueries())
                .filter(query -> query.contains(queryFragment))
                .mapToLong(query -> statistics.getQueryStatistics(query).getExecutionCount())
                .sum();
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
