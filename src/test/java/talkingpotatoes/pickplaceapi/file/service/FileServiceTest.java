package talkingpotatoes.pickplaceapi.file.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

/**
 * @author : 박지혁
 * @since : 2026/04/28
 */
@DisplayName("파일서비스 테스트")
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private FileProp fileProp;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private PhotoFileRepository photoFileRepository;
    @Mock
    private UserFileRepository userFileRepository;

    @TempDir
    Path tempDir;

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

    void setup() {
        when(fileProp.getFilePath()).thenReturn(tempDir.toString());
        when(fileProp.getFileTypes()).thenReturn(List.of(".jpg", ".png"));
    }

    /**
     * =========================
     * upload() - 업로드 로직 테스트
     * =========================
     */

    @Test
    void 새로운UUID로_파일업로드에_성공한다() {
        // Given
        setup();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

        // When
        fileService.upload(List.of(file), FileType.PHOTO, null);

        // Then
        verify(fileRepository).save(any(File.class));
        verify(photoFileRepository).save(any(PhotoFile.class));
    }

    @Test
    void 유효하지않은확장자로_파일업로드요청시_파일업로드에_실패한다() {
        // Given
        setup();
        MockMultipartFile file = new MockMultipartFile("file", "test.exe", "application/octet-stream",
                "data".getBytes());

        // When & Then
        assertThrows(FileException.class, () -> fileService.upload(List.of(file), FileType.PHOTO, null));
    }

    @Test
    void 이미존재하는UUID로_파일업로드를시도하면_시퀀스가증가하고_성공한다() {
        // Given
        setup();
        String uuid = UUID.randomUUID().toString();
        File existing = createFileEntity("dummy", "old.jpg", uuid, 5);
        when(fileRepository.findByUUID(uuid)).thenReturn(List.of(existing));
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

        // When
        fileService.upload(List.of(file), FileType.USER, uuid);

        // Then
        verify(fileRepository).save(argThat(f -> f.getFileSeq() == 6));
        verify(userFileRepository).save(any(UserFile.class));
    }

    /**
     * =========================
     * delete() - 삭제 로직 테스트
     * =========================
     */

    @Test
    void 파일삭제에_성공한다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();
        Path filePath = tempDir.resolve("test.jpg");
        Files.write(filePath, "data".getBytes());
        File file = createFileEntity(filePath.toString(), "test.jpg", uuid, 1);

        when(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).thenReturn(List.of(file));

        // When
        fileService.delete(uuid, List.of(1L));

        // Then
        assertFalse(Files.exists(filePath));
        verify(fileRepository).delete(file);
    }

    @Test
    void 존재하지않는파일삭제요청시_에러가발생하지않고성공한다() {
        // Given
        when(fileRepository.findByUUIDAndFileSeq(any(), any())).thenReturn(List.of());

        // When
        fileService.delete("uuid", List.of(1L));

        // Then
        verify(fileRepository, never()).delete(any());
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
        Files.write(filePath, "data".getBytes());
        File file = createFileEntity(filePath.toString(), "test.jpg", uuid, 1);
        when(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L))).thenReturn(List.of(file));
        HttpHeaders headers = new HttpHeaders();

        // When
        Resource resource = fileService.download(uuid, List.of(1L), headers);

        // Then
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("test.jpg"));
    }

    @Test
    void 여러파일들을_zip형태다운로드에_성공한다() throws IOException {
        // Given
        String uuid = UUID.randomUUID().toString();
        Path file1 = tempDir.resolve("a.jpg");
        Path file2 = tempDir.resolve("b.jpg");
        Files.write(file1, "data1".getBytes());
        Files.write(file2, "data2".getBytes());
        File f1 = createFileEntity(file1.toString(), "a.jpg", uuid, 1);
        File f2 = createFileEntity(file2.toString(), "b.jpg", uuid, 2);
        when(fileRepository.findByUUIDAndFileSeq(uuid, List.of(1L, 2L))).thenReturn(List.of(f1, f2));
        HttpHeaders headers = new HttpHeaders();

        // When
        Resource resource = fileService.download(uuid, List.of(1L, 2L), headers);

        // Then
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("application/zip", headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void 파일이존재하지않으면_파일다운로드에_실패한다() {
        // Given
        when(fileRepository.findByUUIDAndFileSeq(any(), any())).thenReturn(List.of());
        HttpHeaders headers = new HttpHeaders();

        // When & Then
        assertThrows(FileException.class, () -> fileService.download("uuid", List.of(1L), headers));
    }
}
