package talkingpotatoes.pickplaceapi.place.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.place.repository.PlaceRepository;

/**
 * 장소 삭제 시 사진·파일 정리를 실제 FileService와 연결해 검증한다.
 * <pre>
 *  - 리포지토리는 mock, 디스크는 임시 디렉토리를 사용한다.
 *  - 트랜잭션 동기화는 TransactionSynchronizationManager로 직접 커밋/롤백 시점을 흉내낸다.
 * </pre>
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("장소 삭제 사진 정리 테스트")
@ExtendWith(MockitoExtension.class)
class PlaceDeletePhotoCleanupTest {

    private static final String WRITER_ID = "writer";

    @Mock
    private FileProp fileProp;
    @Mock
    private UserInfoProvider userInfoProvider;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private PhotoFileRepository photoFileRepository;
    @Mock
    private UserFileRepository userFileRepository;

    @TempDir
    Path tempDir;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        LocalFileStorageService localFileStorageService = new LocalFileStorageService(fileProp);
        FileMetadataService fileMetadataService = new FileMetadataService(
                userInfoProvider, fileRepository, photoFileRepository, userFileRepository
        );
        FileDownloadService fileDownloadService = new FileDownloadService(localFileStorageService);
        FileService fileService = new FileService(
                fileProp, userInfoProvider, fileRepository, localFileStorageService,
                fileMetadataService, fileDownloadService, photoFileRepository
        );
        placeService = new PlaceService(
                userInfoProvider, placeRepository, photoFileRepository, fileService, fileDownloadService
        );
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 장소삭제가_커밋되면_사진행과_파일행이_삭제되고_물리파일도_삭제된다() throws IOException {
        // Given
        Place place = createPlace();
        Path photoPath = writeFile("photo1");
        File file = createFile(10L, photoPath);
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of(createPhotoFile(file, place)));

        // When
        TransactionSynchronizationManager.initSynchronization();
        placeService.deletePlace(1L);

        // Then - 커밋 전: DB 행 삭제는 장소 삭제보다 먼저, 물리 파일은 아직 남아있다
        InOrder inOrder = inOrder(userInfoProvider, photoFileRepository, fileRepository, placeRepository);
        inOrder.verify(userInfoProvider).checkUserAuthorization(WRITER_ID);
        inOrder.verify(photoFileRepository).deleteAllByFileSrlIn(List.of(10L));
        inOrder.verify(fileRepository).deleteAllByFileSrlIn(List.of(10L));
        inOrder.verify(placeRepository).delete(place);
        assertTrue(Files.exists(photoPath));

        // Then - 커밋 후: 물리 파일 삭제
        complete(TransactionSynchronization.STATUS_COMMITTED);
        assertFalse(Files.exists(photoPath));
    }

    @Test
    void 장소삭제가_롤백되면_물리파일은_남아있다() throws IOException {
        // Given - 장소 삭제 단계에서 DB 오류가 발생한다
        Place place = createPlace();
        Path photoPath = writeFile("photo1");
        File file = createFile(10L, photoPath);
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of(createPhotoFile(file, place)));
        willThrow(new RuntimeException("DB 오류")).given(placeRepository).delete(place);

        // When
        TransactionSynchronizationManager.initSynchronization();
        assertThrows(RuntimeException.class, () -> placeService.deletePlace(1L));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        // Then
        assertTrue(Files.exists(photoPath));
    }

    @Test
    void 작성자가아닌사용자는_장소를삭제할수없고_사진과파일도_그대로다() throws IOException {
        // Given
        Place place = createPlace();
        Path photoPath = writeFile("photo1");
        given(placeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(place));
        willThrow(new AuthException(ERR_AUTH_ACCESS_DENIED))
                .given(userInfoProvider)
                .checkUserAuthorization(WRITER_ID);

        // When
        TransactionSynchronizationManager.initSynchronization();
        assertThrows(AuthException.class, () -> placeService.deletePlace(1L));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        // Then
        assertTrue(Files.exists(photoPath));
        then(photoFileRepository).should(never()).findByPlaceWithFile(any());
        then(photoFileRepository).should(never()).deleteAllByFileSrlIn(any());
        then(fileRepository).should(never()).deleteAllByFileSrlIn(any());
        then(placeRepository).should(never()).delete(any(Place.class));
    }

    private void complete(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        try {
            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                synchronizations.forEach(TransactionSynchronization::afterCommit);
            }
            synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Place createPlace() {
        Place place = new Place(
                new PlaceRequest("경복궁", "설명", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111, 126.9772222222)
        );
        ReflectionTestUtils.setField(place, "plSrl", 1L);
        ReflectionTestUtils.setField(place, "createdBy", WRITER_ID);
        return place;
    }

    private Path writeFile(String name) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, name.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private File createFile(Long fileSrl, Path path) {
        return File.builder()
                .fileSrl(fileSrl)
                .fileNm(path.getFileName().toString())
                .fileSrc(path.toString())
                .fileExtension(".jpg")
                .fileManageSrl("uuid")
                .fileSeq(1L)
                .build();
    }

    private PhotoFile createPhotoFile(File file, Place place) {
        return PhotoFile.builder()
                .file(file)
                .place(place)
                .build();
    }
}
