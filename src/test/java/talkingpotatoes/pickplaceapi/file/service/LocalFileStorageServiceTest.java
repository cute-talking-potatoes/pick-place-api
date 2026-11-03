package talkingpotatoes.pickplaceapi.file.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static talkingpotatoes.pickplaceapi.global.exception.ExceptionCode.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import talkingpotatoes.pickplaceapi.file.domain.FileMetadata;
import talkingpotatoes.pickplaceapi.file.domain.FileType;
import talkingpotatoes.pickplaceapi.file.domain.entity.File;
import talkingpotatoes.pickplaceapi.file.domain.prop.FileProp;
import talkingpotatoes.pickplaceapi.global.exception.ExceptionCode;
import talkingpotatoes.pickplaceapi.global.exception.FileException;

/**
 * 파일 I/O 실패 시 클라이언트로 나가는 예외 메시지에 서버 경로·IOException 메시지가 들어가지 않는지 검증한다.
 *
 * @author : 박지혁
 * @since : 2026/11/03
 */
@DisplayName("로컬 파일 저장소 예외 메시지 테스트")
@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    @Mock
    private FileProp fileProp;

    @TempDir
    Path tempDir;

    private LocalFileStorageService localFileStorageService;
    private FileDownloadService fileDownloadService;

    @BeforeEach
    void setUp() {
        localFileStorageService = new LocalFileStorageService(fileProp);
        fileDownloadService = new FileDownloadService(localFileStorageService);
    }

    @Test
    void 존재하지않는경로로_validateExists호출시_예외메시지에_경로가없다() {
        // Given
        Path notExists = tempDir.resolve("secret-dir").resolve("not-exists.jpg");

        // When
        FileException exception = assertThrows(FileException.class,
                () -> localFileStorageService.validateExists(notExists));

        // Then
        assertSafeMessage(exception, ERR_FILE_NOT_EXIST, notExists);
    }

    @Test
    void 디렉토리생성에_실패해도_예외메시지에_경로가없다() throws IOException {
        // Given - 저장 루트가 일반 파일이면 하위 디렉토리를 만들 수 없다
        Path rootAsFile = tempDir.resolve("root-file");
        Files.write(rootAsFile, "x".getBytes(StandardCharsets.UTF_8));
        given(fileProp.getFilePath()).willReturn(rootAsFile.toString());

        // When
        FileException exception = assertThrows(FileException.class,
                () -> localFileStorageService.createDirectory("uuid-dir"));

        // Then
        assertSafeMessage(exception, ERR_FILE_CREATE_DIRECTORY, rootAsFile);
    }

    @Test
    void 파일저장에_실패해도_예외메시지에_경로가없다() {
        // Given - 존재하지 않는 디렉토리 하위로 저장하면 IOException이 발생한다
        Path missingDir = tempDir.resolve("missing-dir");
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[] {1, 2, 3});
        FileMetadata fileMetadata = FileMetadata.create(file, missingDir, "uuid", 1, FileType.USER);

        // When
        FileException exception = assertThrows(FileException.class,
                () -> localFileStorageService.save(fileMetadata));

        // Then
        assertSafeMessage(exception, ERR_FILE_SAVE, missingDir);
    }

    @Test
    void 파일삭제에_실패해도_예외메시지에_경로가없다() throws IOException {
        // Given - 비어있지 않은 디렉토리는 삭제할 수 없다
        Path nonEmptyDir = tempDir.resolve("non-empty");
        Files.createDirectories(nonEmptyDir);
        Files.write(nonEmptyDir.resolve("child"), "x".getBytes(StandardCharsets.UTF_8));

        // When
        FileException exception = assertThrows(FileException.class,
                () -> localFileStorageService.delete(nonEmptyDir));

        // Then
        assertSafeMessage(exception, ERR_FILE_DELETE, nonEmptyDir);
    }

    @Test
    void 물리파일이없는_사진표시요청시_예외메시지에_경로가없다() {
        // Given
        Path notExists = tempDir.resolve("photo-not-exists.jpg");
        File file = File.builder()
                .fileNm("photo.jpg")
                .fileSrc(notExists.toString())
                .fileExtension(".jpg")
                .build();

        // When
        FileException exception = assertThrows(FileException.class,
                () -> fileDownloadService.displayImage(file, new HttpHeaders()));

        // Then
        assertSafeMessage(exception, ERR_FILE_NOT_EXIST, notExists);
    }

    @Test
    void ZIP다운로드중_물리파일이없으면_예외메시지에_경로가없다() throws IOException {
        // Given
        Path exists = tempDir.resolve("a.jpg");
        Files.write(exists, "a".getBytes(StandardCharsets.UTF_8));
        Path notExists = tempDir.resolve("b-not-exists.jpg");
        File file1 = File.builder().fileNm("a.jpg").fileSrc(exists.toString()).fileSeq(1L).build();
        File file2 = File.builder().fileNm("b.jpg").fileSrc(notExists.toString()).fileSeq(2L).build();

        // When
        FileException exception = assertThrows(FileException.class,
                () -> fileDownloadService.download("zip-uuid", List.of(1L, 2L), List.of(file1, file2),
                        new HttpHeaders()));

        // Then
        assertAll(
                () -> assertEquals(ERR_FILE_NOT_EXIST.name(), exception.getCode()),
                () -> assertFalse(exception.getMessage().contains(tempDir.toString())),
                () -> assertFalse(exception.getMessage().contains("b-not-exists"))
        );
    }

    private void assertSafeMessage(FileException exception, ExceptionCode expectedCode, Path sensitivePath) {
        assertAll(
                () -> assertEquals(expectedCode.name(), exception.getCode()),
                () -> assertEquals(expectedCode.getKorErrorMessage(), exception.getMessage()),
                () -> assertFalse(exception.getMessage().contains(sensitivePath.toString())),
                () -> assertFalse(exception.getMessage().contains(tempDir.toString()))
        );
    }
}
