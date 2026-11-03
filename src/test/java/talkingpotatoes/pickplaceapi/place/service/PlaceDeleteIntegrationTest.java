package talkingpotatoes.pickplaceapi.place.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
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
import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.global.security.SessionUserPrincipal;
import talkingpotatoes.pickplaceapi.global.security.SpringSecurityAuditorAware;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.place.repository.PlaceRepository;
import talkingpotatoes.pickplaceapi.user.domain.UserRole;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;
import talkingpotatoes.pickplaceapi.user.domain.repository.UserRepository;

/**
 * 장소 삭제 실DB 통합 테스트 (H2, MySQL 호환 모드)
 * <pre>
 *  - 실제 PlaceService / FileService 빈과 JPA 리포지토리로 장소 삭제 흐름을 검증한다.
 *  - 커밋/롤백을 실제로 일으키기 위해 테스트 메서드 트랜잭션을 끈다. (NOT_SUPPORTED)
 *  - 데이터소스 설정은 place-delete-h2.properties로 이 클래스에만 한정한다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("장소 삭제 실DB 통합 테스트")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:place-delete-h2.properties")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    PlaceService.class,
    FileService.class,
    FileMetadataService.class,
    LocalFileStorageService.class,
    FileDownloadService.class,
    UserInfoProvider.class,
    FileProp.class,
    PlaceDeleteIntegrationTest.AuditingConfig.class
})
class PlaceDeleteIntegrationTest {

    private static final String WRITER_ID = "writer";
    private static final String OTHER_ID = "other";

    @Autowired
    private PlaceService placeService;
    @Autowired
    private FileService fileService;
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
    private FileProp fileProp;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @TempDir
    Path tempDir;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        fileProp.setFilePath(tempDir.toString());
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
    void 사진있는장소삭제가_커밋되면_사진_파일_장소행과_디스크파일이_모두삭제된다() throws IOException {
        // Given
        SavedPlace saved = savePlaceWithPhotos(2);

        // When
        placeService.deletePlace(saved.plSrl());

        // Then
        assertAll(
                () -> assertTrue(placeRepository.findById(saved.plSrl()).isEmpty()),
                () -> assertEquals(0, photoFileRepository.count()),
                () -> assertEquals(0, fileRepository.count()),
                () -> saved.paths().forEach(path -> assertFalse(Files.exists(path)))
        );
    }

    @Test
    void 장소삭제가_롤백되면_DB행과_디스크파일이_남아있다() throws IOException {
        // Given
        SavedPlace saved = savePlaceWithPhotos(2);

        // When - 같은 트랜잭션에서 삭제 후 롤백
        transactionTemplate.executeWithoutResult(status -> {
            placeService.deletePlace(saved.plSrl());
            status.setRollbackOnly();
        });

        // Then
        assertAll(
                () -> assertTrue(placeRepository.findById(saved.plSrl()).isPresent()),
                () -> assertEquals(2, photoFileRepository.count()),
                () -> assertEquals(2, fileRepository.count()),
                () -> saved.paths().forEach(path -> assertTrue(Files.exists(path)))
        );
    }

    @Test
    void 다른사용자는_장소를삭제할수없고_DB행과_디스크파일이_남아있다() throws IOException {
        // Given
        SavedPlace saved = savePlaceWithPhotos(1);
        loginAs(OTHER_ID);

        // When & Then
        assertThrows(AuthException.class, () -> placeService.deletePlace(saved.plSrl()));
        assertAll(
                () -> assertTrue(placeRepository.findById(saved.plSrl()).isPresent()),
                () -> assertEquals(1, photoFileRepository.count()),
                () -> assertEquals(1, fileRepository.count()),
                () -> assertTrue(Files.exists(saved.paths().get(0)))
        );
    }

    @Test
    void 벌크삭제로_영속성컨텍스트가_비워진뒤에도_준영속_장소삭제가_실제로반영된다() throws IOException {
        // Given
        SavedPlace saved = savePlaceWithPhotos(2);

        // When
        transactionTemplate.executeWithoutResult(status -> {
            Place place = placeRepository.findByIdForUpdate(saved.plSrl()).orElseThrow();
            List<File> loadedFiles = photoFileRepository.findByPlaceWithFile(place).stream()
                    .map(PhotoFile::getFile)
                    .toList();
            assertTrue(entityManager.contains(place));
            loadedFiles.forEach(file -> assertTrue(entityManager.contains(file)));

            fileService.deletePlacePhotos(place); // @Modifying(clearAutomatically = true) 벌크 삭제

            // Then - 벌크 삭제 후 장소·파일 엔티티는 준영속 상태다
            assertFalse(entityManager.contains(place));
            loadedFiles.forEach(file -> assertFalse(entityManager.contains(file)));

            placeRepository.delete(place); // 준영속 장소 삭제 (find + merge 경로)
        });

        // Then - 커밋 후 DB에서 실제로 모두 삭제됐다
        assertAll(
                () -> assertTrue(placeRepository.findById(saved.plSrl()).isEmpty()),
                () -> assertEquals(0, photoFileRepository.count()),
                () -> assertEquals(0, fileRepository.count()),
                () -> saved.paths().forEach(path -> assertFalse(Files.exists(path)))
        );
    }

    @Test
    void 다른트랜잭션이_장소행락을_잡고있으면_삭제와_수정은_대기하다_실패하고_락해제후_삭제된다() throws Exception {
        // Given
        SavedPlace saved = savePlaceWithPhotos(1);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 다른 트랜잭션(예: 사진 추가 중인 updatePlace)이 장소 행 락을 보유
            Future<?> holder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                placeRepository.findByIdForUpdate(saved.plSrl()).orElseThrow();
                lockAcquired.countDown();
                awaitQuietly(release);
            }));
            assertTrue(lockAcquired.await(5, TimeUnit.SECONDS));

            // When & Then - 삭제는 장소 조회(락 획득) 단계에서 대기하다 LOCK_TIMEOUT으로 실패한다
            Statistics statistics = entityManager.getEntityManagerFactory()
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();
            assertThrows(PessimisticLockingFailureException.class, () -> placeService.deletePlace(saved.plSrl()));
            assertEquals(0, executionCountOf(statistics, "pf.place = :place"),
                    "락을 얻기 전에는 사진 조회·삭제로 진행하면 안 된다");

            // 값이 바뀌지 않는 수정 요청도 락 없이 조회하면 UPDATE가 없어 바로 성공하므로, 실패해야 락 조회가 증명된다
            assertThrows(PessimisticLockingFailureException.class, () -> placeService.updatePlace(
                    saved.plSrl(), createRequest("경복궁"), List.of()));
            assertEquals(1, photoFileRepository.count());
            assertTrue(Files.exists(saved.paths().get(0)));

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        // Then - 락이 풀리면 삭제가 정상 수행된다
        placeService.deletePlace(saved.plSrl());
        assertTrue(placeRepository.findById(saved.plSrl()).isEmpty());
        assertEquals(0, photoFileRepository.count());
        assertFalse(Files.exists(saved.paths().get(0)));
    }

    private SavedPlace savePlaceWithPhotos(int photoCount) throws IOException {
        List<Path> paths = new ArrayList<>();
        for (int i = 1; i <= photoCount; i++) {
            Path path = tempDir.resolve("photo-" + i);
            Files.write(path, ("photo-" + i).getBytes(StandardCharsets.UTF_8));
            paths.add(path);
        }

        Long plSrl = transactionTemplate.execute(status -> {
            User writer = userRepository.save(
                    User.createLocalUser(WRITER_ID, "writer@example.com", "dummy-hash", "작성자", LocalDateTime.now())
            );
            Place place = placeRepository.save(new Place(createRequest("경복궁")));
            String uuid = UUID.randomUUID().toString();

            for (int i = 0; i < paths.size(); i++) {
                File file = fileRepository.save(File.builder()
                        .user(writer)
                        .fileNm("photo-" + (i + 1) + ".jpg")
                        .fileSrc(paths.get(i).toString())
                        .fileExtension(".jpg")
                        .uploadedAt(LocalDateTime.now())
                        .fileManageSrl(uuid)
                        .fileSeq((long)(i + 1))
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
        return new SavedPlace(plSrl, paths);
    }

    private PlaceRequest createRequest(String name) {
        return new PlaceRequest(name, "설명", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111, 126.9772222222);
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

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record SavedPlace(Long plSrl, List<Path> paths) {
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
