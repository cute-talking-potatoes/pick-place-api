package talkingpotatoes.pickplaceapi.file.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.entity.PhotoFile;
import talkingpotatoes.pickplaceapi.file.domain.entity.UserFile;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.file.repository.FileRepository;
import talkingpotatoes.pickplaceapi.file.repository.PhotoFileRepository;
import talkingpotatoes.pickplaceapi.file.repository.UserFileRepository;
import talkingpotatoes.pickplaceapi.global.exception.FileException;
import talkingpotatoes.pickplaceapi.global.security.UserInfoProvider;
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

    @InjectMocks
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

    private User createMockUser(String userId) {
        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        return user;
    }

    void setup() {
        given(fileProp.getFilePath()).willReturn(tempDir.toString());
        given(fileProp.getFileTypes()).willReturn(List.of(".jpg", ".png"));
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
        setup();
        setupCurrentUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When
        fileService.upload(List.of(file), FileType.PHOTO);

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
        fileService.upload(List.of(file), FileType.USER);

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
    void 유효하지않은확장자로_파일업로드요청시_파일업로드에_실패한다() {
        // Given
        setup();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(FileException.class, () -> fileService.upload(List.of(file), FileType.PHOTO));

        verify(fileRepository, never()).save(any(File.class));
        verify(photoFileRepository, never()).save(any(PhotoFile.class));
        verify(userFileRepository, never()).save(any(UserFile.class));
    }

    @Test
    void 여러파일업로드중_유효하지않은확장자가있으면_이미저장된_물리파일을_삭제한다() throws IOException {
        // Given
        setup();
        setupCurrentUser();

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
                () -> fileService.upload(List.of(successFile, failFile), FileType.PHOTO)
        );

        assertEquals(0, countRegularFiles(tempDir));
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
        fileService.update(List.of(file), FileType.USER, uuid);

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
                () -> assertThrows(FileException.class, () -> fileService.update(List.of(file), FileType.USER, null)),
                () -> assertThrows(FileException.class, () -> fileService.update(List.of(file), FileType.USER, " ")),
                () -> assertThrows(FileException.class, () -> fileService.update(List.of(file), FileType.USER, "invalid-uuid"))
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
        assertThrows(FileException.class, () -> fileService.update(List.of(file), FileType.USER, uuid));

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
        assertThrows(FileException.class, () -> fileService.update(List.of(file), FileType.USER, uuid));

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

        Map<String, String> zipContents = readZipContents(resource);

        assertEquals("data1", zipContents.get("a.jpg"));
        assertEquals("data2", zipContents.get("b.jpg"));
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

    private long countRegularFiles(Path rootPath) throws IOException {
        try (var paths = Files.walk(rootPath)) {
            return paths.filter(Files::isRegularFile)
                    .count();
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
}
