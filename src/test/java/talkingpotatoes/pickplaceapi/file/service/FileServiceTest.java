package talkingpotatoes.pickplaceapi.file.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.global.exception.ExceptionCode;
import talkingpotatoes.pickplaceapi.global.exception.FileException;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
import talkingpotatoes.pickplaceapi.place.domain.entity.Place;
import talkingpotatoes.pickplaceapi.place.dto.PlaceRequest;
import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * @author : 박지혁
 * @since : 2026/04/28
 */
@DisplayName("파일서비스 테스트")
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final String CURRENT_USER_ID = "CURRENT_USER";
    private static final String OTHER_USER_ID = "OTHER_USER";

    private static final byte[] PNG_BYTES = {
        (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };
    private static final byte[] JPEG_BYTES = {
        (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };
    private static final byte[] GIF_BYTES = {'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00, 0x01, 0x00, 0x00, 0x00};
    private static final byte[] WEBP_BYTES = {
        'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };
    private static final byte[] TEXT_BYTES = "this is not an image".getBytes(StandardCharsets.UTF_8);

    private FileService fileService;

    @Mock
    private FileProp fileProp;
    @Mock
    private UserInfoProvider userInfoProvider;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private PhotoFileRepository photoFileRepository;
    @Mock
    private UserFileRepository userFileRepository;

    @TempDir
    Path tempDir;

    User user = createMockUser(CURRENT_USER_ID);

    @BeforeEach
    void setUpFileService() {
        LocalFileStorageService localFileStorageService = new LocalFileStorageService(fileProp);
        FileMetadataService fileMetadataService = new FileMetadataService(
                userInfoProvider,
                fileRepository,
                photoFileRepository,
                userFileRepository
        );
        FileDownloadService fileDownloadService = new FileDownloadService(localFileStorageService);

        fileService = new FileService(
                fileProp,
                userInfoProvider,
                fileRepository,
                localFileStorageService,
                fileMetadataService,
                fileDownloadService,
                photoFileRepository
        );
    }

    private File createFileEntity(String path, String name, String uuid, long seq, String userId) {
        return File.builder()
                .user(createMockUser(userId))
                .fileNm(name)
                .fileSrc(path)
                .fileExtension(".jpg")
                .fileManageSrl(uuid)
                .fileSeq(seq)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    private File createFileEntity(String path, String name, String uuid, long seq) {
        return File.builder()
                .fileNm(name)
                .fileSrc(path)
                .fileExtension(".jpg")
                .fileManageSrl(uuid)
                .fileSeq(seq)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    private Place createPlaceEntity() {
        PlaceRequest request = new PlaceRequest("경복궁", "", "서울 종로구 사직로 161 경복궁", "", "03045", 37.5786111111,
                126.9772222222);
        return new Place(request);
    }

    private User createMockUser(String userId) {
        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        return user;
    }

    void setup() {
        given(fileProp.getFilePath()).willReturn(tempDir.toString());
        given(fileProp.getFileTypes()).willReturn(List.of(".jpg", ".png"));
    }

    void setupPhoto() {
        given(fileProp.getFilePath()).willReturn(tempDir.toString());
        given(fileProp.getImageTypes()).willReturn(List.of(".jpg", ".jpeg", ".png", ".gif", ".webp"));
    }

    void setupCurrentUser() {
        given(userInfoProvider.getUser()).willReturn(user);
    }

    void setupCurrentUserId() {
        given(userInfoProvider.getUserId()).willReturn(CURRENT_USER_ID);
    }

    void setupCurrentUser(User user) {
        given(userInfoProvider.getUser()).willReturn(user);
    }

    void setupCurrentUserId(User user) {
        given(userInfoProvider.getUserId()).willReturn(user.getUserId());
    }

    /**
     * =========================
     * upload() - 업로드 로직 테스트
     * =========================
     */

    @Test
    void 새로운UUID로_사진파일업로드에_성공한다() {
        // Given
        setupPhoto();
        setupCurrentUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                JPEG_BYTES
        );

        // When
        executeInTransaction(() -> fileService.uploadPhoto(List.of(file), createPlaceEntity()));

        // Then
        verify(fileRepository).save(argThat(f ->
                f.getUser() != null
                        && f.getFileNm().equals("test.jpg")
                        && f.getFileExtension().equals(".jpg")
                        && f.getFileSeq() == 1
                        && Files.exists(Path.of(f.getFileSrc()))
        ));
        verify(photoFileRepository).save(any(PhotoFile.class));
        verify(userFileRepository, never()).save(any(UserFile.class));
    }

    @Test
    void 새로운UUID로_회원파일업로드에_성공한다() {
        // Given
        setup();
        setupCurrentUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When
        executeInTransaction(() -> fileService.upload(List.of(file)));

        // Then
        verify(fileRepository).save(argThat(f ->
                f.getUser() != null
                        && f.getFileNm().equals("profile.png")
                        && f.getFileExtension().equals(".png")
                        && f.getFileSeq() == 1
                        && Files.exists(Path.of(f.getFileSrc()))
        ));
        verify(userFileRepository).save(any(UserFile.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    @Test
    void 유효하지않은확장자로_파일업로드요청시_파일업로드에_실패한다() throws IOException {
        // Given
        setup();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(FileException.class, () -> executeInTransaction(() -> fileService.upload(List.of(file))));

        assertEquals(0, countEntries(tempDir)); // 검증 실패 시 UUID 디렉토리도 만들지 않는다
        verify(fileRepository, never()).save(any(File.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
        verify(userFileRepository, never()).save(any(UserFile.class));
    }

    @Test
    void 여러파일업로드중_유효하지않은확장자가있으면_이미저장된_물리파일을_삭제한다() throws IOException {
        // Given
        setup(); // 모든 파일 검증이 디스크 쓰기 전에 끝나므로 첫 파일도 저장되지 않는다

        MockMultipartFile successFile = new MockMultipartFile(
                "file",
                "success.jpg",
                "image/jpeg",
                "success".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile failFile = new MockMultipartFile(
                "file",
                "fail.exe",
                "application/octet-stream",
                "fail".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(
                FileException.class,
                () -> executeInTransaction(() -> fileService.upload(List.of(successFile, failFile)))
        );

        assertEquals(0, countEntries(tempDir));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void 메타데이터저장에_실패하면_이미저장된_물리파일을_삭제한다() throws IOException {
        // Given
        setup();
        setupCurrentUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        given(fileRepository.save(any(File.class))).willThrow(new RuntimeException("DB 저장 실패"));

        // When & Then
        assertThrows(RuntimeException.class, () -> executeInTransaction(() -> fileService.upload(List.of(file))));

        assertEquals(0, countRegularFiles(tempDir));
        assertEquals(0, countEntries(tempDir)); // 롤백 시 빈 UUID 디렉토리도 정리된다
        verify(userFileRepository, never()).save(any(UserFile.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    /**
     * =========================
     * update() - 수정 / 추가 업로드 로직 테스트
     * =========================
     */

    @Test
    void 이미존재하는UUID로_파일업로드를시도하면_시퀀스가증가하고_성공한다() {
        // Given
        setup();
        setupCurrentUserId();
        setupCurrentUser();

        String uuid = UUID.randomUUID().toString();

        File existing = createFileEntity("dummy", "old.jpg", uuid, 5, CURRENT_USER_ID);

        given(fileRepository.findByUUID(uuid)).willReturn(List.of(existing));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When
        executeInTransaction(() -> fileService.update(List.of(file), uuid));

        // Then
        verify(fileRepository).save(argThat(f ->
                f.getFileSeq() == 6
                        && f.getFileManageSrl().equals(uuid)
                        && f.getFileNm().equals("test.jpg")
                        && Files.exists(Path.of(f.getFileSrc()))
        ));
        verify(userFileRepository).save(any(UserFile.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    @Test
    void 잘못된UUID로_파일수정요청시_파일수정에_실패한다() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertAll(
                () -> assertThrows(FileException.class, () -> fileService.update(List.of(file), null)),
                () -> assertThrows(FileException.class, () -> fileService.update(List.of(file), " ")),
                () -> assertThrows(FileException.class,
                        () -> fileService.update(List.of(file), "invalid-uuid"))
        );

        verify(fileRepository, never()).findByUUID(any());
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void 존재하지않는UUID로_파일수정요청시_파일수정에_실패한다() {
        // Given
        String uuid = UUID.randomUUID().toString();

        given(fileRepository.findByUUID(uuid)).willReturn(List.of());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(FileException.class, () -> fileService.update(List.of(file), uuid));

        verify(fileRepository, never()).save(any(File.class));
        verify(userFileRepository, never()).save(any(UserFile.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    @Test
    void 다른사용자의UUID로_파일수정요청시_파일수정에_실패한다() {
        // Given
        setupCurrentUserId();

        String uuid = UUID.randomUUID().toString();

        File otherUserFile = createFileEntity("dummy", "old.jpg", uuid, 5, OTHER_USER_ID);

        given(fileRepository.findByUUID(uuid)).willReturn(List.of(otherUserFile));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(FileException.class, () -> fileService.update(List.of(file), uuid));

        verify(fileRepository, never()).save(any(File.class));
        verify(userFileRepository, never()).save(any(UserFile.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    /**
     * =========================
     * delete() - 삭제 로직 테스트
     * =========================
     */

    @Test
    void 파일삭제에_성공한다() throws IOException {
        // Given
        setupCurrentUserId();

        String uuid = UUID.randomUUID().toString();

        Path filePath = tempDir.resolve("test.jpg");
        Files.write(filePath, "data".getBytes(StandardCharsets.UTF_8));

        File file = createFileEntity(filePath.toString(), "test.jpg", uuid, 1, CURRENT_USER_ID);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).willReturn(List.of(file));

        // When
        fileService.delete(uuid, List.of(1L));

        // Then
        assertFalse(Files.exists(filePath));

        verify(userFileRepository).deleteByFileSrl(file.getFileSrl());
        verify(photoFileRepository).deleteByFileSrl(file.getFileSrl());
        verify(fileRepository).delete(file);
    }

    @Test
    void 존재하지않는파일삭제요청시_에러가발생하지않고성공한다() {
        // Given
        given(fileRepository.findByUUIDAndFileSeq(any(), any())).willReturn(List.of());

        // When
        fileService.delete("uuid", List.of(1L));

        // Then
        verify(fileRepository, never()).delete(any(File.class));
        verify(userFileRepository, never()).deleteByFileSrl(any());
        verify(photoFileRepository, never()).deleteByFileSrl(any());
    }

    @Test
    void 다른사용자의파일삭제요청시_파일삭제에_실패한다() throws IOException {
        // Given
        setupCurrentUserId();

        String uuid = UUID.randomUUID().toString();

        Path filePath = tempDir.resolve("test.jpg");
        Files.write(filePath, "data".getBytes(StandardCharsets.UTF_8));

        File file = createFileEntity(filePath.toString(), "test.jpg", uuid, 1, OTHER_USER_ID);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).willReturn(List.of(file));

        // When & Then
        assertThrows(FileException.class, () -> fileService.delete(uuid, List.of(1L)));

        assertTrue(Files.exists(filePath));

        verify(fileRepository, never()).delete(any(File.class));
        verify(userFileRepository, never()).deleteByFileSrl(any());
        verify(photoFileRepository, never()).deleteByFileSrl(any());
    }

    @Test
    void DB에는있지만_물리파일이없어도_파일삭제에_성공한다() {
        // Given
        setupCurrentUserId();

        String uuid = UUID.randomUUID().toString();

        Path notExistsPath = tempDir.resolve("not-exists.jpg");

        File file = createFileEntity(notExistsPath.toString(), "not-exists.jpg", uuid, 1, CURRENT_USER_ID);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).willReturn(List.of(file));

        // When
        fileService.delete(uuid, List.of(1L));

        // Then
        verify(userFileRepository).deleteByFileSrl(file.getFileSrl());
        verify(photoFileRepository).deleteByFileSrl(file.getFileSrl());
        verify(fileRepository).delete(file);
    }

    /**
     * =========================
     * download() - 다운로드 로직 테스트
     * =========================
     */

    @Test
    void 단건파일다운로드에_성공한다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();

        Path filePath = tempDir.resolve("test.jpg");
        Files.write(filePath, "data".getBytes(StandardCharsets.UTF_8));

        File file = createFileEntity(filePath.toString(), "test.jpg", uuid, 1);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).willReturn(List.of(file));

        HttpHeaders headers = new HttpHeaders();

        // When
        Resource resource = fileService.download(uuid, List.of(1L), headers);

        // Then
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("application/octet-stream", headers.getFirst(HttpHeaders.CONTENT_TYPE));
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("test.jpg"));
        assertEquals(String.valueOf(Files.size(filePath)), headers.getFirst(HttpHeaders.CONTENT_LENGTH));
    }

    @Test
    void 여러파일들을_zip형태다운로드에_성공한다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();

        Path file1 = tempDir.resolve("a.jpg");
        Path file2 = tempDir.resolve("b.jpg");

        Files.write(file1, "data1".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "data2".getBytes(StandardCharsets.UTF_8));

        File f1 = createFileEntity(file1.toString(), "a.jpg", uuid, 1);
        File f2 = createFileEntity(file2.toString(), "b.jpg", uuid, 2);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L, 2L))).willReturn(List.of(f1, f2));

        HttpHeaders headers = new HttpHeaders();

        // When
        Resource resource = fileService.download(uuid, List.of(1L, 2L), headers);

        // Then
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("application/zip", headers.getFirst(HttpHeaders.CONTENT_TYPE));
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).endsWith(".zip\""));
        assertEquals(String.valueOf(resource.contentLength()), headers.getFirst(HttpHeaders.CONTENT_LENGTH));

        Path tempZipPath = resource.getFile().toPath();
        assertTrue(Files.exists(tempZipPath));

        Map<String, String> zipContents = readZipContents(resource);

        assertEquals("data1", zipContents.get("a.jpg"));
        assertEquals("data2", zipContents.get("b.jpg"));
        assertFalse(Files.exists(tempZipPath));
    }

    @Test
    void ZIP다운로드시_중복파일명은_순번이붙는다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();

        Path file1 = tempDir.resolve("file1.jpg");
        Path file2 = tempDir.resolve("file2.jpg");

        Files.write(file1, "data1".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "data2".getBytes(StandardCharsets.UTF_8));

        File f1 = createFileEntity(file1.toString(), "same.jpg", uuid, 1);
        File f2 = createFileEntity(file2.toString(), "same.jpg", uuid, 2);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L, 2L))).willReturn(List.of(f1, f2));

        HttpHeaders headers = new HttpHeaders();

        // When
        Resource resource = fileService.download(uuid, List.of(1L, 2L), headers);

        // Then
        Map<String, String> zipContents = readZipContents(resource);

        assertEquals("data1", zipContents.get("same.jpg"));
        assertEquals("data2", zipContents.get("same(1).jpg"));
    }

    @Test
    void ZIP파일생성중_실패하면_임시파일을_삭제한다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();

        Path filePath = tempDir.resolve("a.jpg");
        Path missingPath = tempDir.resolve("missing.jpg");

        Files.write(filePath, "data".getBytes(StandardCharsets.UTF_8));

        File f1 = createFileEntity(filePath.toString(), "a.jpg", uuid, 1);
        File f2 = createFileEntity(missingPath.toString(), "missing.jpg", uuid, 2);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L, 2L))).willReturn(List.of(f1, f2));

        HttpHeaders headers = new HttpHeaders();

        // When & Then
        assertThrows(FileException.class, () -> fileService.download(uuid, List.of(1L, 2L), headers));
        assertTrue(findTempZipFiles(uuid).isEmpty());
    }

    @Test
    void 파일이존재하지않으면_파일다운로드에_실패한다() {
        // Given
        given(fileRepository.findByUUIDAndFileSeq(any(), any())).willReturn(List.of());

        HttpHeaders headers = new HttpHeaders();

        // When & Then
        assertThrows(FileException.class, () -> fileService.download("uuid", List.of(1L), headers));
    }

    @Test
    void 요청한파일개수와_조회된파일개수가_다르면_파일다운로드에_실패한다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();

        Path filePath = tempDir.resolve("test.jpg");
        Files.write(filePath, "data".getBytes(StandardCharsets.UTF_8));

        File file = createFileEntity(filePath.toString(), "test.jpg", uuid, 1);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L, 2L))).willReturn(List.of(file));

        HttpHeaders headers = new HttpHeaders();

        // When & Then
        assertThrows(FileException.class, () -> fileService.download(uuid, List.of(1L, 2L), headers));
    }

    @Test
    void DB에는있지만_물리파일이없으면_파일다운로드에_실패한다() {
        // Given
        String uuid = UUID.randomUUID().toString();

        Path notExistsPath = tempDir.resolve("not-exists.jpg");

        File file = createFileEntity(notExistsPath.toString(), "test.jpg", uuid, 1);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).willReturn(List.of(file));

        HttpHeaders headers = new HttpHeaders();

        // When & Then
        assertThrows(FileException.class, () -> fileService.download(uuid, List.of(1L), headers));
    }

    @Test
    void DB의파일경로가_디렉터리이면_파일다운로드에_실패한다() {
        // Given
        String uuid = UUID.randomUUID().toString();

        File file = createFileEntity(tempDir.toString(), "test.jpg", uuid, 1);

        given(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).willReturn(List.of(file));

        HttpHeaders headers = new HttpHeaders();

        // When & Then
        assertThrows(FileException.class, () -> fileService.download(uuid, List.of(1L), headers));
    }

    /**
     * =========================
     * uploadPhoto() / updatePhoto() - 장소 사진 이미지 검증 테스트
     * =========================
     */

    static Stream<Arguments> invalidPhotoFiles() {
        return Stream.of(
                Arguments.of("텍스트 확장자", "memo.txt", TEXT_BYTES, ERR_FILE_EXTENSION_NOT_MATCH),
                Arguments.of("PDF 확장자", "doc.pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII),
                        ERR_FILE_EXTENSION_NOT_MATCH),
                Arguments.of("확장자 없음", "noextension", PNG_BYTES, ERR_FILE_EXTENSION_NOT_MATCH),
                Arguments.of("png 확장자에 텍스트 내용", "fake.png", TEXT_BYTES, ERR_FILE_INVALID_IMAGE),
                Arguments.of("빈 png 파일", "empty.png", new byte[0], ERR_FILE_INVALID_IMAGE),
                Arguments.of("시그니처보다 짧은 파일", "short.png", new byte[] {(byte)0x89, 0x50}, ERR_FILE_INVALID_IMAGE),
                Arguments.of("png 확장자에 jpeg 내용", "mismatch.png", JPEG_BYTES, ERR_FILE_INVALID_IMAGE),
                Arguments.of("jpg 확장자에 png 내용", "mismatch.jpg", PNG_BYTES, ERR_FILE_INVALID_IMAGE)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPhotoFiles")
    void 이미지가아닌파일로_장소사진업로드시_거부된다(String caseName, String filename, byte[] content,
            ExceptionCode expectedCode) throws IOException {
        // Given
        setupPhoto();
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", content);

        // When
        FileException exception = assertThrows(FileException.class,
                () -> executeInTransaction(() -> fileService.uploadPhoto(List.of(file), createPlaceEntity())));

        // Then
        assertEquals(expectedCode.name(), exception.getCode());
        assertEquals(0, countEntries(tempDir)); // 파일도, UUID 디렉토리도 남지 않는다
        verify(fileRepository, never()).save(any(File.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    static Stream<Arguments> validPhotoFiles() {
        return Stream.of(
                Arguments.of("photo.png", PNG_BYTES),
                Arguments.of("photo.jpg", JPEG_BYTES),
                Arguments.of("photo.jpeg", JPEG_BYTES),
                Arguments.of("PHOTO.PNG", PNG_BYTES),
                Arguments.of("Photo.JpG", JPEG_BYTES),
                Arguments.of("photo.gif", GIF_BYTES),
                Arguments.of("photo.webp", WEBP_BYTES)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPhotoFiles")
    void 정상이미지파일은_장소사진업로드에_성공한다(String filename, byte[] content) {
        // Given
        setupPhoto();
        setupCurrentUser();
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", content);

        // When
        executeInTransaction(() -> fileService.uploadPhoto(List.of(file), createPlaceEntity()));

        // Then
        verify(fileRepository).save(argThat(f ->
                f.getFileNm().equals(filename) && Files.exists(Path.of(f.getFileSrc()))
        ));
        verify(photoFileRepository).save(any(PhotoFile.class));
    }

    @Test
    void 이미지화이트리스트가_설정되지않으면_장소사진업로드를_거부한다() {
        // Given
        given(fileProp.getFilePath()).willReturn(tempDir.toString());
        given(fileProp.getImageTypes()).willReturn(null);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        // When & Then
        FileException exception = assertThrows(FileException.class,
                () -> executeInTransaction(() -> fileService.uploadPhoto(List.of(file), createPlaceEntity())));
        assertEquals(ERR_FILE_EXTENSION_NOT_MATCH.name(), exception.getCode());
    }

    @Test
    void 장소사진추가업로드도_이미지가아니면_거부된다() throws IOException {
        // Given
        setupPhoto();
        setupCurrentUserId();
        String uuid = UUID.randomUUID().toString();
        File existing = createFileEntity("dummy", "old.jpg", uuid, 1, CURRENT_USER_ID);
        given(fileRepository.findByUUID(uuid)).willReturn(List.of(existing));
        MockMultipartFile file = new MockMultipartFile("file", "memo.txt", "text/plain", TEXT_BYTES);

        // When & Then
        assertThrows(FileException.class,
                () -> executeInTransaction(() -> fileService.updatePhoto(List.of(file), uuid, createPlaceEntity())));
        assertEquals(0, countRegularFiles(tempDir));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void 일반파일업로드는_이미지검증없이_기존화이트리스트를_따른다() {
        // Given - 일반 업로드는 file.fileTypes만 확인하고 내용은 검사하지 않는다
        given(fileProp.getFilePath()).willReturn(tempDir.toString());
        given(fileProp.getFileTypes()).willReturn(List.of(".txt"));
        setupCurrentUser();
        MockMultipartFile file = new MockMultipartFile("file", "memo.txt", "text/plain", TEXT_BYTES);

        // When
        executeInTransaction(() -> fileService.upload(List.of(file)));

        // Then
        verify(fileRepository).save(argThat(f -> f.getFileNm().equals("memo.txt")));
        verify(userFileRepository).save(any(UserFile.class));
        verify(fileProp, never()).getImageTypes();
    }

    @Test
    void 여러사진업로드중_두번째가이미지가아니면_첫번째파일도_디렉토리도_남지않는다() throws IOException {
        // Given
        setupPhoto();
        MockMultipartFile valid = new MockMultipartFile("file", "ok.png", "image/png", PNG_BYTES);
        MockMultipartFile invalid = new MockMultipartFile("file", "fake.png", "image/png", TEXT_BYTES);

        // When & Then
        assertThrows(FileException.class, () -> executeInTransaction(
                () -> fileService.uploadPhoto(List.of(valid, invalid), createPlaceEntity())));
        assertEquals(0, countEntries(tempDir));
        verify(fileRepository, never()).save(any(File.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
    }

    @Test
    void 여러사진업로드중_두번째메타데이터저장에_실패하면_첫번째물리파일과_디렉토리가_정리된다() throws IOException {
        // Given
        setupPhoto();
        setupCurrentUser();
        MockMultipartFile first = new MockMultipartFile("file", "first.png", "image/png", PNG_BYTES);
        MockMultipartFile second = new MockMultipartFile("file", "second.jpg", "image/jpeg", JPEG_BYTES);
        given(fileRepository.save(any(File.class)))
                .willAnswer(invocation -> invocation.getArgument(0))
                .willThrow(new RuntimeException("DB 저장 실패"));

        // When & Then
        assertThrows(RuntimeException.class, () -> executeInTransaction(
                () -> fileService.uploadPhoto(List.of(first, second), createPlaceEntity())));
        verify(fileRepository, times(2)).save(any(File.class)); // 첫 파일은 실제로 저장된 뒤 롤백됐다
        assertEquals(0, countEntries(tempDir));
    }

    @Test
    void 기존디렉토리에_추가업로드가_롤백되면_기존파일과_디렉토리는_유지된다() throws IOException {
        // Given
        setupPhoto();
        setupCurrentUserId();
        setupCurrentUser();
        String uuid = UUID.randomUUID().toString();
        Path existingDir = Files.createDirectories(tempDir.resolve(uuid));
        Path existingFile = Files.write(existingDir.resolve(uuid + "_1"), PNG_BYTES);
        File existing = createFileEntity(existingFile.toString(), "old.png", uuid, 1, CURRENT_USER_ID);
        given(fileRepository.findByUUID(uuid)).willReturn(List.of(existing));
        given(fileRepository.save(any(File.class))).willThrow(new RuntimeException("DB 저장 실패"));
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", PNG_BYTES);

        // When & Then
        assertThrows(RuntimeException.class, () -> executeInTransaction(
                () -> fileService.updatePhoto(List.of(file), uuid, createPlaceEntity())));
        assertTrue(Files.exists(existingFile));
        assertFalse(Files.exists(existingDir.resolve(uuid + "_2")));
    }

    @Test
    void 트랜잭션밖에서_업로드하면_검증전에_실패하고_디렉토리를_만들지않는다() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "ok.png", "image/png", PNG_BYTES);

        // When & Then
        FileException exception = assertThrows(FileException.class,
                () -> fileService.uploadPhoto(List.of(file), createPlaceEntity()));
        assertEquals(ERR_FILE_SAVE_INCORRECT_TRANSACTION.name(), exception.getCode());
        assertEquals(0, countEntries(tempDir));
    }

    /**
     * =========================
     * deletePlacePhotos() - 장소 사진 정리 테스트
     * =========================
     */

    @Test
    void 장소사진삭제시_DB행은_즉시삭제되고_물리파일은_커밋후에_삭제된다() throws IOException {
        // Given
        Place place = createPlaceEntity();
        Path photo1 = writeFile("photo1");
        Path photo2 = writeFile("photo2");
        File file1 = createPhotoFileEntity(1L, photo1, "photo1.jpg");
        File file2 = createPhotoFileEntity(2L, photo2, "photo2.jpg");
        given(photoFileRepository.findByPlaceWithFile(place))
                .willReturn(List.of(createPhotoFile(file1, place), createPhotoFile(file2, place)));

        // When
        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePlacePhotos(place);

            // Then - 커밋 전에는 물리 파일이 남아있다
            assertTrue(Files.exists(photo1));
            assertTrue(Files.exists(photo2));
            verify(userFileRepository).deleteAllByFileSrlIn(List.of(1L, 2L));
            verify(photoFileRepository).deleteAllByFileSrlIn(List.of(1L, 2L));
            verify(fileRepository).deleteAllByFileSrlIn(List.of(1L, 2L)); // 파일당 개별 삭제가 아닌 일괄 삭제
            verify(fileRepository, never()).delete(any(File.class));
        } finally {
            completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        }

        // Then - 커밋 후에는 물리 파일이 삭제된다
        assertFalse(Files.exists(photo1));
        assertFalse(Files.exists(photo2));
    }

    @Test
    void 장소사진삭제후_롤백되면_물리파일은_남아있다() throws IOException {
        // Given
        Place place = createPlaceEntity();
        Path photo = writeFile("photo");
        File file = createPhotoFileEntity(1L, photo, "photo.jpg");
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of(createPhotoFile(file, place)));

        // When
        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePlacePhotos(place);
        } finally {
            completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        // Then
        assertTrue(Files.exists(photo));
    }

    @Test
    void 커밋후_물리파일삭제에_실패해도_예외없이_나머지파일은_삭제된다() throws IOException {
        // Given - 비어있지 않은 디렉토리는 삭제에 실패한다
        Place place = createPlaceEntity();
        Path undeletable = tempDir.resolve("undeletable");
        Files.createDirectories(undeletable);
        Files.write(undeletable.resolve("child"), "x".getBytes(StandardCharsets.UTF_8));
        Path photo = writeFile("photo");
        File file1 = createPhotoFileEntity(1L, undeletable, "broken.jpg");
        File file2 = createPhotoFileEntity(2L, photo, "photo.jpg");
        given(photoFileRepository.findByPlaceWithFile(place))
                .willReturn(List.of(createPhotoFile(file1, place), createPhotoFile(file2, place)));

        // When & Then
        assertDoesNotThrow(() -> executeInTransaction(() -> fileService.deletePlacePhotos(place)));
        assertTrue(Files.exists(undeletable));
        assertFalse(Files.exists(photo));
    }

    @Test
    void 사진이없는장소는_아무것도삭제하지않는다() {
        // Given
        Place place = createPlaceEntity();
        given(photoFileRepository.findByPlaceWithFile(place)).willReturn(List.of());

        // When
        executeInTransaction(() -> fileService.deletePlacePhotos(place));

        // Then
        verify(photoFileRepository, never()).deleteAllByFileSrlIn(any());
        verify(fileRepository, never()).deleteAllByFileSrlIn(any());
    }

    @Test
    void 트랜잭션밖에서_장소사진삭제를_호출하면_아무것도삭제하지않고_실패한다() throws IOException {
        // Given
        Place place = createPlaceEntity();
        Path photo = writeFile("photo");

        // When & Then
        FileException exception = assertThrows(FileException.class, () -> fileService.deletePlacePhotos(place));
        assertEquals(ERR_FILE_DELETE_INCORRECT_TRANSACTION.name(), exception.getCode());
        verify(photoFileRepository, never()).findByPlaceWithFile(any());
        assertTrue(Files.exists(photo));
        verify(photoFileRepository, never()).deleteAllByFileSrlIn(any());
        verify(fileRepository, never()).deleteAllByFileSrlIn(any());
    }

    /**
     * =========================
     * deletePlacePhoto() - 장소 사진 단건 삭제 테스트
     * =========================
     */

    @Test
    void 장소사진단건삭제시_해당파일의_DB행만_즉시삭제되고_물리파일은_커밋후에_삭제된다() throws IOException {
        // Given
        Place place = createPlaceEntity();
        Path target = writeFile("target");
        Path remain = writeFile("remain");
        PhotoFile photoFile = createPhotoFile(createPhotoFileEntity(1L, target, "target.jpg"), place);

        // When
        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePlacePhoto(photoFile);

            // Then - 커밋 전에는 물리 파일이 남아있고, 대상 파일 번호로만 행을 지운다
            assertTrue(Files.exists(target));
            verify(userFileRepository).deleteAllByFileSrlIn(List.of(1L));
            verify(photoFileRepository).deleteAllByFileSrlIn(List.of(1L));
            verify(fileRepository).deleteAllByFileSrlIn(List.of(1L));
            verify(photoFileRepository, never()).findByPlaceWithFile(any()); // 장소의 다른 사진은 조회하지 않는다
        } finally {
            completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        }

        // Then - 커밋 후에는 대상 물리 파일만 삭제된다
        assertFalse(Files.exists(target));
        assertTrue(Files.exists(remain));
    }

    @Test
    void 장소사진단건삭제후_롤백되면_물리파일은_남아있다() throws IOException {
        // Given
        Path photo = writeFile("photo");
        PhotoFile photoFile = createPhotoFile(createPhotoFileEntity(1L, photo, "photo.jpg"), createPlaceEntity());

        // When
        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deletePlacePhoto(photoFile);
        } finally {
            completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        // Then
        assertTrue(Files.exists(photo));
    }

    @Test
    void 장소사진단건삭제후_커밋후_물리파일삭제에_실패해도_예외가발생하지않는다() throws IOException {
        // Given - 비어있지 않은 디렉토리는 삭제에 실패한다
        Path undeletable = tempDir.resolve("undeletable");
        Files.createDirectories(undeletable);
        Files.write(undeletable.resolve("child"), "x".getBytes(StandardCharsets.UTF_8));
        PhotoFile photoFile = createPhotoFile(createPhotoFileEntity(1L, undeletable, "broken.jpg"),
                createPlaceEntity());

        // When & Then
        assertDoesNotThrow(() -> executeInTransaction(() -> fileService.deletePlacePhoto(photoFile)));
        assertTrue(Files.exists(undeletable));
    }

    @Test
    void 트랜잭션밖에서_장소사진단건삭제를_호출하면_아무것도삭제하지않고_실패한다() throws IOException {
        // Given
        Path photo = writeFile("photo");
        PhotoFile photoFile = createPhotoFile(createPhotoFileEntity(1L, photo, "photo.jpg"), createPlaceEntity());

        // When & Then
        FileException exception = assertThrows(FileException.class, () -> fileService.deletePlacePhoto(photoFile));
        assertEquals(ERR_FILE_DELETE_INCORRECT_TRANSACTION.name(), exception.getCode());
        assertTrue(Files.exists(photo));
        verify(userFileRepository, never()).deleteAllByFileSrlIn(any());
        verify(photoFileRepository, never()).deleteAllByFileSrlIn(any());
        verify(fileRepository, never()).deleteAllByFileSrlIn(any());
    }

    private Path writeFile(String name) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, name.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private File createPhotoFileEntity(Long fileSrl, Path path, String name) {
        return File.builder()
                .fileSrl(fileSrl)
                .fileNm(name)
                .fileSrc(path.toString())
                .fileExtension(".jpg")
                .fileManageSrl("uuid")
                .fileSeq(fileSrl)
                .build();
    }

    private PhotoFile createPhotoFile(File file, Place place) {
        return PhotoFile.builder()
                .file(file)
                .place(place)
                .build();
    }

    private long countEntries(Path rootPath) throws IOException {
        try (var paths = Files.list(rootPath)) {
            return paths.count();
        }
    }

    private long countRegularFiles(Path rootPath) throws IOException {
        try (var paths = Files.walk(rootPath)) {
            return paths.filter(Files::isRegularFile)
                    .count();
        }
    }

    private void executeInTransaction(Runnable action) {
        TransactionSynchronizationManager.initSynchronization();
        int completionStatus = TransactionSynchronization.STATUS_COMMITTED;

        try {
            action.run();
        } catch (RuntimeException | Error e) {
            completionStatus = TransactionSynchronization.STATUS_ROLLED_BACK;
            throw e;
        } finally {
            completeTransaction(completionStatus);
        }
    }

    private void completeTransaction(int completionStatus) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        try {
            if (completionStatus == TransactionSynchronization.STATUS_COMMITTED) { // 실제 트랜잭션처럼 afterCommit 먼저 호출
                synchronizations.forEach(TransactionSynchronization::afterCommit);
            }
            synchronizations.forEach(synchronization -> synchronization.afterCompletion(completionStatus));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Map<String, String> readZipContents(Resource resource) throws IOException {
        Map<String, String> result = new java.util.HashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(resource.getInputStream())) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                String content = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                result.put(entry.getName(), content);
                zipInputStream.closeEntry();
            }
        }

        return result;
    }

    private List<String> getZipEntryNames(Resource resource) throws IOException {
        List<String> names = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(resource.getInputStream())) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                names.add(entry.getName());
                zipInputStream.closeEntry();
            }
        }

        return names;
    }

    private List<Path> findTempZipFiles(String uuid) throws IOException {
        Path tempPath = Path.of(System.getProperty("java.io.tmpdir"));

        try (var paths = Files.list(tempPath)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.startsWith(uuid) && filename.endsWith(".zip");
                    })
                    .toList();
        }
    }
}
