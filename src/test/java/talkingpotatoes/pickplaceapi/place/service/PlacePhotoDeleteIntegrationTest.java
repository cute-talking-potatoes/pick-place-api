package talkingpotatoes.pickplaceapi.place.service;

import static org.junit.jupiter.api.Assertions.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.ERR_PLACE_PHOTO_NOT_FOUND;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.file.service.FileDownloadService;
import talkingpotatoes.pickplaceapi.file.service.FileMetadataService;
import talkingpotatoes.pickplaceapi.file.service.FileService;
import talkingpotatoes.pickplaceapi.file.service.LocalFileStorageService;
import talkingpotatoes.pickplaceapi.global.exception.AuthException;
import talkingpotatoes.pickplaceapi.global.exception.PlaceException;
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
 * 장소 사진 단건 삭제 실DB 통합 테스트 (H2, MySQL 호환 모드)
 * <pre>
 *  - 실제 PlaceService / FileService 빈과 JPA 리포지토리로 사진 한 장 삭제 흐름을 검증한다.
 *  - 커밋/롤백을 실제로 일으키기 위해 테스트 메서드 트랜잭션을 끈다. (NOT_SUPPORTED)
 *  - 데이터소스 설정은 place-delete-h2.properties를 재사용하되, 다른 테스트와 DB를 공유하지 않도록 URL만 바꾼다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("장소 사진 단건 삭제 실DB 통합 테스트")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
        locations = "classpath:place-delete-h2.properties",
        properties = "spring.datasource.url="
                + "jdbc:h2:mem:place-photo-delete-it;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=500"
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
    PlacePhotoDeleteIntegrationTest.AuditingConfig.class
})
class PlacePhotoDeleteIntegrationTest {

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
    private FileProp fileProp;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @TempDir
    Path tempDir;

    private TransactionTemplate transactionTemplate;
    private User writer;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        fileProp.setFilePath(tempDir.toString());
        loginAs(WRITER_ID);
        writer = transactionTemplate.execute(status -> userRepository.save(
                User.createLocalUser(WRITER_ID, "writer@example.com", "dummy-hash", "작성자", LocalDateTime.now())
        ));
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
    void 사진2장중_1장삭제가_커밋되면_그사진의_행과_디스크파일만_삭제되고_나머지와_장소는_유지된다() throws IOException {
        // Given - 삭제 대상 파일에는 tb_user_file 연결도 있다
        SavedPlace saved = savePlaceWithPhotos("경복궁", 2);
        SavedPhoto target = saved.photos().get(0);
        SavedPhoto remain = saved.photos().get(1);
        linkUserFile(target.fileSrl());

        // When
        placeService.deletePlacePhoto(saved.plSrl(), target.photoFileSrl());

        // Then
        assertAll(
                () -> assertTrue(placeRepository.findById(saved.plSrl()).isPresent()),
                () -> assertTrue(photoFileRepository.findById(target.photoFileSrl()).isEmpty()),
                () -> assertTrue(fileRepository.findById(target.fileSrl()).isEmpty()),
                () -> assertEquals(0, userFileRepository.count()),
                () -> assertFalse(Files.exists(target.path())),
                () -> assertTrue(photoFileRepository.findById(remain.photoFileSrl()).isPresent()),
                () -> assertTrue(fileRepository.findById(remain.fileSrl()).isPresent()),
                () -> assertTrue(Files.exists(remain.path())),
                () -> assertEquals(1, photoFileRepository.count()),
                () -> assertEquals(1, fileRepository.count())
        );
    }

    @Test
    void 사진삭제가_롤백되면_DB행과_디스크파일이_남아있다() throws IOException {
        // Given
        SavedPlace saved = savePlaceWithPhotos("경복궁", 2);
        SavedPhoto target = saved.photos().get(0);
        linkUserFile(target.fileSrl());

        // When - 같은 트랜잭션에서 삭제 후 롤백
        transactionTemplate.executeWithoutResult(status -> {
            placeService.deletePlacePhoto(saved.plSrl(), target.photoFileSrl());
            status.setRollbackOnly();
        });

        // Then
        assertAll(
                () -> assertTrue(photoFileRepository.findById(target.photoFileSrl()).isPresent()),
                () -> assertTrue(fileRepository.findById(target.fileSrl()).isPresent()),
                () -> assertEquals(1, userFileRepository.count()),
                () -> assertEquals(2, photoFileRepository.count()),
                () -> saved.photos().forEach(photo -> assertTrue(Files.exists(photo.path())))
        );
    }

    @Test
    void 다른장소의_사진번호로_요청하면_아무것도_삭제되지않는다() throws IOException {
        // Given - 같은 작성자의 두 장소. 권한 검사는 통과하므로 장소 범위 검사만으로 막아야 한다
        SavedPlace place = savePlaceWithPhotos("경복궁", 1);
        SavedPlace otherPlace = savePlaceWithPhotos("창덕궁", 1);
        SavedPhoto otherPhoto = otherPlace.photos().get(0);

        // When & Then
        PlaceException exception = assertThrows(PlaceException.class,
                () -> placeService.deletePlacePhoto(place.plSrl(), otherPhoto.photoFileSrl()));
        assertEquals(ERR_PLACE_PHOTO_NOT_FOUND.name(), exception.getCode());
        assertAll(
                () -> assertEquals(2, photoFileRepository.count()),
                () -> assertEquals(2, fileRepository.count()),
                () -> assertTrue(photoFileRepository.findById(otherPhoto.photoFileSrl()).isPresent()),
                () -> assertTrue(Files.exists(otherPhoto.path())),
                () -> assertTrue(Files.exists(place.photos().get(0).path()))
        );
    }

    @Test
    void 다른사용자는_장소사진을_삭제할수없고_DB행과_디스크파일이_남아있다() throws IOException {
        // Given
        SavedPlace saved = savePlaceWithPhotos("경복궁", 1);
        SavedPhoto target = saved.photos().get(0);
        loginAs(OTHER_ID);

        // When & Then
        assertThrows(AuthException.class, () -> placeService.deletePlacePhoto(saved.plSrl(), target.photoFileSrl()));
        assertAll(
                () -> assertTrue(photoFileRepository.findById(target.photoFileSrl()).isPresent()),
                () -> assertTrue(fileRepository.findById(target.fileSrl()).isPresent()),
                () -> assertTrue(Files.exists(target.path()))
        );
    }

    private SavedPlace savePlaceWithPhotos(String placeName, int photoCount) throws IOException {
        List<Path> paths = new ArrayList<>();
        for (int i = 1; i <= photoCount; i++) {
            Path path = tempDir.resolve(placeName + "-photo-" + i);
            Files.write(path, ("photo-" + i).getBytes(StandardCharsets.UTF_8));
            paths.add(path);
        }

        return transactionTemplate.execute(status -> {
            Place place = placeRepository.save(new Place(createRequest(placeName)));
            String uuid = UUID.randomUUID().toString();
            List<SavedPhoto> photos = new ArrayList<>();

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
                PhotoFile photoFile = photoFileRepository.save(PhotoFile.builder()
                        .file(file)
                        .user(writer)
                        .place(place)
                        .build());
                photos.add(new SavedPhoto(photoFile.getPhotoFileSrl(), file.getFileSrl(), paths.get(i)));
            }
            return new SavedPlace(place.getPlSrl(), photos);
        });
    }

    private void linkUserFile(Long fileSrl) {
        transactionTemplate.executeWithoutResult(status -> userFileRepository.save(UserFile.builder()
                .file(fileRepository.getReferenceById(fileSrl))
                .user(writer)
                .build()));
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

    private record SavedPlace(Long plSrl, List<SavedPhoto> photos) {
    }

    private record SavedPhoto(Long photoFileSrl, Long fileSrl, Path path) {
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
